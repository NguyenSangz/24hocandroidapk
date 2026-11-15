/// <reference path="../pb_data/types.d.ts" />

/**
 * F20a Migration: Hạ tầng thông báo đẩy tối thiểu.
 *
 * 1. `device_tokens` – mỗi dòng là một token FCM gắn với MỘT tài khoản.
 *    - Unique index theo `token` để đăng ký lặp lại là idempotent.
 *    - `updatedAt` do server đặt (autodate) làm mốc lần cuối nhìn thấy thiết bị.
 *    - API Rules: người dùng chỉ đọc/xóa token của chính mình; việc tạo/cập nhật
 *      chỉ qua endpoint `/api/custom/push/register` (kiểm tra quyền + rate limit).
 *
 * 2. `push_outbox` – hàng đợi gửi do worker Node đọc.
 *    - Chỉ server/worker truy cập (API Rules = null), app không đọc/ghi trực tiếp.
 *    - `dedupeKey` unique để chống gửi trùng cùng một sự kiện cho cùng thiết bị.
 *    - `token` lưu bản sao để worker không phải join; khi token chết thì xóa
 *      bản ghi `device_tokens` tương ứng.
 */
migrate((app) => {
    const users = app.findCollectionByNameOrId("users");

    // ---------- 1. device_tokens ----------
    let deviceTokens = null;
    try {
        deviceTokens = app.findCollectionByNameOrId("device_tokens");
    } catch (_) {
        deviceTokens = null;
    }
    if (!deviceTokens) {
        const deviceTokenFields = [
            new RelationField({
                name: "userId",
                collectionId: users.id,
                cascadeDelete: true,
                maxSelect: 1,
                required: true
            }),
            new TextField({
                name: "token",
                required: true,
                min: 20,
                max: 4096
            }),
            new TextField({
                name: "platform",
                required: true,
                min: 2,
                max: 20
            }),
            new AutodateField({
                name: "createdAt",
                onCreate: true,
                onUpdate: false
            }),
            new AutodateField({
                name: "updatedAt",
                onCreate: true,
                onUpdate: true
            })
        ];

        const deviceTokenIndexes = [
            "CREATE UNIQUE INDEX idx_device_tokens_token ON device_tokens (token)",
            "CREATE INDEX idx_device_tokens_user ON device_tokens (userId)"
        ];

        const collection = new Collection({
            name: "device_tokens",
            type: "base",
            listRule: "@request.auth.id != '' && userId = @request.auth.id",
            viewRule: "@request.auth.id != '' && userId = @request.auth.id",
            // Tạo/sửa chỉ qua custom endpoint (có rate limit + kiểm tra token).
            createRule: null,
            updateRule: null,
            deleteRule: "@request.auth.id != '' && userId = @request.auth.id",
            fields: deviceTokenFields,
            indexes: deviceTokenIndexes
        });
        app.save(collection);
        deviceTokens = collection;
    }

    // ---------- 2. push_outbox ----------
    let outbox = null;
    try {
        outbox = app.findCollectionByNameOrId("push_outbox");
    } catch (_) {
        outbox = null;
    }
    if (!outbox) {
        const outboxFields = [
            new RelationField({
                name: "userId",
                collectionId: users.id,
                cascadeDelete: true,
                maxSelect: 1,
                required: false
            }),
            new RelationField({
                name: "deviceTokenId",
                collectionId: deviceTokens.id,
                cascadeDelete: true,
                maxSelect: 1,
                required: true
            }),
            new TextField({
                name: "token",
                required: true,
                min: 20,
                max: 4096
            }),
            new TextField({
                name: "type",
                required: true,
                min: 2,
                max: 40
            }),
            new TextField({
                name: "contentId",
                required: false,
                max: 128
            }),
            new TextField({
                name: "status",
                required: true,
                min: 6,
                max: 20
            }),
            new NumberField({
                name: "attempts",
                required: false,
                min: 0,
                onlyInt: true
            }),
            new NumberField({
                name: "scheduledAt",
                required: false,
                min: 0,
                onlyInt: true
            }),
            new NumberField({
                name: "sentAt",
                required: false,
                min: 0,
                onlyInt: true
            }),
            new TextField({
                name: "lastError",
                required: false,
                max: 500
            }),
            new TextField({
                name: "dedupeKey",
                required: false,
                max: 200
            }),
            new AutodateField({
                name: "createdAt",
                onCreate: true,
                onUpdate: false
            }),
            new AutodateField({
                name: "updatedAt",
                onCreate: true,
                onUpdate: true
            })
        ];

        const outboxIndexes = [
            "CREATE INDEX idx_push_outbox_status ON push_outbox (status, scheduledAt)",
            "CREATE INDEX idx_push_outbox_device ON push_outbox (deviceTokenId)",
            "CREATE UNIQUE INDEX idx_push_outbox_dedupe ON push_outbox (dedupeKey)"
        ];

        const collection = new Collection({
            name: "push_outbox",
            type: "base",
            // Chỉ server / worker (admin) được đọc-ghi hàng đợi.
            listRule: null,
            viewRule: null,
            createRule: null,
            updateRule: null,
            deleteRule: null,
            fields: outboxFields,
            indexes: outboxIndexes
        });
        app.save(collection);
    }
}, (app) => {
    for (const colName of ["push_outbox", "device_tokens"]) {
        try {
            const col = app.findCollectionByNameOrId(colName);
            if (col) app.delete(col);
        } catch (_) {
            // Bỏ qua nếu chưa tồn tại
        }
    }
});
