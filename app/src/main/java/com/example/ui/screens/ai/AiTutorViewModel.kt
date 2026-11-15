package com.example.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferencesManager
import com.example.domain.model.AiChatMessage
import com.example.domain.model.AiPromptTemplate
import com.example.domain.repository.AiTutorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AiTutorUiState(
    val messages: List<AiChatMessage> = emptyList(),
    val selectedSubject: String = "general",
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val selectedGrade: Int = 9
)

class AiTutorViewModel(
    private val aiTutorRepository: AiTutorRepository,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _selectedSubject = MutableStateFlow("general")
    private val _inputText = MutableStateFlow("")
    private val _isGenerating = MutableStateFlow(false)

    val uiState: StateFlow<AiTutorUiState> = combine(
        aiTutorRepository.getChatMessages(),
        _selectedSubject,
        _inputText,
        _isGenerating,
        userPreferencesManager.selectedGrade.onStart { emit(9) }
    ) { messages, subject, text, generating, grade ->
        AiTutorUiState(
            messages = messages,
            selectedSubject = subject,
            inputText = text,
            isGenerating = generating,
            selectedGrade = grade
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AiTutorUiState()
    )

    fun onSubjectSelected(subjectId: String) {
        _selectedSubject.value = subjectId
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun sendMessage(customPrompt: String? = null) {
        val promptToSend = customPrompt ?: _inputText.value.trim()
        if (promptToSend.isBlank() || _isGenerating.value) return

        if (customPrompt == null) {
            _inputText.value = ""
        }

        viewModelScope.launch {
            _isGenerating.value = true
            val currentSubject = _selectedSubject.value
            val currentGrade = uiState.value.selectedGrade
            try {
                aiTutorRepository.sendMessage(promptToSend, currentSubject, currentGrade)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun applyPromptTemplate(template: AiPromptTemplate) {
        _selectedSubject.value = if (template.subjectId != "general") template.subjectId else _selectedSubject.value
        _inputText.value = template.promptText
    }

    fun clearHistory() {
        viewModelScope.launch {
            aiTutorRepository.clearHistory()
        }
    }
}
