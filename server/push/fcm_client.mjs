/**
 * F20a – Lõi gửi FCM HTTP v1, không phụ thuộc thư viện ngoài.
 *
 * Chỉ dùng Node >= 18 (global fetch, node:crypto). Các hàm ở đây được tách khỏi
 * vòng lặp worker để test bằng `node --test` mà không cần gọi Google thật.
 */

import crypto from "node:crypto";

export const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
export const GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";

/** Kiểm tra + chuẩn hóa service account JSON. Ném lỗi nếu thiếu trường bắt buộc. */
export function normalizeServiceAccount(raw) {
  const sa = typeof raw === "string" ? JSON.parse(raw) : raw;
  if (!sa || typeof sa !== "object") {
    throw new Error("Service account JSON rỗng hoặc sai định dạng.");
  }
  if (sa.type !== "service_account") {
    throw new Error("Service account JSON phải có type = service_account.");
  }
  if (!sa.project_id || !sa.client_email || !sa.private_key) {
    throw new Error("Service account thiếu project_id, client_email hoặc private_key.");
  }
  return {
    projectId: String(sa.project_id),
    clientEmail: String(sa.client_email),
    privateKey: String(sa.private_key)
  };
}

function toBase64Url(input) {
  const buffer = Buffer.isBuffer(input) ? input : Buffer.from(input, "utf8");
  return buffer.toString("base64").replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

/**
 * Tạo JWT assertion RS256 để đổi lấy access token Google.
 * @param {{clientEmail:string, privateKey:string, nowSec?:number, scope?:string}} options
 */
export function buildAssertion(options) {
  const nowSec = Number(options.nowSec || Math.floor(Date.now() / 1000));
  const header = { alg: "RS256", typ: "JWT" };
  const payload = {
    iss: options.clientEmail,
    scope: options.scope || FCM_SCOPE,
    aud: GOOGLE_TOKEN_URL,
    iat: nowSec,
    exp: nowSec + 3600
  };
  const signingInput =
    toBase64Url(JSON.stringify(header)) + "." + toBase64Url(JSON.stringify(payload));
  const signature = crypto.createSign("RSA-SHA256").update(signingInput).sign(options.privateKey);
  return signingInput + "." + toBase64Url(signature);
}

/** Đổi JWT assertion lấy access token; dùng fetchImpl để test. */
export async function fetchAccessToken(options) {
  const fetchImpl = options.fetchImpl || fetch;
  const assertion = buildAssertion(options);
  const response = await fetchImpl(GOOGLE_TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion
    }).toString()
  });
  if (!response.ok) {
    const text = await safeText(response);
    throw new Error("Không lấy được access token Google (" + response.status + "): " + text);
  }
  const json = await response.json();
  if (!json.access_token) {
    throw new Error("Phản hồi Google không có access_token.");
  }
  return json.access_token;
}

/**
 * Dựng message FCM data-only. CHỈ giữ type + content_id.
 * Không gửi notification title/body vì chữ được dựng cục bộ trên máy.
 */
export function buildDataMessage(token, data) {
  if (!token || typeof token !== "string") {
    throw new Error("Thiếu token thiết bị.");
  }
  const safeData = {};
  if (data && typeof data.type === "string" && data.type.trim()) {
    safeData.type = data.type.trim();
  }
  if (data && data.content_id !== undefined && data.content_id !== null) {
    const contentId = String(data.content_id).trim();
    if (contentId) safeData.content_id = contentId;
  }
  return {
    message: {
      token,
      data: safeData,
      android: { priority: "normal" }
    }
  };
}

async function safeText(response) {
  try {
    return await response.text();
  } catch (_) {
    return "";
  }
}

/**
 * Phân loại phản hồi FCM:
 * - sent: gửi thành công.
 * - invalid_token: token chết/cần xóa khỏi DB.
 * - retry: lỗi tạm thời (429/5xx), thử lại với backoff.
 * - auth_error: sai service account/quyền, dừng lượt chạy.
 * - failed: lỗi không thể thử lại.
 */
export function classifyFcmResponse(status, bodyText) {
  const text = String(bodyText || "");
  if (status >= 200 && status < 300) {
    return { kind: "sent", retryable: false, cleanupToken: false };
  }

  let errorStatus = "";
  let errorMessage = text;
  try {
    const parsed = JSON.parse(text);
    if (parsed && parsed.error) {
      errorStatus = String(parsed.error.status || "");
      errorMessage = String(parsed.error.message || text);
    }
  } catch (_) {
    // Không phải JSON: dùng text gốc.
  }

  if (
    status === 404 ||
    errorStatus === "NOT_FOUND" ||
    errorStatus === "UNREGISTERED" ||
    /registration token is not registered|requested entity was not found|unregistered/i.test(errorMessage)
  ) {
    return { kind: "invalid_token", retryable: false, cleanupToken: true };
  }

  if (
    status === 400 &&
    /registration token|invalid.*token|token.*invalid/i.test(errorMessage)
  ) {
    return { kind: "invalid_token", retryable: false, cleanupToken: true };
  }

  if (status === 401 || status === 403 || errorStatus === "UNAUTHENTICATED" || errorStatus === "PERMISSION_DENIED") {
    return { kind: "auth_error", retryable: false, cleanupToken: false };
  }

  if (status === 429 || status >= 500) {
    return { kind: "retry", retryable: true, cleanupToken: false };
  }

  return { kind: "failed", retryable: false, cleanupToken: false };
}

/** Tính thời điểm thử lại: 30s, 60s, 2m, 4m ... tối đa 1 giờ. */
export function computeBackoffMs(attempts, nowMs) {
  const n = Math.max(1, Number(attempts || 1));
  const delay = Math.min(30_000 * Math.pow(2, n - 1), 60 * 60 * 1000);
  return Number(nowMs || Date.now()) + delay;
}

/**
 * Giới hạn tốc độ gửi (mặc định 20 tin/giây) và cho phép test bằng đồng hồ giả.
 */
export class RateLimiter {
  constructor(options = {}) {
    this.minIntervalMs = Number(options.minIntervalMs ?? 50);
    this.now = options.now || (() => Date.now());
    this.sleep = options.sleep || ((ms) => new Promise((resolve) => setTimeout(resolve, ms)));
    this.lastAt = null;
  }

  async wait() {
    const current = this.now();
    const elapsed = this.lastAt === null ? Number.POSITIVE_INFINITY : current - this.lastAt;
    if (elapsed < this.minIntervalMs) {
      await this.sleep(this.minIntervalMs - elapsed);
    }
    this.lastAt = this.now();
  }
}
