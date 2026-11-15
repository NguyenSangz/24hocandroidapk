package com.example.ui.screens.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.SearchFilter
import com.example.domain.model.SearchResultType
import com.example.domain.repository.SearchRepository
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý tìm kiếm toàn ứng dụng (F13):
 * - Debounce 250ms, hủy bỏ truy vấn cũ khi có ký tự mới.
 * - Lưu và quản lý lịch sử tìm kiếm tối đa 10 mục.
 * - Quản lý các bộ lọc dạng chip: Lớp, Bộ sách, Môn, Phần môn, Loại kết quả.
 * - Tự động phát hiện gợi ý bộ sách khác khi kết quả rỗng.
 */
class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle? = null
) : ViewModel() {

    companion object {
        private const val DEBOUNCE_MILLIS = 250L
        private const val KEY_QUERY = "search_query"
    }

    private val _query = MutableStateFlow(savedStateHandle?.get<String>(KEY_QUERY) ?: "")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(SearchFilter(grade = 6, series = "CURRENT"))
    val filter: StateFlow<SearchFilter> = _filter.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val isIndexing: StateFlow<Boolean> = searchRepository.isIndexing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isHistoryEnabled: StateFlow<Boolean> = searchRepository.isSearchHistoryEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val recentSearches: StateFlow<List<String>> = searchRepository.getRecentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            // Lấy khối lớp mặc định của người học
            val currentGrade = settingsRepository.selectedGrade.first()
            _filter.value = _filter.value.copy(grade = currentGrade)
            loadIdleState()

            // Nếu có sẵn query trong SavedStateHandle
            if (_query.value.isNotBlank()) {
                triggerSearch(_query.value, _filter.value, isImmediate = true)
            }
        }
    }

    private fun loadIdleState() {
        viewModelScope.launch {
            val suggestions = searchRepository.getSearchSuggestions(_filter.value.grade).first()
            val recents = searchRepository.getRecentSearches().first()
            val historyEnabled = searchRepository.isSearchHistoryEnabled.first()
            _uiState.value = SearchUiState.Idle(
                recentSearches = recents,
                suggestions = suggestions,
                isHistoryEnabled = historyEnabled
            )
        }
    }

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        savedStateHandle?.set(KEY_QUERY, newQuery)

        if (newQuery.isBlank()) {
            searchJob?.cancel()
            loadIdleState()
            return
        }

        // Hủy truy vấn cũ và khởi động truy vấn mới với debounce 250ms
        triggerSearch(newQuery, _filter.value, isImmediate = false)
    }

    fun onFilterGradeChanged(newGrade: Int) {
        if (_filter.value.grade == newGrade) return
        _filter.value = _filter.value.copy(grade = newGrade)
        if (_query.value.isNotBlank()) {
            triggerSearch(_query.value, _filter.value, isImmediate = true)
        }
    }

    fun onFilterSeriesChanged(newSeries: String) {
        if (_filter.value.series.equals(newSeries, ignoreCase = true)) return
        _filter.value = _filter.value.copy(series = newSeries)
        if (_query.value.isNotBlank()) {
            triggerSearch(_query.value, _filter.value, isImmediate = true)
        }
    }

    fun onFilterSubjectChanged(subjectId: String) {
        if (_filter.value.subjectId == subjectId) return
        _filter.value = _filter.value.copy(subjectId = subjectId, phanMon = "ALL")
        if (_query.value.isNotBlank()) {
            triggerSearch(_query.value, _filter.value, isImmediate = true)
        }
    }

    fun onFilterPhanMonChanged(phanMon: String) {
        if (_filter.value.phanMon == phanMon) return
        _filter.value = _filter.value.copy(phanMon = phanMon)
        if (_query.value.isNotBlank()) {
            triggerSearch(_query.value, _filter.value, isImmediate = true)
        }
    }

    fun onFilterTypeChanged(type: SearchResultType?) {
        if (_filter.value.resultType == type) return
        _filter.value = _filter.value.copy(resultType = type)
        if (_query.value.isNotBlank()) {
            triggerSearch(_query.value, _filter.value, isImmediate = true)
        }
    }

    fun onQuerySubmitted(targetQuery: String? = null) {
        val q = (targetQuery ?: _query.value).trim()
        if (q.isBlank()) return
        if (targetQuery != null && targetQuery != _query.value) {
            _query.value = targetQuery
            savedStateHandle?.set(KEY_QUERY, targetQuery)
        }
        viewModelScope.launch {
            searchRepository.saveSearchQuery(q)
        }
        triggerSearch(q, _filter.value, isImmediate = true)
    }

    fun deleteRecentSearch(item: String) {
        viewModelScope.launch {
            searchRepository.deleteSearchQuery(item)
            if (_query.value.isBlank()) {
                loadIdleState()
            }
        }
    }

    fun clearAllRecentSearches() {
        viewModelScope.launch {
            searchRepository.clearSearchHistory()
            if (_query.value.isBlank()) {
                loadIdleState()
            }
        }
    }

    fun toggleHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            searchRepository.setSearchHistoryEnabled(enabled)
            loadIdleState()
        }
    }

    fun switchSeries(series: String) {
        onFilterSeriesChanged(series)
    }

    fun retry() {
        if (_query.value.isNotBlank()) {
            triggerSearch(_query.value, _filter.value, isImmediate = true)
        }
    }

    private fun triggerSearch(queryString: String, currentFilter: SearchFilter, isImmediate: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (!isImmediate) {
                delay(DEBOUNCE_MILLIS)
            }
            _uiState.value = SearchUiState.Loading

            try {
                val results = searchRepository.search(queryString, currentFilter, limit = 100, offset = 0)
                val grouped = searchRepository.searchGrouped(queryString, currentFilter, limitPerGroup = 20)

                if (results.isEmpty()) {
                    // Kiểm tra xem có kết quả ở các bộ sách khác không
                    val activeSeries = if (currentFilter.series.equals("CURRENT", ignoreCase = true)) "kntt" else currentFilter.series
                    val crossSuggestions = searchRepository.findCrossSeriesSuggestions(
                        query = queryString,
                        currentSeries = activeSeries,
                        grade = currentFilter.grade
                    )
                    _uiState.value = SearchUiState.Empty(
                        query = queryString,
                        filter = currentFilter,
                        crossSeriesSuggestions = crossSuggestions
                    )
                } else {
                    _uiState.value = SearchUiState.Success(
                        query = queryString,
                        filter = currentFilter,
                        results = results,
                        groupedSections = grouped
                    )
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(
                    errorMessage = e.message ?: "Đã xảy ra lỗi trong quá trình tìm kiếm."
                )
            }
        }
    }
}
