package com.example.domain.gamification

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Trạng thái của một ngày học tập hiển thị trên lịch
 */
data class DayActivityStatus(
    val dateString: String, // YYYY-MM-DD
    val dayOfMonth: Int,
    val dayOfWeekText: String, // T2, T3, T4, T5, T6, T7, CN
    val isToday: Boolean,
    val isStudied: Boolean,
    val isFrozen: Boolean
)

/**
 * Kết quả tính toán chuỗi ngày học liên tiếp thuần túy
 */
data class StreakResult(
    val currentStreak: Int,
    val longestStreak: Int,
    val isStudiedToday: Boolean,
    val remainingFreezes: Int,
    val usedFreezeDates: Set<String>,
    val recent7Days: List<DayActivityStatus>
)

/**
 * Lớp logic thuần tính toán Streak và Đóng băng streak (F10).
 * Hoàn toàn không lưu số đếm cứng, tính trực tiếp từ danh sách ngày có hoạt động.
 * Chống gian lận khi tua giờ tiến/lùi.
 */
object StreakCalculator {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Tính toán chuỗi streak hiện tại và chuỗi dài nhất từ lịch sử các ngày có hoạt động.
     *
     * @param rawActivityDates Danh sách chuỗi "YYYY-MM-DD" có hoạt động học tập hợp lệ.
     * @param today Ngày hiện tại theo múi giờ thiết bị.
     * @param initialFreezes Số lượt đóng băng đang có trước khi tự động dùng (0..2).
     * @param alreadyUsedFreezeDates Các ngày đã từng dùng đóng băng trước đó nếu có.
     */
    fun calculateStreak(
        rawActivityDates: Collection<String>,
        today: LocalDate = LocalDate.now(),
        initialFreezes: Int = 0,
        alreadyUsedFreezeDates: Set<String> = emptySet()
    ): StreakResult {
        // Lọc ngày hợp lệ và loại bỏ ngày trong tương lai (chống gian lận chỉnh giờ tiến)
        val validDates = rawActivityDates.mapNotNull { parseDateSafely(it) }
            .filter { !it.isAfter(today) }
            .toSet()

        val isStudiedToday = validDates.contains(today)
        var freezesLeft = initialFreezes.coerceIn(0, GamificationConfig.MAX_STREAK_FREEZES)
        val newlyUsedFreezeDates = alreadyUsedFreezeDates.toMutableSet()

        // === 1. TÍNH CHUỖI HIỆN TẠI (CURRENT STREAK) ===
        var currentStreak = 0
        // Điểm bắt đầu xét lùi:
        var checkDate = if (isStudiedToday) {
            currentStreak = 1
            today.minusDays(1)
        } else {
            // Hôm nay chưa học: kiểm tra hôm qua
            today.minusDays(1)
        }

        val earliestStudiedDate = validDates.minOrNull()
        var canContinue = true
        while (canContinue && earliestStudiedDate != null) {
            val dateStr = checkDate.format(DATE_FORMATTER)
            if (validDates.contains(checkDate)) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            } else if (newlyUsedFreezeDates.contains(dateStr)) {
                // Ngày này đã được đóng băng từ trước -> giữ chuỗi, không tăng điểm
                checkDate = checkDate.minusDays(1)
            } else if (freezesLeft > 0 && checkDate.isAfter(earliestStudiedDate)) {
                // Tự động dùng 1 lượt đóng băng streak để bảo vệ chuỗi
                freezesLeft--
                newlyUsedFreezeDates.add(dateStr)
                checkDate = checkDate.minusDays(1)
            } else {
                canContinue = false
            }
        }

        // === 2. TÍNH CHUỖI DÀI NHẤT (LONGEST STREAK) ===
        val sortedDates = validDates.sorted()
        var longestStreak = 0
        var tempStreak = 0
        var prevDate: LocalDate? = null

        for (d in sortedDates) {
            if (prevDate == null) {
                tempStreak = 1
            } else {
                val diffDays = ChronoUnit.DAYS.between(prevDate, d)
                when (diffDays) {
                    1L -> {
                        tempStreak++
                    }
                    2L -> {
                        // Nếu lỡ đúng 1 ngày và ngày đó có dùng freeze
                        val missedStr = prevDate.plusDays(1).format(DATE_FORMATTER)
                        if (newlyUsedFreezeDates.contains(missedStr)) {
                            tempStreak++
                        } else {
                            tempStreak = 1
                        }
                    }
                    else -> {
                        tempStreak = 1
                    }
                }
            }
            if (tempStreak > longestStreak) {
                longestStreak = tempStreak
            }
            prevDate = d
        }

        // Đảm bảo longestStreak >= currentStreak
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak
        }

        // === 3. DỰNG DANH SÁCH 7 NGÀY GẦN NHẤT ===
        val recent7 = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val dateStr = date.format(DATE_FORMATTER)
            DayActivityStatus(
                dateString = dateStr,
                dayOfMonth = date.dayOfMonth,
                dayOfWeekText = getDayOfWeekVietnamese(date.dayOfWeek.value),
                isToday = daysAgo == 0,
                isStudied = validDates.contains(date),
                isFrozen = newlyUsedFreezeDates.contains(dateStr)
            )
        }

        return StreakResult(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            isStudiedToday = isStudiedToday,
            remainingFreezes = freezesLeft,
            usedFreezeDates = newlyUsedFreezeDates,
            recent7Days = recent7
        )
    }

    /**
     * Chuyển đổi an toàn chuỗi ngày sang LocalDate
     */
    fun parseDateSafely(dateStr: String): LocalDate? {
        return try {
            LocalDate.parse(dateStr.trim(), DATE_FORMATTER)
        } catch (_: Exception) {
            null
        }
    }

    private fun getDayOfWeekVietnamese(dayOfWeekValue: Int): String {
        return when (dayOfWeekValue) {
            1 -> "T2"
            2 -> "T3"
            3 -> "T4"
            4 -> "T5"
            5 -> "T6"
            6 -> "T7"
            7 -> "CN"
            else -> ""
        }
    }
}
