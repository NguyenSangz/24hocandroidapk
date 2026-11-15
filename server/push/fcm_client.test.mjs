import test from "node:test";
import assert from "node:assert/strict";
import crypto from "node:crypto";
import {
  normalizeServiceAccount,
  buildAssertion,
  fetchAccessToken,
  buildDataMessage,
  classifyFcmResponse,
  computeBackoffMs,
  RateLimiter
} from "./fcm_client.mjs";

function generateKeyPair() {
  const { privateKey, publicKey } = crypto.generateKeyPairSync("rsa", { modulusLength: 2048 });
  return {
    privatePem: privateKey.export({ type: "pkcs8", format: "pem" }),
    publicKey
  };
}

test("normalizeServiceAccount chấp nhận JSON hợp lệ và từ chối JSON thiếu trường", () => {
  const valid = normalizeServiceAccount({
    type: "service_account",
    project_id: "gen-lang-client-0422573454",
    client_email: "fcm@example.com",
    private_key: "-----BEGIN PRIVATE KEY-----\nabc\n-----END PRIVATE KEY-----"
  });
  assert.equal(valid.projectId, "gen-lang-client-0422573454");
  assert.equal(valid.clientEmail, "fcm@example.com");

  assert.throws(() => normalizeServiceAccount("{}"), /type = service_account/);
  assert.throws(
    () => normalizeServiceAccount({ type: "service_account", project_id: "x" }),
    /thiếu project_id/
  );
});

test("buildAssertion tạo JWT RS256 có chữ ký hợp lệ và đúng claims", () => {
  const { privatePem, publicKey } = generateKeyPair();
  const assertion = buildAssertion({
    clientEmail: "fcm@example.com",
    privateKey: privatePem,
    nowSec: 1_700_000_000
  });

  const parts = assertion.split(".");
  assert.equal(parts.length, 3);

  const header = JSON.parse(Buffer.from(parts[0], "base64url").toString("utf8"));
  assert.equal(header.alg, "RS256");
  assert.equal(header.typ, "JWT");

  const payload = JSON.parse(Buffer.from(parts[1], "base64url").toString("utf8"));
  assert.equal(payload.iss, "fcm@example.com");
  assert.equal(payload.aud, "https://oauth2.googleapis.com/token");
  assert.equal(payload.scope, "https://www.googleapis.com/auth/firebase.messaging");
  assert.equal(payload.iat, 1_700_000_000);
  assert.equal(payload.exp, 1_700_003_600);

  const verified = crypto
    .createVerify("RSA-SHA256")
    .update(parts[0] + "." + parts[1])
    .verify(publicKey, Buffer.from(parts[2], "base64url"));
  assert.equal(verified, true);
});

test("fetchAccessToken gửi đúng grant_type và trả access_token", async () => {
  const { privatePem } = generateKeyPair();
  let captured = null;
  const fakeFetch = async (url, options) => {
    captured = { url, options };
    return {
      ok: true,
      status: 200,
      json: async () => ({ access_token: "ya29.test" }),
      text: async () => ""
    };
  };

  const accessToken = await fetchAccessToken({
    clientEmail: "fcm@example.com",
    privateKey: privatePem,
    nowSec: 1_700_000_000,
    fetchImpl: fakeFetch
  });

  assert.equal(accessToken, "ya29.test");
  assert.equal(captured.url, "https://oauth2.googleapis.com/token");
  const body = new URLSearchParams(captured.options.body);
  assert.equal(body.get("grant_type"), "urn:ietf:params:oauth:grant-type:jwt-bearer");
  assert.ok(body.get("assertion").split(".").length === 3);
});

test("buildDataMessage chỉ giữ type và content_id, bỏ mọi trường nhạy cảm", () => {
  const message = buildDataMessage("fcm_token_abcdefghijklmnopqrstuvwxyz", {
    type: "assignment_due",
    content_id: "assign_42",
    student_name: "Nguyễn Văn A",
    score: "9.5",
    title: "Bài tập sắp đến hạn"
  });

  assert.deepEqual(Object.keys(message.message.data).sort(), ["content_id", "type"]);
  assert.equal(message.message.data.student_name, undefined);
  assert.equal(message.message.data.score, undefined);
  assert.equal(message.message.data.title, undefined);
  assert.equal(message.message.android.priority, "normal");
  assert.equal(message.message.notification, undefined);
});

test("classifyFcmResponse nhận diện token chết, lỗi tạm và lỗi xác thực", () => {
  assert.equal(classifyFcmResponse(200, "{}").kind, "sent");

  const notFound = classifyFcmResponse(
    404,
    JSON.stringify({ error: { status: "NOT_FOUND", message: "Requested entity was not found." } })
  );
  assert.equal(notFound.kind, "invalid_token");
  assert.equal(notFound.cleanupToken, true);

  const badToken = classifyFcmResponse(
    400,
    JSON.stringify({ error: { status: "INVALID_ARGUMENT", message: "The registration token is not valid." } })
  );
  assert.equal(badToken.kind, "invalid_token");
  assert.equal(badToken.cleanupToken, true);

  assert.equal(classifyFcmResponse(429, "quota").kind, "retry");
  assert.equal(classifyFcmResponse(500, "server").kind, "retry");
  assert.equal(classifyFcmResponse(503, "unavailable").kind, "retry");

  assert.equal(classifyFcmResponse(401, "unauthorized").kind, "auth_error");
  assert.equal(classifyFcmResponse(403, "forbidden").kind, "auth_error");

  const other = classifyFcmResponse(400, JSON.stringify({ error: { status: "INVALID_ARGUMENT", message: "Bad data" } }));
  assert.equal(other.kind, "failed");
});

test("computeBackoffMs tăng theo số lần thử và chặn trần 1 giờ", () => {
  const now = 1_000_000;
  assert.equal(computeBackoffMs(1, now), now + 30_000);
  assert.equal(computeBackoffMs(2, now), now + 60_000);
  assert.equal(computeBackoffMs(3, now), now + 120_000);
  assert.equal(computeBackoffMs(20, now), now + 60 * 60 * 1000);
});

test("RateLimiter giãn cách các lần gửi theo minIntervalMs", async () => {
  let clock = 0;
  const sleeps = [];
  const limiter = new RateLimiter({
    minIntervalMs: 50,
    now: () => clock,
    sleep: async (ms) => {
      sleeps.push(ms);
      clock += ms;
    }
  });

  await limiter.wait();
  clock += 10;
  await limiter.wait();

  assert.deepEqual(sleeps, [40]);
});
