import test from "node:test";
import assert from "node:assert/strict";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const core = require("../pb_hooks/lib/push_core.js");

test("normalizeToken chỉ nhận token FCM hợp lý", () => {
  assert.equal(core.normalizeToken("fcm_token_abcdefghijklmnopqrstuvwxyz"), "fcm_token_abcdefghijklmnopqrstuvwxyz");
  assert.equal(core.normalizeToken("  fcm_token_abcdefghijklmnopqrstuvwxyz  "), "fcm_token_abcdefghijklmnopqrstuvwxyz");
  assert.equal(core.normalizeToken("short"), null);
  assert.equal(core.normalizeToken("a".repeat(4097)), null);
  assert.equal(core.normalizeToken("has space in token abcdefghijklmnop"), null);
  assert.equal(core.normalizeToken(12345), null);
  assert.equal(core.normalizeToken(null), null);
});

test("normalizePlatform chỉ nhận android/ios", () => {
  assert.equal(core.normalizePlatform("android"), "android");
  assert.equal(core.normalizePlatform("ANDROID"), "android");
  assert.equal(core.normalizePlatform("ios"), "ios");
  assert.equal(core.normalizePlatform(undefined), "android");
  assert.equal(core.normalizePlatform("windows"), null);
});

test("normalizeType chặn mã loại lạ", () => {
  assert.equal(core.normalizeType("study_reminder"), "study_reminder");
  assert.equal(core.normalizeType("system"), "system");
  assert.equal(core.normalizeType("promo_banner"), null);
  assert.equal(core.normalizeType(""), null);
});

test("normalizeContentId chỉ cho chữ, số và _ . -", () => {
  assert.equal(core.normalizeContentId("lesson_toan_6_bai-1.v2"), "lesson_toan_6_bai-1.v2");
  assert.equal(core.normalizeContentId(""), null);
  assert.equal(core.normalizeContentId(null), null);
  assert.equal(core.normalizeContentId("abc<script>"), null);
  assert.equal(core.normalizeContentId("a".repeat(129)), null);
});

test("checkRateLimit chặn sau max lượt trong cửa sổ và mở lại khi hết cửa sổ", () => {
  const store = new Map();
  const adapter = {
    get: (key) => store.get(key),
    set: (key, value) => store.set(key, value)
  };
  const options = { windowMs: 60_000, max: 3 };
  const base = 1_000_000;

  assert.equal(core.checkRateLimit(adapter, "user_1", { ...options, now: base }), true);
  assert.equal(core.checkRateLimit(adapter, "user_1", { ...options, now: base + 1 }), true);
  assert.equal(core.checkRateLimit(adapter, "user_1", { ...options, now: base + 2 }), true);
  assert.equal(core.checkRateLimit(adapter, "user_1", { ...options, now: base + 3 }), false);
  // Người dùng khác có ngân sách riêng.
  assert.equal(core.checkRateLimit(adapter, "user_2", { ...options, now: base + 4 }), true);
  // Sau cửa sổ, lượt cũ trôi khỏi bucket.
  assert.equal(core.checkRateLimit(adapter, "user_1", { ...options, now: base + 61_000 }), true);
});

test("buildDedupeKey ổn định và tách theo token cuối", () => {
  const key1 = core.buildDedupeKey("user_1", "assignment_due", "assign_42", "fcm_token_aaaaaaaaaaaa123456");
  const key2 = core.buildDedupeKey("user_1", "assignment_due", "assign_42", "fcm_token_bbbbbbbbbbbb123456");
  assert.equal(key1, "user_1:assignment_due:assign_42:aaaaaa123456");
  assert.notEqual(key1, key2);
});

test("safeErrorMessage cắt ngắn và bỏ xuống dòng", () => {
  const message = core.safeErrorMessage("line1\nline2\t" + "x".repeat(500));
  assert.ok(message.length <= 400);
  assert.ok(!message.includes("\n"));
  assert.ok(!message.includes("\t"));
});
