package com.example.domain.repository

import com.example.domain.model.CrossSeriesSuggestion
import com.example.domain.model.SearchFilter
import com.example.domain.model.SearchGroupedSection
import com.example.domain.model.SearchResult
import com.example.domain.model.SearchSuggestion
import kotlinx.coroutines.flow.Flow

/**
 * Domain interface cho tính năng Tìm kiếm toàn ứng dụng (F13)
 */
interface SearchRepository {

    /**
     * Thực hiện tìm kiếm theo truy vấn và bộ lọc, hỗ trợ phân trang
     */
    suspend fun search(
        query: String,
        filter: SearchFilter,
        limit: Int = 50,
        offset: Int = 0
    ): List<SearchResult>

    /**
     * Tìm kiếm và tự động phân nhóm theo danh mục (Bài học, Câu hỏi, Flashcard, Chương & Môn)
     */
    suspend fun searchGrouped(
        query: String,
        filter: SearchFilter,
        limitPerGroup: Int = 20
    ): List<SearchGroupedSection>

    /**
     * Tìm kiếm gợi ý các bộ sách khác khi kết quả ở bộ sách hiện tại bị rỗng
     */
    suspend fun findCrossSeriesSuggestions(
        query: String,
        currentSeries: String,
        grade: Int
    ): List<CrossSeriesSuggestion>

    /**
     * Lấy các gợi ý khi chưa gõ từ khóa (tìm kiếm gần đây, chủ đề đang học dở, môn ghim)
     */
    fun getSearchSuggestions(grade: Int): Flow<List<SearchSuggestion>>

    /**
     * Danh sách lịch sử tìm kiếm gần đây (tối đa 10 mục)
     */
    fun getRecentSearches(): Flow<List<String>>

    /**
     * Lưu một từ khóa vào lịch sử tìm kiếm (chỉ lưu khi bật lưu lịch sử)
     */
    suspend fun saveSearchQuery(query: String)

    /**
     * Xóa một từ khóa khỏi lịch sử tìm kiếm
     */
    suspend fun deleteSearchQuery(query: String)

    /**
     * Xóa toàn bộ lịch sử tìm kiếm
     */
    suspend fun clearSearchHistory()

    /**
     * Cài đặt cho phép lưu lịch sử tìm kiếm
     */
    val isSearchHistoryEnabled: Flow<Boolean>
    suspend fun setSearchHistoryEnabled(enabled: Boolean)

    /**
     * Đồng bộ hoặc cập nhật lại chỉ mục FTS
     */
    suspend fun syncIndexIfNeeded()

    /**
     * Luồng thông báo tiến độ hoặc trạng thái đang dựng chỉ mục
     */
    val isIndexing: Flow<Boolean>
}
