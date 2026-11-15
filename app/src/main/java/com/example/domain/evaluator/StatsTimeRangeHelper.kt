package com.example.domain.evaluator

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Khoảng thời gian thống kê (F11).
 */
enum class StatsTimeRange(val displayName: String) {
    LAST_7_DAYS("7 ngày"),
    LAST_30_DAYS("30 ngày"),
    THIS_SEMESTER("Học kỳ này"),
    ALL_TIME("Tất cả")
}

/**
 * Kết quả khoảng mốc thời gian bắt đầu và kết thúc (epoch millis)
 */
data class TimeRangeBounds(
    val fromMillis: Long,
    val toMillis: Long,
    val previousFromMillis: Long,
    val previousToMillis: Long,
    val label: String
)

/**
 * Lớp tính toán khoảng thời gian chuẩn xác theo múi giờ Việt Nam (UTC+7)
 * Tuần bắt đầu từ thứ Hai (DayOfWeek.MONDAY).
 */
object StatsTimeRangeHelper {

    val VIETNAM_ZONE_ID: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

    /**
     * Xác định ranh giới epoch millis cho từng khoảng thời gian và kỳ trước đối chiếu.
     */
    fun getTimeBounds(
        range: StatsTimeRange,
        today: LocalDate = LocalDate.now(VIETNAM_ZONE_ID),
        zoneId: ZoneId = VIETNAM_ZONE_ID
    ): TimeRangeBounds {
        val endOfDay = today.atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()

        return when (range) {
            StatsTimeRange.LAST_7_DAYS -> {
                val startDay = today.minusDays(6)
                val startMillis = startDay.atStartOfDay(zoneId).toInstant().toEpochMilli()
                // Kỳ trước liền kề (7 ngày trước đó)
                val prevStartDay = startDay.minusDays(7)
                val prevEndDay = startDay.minusDays(1)
                val prevStartMillis = prevStartDay.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val prevEndMillis = prevEndDay.atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
                TimeRangeBounds(startMillis, endOfDay, prevStartMillis, prevEndMillis, "7 ngày qua")
            }

            StatsTimeRange.LAST_30_DAYS -> {
                val startDay = today.minusDays(29)
                val startMillis = startDay.atStartOfDay(zoneId).toInstant().toEpochMilli()
                // Kỳ trước liền kề (30 ngày trước đó)
                val prevStartDay = startDay.minusDays(30)
                val prevEndDay = startDay.minusDays(1)
                val prevStartMillis = prevStartDay.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val prevEndMillis = prevEndDay.atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
                TimeRangeBounds(startMillis, endOfDay, prevStartMillis, prevEndMillis, "30 ngày qua")
            }

            StatsTimeRange.THIS_SEMESTER -> {
                // GDPT Việt Nam: Học kỳ 1 (05/09 đến 15/01), Học kỳ 2 (16/01 đến 31/05), Hè (01/06 đến 04/09)
                val year = today.year
                val (semStart, semEnd, prevSemStart, prevSemEnd, title) = if (today.monthValue >= 9) {
                    // HK1 năm học [year] - [year+1]
                    val start = LocalDate.of(year, 9, 5)
                    val end = LocalDate.of(year + 1, 1, 15)
                    // Kỳ trước: HK2 năm học trước (16/01 -> 31/05 năm hiện tại)
                    val pStart = LocalDate.of(year, 1, 16)
                    val pEnd = LocalDate.of(year, 5, 31)
                    Pentad(start, end, pStart, pEnd, "Học kỳ 1 ($year - ${year + 1})")
                } else if (today.monthValue <= 1 && today.dayOfMonth <= 15) {
                    // Đầu tháng 1: Vẫn thuộc HK1 năm học [year-1] - [year]
                    val start = LocalDate.of(year - 1, 9, 5)
                    val end = LocalDate.of(year, 1, 15)
                    val pStart = LocalDate.of(year - 1, 1, 16)
                    val pEnd = LocalDate.of(year - 1, 5, 31)
                    Pentad(start, end, pStart, pEnd, "Học kỳ 1 (${year - 1} - $year)")
                } else if (today.monthValue <= 5) {
                    // HK2 năm học [year-1] - [year]
                    val start = LocalDate.of(year, 1, 16)
                    val end = LocalDate.of(year, 5, 31)
                    // Kỳ trước: HK1 năm học [year-1]
                    val pStart = LocalDate.of(year - 1, 9, 5)
                    val pEnd = LocalDate.of(year, 1, 15)
                    Pentad(start, end, pStart, pEnd, "Học kỳ 2 (${year - 1} - $year)")
                } else {
                    // Kỳ hè (tháng 6 - tháng 8): tính từ đầu hè đến hiện tại
                    val start = LocalDate.of(year, 6, 1)
                    val end = LocalDate.of(year, 8, 31)
                    val pStart = LocalDate.of(year, 1, 16)
                    val pEnd = LocalDate.of(year, 5, 31)
                    Pentad(start, end, pStart, pEnd, "Kỳ hè ($year)")
                }

                val startMillis = semStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val prevStartMillis = prevSemStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val prevEndMillis = prevSemEnd.atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
                TimeRangeBounds(startMillis, endOfDay, prevStartMillis, prevEndMillis, title)
            }

            StatsTimeRange.ALL_TIME -> {
                // Tính từ lúc bắt đầu (0) đến hiện tại
                TimeRangeBounds(0L, endOfDay, 0L, 0L, "Tất cả thời gian")
            }
        }
    }

    private data class Pentad(
        val s1: LocalDate,
        val s2: LocalDate,
        val p1: LocalDate,
        val p2: LocalDate,
        val label: String
    )
}
