package com.example.ui.screens.search

import com.example.domain.model.CrossSeriesSuggestion
import com.example.domain.model.SearchFilter
import com.example.domain.model.SearchGroupedSection
import com.example.domain.model.SearchResult
import com.example.domain.model.SearchSuggestion

/**
 * Các trạng thái giao diện của màn hình Tìm kiếm (F13)
 */
sealed interface SearchUiState {

    /**
     * Trạng thái nghỉ (chưa nhập từ khóa):
     * Hiển thị danh sách tìm kiếm gần đây, chủ đề đang học dở, môn học yêu thích
     */
    data class Idle(
        val recentSearches: List<String> = emptyList(),
        val suggestions: List<SearchSuggestion> = emptyList(),
        val isHistoryEnabled: Boolean = true
    ) : SearchUiState

    /**
     * Đang thực hiện truy vấn (sau khi hết 250ms debounce)
     */
    data object Loading : SearchUiState

    /**
     * Tìm thấy kết quả phù hợp
     */
    data class Success(
        val query: String,
        val filter: SearchFilter,
        val results: List<SearchResult>,
        val groupedSections: List<SearchGroupedSection>,
        val crossSeriesSuggestions: List<CrossSeriesSuggestion> = emptyList(),
        val totalCount: Int = results.size
    ) : SearchUiState

    /**
     * Không tìm thấy kết quả nào trong bộ lọc hiện tại
     */
    data class Empty(
        val query: String,
        val filter: SearchFilter,
        val crossSeriesSuggestions: List<CrossSeriesSuggestion> = emptyList(),
        val helpfulTips: String = "Thử gõ ít từ hơn, bỏ dấu tiếng Việt hoặc kiểm tra lại bộ lọc lớp/môn."
    ) : SearchUiState

    /**
     * Có lỗi xảy ra trong quá trình truy vấn
     */
    data class Error(
        val errorMessage: String
    ) : SearchUiState
}
