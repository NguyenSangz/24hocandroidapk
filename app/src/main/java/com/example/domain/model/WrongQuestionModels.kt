package com.example.domain.model

import com.example.data.local.entity.WrongQuestionEntity

/**
 * Trạng thái ôn tập của câu hỏi trong Ngân hàng câu sai (F08).
 * - NEEDS_REVIEW: Cần ôn lại (vừa làm sai hoặc làm sai lại sau khi đã củng cố/đã thuộc)
 * - CONSOLIDATING: Đang củng cố (đã làm đúng 1-2 lần cách ngày)
 * - MASTERED: Đã thuộc (đã làm đúng liên tiếp đủ 3 lần cách nhau ít nhất 1 ngày)
 */
enum class WrongQuestionStatus(val code: String, val displayName: String) {
    NEEDS_REVIEW("NEEDS_REVIEW", "Cần ôn"),
    CONSOLIDATING("CONSOLIDATING", "Đang củng cố"),
    MASTERED("MASTERED", "Đã thuộc");

    companion object {
        fun fromCode(code: String): WrongQuestionStatus =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: NEEDS_REVIEW
    }
}

/**
 * Cấu hình quy tắc lặp lại ngắt quãng (Spaced Repetition) cho Ngân hàng câu sai.
 * Dễ dàng tùy chỉnh ngưỡng số lần đúng và khoảng cách ngày ôn tập.
 */
data class ReviewConfig(
    val masteredThreshold: Int = 3, // Cần đúng 3 lần liên tiếp ở các ngày khác nhau để thành "Đã thuộc"
    val minDaysBetweenReviews: Int = 1, // Khoảng cách tối thiểu giữa 2 lần tính chuỗi đúng (1 ngày)
    val intervalDays: List<Int> = listOf(1, 3, 7) // Khoảng cách ôn tăng dần: lần 1 (1 ngày), lần 2 (3 ngày), lần 3 (7 ngày)
)

/**
 * Tiêu chí sắp xếp danh sách câu sai
 */
enum class WrongBankSort(val displayName: String) {
    BY_DUE_DATE("Hạn ôn gần nhất"),
    BY_ERROR_COUNT("Sai nhiều nhất"),
    BY_LATEST("Lần sai mới nhất")
}

/**
 * Thông tin chi tiết một câu hỏi trong ngân hàng câu sai (gắn kèm đề bài, lời giải và ngữ cảnh chương/bài)
 */
data class WrongQuestionDetail(
    val entity: WrongQuestionEntity,
    val question: Question?,
    val lessonTitle: String = "",
    val chapterTitle: String = "",
    val subjectName: String = "",
    val isDueForReview: Boolean = false
) {
    val status: WrongQuestionStatus
        get() = WrongQuestionStatus.fromCode(entity.status)
}

/**
 * Thống kê tổng quan ngân hàng câu sai
 */
data class WrongBankStats(
    val needsReviewCount: Int = 0,
    val consolidatingCount: Int = 0,
    val masteredCount: Int = 0,
    val dueReviewCount: Int = 0,
    val subjectCounts: Map<String, Int> = emptyMap()
) {
    val totalCount: Int
        get() = needsReviewCount + consolidatingCount + masteredCount
}
