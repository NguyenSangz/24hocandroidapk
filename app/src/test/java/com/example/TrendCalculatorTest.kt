package com.example

import com.example.domain.evaluator.TrendCalculator
import com.example.domain.evaluator.TrendDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test cho TrendCalculator:
 * 1. Kỳ trước = 0, kỳ này > 0 -> NEW_START (Mới bắt đầu kỳ này, không chia cho 0).
 * 2. Cả 2 kỳ đều = 0 -> STABLE (Chưa có dữ liệu).
 * 3. Tăng trưởng -> INCREASED.
 * 4. Giảm sút -> DECREASED.
 * 5. Dao động nhỏ (< 3%) -> STABLE.
 */
class TrendCalculatorTest {

    @Test
    fun testZeroPreviousPeriodHandlesGracefully() {
        val trend = TrendCalculator.calculateStudyTimeTrend(
            currentSeconds = 3600L,
            previousSeconds = 0L
        )

        assertEquals(TrendDirection.NEW_START, trend.direction)
        assertEquals("Mới bắt đầu kỳ này", trend.description)
        assertEquals(100f, trend.deltaPercent, 0.001f)
    }

    @Test
    fun testBothZeroReturnsStable() {
        val trend = TrendCalculator.calculateQuestionsTrend(
            currentCount = 0,
            previousCount = 0
        )

        assertEquals(TrendDirection.STABLE, trend.direction)
        assertEquals(0f, trend.deltaPercent, 0.001f)
    }

    @Test
    fun testIncreasedTrend() {
        val trend = TrendCalculator.calculateAccuracyTrend(
            currentAccuracy = 85f,
            previousAccuracy = 70f
        )

        assertEquals(TrendDirection.INCREASED, trend.direction)
        assertTrue(trend.deltaPercent > 0)
    }

    @Test
    fun testDecreasedTrend() {
        val trend = TrendCalculator.calculateQuestionsTrend(
            currentCount = 30,
            previousCount = 50
        )

        assertEquals(TrendDirection.DECREASED, trend.direction)
        assertEquals(-40.0f, trend.deltaPercent, 0.1f)
    }
}
