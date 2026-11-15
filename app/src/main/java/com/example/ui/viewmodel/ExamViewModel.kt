package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ExamResultEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.PracticeRepository
import com.example.data.repository.UserProgressRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray

data class ExamTakingState(
    val title: String = "Đề kiểm tra",
    val questions: List<QuestionEntity> = emptyList(),
    val currentIndex: Int = 0,
    val userAnswers: Map<Int, String> = emptyMap(),
    val totalTimeSeconds: Int = 900, // 15 mins default
    val remainingSeconds: Int = 900,
    val isSubmitted: Boolean = false,
    val score: Float = 0f,
    val correctCount: Int = 0,
    val totalQuestions: Int = 0,
    val isLoading: Boolean = false
)

class ExamViewModel(
    private val practiceRepository: PracticeRepository,
    private val userProgressRepository: UserProgressRepository,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    val selectedGrade: StateFlow<Int> = userPreferencesManager.selectedGrade
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)

    private val _examState = MutableStateFlow(ExamTakingState())
    val examState: StateFlow<ExamTakingState> = _examState.asStateFlow()

    private var timerJob: Job? = null

    fun startExam(title: String, subjectId: String?, durationMinutes: Int = 15, questionCount: Int = 10) {
        timerJob?.cancel()
        viewModelScope.launch {
            _examState.value = ExamTakingState(isLoading = true, title = title)
            val grade = selectedGrade.value
            val questions = practiceRepository.getRandomExamQuestions(grade, subjectId, questionCount)
            val totalSeconds = durationMinutes * 60

            _examState.value = ExamTakingState(
                title = title,
                questions = questions,
                totalTimeSeconds = totalSeconds,
                remainingSeconds = totalSeconds,
                totalQuestions = questions.size,
                isLoading = false
            )

            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_examState.value.remainingSeconds > 0 && !_examState.value.isSubmitted) {
                delay(1000)
                val newRemaining = _examState.value.remainingSeconds - 1
                _examState.value = _examState.value.copy(remainingSeconds = newRemaining)
                if (newRemaining <= 0) {
                    submitExam()
                    break
                }
            }
        }
    }

    fun selectAnswerForCurrentQuestion(answer: String) {
        val state = _examState.value
        if (!state.isSubmitted) {
            val updated = state.userAnswers.toMutableMap()
            updated[state.currentIndex] = answer
            _examState.value = state.copy(userAnswers = updated)
        }
    }

    fun jumpToQuestion(index: Int) {
        val state = _examState.value
        if (index in state.questions.indices) {
            _examState.value = state.copy(currentIndex = index)
        }
    }

    fun nextQuestion() {
        val state = _examState.value
        if (state.currentIndex + 1 < state.questions.size) {
            _examState.value = state.copy(currentIndex = state.currentIndex + 1)
        }
    }

    fun prevQuestion() {
        val state = _examState.value
        if (state.currentIndex > 0) {
            _examState.value = state.copy(currentIndex = state.currentIndex - 1)
        }
    }

    fun submitExam() {
        timerJob?.cancel()
        val state = _examState.value
        if (state.isSubmitted) return

        var correct = 0
        state.questions.forEachIndexed { index, question ->
            val userAns = state.userAnswers[index]?.trim() ?: ""
            if (userAns.equals(question.correctAnswer.trim(), ignoreCase = true)) {
                correct++
            }
        }

        val total = state.questions.size.coerceAtLeast(1)
        // Thang điểm 10 theo chuẩn giáo dục Việt Nam
        val rawScore = (correct.toFloat() / total.toFloat()) * 10f
        val roundedScore = Math.round(rawScore * 10f) / 10f
        val timeSpent = state.totalTimeSeconds - state.remainingSeconds

        _examState.value = state.copy(
            isSubmitted = true,
            score = roundedScore,
            correctCount = correct
        )

        viewModelScope.launch {
            practiceRepository.saveExamResult(
                ExamResultEntity(
                    title = state.title,
                    grade = selectedGrade.value,
                    subjectId = "math", // or selected subject
                    score = roundedScore,
                    totalQuestions = total,
                    correctCount = correct,
                    timeSpentSeconds = timeSpent
                )
            )
            userProgressRepository.recordPracticeActivity(
                answeredCount = total,
                correctCount = correct,
                studyMinutes = (timeSpent / 60).coerceAtLeast(1)
            )
        }
    }

    fun parseOptions(optionsJson: String): List<String> {
        return try {
            val array = JSONArray(optionsJson)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
