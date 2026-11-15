/// <reference path="../pb_data/types.d.ts" />

/**
 * F20a PocketBase Hooks – thông báo đẩy tối thiểu.
 *
 * 1. POST /api/custom/push/register
 *    - Bắt buộc đăng nhập (khách không có phiên nên không thể gọi).
 *    - Chuẩn hóa token/nền tảng, rate limit 30 lượt / 5 phút / tài khoản.
 *    - Upsert idempotent theo token; nếu thiết bị đổi tài khoản thì chuyển token
 *      sang tài khoản mới (token thuộc về thiết bị, không thuộc người dùng cũ).
 *
 * 2. POST /api/custom/push/unregister
 *    - Chỉ xóa token thuộc chính tài khoản đang đăng nhập.
 *    - Gọi trước khi đăng xuất / xóa tài khoản / tắt thông báo.
 *
 * 3. POST /api/custom/push/enqueue (chỉ server/ops, bảo vệ bằng PUSH_ENQUEUE_SECRET)
 *    - Đưa thông báo vào `push_outbox` cho worker Node gửi qua FCM HTTP v1.
 *    - Payload chỉ có type + content_id; dedupe theo (user, type, contentId, token).
 *
 * LƯU Ý BẢO MẬT:
 * - Không log token thật.
 * - Worker đọc outbox bằng quyền admin PocketBase; app không truy cập outbox.
 */

const pushCore = require(`${__hooks}/lib/push_core.js`);

const REGISTER_RATE_WINDOW_MS = 5 * 60 * 1000;
const REGISTER_RATE_MAX = 30;
const ENQUEUE_RATE_WINDOW_MS = 60 * 1000;
const ENQUEUE_RATE_MAX = 60;

function findDeviceTokenByValue(token) {
    try {
        return $app.findFirstRecordByData("device_tokens", "token", token);
    } catch (_) {
        return null;
    }
}

routerAdd("POST", "/api/custom/push/register", (e) => {
    const authRecord = e.auth;
    if (!authRecord) {
        throw new UnauthorizedError("Yêu cầu đăng nhập để đăng ký thông báo.");
    }
    const birthYear = authRecord.getInt("birthYear");
    const isMinor = !birthYear ||
        authRecord.getBool("isMinorUnder16") ||
        new Date().getUTCFullYear() - birthYear < 16;
    if (isMinor && !authRecord.getBool("parentalConsentGranted")) {
        return e.json(403, {
            code: 403,
            message: "Chờ xác nhận của phụ huynh trước khi bật tính năng trực tuyến."
        });
    }

    const body = e.requestInfo().body || {};
    const token = pushCore.normalizeToken(body.token);
    if (!token) {
        throw new BadRequestError("Token thiết bị không hợp lệ.");
    }
    const platform = pushCore.normalizePlatform(body.platform);
    if (!platform) {
        throw new BadRequestError("Nền tảng thiết bị không hợp lệ.");
    }

    const allowed = pushCore.checkRateLimit($app.store(), "f20a_push_reg_" + authRecord.id, {
        windowMs: REGISTER_RATE_WINDOW_MS,
        max: REGISTER_RATE_MAX
    });
    if (!allowed) {
        return e.json(429, {
            code: 429,
            message: "Bạn đang đăng ký thông báo quá nhanh. Vui lòng thử lại sau ít phút."
        });
    }

    const existing = findDeviceTokenByValue(token);
    if (existing) {
        const ownerId = existing.getString("userId");
        if (ownerId !== authRecord.id) {
            // Thiết bị được đăng nhập bằng tài khoản khác: chuyển quyền sở hữu token.
            existing.set("userId", authRecord.id);
        }
        existing.set("platform", platform);
        $app.save(existing);
    } else {
        const collection = $app.findCollectionByNameOrId("device_tokens");
        const record = new Record(collection);
        record.set("userId", authRecord.id);
        record.set("token", token);
        record.set("platform", platform);
        $app.save(record);
    }

    return e.json(200, { registered: true, platform: platform });
}, $apis.requireAuth());

routerAdd("POST", "/api/custom/push/unregister", (e) => {
    const authRecord = e.auth;
    if (!authRecord) {
        throw new UnauthorizedError("Yêu cầu đăng nhập để hủy thông báo.");
    }

    const body = e.requestInfo().body || {};
    const token = pushCore.normalizeToken(body.token);
    if (!token) {
        throw new BadRequestError("Token thiết bị không hợp lệ.");
    }

    let records = [];
    try {
        records = $app.findRecordsByFilter(
            "device_tokens",
            "token = {:token} && userId = {:uid}",
            "",
            0,
            0,
            { token: token, uid: authRecord.id }
        );
    } catch (_) {
        records = [];
    }

    let removed = 0;
    for (let i = 0; i < records.length; i++) {
        try {
            $app.delete(records[i]);
            removed++;
        } catch (_) {
            // Bỏ qua bản ghi đã bị xóa đồng thời.
        }
    }

    return e.json(200, { unregistered: removed });
}, $apis.requireAuth());

routerAdd("POST", "/api/custom/push/enqueue", (e) => {
    const expectedSecret = ($os.getenv("PUSH_ENQUEUE_SECRET") || "").trim();
    if (expectedSecret.length < 16) {
        return e.json(503, {
            code: 503,
            message: "Server chưa cấu hình PUSH_ENQUEUE_SECRET (tối thiểu 16 ký tự)."
        });
    }
    const providedSecret = String(e.request.header.get("X-Push-Admin-Secret") || "").trim();
    if (providedSecret !== expectedSecret) {
        return e.json(401, { code: 401, message: "Không có quyền đưa thông báo vào hàng đợi." });
    }

    const allowed = pushCore.checkRateLimit($app.store(), "f20a_push_enqueue", {
        windowMs: ENQUEUE_RATE_WINDOW_MS,
        max: ENQUEUE_RATE_MAX
    });
    if (!allowed) {
        return e.json(429, { code: 429, message: "Đưa thông báo vào hàng đợi quá nhanh." });
    }

    const body = e.requestInfo().body || {};
    const userId = String(body.userId || "").trim();
    const type = pushCore.normalizeType(body.type);
    if (!userId || !type) {
        throw new BadRequestError("Thiếu userId hoặc type không hợp lệ.");
    }
    const contentId = pushCore.normalizeContentId(body.contentId);
    if (body.contentId && contentId === null) {
        throw new BadRequestError("contentId không hợp lệ (chỉ chữ, số, _ . -; tối đa 128 ký tự).");
    }

    try {
        $app.findRecordById("users", userId);
    } catch (_) {
        return e.json(404, { code: 404, message: "Không tìm thấy người dùng." });
    }

    let devices = [];
    try {
        devices = $app.findRecordsByFilter(
            "device_tokens",
            "userId = {:uid}",
            "",
            200,
            0,
            { uid: userId }
        );
    } catch (_) {
        devices = [];
    }

    const now = Date.now();
    const rawScheduledAt = Number(body.scheduledAt || 0);
    const scheduledAt = rawScheduledAt > now ? Math.floor(rawScheduledAt) : now;
    const baseDedupe = String(body.dedupeKey || "").trim();

    let enqueued = 0;
    for (let i = 0; i < devices.length; i++) {
        const device = devices[i];
        const token = device.getString("token");
        const dedupeKey = baseDedupe
            ? (baseDedupe + ":" + token.slice(-12))
            : pushCore.buildDedupeKey(userId, type, contentId, token);

        try {
            const collection = $app.findCollectionByNameOrId("push_outbox");
            const record = new Record(collection);
            record.set("userId", userId);
            record.set("deviceTokenId", device.id);
            record.set("token", token);
            record.set("type", type);
            record.set("contentId", contentId || "");
            record.set("status", "pending");
            record.set("attempts", 0);
            record.set("scheduledAt", scheduledAt);
            record.set("dedupeKey", dedupeKey);
            $app.save(record);
            enqueued++;
        } catch (_) {
            // Trùng dedupeKey hoặc lỗi tạm thời: bỏ qua, không chặn các thiết bị khác.
        }
    }

    return e.json(200, { enqueued: enqueued, devices: devices.length });
});
