package com.example.domain.model

/**
 * Phân loại bộ thẻ Flashcard theo chương trình học (F09).
 */
enum class FlashcardDeckType(val displayName: String, val code: String) {
    TU_VUNG("Từ vựng", "TỪ VỰNG"),
    CONG_THUC("Công thức", "CÔNG THỨC"),
    KHAI_NIEM("Khái niệm", "KHÁI NIỆM"),
    MOC_LICH_SU("Mốc lịch sử", "MỐC LỊCH SỬ");

    companion object {
        fun fromString(value: String): FlashcardDeckType {
            return entries.firstOrNull { 
                it.code.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) 
            } ?: KHAI_NIEM
        }
    }
}

/**
 * 4 mức đánh giá tự đánh giá mức độ ghi nhớ theo thuật toán SM-2:
 * - AGAIN (0): Quên hoàn toàn, cần học lại ngay trong phiên.
 * - HARD (3): Nhớ nhưng khó khăn, tốn nhiều nỗ lực suy nghĩ.
 * - GOOD (4): Nhớ tốt sau một chút suy nghĩ (mức đạt chuẩn).
 * - EASY (5): Nhớ ngay lập tức một cách dễ dàng.
 */
enum class FlashcardRating(
    val quality: Int,
    val title: String,
    val description: String
) {
    AGAIN(0, "Lại", "Chưa nhớ, học lại ngay"),
    HARD(3, "Khó", "Nhớ nhưng còn chập chững"),
    GOOD(4, "Được", "Nhớ chuẩn sau suy nghĩ"),
    EASY(5, "Dễ", "Thuộc làu, nhớ ngay");
}

/**
 * Chế độ ôn tập:
 * - SM2_SPACED: Ôn tập có tính toán lặp lại ngắt quãng, cập nhật tiến độ và lịch ôn.
 * - FREE_STUDY: Ôn tập tự do toàn bộ thẻ, không làm xáo trộn tiến độ SM-2, có xáo trộn thẻ.
 */
enum class FlashcardStudyMode {
    SM2_SPACED,
    FREE_STUDY
}

/**
 * Trạng thái tiến độ học của một thẻ Flashcard:
 * - NEW: Thẻ mới chưa học.
 * - LEARNING: Đang học lại / vừa đánh giá Lại.
 * - REVIEW: Đang trong chu kỳ lặp lại ngắt quãng.
 * - MASTERED: Đã thuộc vững (khoảng cách >= 21 ngày hoặc thuộc hoàn toàn).
 */
enum class FlashcardCardState(val code: String, val title: String) {
    NEW("NEW", "Thẻ mới"),
    LEARNING("LEARNING", "Đang học"),
    REVIEW("REVIEW", "Đang ôn"),
    MASTERED("MASTERED", "Đã thuộc");

    companion object {
        fun fromCode(code: String): FlashcardCardState {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: NEW
        }
    }
}

/**
 * Dữ liệu tổng hợp hiển thị cho từng bộ thẻ ở màn hình danh sách.
 */
data class FlashcardDeckSummary(
    val id: String,
    val name: String,
    val grade: Int,
    val subjectId: String,
    val type: FlashcardDeckType,
    val description: String,
    val totalCount: Int,
    val newCount: Int,
    val dueTodayCount: Int,
    val masteredCount: Int
)

/**
 * Kết quả sau khi hoàn thành một phiên ôn Flashcard.
 */
data class FlashcardSessionSummary(
    val deckName: String,
    val totalReviewed: Int,
    val rememberedCount: Int,
    val againCount: Int,
    val retentionRatePercent: Int,
    val difficultCardFronts: List<String>,
    val nextReviewPreview: String
)
