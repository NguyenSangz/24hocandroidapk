/// <reference path="../pb_data/types.d.ts" />

/**
 * F16b Migration: Tạo 10 collection đồng bộ Offline-First cho Học Tốt THCS:
 * - sync_lesson_progress
 * - sync_attempts
 * - sync_tests
 * - sync_wrong_questions
 * - sync_flashcards
 * - sync_xp_events (thêm sourceType, sourceId + unique index (userId, sourceType, sourceId))
 * - sync_badges
 * - sync_settings
 * - sync_vocab_packs
 * - sync_irregular_verbs
 *
 * Bảo mật & Hiệu năng:
 * - Mọi collection đều có `userId` (Relation -> users, cascadeDelete: true), `entityKey`,
 *   `payload` (JSONField giới hạn maxSize 32KB), `clientUpdatedAt`, `deleted` (soft-delete),
 *   `created` (autodate), `updated` (autodate server làm con trỏ delta).
 * - Unique index (userId, entityKey) trên tất cả 10 collection.
 * - API Rules: người dùng chỉ đọc/ghi/sửa/xóa bản ghi của chính mình.
 */
migrate((app) => {
    const users = app.findCollectionByNameOrId("users");

    const syncCollections = [
        "sync_lesson_progress",
        "sync_attempts",
        "sync_tests",
        "sync_wrong_questions",
        "sync_flashcards",
        "sync_xp_events",
        "sync_badges",
        "sync_settings",
        "sync_vocab_packs",
        "sync_irregular_verbs"
    ];

    for (const colName of syncCollections) {
        let existing = null;
        try {
            existing = app.findCollectionByNameOrId(colName);
        } catch (_) {
            existing = null;
        }
        if (existing) {
            continue;
        }

        const fields = [
            new RelationField({
                name: "userId",
                collectionId: users.id,
                cascadeDelete: true,
                maxSelect: 1,
                required: true
            }),
            new TextField({
                name: "entityKey",
                required: true,
                min: 1,
                max: 128
            }),
            new JSONField({
                name: "payload",
                required: false,
                maxSize: 32768
            }),
            new NumberField({
                name: "clientUpdatedAt",
                required: false,
                min: 0,
                onlyInt: true
            }),
            new BoolField({
                name: "deleted",
                required: false
            }),
            new AutodateField({
                name: "created",
                onCreate: true,
                onUpdate: false
            }),
            new AutodateField({
                name: "updated",
                onCreate: true,
                onUpdate: true
            })
        ];

        const indexes = [
            `CREATE UNIQUE INDEX idx_${colName}_user_key ON ${colName} (userId, entityKey)`,
            `CREATE INDEX idx_${colName}_user_updated ON ${colName} (userId, updated)`
        ];

        if (colName === "sync_xp_events") {
            fields.push(
                new TextField({
                    name: "sourceType",
                    required: true,
                    min: 1,
                    max: 40
                }),
                new TextField({
                    name: "sourceId",
                    required: true,
                    min: 1,
                    max: 128
                })
            );
            indexes.push(
                "CREATE UNIQUE INDEX idx_sync_xp_events_user_source ON sync_xp_events (userId, sourceType, sourceId)"
            );
        }

        const collection = new Collection({
            name: colName,
            type: "base",
            listRule: "@request.auth.id != '' && userId = @request.auth.id",
            viewRule: "@request.auth.id != '' && userId = @request.auth.id",
            createRule: "@request.auth.id != '' && @request.body.userId = @request.auth.id",
            updateRule: "@request.auth.id != '' && userId = @request.auth.id && (@request.body.userId:isset = false || @request.body.userId = @request.auth.id)",
            deleteRule: "@request.auth.id != '' && userId = @request.auth.id",
            fields: fields,
            indexes: indexes
        });

        app.save(collection);
    }
}, (app) => {
    const syncCollections = [
        "sync_lesson_progress",
        "sync_attempts",
        "sync_tests",
        "sync_wrong_questions",
        "sync_flashcards",
        "sync_xp_events",
        "sync_badges",
        "sync_settings",
        "sync_vocab_packs",
        "sync_irregular_verbs"
    ];

    for (const colName of syncCollections) {
        try {
            const col = app.findCollectionByNameOrId(colName);
            if (col) {
                app.delete(col);
            }
        } catch (_) {
            // Bỏ qua nếu chưa tồn tại
        }
    }
});
