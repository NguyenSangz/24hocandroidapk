package com.example.ui.screens.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.SubjectEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.domain.model.ChapterWithProgress
import com.example.domain.repository.LessonRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Trạng thái UI cho màn hình danh sách chương/bài (F04)
 */
sealed interface ChapterListUiState {
    data object Loading : ChapterListUiState
    data class Success(
        val subject: SubjectEntity,
        val grade: Int,
        val chapters: List<ChapterWithProgress>,
        val firstUncompletedLessonId: String?,
        val totalLessons: Int,
        val completedLessons: Int
    ) : ChapterListUiState
    data class Empty(
        val subject: SubjectEntity?,
        val grade: Int,
        val message: String
    ) : ChapterListUiState
    data class Error(val message: String) : ChapterListUiState
}

class ChapterListViewModel(
    private val lessonRepository: LessonRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val subjectId: String
) : ViewModel() {

    val selectedGrade: StateFlow<Int> = userPreferencesManager.selectedGrade
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)

    private val _subject = MutableStateFlow<SubjectEntity?>(null)
    val subject: StateFlow<SubjectEntity?> = _subject.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ChapterListUiState> = combine(
        selectedGrade,
        _subject
    ) { grade, currentSubject ->
        Pair(grade, currentSubject)
    }.flatMapLatest { (grade, currentSubject) ->
        val sub = currentSubject ?: lessonRepository.getSubjectById(subjectId)
        if (sub == null) {
            MutableStateFlow(ChapterListUiState.Loading)
        } else {
            lessonRepository.getChaptersWithProgress(grade, subjectId).combine(
                userPreferencesManager.selectedGrade
            ) { chaptersWithProg, _ ->
                if (chaptersWithProg.isEmpty()) {
                    ChapterListUiState.Empty(
                        subject = sub,
                        grade = grade,
                        message = "Chưa có nội dung bài học cho môn ${sub.name} ở Lớp $grade."
                    )
                } else {
                    val total = chaptersWithProg.sumOf { it.totalLessonCount }
                    val completed = chaptersWithProg.sumOf { it.completedLessonCount }
                    var firstUncompletedId: String? = null
                    for (ch in chaptersWithProg) {
                        val uncompleted = ch.lessons.firstOrNull { !it.isCompleted }
                        if (uncompleted != null) {
                            firstUncompletedId = uncompleted.id
                            break
                        }
                    }
                    if (firstUncompletedId == null && chaptersWithProg.isNotEmpty() && chaptersWithProg.first().lessons.isNotEmpty()) {
                        firstUncompletedId = chaptersWithProg.first().lessons.first().id
                    }

                    ChapterListUiState.Success(
                        subject = sub,
                        grade = grade,
                        chapters = chaptersWithProg,
                        firstUncompletedLessonId = firstUncompletedId,
                        totalLessons = total,
                        completedLessons = completed
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChapterListUiState.Loading)

    init {
        loadSubject()
    }

    private fun loadSubject() {
        viewModelScope.launch {
            val sub = lessonRepository.getSubjectById(subjectId)
            _subject.value = sub
        }
    }
}
