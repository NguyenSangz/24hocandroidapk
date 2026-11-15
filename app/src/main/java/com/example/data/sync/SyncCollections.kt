package com.example.data.sync

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AttemptEntity
import com.example.data.local.entity.BadgeUnlockEntity
import com.example.data.local.entity.FlashcardProgressEntity
import com.example.data.local.entity.SyncOutboxEntity
import com.example.data.local.entity.TestAnswerEntity
import com.example.data.local.entity.TestEntity
import com.example.data.local.entity.WrongQuestionEntity
import com.example.data.local.entity.XpEventEntity
import org.json.JSONArray
import org.json.JSONObject

object SyncCollections {
    const val LESSON_PROGRESS = "sync_lesson_progress"
    const val ATTEMPTS = "sync_attempts"
    const val TESTS = "sync_tests"
    const val WRONG_QUESTIONS = "sync_wrong_questions"
    const val FLASHCARDS = "sync_flashcards"
    const val XP_EVENTS = "sync_xp_events"
    const val BADGES = "sync_badges"
    const val SETTINGS = "sync_settings"
    const val VOCAB_PACKS = "sync_vocab_packs"
    const val IRREGULAR_VERBS = "sync_irregular_verbs"

    val ALL = listOf(
        LESSON_PROGRESS,
        ATTEMPTS,
        TESTS,
        WRONG_QUESTIONS,
        FLASHCARDS,
        XP_EVENTS,
        BADGES,
        SETTINGS,
        VOCAB_PACKS,
        IRREGULAR_VERBS
    )
}

/**
 * Hỗ trợ đóng gói các thay đổi cục bộ vào bảng `sync_outbox` một cách an toàn, idempotent,
 * không bao giờ làm gián đoạn thao tác học tập offline.
 */
object SyncOutboxHelper {

    fun attemptEntityKey(questionId: String, timestamp: Long): String =
        "${questionId}_$timestamp"

    suspend fun enqueueLessonProgress(
        database: AppDatabase?,
        lessonId: String,
        completed: Boolean,
        now: Long = System.currentTimeMillis()
    ) {
        val db = database ?: return
        val payload = JSONObject().apply {
            put("lessonId", lessonId)
            put("completed", completed)
            put("updatedAt", now)
        }.toString()
        upsertOutboxSafely(
            db = db,
            collection = SyncCollections.LESSON_PROGRESS,
            entityKey = lessonId,
            payloadJson = payload,
            clientUpdatedAt = now
        )
    }

    suspend fun enqueueAttempt(
        database: AppDatabase?,
        attempt: AttemptEntity,
        now: Long = attempt.timestamp
    ) {
        val db = database ?: return
        val key = attemptEntityKey(attempt.questionId, attempt.timestamp)
        val payload = JSONObject().apply {
            put("questionId", attempt.questionId)
            put("isCorrect", attempt.isCorrect)
            put("score", attempt.score.toDouble())
            put("timestamp", attempt.timestamp)
            put("timeSpentSeconds", attempt.timeSpentSeconds)
            put("userAnswer", attempt.userAnswer)
            put("usedHint", attempt.usedHint)
        }.toString()
        upsertOutboxSafely(
            db = db,
            collection = SyncCollections.ATTEMPTS,
            entityKey = key,
            payloadJson = payload,
            clientUpdatedAt = now
        )
    }

    suspend fun enqueueTest(
        database: AppDatabase?,
        test: TestEntity,
        answers: List<TestAnswerEntity>,
        deleted: Boolean = false,
        now: Long = System.currentTimeMillis()
    ) {
        val db = database ?: return
        val answersArray = JSONArray()
        answers.forEach { ans ->
            answersArray.put(
                JSONObject().apply {
                    put("questionId", ans.questionId)
                    put("questionIndex", ans.questionIndex)
                    put("userAnswerJson", ans.userAnswerJson)
                    put("isAnswered", ans.isAnswered)
                    put("isFlagged", ans.isFlagged)
                    put("score", ans.score.toDouble())
                    put("isCorrect", ans.isCorrect)
                    put("gradeStatus", ans.gradeStatus)
                    put("selfEvaluated", ans.selfEvaluated)
                }
            )
        }
        val payload = JSONObject().apply {
            put("id", test.id)
            put("title", test.title)
            put("type", test.type)
            put("subjectId", test.subjectId)
            put("subjectName", test.subjectName)
            put("grade", test.grade)
            put("targetId", test.targetId)
            put("score", test.score.toDouble())
            put("maxScore", test.maxScore.toDouble())
            put("totalQuestions", test.totalQuestions)
            put("correctCount", test.correctCount)
            put("timeSpentSeconds", test.timeSpentSeconds)
            put("durationMinutes", test.durationMinutes)
            put("timestamp", test.timestamp)
            put("isDraft", test.isDraft)
            put("remainingSeconds", test.remainingSeconds)
            put("endTimeEpochMillis", test.endTimeEpochMillis)
            put("answers", answersArray)
        }.toString()
        upsertOutboxSafely(
            db = db,
            collection = SyncCollections.TESTS,
            entityKey = test.id,
            payloadJson = payload,
            deleted = deleted,
            clientUpdatedAt = maxOf(now, test.timestamp)
        )
    }

    suspend fun enqueueTestDeletion(
        database: AppDatabase?,
        testId: String,
        now: Long = System.currentTimeMillis()
    ) {
        val db = database ?: return
        val payload = JSONObject().apply { put("id", testId) }.toString()
        upsertOutboxSafely(
            db = db,
            collection = SyncCollections.TESTS,
            entityKey = testId,
            payloadJson = payload,
            deleted = true,
            clientUpdatedAt = now
        )
    }

    suspend fun enqueueWrongQuestion(
        database: AppDatabase?,
        entity: WrongQuestionEntity,
        deleted: Boolean = false,
        now: Long = System.currentTimeMillis()
    ) {
        val db = database ?: return
        val payload = JSONObject().apply {
            put("questionId", entity.questionId)
            put("grade", entity.grade)
            put("subjectId", entity.subjectId)
            put("chapterId", entity.chapterId)
            put("lessonId", entity.lessonId)
            put("questionType", entity.questionType)
            put("errorCount", entity.errorCount)
            put("consecutiveCorrectCount", entity.consecutiveCorrectCount)
            put("lastWrongTimestamp", entity.lastWrongTimestamp)
            put("lastWrongAnswer", entity.lastWrongAnswer)
            put("status", entity.status)
            put("nextReviewTimestamp", entity.nextReviewTimestamp)
            put("lastReviewTimestamp", entity.lastReviewTimestamp)
        }.toString()
        upsertOutboxSafely(
            db = db,
            collection = SyncCollections.WRONG_QUESTIONS,
            entityKey = entity.questionId,
            payloadJson = payload,
            deleted = deleted,
            clientUpdatedAt = maxOf(now, entity.lastWrongTimestamp, entity.lastReviewTimestamp)
        )
    }

    suspend fun enqueueWrongQuestionDeletion(
        database: AppDatabase?,
        questionId: String,
        now: Long = System.currentTimeMillis()
    ) {
        val db = database ?: return
        val payload = JSONObject().apply { put("questionId", questionId) }.toString()
        upsertOutboxSafely(
            db = db,
            collection = SyncCollections.WRONG_QUESTIONS,
            entityKey = questionId,
            payloadJson = payload,
            deleted = true,
            clientUpdatedAt = now
        )
    }

    suspend fun enqueueFlashcardProgress(
        database: AppDatabase?,
        progress: FlashcardProgressEntity,
        now: Long = System.currentTimeMillis()
    ) {
        val db = database ?: return
        val payload = JSONObject().apply {
            put("cardId", progress.cardId)
            put("deckId", progress.deckId)
            put("repetitions", progress.repetitions)
            put("intervalDays", progress.intervalDays)
            put("easeFactor", progress.easeFactor.toDouble())
            put("consecutiveCorrect", progress.consecutiveCorrect)
            put("nextReviewTimestamp", progress.nextReviewTimestamp)
            put("lastReviewTimestamp", progress.lastReviewTimestamp)
            put("state", progress.state)
            put("totalReviews", progress.totalReviews)
            put("lapses", progress.lapses)
        }.toString()
        upsertOutboxSafely(
            db = db,
            collection = SyncCollections.FLASHCARDS,
            entityKey = progress.cardId,
            payloadJson = payload,
            clientUpdatedAt = maxOf(now, progress.lastReviewTimestamp)
        )
    }

    suspend fun enqueueXpEvent(
        database: AppDatabase?,
        event: XpEventEntity
    ) {
        val db = database ?: return
        val payload = JSONObject().apply {
            put("eventType", event.eventType)
            put("eventKey", event.eventKey)
            put("xpAmount", event.xpAmount)
            put("timestamp", event.timestamp)
            put("metadata", event.metadata)
        }.toString()
        upsertOutboxSafely(
            db = db,
            collection = SyncCollections.XP_EVENTS,
            entityKey = event.eventKey,
            sourceType = event.eventType.take(40),
            sourceId = event.eventKey.take(128),
            payloadJson = payload,
            clientUpdatedAt = event.timestamp
        )
    }

    suspend fun enqueueBadgeUnlock(
        database: AppDatabase?,
        badge: BadgeUnlockEntity
    ) {
        val db = database ?: return
        val payload = JSONObject().apply {
            put("badgeId", badge.badgeId)
            put("unlockedAt", badge.unlockedAt)
            put("metadata", badge.metadata)
        }.toString()
        upsertOutboxSafely(
            db = db,
            collection = SyncCollections.BADGES,
            entityKey = badge.badgeId,
            payloadJson = payload,
            clientUpdatedAt = badge.unlockedAt
        )
    }

    suspend fun enqueueSettingsSnapshot(
        database: AppDatabase?,
        snapshot: Map<String, Any?>,
        now: Long = System.currentTimeMillis()
    ) {
        val db = database ?: return
        val json = JSONObject()
        snapshot.forEach { (k, v) -> json.put(k, v) }
        upsertOutboxSafely(
            db = db,
            collection = SyncCollections.SETTINGS,
            entityKey = "user_settings_global",
            payloadJson = json.toString(),
            clientUpdatedAt = now
        )
    }

    suspend fun enqueueIrregularVerbs(
        database: AppDatabase?,
        masteredVerbs: Set<String>,
        now: Long = System.currentTimeMillis()
    ) {
        val db = database ?: return
        val arr = JSONArray()
        masteredVerbs.sorted().forEach { arr.put(it) }
        val json = JSONObject().apply {
            put("masteredVerbs", arr)
            put("updatedAt", now)
        }.toString()
        upsertOutboxSafely(
            db = db,
            collection = SyncCollections.IRREGULAR_VERBS,
            entityKey = "mastered_verbs",
            payloadJson = json,
            clientUpdatedAt = now
        )
    }

    suspend fun enqueueCustomVocabPack(
        database: AppDatabase?,
        packId: String,
        payloadJson: String,
        deleted: Boolean = false,
        now: Long = System.currentTimeMillis()
    ) {
        val db = database ?: return
        upsertOutboxSafely(
            db = db,
            collection = SyncCollections.VOCAB_PACKS,
            entityKey = packId,
            payloadJson = payloadJson,
            deleted = deleted,
            clientUpdatedAt = now
        )
    }

    private suspend fun upsertOutboxSafely(
        db: AppDatabase,
        collection: String,
        entityKey: String,
        sourceType: String = "",
        sourceId: String = "",
        payloadJson: String,
        deleted: Boolean = false,
        clientUpdatedAt: Long
    ) {
        try {
            val syncDao = db.syncDao()
            val existing = syncDao.getOutboxItem(collection, entityKey)
            val entity = SyncOutboxEntity(
                id = existing?.id ?: 0L,
                collectionName = collection,
                entityKey = entityKey.take(128),
                sourceType = sourceType,
                sourceId = sourceId,
                payloadJson = payloadJson,
                deleted = deleted,
                clientUpdatedAt = clientUpdatedAt,
                retryCount = 0,
                lastAttemptAt = 0L,
                lastError = "",
                status = SyncOutboxEntity.STATUS_PENDING,
                createdAt = existing?.createdAt ?: clientUpdatedAt
            )
            syncDao.upsertOutboxItem(entity)
        } catch (_: Exception) {
            // Không để lỗi outbox chặn luồng học offline chính
        }
    }

    fun jsonToMap(jsonString: String): Map<String, Any?> {
        if (jsonString.isBlank()) return emptyMap()
        return try {
            val obj = JSONObject(jsonString)
            jsonObjectToMap(obj)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun jsonObjectToMap(obj: JSONObject): Map<String, Any?> {
        val map = linkedMapOf<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = fromJsonValue(obj.opt(key))
        }
        return map
    }

    private fun jsonArrayToList(arr: JSONArray): List<Any?> {
        val list = ArrayList<Any?>(arr.length())
        for (i in 0 until arr.length()) {
            list.add(fromJsonValue(arr.opt(i)))
        }
        return list
    }

    private fun fromJsonValue(value: Any?): Any? = when (value) {
        null, JSONObject.NULL -> null
        is JSONObject -> jsonObjectToMap(value)
        is JSONArray -> jsonArrayToList(value)
        else -> value
    }
}
