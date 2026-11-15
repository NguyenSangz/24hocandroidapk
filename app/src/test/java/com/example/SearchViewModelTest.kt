package com.example

import com.example.domain.model.CrossSeriesSuggestion
import com.example.domain.model.SearchResult
import com.example.domain.model.SearchResultType
import com.example.fake.FakeSearchRepository
import com.example.fake.FakeSettingsRepository
import com.example.ui.screens.search.SearchUiState
import com.example.ui.screens.search.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeSearchRepo: FakeSearchRepository
    private lateinit var fakeSettingsRepo: FakeSettingsRepository

    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeSearchRepo = FakeSearchRepository()
        fakeSettingsRepo = FakeSettingsRepository(initialGrade = 6)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState_isIdleWithRecentsAndSuggestions() = runTest {
        viewModel = SearchViewModel(fakeSearchRepo, fakeSettingsRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Idle)
        val idleState = state as SearchUiState.Idle
        assertEquals(2, idleState.recentSearches.size)
        assertEquals(1, idleState.suggestions.size)
    }

    @Test
    fun testDebounceSearch_cancelsImmediateQueryAndExecutesAfter250ms() = runTest {
        val sampleResult = listOf(
            SearchResult(
                targetId = "l1",
                type = SearchResultType.LESSON,
                grade = 6,
                series = "kntt",
                subjectId = "toan",
                subjectName = "Toán",
                title = "Bài 1: Tập hợp",
                snippet = "Nội dung"
            )
        )
        fakeSearchRepo.stubResults = sampleResult

        viewModel = SearchViewModel(fakeSearchRepo, fakeSettingsRepo)
        advanceUntilIdle()

        // Người dùng gõ "tap"
        viewModel.onQueryChanged("tap")
        advanceTimeBy(100) // chưa đủ 250ms

        // Người dùng gõ tiếp "tap hop"
        viewModel.onQueryChanged("tap hop")
        advanceTimeBy(200) // 200ms -> chưa hết debounce

        // Tiến thêm 60ms (tổng > 250ms cho "tap hop")
        advanceTimeBy(60)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Success)
        val success = state as SearchUiState.Success
        assertEquals(1, success.results.size)
        assertEquals("tap hop", success.query)
    }

    @Test
    fun testEmptyResults_showsEmptyWithCrossSeriesSuggestions() = runTest {
        fakeSearchRepo.stubCrossSuggestions = listOf(CrossSeriesSuggestion("cd", "Cánh Diều", 4))

        viewModel = SearchViewModel(fakeSearchRepo, fakeSettingsRepo)
        advanceUntilIdle()

        viewModel.onQueryChanged("khong_co")
        advanceTimeBy(300)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SearchUiState.Empty)
        val empty = state as SearchUiState.Empty
        assertEquals(1, empty.crossSeriesSuggestions.size)
        assertEquals("Cánh Diều", empty.crossSeriesSuggestions.first().seriesDisplayName)
    }

    @Test
    fun testBlankQuery_returnsToIdleState() = runTest {
        viewModel = SearchViewModel(fakeSearchRepo, fakeSettingsRepo)
        advanceUntilIdle()

        viewModel.onQueryChanged("toan")
        advanceTimeBy(300)
        advanceUntilIdle()

        // Xóa sạch ô tìm kiếm
        viewModel.onQueryChanged("")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is SearchUiState.Idle)
    }
}
