package com.example

import com.example.data.local.entity.StudySessionEntity
import com.example.domain.evaluator.StatsAggregator
import com.example.domain.evaluator.StatsTimeRange
import com.example.domain.evaluator.StatsTimeRangeHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Unit Test cho StatsAggregator:
 * 1. Nhóm theo ngày đúng múi giờ Việt Nam.
 * 2. Phân loại theo từng môn.
 * 3. Kỳ rỗng không có phiên nào -> Trả về danh sách ngày 0 giây, không chia cho 0, không NaN.
 */
class StatsAggregatorTest {

    private val vnZone = ZoneId.of("Asia/Ho_Chi_Minh")

    @Test
    fun testEmptyPeriodReturnsZeroMetricsSafely() {
        val overview = StatsAggregator.calculateOverview(
            totalStudySeconds = 0L,
            distinctStudyDays = 0,
            totalQuestionsAnswered = 0,
            totalQuestionsCorrect = 0,
            completedLessonsCount = 0,
            totalLessonsCount = 0,
            totalXp = 0
        )

        assertEquals(0L, overview.totalStudySeconds)
        assertEquals(0, overview.distinctStudyDays)
        assertEquals(0f, overview.overallAccuracyPercent, 0.001f)
        assertFalse(overview.overallAccuracyPercent.isNaN())
    }

    @Test
    fun testGroupingByDayAndSubjectSegments() {
        val testDate = LocalDate.of(2026, 10, 11)
        val timeBounds = StatsTimeRangeHelper.getTimeBounds(StatsTimeRange.LAST_7_DAYS, today = testDate, zoneId = vnZone)

        val day1Millis = LocalDate.of(2026, 10, 10).atTime(10, 0).atZone(vnZone).toInstant().toEpochMilli()
        val day2Millis = LocalDate.of(2026, 10, 11).atTime(14, 0).atZone(vnZone).toInstant().toEpochMilli()

        val sessions = listOf(
            StudySessionEntity(id = 1, activityType = "LESSON", subjectId = "math", grade = 6, startTimestamp = day1Millis, durationSeconds = 1200),
            StudySessionEntity(id = 2, activityType = "PRACTICE", subjectId = "english", grade = 6, startTimestamp = day1Millis + 2000, durationSeconds = 600),
            StudySessionEntity(id = 3, activityType = "TEST", subjectId = "math", grade = 6, startTimestamp = day2Millis, durationSeconds = 1800)
        )

        val bars = StatsAggregator.aggregateDailyStudyBars(sessions, timeBounds, zoneId = vnZone)
        assertEquals(7, bars.size) // 7 ngày

        val bar10 = bars.find { it.dateString == "2026-10-10" }
        assertNotNull(bar10)
        assertEquals(1800L, bar10!!.totalSeconds)
        assertEquals(1200L, bar10.subjectSegments["math"])
        assertEquals(600L, bar10.subjectSegments["english"])

        val bar11 = bars.find { it.dateString == "2026-10-11" }
        assertNotNull(bar11)
        assertEquals(1800L, bar11!!.totalSeconds)
        assertEquals(1800L, bar11.subjectSegments["math"])
    }
}
