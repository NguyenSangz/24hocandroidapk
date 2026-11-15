package com.example.domain.evaluator

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Hướng biến động của chỉ số
 */
enum class TrendDirection {
    INCREASED, // Tăng (tích cực hoặc tăng trưởng)
    DECREASED, // Giảm
    STABLE,    // Ổn định (dao động nhỏ hơn 3%)
    NEW_START  // Mới bắt đầu kỳ này (kỳ trước bằng 0)
}

/**
 * Kết quả so sánh một chỉ số giữa kỳ này và kỳ trước
 */
data class MetricTrend(
    val currentValue: Float,
    val previousValue: Float,
    val deltaPercent: Float,
    val direction: TrendDirection,
    val description: String
)

/**
 * Bộ tính toán xu hướng so sánh kỳ này với kỳ trước (TrendCalculator).
 * Xử lý trường hợp kỳ trước = 0, chia cho 0 an toàn.
 */
object TrendCalculator {

    /**
     * So sánh giá trị thời gian học (giây) giữa 2 kỳ
     */
    fun calculateStudyTimeTrend(currentSeconds: Long, previousSeconds: Long): MetricTrend {
        return calculateTrendInternal(
            current = currentSeconds.toFloat(),
            previous = previousSeconds.toFloat(),
            unitName = "thời gian học"
        )
    }

    /**
     * So sánh tỉ lệ đúng (%) giữa 2 kỳ
     */
    fun calculateAccuracyTrend(currentAccuracy: Float, previousAccuracy: Float): MetricTrend {
        return calculateTrendInternal(
            current = currentAccuracy,
            previous = previousAccuracy,
            unitName = "tỉ lệ đúng",
            tolerancePercent = 2.0f
        )
    }

    /**
     * So sánh số câu hỏi đã làm giữa 2 kỳ
     */
    fun calculateQuestionsTrend(currentCount: Int, previousCount: Int): MetricTrend {
        return calculateTrendInternal(
            current = currentCount.toFloat(),
            previous = previousCount.toFloat(),
            unitName = "số câu hỏi"
        )
    }

    private fun calculateTrendInternal(
        current: Float,
        previous: Float,
        unitName: String,
        tolerancePercent: Float = 3.0f
    ): MetricTrend {
        if (previous <= 0f) {
            return if (current > 0f) {
                MetricTrend(
                    currentValue = current,
                    previousValue = 0f,
                    deltaPercent = 100f,
                    direction = TrendDirection.NEW_START,
                    description = "Mới bắt đầu kỳ này"
                )
            } else {
                MetricTrend(
                    currentValue = 0f,
                    previousValue = 0f,
                    deltaPercent = 0f,
                    direction = TrendDirection.STABLE,
                    description = "Chưa có dữ liệu"
                )
            }
        }

        val rawDeltaPercent = ((current - previous) / previous) * 100f
        val roundedDelta = (rawDeltaPercent * 10f).roundToInt() / 10f

        val direction = when {
            rawDeltaPercent > tolerancePercent -> TrendDirection.INCREASED
            rawDeltaPercent < -tolerancePercent -> TrendDirection.DECREASED
            else -> TrendDirection.STABLE
        }

        val desc = when (direction) {
            TrendDirection.INCREASED -> "Tăng ${abs(roundedDelta)}% so với kỳ trước"
            TrendDirection.DECREASED -> "Giảm ${abs(roundedDelta)}% so với kỳ trước"
            TrendDirection.STABLE -> "Ổn định so với kỳ trước"
            TrendDirection.NEW_START -> "Mới bắt đầu kỳ này"
        }

        return MetricTrend(
            currentValue = current,
            previousValue = previous,
            deltaPercent = roundedDelta,
            direction = direction,
            description = desc
        )
    }
}
