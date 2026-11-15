package com.example.domain.evaluator

import com.example.domain.model.TimerWarning

/**
 * Interface cung cấp thời gian thực tế, cho phép mock dễ dàng khi Unit Test.
 */
fun interface ClockProvider {
    fun currentTimeMillis(): Long
}

/**
 * Module quản lý logic đếm ngược thời gian bài kiểm tra (F07).
 * Logic thuần túy (Pure Logic), không phụ thuộc Android framework hoặc UI.
 *
 * Sử dụng mốc thời gian kết thúc (endTimeMillis) thay vì biến tăng dần,
 * đảm bảo tính toán chính xác tuyệt đối ngay cả khi app bị kill, xoay màn hình,
 * hoặc người học chuyển qua ứng dụng khác.
 */
class TestTimer(
    val totalDurationSeconds: Int,
    initialEndTimeMillis: Long? = null,
    private val clockProvider: ClockProvider = ClockProvider { System.currentTimeMillis() }
) {
    /**
     * Mốc thời gian kết thúc tính theo Epoch Millis
     */
    val endTimeMillis: Long = initialEndTimeMillis ?: (clockProvider.currentTimeMillis() + totalDurationSeconds * 1000L)

    /**
     * Mốc thời gian bắt đầu
     */
    val startTimeMillis: Long = endTimeMillis - (totalDurationSeconds * 1000L)

    /**
     * Tính số giây còn lại tại thời điểm hiện tại.
     * Có cơ chế phòng vệ khi người dùng cố tình chỉnh lùi giờ máy tính (now < startTime):
     * Nếu now < startTime, không để thời gian vượt quá tổng thời gian ban đầu.
     */
    fun calculateRemainingSeconds(currentNowMillis: Long = clockProvider.currentTimeMillis()): Int {
        if (currentNowMillis >= endTimeMillis) {
            return 0
        }
        val diffMillis = endTimeMillis - currentNowMillis
        val seconds = (diffMillis / 1000L).toInt()
        return seconds.coerceIn(0, totalDurationSeconds)
    }

    /**
     * Kiểm tra bài thi đã hết giờ hay chưa
     */
    fun isTimeUp(currentNowMillis: Long = clockProvider.currentTimeMillis()): Boolean {
        return calculateRemainingSeconds(currentNowMillis) <= 0
    }

    /**
     * Thời gian đã làm tính theo giây
     */
    fun calculateTimeSpentSeconds(currentNowMillis: Long = clockProvider.currentTimeMillis()): Int {
        val remaining = calculateRemainingSeconds(currentNowMillis)
        return (totalDurationSeconds - remaining).coerceAtLeast(0)
    }

    /**
     * Kiểm tra thời gian còn lại có thuộc vùng nguy cấp (dưới 20% tổng thời gian) để đổi màu cảnh báo
     */
    fun isCriticalTime(remainingSeconds: Int): Boolean {
        if (totalDurationSeconds <= 0) return true
        val criticalThreshold = totalDurationSeconds * 0.20f
        return remainingSeconds.toFloat() <= criticalThreshold
    }

    /**
     * Kiểm tra và phát hiện sự kiện cảnh báo thời gian (5 phút và 1 phút).
     * @param previousRemaining Số giây còn lại ở chu kỳ trước
     * @param currentRemaining Số giây còn lại ở chu kỳ hiện tại
     */
    fun checkWarningNotification(previousRemaining: Int, currentRemaining: Int): TimerWarning? {
        // Cảnh báo 5 phút (300 giây)
        if (previousRemaining > 300 && currentRemaining in 1..300) {
            return TimerWarning.FIVE_MINUTES_LEFT
        }
        // Cảnh báo 1 phút (60 giây)
        if (previousRemaining > 60 && currentRemaining in 1..60) {
            return TimerWarning.ONE_MINUTE_LEFT
        }
        return null
    }

    /**
     * Định dạng số giây còn lại sang chuỗi hiển thị mm:ss hoặc hh:mm:ss
     */
    companion object {
        fun formatRemainingTime(seconds: Int): String {
            val safeSec = seconds.coerceAtLeast(0)
            val hours = safeSec / 3600
            val minutes = (safeSec % 3600) / 60
            val secs = safeSec % 60
            return if (hours > 0) {
                String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", hours, minutes, secs)
            } else {
                String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, secs)
            }
        }
    }
}
