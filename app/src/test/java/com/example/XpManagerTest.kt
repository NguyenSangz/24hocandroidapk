package com.example

import com.example.domain.gamification.GamificationConfig
import com.example.domain.gamification.XpManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test toàn diện cho XpManager (F10 - Quy tắc XP & Chống gian lận).
 * Kiểm tra các ca:
 * 1. Câu hỏi luyện tập đúng: +2 XP, trừ 1 XP khi xem gợi ý, không âm khi trả lời sai
 * 2. Bài kiểm tra: Điểm x 5, thưởng +10 XP khi đạt >= 8.0 điểm
 * 3. Giới hạn trần XP mỗi ngày (mặc định 300 XP), cắt giảm chính xác khi chạm trần
 * 4. Chống cày lặp câu hỏi trong cùng một ngày
 * 5. Khóa sự kiện duy nhất chống cộng trùng
 */
class XpManagerTest {

    @Test
    fun testPracticeQuestionXpWithAndWithoutHint() {
        // Trả lời đúng, không dùng gợi ý: +2 XP
        assertEquals(2, XpManager.calculatePracticeQuestionXp(isCorrect = true, usedHint = false))

        // Trả lời đúng, có dùng gợi ý: 2 - 1 = 1 XP
        assertEquals(1, XpManager.calculatePracticeQuestionXp(isCorrect = true, usedHint = true))

        // Trả lời sai, không dùng gợi ý: 0 XP
        assertEquals(0, XpManager.calculatePracticeQuestionXp(isCorrect = false, usedHint = false))

        // Trả lời sai, có xem gợi ý: Vẫn là 0 XP (không bị âm điểm)
        assertEquals(0, XpManager.calculatePracticeQuestionXp(isCorrect = false, usedHint = true))
    }

    @Test
    fun testTestXpCalculationAndHighScoreBonus() {
        // Điểm 10.0 tuyệt đối: (10.0 * 5) + 10 bonus = 60 XP
        assertEquals(60, XpManager.calculateTestXp(10.0f))

        // Điểm 8.0 (chạm mốc thưởng): (8.0 * 5) + 10 bonus = 50 XP
        assertEquals(50, XpManager.calculateTestXp(8.0f))

        // Điểm 8.5: (8.5 * 5 = 42.5 -> 43) + 10 bonus = 53 XP
        assertEquals(53, XpManager.calculateTestXp(8.5f))

        // Điểm 7.9 (dưới mốc thưởng): (7.9 * 5 = 39.5 -> 40) + 0 = 40 XP
        assertEquals(40, XpManager.calculateTestXp(7.9f))

        // Điểm 5.0: (5.0 * 5) = 25 XP
        assertEquals(25, XpManager.calculateTestXp(5.0f))

        // Điểm 0.0: 0 XP
        assertEquals(0, XpManager.calculateTestXp(0.0f))

        // Điểm âm bất thường hoặc vượt 10: được coerce an toàn trong khoảng 0..10
        assertEquals(0, XpManager.calculateTestXp(-2.0f))
        assertEquals(60, XpManager.calculateTestXp(15.0f))
    }

    @Test
    fun testDailyXpCapEnforcement() {
        val dailyMax = 300

        // Khi trong ngày mới kiếm 100 XP, xin cộng 50 XP -> được cấp đủ 50 XP
        assertEquals(50, XpManager.calculateAllowedXp(currentTodayXp = 100, requestedXp = 50, dailyMax = dailyMax))

        // Khi trong ngày đã đạt 290 XP, xin cộng 20 XP -> chỉ được cấp 10 XP còn lại
        assertEquals(10, XpManager.calculateAllowedXp(currentTodayXp = 290, requestedXp = 20, dailyMax = dailyMax))

        // Khi trong ngày đã chạm trần 300 XP -> không được cấp thêm (0 XP)
        assertEquals(0, XpManager.calculateAllowedXp(currentTodayXp = 300, requestedXp = 10, dailyMax = dailyMax))

        // Khi trong ngày vượt trần bất thường -> 0 XP
        assertEquals(0, XpManager.calculateAllowedXp(currentTodayXp = 350, requestedXp = 10, dailyMax = dailyMax))

        // Yêu cầu 0 XP hoặc XP âm -> 0 XP
        assertEquals(0, XpManager.calculateAllowedXp(currentTodayXp = 100, requestedXp = 0, dailyMax = dailyMax))
        assertEquals(0, XpManager.calculateAllowedXp(currentTodayXp = 100, requestedXp = -5, dailyMax = dailyMax))
    }

    @Test
    fun testQuestionRepeatLimitPerDay() {
        val limit = GamificationConfig.MAX_REPEATS_PER_QUESTION_PER_DAY // 3

        assertTrue(XpManager.isQuestionRepeatAllowed(timesAttemptedToday = 0, maxLimit = limit))
        assertTrue(XpManager.isQuestionRepeatAllowed(timesAttemptedToday = 1, maxLimit = limit))
        assertTrue(XpManager.isQuestionRepeatAllowed(timesAttemptedToday = 2, maxLimit = limit))

        // Làm lại lần thứ 3 (đã làm 3 lần trước đó) -> Không cho cày thêm
        assertFalse(XpManager.isQuestionRepeatAllowed(timesAttemptedToday = 3, maxLimit = limit))
        assertFalse(XpManager.isQuestionRepeatAllowed(timesAttemptedToday = 5, maxLimit = limit))
    }

    @Test
    fun testOtherActivitiesStandardXpAmounts() {
        assertEquals(10, XpManager.calculateLessonXp())
        assertEquals(5, XpManager.calculatePracticeSessionBonus())
        assertEquals(1, XpManager.calculateFlashcardReviewXp())
        assertEquals(3, XpManager.calculateWrongQuestionMasteredXp())
    }

    @Test
    fun testEventKeyGenerationFormats() {
        assertEquals("lesson_les_1", XpManager.makeLessonEventKey("les_1"))
        assertEquals("test_t_uuid", XpManager.makeTestEventKey("t_uuid"))
        assertEquals("wrong_mastered_q_99", XpManager.makeWrongMasteredEventKey("q_99"))
        assertEquals("practice_q_q1_1000", XpManager.makePracticeQuestionEventKey("q1", 1000L))
        assertEquals("flashcard_c1_2000", XpManager.makeFlashcardReviewEventKey("c1", 2000L))
    }
}
