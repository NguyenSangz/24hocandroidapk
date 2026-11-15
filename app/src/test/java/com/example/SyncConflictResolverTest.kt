package com.example

import com.example.data.local.entity.BadgeUnlockEntity
import com.example.data.local.entity.FlashcardProgressEntity
import com.example.data.local.entity.TestEntity
import com.example.data.local.entity.WrongQuestionEntity
import com.example.data.sync.SyncConflictResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test thuần cho bộ giải quyết xung đột [SyncConflictResolver] (F16b).
 */
class SyncConflictResolverTest {

    @Test
    fun `lesson completion uses OR semantics`() {
        assertFalse(SyncConflictResolver.mergeLessonCompletion(false, false))
        assertTrue(SyncConflictResolver.mergeLessonCompletion(true, false))
        assertTrue(SyncConflictResolver.mergeLessonCompletion(false, true))
        assertTrue(SyncConflictResolver.mergeLessonCompletion(true, true))
    }

    @Test
    fun `wrong question keeps max errorCount and prefers newer mastered state`() {
        val local = WrongQuestionEntity(
            questionId = "q1",
            grade = 7,
            subjectId = "toan",
            errorCount = 4,
            lastWrongTimestamp = 1_000L,
            status = "NEEDS_REVIEW",
            lastReviewTimestamp = 1_200L
        )
        val remotePayload = mapOf<String, Any?>(
            "questionId" to "q1",
            "grade" to 7,
            "subjectId" to "toan",
            "errorCount" to 2,
            "consecutiveCorrectCount" to 3,
            "lastWrongTimestamp" to 900L,
            "status" to "MASTERED",
            "lastReviewTimestamp" to 2_000L
        )

        val decision = SyncConflictResolver.mergeWrongQuestion(
            local = local,
            remotePayload = remotePayload,
            remoteDeleted = false,
            remoteClientUpdatedAt = 2_000L
        )

        assertTrue(decision is SyncConflictResolver.WrongQuestionMergeDecision.Upsert)
        val merged = (decision as SyncConflictResolver.WrongQuestionMergeDecision.Upsert).entity
        assertEquals(4, merged.errorCount)
        assertEquals("MASTERED", merged.status)
    }

    @Test
    fun `wrong question soft delete removes local when tombstone is newer`() {
        val local = WrongQuestionEntity(
            questionId = "q2",
            lastWrongTimestamp = 1_000L,
            lastReviewTimestamp = 1_100L
        )
        val newerDelete = SyncConflictResolver.mergeWrongQuestion(
            local = local,
            remotePayload = mapOf("questionId" to "q2"),
            remoteDeleted = true,
            remoteClientUpdatedAt = 2_000L
        )
        assertTrue(newerDelete is SyncConflictResolver.WrongQuestionMergeDecision.DeleteLocal)

        val staleDelete = SyncConflictResolver.mergeWrongQuestion(
            local = local,
            remotePayload = mapOf("questionId" to "q2"),
            remoteDeleted = true,
            remoteClientUpdatedAt = 500L
        )
        assertEquals(SyncConflictResolver.WrongQuestionMergeDecision.NoOp, staleDelete)
    }

    @Test
    fun `flashcard SM-2 merge chooses higher totalReviews then latest timestamp`() {
        val local = FlashcardProgressEntity(
            cardId = "fc_1",
            deckId = "deck_eng6",
            repetitions = 2,
            intervalDays = 6,
            easeFactor = 2.5f,
            totalReviews = 3,
            lastReviewTimestamp = 1_500L,
            state = "REVIEW"
        )
        val remoteHigherReviews = mapOf<String, Any?>(
            "cardId" to "fc_1",
            "deckId" to "deck_eng6",
            "repetitions" to 4,
            "intervalDays" to 15,
            "easeFactor" to 2.6,
            "totalReviews" to 5,
            "lastReviewTimestamp" to 1_400L,
            "state" to "MASTERED"
        )

        val merged1 = SyncConflictResolver.mergeFlashcardProgress(local, remoteHigherReviews, false)
        assertNotNull(merged1)
        assertEquals(5, merged1!!.totalReviews)
        assertEquals(15, merged1.intervalDays)
        assertEquals("MASTERED", merged1.state)

        // Same review count, older timestamp -> keep local
        val remoteSameReviewsOlder = remoteHigherReviews.toMutableMap().apply {
            this["totalReviews"] = 3
            this["lastReviewTimestamp"] = 1_000L
        }
        val merged2 = SyncConflictResolver.mergeFlashcardProgress(local, remoteSameReviewsOlder, false)
        assertEquals(1_500L, merged2!!.lastReviewTimestamp)
        assertEquals(6, merged2.intervalDays)
    }

    @Test
    fun `test merge always prefers submitted test over draft`() {
        val localSubmitted = TestEntity(
            id = "t_1",
            title = "Kiểm tra Toán 7",
            type = "CHAPTER",
            subjectId = "toan",
            subjectName = "Toán học",
            grade = 7,
            targetId = "c1",
            score = 9.0f,
            totalQuestions = 10,
            correctCount = 9,
            timeSpentSeconds = 300,
            durationMinutes = 15,
            timestamp = 1_000L,
            isDraft = false
        )
        val remoteDraft = mapOf<String, Any?>(
            "id" to "t_1",
            "title" to "Kiểm tra Toán 7",
            "isDraft" to true,
            "score" to 0.0,
            "timestamp" to 5_000L
        )

        val decision = SyncConflictResolver.mergeTest(
            local = localSubmitted,
            remoteEntityKey = "t_1",
            remotePayload = remoteDraft,
            remoteDeleted = false,
            remoteClientUpdatedAt = 5_000L
        )
        assertEquals(SyncConflictResolver.TestMergeDecision.NoOp, decision)

        val localDraft = localSubmitted.copy(isDraft = true, score = 0f, timestamp = 5_000L)
        val remoteSubmitted = mapOf<String, Any?>(
            "id" to "t_1",
            "title" to "Kiểm tra Toán 7",
            "isDraft" to false,
            "score" to 8.5,
            "timestamp" to 4_000L,
            "answers" to listOf(
                mapOf("questionId" to "q1", "questionIndex" to 0, "isAnswered" to true, "isCorrect" to true, "score" to 1.0)
            )
        )
        val decision2 = SyncConflictResolver.mergeTest(
            local = localDraft,
            remoteEntityKey = "t_1",
            remotePayload = remoteSubmitted,
            remoteDeleted = false,
            remoteClientUpdatedAt = 4_000L
        )
        assertTrue(decision2 is SyncConflictResolver.TestMergeDecision.Upsert)
        val upsert = decision2 as SyncConflictResolver.TestMergeDecision.Upsert
        assertFalse(upsert.test.isDraft)
        assertEquals(8.5f, upsert.test.score, 0.01f)
        assertEquals(1, upsert.answers.size)
    }

    @Test
    fun `xp events deduplicate strictly by eventKey`() {
        val existing = setOf("lesson_l1", "test_t1")
        val duplicate = SyncConflictResolver.parseXpEventIfNew(
            remoteEntityKey = "lesson_l1",
            remotePayload = mapOf("eventType" to "LESSON_COMPLETED", "eventKey" to "lesson_l1", "xpAmount" to 10, "timestamp" to 100L),
            existingEventKeys = existing
        )
        assertNull(duplicate)

        val fresh = SyncConflictResolver.parseXpEventIfNew(
            remoteEntityKey = "lesson_l2",
            remotePayload = mapOf("eventType" to "LESSON_COMPLETED", "eventKey" to "lesson_l2", "xpAmount" to 10, "timestamp" to 200L, "metadata" to "Bài 2"),
            existingEventKeys = existing
        )
        assertNotNull(fresh)
        assertEquals("lesson_l2", fresh!!.eventKey)
        assertEquals(10, fresh.xpAmount)
    }

    @Test
    fun `badge unlock union keeps earliest timestamp`() {
        val local = BadgeUnlockEntity(badgeId = "streak_7", unlockedAt = 5_000L, metadata = "")
        val merged = SyncConflictResolver.mergeBadge(
            local = local,
            remoteEntityKey = "streak_7",
            remotePayload = mapOf("badgeId" to "streak_7", "unlockedAt" to 2_000L, "metadata" to "Chuỗi 7 ngày")
        )
        assertNotNull(merged)
        assertEquals(2_000L, merged!!.unlockedAt)
        assertEquals("Chuỗi 7 ngày", merged.metadata)
    }

    @Test
    fun `settings uses LWW and irregular verbs use set union`() {
        assertFalse(SyncConflictResolver.shouldApplySettings(localUpdatedAt = 5_000L, remoteClientUpdatedAt = 4_000L))
        assertTrue(SyncConflictResolver.shouldApplySettings(localUpdatedAt = 5_000L, remoteClientUpdatedAt = 6_000L))

        val mergedVerbs = SyncConflictResolver.mergeIrregularVerbs(
            localVerbs = setOf("go", "see"),
            remotePayload = mapOf("masteredVerbs" to listOf("see", "write", "take")),
            remoteDeleted = false
        )
        assertEquals(setOf("go", "see", "write", "take"), mergedVerbs)
    }
}
