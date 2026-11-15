package com.example

import com.example.domain.gamification.GamificationConfig
import com.example.domain.gamification.LevelBadgeGroup
import com.example.domain.gamification.LevelCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test toàn diện cho LevelCalculator (F10 - Cấp độ và tiến trình thăng hạng).
 * Kiểm tra các ca:
 * 1. Biên mỗi cấp (cấp 1, 2, 3, 5, 10, 50)
 * 2. Cấp 1 khi 0 XP hoặc XP âm
 * 3. Tiến độ % chính xác giữa các cấp
 * 4. Chạm trần cấp 50 (max level cap)
 * 5. Tên danh xưng và nhóm biểu tượng chính xác
 */
class LevelCalculatorTest {

    @Test
    fun testXpRequiredForLevelBoundaries() {
        assertEquals(0, LevelCalculator.xpRequiredForLevel(1))
        assertEquals(100, LevelCalculator.xpRequiredForLevel(2)) // 100 * 1^1.5 = 100
        assertEquals(282, LevelCalculator.xpRequiredForLevel(3)) // 100 * 2^1.5 = 282.84 -> 282
        assertEquals(800, LevelCalculator.xpRequiredForLevel(5)) // 100 * 4^1.5 = 800
        assertEquals(2700, LevelCalculator.xpRequiredForLevel(10)) // 100 * 9^1.5 = 2700
        assertEquals(34300, LevelCalculator.xpRequiredForLevel(50)) // 100 * 49^1.5 = 34300
    }

    @Test
    fun testNegativeXpHandledSafely() {
        val info = LevelCalculator.calculateLevel(-50)
        assertEquals(1, info.level)
        assertEquals(0, info.totalXp)
        assertEquals(0.0f, info.progress, 0.001f)
        assertEquals(100, info.xpRequiredForNextLevel)
        assertFalse(info.isMaxLevel)
    }

    @Test
    fun testZeroXpIsLevel1() {
        val info = LevelCalculator.calculateLevel(0)
        assertEquals(1, info.level)
        assertEquals(0, info.totalXp)
        assertEquals(0, info.currentLevelBaseXp)
        assertEquals(100, info.nextLevelTargetXp)
        assertEquals(0.0f, info.progress, 0.001f)
        assertEquals(100, info.xpRequiredForNextLevel)
        assertEquals(LevelBadgeGroup.APPRENTICE, info.badgeGroup)
        assertFalse(info.isMaxLevel)
    }

    @Test
    fun testIntermediateXpProgress() {
        // Cấp 1 cần 0, cấp 2 cần 100. Đạt 50 XP -> tiến độ 50%
        val info50 = LevelCalculator.calculateLevel(50)
        assertEquals(1, info50.level)
        assertEquals(0.5f, info50.progress, 0.001f)
        assertEquals(50, info50.xpRequiredForNextLevel)

        // Cấp 2: từ 100 đến 282 (khoảng cách 182 XP). Có 191 XP -> 91 / 182 = 0.5f
        val info191 = LevelCalculator.calculateLevel(191)
        assertEquals(2, info191.level)
        assertEquals(100, info191.currentLevelBaseXp)
        assertEquals(282, info191.nextLevelTargetXp)
        assertEquals(0.5f, info191.progress, 0.01f)
        assertEquals(91, info191.xpRequiredForNextLevel)
    }

    @Test
    fun testExactLevelBoundaryTransitions() {
        // 99 XP: Cấp 1
        val at99 = LevelCalculator.calculateLevel(99)
        assertEquals(1, at99.level)

        // 100 XP: Lên Cấp 2 ngay lập tức
        val at100 = LevelCalculator.calculateLevel(100)
        assertEquals(2, at100.level)
        assertEquals(0.0f, at100.progress, 0.001f)

        // 281 XP: Cấp 2
        val at281 = LevelCalculator.calculateLevel(281)
        assertEquals(2, at281.level)

        // 282 XP: Cấp 3
        val at282 = LevelCalculator.calculateLevel(282)
        assertEquals(3, at282.level)
        assertEquals(0.0f, at282.progress, 0.001f)
    }

    @Test
    fun testMaxLevelCapAt50() {
        val req50 = LevelCalculator.xpRequiredForLevel(50) // 34300
        val atMax = LevelCalculator.calculateLevel(req50)
        assertEquals(50, atMax.level)
        assertTrue(atMax.isMaxLevel)
        assertEquals(1.0f, atMax.progress, 0.001f)
        assertEquals(0, atMax.xpRequiredForNextLevel)
        assertEquals(LevelBadgeGroup.LEGEND, atMax.badgeGroup)

        // Vượt xa mốc cấp 50 (ví dụ 100,000 XP) vẫn giữ cấp 50 và 100%
        val wayAbove = LevelCalculator.calculateLevel(100000)
        assertEquals(50, wayAbove.level)
        assertTrue(wayAbove.isMaxLevel)
        assertEquals(1.0f, wayAbove.progress, 0.001f)
        assertEquals(0, wayAbove.xpRequiredForNextLevel)
    }

    @Test
    fun testBadgeGroupsDistribution() {
        assertEquals(LevelBadgeGroup.APPRENTICE, LevelCalculator.getBadgeGroupForLevel(1))
        assertEquals(LevelBadgeGroup.APPRENTICE, LevelCalculator.getBadgeGroupForLevel(5))
        assertEquals(LevelBadgeGroup.DILIGENT, LevelCalculator.getBadgeGroupForLevel(6))
        assertEquals(LevelBadgeGroup.DILIGENT, LevelCalculator.getBadgeGroupForLevel(10))
        assertEquals(LevelBadgeGroup.ENERGETIC, LevelCalculator.getBadgeGroupForLevel(11))
        assertEquals(LevelBadgeGroup.ENERGETIC, LevelCalculator.getBadgeGroupForLevel(15))
        assertEquals(LevelBadgeGroup.PERSISTENT, LevelCalculator.getBadgeGroupForLevel(16))
        assertEquals(LevelBadgeGroup.PERSISTENT, LevelCalculator.getBadgeGroupForLevel(20))
        assertEquals(LevelBadgeGroup.WISE, LevelCalculator.getBadgeGroupForLevel(25))
        assertEquals(LevelBadgeGroup.SCHOLAR, LevelCalculator.getBadgeGroupForLevel(35))
        assertEquals(LevelBadgeGroup.LEGEND, LevelCalculator.getBadgeGroupForLevel(50))
    }
}
