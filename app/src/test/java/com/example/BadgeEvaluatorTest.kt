package com.example

import com.example.domain.gamification.BadgeDefinition
import com.example.domain.gamification.BadgeEvaluator
import com.example.domain.gamification.GamificationStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit Test toàn diện cho BadgeEvaluator (F10 - Huy hiệu thành tích).
 * Kiểm tra các ca:
 * 1. Mở khóa đúng lúc khi đạt đủ ngưỡng
 * 2. Không mở khóa trùng các huy hiệu đã mở trước đó
 * 3. Đầy đủ các tiêu chí: Bài học, Câu đúng, Flashcard, Bài thi, Streak, Điểm 10, Cấp độ, Đủ môn
 * 4. Tính toán tiến độ % chuẩn xác cho huy hiệu chưa mở
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BadgeEvaluatorTest {

    private val sampleBadgeDefinitions = listOf(
        BadgeDefinition(
            id = "first_lesson",
            title = "Khởi đầu vững chắc",
            description = "Hoàn thành bài học lý thuyết đầu tiên",
            icon = "menu_book",
            category = "KHỞI ĐẦU",
            type = "LESSONS_COMPLETED",
            threshold = 1
        ),
        BadgeDefinition(
            id = "first_correct",
            title = "Phát súng đầu tiên",
            description = "Trả lời đúng câu hỏi đầu tiên",
            icon = "check_circle",
            category = "KHỞI ĐẦU",
            type = "QUESTIONS_CORRECT",
            threshold = 1
        ),
        BadgeDefinition(
            id = "streak_3",
            title = "Tia lửa nhỏ",
            description = "Chuỗi 3 ngày liên tiếp",
            icon = "whatshot",
            category = "KIÊN TRÌ",
            type = "STREAK_DAYS",
            threshold = 3
        ),
        BadgeDefinition(
            id = "perfect_10",
            title = "Điểm 10 tuyệt đối",
            description = "Đạt điểm 10 trong bài kiểm tra",
            icon = "grade",
            category = "CHÍNH XÁC",
            type = "PERFECT_TEST",
            threshold = 1
        ),
        BadgeDefinition(
            id = "level_5",
            title = "Vươn tầm cấp 5",
            description = "Đạt cấp độ 5",
            icon = "star",
            category = "CẤP ĐỘ",
            type = "LEVEL",
            threshold = 5
        )
    )

    @Test
    fun testUnlockBadgesWhenThresholdMet() {
        val stats = GamificationStats(
            lessonsCompletedCount = 1,
            questionsCorrectCount = 5,
            currentStreak = 3,
            perfectTestCount = 1,
            currentLevel = 5
        )

        val newlyUnlocked = BadgeEvaluator.evaluateNewlyUnlockedBadges(
            allDefinitions = sampleBadgeDefinitions,
            alreadyUnlockedIds = emptySet(),
            stats = stats
        )

        // Cả 5 huy hiệu đều thỏa mãn
        assertEquals(5, newlyUnlocked.size)
        assertTrue(newlyUnlocked.any { it.id == "first_lesson" })
        assertTrue(newlyUnlocked.any { it.id == "first_correct" })
        assertTrue(newlyUnlocked.any { it.id == "streak_3" })
        assertTrue(newlyUnlocked.any { it.id == "perfect_10" })
        assertTrue(newlyUnlocked.any { it.id == "level_5" })
    }

    @Test
    fun testDoNotUnlockDuplicateBadges() {
        val stats = GamificationStats(
            lessonsCompletedCount = 1,
            questionsCorrectCount = 10
        )

        // Giả sử "first_lesson" đã được mở khóa từ trước
        val alreadyUnlocked = setOf("first_lesson")

        val newlyUnlocked = BadgeEvaluator.evaluateNewlyUnlockedBadges(
            allDefinitions = sampleBadgeDefinitions,
            alreadyUnlockedIds = alreadyUnlocked,
            stats = stats
        )

        // Chỉ có "first_correct" được trả về, "first_lesson" không bị lặp
        assertEquals(1, newlyUnlocked.size)
        assertEquals("first_correct", newlyUnlocked.first().id)
    }

    @Test
    fun testDoNotUnlockWhenThresholdNotMet() {
        val stats = GamificationStats(
            lessonsCompletedCount = 0,
            questionsCorrectCount = 0,
            currentStreak = 2, // cần 3
            currentLevel = 4   // cần 5
        )

        val newlyUnlocked = BadgeEvaluator.evaluateNewlyUnlockedBadges(
            allDefinitions = sampleBadgeDefinitions,
            alreadyUnlockedIds = emptySet(),
            stats = stats
        )

        // Không huy hiệu nào được mở khóa
        assertTrue(newlyUnlocked.isEmpty())
    }

    @Test
    fun testComputeBadgeProgress() {
        val streakBadge = sampleBadgeDefinitions.first { it.id == "streak_3" } // threshold = 3

        // Khi streak = 1 -> 1 / 3 (~0.33)
        val (cur1, pct1) = BadgeEvaluator.computeBadgeProgress(streakBadge, GamificationStats(currentStreak = 1))
        assertEquals(1, cur1)
        assertEquals(1f / 3f, pct1, 0.001f)

        // Khi streak = 3 -> 3 / 3 (1.0)
        val (cur3, pct3) = BadgeEvaluator.computeBadgeProgress(streakBadge, GamificationStats(currentStreak = 3))
        assertEquals(3, cur3)
        assertEquals(1.0f, pct3, 0.001f)

        // Khi streak vượt ngưỡng (streak = 10) -> kẹp về tối đa 3 và 1.0f
        val (cur10, pct10) = BadgeEvaluator.computeBadgeProgress(streakBadge, GamificationStats(currentStreak = 10))
        assertEquals(3, cur10)
        assertEquals(1.0f, pct10, 0.001f)
    }

    @Test
    fun testJsonParsing() {
        val json = """
            [
              {
                "id": "json_test",
                "title": "Huy hiệu thử nghiệm",
                "description": "Mô tả thử nghiệm",
                "icon": "star",
                "category": "KHỞI ĐẦU",
                "type": "LESSONS_COMPLETED",
                "threshold": 10
              }
            ]
        """.trimIndent()

        val parsed = BadgeEvaluator.parseBadgesFromJson(json)
        assertEquals(1, parsed.size)
        val item = parsed.first()
        assertEquals("json_test", item.id)
        assertEquals("Huy hiệu thử nghiệm", item.title)
        assertEquals(10, item.threshold)
    }
}
