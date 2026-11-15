package com.example.fake

import com.example.domain.model.CrossSeriesSuggestion
import com.example.domain.model.SearchFilter
import com.example.domain.model.SearchGroupedSection
import com.example.domain.model.SearchResult
import com.example.domain.model.SearchResultType
import com.example.domain.model.SearchSuggestion
import com.example.domain.model.SuggestionType
import com.example.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSearchRepository : SearchRepository {

    private val _isIndexing = MutableStateFlow(false)
    override val isIndexing: Flow<Boolean> = _isIndexing.asStateFlow()

    private val _isHistoryEnabled = MutableStateFlow(true)
    override val isSearchHistoryEnabled: Flow<Boolean> = _isHistoryEnabled.asStateFlow()

    private val _recentSearches = MutableStateFlow(listOf("phân số", "tập hợp"))
    override fun getRecentSearches(): Flow<List<String>> = _recentSearches.asStateFlow()

    var stubResults: List<SearchResult> = emptyList()
    var stubGrouped: List<SearchGroupedSection> = emptyList()
    var stubCrossSuggestions: List<CrossSeriesSuggestion> = emptyList()

    override suspend fun search(
        query: String,
        filter: SearchFilter,
        limit: Int,
        offset: Int
    ): List<SearchResult> {
        if (query.equals("khong_co", ignoreCase = true)) return emptyList()
        return stubResults
    }

    override suspend fun searchGrouped(
        query: String,
        filter: SearchFilter,
        limitPerGroup: Int
    ): List<SearchGroupedSection> {
        if (query.equals("khong_co", ignoreCase = true)) return emptyList()
        return stubGrouped
    }

    override suspend fun findCrossSeriesSuggestions(
        query: String,
        currentSeries: String,
        grade: Int
    ): List<CrossSeriesSuggestion> {
        return stubCrossSuggestions
    }

    override fun getSearchSuggestions(grade: Int): Flow<List<SearchSuggestion>> {
        return MutableStateFlow(
            listOf(SearchSuggestion("Bài 1", SuggestionType.IN_PROGRESS_TOPIC))
        )
    }

    override suspend fun saveSearchQuery(query: String) {
        val list = _recentSearches.value.toMutableList()
        list.remove(query)
        list.add(0, query)
        _recentSearches.value = list
    }

    override suspend fun deleteSearchQuery(query: String) {
        val list = _recentSearches.value.toMutableList()
        list.remove(query)
        _recentSearches.value = list
    }

    override suspend fun clearSearchHistory() {
        _recentSearches.value = emptyList()
    }

    override suspend fun setSearchHistoryEnabled(enabled: Boolean) {
        _isHistoryEnabled.value = enabled
        if (!enabled) {
            _recentSearches.value = emptyList()
        }
    }

    override suspend fun syncIndexIfNeeded() {}
}
