/// <reference path="../pb_data/types.d.ts" />

/**
 * Custom API Endpoint để học sinh tham gia lớp học bằng mã code (join_code).
 * POST /api/custom/join-class
 * Body: { "join_code": "ABC123" }
 */
routerAdd("POST", "/api/custom/join-class", (c) => {
    const authRecord = c.get("authRecord");
    if (!authRecord) {
        return c.json(401, { error: "Yêu cầu đăng nhập tài khoản để tham gia lớp học." });
    }

    const data = $apis.readData(c);
    const joinCode = (data.join_code || "").trim().toUpperCase();
    if (!joinCode) {
        return c.json(400, { error: "Mã lớp học không được để trống." });
    }

    try {
        const classRecord = $app.dao().findFirstRecordByData("classes", "join_code", joinCode);
        if (!classRecord) {
            return c.json(404, { error: "Không tìm thấy lớp học với mã code này." });
        }

        const classId = classRecord.getId();
        const userId = authRecord.getId();

        // Kiểm tra xem đã tham gia chưa
        let existingMember = null;
        try {
            existingMember = $app.dao().findFirstRecordByFilter("class_members", "class_id = {:classId} && user_id = {:userId}", {
                classId: classId,
                userId: userId
            });
        } catch (_) {}

        if (existingMember) {
            return c.json(200, {
                message: "Bạn đã là thành viên của lớp học này rồi.",
                class_id: classId,
                class_name: classRecord.get("name")
            });
        }

        // Tạo bản ghi thành viên mới
        const memberCollection = $app.dao().findCollectionByNameOrId("class_members");
        const record = new Record(memberCollection);
        record.set("class_id", classId);
        record.set("user_id", userId);
        $app.dao().saveRecord(record);

        return c.json(200, {
            message: "Tham gia lớp học thành công!",
            class_id: classId,
            class_name: classRecord.get("name"),
            teacher_name: classRecord.get("teacher_name")
        });
    } catch (err) {
        return c.json(500, { error: "Lỗi hệ thống khi tham gia lớp: " + err.message });
    }
});
