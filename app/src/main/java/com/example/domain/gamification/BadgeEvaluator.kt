package com.example.domain.gamification

import org.json.JSONArray

/**
 * Lớp logic thuần đánh giá điều kiện mở khóa huy hiệu (F10).
 * Không mở khóa trùng, độc lập với cơ sở dữ liệu.
 */
object BadgeEvaluator {

    /**
     * Phân tích tệp JSON cấu hình danh sách huy hiệu
     */
    fun parseBadgesFromJson(jsonString: String): List<BadgeDefinition> {
        val badges = mutableListOf<BadgeDefinition>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                badges.add(
                    BadgeDefinition(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        description = obj.getString("description"),
                        icon = obj.getString("icon"),
                        category = obj.getString("category"),
                        type = obj.getString("type"),
                        threshold = obj.getInt("threshold")
                    )
                )
            }
        } catch (_: Exception) {}
        return badges
    }

    /**
     * Đánh giá và trả về danh sách các huy hiệu MỚI được mở khóa.
     * Bỏ qua các huy hiệu đã có trong [alreadyUnlockedIds].
     */
    fun evaluateNewlyUnlockedBadges(
        allDefinitions: List<BadgeDefinition>,
        alreadyUnlockedIds: Set<String>,
        stats: GamificationStats
    ): List<BadgeDefinition> {
        val newlyUnlocked = mutableListOf<BadgeDefinition>()

        for (def in allDefinitions) {
            if (alreadyUnlockedIds.contains(def.id)) {
                continue
            }

            val isSatisfied = when (def.type) {
                "LESSONS_COMPLETED" -> stats.lessonsCompletedCount >= def.threshold
                "QUESTIONS_CORRECT" -> stats.questionsCorrectCount >= def.threshold
                "FLASHCARDS_REVIEWED" -> stats.flashcardsReviewedCount >= def.threshold
                "TESTS_COMPLETED" -> stats.testsCompletedCount >= def.threshold
                "STREAK_DAYS" -> stats.currentStreak >= def.threshold
                "CHAPTERS_COMPLETED" -> stats.chaptersCompletedCount >= def.threshold
                "PERFECT_TEST" -> stats.perfectTestCount >= def.threshold
                "CONSECUTIVE_CORRECT" -> stats.maxConsecutiveCorrectInSession >= def.threshold
                "HIGH_SCORE_TESTS" -> stats.highScoreTestsCount >= def.threshold
                "WRONG_MASTERED" -> stats.wrongMasteredCount >= def.threshold
                "LEVEL" -> stats.currentLevel >= def.threshold
                "SUBJECTS_STUDIED" -> stats.subjectsStudiedCount >= def.threshold
                else -> false
            }

            if (isSatisfied) {
                newlyUnlocked.add(def)
            }
        }

        return newlyUnlocked
    }

    /**
     * Tính toán tiến độ % của từng huy hiệu (cho UI hiển thị huy hiệu chưa mở)
     */
    fun computeBadgeProgress(
        definition: BadgeDefinition,
        stats: GamificationStats
    ): Pair<Int, Float> {
        val currentVal = when (definition.type) {
            "LESSONS_COMPLETED" -> stats.lessonsCompletedCount
            "QUESTIONS_CORRECT" -> stats.questionsCorrectCount
            "FLASHCARDS_REVIEWED" -> stats.flashcardsReviewedCount
            "TESTS_COMPLETED" -> stats.testsCompletedCount
            "STREAK_DAYS" -> stats.currentStreak
            "CHAPTERS_COMPLETED" -> stats.chaptersCompletedCount
            "PERFECT_TEST" -> stats.perfectTestCount
            "CONSECUTIVE_CORRECT" -> stats.maxConsecutiveCorrectInSession
            "HIGH_SCORE_TESTS" -> stats.highScoreTestsCount
            "WRONG_MASTERED" -> stats.wrongMasteredCount
            "LEVEL" -> stats.currentLevel
            "SUBJECTS_STUDIED" -> stats.subjectsStudiedCount
            else -> 0
        }

        val threshold = definition.threshold.coerceAtLeast(1)
        val progress = (currentVal.toFloat() / threshold.toFloat()).coerceIn(0f, 1f)
        return Pair(currentVal.coerceAtMost(threshold), progress)
    }
}
