package com.example.ai

import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.AiTutorRepositoryImpl
import com.example.domain.model.AiPromptTemplates
import com.example.ui.screens.ai.AiTutorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiTutorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AiTutorRepositoryImpl
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var viewModel: AiTutorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = AiTutorRepositoryImpl()
        userPreferencesManager = UserPreferencesManager(ApplicationProvider.getApplicationContext())
        viewModel = AiTutorViewModel(repository, userPreferencesManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiState() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("general", state.selectedSubject)
        assertEquals("", state.inputText)
        assertFalse(state.isGenerating)
        assertTrue(state.messages.isNotEmpty())
    }

    @Test
    fun testSelectSubject() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSubjectSelected("math")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("math", state.selectedSubject)
    }

    @Test
    fun testApplyPromptTemplate() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val template = AiPromptTemplates.DEFAULT_PROMPTS.first { it.id == "p1" }
        viewModel.applyPromptTemplate(template)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("math", state.selectedSubject)
        assertEquals(template.promptText, state.inputText)
    }

    @Test
    fun testSendMessage() = runTest {
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onInputTextChanged("Giải bài toán x^2 - 4 = 0")
        viewModel.sendMessage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.inputText)
        assertTrue(state.messages.size >= 3)
    }
}
