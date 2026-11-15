package com.example.domain.model

/**
 * Các loại kết quả tìm kiếm được hỗ trợ (F13):
 * 1. Bài học (LESSON)
 * 2. Câu hỏi (QUESTION)
 * 3. Thẻ ghi nhớ (FLASHCARD)
 * 4. Chương & Môn học (CHAPTER, SUBJECT)
 */
enum class SearchResultType(val displayName: String, val order: Int) {
    LESSON("Bài học", 1),
    CHAPTER("Chương học", 2),
    QUESTION("Câu hỏi trắc nghiệm", 3),
    FLASHCARD("Thẻ ghi nhớ Flashcard", 4),
    SUBJECT("Môn học", 5),
    TOOL("Công cụ", 6);

    companion object {
        fun fromString(value: String): SearchResultType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: LESSON
        }
    }
}

/**
 * Bản ghi kết quả tìm kiếm đã được chuẩn hóa và tính điểm xếp hạng
 */
data class SearchResult(
    val rowId: Long = 0,
    val targetId: String,
    val type: SearchResultType,
    val grade: Int,
    val series: String, // kntt, cd, ctst, chung
    val subjectId: String,
    val subjectName: String,
    val subjectColor: String = "#2563EB",
    val phanMon: String = "",
    val topicId: String = "",
    val title: String,
    val snippet: String,
    val isCompleted: Boolean = false,
    val relevanceScore: Int = 0,
    val matchedSeriesList: List<String> = emptyList(), // Danh sách bộ sách có chung topicId khi chọn Tất cả
    val extraDetails: String = "" // Thông tin bổ trợ (lời giải, đáp án, mặt sau flashcard)
) {
    val seriesDisplayName: String
        get() = when (series.lowercase()) {
            "kntt" -> "Kết nối tri thức"
            "cd" -> "Cánh Diều"
            "ctst" -> "Chân trời sáng tạo"
            "chung" -> "Dùng chung"
            else -> series.uppercase()
        }
}

/**
 * Bộ lọc tìm kiếm dạng chip theo yêu cầu F13:
 * - Lớp: 6, 7, 8, 9 (mặc định lớp hiện tại)
 * - Bộ sách: "CURRENT" (bộ đang dùng), "kntt", "ctst", "cd", "chung", "ALL" (tất cả)
 * - Môn: "ALL" hoặc id môn (toan, van, khtn, ...)
 * - Phần môn: "ALL", "ly", "hoa", "sinh", "lichsu", "diali"
 * - Loại kết quả: null (tất cả) hoặc từng loại
 */
data class SearchFilter(
    val grade: Int = 6,
    val series: String = "CURRENT",
    val subjectId: String = "ALL",
    val phanMon: String = "ALL",
    val resultType: SearchResultType? = null
)

/**
 * Kết quả nhóm theo danh mục hiển thị
 */
data class SearchGroupedSection(
    val type: SearchResultType,
    val title: String,
    val items: List<SearchResult>
)

/**
 * Gợi ý khi kết quả rỗng ở bộ sách hiện tại nhưng có ở bộ sách khác
 */
data class CrossSeriesSuggestion(
    val targetSeries: String,
    val seriesDisplayName: String,
    val resultCount: Int
)

/**
 * Gợi ý tìm kiếm (khi chưa gõ từ khóa)
 */
enum class SuggestionType {
    RECENT_SEARCH,
    IN_PROGRESS_TOPIC,
    PINNED_SUBJECT
}

data class SearchSuggestion(
    val text: String,
    val type: SuggestionType,
    val targetId: String = "",
    val extraInfo: String = ""
)
