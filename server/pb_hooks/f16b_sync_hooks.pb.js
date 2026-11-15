/// <reference path="../pb_data/types.d.ts" />

/**
 * F16b PocketBase Sync Hooks:
 * - POST /api/custom/sync/push:
 *     + Kiểm tra đăng nhập & giới hạn tần suất (rate limit: 60 lượt / 5 phút / userId).
 *     + Giới hạn kích thước: tối đa 50 mục/lô, payload <= 16 KB/mục.
 *     + Upsert idempotent theo (userId, entityKey) và chống cộng trùng XP theo (userId, sourceType, sourceId).
 *     + Giải quyết xung đột ngay trên server: bài học OR-completed, SM-2 ưu tiên bản có nhiều lượt ôn hơn,
 *       câu sai gộp max errorCount & trạng thái MASTERED, cài đặt LWW theo clientUpdatedAt.
 *     + Thời gian cập nhật `updated` do server đặt qua AutodateField làm con trỏ delta.
 *
 * - GET /api/custom/sync/pull?since=...&limit=...:
 *     + Kéo delta các bản ghi của chính userId có updated > since, phân trang ổn định, không rò rỉ user khác.
 */

const ALLOWED_SYNC_COLLECTIONS = [
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

const SYNC_RATE_WINDOW_MS = 5 * 60 * 1000;
const SYNC_RATE_MAX_REQUESTS = 60;
const MAX_BATCH_ITEMS = 50;
const MAX_PAYLOAD_BYTES = 16384;

function needsVerifiedParentalConsent(authRecord) {
    const birthYear = authRecord.getInt("birthYear");
    const currentYear = new Date().getUTCFullYear();
    const isMinor = !birthYear ||
        authRecord.getBool("isMinorUnder16") ||
        currentYear - birthYear < 16;
    return isMinor && !authRecord.getBool("parentalConsentGranted");
}

function enforceSyncRateLimit(userId) {
    const store = $app.store();
    const key = "f16b_rate_" + userId;
    const now = Date.now();
    let bucket = store.get(key);
    if (!Array.isArray(bucket)) {
        bucket = [];
    }
    const cutoff = now - SYNC_RATE_WINDOW_MS;
    const recent = [];
    for (let i = 0; i < bucket.length; i++) {
        if (bucket[i] > cutoff) {
            recent.push(bucket[i]);
        }
    }
    if (recent.length >= SYNC_RATE_MAX_REQUESTS) {
        store.set(key, recent);
        return false;
    }
    recent.push(now);
    store.set(key, recent);
    return true;
}

function parseJsonSafe(raw) {
    if (!raw) return {};
    if (typeof raw === "object") return raw;
    try {
        return JSON.parse(String(raw));
    } catch (_) {
        return {};
    }
}

function mergePayloadForCollection(colName, existingPayload, incomingPayload, existingClientTs, incomingClientTs) {
    if (colName === "sync_lesson_progress") {
        const mergedCompleted = Boolean(existingPayload.completed) || Boolean(incomingPayload.completed);
        return {
            lessonId: incomingPayload.lessonId || existingPayload.lessonId || "",
            completed: mergedCompleted,
            updatedAt: Math.max(Number(existingPayload.updatedAt || 0), Number(incomingPayload.updatedAt || 0))
        };
    }

    if (colName === "sync_flashcards") {
        const exReviews = Number(existingPayload.totalReviews || 0);
        const inReviews = Number(incomingPayload.totalReviews || 0);
        const exLast = Number(existingPayload.lastReviewTimestamp || 0);
        const inLast = Number(incomingPayload.lastReviewTimestamp || 0);
        if (inReviews > exReviews || (inReviews === exReviews && inLast >= exLast)) {
            return incomingPayload;
        }
        return existingPayload;
    }

    if (colName === "sync_wrong_questions") {
        const mergedErrorCount = Math.max(
            Number(existingPayload.errorCount || 1),
            Number(incomingPayload.errorCount || 1)
        );
        const exReview = Number(existingPayload.lastReviewTimestamp || 0);
        const inReview = Number(incomingPayload.lastReviewTimestamp || 0);
        const exWrong = Number(existingPayload.lastWrongTimestamp || 0);
        const inWrong = Number(incomingPayload.lastWrongTimestamp || 0);
        const winner = Math.max(inReview, inWrong) >= Math.max(exReview, exWrong)
            ? Object.assign({}, incomingPayload)
            : Object.assign({}, existingPayload);
        winner.errorCount = mergedErrorCount;
        if (existingPayload.status === "MASTERED" && exReview >= inWrong) {
            winner.status = "MASTERED";
        }
        return winner;
    }

    if (colName === "sync_badges") {
        const exUnlocked = Number(existingPayload.unlockedAt || 0);
        const inUnlocked = Number(incomingPayload.unlockedAt || 0);
        const earliest = (exUnlocked > 0 && inUnlocked > 0)
            ? Math.min(exUnlocked, inUnlocked)
            : (exUnlocked || inUnlocked);
        return Object.assign({}, existingPayload, incomingPayload, { unlockedAt: earliest });
    }

    if (colName === "sync_tests") {
        const exDraft = Boolean(existingPayload.isDraft);
        const inDraft = Boolean(incomingPayload.isDraft);
        // Bài đã nộp luôn thắng bản nháp
        if (!exDraft && inDraft) return existingPayload;
        if (exDraft && !inDraft) return incomingPayload;
        return incomingClientTs >= existingClientTs ? incomingPayload : existingPayload;
    }

    // sync_settings, sync_attempts, sync_vocab_packs, sync_irregular_verbs -> LWW theo clientUpdatedAt
    return incomingClientTs >= existingClientTs ? incomingPayload : existingPayload;
}

routerAdd("POST", "/api/custom/sync/push", (e) => {
    const authRecord = e.auth;
    if (!authRecord) {
        throw new UnauthorizedError("Yêu cầu đăng nhập để đồng bộ.");
    }
    if (needsVerifiedParentalConsent(authRecord)) {
        return e.json(403, {
            code: 403,
            message: "Chờ xác nhận của phụ huynh trước khi đồng bộ dữ liệu."
        });
    }

    const userId = authRecord.id;
    if (!enforceSyncRateLimit(userId)) {
        return e.json(429, {
            code: 429,
            message: "Bạn đang đồng bộ quá nhanh. Vui lòng thử lại sau ít phút."
        });
    }

    const body = e.requestInfo().body || {};
    const items = Array.isArray(body.items) ? body.items : [];
    if (items.length > MAX_BATCH_ITEMS) {
        throw new BadRequestError("Mỗi lô đồng bộ chỉ được chứa tối đa " + MAX_BATCH_ITEMS + " bản ghi.");
    }

    const results = [];

    $app.runInTransaction((txApp) => {
        for (let i = 0; i < items.length; i++) {
            const item = items[i] || {};
            const colName = String(item.collection || "").trim();
            const entityKey = String(item.entityKey || "").trim();
            const deleted = Boolean(item.deleted);
            const clientUpdatedAt = Math.max(0, Math.floor(Number(item.clientUpdatedAt || 0)));
            const payloadObj = (item.payload && typeof item.payload === "object") ? item.payload : {};

            if (ALLOWED_SYNC_COLLECTIONS.indexOf(colName) === -1) {
                throw new BadRequestError("Collection đồng bộ không hợp lệ: " + colName);
            }
            if (!entityKey || entityKey.length > 128) {
                throw new BadRequestError("entityKey không hợp lệ tại vị trí " + i);
            }

            const serialized = JSON.stringify(payloadObj);
            if (serialized.length > MAX_PAYLOAD_BYTES) {
                throw new BadRequestError("Bản ghi vượt quá giới hạn kích thước " + MAX_PAYLOAD_BYTES + " byte.");
            }

            const col = txApp.findCollectionByNameOrId(colName);

            if (colName === "sync_xp_events") {
                const sourceType = String(item.sourceType || payloadObj.eventType || "XP").trim().slice(0, 40);
                const sourceId = String(item.sourceId || entityKey).trim().slice(0, 128);
                if (!sourceType || !sourceId) {
                    throw new BadRequestError("sync_xp_events bắt buộc có sourceType và sourceId.");
                }

                // Kiểm tra chống cộng trùng lặp tuyệt đối theo (userId, sourceType, sourceId) hoặc (userId, entityKey)
                let dup = null;
                try {
                    const bySource = txApp.findRecordsByFilter(
                        colName,
                        "userId = {:uid} && sourceType = {:st} && sourceId = {:sid}",
                        "",
                        1,
                        0,
                        { uid: userId, st: sourceType, sid: sourceId }
                    );
                    if (bySource && bySource.length > 0) {
                        dup = bySource[0];
                    }
                } catch (_) {
                    dup = null;
                }
                if (!dup) {
                    try {
                        const byKey = txApp.findRecordsByFilter(
                            colName,
                            "userId = {:uid} && entityKey = {:ek}",
                            "",
                            1,
                            0,
                            { uid: userId, ek: entityKey }
                        );
                        if (byKey && byKey.length > 0) {
                            dup = byKey[0];
                        }
                    } catch (_) {
                        dup = null;
                    }
                }

                if (dup) {
                    results.push({
                        collection: colName,
                        entityKey: entityKey,
                        serverUpdated: String(dup.getString("updated") || ""),
                        status: "ignored_duplicate"
                    });
                    continue;
                }

                const rec = new Record(col);
                rec.set("userId", userId);
                rec.set("entityKey", entityKey);
                rec.set("sourceType", sourceType);
                rec.set("sourceId", sourceId);
                rec.set("payload", payloadObj);
                rec.set("clientUpdatedAt", clientUpdatedAt);
                rec.set("deleted", false);
                txApp.save(rec);

                results.push({
                    collection: colName,
                    entityKey: entityKey,
                    serverUpdated: String(rec.getString("updated") || ""),
                    status: "upserted"
                });
                continue;
            }

            let existing = null;
            try {
                const found = txApp.findRecordsByFilter(
                    colName,
                    "userId = {:uid} && entityKey = {:ek}",
                    "",
                    1,
                    0,
                    { uid: userId, ek: entityKey }
                );
                if (found && found.length > 0) {
                    existing = found[0];
                }
            } catch (_) {
                existing = null;
            }

            if (!existing) {
                const rec = new Record(col);
                rec.set("userId", userId);
                rec.set("entityKey", entityKey);
                rec.set("payload", payloadObj);
                rec.set("clientUpdatedAt", clientUpdatedAt);
                rec.set("deleted", deleted);
                txApp.save(rec);

                results.push({
                    collection: colName,
                    entityKey: entityKey,
                    serverUpdated: String(rec.getString("updated") || ""),
                    status: "upserted"
                });
            } else {
                const existingClientTs = Number(existing.getInt("clientUpdatedAt") || 0);
                const existingPayload = parseJsonSafe(existing.get("payload"));
                const existingDeleted = Boolean(existing.getBool("deleted"));

                let nextDeleted = existingDeleted;
                let nextPayload = existingPayload;
                let nextClientTs = Math.max(existingClientTs, clientUpdatedAt);

                if (deleted) {
                    if (clientUpdatedAt >= existingClientTs) {
                        nextDeleted = true;
                        nextPayload = payloadObj;
                    }
                } else if (existingDeleted && clientUpdatedAt > existingClientTs) {
                    nextDeleted = false;
                    nextPayload = payloadObj;
                } else if (!existingDeleted) {
                    nextPayload = mergePayloadForCollection(
                        colName,
                        existingPayload,
                        payloadObj,
                        existingClientTs,
                        clientUpdatedAt
                    );
                }

                existing.set("payload", nextPayload);
                existing.set("clientUpdatedAt", nextClientTs);
                existing.set("deleted", nextDeleted);
                txApp.save(existing);

                results.push({
                    collection: colName,
                    entityKey: entityKey,
                    serverUpdated: String(existing.getString("updated") || ""),
                    status: "merged"
                });
            }
        }
    });

    return e.json(200, {
        acceptedCount: results.length,
        serverTime: new Date().toISOString(),
        results: results
    });
}, $apis.requireAuth());

routerAdd("GET", "/api/custom/sync/pull", (e) => {
    const authRecord = e.auth;
    if (!authRecord) {
        throw new UnauthorizedError("Yêu cầu đăng nhập để đồng bộ.");
    }
    if (needsVerifiedParentalConsent(authRecord)) {
        return e.json(403, {
            code: 403,
            message: "Chờ xác nhận của phụ huynh trước khi đồng bộ dữ liệu."
        });
    }

    const userId = authRecord.id;
    if (!enforceSyncRateLimit(userId)) {
        return e.json(429, {
            code: 429,
            message: "Bạn đang đồng bộ quá nhanh. Vui lòng thử lại sau ít phút."
        });
    }

    const query = e.requestInfo().query || {};
    const since = String(query.since || "").trim();
    const rawLimit = Number(query.limit || 100);
    const limit = Math.min(200, Math.max(1, isNaN(rawLimit) ? 100 : Math.floor(rawLimit)));

    const rawCols = String(query.collections || "").trim();
    const requestedCols = rawCols
        ? rawCols.split(",").map((s) => s.trim()).filter((c) => ALLOWED_SYNC_COLLECTIONS.indexOf(c) !== -1)
        : ALLOWED_SYNC_COLLECTIONS;

    const allItems = [];

    for (let i = 0; i < requestedCols.length; i++) {
        const colName = requestedCols[i];
        const filter = since
            ? "userId = {:uid} && updated > {:since}"
            : "userId = {:uid}";
        const params = since ? { uid: userId, since: since } : { uid: userId };

        let rows = [];
        try {
            rows = $app.findRecordsByFilter(colName, filter, "+updated,+id", limit + 1, 0, params);
        } catch (_) {
            rows = [];
        }

        for (let j = 0; j < rows.length; j++) {
            const r = rows[j];
            const dto = {
                collection: colName,
                id: r.id,
                entityKey: r.getString("entityKey"),
                payload: parseJsonSafe(r.get("payload")),
                deleted: Boolean(r.getBool("deleted")),
                clientUpdatedAt: Number(r.getInt("clientUpdatedAt") || 0),
                updated: String(r.getString("updated") || "")
            };
            if (colName === "sync_xp_events") {
                dto.sourceType = r.getString("sourceType");
                dto.sourceId = r.getString("sourceId");
            }
            allItems.push(dto);
        }
    }

    allItems.sort((a, b) => {
        if (a.updated < b.updated) return -1;
        if (a.updated > b.updated) return 1;
        if (a.id < b.id) return -1;
        if (a.id > b.id) return 1;
        return 0;
    });

    const hasMore = allItems.length > limit;
    const sliced = hasMore ? allItems.slice(0, limit) : allItems;
    const nextCursor = sliced.length > 0 ? sliced[sliced.length - 1].updated : since;

    return e.json(200, {
        items: sliced,
        nextCursor: nextCursor,
        hasMore: hasMore,
        serverTime: new Date().toISOString()
    });
}, $apis.requireAuth());
