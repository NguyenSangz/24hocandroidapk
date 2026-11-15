package com.example.ui.screens.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.domain.model.LessonFontSize
import com.example.domain.model.LessonNavigationInfo
import com.example.domain.repository.LessonRepository
import com.example.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Trạng thái UI của trình xem bài học (F04)
 */
sealed interface LessonUiState {
    data object Loading : LessonUiState
    data class Success(
        val lesson: LessonEntity,
        val subject: SubjectEntity?,
        val grade: Int,
        val navigation: LessonNavigationInfo,
        val fontSize: LessonFontSize
    ) : LessonUiState
    data class Error(val message: String) : LessonUiState
}

class LessonViewModel(
    private val lessonRepository: LessonRepository,
    private val progressRepository: ProgressRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val lessonId: String,
    private val gamificationRepository: com.example.data.repository.GamificationRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<LessonUiState>(LessonUiState.Loading)
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    val lessonFontSize: StateFlow<LessonFontSize> = lessonRepository.lessonFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LessonFontSize.MEDIUM)

    init {
        loadLesson()
    }

    fun loadLesson(targetLessonId: String = lessonId) {
        viewModelScope.launch {
            _uiState.value = LessonUiState.Loading
            val lesson = lessonRepository.getLessonById(targetLessonId)
            if (lesson == null) {
                _uiState.value = LessonUiState.Error("Không tìm thấy bài học này.")
                return@launch
            }

            userPreferencesManager.setLastStudiedLessonId(targetLessonId)

            // Lấy thông tin khối lớp từ preferences
            val grade = userPreferencesManager.selectedGrade.stateIn(viewModelScope).value
            // Tìm môn học tương ứng từ chapterId
            val subjectId = parseSubjectIdFromChapter(lesson.chapterId)
            val subject = lessonRepository.getSubjectById(subjectId)

            val navigation = lessonRepository.getLessonNavigation(grade, subjectId, lesson.id)
            val currentFontSize = lessonFontSize.value

            _uiState.value = LessonUiState.Success(
                lesson = lesson,
                subject = subject,
                grade = grade,
                navigation = navigation,
                fontSize = currentFontSize
            )
        }
    }

    fun toggleLessonCompleted() {
        val currentState = _uiState.value
        if (currentState is LessonUiState.Success) {
            val newCompleted = !currentState.lesson.isCompleted
            viewModelScope.launch {
                progressRepository.setLessonCompleted(currentState.lesson.id, newCompleted)
                if (newCompleted) {
                    gamificationRepository?.onLearningEvent(
                        com.example.domain.gamification.LearningEvent.LessonCompleted(
                            lessonId = currentState.lesson.id,
                            chapterId = currentState.lesson.chapterId,
                            subjectId = parseSubjectIdFromChapter(currentState.lesson.chapterId)
                        )
                    )
                }
                val updatedLesson = currentState.lesson.copy(isCompleted = newCompleted)
                _uiState.value = currentState.copy(lesson = updatedLesson)
            }
        }
    }

    fun setFontSize(fontSize: LessonFontSize) {
        viewModelScope.launch {
            lessonRepository.setLessonFontSize(fontSize)
            val currentState = _uiState.value
            if (currentState is LessonUiState.Success) {
                _uiState.value = currentState.copy(fontSize = fontSize)
            }
        }
    }

    private fun parseSubjectIdFromChapter(chapterId: String): String {
        return when {
            chapterId.contains("math") -> "math"
            chapterId.contains("literature") -> "literature"
            chapterId.contains("eng") -> "english"
            chapterId.contains("physics") -> "physics"
            chapterId.contains("chem") -> "chemistry"
            chapterId.contains("bio") -> "biology"
            chapterId.contains("his") -> "history"
            chapterId.contains("geo") -> "geography"
            chapterId.contains("civic") -> "civic"
            chapterId.contains("info") -> "informatics"
            chapterId.contains("tech") -> "technology"
            else -> "math"
        }
    }
}
