package com.example.domain.evaluator

import com.example.data.local.entity.WrongQuestionEntity
import com.example.domain.model.ReviewConfig
import com.example.domain.model.WrongQuestionStatus

/**
 * Thuật toán tính toán chu kỳ lặp lại ngắt quãng (Spaced Repetition) cho Ngân hàng câu sai (F08).
 * Pure business logic class, nhận Clock / Time Provider độc lập để dễ dàng Unit Test 100%.
 */
class ReviewScheduler(
    val config: ReviewConfig = ReviewConfig(),
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    companion object {
        const val ONE_DAY_MS: Long = 24L * 60L * 60L * 1000L
    }

    /**
     * Xử lý khi học sinh làm sai câu hỏi (ở Luyện tập thường, Kiểm tra, hoặc trong Phiên ôn câu sai):
     * - Tăng số lần làm sai (errorCount + 1).
     * - Đặt lại chuỗi làm đúng liên tiếp về 0 (consecutiveCorrectCount = 0).
     * - Chuyển/giữ trạng thái là "Cần ôn" (NEEDS_REVIEW).
     * - Đặt lịch ôn lại tiếp theo vào 1 ngày sau (currentTime + 1 day).
     */
    fun onAnswerWrong(
        existingEntity: WrongQuestionEntity?,
        questionId: String,
        grade: Int = 6,
        subjectId: String = "",
        chapterId: String = "",
        lessonId: String = "",
        questionType: String = "",
        wrongAnswer: String = "",
        currentTime: Long = clock()
    ): WrongQuestionEntity {
        return if (existingEntity == null) {
            WrongQuestionEntity(
                questionId = questionId,
                grade = grade,
                subjectId = subjectId,
                chapterId = chapterId,
                lessonId = lessonId,
                questionType = questionType,
                errorCount = 1,
                consecutiveCorrectCount = 0,
                lastWrongTimestamp = currentTime,
                lastWrongAnswer = wrongAnswer,
                status = WrongQuestionStatus.NEEDS_REVIEW.code,
                nextReviewTimestamp = currentTime + ONE_DAY_MS,
                lastReviewTimestamp = currentTime
            )
        } else {
            existingEntity.copy(
                grade = if (existingEntity.grade > 0) existingEntity.grade else grade,
                subjectId = existingEntity.subjectId.ifBlank { subjectId },
                chapterId = existingEntity.chapterId.ifBlank { chapterId },
                lessonId = existingEntity.lessonId.ifBlank { lessonId },
                questionType = existingEntity.questionType.ifBlank { questionType },
                errorCount = existingEntity.errorCount + 1,
                consecutiveCorrectCount = 0, // Reset chuỗi đúng
                lastWrongTimestamp = currentTime,
                lastWrongAnswer = wrongAnswer.ifBlank { existingEntity.lastWrongAnswer },
                status = WrongQuestionStatus.NEEDS_REVIEW.code, // Quay về Cần ôn
                nextReviewTimestamp = currentTime + ONE_DAY_MS,
                lastReviewTimestamp = currentTime
            )
        }
    }

    /**
     * Xử lý khi học sinh trả lời ĐÚNG câu hỏi (ở phiên ôn tập hoặc luyện tập thường):
     * - Quy tắc: Phải cách lần ôn/đúng trước đó ít nhất `minDaysBetweenReviews` (1 ngày = 86_400_000 ms)
     *   thì mới được tính tăng chuỗi làm đúng liên tiếp. Nếu làm nhiều lần trong cùng ngày, giữ nguyên chuỗi đúng.
     * - Đủ `masteredThreshold` (3 lần đúng cách ngày) -> Chuyển sang "Đã thuộc" (MASTERED).
     * - Lần 1 đúng -> Đang củng cố (CONSOLIDATING), hẹn ôn sau 1 ngày.
     * - Lần 2 đúng -> Đang củng cố (CONSOLIDATING), hẹn ôn sau 3 ngày.
     * - Lần 3 đúng -> Đã thuộc (MASTERED), hoàn tất củng cố.
     */
    fun onAnswerCorrect(
        entity: WrongQuestionEntity,
        currentTime: Long = clock()
    ): WrongQuestionEntity {
        val minIntervalMs = config.minDaysBetweenReviews * ONE_DAY_MS

        // Kiểm tra xem lần ôn này có bị trùng trong cùng ngày (< minIntervalMs) với lần đúng trước đó hay không
        val isSameDayReview = entity.lastReviewTimestamp > 0L &&
                (currentTime - entity.lastReviewTimestamp) < minIntervalMs

        if (isSameDayReview) {
            // Không tăng chuỗi đúng trong cùng ngày để tránh cày chuỗi ảo
            return entity.copy(
                lastReviewTimestamp = currentTime
            )
        }

        val newConsecutive = entity.consecutiveCorrectCount + 1

        return if (newConsecutive >= config.masteredThreshold) {
            // Đạt ngưỡng 3 lần đúng cách ngày -> Chuyển sang ĐÃ THUỘC
            entity.copy(
                consecutiveCorrectCount = newConsecutive,
                status = WrongQuestionStatus.MASTERED.code,
                nextReviewTimestamp = 0L, // Đã thuộc, không còn bắt buộc ôn
                lastReviewTimestamp = currentTime
            )
        } else {
            // Đang củng cố (1 hoặc 2 lần đúng)
            val intervalDaysIndex = (newConsecutive - 1).coerceIn(0, config.intervalDays.size - 1)
            val daysToAdd = config.intervalDays.getOrElse(intervalDaysIndex) { 1 }
            val nextReview = currentTime + (daysToAdd * ONE_DAY_MS)

            entity.copy(
                consecutiveCorrectCount = newConsecutive,
                status = WrongQuestionStatus.CONSOLIDATING.code,
                nextReviewTimestamp = nextReview,
                lastReviewTimestamp = currentTime
            )
        }
    }

    /**
     * Đánh dấu câu hỏi là "Đã thuộc" theo thao tác thủ công của học sinh
     */
    fun markMasteredManually(
        entity: WrongQuestionEntity,
        currentTime: Long = clock()
    ): WrongQuestionEntity {
        return entity.copy(
            consecutiveCorrectCount = maxOf(entity.consecutiveCorrectCount, config.masteredThreshold),
            status = WrongQuestionStatus.MASTERED.code,
            nextReviewTimestamp = 0L,
            lastReviewTimestamp = currentTime
        )
    }

    /**
     * Kiểm tra xem câu hỏi đã đến hạn cần ôn lại hay chưa
     */
    fun isDueForReview(
        entity: WrongQuestionEntity,
        currentTime: Long = clock()
    ): Boolean {
        if (entity.status == WrongQuestionStatus.MASTERED.code) return false
        return entity.nextReviewTimestamp <= currentTime
    }
}
