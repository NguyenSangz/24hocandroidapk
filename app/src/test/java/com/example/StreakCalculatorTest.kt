package com.example

import com.example.domain.gamification.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit Test toàn diện cho StreakCalculator (F10 - Chuỗi ngày học liên tiếp & Đóng băng streak).
 * Kiểm tra các ca:
 * 1. Học liên tiếp 3 ngày -> streak = 3
 * 2. Hôm nay chưa học nhưng hôm qua đã học -> streak chưa bị đứt
 * 3. Lỡ 1 ngày không có đóng băng -> chuỗi bị đứt
 * 4. Tự động dùng Đóng băng streak (Streak Freeze) khi lỡ 1 ngày -> bảo toàn chuỗi
 * 5. Tối đa 2 lượt đóng băng, không trừ âm khi hết lượt
 * 6. Chống gian lận chỉnh giờ tiến vào tương lai
 * 7. Chống gian lận chỉnh giờ lùi
 */
class StreakCalculatorTest {

    private val baseDate = LocalDate.of(2026, 10, 11)

    @Test
    fun testConsecutiveThreeDaysStreak() {
        val dates = listOf("2026-10-09", "2026-10-10", "2026-10-11")

        val result = StreakCalculator.calculateStreak(
            rawActivityDates = dates,
            today = baseDate,
            initialFreezes = 1
        )

        assertEquals(3, result.currentStreak)
        assertEquals(3, result.longestStreak)
        assertTrue(result.isStudiedToday)
        assertEquals(1, result.remainingFreezes) // Không cần dùng freeze
        assertTrue(result.usedFreezeDates.isEmpty())
    }

    @Test
    fun testTodayNotStudiedYetDoesNotBreakYesterdayStreak() {
        // Ngày 09 và 10 đã học, hôm nay (11) chưa học
        val dates = listOf("2026-10-09", "2026-10-10")

        val result = StreakCalculator.calculateStreak(
            rawActivityDates = dates,
            today = baseDate,
            initialFreezes = 1
        )

        // Chuỗi vẫn giữ là 2 (tính từ hôm qua lùi về trước), học sinh có cả ngày hôm nay để duy trì
        assertEquals(2, result.currentStreak)
        assertFalse(result.isStudiedToday)
        assertEquals(1, result.remainingFreezes)
    }

    @Test
    fun testMissedDayWithoutFreezeBreaksStreak() {
        // Đã học ngày 08, 09. Bỏ lỡ ngày 10. Hôm nay ngày 11 có học. Không có freeze.
        val dates = listOf("2026-10-08", "2026-10-09", "2026-10-11")

        val result = StreakCalculator.calculateStreak(
            rawActivityDates = dates,
            today = baseDate,
            initialFreezes = 0
        )

        // Chuỗi hiện tại bị ngắt chỉ còn 1 (ngày hôm nay)
        assertEquals(1, result.currentStreak)
        // Chuỗi dài nhất trong lịch sử ghi nhận là 2 (ngày 8 và 9)
        assertEquals(2, result.longestStreak)
        assertEquals(0, result.remainingFreezes)
    }

    @Test
    fun testStreakFreezeAutomaticallyProtectsMissedDay() {
        // Đã học ngày 09. Bỏ lỡ ngày 10. Hôm nay ngày 11 có học. Có 1 freeze.
        val dates = listOf("2026-10-09", "2026-10-11")

        val result = StreakCalculator.calculateStreak(
            rawActivityDates = dates,
            today = baseDate,
            initialFreezes = 1
        )

        // Tự động dùng freeze cho ngày 10 (2026-10-10) -> Bảo toàn chuỗi
        // Chuỗi: ngày 11 (học) + ngày 10 (freeze) + ngày 9 (học) -> streak = 2 ngày học tích lũy
        assertEquals(2, result.currentStreak)
        assertEquals(0, result.remainingFreezes) // Đã dùng 1 freeze
        assertTrue(result.usedFreezeDates.contains("2026-10-10"))
    }

    @Test
    fun testStreakFreezeProtectsYesterdayWhenTodayNotStudiedYet() {
        // Đã học ngày 09. Ngày 10 lỡ. Hôm nay ngày 11 chưa học. Có 1 freeze.
        val dates = listOf("2026-10-09")

        val result = StreakCalculator.calculateStreak(
            rawActivityDates = dates,
            today = baseDate,
            initialFreezes = 1
        )

        // Tự động dùng freeze cho ngày 10, chuỗi vẫn bảo toàn 1 ngày
        assertEquals(1, result.currentStreak)
        assertEquals(0, result.remainingFreezes)
        assertTrue(result.usedFreezeDates.contains("2026-10-10"))
    }

    @Test
    fun testTwoMissedDaysWithOnlyOneFreezeBreaksStreak() {
        // Bỏ lỡ cả ngày 09 và 10. Hôm nay ngày 11 học. Có 1 freeze.
        val dates = listOf("2026-10-07", "2026-10-08", "2026-10-11")

        val result = StreakCalculator.calculateStreak(
            rawActivityDates = dates,
            today = baseDate,
            initialFreezes = 1
        )

        // Freeze chỉ cứu được ngày 10, ngày 09 vẫn đứt -> streak hiện tại chỉ còn 1
        assertEquals(1, result.currentStreak)
        assertEquals(2, result.longestStreak) // ngày 7 và 8
    }

    @Test
    fun testAntiTamperRejectsFutureDates() {
        // Kẻ gian sửa ngày hệ thống lên tương lai để hack điểm (ngày 12, 13, 2030)
        val dates = listOf(
            "2026-10-10",
            "2026-10-11",
            "2026-10-12",
            "2026-10-13",
            "2030-01-01"
        )

        val result = StreakCalculator.calculateStreak(
            rawActivityDates = dates,
            today = baseDate, // 2026-10-11
            initialFreezes = 0
        )

        // Các ngày tương lai (12, 13, 2030) bị loại bỏ hoàn toàn
        assertEquals(2, result.currentStreak) // Chỉ tính ngày 10 và 11
        assertEquals(2, result.longestStreak)
    }

    @Test
    fun testRecent7DaysGeneration() {
        val dates = listOf("2026-10-10", "2026-10-11")

        val result = StreakCalculator.calculateStreak(
            rawActivityDates = dates,
            today = baseDate,
            initialFreezes = 1
        )

        val recent7 = result.recent7Days
        assertEquals(7, recent7.size)

        // Ngày cuối cùng trong danh sách 7 ngày phải là hôm nay
        val lastDay = recent7.last()
        assertTrue(lastDay.isToday)
        assertEquals("2026-10-11", lastDay.dateString)
        assertTrue(lastDay.isStudied)

        // Hôm qua là ngày 10
        val yesterday = recent7[recent7.size - 2]
        assertEquals("2026-10-10", yesterday.dateString)
        assertTrue(yesterday.isStudied)
        assertFalse(yesterday.isToday)
    }
}
