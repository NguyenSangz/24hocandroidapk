package com.example.domain.gamification

import kotlin.math.roundToInt

/**
 * Lớp logic quản lý tính toán lượng XP, giới hạn hàng ngày và chống gian lận (F10).
 * Hoàn toàn là hàm thuần, kiểm thử được 100%.
 */
object XpManager {

    /**
     * Tính XP cho một câu hỏi luyện tập:
     * - Trả lời đúng: +2 XP
     * - Trả lời đúng nhưng dùng gợi ý: 2 - 1 = 1 XP
     * - Trả lời sai: 0 XP (không âm kể cả khi có gợi ý)
     */
    fun calculatePracticeQuestionXp(isCorrect: Boolean, usedHint: Boolean): Int {
        if (!isCorrect) return 0
        val base = GamificationConfig.XP_PRACTICE_CORRECT
        val penalty = if (usedHint) GamificationConfig.XP_PRACTICE_HINT_PENALTY else 0
        return (base - penalty).coerceAtLeast(0)
    }

    /**
     * Tính XP cho bài kiểm tra:
     * - Điểm thang 10 nhân với 5 (làm tròn số nguyên gần nhất)
     * - Đạt từ 8.0 điểm trở lên thưởng thêm +10 XP
     */
    fun calculateTestXp(score: Float): Int {
        val safeScore = score.coerceIn(0f, 10f)
        var xp = (safeScore * GamificationConfig.XP_TEST_SCORE_MULTIPLIER).roundToInt()
        if (safeScore >= GamificationConfig.TEST_HIGH_SCORE_THRESHOLD) {
            xp += GamificationConfig.XP_TEST_HIGH_SCORE_BONUS
        }
        return xp.coerceAtLeast(0)
    }

    /**
     * XP hoàn thành bài học lý thuyết lần đầu
     */
    fun calculateLessonXp(): Int = GamificationConfig.XP_LESSON_COMPLETED

    /**
     * XP thưởng hoàn thành phiên luyện tập
     */
    fun calculatePracticeSessionBonus(): Int = GamificationConfig.XP_PRACTICE_SESSION_BONUS

    /**
     * XP ôn tập 1 thẻ flashcard
     */
    fun calculateFlashcardReviewXp(): Int = GamificationConfig.XP_FLASHCARD_REVIEW

    /**
     * XP khi câu sai chuyển sang trạng thái Đã thuộc
     */
    fun calculateWrongQuestionMasteredXp(): Int = GamificationConfig.XP_WRONG_QUESTION_MASTERED

    /**
     * Kiểm tra và giới hạn XP được phép cộng thực tế theo trần tối đa trong ngày (mặc định 300 XP).
     * Trả về số XP thực tế được phép cộng (có thể bị cắt bớt nếu sắp chạm trần, hoặc 0 nếu đã chạm trần).
     */
    fun calculateAllowedXp(
        currentTodayXp: Int,
        requestedXp: Int,
        dailyMax: Int = GamificationConfig.DAILY_MAX_XP
    ): Int {
        if (requestedXp <= 0) return 0
        val remainingQuota = (dailyMax - currentTodayXp).coerceAtLeast(0)
        return requestedXp.coerceAtMost(remainingQuota)
    }

    /**
     * Kiểm tra chống cày điểm lặp: không cộng XP nếu làm lại cùng 1 câu hỏi quá số lần quy định trong 1 ngày
     */
    fun isQuestionRepeatAllowed(
        timesAttemptedToday: Int,
        maxLimit: Int = GamificationConfig.MAX_REPEATS_PER_QUESTION_PER_DAY
    ): Boolean {
        return timesAttemptedToday < maxLimit
    }

    // === TẠO KHÓA DUY NHẤT CHỐNG CỘNG TRÙNG SỰ KIỆN ===

    fun makeLessonEventKey(lessonId: String): String {
        return "lesson_${lessonId.trim()}"
    }

    fun makePracticeQuestionEventKey(questionId: String, attemptTimestamp: Long): String {
        return "practice_q_${questionId.trim()}_$attemptTimestamp"
    }

    fun makePracticeSessionBonusEventKey(sessionId: String): String {
        return "practice_session_bonus_${sessionId.trim()}"
    }

    fun makeTestEventKey(testId: String): String {
        return "test_${testId.trim()}"
    }

    fun makeFlashcardReviewEventKey(cardId: String, reviewTimestamp: Long): String {
        return "flashcard_${cardId.trim()}_$reviewTimestamp"
    }

    fun makeWrongMasteredEventKey(questionId: String): String {
        return "wrong_mastered_${questionId.trim()}"
    }

    fun makeStreakFreezePurchaseKey(timestamp: Long): String {
        return "freeze_purchase_$timestamp"
    }
}
