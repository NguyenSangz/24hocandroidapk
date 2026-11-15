package com.example.domain.evaluator

import com.example.data.local.entity.FlashcardProgressEntity
import com.example.domain.model.FlashcardCardState
import com.example.domain.model.FlashcardRating
import kotlin.math.roundToInt

/**
 * Triển khai thuật toán SuperMemo-2 (SM-2) thuần túy cho Flashcard học tập THCS (F09).
 * - Nhận `clock` giả lập để đảm bảo 100% testable trong mọi ngữ cảnh thời gian / múi giờ.
 * - Hỗ trợ tính toán khoảng cách ôn tập, hệ số dễ (EF >= 1.3), và xem trước khoảng cách ngày cho 4 nút.
 */
class SpacedRepetitionScheduler(
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    companion object {
        const val ONE_MINUTE_MS: Long = 60L * 1000L
        const val TEN_MINUTES_MS: Long = 10L * ONE_MINUTE_MS
        const val ONE_DAY_MS: Long = 24L * 60L * 60L * 1000L
        const val MIN_EASE_FACTOR: Float = 1.3f
        const val DEFAULT_EASE_FACTOR: Float = 2.5f
    }

    /**
     * Tính toán cập nhật trạng thái SM-2 cho một thẻ sau khi người học đánh giá.
     *
     * @param existingProgress Tiến độ hiện tại của thẻ (hoặc null nếu là thẻ mới).
     * @param cardId Mã thẻ.
     * @param deckId Mã bộ thẻ.
     * @param rating Mức đánh giá (AGAIN = 0, HARD = 3, GOOD = 4, EASY = 5).
     * @param currentTime Thời điểm làm bài (mặc định lấy từ clock).
     * @return Bản ghi FlashcardProgressEntity mới đã được cập nhật chuẩn SM-2.
     */
    fun calculateNextState(
        existingProgress: FlashcardProgressEntity?,
        cardId: String,
        deckId: String,
        rating: FlashcardRating,
        currentTime: Long = clock()
    ): FlashcardProgressEntity {
        val current = existingProgress ?: FlashcardProgressEntity(
            cardId = cardId,
            deckId = deckId,
            repetitions = 0,
            intervalDays = 0,
            easeFactor = DEFAULT_EASE_FACTOR,
            consecutiveCorrect = 0,
            nextReviewTimestamp = 0L,
            lastReviewTimestamp = 0L,
            state = FlashcardCardState.NEW.code,
            totalReviews = 0,
            lapses = 0
        )

        val q = rating.quality

        // 1. Cập nhật hệ số dễ EF (Easiness Factor) theo công thức SM-2 chuẩn
        // EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        val delta = 0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f)
        var newEase = current.easeFactor + delta
        if (newEase < MIN_EASE_FACTOR) {
            newEase = MIN_EASE_FACTOR
        }

        return if (q < 3) {
            // Đánh giá "Lại" (chất lượng < 3):
            // - Đặt lại chuỗi đúng liên tiếp và số lần lặp về 0
            // - Đánh dấu quay lại ôn trong phiên (hẹn sau 10 phút)
            // - Tăng số lần quên (lapses)
            current.copy(
                repetitions = 0,
                intervalDays = 1,
                easeFactor = newEase,
                consecutiveCorrect = 0,
                nextReviewTimestamp = currentTime + TEN_MINUTES_MS,
                lastReviewTimestamp = currentTime,
                state = FlashcardCardState.LEARNING.code,
                totalReviews = current.totalReviews + 1,
                lapses = current.lapses + 1
            )
        } else {
            // Đánh giá đạt chuẩn (Khó: 3, Được: 4, Dễ: 5):
            val newConsecutive = current.consecutiveCorrect + 1
            val newRepetitions = current.repetitions + 1

            // Quy tắc khoảng cách:
            // Lần 1: 1 ngày
            // Lần 2: 6 ngày
            // Lần 3 trở đi: khoảng cách trước * EF
            val newIntervalDays: Int = when (newConsecutive) {
                1 -> {
                    if (rating == FlashcardRating.EASY) 2 else 1
                }
                2 -> {
                    if (rating == FlashcardRating.EASY) 7 else 6
                }
                else -> {
                    val prevInterval = if (current.intervalDays > 0) current.intervalDays else 6
                    val factor = if (rating == FlashcardRating.HARD) {
                        (newEase * 0.85f).coerceAtLeast(1.1f)
                    } else if (rating == FlashcardRating.EASY) {
                        newEase * 1.3f
                    } else {
                        newEase
                    }
                    (prevInterval * factor).roundToInt().coerceAtLeast(current.intervalDays + 1)
                }
            }

            val nextReview = currentTime + (newIntervalDays * ONE_DAY_MS)
            val newState = if (newIntervalDays >= 21) {
                FlashcardCardState.MASTERED.code
            } else {
                FlashcardCardState.REVIEW.code
            }

            current.copy(
                repetitions = newRepetitions,
                intervalDays = newIntervalDays,
                easeFactor = newEase,
                consecutiveCorrect = newConsecutive,
                nextReviewTimestamp = nextReview,
                lastReviewTimestamp = currentTime,
                state = newState,
                totalReviews = current.totalReviews + 1
            )
        }
    }

    /**
     * Xem trước khoảng cách ôn tập cho từng nút đánh giá để hiển thị lên giao diện (Preview).
     * Ví dụ: "10 phút", "1 ngày", "3 ngày", "6 ngày".
     */
    fun previewIntervalText(
        existingProgress: FlashcardProgressEntity?,
        rating: FlashcardRating,
        currentTime: Long = clock()
    ): String {
        return when (rating) {
            FlashcardRating.AGAIN -> "10 phút"
            FlashcardRating.HARD -> {
                val next = calculateNextState(existingProgress, "temp", "temp", rating, currentTime)
                formatDays(next.intervalDays)
            }
            FlashcardRating.GOOD -> {
                val next = calculateNextState(existingProgress, "temp", "temp", rating, currentTime)
                formatDays(next.intervalDays)
            }
            FlashcardRating.EASY -> {
                val next = calculateNextState(existingProgress, "temp", "temp", rating, currentTime)
                formatDays(next.intervalDays)
            }
        }
    }

    /**
     * Định dạng số ngày hiển thị thân thiện với học sinh.
     */
    private fun formatDays(days: Int): String {
        return when {
            days <= 0 -> "10 phút"
            days == 1 -> "1 ngày"
            days < 30 -> "$days ngày"
            days < 365 -> "${(days / 30.0).roundToInt()} tháng"
            else -> "${(days / 365.0).roundToInt()} năm"
        }
    }

    /**
     * Kiểm tra xem thẻ đã đến hạn ôn tập hôm nay hay chưa.
     */
    fun isDue(progress: FlashcardProgressEntity?, currentTime: Long = clock()): Boolean {
        if (progress == null) return true // Thẻ mới chưa học luôn sẵn sàng
        if (progress.state == FlashcardCardState.NEW.code) return true
        if (progress.nextReviewTimestamp <= 0L) return true
        return progress.nextReviewTimestamp <= currentTime
    }
}
