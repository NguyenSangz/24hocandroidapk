/// <reference path="../pb_data/types.d.ts" />

/**
 * F16a PocketBase Hooks:
 * 1. POST /api/custom/google-signin: Xác minh Google ID Token từ Jetpack Credential Manager -> cấp phiên PocketBase.
 * 2. Phụ huynh xác nhận qua liên kết dùng một lần gửi tới email đã cung cấp.
 * 3. DELETE /api/custom/account (và POST /api/custom/account/delete): Xóa tài khoản & toàn bộ dữ liệu người dùng trong transaction.
 */

onRecordCreateRequest((e) => {
    const currentYear = new Date().getUTCFullYear();
    const birthYear = Number(e.record.get("birthYear") || 0);
    const isMinor = !birthYear || birthYear < 1940 || birthYear > currentYear || currentYear - birthYear < 16;
    e.record.set("isMinorUnder16", isMinor);
    e.record.set("parentalConsentGranted", !isMinor);
    e.record.set("parentEmail", "");
    e.record.set("parentConsentAt", "");
    e.next();
}, "users");

function allowParentalConsentEmail(userId) {
    const key = "parent_consent_rate_" + userId;
    const now = Date.now();
    const cutoff = now - 60 * 60 * 1000;
    const store = $app.store();
    const entries = store.get(key);
    const recent = Array.isArray(entries) ? entries.filter((timestamp) => timestamp > cutoff) : [];
    if (recent.length >= 3) {
        store.set(key, recent);
        return false;
    }
    recent.push(now);
    store.set(key, recent);
    return true;
}

function cleanupExpiredParentalConsentChallenges() {
    const expired = $app.findRecordsByFilter(
        "parental_consent_challenges",
        "expiresAt <= {:now}",
        "",
        100,
        0,
        { now: new Date().toISOString() }
    );
    for (let i = 0; i < expired.length; i++) {
        $app.delete(expired[i]);
    }
}

cronAdd("parental_consent_challenge_cleanup", "*/30 * * * *", () => {
    cleanupExpiredParentalConsentChallenges();
});

routerAdd("POST", "/api/custom/google-signin", (e) => {
    const body = e.requestInfo().body || {};
    const idToken = String(body.idToken || "").trim();
    if (!idToken) {
        throw new BadRequestError("Thiếu Google ID Token.");
    }

    const expectedClientId = ($os.getenv("GOOGLE_WEB_CLIENT_ID") || "").trim();
    if (!expectedClientId || expectedClientId.indexOf("TÔI_CẦN_ĐIỀN") !== -1) {
        throw new InternalServerError("Server chưa cấu hình GOOGLE_WEB_CLIENT_ID trong /etc/pocketbase/pocketbase.env.");
    }

    // Xác minh Google ID Token trực tiếp qua endpoint tokeninfo chính thức của Google
    const verifyRes = $http.send({
        url: "https://oauth2.googleapis.com/tokeninfo?id_token=" + encodeURIComponent(idToken),
        method: "GET",
        timeout: 10
    });

    if (verifyRes.statusCode !== 200 || !verifyRes.json) {
        throw new UnauthorizedError("Google ID Token không hợp lệ hoặc đã hết hạn.");
    }

    const tokenInfo = verifyRes.json;
    const aud = String(tokenInfo.aud || "");
    const iss = String(tokenInfo.iss || "");
    const email = String(tokenInfo.email || "").trim().toLowerCase();
    const emailVerified = tokenInfo.email_verified === true || String(tokenInfo.email_verified) === "true";
    const expSec = Number(tokenInfo.exp || 0);
    const nowSec = Math.floor(Date.now() / 1000);

    if (aud !== expectedClientId) {
        throw new UnauthorizedError("Audience của Google ID Token không khớp GOOGLE_WEB_CLIENT_ID.");
    }
    if (iss !== "accounts.google.com" && iss !== "https://accounts.google.com") {
        throw new UnauthorizedError("Issuer của Google ID Token không hợp lệ.");
    }
    if (!email || !emailVerified) {
        throw new UnauthorizedError("Tài khoản Google chưa xác minh email.");
    }
    if (!expSec || expSec <= nowSec) {
        throw new UnauthorizedError("Google ID Token đã hết hạn.");
    }

    const usersCol = $app.findCollectionByNameOrId("users");
    let record = null;
    try {
        record = $app.findAuthRecordByEmail("users", email);
    } catch (_) {
        record = null;
    }

    const currentYear = new Date().getUTCFullYear();
    const rawBirthYear = body.birthYear !== undefined && body.birthYear !== null ? Number(body.birthYear) : null;
    const validBirthYear = (rawBirthYear && rawBirthYear >= 1940 && rawBirthYear <= currentYear) ? Math.floor(rawBirthYear) : null;
    const requestedName = String(body.displayName || tokenInfo.name || "Học sinh THCS").trim().slice(0, 50);
    const safeName = requestedName.length >= 2 ? requestedName : "Học sinh THCS";
    const safeGrade = Math.min(9, Math.max(6, Number(body.selectedGrade || 6)));
    const allowedSeries = ["kntt", "ctst", "cd", "chung"];
    const safeSeries = allowedSeries.indexOf(String(body.defaultSeries || "")) !== -1 ? String(body.defaultSeries) : "kntt";

    if (!record) {
        record = new Record(usersCol);
        record.set("email", email);
        record.set("emailVisibility", false);
        record.set("verified", true);
        // Không lưu hay dùng mật khẩu người dùng cho tài khoản Google; đặt mật khẩu ngẫu nhiên entropy cao
        record.setPassword($security.randomString(48));
        record.set("displayName", safeName);
        record.set("authProvider", "GOOGLE");
        record.set("selectedGrade", safeGrade);
        record.set("defaultSeries", safeSeries);

        if (validBirthYear !== null) {
            const isMinor = (currentYear - validBirthYear) < 16;
            record.set("birthYear", validBirthYear);
            record.set("isMinorUnder16", isMinor);
            record.set("parentalConsentGranted", !isMinor);
        } else {
            record.set("isMinorUnder16", true);
            record.set("parentalConsentGranted", false);
        }
        $app.save(record);
    } else {
        // Giữ nguyên dữ liệu hiện có, chỉ bổ sung nếu còn thiếu
        if (!record.getString("displayName")) {
            record.set("displayName", safeName);
        }
        if (!record.getString("authProvider")) {
            record.set("authProvider", "GOOGLE");
        }
        if (validBirthYear !== null && !record.getInt("birthYear")) {
            const isMinor = (currentYear - validBirthYear) < 16;
            record.set("birthYear", validBirthYear);
            record.set("isMinorUnder16", isMinor);
            if (!isMinor) {
                record.set("parentalConsentGranted", true);
            }
        }
        record.set("verified", true);
        $app.save(record);
    }

    return $apis.recordAuthResponse(e, record, "oauth2", null);
});

routerAdd("POST", "/api/custom/parental-consent/request", (e) => {
    const authRecord = e.auth;
    if (!authRecord) {
        throw new UnauthorizedError("Yêu cầu đăng nhập.");
    }

    const body = e.requestInfo().body || {};
    const currentYear = new Date().getUTCFullYear();
    const birthYear = Math.floor(Number(body.birthYear || 0));
    if (!birthYear || birthYear < 1940 || birthYear > currentYear) {
        throw new BadRequestError("Năm sinh không hợp lệ.");
    }

    const isMinor = (currentYear - birthYear) < 16;
    const storedBirthYear = authRecord.getInt("birthYear");
    const previouslyMinor = authRecord.getBool("isMinorUnder16") || !storedBirthYear;
    if (storedBirthYear && storedBirthYear !== birthYear) {
        throw new BadRequestError("Năm sinh đã lưu không thể tự thay đổi trong bước xác nhận.");
    }
    const parentEmail = String(body.parentEmail || "").trim().toLowerCase();
    const emailPattern = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
    const verificationRequired = isMinor || previouslyMinor;

    if (verificationRequired && (!parentEmail || parentEmail.length > 120 || !emailPattern.test(parentEmail))) {
        throw new BadRequestError("Học sinh dưới 16 tuổi cần email phụ huynh/người giám hộ hợp lệ.");
    }

    if (!verificationRequired) {
        authRecord.set("birthYear", birthYear);
        authRecord.set("isMinorUnder16", false);
        authRecord.set("parentalConsentGranted", true);
        authRecord.set("parentEmail", "");
        authRecord.set("parentConsentAt", "");
        $app.save(authRecord);
        return e.json(200, { verificationRequired: false, isMinorUnder16: false });
    }

    if (!allowParentalConsentEmail(authRecord.id)) {
        return e.json(429, {
            code: 429,
            message: "Bạn đã yêu cầu nhiều email xác nhận. Vui lòng thử lại sau một giờ."
        });
    }

    const previousChallenges = $app.findRecordsByFilter(
        "parental_consent_challenges",
        "userId = {:uid}",
        "",
        0,
        0,
        { uid: authRecord.id }
    );
    for (let i = 0; i < previousChallenges.length; i++) {
        $app.delete(previousChallenges[i]);
    }

    const publicUrl = ($os.getenv("PB_PUBLIC_URL") || "").trim().replace(/\/+$/, "");
    if (!/^https:\/\/[^/]+$/i.test(publicUrl)) {
        throw new InternalServerError("Server chưa cấu hình PB_PUBLIC_URL HTTPS.");
    }
    const token = $security.randomString(64);
    const expiresAt = new Date(Date.now() + 60 * 60 * 1000).toISOString();
    const challenge = new Record($app.findCollectionByNameOrId("parental_consent_challenges"));
    challenge.set("userId", authRecord.id);
    challenge.set("token", token);
    challenge.set("birthYear", birthYear);
    challenge.set("parentEmail", parentEmail);
    challenge.set("expiresAt", expiresAt);
    $app.save(challenge);

    try {
        const senderAddress = String($app.settings().meta.senderAddress || "").trim();
        const senderName = String($app.settings().meta.senderName || "Học Tốt THCS").trim();
        if (!senderAddress) {
            throw new Error("SMTP sender address is not configured");
        }
        const confirmUrl = publicUrl + "/parental-consent.html#token=" + encodeURIComponent(token);
        $app.newMailClient().send(new MailerMessage({
            from: { address: senderAddress, name: senderName },
            to: [{ address: parentEmail }],
            subject: "Xác nhận của phụ huynh – Học Tốt THCS",
            html: "<p>Một tài khoản học tập Học Tốt THCS đề nghị xác nhận của phụ huynh để bật đồng bộ trực tuyến.</p>" +
                "<p>Chỉ tiếp tục nếu bạn là cha mẹ/người giám hộ và đồng ý sau khi đọc <a href=\"" +
                publicUrl + "/privacy.html\">Chính sách quyền riêng tư</a>.</p>" +
                "<p><a href=\"" + confirmUrl + "\">Đọc thông tin và xác nhận</a></p>" +
                "<p>Liên kết hết hạn sau một giờ. Nếu bạn không yêu cầu, hãy bỏ qua email này.</p>"
        }));
    } catch (_) {
        $app.delete(challenge);
        throw new InternalServerError("Không gửi được email xác nhận. Vui lòng thử lại sau.");
    }

    return e.json(200, {
        verificationRequired: true,
        isMinorUnder16: true,
        expiresAt: expiresAt
    });
}, $apis.requireAuth());

routerAdd("POST", "/api/custom/parental-consent/confirm", (e) => {
    const body = e.requestInfo().body || {};
    const token = String(body.token || "").trim();
    if (!token || token.length > 128) {
        throw new BadRequestError("Liên kết xác nhận không hợp lệ.");
    }

    let challenge = null;
    try {
        challenge = $app.findFirstRecordByData("parental_consent_challenges", "token", token);
    } catch (_) {
        challenge = null;
    }
    if (!challenge) {
        throw new BadRequestError("Liên kết xác nhận không hợp lệ hoặc đã được sử dụng.");
    }

    $app.runInTransaction((txApp) => {
        const freshChallenge = txApp.findRecordById("parental_consent_challenges", challenge.id);
        const expiry = Date.parse(freshChallenge.getString("expiresAt"));
        if (!expiry || expiry <= Date.now()) {
            throw new BadRequestError("Liên kết xác nhận đã hết hạn hoặc đã được sử dụng.");
        }

        const user = txApp.findRecordById("users", freshChallenge.getString("userId"));
        const birthYear = freshChallenge.getInt("birthYear");
        const currentYear = new Date().getUTCFullYear();
        if (birthYear < 1940 || birthYear > currentYear) {
            throw new BadRequestError("Năm sinh trong yêu cầu không còn hợp lệ.");
        }

        const isMinor = currentYear - birthYear < 16;
        user.set("birthYear", birthYear);
        user.set("isMinorUnder16", isMinor);
        user.set("parentalConsentGranted", true);
        user.set("parentEmail", isMinor ? freshChallenge.getString("parentEmail") : "");
        user.set("parentConsentAt", isMinor ? new Date().toISOString() : "");
        txApp.save(user);
        txApp.delete(freshChallenge);
    });

    return e.json(200, { confirmed: true });
});

// Không cho phép client cũ đánh dấu đã có đồng ý chỉ bằng cách gửi email.
routerAdd("POST", "/api/custom/parental-consent", () => {
    throw new BadRequestError("Cần cập nhật ứng dụng để xác nhận qua email phụ huynh.");
}, $apis.requireAuth());

function handleDeleteAccount(e) {
    const authRecord = e.auth;
    if (!authRecord) {
        throw new UnauthorizedError("Yêu cầu đăng nhập.");
    }

    const userId = authRecord.id;
    // Các collection đồng bộ ở F16b (sẽ xóa sạch nếu đã tồn tại)
    // F20a: device_tokens + push_outbox cũng bị xóa để không còn token thiết bị
    // sau khi xóa tài khoản (ngoài cascadeDelete của relation userId).
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
        "sync_irregular_verbs",
        "push_outbox",
        "device_tokens",
        "parental_consent_challenges"
    ];

    $app.runInTransaction((txApp) => {
        for (const colName of syncCollections) {
            let col = null;
            try {
                col = txApp.findCollectionByNameOrId(colName);
            } catch (_) {
                col = null;
            }
            if (!col) continue;

            const userRecords = txApp.findRecordsByFilter(colName, "userId = {:uid}", "", 0, 0, { uid: userId });
            for (const rec of userRecords) {
                txApp.delete(rec);
            }
        }

        const freshUser = txApp.findRecordById("users", userId);
        txApp.delete(freshUser);
    });

    return e.json(200, {
        deleted: true,
        deletedAt: new Date().toISOString()
    });
}

routerAdd("DELETE", "/api/custom/account", handleDeleteAccount, $apis.requireAuth());
routerAdd("POST", "/api/custom/account/delete", handleDeleteAccount, $apis.requireAuth());
