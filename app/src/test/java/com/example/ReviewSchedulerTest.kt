package com.example

import com.example.data.local.entity.WrongQuestionEntity
import com.example.domain.evaluator.ReviewScheduler
import com.example.domain.model.ReviewConfig
import com.example.domain.model.WrongQuestionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit Test toàn diện cho ReviewScheduler (F08 - Spaced Repetition Logic).
 * Kiểm tra các ca:
 * 1. Chuyển trạng thái khi làm sai lần đầu -> NEEDS_REVIEW
 * 2. Đủ 3 lần đúng cách nhau >= 1 ngày -> MASTERED
 * 3. Đang củng cố hoặc đã thuộc mà làm sai lại -> quay về NEEDS_REVIEW, chuỗi đúng reset về 0
 * 4. Khoảng cách ngày tăng dần (1 ngày, 3 ngày, 7 ngày)
 * 5. Chuỗi đúng trong cùng ngày (< 1 ngày) KHÔNG tính tăng chuỗi
 * 6. Đánh dấu đã thuộc thủ công
 */
class ReviewSchedulerTest {

    private lateinit var scheduler: ReviewScheduler
    private var simulatedTime: Long = 1_000_000_000L // mốc thời gian giả định

    private val oneDayMs: Long = 24L * 60L * 60L * 1000L

    @Before
    fun setup() {
        simulatedTime = 1_000_000_000L
        scheduler = ReviewScheduler(
            config = ReviewConfig(
                masteredThreshold = 3,
                minDaysBetweenReviews = 1,
                intervalDays = listOf(1, 3, 7)
            ),
            clock = { simulatedTime }
        )
    }

    @Test
    fun testFirstWrongAnswerCreatesNeedsReviewEntity() {
        val entity = scheduler.onAnswerWrong(
            existingEntity = null,
            questionId = "q_test_1",
            grade = 6,
            subjectId = "math",
            chapterId = "c1",
            lessonId = "l1",
            questionType = "MULTIPLE_CHOICE",
            wrongAnswer = "A",
            currentTime = simulatedTime
        )

        assertEquals("q_test_1", entity.questionId)
        assertEquals(WrongQuestionStatus.NEEDS_REVIEW.code, entity.status)
        assertEquals(1, entity.errorCount)
        assertEquals(0, entity.consecutiveCorrectCount)
        assertEquals(simulatedTime + oneDayMs, entity.nextReviewTimestamp)
        assertEquals(simulatedTime, entity.lastWrongTimestamp)
        assertEquals(simulatedTime, entity.lastReviewTimestamp)
    }

    @Test
    fun testAnswerCorrectInSameDayDoesNotIncrementStreak() {
        // Tạo câu sai
        val initial = scheduler.onAnswerWrong(
            existingEntity = null,
            questionId = "q_test_1",
            currentTime = simulatedTime
        )

        // Trả lời đúng lần 1 (lần đầu tiên trả lời đúng)
        // Lưu ý: initial có lastReviewTimestamp = simulatedTime. Nếu trả lời ngay trong ngày (< 24h):
        val sameDayTime = simulatedTime + (2 * 60 * 60 * 1000L) // sau 2 tiếng
        val sameDayResult = scheduler.onAnswerCorrect(initial, currentTime = sameDayTime)

        // Do cùng ngày (< 24h), không được tăng chuỗi đúng
        assertEquals(0, sameDayResult.consecutiveCorrectCount)
        assertEquals(WrongQuestionStatus.NEEDS_REVIEW.code, sameDayResult.status)
    }

    @Test
    fun testAnswerCorrectAfterOneDayIncrementsStreakToConsolidating() {
        val initial = scheduler.onAnswerWrong(
            existingEntity = null,
            questionId = "q_test_1",
            currentTime = simulatedTime
        )

        // Sau 1 ngày (24 giờ)
        val day1Time = simulatedTime + oneDayMs
        val correctDay1 = scheduler.onAnswerCorrect(initial, currentTime = day1Time)

        assertEquals(1, correctDay1.consecutiveCorrectCount)
        assertEquals(WrongQuestionStatus.CONSOLIDATING.code, correctDay1.status)
        // Lần 1 hẹn ôn sau 1 ngày
        assertEquals(day1Time + (1 * oneDayMs), correctDay1.nextReviewTimestamp)
    }

    @Test
    fun testThreeConsecutiveCorrectAcrossDaysTransitionsToMastered() {
        val initial = scheduler.onAnswerWrong(
            existingEntity = null,
            questionId = "q_test_1",
            currentTime = simulatedTime
        )

        // Lần 1 đúng (sau 1 ngày)
        val t1 = simulatedTime + oneDayMs
        val r1 = scheduler.onAnswerCorrect(initial, currentTime = t1)
        assertEquals(1, r1.consecutiveCorrectCount)
        assertEquals(WrongQuestionStatus.CONSOLIDATING.code, r1.status)
        assertEquals(t1 + (1 * oneDayMs), r1.nextReviewTimestamp)

        // Lần 2 đúng (sau 1 ngày tiếp theo)
        val t2 = t1 + oneDayMs
        val r2 = scheduler.onAnswerCorrect(r1, currentTime = t2)
        assertEquals(2, r2.consecutiveCorrectCount)
        assertEquals(WrongQuestionStatus.CONSOLIDATING.code, r2.status)
        // Lần 2 hẹn ôn sau 3 ngày
        assertEquals(t2 + (3 * oneDayMs), r2.nextReviewTimestamp)

        // Lần 3 đúng (sau 3 ngày tiếp theo)
        val t3 = t2 + (3 * oneDayMs)
        val r3 = scheduler.onAnswerCorrect(r2, currentTime = t3)
        assertEquals(3, r3.consecutiveCorrectCount)
        assertEquals(WrongQuestionStatus.MASTERED.code, r3.status)
        assertEquals(0L, r3.nextReviewTimestamp) // Đã thuộc, không còn lịch hẹn bắt buộc
    }

    @Test
    fun testAnswerWrongResetsStreakAndReturnsToNeedsReview() {
        // Đã đạt mốc 2 lần đúng (CONSOLIDATING)
        var entity = scheduler.onAnswerWrong(null, "q_1", currentTime = simulatedTime)
        entity = scheduler.onAnswerCorrect(entity, currentTime = simulatedTime + oneDayMs)
        entity = scheduler.onAnswerCorrect(entity, currentTime = simulatedTime + (2 * oneDayMs))
        assertEquals(2, entity.consecutiveCorrectCount)
        assertEquals(WrongQuestionStatus.CONSOLIDATING.code, entity.status)

        // Bị trả lời sai lại
        val wrongTime = simulatedTime + (3 * oneDayMs)
        val afterWrong = scheduler.onAnswerWrong(
            existingEntity = entity,
            questionId = "q_1",
            wrongAnswer = "B",
            currentTime = wrongTime
        )

        assertEquals(0, afterWrong.consecutiveCorrectCount) // Reset về 0
        assertEquals(WrongQuestionStatus.NEEDS_REVIEW.code, afterWrong.status) // Quay lại Cần ôn
        assertEquals(2, afterWrong.errorCount) // Tăng số lần sai lên 2
        assertEquals(wrongTime + oneDayMs, afterWrong.nextReviewTimestamp)
    }

    @Test
    fun testMasteredQuestionBecomesNeedsReviewWhenAnsweredWrongLater() {
        // Câu đã thuộc
        var entity = WrongQuestionEntity(
            questionId = "q_mastered",
            grade = 6,
            subjectId = "math",
            status = WrongQuestionStatus.MASTERED.code,
            consecutiveCorrectCount = 3,
            errorCount = 1
        )

        // Học sinh làm bài thi sau đó bị sai lại
        val wrongTime = simulatedTime + (10 * oneDayMs)
        val afterWrong = scheduler.onAnswerWrong(
            existingEntity = entity,
            questionId = "q_mastered",
            wrongAnswer = "C",
            currentTime = wrongTime
        )

        assertEquals(WrongQuestionStatus.NEEDS_REVIEW.code, afterWrong.status)
        assertEquals(0, afterWrong.consecutiveCorrectCount)
        assertEquals(2, afterWrong.errorCount)
        assertEquals(wrongTime + oneDayMs, afterWrong.nextReviewTimestamp)
    }

    @Test
    fun testManualMarkMasteredSetsStatusAndThreshold() {
        val entity = scheduler.onAnswerWrong(null, "q_manual", currentTime = simulatedTime)
        assertEquals(WrongQuestionStatus.NEEDS_REVIEW.code, entity.status)

        val mastered = scheduler.markMasteredManually(entity, currentTime = simulatedTime + 1000)
        assertEquals(WrongQuestionStatus.MASTERED.code, mastered.status)
        assertEquals(3, mastered.consecutiveCorrectCount)
        assertEquals(0L, mastered.nextReviewTimestamp)
    }

    @Test
    fun testIsDueForReviewCalculation() {
        val entity = WrongQuestionEntity(
            questionId = "q_due",
            status = WrongQuestionStatus.NEEDS_REVIEW.code,
            nextReviewTimestamp = simulatedTime + oneDayMs
        )

        // Chưa đến hạn
        assertFalse(scheduler.isDueForReview(entity, currentTime = simulatedTime))

        // Đúng hạn hoặc quá hạn
        assertTrue(scheduler.isDueForReview(entity, currentTime = simulatedTime + oneDayMs))
        assertTrue(scheduler.isDueForReview(entity, currentTime = simulatedTime + (2 * oneDayMs)))

        // Đã thuộc -> không bao giờ tính là đến hạn
        val mastered = entity.copy(status = WrongQuestionStatus.MASTERED.code)
        assertFalse(scheduler.isDueForReview(mastered, currentTime = simulatedTime + (10 * oneDayMs)))
    }
}
