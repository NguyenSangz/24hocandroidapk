package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.CurriculumRepository
import com.example.data.repository.FlashcardRepository
import com.example.data.repository.PracticeRepository
import com.example.data.repository.StatsRepository
import com.example.domain.evaluator.ChapterAccuracyItem
import com.example.domain.evaluator.TestScorePoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class SubjectDetailStats(
    val subjectId: String,
    val subjectName: String,
    val grade: Int,
    val totalStudySeconds: Long,
    val recentTests: List<TestScorePoint>,
    val chapterAccuracies: List<ChapterAccuracyItem>,
    val completedLessons: Int,
    val totalLessons: Int,
    val remainingWrongCount: Int,
    val masteredCardsCount: Int
)

sealed interface SubjectStatsUiState {
    data object Loading : SubjectStatsUiState
    data class Success(val data: SubjectDetailStats) : SubjectStatsUiState
    data class Error(val message: String) : SubjectStatsUiState
}

/**
 * ViewModel cho màn hình Thống kê chi tiết từng môn học (F11 - Màn hình chi tiết môn).
 */
class SubjectStatsViewModel(
    val subjectId: String,
    private val statsRepository: StatsRepository,
    private val curriculumRepository: CurriculumRepository,
    private val practiceRepository: PracticeRepository,
    private val flashcardRepository: FlashcardRepository,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    val selectedGrade: StateFlow<Int> = userPreferencesManager.selectedGrade
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SubjectStatsUiState> = selectedGrade.flatMapLatest { grade ->
        flow {
            emit(SubjectStatsUiState.Loading)
            try {
                val now = System.currentTimeMillis()
                val testsFlow = statsRepository.getCompletedTestsInPeriod(grade, subjectId, 0L, now)
                val durationFlow = statsRepository.getSubjectStudySeconds(grade, subjectId, 0L, now)
                val accuracyFlow = statsRepository.getAccuracyByChapterInPeriod(grade, subjectId, 0L, now)

                combine(
                    testsFlow,
                    durationFlow,
                    accuracyFlow
                ) { tests: List<com.example.data.local.entity.TestEntity>,
                    durationSec: Long,
                    chapterStats: List<com.example.data.local.dao.ChapterAccuracyStat> ->
                    val recentTests = tests.takeLast(10).reversed().map { test ->
                        TestScorePoint(
                            testId = test.id,
                            title = test.title,
                            subjectId = test.subjectId,
                            score = test.score,
                            timestamp = test.timestamp,
                            formattedDate = ""
                        )
                    }

                    val chapterItems = chapterStats.map { ch ->
                        val pct = if (ch.totalAnswered > 0) (ch.totalCorrect.toFloat() / ch.totalAnswered) * 100f else 0f
                        ChapterAccuracyItem(
                            chapterId = ch.chapterId,
                            chapterTitle = ch.chapterId,
                            subjectId = ch.subjectId,
                            totalAnswered = ch.totalAnswered,
                            totalCorrect = ch.totalCorrect,
                            accuracyPercent = pct
                        )
                    }

                    val details = SubjectDetailStats(
                        subjectId = subjectId,
                        subjectName = subjectId,
                        grade = grade,
                        totalStudySeconds = durationSec,
                        recentTests = recentTests,
                        chapterAccuracies = chapterItems,
                        completedLessons = 4,
                        totalLessons = 16,
                        remainingWrongCount = 3,
                        masteredCardsCount = 12
                    )

                    SubjectStatsUiState.Success(details)
                }.collect { state ->
                    emit(state)
                }
            } catch (e: Exception) {
                emit(SubjectStatsUiState.Error(e.message ?: "Lỗi khi tải chi tiết môn"))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubjectStatsUiState.Loading)
}
