package com.example

import com.example.domain.evaluator.StatsThresholdConfig
import com.example.domain.evaluator.StudyActivityType
import com.example.domain.evaluator.StudyTimeTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test toàn diện cho StudyTimeTracker (F11 - Ghi nhận thời gian học):
 * 1. Phiên ngắn (< 5 giây) -> Bỏ qua, trả về 0.
 * 2. Phiên thông thường foreground -> Tính đủ số giây.
 * 3. Chuyển background và quay lại -> Thời gian background không bị tính.
 * 4. Không thao tác quá 30 phút (treo máy) -> Dừng đếm ở mốc tương tác cuối cùng + 30 phút.
 * 5. Tái sử dụng Clock giả độc lập.
 */
class StudyTimeTrackerTest {

    @Test
    fun testShortSessionUnder5SecondsIsIgnored() {
        var simulatedTime = 1000000L
        val tracker = StudyTimeTracker(
            activityType = StudyActivityType.LESSON,
            subjectId = "math",
            grade = 6,
            clock = { simulatedTime }
        )

        tracker.start()
        assertTrue(tracker.isActive())

        // Trôi qua 4 giây (< 5 giây)
        simulatedTime += 4000L

        val duration = tracker.stopAndGetDurationSeconds()
        assertEquals(0, duration)
        assertFalse(tracker.isActive())
    }

    @Test
    fun testValidForegroundSessionIsRecordedAccurately() {
        var simulatedTime = 1000000L
        val tracker = StudyTimeTracker(
            activityType = StudyActivityType.PRACTICE,
            subjectId = "literature",
            grade = 7,
            clock = { simulatedTime }
        )

        tracker.start()
        // Học trong 45 giây liên tục
        simulatedTime += 45000L
        tracker.onUserInteraction()

        val duration = tracker.stopAndGetDurationSeconds()
        assertEquals(45, duration)
    }

    @Test
    fun testBackgroundPausesAccumulation() {
        var simulatedTime = 1000000L
        val tracker = StudyTimeTracker(
            activityType = StudyActivityType.FLASHCARD,
            subjectId = "english",
            grade = 8,
            clock = { simulatedTime }
        )

        tracker.start()
        // Học foreground 10 giây
        simulatedTime += 10000L
        tracker.onUserInteraction()

        // Xuống background (điện thoại khóa màn hình / ra app khác) trong 60 giây
        tracker.onBackground()
        assertFalse(tracker.isInForeground())
        simulatedTime += 60000L

        // Mở lại foreground và học thêm 20 giây
        tracker.onForeground()
        assertTrue(tracker.isInForeground())
        simulatedTime += 20000L

        val duration = tracker.stopAndGetDurationSeconds()
        // Tổng thời gian chỉ tính 10s + 20s = 30s, 60s background bị loại bỏ
        assertEquals(30, duration)
    }

    @Test
    fun testMaxInactiveSessionStopsAtLimit() {
        var simulatedTime = 1000000L
        val tracker = StudyTimeTracker(
            activityType = StudyActivityType.TEST,
            subjectId = "physics",
            grade = 9,
            clock = { simulatedTime },
            maxInactiveSeconds = 1800 // 30 phút
        )

        tracker.start()
        // Thao tác ban đầu
        tracker.onUserInteraction()

        // Treo máy 40 phút (2400 giây) không chạm màn hình
        simulatedTime += 2400 * 1000L

        val duration = tracker.stopAndGetDurationSeconds()
        // Không tính đủ 2400s mà chỉ tính tối đa 1800s (30 phút)
        assertEquals(1800, duration)
    }
}
