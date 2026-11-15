#!/usr/bin/env node
/**
 * F20a – Worker gửi thông báo đẩy qua FCM HTTP v1.
 *
 * Đọc `push_outbox` (status = pending, scheduledAt <= now) bằng quyền admin
 * PocketBase, gửi data-only message (chỉ type + content_id), sau đó:
 * - thành công  -> status = sent
 * - token chết -> status = failed + xóa device_tokens tương ứng
 * - lỗi tạm    -> status = pending + backoff (30s, 60s, 2m ... tối đa 1h)
 *
 * Chạy một lượt rồi thoát (hợp với systemd timer). Không log token hay nội dung.
 *
 * Biến môi trường (đặt trong /etc/pocketbase/push.env, chmod 600):
 *   PB_URL=https://api.tenmiencuatoi.vn
 *   PB_ADMIN_EMAIL=admin@tenmiencuatoi.vn
 *   PB_ADMIN_PASSWORD=...
 *   FCM_SERVICE_ACCOUNT_FILE=/etc/pocketbase/fcm-service-account.json
 *   MAX_PER_RUN=50
 *   SEND_INTERVAL_MS=50
 */

import fs from "node:fs";
import {
  normalizeServiceAccount,
  fetchAccessToken,
  buildDataMessage,
  classifyFcmResponse,
  computeBackoffMs,
  RateLimiter
} from "./fcm_client.mjs";

function log(message) {
  const time = new Date().toISOString();
  // Không bao giờ in token/payload cá nhân.
  console.log(`[${time}] ${message}`);
}

function requireEnv(name) {
  const value = (process.env[name] || "").trim();
  if (!value) throw new Error(`Thiếu biến môi trường ${name}.`);
  return value;
}

function loadServiceAccount() {
  const filePath = requireEnv("FCM_SERVICE_ACCOUNT_FILE");
  if (!fs.existsSync(filePath)) {
    throw new Error("Không tìm thấy file service account FCM.");
  }
  if (process.platform !== "win32") {
    const mode = fs.statSync(filePath).mode & 0o777;
    if ((mode & 0o077) !== 0) {
      log("CẢNH BÁO: service account đang có quyền đọc với nhóm/khác. Hãy chmod 600.");
    }
  }
  const raw = JSON.parse(fs.readFileSync(filePath, "utf8"));
  return normalizeServiceAccount(raw);
}

async function pocketBaseLogin(baseUrl) {
  const token = (process.env.PB_ADMIN_TOKEN || "").trim();
  if (token) return token;

  const email = requireEnv("PB_ADMIN_EMAIL");
  const password = requireEnv("PB_ADMIN_PASSWORD");
  const candidates = [
    "/api/collections/_superusers/auth-with-password",
    "/api/admins/auth-with-password"
  ];

  let lastError = "không rõ";
  for (const path of candidates) {
    const response = await fetch(baseUrl + path, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ identity: email, password })
    });
    if (response.ok) {
      const json = await response.json();
      if (json.token) return json.token;
    }
    lastError = `${response.status}`;
  }
  throw new Error(`Đăng nhập admin PocketBase thất bại (${lastError}).`);
}

async function pbRequest(baseUrl, token, path, options = {}) {
  const response = await fetch(baseUrl + path, {
    ...options,
    headers: {
      Authorization: token,
      "Content-Type": "application/json",
      ...(options.headers || {})
    }
  });
  const text = await response.text();
  if (!response.ok) {
    throw new Error(`PocketBase ${response.status}: ${text.slice(0, 200)}`);
  }
  return text ? JSON.parse(text) : {};
}

async function fetchPendingOutbox(baseUrl, token, limit) {
  const filter = encodeURIComponent(`status='pending' && scheduledAt <= ${Date.now()}`);
  const path = `/api/collections/push_outbox/records?perPage=${limit}&sort=createdAt&filter=${filter}`;
  const data = await pbRequest(baseUrl, token, path);
  return Array.isArray(data.items) ? data.items : [];
}

async function updateOutbox(baseUrl, token, id, patch) {
  await pbRequest(baseUrl, token, `/api/collections/push_outbox/records/${id}`, {
    method: "PATCH",
    body: JSON.stringify(patch)
  });
}

async function deleteDeviceToken(baseUrl, token, deviceTokenId) {
  try {
    await pbRequest(baseUrl, token, `/api/collections/device_tokens/records/${deviceTokenId}`, {
      method: "DELETE"
    });
  } catch (_) {
    // Token có thể đã bị xóa bởi đăng xuất/xóa tài khoản.
  }
}

async function sendOne({ baseUrl, pbToken, accessToken, projectId, item, limiter }) {
  await limiter.wait();
  const message = buildDataMessage(item.token, {
    type: item.type,
    content_id: item.contentId
  });

  const response = await fetch(
    `https://fcm.googleapis.com/v1/projects/${encodeURIComponent(projectId)}/messages:send`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify(message)
    }
  );
  const text = await response.text();
  const result = classifyFcmResponse(response.status, text);
  const attempts = Number(item.attempts || 0);

  if (result.kind === "sent") {
    await updateOutbox(baseUrl, pbToken, item.id, {
      status: "sent",
      sentAt: Date.now(),
      lastError: ""
    });
    log(`Đã gửi thông báo ${item.type || "?"} (outbox ${item.id}).`);
    return "sent";
  }

  if (result.kind === "invalid_token") {
    await updateOutbox(baseUrl, pbToken, item.id, {
      status: "failed",
      lastError: "invalid_token"
    });
    if (item.deviceTokenId) {
      await deleteDeviceToken(baseUrl, pbToken, item.deviceTokenId);
    }
    log(`Token không còn hợp lệ, đã dọn thiết bị (outbox ${item.id}).`);
    return "invalid_token";
  }

  if (result.kind === "retry") {
    const nextAt = computeBackoffMs(attempts + 1, Date.now());
    await updateOutbox(baseUrl, pbToken, item.id, {
      status: "pending",
      attempts: attempts + 1,
      scheduledAt: nextAt,
      lastError: `retry_${response.status}`
    });
    log(`FCM tạm lỗi ${response.status}; hẹn thử lại (outbox ${item.id}).`);
    return "retry";
  }

  if (result.kind === "auth_error") {
    log(`FCM từ chối xác thực (${response.status}). Kiểm tra service account/quyền.`);
    return "auth_error";
  }

  await updateOutbox(baseUrl, pbToken, item.id, {
    status: "failed",
    attempts: attempts + 1,
    lastError: `fcm_${response.status}`
  });
  log(`Gửi thất bại không thể thử lại (${response.status}, outbox ${item.id}).`);
  return "failed";
}

async function main() {
  const baseUrl = requireEnv("PB_URL").replace(/\/+$/, "");
  const serviceAccount = loadServiceAccount();
  const maxPerRun = Number(process.env.MAX_PER_RUN || 50);
  const intervalMs = Number(process.env.SEND_INTERVAL_MS || 50);

  const pbToken = await pocketBaseLogin(baseUrl);
  const accessToken = await fetchAccessToken({
    clientEmail: serviceAccount.clientEmail,
    privateKey: serviceAccount.privateKey,
    scope: "https://www.googleapis.com/auth/firebase.messaging"
  });

  const items = await fetchPendingOutbox(baseUrl, pbToken, maxPerRun);
  if (items.length === 0) {
    log("Không có thông báo chờ gửi.");
    return;
  }

  const limiter = new RateLimiter({ minIntervalMs: intervalMs });
  let sent = 0;
  let retry = 0;
  let failed = 0;

  for (const item of items) {
    const outcome = await sendOne({
      baseUrl,
      pbToken,
      accessToken,
      projectId: serviceAccount.projectId,
      item,
      limiter
    });
    if (outcome === "sent") sent++;
    else if (outcome === "retry") retry++;
    else failed++;
    if (outcome === "auth_error") break;
  }

  log(`Kết thúc lượt gửi: ${sent} thành công, ${retry} chờ thử lại, ${failed} thất bại.`);
}

main().catch((error) => {
  // Chỉ log thông điệp, không log biến môi trường hay token.
  console.error(`[push-sender] Lỗi: ${error.message}`);
  process.exitCode = 1;
});
