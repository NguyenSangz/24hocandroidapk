package com.example.data.sync

import com.example.data.local.entity.BadgeUnlockEntity
import com.example.data.local.entity.FlashcardProgressEntity
import com.example.data.local.entity.TestAnswerEntity
import com.example.data.local.entity.TestEntity
import com.example.data.local.entity.WrongQuestionEntity
import com.example.data.local.entity.XpEventEntity

/**
 * Bộ giải quyết xung đột thuần Kotlin (100% deterministic, dễ viết unit test)
 * theo đúng quy tắc Offline-First và gộp dữ liệu F15b/F16b:
 * - Bài học: OR-completed (`local || remote`)
 * - Kiểm tra/Luyện tập: chèn không trùng theo UUID / `(questionId, timestamp)`; bài đã nộp thắng bản nháp
 * - Câu sai: giữ max `errorCount`, ưu tiên trạng thái đã thuộc (`MASTERED`) hoặc lần ôn mới hơn
 * - Flashcard SM-2: bản có `totalReviews` cao hơn hoặc `lastReviewTimestamp` mới hơn thắng
 * - XP: union không trùng theo `eventKey` / `(sourceType, sourceId)`, không bao giờ cộng đúp
 * - Huy hiệu: union không trùng, giữ `unlockedAt` sớm nhất
 * - Cài đặt / Bộ từ / Động từ: LWW theo `clientUpdatedAt`, tôn trọng `deleted = true` (soft delete)
 */
object SyncConflictResolver {

    fun mergeLessonCompletion(localCompleted: Boolean, remoteCompleted: Boolean): Boolean {
        return localCompleted || remoteCompleted
    }

    fun mergeWrongQuestion(
        local: WrongQuestionEntity?,
        remotePayload: Map<String, Any?>,
        remoteDeleted: Boolean,
        remoteClientUpdatedAt: Long
    ): WrongQuestionMergeDecision {
        val questionId = remotePayload.string("questionId").ifBlank { local?.questionId.orEmpty() }
        if (questionId.isBlank()) return WrongQuestionMergeDecision.NoOp

        if (remoteDeleted) {
            if (local == null) return WrongQuestionMergeDecision.NoOp
            val localLatest = maxOf(local.lastWrongTimestamp, local.lastReviewTimestamp)
            return if (remoteClientUpdatedAt >= localLatest) {
                WrongQuestionMergeDecision.DeleteLocal(questionId)
            } else {
                WrongQuestionMergeDecision.NoOp
            }
        }

        val remoteEntity = WrongQuestionEntity(
            questionId = questionId,
            grade = remotePayload.int("grade", local?.grade ?: 6),
            subjectId = remotePayload.string("subjectId").ifBlank { local?.subjectId.orEmpty() },
            chapterId = remotePayload.string("chapterId").ifBlank { local?.chapterId.orEmpty() },
            lessonId = remotePayload.string("lessonId").ifBlank { local?.lessonId.orEmpty() },
            questionType = remotePayload.string("questionType").ifBlank { local?.questionType.orEmpty() },
            errorCount = remotePayload.int("errorCount", 1),
            consecutiveCorrectCount = remotePayload.int("consecutiveCorrectCount", 0),
            lastWrongTimestamp = remotePayload.long("lastWrongTimestamp", 0L),
            lastWrongAnswer = remotePayload.string("lastWrongAnswer"),
            status = remotePayload.string("status").ifBlank { "NEEDS_REVIEW" },
            nextReviewTimestamp = remotePayload.long("nextReviewTimestamp", 0L),
            lastReviewTimestamp = remotePayload.long("lastReviewTimestamp", 0L)
        )

        if (local == null) {
            return WrongQuestionMergeDecision.Upsert(remoteEntity)
        }

        val mergedErrorCount = maxOf(local.errorCount, remoteEntity.errorCount)
        val localLatest = maxOf(local.lastReviewTimestamp, local.lastWrongTimestamp)
        val remoteLatest = maxOf(remoteEntity.lastReviewTimestamp, remoteEntity.lastWrongTimestamp)

        val winner = when {
            remoteEntity.status == "MASTERED" && remoteEntity.lastReviewTimestamp >= local.lastWrongTimestamp -> remoteEntity
            local.status == "MASTERED" && local.lastReviewTimestamp >= remoteEntity.lastWrongTimestamp -> local
            remoteLatest >= localLatest -> remoteEntity
            else -> local
        }

        val finalStatus = if (
            (local.status == "MASTERED" && local.lastReviewTimestamp >= remoteEntity.lastWrongTimestamp) ||
            (remoteEntity.status == "MASTERED" && remoteEntity.lastReviewTimestamp >= local.lastWrongTimestamp)
        ) {
            "MASTERED"
        } else {
            winner.status
        }

        return WrongQuestionMergeDecision.Upsert(
            winner.copy(
                errorCount = mergedErrorCount,
                status = finalStatus,
                grade = if (winner.grade in 6..9) winner.grade else local.grade,
                subjectId = winner.subjectId.ifBlank { local.subjectId },
                chapterId = winner.chapterId.ifBlank { local.chapterId },
                lessonId = winner.lessonId.ifBlank { local.lessonId },
                questionType = winner.questionType.ifBlank { local.questionType }
            )
        )
    }

    fun mergeFlashcardProgress(
        local: FlashcardProgressEntity?,
        remotePayload: Map<String, Any?>,
        remoteDeleted: Boolean
    ): FlashcardProgressEntity? {
        if (remoteDeleted) return null
        val cardId = remotePayload.string("cardId")
        if (cardId.isBlank()) return null

        val remote = FlashcardProgressEntity(
            cardId = cardId,
            deckId = remotePayload.string("deckId").ifBlank { local?.deckId ?: "deck_default" },
            repetitions = remotePayload.int("repetitions", 0),
            intervalDays = remotePayload.int("intervalDays", 0),
            easeFactor = remotePayload.float("easeFactor", 2.5f).coerceAtLeast(1.3f),
            consecutiveCorrect = remotePayload.int("consecutiveCorrect", 0),
            nextReviewTimestamp = remotePayload.long("nextReviewTimestamp", 0L),
            lastReviewTimestamp = remotePayload.long("lastReviewTimestamp", 0L),
            state = remotePayload.string("state").ifBlank { "NEW" },
            totalReviews = remotePayload.int("totalReviews", 0),
            lapses = remotePayload.int("lapses", 0)
        )

        if (local == null) return remote

        return when {
            remote.totalReviews > local.totalReviews -> remote
            remote.totalReviews < local.totalReviews -> local
            remote.lastReviewTimestamp >= local.lastReviewTimestamp -> remote
            else -> local
        }
    }

    fun mergeTest(
        local: TestEntity?,
        remoteEntityKey: String,
        remotePayload: Map<String, Any?>,
        remoteDeleted: Boolean,
        remoteClientUpdatedAt: Long
    ): TestMergeDecision {
        val testId = remotePayload.string("id").ifBlank { remoteEntityKey }
        if (testId.isBlank()) return TestMergeDecision.NoOp

        if (remoteDeleted) {
            return if (local == null) {
                TestMergeDecision.NoOp
            } else if (remoteClientUpdatedAt >= local.timestamp) {
                TestMergeDecision.DeleteLocal(testId)
            } else {
                TestMergeDecision.NoOp
            }
        }

        val remoteTest = TestEntity(
            id = testId,
            title = remotePayload.string("title").ifBlank { "Bài kiểm tra" },
            type = remotePayload.string("type").ifBlank { "LESSON" },
            subjectId = remotePayload.string("subjectId").ifBlank { "all" },
            subjectName = remotePayload.string("subjectName").ifBlank { "Tổng hợp" },
            grade = remotePayload.int("grade", 6),
            targetId = remotePayload.string("targetId").ifBlank { "all" },
            score = remotePayload.float("score", 0f),
            maxScore = remotePayload.float("maxScore", 10f),
            totalQuestions = remotePayload.int("totalQuestions", 0),
            correctCount = remotePayload.int("correctCount", 0),
            timeSpentSeconds = remotePayload.int("timeSpentSeconds", 0),
            durationMinutes = remotePayload.int("durationMinutes", 15),
            timestamp = remotePayload.long("timestamp", remoteClientUpdatedAt),
            isDraft = remotePayload.bool("isDraft", false),
            remainingSeconds = remotePayload.int("remainingSeconds", 0),
            endTimeEpochMillis = remotePayload.long("endTimeEpochMillis", 0L)
        )

        val shouldTakeRemote = when {
            local == null -> true
            !local.isDraft && remoteTest.isDraft -> false
            local.isDraft && !remoteTest.isDraft -> true
            else -> remoteClientUpdatedAt >= local.timestamp
        }
        if (!shouldTakeRemote) return TestMergeDecision.NoOp

        @Suppress("UNCHECKED_CAST")
        val rawAnswers = (remotePayload["answers"] as? List<Map<String, Any?>>).orEmpty()
        val parsedAnswers = rawAnswers.mapIndexed { idx, a ->
            TestAnswerEntity(
                testId = testId,
                questionId = a.string("questionId"),
                questionIndex = a.int("questionIndex", idx),
                userAnswerJson = a.string("userAnswerJson"),
                isAnswered = a.bool("isAnswered", false),
                isFlagged = a.bool("isFlagged", false),
                score = a.float("score", 0f),
                isCorrect = a.bool("isCorrect", false),
                gradeStatus = a.string("gradeStatus").ifBlank { "INCORRECT" },
                selfEvaluated = a.bool("selfEvaluated", false)
            )
        }
        return TestMergeDecision.Upsert(remoteTest, parsedAnswers)
    }

    fun parseXpEventIfNew(
        remoteEntityKey: String,
        remotePayload: Map<String, Any?>,
        existingEventKeys: Set<String>
    ): XpEventEntity? {
        val eventKey = remotePayload.string("eventKey").ifBlank { remoteEntityKey }
        if (eventKey.isBlank() || existingEventKeys.contains(eventKey)) return null
        return XpEventEntity(
            eventType = remotePayload.string("eventType").ifBlank { "SYNCED" },
            eventKey = eventKey,
            xpAmount = remotePayload.int("xpAmount", 0),
            timestamp = remotePayload.long("timestamp", 0L),
            metadata = remotePayload.string("metadata")
        )
    }

    fun mergeBadge(
        local: BadgeUnlockEntity?,
        remoteEntityKey: String,
        remotePayload: Map<String, Any?>
    ): BadgeUnlockEntity? {
        val badgeId = remotePayload.string("badgeId").ifBlank { remoteEntityKey }
        if (badgeId.isBlank()) return null
        val remoteUnlockedAt = remotePayload.long("unlockedAt", 0L)
        val metadata = remotePayload.string("metadata")
        if (local == null) {
            return BadgeUnlockEntity(
                badgeId = badgeId,
                unlockedAt = remoteUnlockedAt,
                metadata = metadata
            )
        }
        val earliest = when {
            local.unlockedAt > 0L && remoteUnlockedAt > 0L -> minOf(local.unlockedAt, remoteUnlockedAt)
            local.unlockedAt > 0L -> local.unlockedAt
            else -> remoteUnlockedAt
        }
        return local.copy(
            unlockedAt = earliest,
            metadata = local.metadata.ifBlank { metadata }
        )
    }

    fun shouldApplySettings(localUpdatedAt: Long, remoteClientUpdatedAt: Long): Boolean {
        return remoteClientUpdatedAt > localUpdatedAt
    }

    fun mergeIrregularVerbs(
        localVerbs: Set<String>,
        remotePayload: Map<String, Any?>,
        remoteDeleted: Boolean
    ): Set<String> {
        if (remoteDeleted) return localVerbs
        val raw = (remotePayload["masteredVerbs"] as? List<*>).orEmpty()
            .mapNotNull { it?.toString()?.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        return localVerbs + raw
    }

    sealed class WrongQuestionMergeDecision {
        data object NoOp : WrongQuestionMergeDecision()
        data class Upsert(val entity: WrongQuestionEntity) : WrongQuestionMergeDecision()
        data class DeleteLocal(val questionId: String) : WrongQuestionMergeDecision()
    }

    sealed class TestMergeDecision {
        data object NoOp : TestMergeDecision()
        data class Upsert(val test: TestEntity, val answers: List<TestAnswerEntity>) : TestMergeDecision()
        data class DeleteLocal(val testId: String) : TestMergeDecision()
    }

    private fun Map<String, Any?>.string(key: String): String =
        (this[key] as? String)?.trim().orEmpty()

    private fun Map<String, Any?>.int(key: String, default: Int): Int =
        (this[key] as? Number)?.toInt() ?: default

    private fun Map<String, Any?>.long(key: String, default: Long): Long =
        (this[key] as? Number)?.toLong() ?: default

    private fun Map<String, Any?>.float(key: String, default: Float): Float =
        (this[key] as? Number)?.toFloat() ?: default

    private fun Map<String, Any?>.bool(key: String, default: Boolean): Boolean =
        (this[key] as? Boolean) ?: default
}
