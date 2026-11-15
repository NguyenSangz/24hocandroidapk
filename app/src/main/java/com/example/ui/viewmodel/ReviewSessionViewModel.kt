package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.PracticeRepository
import com.example.data.repository.UserProgressRepository
import com.example.data.repository.WrongQuestionRepository
import com.example.domain.evaluator.QuestionGrader
import com.example.domain.mapper.QuestionMapper
import com.example.domain.model.GradeResult
import com.example.domain.model.Question
import com.example.domain.model.UserAnswer
import com.example.domain.model.WrongQuestionStatus
import com.example.ui.screens.practice.AttemptQuestionRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Trạng thái phiên ôn tập câu sai (F08)
 */
sealed class ReviewSessionStatus {
    data object Idle : ReviewSessionStatus()
    data object Loading : ReviewSessionStatus()
    data object InProgress : ReviewSessionStatus()
    data object Completed : ReviewSessionStatus()
    data class Error(val message: String) : ReviewSessionStatus()
}

/**
 * UI State cho phiên ôn tập câu sai
 */
data class ReviewSessionUiState(
    val status: ReviewSessionStatus = ReviewSessionStatus.Idle,
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val currentUserAnswer: UserAnswer? = null,
    val stepState: QuestionStepState = QuestionStepState.Answering,
    val questionRecords: List<AttemptQuestionRecord> = emptyList(),
    val showHintDialog: Boolean = false,
    val isNewlyMasteredThisQuestion: Boolean = false,
    val newlyMasteredCount: Int = 0,
    val stillNeedsReviewCount: Int = 0,
    val sessionTitle: String = "Ôn tập câu sai",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
) {
    val currentQuestion: Question? get() = questions.getOrNull(currentIndex)
    val correctCount: Int get() = questionRecords.count { it.gradeResult.isCorrect }
    val totalCount: Int get() = questions.size
}

class ReviewSessionViewModel(
    private val wrongQuestionRepository: WrongQuestionRepository,
    private val practiceRepository: PracticeRepository,
    private val userProgressRepository: UserProgressRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val gamificationRepository: com.example.data.repository.GamificationRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewSessionUiState())
    val uiState: StateFlow<ReviewSessionUiState> = _uiState.asStateFlow()

    private var questionStartTimeMillis: Long = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            combine(
                userPreferencesManager.soundEffectsEnabled,
                userPreferencesManager.vibrationEnabled
            ) { sound, vibration ->
                Pair(sound, vibration)
            }.collect { (sound, vibration) ->
                _uiState.value = _uiState.value.copy(
                    soundEnabled = sound,
                    vibrationEnabled = vibration
                )
            }
        }
    }

    /**
     * Bắt đầu phiên ôn câu sai:
     * Tải câu hỏi từ ngân hàng câu sai, xáo trộn thứ tự các lựa chọn trắc nghiệm để chống học vẹt.
     */
    fun startReviewSession(
        grade: Int = 6,
        subjectId: String? = null,
        chapterId: String? = null,
        count: Int = 10,
        questionIds: List<String>? = null,
        title: String = "Ôn tập câu sai"
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                status = ReviewSessionStatus.Loading,
                sessionTitle = title
            )

            try {
                val questionEntities = wrongQuestionRepository.getQuestionsForReviewSession(
                    grade = grade,
                    subjectId = subjectId,
                    chapterId = chapterId,
                    count = count,
                    questionIds = questionIds
                )

                if (questionEntities.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        status = ReviewSessionStatus.Error("Không có câu hỏi nào cần ôn tập trong danh mục này.")
                    )
                    return@launch
                }

                // Chuyển sang domain Question và xáo trộn phương án trắc nghiệm
                val domainQuestions = questionEntities.map { entity ->
                    val domain = QuestionMapper.toDomain(entity)
                    shuffleQuestionOptions(domain)
                }

                questionStartTimeMillis = System.currentTimeMillis()

                _uiState.value = _uiState.value.copy(
                    status = ReviewSessionStatus.InProgress,
                    questions = domainQuestions,
                    currentIndex = 0,
                    currentUserAnswer = null,
                    stepState = QuestionStepState.Answering,
                    questionRecords = emptyList(),
                    newlyMasteredCount = 0,
                    stillNeedsReviewCount = 0,
                    isNewlyMasteredThisQuestion = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    status = ReviewSessionStatus.Error("Không thể tải danh sách câu hỏi: ${e.message}")
                )
            }
        }
    }

    /**
     * Xáo trộn danh sách lựa chọn trắc nghiệm để tránh học sinh học vẹt theo vị trí (A/B/C/D)
     */
    private fun shuffleQuestionOptions(question: Question): Question {
        return if (question is Question.MultipleChoice) {
            question.copy(options = question.options.shuffled())
        } else {
            question
        }
    }

    fun selectAnswer(answer: UserAnswer) {
        val current = _uiState.value
        if (current.stepState is QuestionStepState.Answering) {
            _uiState.value = current.copy(currentUserAnswer = answer)
        }
    }

    fun toggleHint() {
        val current = _uiState.value
        _uiState.value = current.copy(showHintDialog = !current.showHintDialog)
    }

    /**
     * Nộp bài câu hiện tại: Chấm điểm và cập nhật Spaced Repetition trong Ngân hàng câu sai
     */
    fun submitCurrentAnswer() {
        val current = _uiState.value
        val question = current.currentQuestion ?: return
        val answer = current.currentUserAnswer ?: return
        if (current.stepState !is QuestionStepState.Answering) return

        val gradeResult = QuestionGrader.grade(question, answer)
        val timeSpent = ((System.currentTimeMillis() - questionStartTimeMillis) / 1000).toInt().coerceAtLeast(1)

        val record = AttemptQuestionRecord(
            question = question,
            userAnswer = answer,
            gradeResult = gradeResult,
            usedHint = false,
            timeSpentSeconds = timeSpent
        )

        val initialStillNeedsReview = if (!gradeResult.isCorrect) current.stillNeedsReviewCount + 1 else current.stillNeedsReviewCount

        // Cập nhật ngay UI state sang Graded để giao diện hiển thị phản hồi lập tức
        _uiState.value = current.copy(
            stepState = QuestionStepState.Graded(gradeResult),
            questionRecords = current.questionRecords + record,
            stillNeedsReviewCount = initialStillNeedsReview
        )

        viewModelScope.launch {
            var newlyMastered = _uiState.value.newlyMasteredCount
            var stillNeedsReview = _uiState.value.stillNeedsReviewCount
            var isMasteredNow = false
            val userAnswerStr = serializeUserAnswer(answer)

            if (gradeResult.isCorrect) {
                val updated = wrongQuestionRepository.recordCorrectAnswer(question.id)
                if (updated != null && updated.status == WrongQuestionStatus.MASTERED.code) {
                    newlyMastered++
                    isMasteredNow = true
                    gamificationRepository?.onLearningEvent(
                        com.example.domain.gamification.LearningEvent.WrongQuestionMastered(question.id)
                    )
                }
            } else {
                wrongQuestionRepository.recordWrongAnswer(
                    questionId = question.id,
                    lessonId = question.lessonId,
                    wrongAnswer = userAnswerStr
                )
            }

            // Lưu vết lịch sử làm bài
            practiceRepository.recordAttempt(
                questionId = question.id,
                isCorrect = gradeResult.isCorrect,
                score = gradeResult.score,
                timeSpentSeconds = timeSpent,
                userAnswer = userAnswerStr,
                usedHint = false
            )

            userProgressRepository.recordPracticeActivity(
                answeredCount = 1,
                correctCount = if (gradeResult.isCorrect) 1 else 0
            )
            userPreferencesManager.updateStreakOnActive()

            _uiState.value = _uiState.value.copy(
                newlyMasteredCount = newlyMastered,
                stillNeedsReviewCount = stillNeedsReview,
                isNewlyMasteredThisQuestion = isMasteredNow
            )
        }
    }

    /**
     * Chuyển sang câu hỏi tiếp theo hoặc hoàn thành phiên ôn
     */
    fun nextQuestion() {
        val current = _uiState.value
        val nextIdx = current.currentIndex + 1
        if (nextIdx < current.questions.size) {
            questionStartTimeMillis = System.currentTimeMillis()
            _uiState.value = current.copy(
                currentIndex = nextIdx,
                currentUserAnswer = null,
                stepState = QuestionStepState.Answering,
                isNewlyMasteredThisQuestion = false
            )
        } else {
            _uiState.value = current.copy(
                status = ReviewSessionStatus.Completed,
                isNewlyMasteredThisQuestion = false
            )
        }
    }

    fun exitSession() {
        _uiState.value = ReviewSessionUiState()
    }

    private fun serializeUserAnswer(answer: UserAnswer): String {
        return when (answer) {
            is UserAnswer.MultipleChoice -> answer.selectedOption
            is UserAnswer.TrueFalse -> answer.selectedValue.toString()
            is UserAnswer.FillBlank -> answer.filledText
            is UserAnswer.Matching -> {
                val jsonArr = JSONArray()
                answer.userPairs.forEach { p ->
                    val obj = JSONObject()
                    obj.put("left", p.left)
                    obj.put("right", p.right)
                    jsonArr.put(obj)
                }
                jsonArr.toString()
            }
            is UserAnswer.Ordering -> {
                val jsonArr = JSONArray()
                answer.userOrder.forEach { jsonArr.put(it) }
                jsonArr.toString()
            }
            is UserAnswer.ShortAnswer -> answer.submittedText
        }
    }
}
