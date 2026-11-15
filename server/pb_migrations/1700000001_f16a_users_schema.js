/// <reference path="../pb_data/types.d.ts" />

/**
 * F16a Migration: Mở rộng auth collection `users` cho Học Tốt THCS
 * - Biệt danh (displayName)
 * - Năm sinh & nhóm tuổi dưới 16 (birthYear, isMinorUnder16)
 * - Đồng ý của phụ huynh (parentalConsentGranted, parentConsentAt, parentEmail)
 * - Nhà cung cấp xác thực (authProvider: EMAIL | GOOGLE)
 * - Lớp & bộ sách mặc định (selectedGrade, defaultSeries)
 * - API Rules nghiêm ngặt: người dùng chỉ đọc/sửa/xóa bản ghi của chính mình.
 */
migrate((app) => {
    const users = app.findCollectionByNameOrId("users");

    const addIfMissing = (field) => {
        if (!users.fields.getByName(field.name)) {
            users.fields.add(field);
        }
    };

    addIfMissing(new TextField({
        name: "displayName",
        required: false,
        min: 2,
        max: 50
    }));

    addIfMissing(new NumberField({
        name: "birthYear",
        required: false,
        min: 1940,
        max: 2026,
        onlyInt: true
    }));

    addIfMissing(new BoolField({
        name: "isMinorUnder16",
        required: false
    }));

    addIfMissing(new BoolField({
        name: "parentalConsentGranted",
        required: false
    }));

    addIfMissing(new DateField({
        name: "parentConsentAt",
        required: false
    }));

    addIfMissing(new EmailField({
        name: "parentEmail",
        required: false
    }));

    addIfMissing(new TextField({
        name: "authProvider",
        required: false,
        max: 20
    }));

    addIfMissing(new NumberField({
        name: "selectedGrade",
        required: false,
        min: 6,
        max: 9,
        onlyInt: true
    }));

    addIfMissing(new TextField({
        name: "defaultSeries",
        required: false,
        max: 16
    }));

    // API Rules bảo vệ dữ liệu tối đa: chỉ chính chủ được xem/sửa/xóa tài khoản của mình
    users.listRule = "id = @request.auth.id";
    users.viewRule = "id = @request.auth.id";
    users.createRule = "";
    users.updateRule = "id = @request.auth.id";
    users.deleteRule = "id = @request.auth.id";

    app.save(users);
}, (app) => {
    const users = app.findCollectionByNameOrId("users");
    const customNames = [
        "displayName",
        "birthYear",
        "isMinorUnder16",
        "parentalConsentGranted",
        "parentConsentAt",
        "parentEmail",
        "authProvider",
        "selectedGrade",
        "defaultSeries"
    ];
    for (const name of customNames) {
        users.fields.removeByName(name);
    }
    app.save(users);
});
