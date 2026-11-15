/// <reference path="../pb_data/types.d.ts" />

/**
 * Lưu liên kết xác nhận phụ huynh dùng một lần; collection không truy cập trực tiếp qua API.
 */
migrate((app) => {
    const users = app.findCollectionByNameOrId("users");
    const currentYear = new Date().getUTCFullYear();
    const existingUsers = app.findRecordsByFilter("users", "", "", 0, 0);
    for (let i = 0; i < existingUsers.length; i++) {
        const user = existingUsers[i];
        const birthYear = user.getInt("birthYear");
        const isMinor = !birthYear || currentYear - birthYear < 16;
        user.set("isMinorUnder16", isMinor);
        user.set("parentalConsentGranted", !isMinor);
        user.set("parentEmail", "");
        user.set("parentConsentAt", "");
        app.save(user);
    }

    const challenges = new Collection({
        name: "parental_consent_challenges",
        type: "base",
        listRule: null,
        viewRule: null,
        createRule: null,
        updateRule: null,
        deleteRule: null,
        fields: [
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
                min: 32,
                max: 128,
                hidden: true
            }),
            new NumberField({
                name: "birthYear",
                required: true,
                min: 1940,
                max: currentYear,
                onlyInt: true
            }),
            new EmailField({
                name: "parentEmail",
                required: true
            }),
            new DateField({
                name: "expiresAt",
                required: true
            })
        ],
        indexes: [
            "CREATE UNIQUE INDEX idx_parental_consent_token ON parental_consent_challenges (token)",
            "CREATE INDEX idx_parental_consent_user_created ON parental_consent_challenges (userId, created)"
        ]
    });
    app.save(challenges);

    const usersCollection = app.findCollectionByNameOrId("users");
    usersCollection.createRule =
        "@request.body.parentalConsentGranted = false && " +
        "@request.body.parentEmail:isset = false && " +
        "@request.body.parentConsentAt:isset = false";
    usersCollection.updateRule =
        "id = @request.auth.id && " +
        "@request.body.birthYear:isset = false && " +
        "@request.body.isMinorUnder16:isset = false && " +
        "@request.body.parentalConsentGranted:isset = false && " +
        "@request.body.parentEmail:isset = false && " +
        "@request.body.parentConsentAt:isset = false";
    app.save(usersCollection);
}, (app) => {
    try {
        app.delete(app.findCollectionByNameOrId("parental_consent_challenges"));
    } catch (_) {
        // Collection may not have been created.
    }
});
