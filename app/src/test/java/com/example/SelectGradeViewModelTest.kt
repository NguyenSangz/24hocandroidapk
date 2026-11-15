package com.example

import com.example.fake.FakeSettingsRepository
import com.example.ui.screens.grade.SelectGradeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SelectGradeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsRepository: FakeSettingsRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = FakeSettingsRepository(initialGrade = 6)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialGradeSelection() = runTest {
        val viewModel = SelectGradeViewModel(settingsRepository)
        advanceUntilIdle()

        assertEquals(6, viewModel.uiState.value.selectedGrade)
        assertFalse(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.uiState.value.hasConfirmed)
    }

    @Test
    fun testSelectDifferentGrade() = runTest {
        val viewModel = SelectGradeViewModel(settingsRepository)
        advanceUntilIdle()

        viewModel.onGradeSelected(9)
        assertEquals(9, viewModel.uiState.value.selectedGrade)

        viewModel.onGradeSelected(7)
        assertEquals(7, viewModel.uiState.value.selectedGrade)
    }

    @Test
    fun testConfirmGradeSelection_savesToRepository() = runTest {
        val viewModel = SelectGradeViewModel(settingsRepository)
        advanceUntilIdle()

        viewModel.onGradeSelected(8)

        var callbackInvoked = false
        viewModel.confirmGradeSelection {
            callbackInvoked = true
        }

        advanceUntilIdle()

        assertTrue(callbackInvoked)
        assertTrue(viewModel.uiState.value.hasConfirmed)
        assertFalse(viewModel.uiState.value.isSaving)

        // Kiểm tra trong repository
        assertEquals(8, settingsRepository.selectedGrade.first())
        assertTrue(settingsRepository.isGradeSelected.first())
        assertFalse(settingsRepository.isFirstLaunch.first())
    }
}
