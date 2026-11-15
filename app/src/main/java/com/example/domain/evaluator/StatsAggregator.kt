package com.example.domain.evaluator

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Điểm dữ liệu cho cột thời gian học mỗi ngày
 */
data class DayStudyBarData(
    val dateString: String, // YYYY-MM-DD
    val dayLabel: String, // T2, T3, T4, T5, T6, T7, CN hoặc dd/MM
    val totalSeconds: Long,
    val subjectSegments: Map<String, Long> // subjectId -> seconds trong ngày đó
)

/**
 * Điểm dữ liệu biểu đồ đường điểm thi theo thời gian
 */
data class TestScorePoint(
    val testId: String,
    val title: String,
    val subjectId: String,
    val score: Float,
    val timestamp: Long,
    val formattedDate: String
)

/**
 * Tỉ lệ đúng theo từng môn
 */
data class SubjectAccuracyItem(
    val subjectId: String,
    val subjectName: String,
    val totalAnswered: Int,
    val totalCorrect: Int,
    val accuracyPercent: Float
)

/**
 * Dữ liệu so sánh đa chiều giữa các môn (Radar / Grouped Bar)
 */
data class SubjectComparisonItem(
    val subjectId: String,
    val subjectName: String,
    val accuracyScore: Float, // 0..100
    val lessonProgressScore: Float, // 0..100
    val consistencyScore: Float // 0..100 (độ ổn định phân bố thời gian học)
)

/**
 * Tổng quan chỉ số trong kỳ
 */
data class OverviewMetrics(
    val totalStudySeconds: Long,
    val distinctStudyDays: Int,
    val totalQuestionsAnswered: Int,
    val totalQuestionsCorrect: Int,
    val overallAccuracyPercent: Float,
    val completedLessonsCount: Int,
    val totalLessonsCount: Int,
    val totalXpEarned: Int
)

/**
 * Lớp tính toán tổng hợp thuần túy (StatsAggregator).
 * Không có side-effect, an toàn tuyệt đối với chia cho 0 và múi giờ.
 */
object StatsAggregator {

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM")

    /**
     * Tổng hợp biểu đồ cột thời gian theo ngày từ danh sách phiên học.
     */
    fun aggregateDailyStudyBars(
        sessions: List<com.example.data.local.entity.StudySessionEntity>,
        timeBounds: TimeRangeBounds,
        zoneId: ZoneId = StatsTimeRangeHelper.VIETNAM_ZONE_ID
    ): List<DayStudyBarData> {
        val startLocalDate = Instant.ofEpochMilli(timeBounds.fromMillis.coerceAtLeast(1L)).atZone(zoneId).toLocalDate()
        val endLocalDate = Instant.ofEpochMilli(timeBounds.toMillis).atZone(zoneId).toLocalDate()

        // Giới hạn số ngày hiển thị hợp lý trên mobile: tối đa 30 ngày gần nhất
        val actualStartDate = if (java.time.temporal.ChronoUnit.DAYS.between(startLocalDate, endLocalDate) > 30) {
            endLocalDate.minusDays(29)
        } else {
            startLocalDate
        }

        // Nhóm session theo ngày chuỗi YYYY-MM-DD
        val sessionsByDate = sessions.groupBy { session ->
            Instant.ofEpochMilli(session.startTimestamp).atZone(zoneId).toLocalDate().format(DATE_FORMATTER)
        }

        val result = mutableListOf<DayStudyBarData>()
        var curr = actualStartDate
        while (!curr.isAfter(endLocalDate)) {
            val dateStr = curr.format(DATE_FORMATTER)
            val daySessions = sessionsByDate[dateStr] ?: emptyList()
            val totalSec = daySessions.sumOf { it.durationSeconds.toLong() }

            val segments = mutableMapOf<String, Long>()
            daySessions.forEach { s ->
                segments[s.subjectId] = (segments[s.subjectId] ?: 0L) + s.durationSeconds
            }

            val dayLabel = when (curr.dayOfWeek) {
                java.time.DayOfWeek.MONDAY -> "T2"
                java.time.DayOfWeek.TUESDAY -> "T3"
                java.time.DayOfWeek.WEDNESDAY -> "T4"
                java.time.DayOfWeek.THURSDAY -> "T5"
                java.time.DayOfWeek.FRIDAY -> "T6"
                java.time.DayOfWeek.SATURDAY -> "T7"
                java.time.DayOfWeek.SUNDAY -> "CN"
                else -> curr.format(SHORT_DATE_FORMATTER)
            }

            result.add(
                DayStudyBarData(
                    dateString = dateStr,
                    dayLabel = dayLabel,
                    totalSeconds = totalSec,
                    subjectSegments = segments
                )
            )
            curr = curr.plusDays(1)
        }

        return result
    }

    /**
     * Tính toán tổng quan chỉ số, triệt tiêu lỗi chia cho 0.
     */
    fun calculateOverview(
        totalStudySeconds: Long,
        distinctStudyDays: Int,
        totalQuestionsAnswered: Int,
        totalQuestionsCorrect: Int,
        completedLessonsCount: Int,
        totalLessonsCount: Int,
        totalXp: Int
    ): OverviewMetrics {
        val accuracy = if (totalQuestionsAnswered > 0) {
            (totalQuestionsCorrect.toFloat() / totalQuestionsAnswered.toFloat()) * 100f
        } else {
            0f
        }

        return OverviewMetrics(
            totalStudySeconds = totalStudySeconds.coerceAtLeast(0L),
            distinctStudyDays = distinctStudyDays.coerceAtLeast(0),
            totalQuestionsAnswered = totalQuestionsAnswered.coerceAtLeast(0),
            totalQuestionsCorrect = totalQuestionsCorrect.coerceAtLeast(0),
            overallAccuracyPercent = accuracy.coerceIn(0f, 100f),
            completedLessonsCount = completedLessonsCount.coerceAtLeast(0),
            totalLessonsCount = totalLessonsCount.coerceAtLeast(0),
            totalXpEarned = totalXp.coerceAtLeast(0)
        )
    }

    /**
     * Chuyển đổi điểm kiểm tra thành điểm biểu đồ.
     */
    fun mapTestScorePoints(
        tests: List<com.example.data.local.entity.TestEntity>,
        zoneId: ZoneId = StatsTimeRangeHelper.VIETNAM_ZONE_ID
    ): List<TestScorePoint> {
        return tests.map { test ->
            val dateStr = Instant.ofEpochMilli(test.timestamp).atZone(zoneId).toLocalDate().format(SHORT_DATE_FORMATTER)
            TestScorePoint(
                testId = test.id,
                title = test.title,
                subjectId = test.subjectId,
                score = test.score,
                timestamp = test.timestamp,
                formattedDate = dateStr
            )
        }
    }
}
