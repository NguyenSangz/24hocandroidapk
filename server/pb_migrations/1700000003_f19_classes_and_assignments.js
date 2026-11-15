/// <reference path="../pb_data/types.d.ts" />
migrate((db) => {
    // 1. Collection classes (Lớp học)
    const classesCollection = new Collection({
        name: "classes",
        type: "base",
        system: false,
        schema: [
            { name: "name", type: "text", required: true, options: { min: 2, max: 100 } },
            { name: "teacher_name", type: "text", required: true, options: { max: 100 } },
            { name: "join_code", type: "text", required: true, options: { min: 4, max: 20 } }
        ],
        indexes: ["CREATE UNIQUE INDEX idx_join_code ON classes (join_code)"],
        listRule: "@request.auth.id != ''",
        viewRule: "@request.auth.id != ''",
        createRule: "@request.auth.id != ''",
        updateRule: "@request.auth.id != ''",
        deleteRule: "@request.auth.id != ''"
    });
    Dao(db).saveCollection(classesCollection);

    // 2. Collection class_members (Học sinh tham gia lớp)
    const membersCollection = new Collection({
        name: "class_members",
        type: "base",
        system: false,
        schema: [
            { name: "class_id", type: "relation", required: true, options: { collectionId: classesCollection.id, cascadeDelete: true, maxSelect: 1, minSelect: 1 } },
            { name: "user_id", type: "relation", required: true, options: { collectionId: "_pb_users_auth_", cascadeDelete: true, maxSelect: 1, minSelect: 1 } }
        ],
        indexes: ["CREATE UNIQUE INDEX idx_class_user ON class_members (class_id, user_id)"],
        listRule: "@request.auth.id != ''",
        viewRule: "@request.auth.id != ''",
        createRule: "@request.auth.id != ''",
        updateRule: null,
        deleteRule: "@request.auth.id != ''"
    });
    Dao(db).saveCollection(membersCollection);

    // 3. Collection assignments (Bài tập giáo viên giao)
    const assignmentsCollection = new Collection({
        name: "assignments",
        type: "base",
        system: false,
        schema: [
            { name: "class_id", type: "relation", required: true, options: { collectionId: classesCollection.id, cascadeDelete: true, maxSelect: 1, minSelect: 1 } },
            { name: "title", type: "text", required: true, options: { min: 3, max: 150 } },
            { name: "description", type: "text", required: false, options: { max: 1000 } },
            { name: "due_date", type: "text", required: false },
            { name: "test_id", type: "text", required: false }
        ],
        indexes: ["CREATE INDEX idx_class_assignment ON assignments (class_id)"],
        listRule: "@request.auth.id != ''",
        viewRule: "@request.auth.id != ''",
        createRule: "@request.auth.id != ''",
        updateRule: "@request.auth.id != ''",
        deleteRule: "@request.auth.id != ''"
    });
    Dao(db).saveCollection(assignmentsCollection);
}, (db) => {
    try { Dao(db).deleteCollectionByName("assignments"); } catch (_) {}
    try { Dao(db).deleteCollectionByName("class_members"); } catch (_) {}
    try { Dao(db).deleteCollectionByName("classes"); } catch (_) {}
});
