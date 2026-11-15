package com.example.domain.gamification

/**
 * Thông tin chi tiết về cấp độ và tiến trình thăng hạng hiện tại
 */
data class LevelInfo(
    val level: Int,
    val title: String,
    val totalXp: Int,
    val currentLevelBaseXp: Int,
    val nextLevelTargetXp: Int,
    val xpIntoCurrentLevel: Int,
    val xpRequiredForNextLevel: Int,
    val progress: Float, // 0.0f .. 1.0f
    val isMaxLevel: Boolean,
    val badgeGroup: LevelBadgeGroup
)

/**
 * Phân nhóm biểu tượng và danh hiệu theo nhóm cấp độ cho lứa tuổi THCS (11-15 tuổi)
 */
enum class LevelBadgeGroup(
    val groupName: String,
    val iconName: String,
    val colorHex: Long
) {
    APPRENTICE("Tập sự", "school", 0xFF10B981),         // Cấp 1 - 5 (Xanh lá non)
    DILIGENT("Chăm chỉ", "auto_stories", 0xFF0284C7),    // Cấp 6 - 10 (Xanh dương)
    ENERGETIC("Năng nổ", "bolt", 0xFF6366F1),          // Cấp 11 - 15 (Xanh tím)
    PERSISTENT("Kiên định", "shield", 0xFF8B5CF6),       // Cấp 16 - 20 (Tím)
    WISE("Thông thái", "psychology", 0xFFDFA34F),       // Cấp 21 - 30 (Cam vàng)
    SCHOLAR("Học giả", "menu_book", 0xFFEF4444),        // Cấp 31 - 40 (Đỏ Ruby)
    LEGEND("Huyền thoại", "military_tech", 0xFFEC4899)  // Cấp 41 - 50 (Hoàng kim hồng)
}

/**
 * Định nghĩa một huy hiệu từ tệp JSON cấu hình
 */
data class BadgeDefinition(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val category: String, // KHỞI ĐẦU, KIÊN TRÌ, CHINH PHỤC, CHÍNH XÁC, ÔN TẬP, CẤP ĐỘ, KHÁM PHÁ
    val type: String,
    val threshold: Int
)

/**
 * Huy hiệu hiển thị lên giao diện kèm trạng thái đã mở khóa
 */
data class BadgeDisplayItem(
    val definition: BadgeDefinition,
    val isUnlocked: Boolean,
    val unlockedAtTimestamp: Long? = null,
    val currentProgress: Int = 0,
    val progressPercentage: Float = 0f
)

/**
 * Dữ liệu thống kê tổng hợp để đánh giá điều kiện mở khóa huy hiệu
 */
data class GamificationStats(
    val totalXp: Int = 0,
    val currentLevel: Int = 1,
    val currentStreak: Int = 0,
    val lessonsCompletedCount: Int = 0,
    val questionsCorrectCount: Int = 0,
    val flashcardsReviewedCount: Int = 0,
    val testsCompletedCount: Int = 0,
    val perfectTestCount: Int = 0,
    val maxConsecutiveCorrectInSession: Int = 0,
    val highScoreTestsCount: Int = 0,
    val chaptersCompletedCount: Int = 0,
    val wrongMasteredCount: Int = 0,
    val subjectsStudiedCount: Int = 0
)

/**
 * Phần thưởng trả về ngay sau khi một sự kiện học tập hoàn tất
 */
data class GamificationReward(
    val xpEarned: Int = 0,
    val previousLevel: Int = 1,
    val newLevelInfo: LevelInfo? = null,
    val isLevelUp: Boolean = false,
    val newlyUnlockedBadges: List<BadgeDefinition> = emptyList(),
    val currentStreak: Int = 0,
    val streakFreezeAvailable: Int = 0,
    val message: String = ""
)

/**
 * Các loại sự kiện học tập nội bộ được phát ra từ các luồng của ứng dụng
 */
sealed class LearningEvent {
    data class LessonCompleted(
        val lessonId: String,
        val chapterId: String = "",
        val subjectId: String = ""
    ) : LearningEvent()

    data class PracticeQuestionAnswered(
        val questionId: String,
        val isCorrect: Boolean,
        val usedHint: Boolean,
        val consecutiveCorrectInSession: Int = 0,
        val subjectId: String? = null
    ) : LearningEvent()

    data class PracticeSessionCompleted(
        val questionCount: Int,
        val correctCount: Int,
        val subjectId: String? = null
    ) : LearningEvent()

    data class TestCompleted(
        val testId: String,
        val score: Float,
        val totalQuestions: Int,
        val correctCount: Int,
        val subjectId: String
    ) : LearningEvent()

    data class FlashcardReviewed(
        val cardId: String,
        val rating: Int, // 0: Lại, 3: Khó, 4: Được, 5: Dễ
        val subjectId: String? = null
    ) : LearningEvent()

    data class WrongQuestionMastered(
        val questionId: String,
        val subjectId: String? = null
    ) : LearningEvent()
}
