package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.dao.SubjectAccuracyStat
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TestEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.CurriculumRepository
import com.example.data.repository.StatsRepository
import com.example.domain.evaluator.DayStudyBarData
import com.example.domain.evaluator.MetricTrend
import com.example.domain.evaluator.OverviewMetrics
import com.example.domain.evaluator.StatsAggregator
import com.example.domain.evaluator.StatsTimeRange
import com.example.domain.evaluator.StatsTimeRangeHelper
import com.example.domain.evaluator.StrengthWeaknessAnalysis
import com.example.domain.evaluator.SubjectAccuracyItem
import com.example.domain.evaluator.SubjectComparisonItem
import com.example.domain.evaluator.TestScorePoint
import com.example.domain.evaluator.TrendCalculator
import com.example.ui.components.chart.SubjectLessonProgressItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * Trạng thái UI chuẩn cho màn hình Thống kê học tập (F11)
 */
sealed interface StatsUiState {
    data object Loading : StatsUiState
    data object Empty : StatsUiState
    data class Success(
        val overview: OverviewMetrics,
        val studyBars: List<DayStudyBarData>,
        val testScores: List<TestScorePoint>,
        val subjectAccuracies: List<SubjectAccuracyItem>,
        val subjectComparisons: List<SubjectComparisonItem>,
        val lessonProgresses: List<SubjectLessonProgressItem>,
        val strengthWeakness: StrengthWeaknessAnalysis,
        val studyTimeTrend: MetricTrend,
        val accuracyTrend: MetricTrend
    ) : StatsUiState
    data class Error(val message: String) : StatsUiState
}

/**
 * ViewModel quản lý màn hình Thống kê học tập (F11 - StatsViewModel)
 */
class StatsViewModel(
    private val statsRepository: StatsRepository,
    private val curriculumRepository: CurriculumRepository,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _selectedTimeRange = MutableStateFlow(StatsTimeRange.LAST_7_DAYS)
    val selectedTimeRange: StateFlow<StatsTimeRange> = _selectedTimeRange.asStateFlow()

    private val _selectedSubjectFilter = MutableStateFlow("all")
    val selectedSubjectFilter: StateFlow<String> = _selectedSubjectFilter.asStateFlow()

    val selectedGrade: StateFlow<Int> = userPreferencesManager.selectedGrade
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)

    fun setTimeRange(range: StatsTimeRange) {
        _selectedTimeRange.value = range
    }

    fun setSubjectFilter(subjectId: String) {
        _selectedSubjectFilter.value = subjectId
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatsUiState> = combine(
        selectedGrade,
        _selectedTimeRange,
        _selectedSubjectFilter
    ) { grade, timeRange, subjectFilter ->
        Triple(grade, timeRange, subjectFilter)
    }.flatMapLatest { (grade, timeRange, subjectFilter) ->
        flow<StatsUiState> {
            emit(StatsUiState.Loading)
            try {
                val timeBounds = StatsTimeRangeHelper.getTimeBounds(timeRange)

                val sessionsFlow: Flow<List<StudySessionEntity>> =
                    statsRepository.getSessionsInPeriod(grade, timeBounds.fromMillis, timeBounds.toMillis)
                val prevSessionsFlow: Flow<List<StudySessionEntity>> =
                    statsRepository.getSessionsInPeriod(grade, timeBounds.previousFromMillis, timeBounds.previousToMillis)
                val testsFlow: Flow<List<TestEntity>> =
                    statsRepository.getCompletedTestsInPeriod(grade, subjectFilter, timeBounds.fromMillis, timeBounds.toMillis)
                val accuracyStatsFlow: Flow<List<SubjectAccuracyStat>> =
                    statsRepository.getAccuracyBySubjectInPeriod(grade, timeBounds.fromMillis, timeBounds.toMillis)
                val prevAccuracyStatsFlow: Flow<List<SubjectAccuracyStat>> =
                    statsRepository.getAccuracyBySubjectInPeriod(grade, timeBounds.previousFromMillis, timeBounds.previousToMillis)

                val subjects: List<SubjectEntity> = curriculumRepository.allSubjects.first()

                combine(
                    sessionsFlow,
                    prevSessionsFlow,
                    testsFlow,
                    accuracyStatsFlow,
                    prevAccuracyStatsFlow
                ) { sessions: List<StudySessionEntity>,
                    prevSessions: List<StudySessionEntity>,
                    tests: List<TestEntity>,
                    accStats: List<SubjectAccuracyStat>,
                    prevAccStats: List<SubjectAccuracyStat> ->

                    val totalSec = sessions.sumOf { it.durationSeconds.toLong() }
                    val prevTotalSec = prevSessions.sumOf { it.durationSeconds.toLong() }
                    val distinctDays = sessions.map { it.startTimestamp / 86400000 }.distinct().size

                    val totalAnswered = accStats.sumOf { it.totalAnswered }
                    val totalCorrect = accStats.sumOf { it.totalCorrect }

                    val prevAnswered = prevAccStats.sumOf { it.totalAnswered }
                    val prevCorrect = prevAccStats.sumOf { it.totalCorrect }

                    val currAccuracy = if (totalAnswered > 0) (totalCorrect.toFloat() / totalAnswered) * 100f else 0f
                    val prevAccuracy = if (prevAnswered > 0) (prevCorrect.toFloat() / prevAnswered) * 100f else 0f

                    val studyBars = StatsAggregator.aggregateDailyStudyBars(sessions, timeBounds)
                    val testScorePoints = StatsAggregator.mapTestScorePoints(tests)

                    val subjectMap = subjects.associate { it.id to it.name }
                    val subjectAccuracies = accStats.map { stat ->
                        val acc = if (stat.totalAnswered > 0) (stat.totalCorrect.toFloat() / stat.totalAnswered) * 100f else 0f
                        SubjectAccuracyItem(
                            subjectId = stat.subjectId,
                            subjectName = subjectMap[stat.subjectId] ?: stat.subjectId,
                            totalAnswered = stat.totalAnswered,
                            totalCorrect = stat.totalCorrect,
                            accuracyPercent = acc
                        )
                    }

                    val subjectComparisons = subjects.take(6).map { sub ->
                        val stat = accStats.find { it.subjectId == sub.id }
                        val acc = if (stat != null && stat.totalAnswered > 0) (stat.totalCorrect.toFloat() / stat.totalAnswered) * 100f else 50f
                        SubjectComparisonItem(
                            subjectId = sub.id,
                            subjectName = sub.name,
                            accuracyScore = acc,
                            lessonProgressScore = 30f,
                            consistencyScore = 60f
                        )
                    }

                    val lessonProgresses = subjects.map { sub ->
                        SubjectLessonProgressItem(
                            subjectId = sub.id,
                            subjectName = sub.name,
                            completedCount = 2,
                            totalCount = 10,
                            progressPercent = 20f
                        )
                    }

                    val overview = StatsAggregator.calculateOverview(
                        totalStudySeconds = totalSec,
                        distinctStudyDays = distinctDays,
                        totalQuestionsAnswered = totalAnswered,
                        totalQuestionsCorrect = totalCorrect,
                        completedLessonsCount = 5,
                        totalLessonsCount = 45,
                        totalXp = (totalCorrect * 2)
                    )

                    val studyTimeTrend = TrendCalculator.calculateStudyTimeTrend(totalSec, prevTotalSec)
                    val accuracyTrend = TrendCalculator.calculateAccuracyTrend(currAccuracy, prevAccuracy)

                    if (sessions.isEmpty() && tests.isEmpty() && totalAnswered == 0) {
                        StatsUiState.Empty
                    } else {
                        val swAnalysis = statsRepository.getStrengthWeaknessAnalysis(grade, subjectFilter)
                        StatsUiState.Success(
                            overview = overview,
                            studyBars = studyBars,
                            testScores = testScorePoints,
                            subjectAccuracies = subjectAccuracies,
                            subjectComparisons = subjectComparisons,
                            lessonProgresses = lessonProgresses,
                            strengthWeakness = swAnalysis,
                            studyTimeTrend = studyTimeTrend,
                            accuracyTrend = accuracyTrend
                        )
                    }
                }.collect { state: StatsUiState ->
                    emit(state)
                }
            } catch (e: Exception) {
                emit(StatsUiState.Error(e.message ?: "Đã có lỗi khi tải thống kê"))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState.Loading)
}
