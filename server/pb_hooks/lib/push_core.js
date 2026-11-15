/// <reference path="../../pb_data/types.d.ts" />

/**
 * F20a – Lõi kiểm tra dùng chung cho hook PocketBase và worker Node.
 *
 * File này CỐ TÌNH không tham chiếu biến toàn cục của PocketBase ($app, ...)
 * để có thể nạp trực tiếp bằng `node --test` mà không cần server.
 *
 * Nguyên tắc: payload thông báo chỉ có `type` + `content_id`; không bao giờ
 * chứa tên học sinh, điểm số, email hay nội dung bài làm.
 */

"use strict";

const TOKEN_MIN_LENGTH = 20;
const TOKEN_MAX_LENGTH = 4096;

const ALLOWED_TYPES = [
    "study_reminder",
    "assignment_due",
    "weekly_report",
    "new_content",
    "system"
];

const CONTENT_ID_MAX_LENGTH = 128;
const SAFE_CONTENT_ID = /^[A-Za-z0-9_.-]{1,128}$/;

/** Chuẩn hóa token FCM; trả null nếu rỗng/quá dài/không phải chuỗi. */
function normalizeToken(raw) {
    if (typeof raw !== "string") return null;
    const token = raw.trim();
    if (token.length < TOKEN_MIN_LENGTH || token.length > TOKEN_MAX_LENGTH) return null;
    // Token FCM không chứa khoảng trắng; chặn luôn ký tự điều khiển.
    if (/[\s\u0000-\u001f\u007f]/.test(token)) return null;
    return token;
}

/** Chỉ nhận nền tảng đã khai báo; mặc định android. */
function normalizePlatform(raw) {
    const platform = String(raw || "android").trim().toLowerCase();
    return platform === "android" || platform === "ios" ? platform : null;
}

/** Chỉ nhận đúng mã loại đã khai báo. */
function normalizeType(raw) {
    const type = String(raw || "").trim();
    return ALLOWED_TYPES.indexOf(type) !== -1 ? type : null;
}

/** content_id phải an toàn và ngắn; rỗng -> null (hợp lệ với thông báo hệ thống). */
function normalizeContentId(raw) {
    if (raw === undefined || raw === null) return null;
    const contentId = String(raw).trim();
    if (contentId === "") return null;
    return SAFE_CONTENT_ID.test(contentId) ? contentId : null;
}

/**
 * Kiểm tra rate limit dạng cửa sổ trượt trên một store có get/set.
 * @param {{get:Function,set:Function}} store
 * @param {string} key
 * @param {{windowMs:number,max:number,now?:number}} options
 * @returns {boolean} true nếu được phép, false nếu vượt giới hạn
 */
function checkRateLimit(store, key, options) {
    const windowMs = Number(options.windowMs);
    const max = Number(options.max);
    const now = Number(options.now || Date.now());
    let bucket = store.get(key);
    if (!Array.isArray(bucket)) bucket = [];

    const cutoff = now - windowMs;
    const recent = [];
    for (let i = 0; i < bucket.length; i++) {
        if (Number(bucket[i]) > cutoff) recent.push(Number(bucket[i]));
    }

    if (recent.length >= max) {
        store.set(key, recent);
        return false;
    }
    recent.push(now);
    store.set(key, recent);
    return true;
}

/**
 * Khóa chống trùng cho hàng đợi: cùng người dùng + cùng sự kiện + cùng token
 * chỉ tạo một bản ghi outbox.
 */
function buildDedupeKey(userId, type, contentId, token) {
    const suffix = token ? String(token).slice(-12) : "no-token";
    return [String(userId || "system"), String(type || ""), String(contentId || "-"), suffix].join(":");
}

/** Rút gọn thông điệp lỗi để lưu log/DB, không chứa token. */
function safeErrorMessage(raw) {
    const text = String(raw || "").replace(/[\r\n\t]+/g, " ").trim();
    return text.length > 400 ? text.slice(0, 400) : text;
}

module.exports = {
    ALLOWED_TYPES,
    TOKEN_MIN_LENGTH,
    TOKEN_MAX_LENGTH,
    CONTENT_ID_MAX_LENGTH,
    normalizeToken,
    normalizePlatform,
    normalizeType,
    normalizeContentId,
    checkRateLimit,
    buildDedupeKey,
    safeErrorMessage
};
