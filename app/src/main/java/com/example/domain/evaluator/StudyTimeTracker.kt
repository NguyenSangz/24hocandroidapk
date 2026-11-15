package com.example.domain.evaluator

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Loại hoạt động học tập (F11)
 */
enum class StudyActivityType(val rawValue: String, val displayName: String) {
    LESSON("LESSON", "Bài học"),
    PRACTICE("PRACTICE", "Luyện tập"),
    TEST("TEST", "Kiểm tra"),
    FLASHCARD("FLASHCARD", "Flashcard"),
    WRONG_REVIEW("WRONG_REVIEW", "Ôn câu sai"),
    TOOL("TOOL", "Công cụ");

    companion object {
        fun fromRaw(raw: String): StudyActivityType {
            return entries.find { it.rawValue == raw } ?: LESSON
        }
    }
}

/**
 * Bộ theo dõi thời gian học thực tế (F11 - StudyTimeTracker).
 * - Nhận `clock` giả lập (epoch millis) để kiểm thử unit test độc lập.
 * - Chỉ tính thời gian khi màn hình hiển thị và app ở foreground.
 * - Tạm dừng khi xuống nền (background).
 * - Bỏ qua các phiên quá ngắn (< 5 giây).
 * - Tự động giới hạn tối đa phiên (ví dụ 30 phút không thao tác thì ngừng đếm, loại bỏ thời gian treo máy).
 */
class StudyTimeTracker(
    val activityType: StudyActivityType,
    val subjectId: String,
    val grade: Int,
    val targetId: String = "",
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val minValidSeconds: Int = StatsThresholdConfig.MIN_VALID_SESSION_SECONDS,
    private val maxInactiveSeconds: Int = StatsThresholdConfig.MAX_INACTIVE_LIMIT_SECONDS
) {
    private val isRunning = AtomicBoolean(false)
    private val isForeground = AtomicBoolean(false)

    private val sessionStartTimestamp = AtomicLong(clock())
    private val lastForegroundTimestamp = AtomicLong(0L)
    private val lastInteractionTimestamp = AtomicLong(clock())
    private val accumulatedActiveSeconds = AtomicLong(0L)

    /**
     * Bắt đầu theo dõi phiên học.
     */
    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            val now = clock()
            sessionStartTimestamp.set(now)
            lastForegroundTimestamp.set(now)
            lastInteractionTimestamp.set(now)
            accumulatedActiveSeconds.set(0L)
            isForeground.set(true)
        }
    }

    /**
     * Ghi nhận thao tác người dùng (chạm màn hình, chọn đáp án, lật thẻ, cuộn bài học).
     * Làm mới mốc thời gian thao tác để tránh bị tính là "treo máy".
     */
    fun onUserInteraction() {
        if (!isRunning.get()) return
        val now = clock()
        if (isForeground.get()) {
            flushForegroundDuration(now)
        }
        lastInteractionTimestamp.set(now)
    }

    /**
     * Gọi khi màn hình/app chuyển vào foreground (onResume hoặc Composable visible).
     */
    fun onForeground() {
        if (!isRunning.get()) return
        if (isForeground.compareAndSet(false, true)) {
            val now = clock()
            lastForegroundTimestamp.set(now)
            lastInteractionTimestamp.set(now)
        }
    }

    /**
     * Gọi khi màn hình/app xuống background (onPause hoặc Composable detached).
     */
    fun onBackground() {
        if (!isRunning.get()) return
        if (isForeground.compareAndSet(true, false)) {
            val now = clock()
            flushForegroundDuration(now)
        }
    }

    /**
     * Chốt và kết thúc phiên học.
     * @return Số giây học hợp lệ. Nếu < minValidSeconds thì trả về 0 (bỏ qua).
     */
    fun stopAndGetDurationSeconds(): Int {
        if (!isRunning.compareAndSet(true, false)) {
            return 0
        }
        val now = clock()
        if (isForeground.get()) {
            flushForegroundDuration(now)
            isForeground.set(false)
        }

        val totalSeconds = accumulatedActiveSeconds.get().toInt()
        return if (totalSeconds >= minValidSeconds) {
            totalSeconds
        } else {
            0
        }
    }

    /**
     * Lấy mốc thời điểm bắt đầu phiên (epoch millis).
     */
    fun getStartTimestamp(): Long = sessionStartTimestamp.get()

    /**
     * Đang chạy hay không.
     */
    fun isActive(): Boolean = isRunning.get()

    /**
     * Đang ở foreground hay không.
     */
    fun isInForeground(): Boolean = isForeground.get()

    /**
     * Cộng dồn khoảng thời gian foreground vừa diễn ra, áp dụng bộ lọc không tương tác.
     */
    private fun flushForegroundDuration(now: Long) {
        val lastFg = lastForegroundTimestamp.get()
        if (lastFg <= 0L || now <= lastFg) return

        val durationSec = ((now - lastFg) / 1000L).coerceAtLeast(0L)
        val lastInteraction = lastInteractionTimestamp.get()
        val inactiveSec = ((now - lastInteraction) / 1000L).coerceAtLeast(0L)

        // Nếu thời gian không tương tác vượt quá giới hạn tối đa (30 phút = 1800 giây)
        val validSecToAdd = if (inactiveSec > maxInactiveSeconds) {
            val validPortion = (durationSec - (inactiveSec - maxInactiveSeconds)).coerceAtLeast(0L)
            validPortion.coerceAtMost(maxInactiveSeconds.toLong())
        } else {
            durationSec
        }

        accumulatedActiveSeconds.addAndGet(validSecToAdd)
        lastForegroundTimestamp.set(now)
    }
}
