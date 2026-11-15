package com.example

import com.example.fake.FakeContentLoader
import com.example.fake.FakeSettingsRepository
import com.example.ui.screens.splash.SplashUiState
import com.example.ui.screens.splash.SplashViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var contentLoader: FakeContentLoader
    private lateinit var settingsRepository: FakeSettingsRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        contentLoader = FakeContentLoader()
        settingsRepository = FakeSettingsRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSplashLoadDataSuccess_whenGradeNotSelected() = runTest {
        contentLoader.shouldSucceed = true
        val viewModel = SplashViewModel(contentLoader, settingsRepository)

        // Ban đầu đang Loading
        assertEquals(SplashUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()

        // Sau khi hoàn thành nạp dữ liệu
        val state = viewModel.uiState.value
        assertTrue(state is SplashUiState.Success)
        val successState = state as SplashUiState.Success
        assertEquals(false, successState.isGradeSelected)
        assertEquals(6, successState.selectedGrade)
        assertEquals(1, contentLoader.callCount)
    }

    @Test
    fun testSplashLoadDataSuccess_whenGradeAlreadySelected() = runTest {
        settingsRepository.setGrade(8)
        contentLoader.shouldSucceed = true

        val viewModel = SplashViewModel(contentLoader, settingsRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SplashUiState.Success)
        val successState = state as SplashUiState.Success
        assertEquals(true, successState.isGradeSelected)
        assertEquals(8, successState.selectedGrade)
    }

    @Test
    fun testSplashLoadDataError_andRetry() = runTest {
        contentLoader.shouldSucceed = false
        contentLoader.errorMessage = "Lỗi đọc tệp assets"

        val viewModel = SplashViewModel(contentLoader, settingsRepository)
        advanceUntilIdle()

        // Kiểm tra trạng thái lỗi
        val state = viewModel.uiState.value
        assertTrue(state is SplashUiState.Error)
        assertEquals("Lỗi đọc tệp assets", (state as SplashUiState.Error).message)

        // Thử lại khi đã sửa lỗi
        contentLoader.shouldSucceed = true
        viewModel.retryLoading()
        advanceUntilIdle()

        val retryState = viewModel.uiState.value
        assertTrue(retryState is SplashUiState.Success)
        assertEquals(2, contentLoader.callCount)
    }
}
