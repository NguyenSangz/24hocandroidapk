package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.CurriculumRepository
import com.example.data.repository.PracticeRepository
import com.example.data.repository.UserProgressRepository
import com.example.data.repository.WrongQuestionRepository
import com.example.domain.evaluator.QuestionGrader
import com.example.domain.evaluator.QuestionSelector
import com.example.domain.mapper.QuestionMapper
import com.example.domain.model.Difficulty
import com.example.domain.model.GradeResult
import com.example.domain.model.GradeStatus
import com.example.domain.model.Question
import com.example.domain.model.UserAnswer
import com.example.ui.screens.practice.AttemptQuestionRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Các trạng thái bước trong quá trình làm một câu hỏi (State Machine)
 */
sealed class QuestionStepState {
    data object Answering : QuestionStepState()
    data object Submitted : QuestionStepState()
    data class Graded(val result: GradeResult) : QuestionStepState()
}

/**
 * Trạng thái tổng thể của phiên luyện tập
 */
sealed class PracticeSessionStatus {
    data object Idle : PracticeSessionStatus()
    data object Loading : PracticeSessionStatus()
    data object InProgress : PracticeSessionStatus()
    data object Completed : PracticeSessionStatus()
    data class Error(val message: String) : PracticeSessionStatus()
}

/**
 * UI State toàn diện cho phiên luyện tập (F06)
 */
data class PracticeUiState(
    val status: PracticeSessionStatus = PracticeSessionStatus.Idle,
    val questions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val currentUserAnswer: UserAnswer? = null,
    val stepState: QuestionStepState = QuestionStepState.Answering,
    val questionRecords: List<AttemptQuestionRecord> = emptyList(),
    val showHintDialog: Boolean = false,
    val usedHintInCurrentQuestion: Boolean = false,
    val totalTimeSpentSeconds: Long = 0L,
    val sessionTitle: String = "Luyện tập",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val sessionEarnedXp: Int = 0,
    val gamificationReward: com.example.domain.gamification.GamificationReward? = null
) {
    val currentQuestion: Question? get() = questions.getOrNull(currentIndex)
    val correctCount: Int get() = questionRecords.count { it.gradeResult.isCorrect }
    val totalAnswered: Int get() = questionRecords.size
}

class PracticeViewModel(
    private val practiceRepository: PracticeRepository,
    private val curriculumRepository: CurriculumRepository,
    private val userProgressRepository: UserProgressRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val wrongQuestionRepository: WrongQuestionRepository? = null,
    private val gamificationRepository: com.example.data.repository.GamificationRepository? = null,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val tag = "PracticeViewModel"

    val selectedGrade: StateFlow<Int> = userPreferencesManager.selectedGrade
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)

    val subjects: StateFlow<List<SubjectEntity>> = curriculumRepository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    private var questionStartTimeMillis: Long = System.currentTimeMillis()
    private var sessionStartTimeMillis: Long = System.currentTimeMillis()

    init {
        // Lắng nghe cài đặt âm thanh và rung
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

        // Khôi phục vị trí câu hỏi từ SavedStateHandle nếu có
        savedStateHandle.get<Int>("saved_current_index")?.let { restoredIndex ->
            if (restoredIndex > 0 && _uiState.value.questions.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    currentIndex = restoredIndex.coerceAtMost(_uiState.value.questions.size - 1)
                )
            }
        }
    }

    /**
     * Bắt đầu phiên luyện tập có cấu hình: môn, chương/bài, số lượng câu, độ khó
     */
    fun startPracticeSession(
        lessonId: String? = null,
        subjectId: String? = null,
        chapterId: String? = null,
        count: Int = 10,
        difficultyFilter: String = "MIXED",
        title: String = "Luyện tập"
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                status = PracticeSessionStatus.Loading,
                sessionTitle = title
            )

            try {
                val grade = selectedGrade.value
                val rawEntities: List<QuestionEntity> = when {
                    !lessonId.isNullOrBlank() && lessonId != "none" -> {
                        practiceRepository.getQuestionsForLesson(lessonId)
                    }
                    !chapterId.isNullOrBlank() && chapterId != "all" -> {
                        practiceRepository.getQuestionsByChapter(chapterId)
                    }
                    !subjectId.isNullOrBlank() && subjectId != "none" && subjectId != "all" -> {
                        practiceRepository.getAllQuestionsBySubject(grade, subjectId)
                    }
                    else -> {
                        practiceRepository.getRandomExamQuestions(grade, null, count * 2)
                    }
                }

                if (rawEntities.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        status = PracticeSessionStatus.Error("Chưa có câu hỏi luyện tập cho phần này.")
                    )
                    return@launch
                }

                // Chuyển đổi an toàn từ Entity sang Domain, bắt lỗi và ghi log
                val domainQuestions = rawEntities.mapNotNull { entity ->
                    try {
                        QuestionMapper.toDomain(entity)
                    } catch (e: Exception) {
                        Log.e(tag, "Lỗi chuyển đổi câu hỏi ${entity.id}: ${e.message}")
                        null
                    }
                }

                if (domainQuestions.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        status = PracticeSessionStatus.Error("Không có câu hỏi hợp lệ để luyện tập.")
                    )
                    return@launch
                }

                // Lọc theo độ khó nếu có yêu cầu
                val filteredByDifficulty = when (difficultyFilter.uppercase()) {
                    "EASY", "DỄ" -> domainQuestions.filter { it.difficulty == Difficulty.RECOGNITION }
                    "MEDIUM", "VỪA" -> domainQuestions.filter { it.difficulty == Difficulty.COMPREHENSION }
                    "HARD", "KHÓ" -> domainQuestions.filter { it.difficulty == Difficulty.APPLICATION || it.difficulty == Difficulty.HIGH_APPLICATION }
                    else -> domainQuestions // Hỗn hợp
                }.ifEmpty { domainQuestions }

                // Chọn N câu theo thuật toán QuestionSelector
                val selected = QuestionSelector.selectQuestions(
                    questions = filteredByDifficulty,
                    totalCount = count.coerceAtLeast(1)
                ).map { q ->
                    // Xáo trộn phương án trắc nghiệm nếu là MultipleChoice
                    if (q is Question.MultipleChoice) {
                        QuestionSelector.shuffleMultipleChoiceOptions(q)
                    } else q
                }

                sessionStartTimeMillis = System.currentTimeMillis()
                questionStartTimeMillis = System.currentTimeMillis()

                savedStateHandle["saved_current_index"] = 0

                _uiState.value = PracticeUiState(
                    status = PracticeSessionStatus.InProgress,
                    questions = selected,
                    currentIndex = 0,
                    currentUserAnswer = null,
                    stepState = QuestionStepState.Answering,
                    questionRecords = emptyList(),
                    showHintDialog = false,
                    usedHintInCurrentQuestion = false,
                    totalTimeSpentSeconds = 0L,
                    sessionTitle = title,
                    soundEnabled = _uiState.value.soundEnabled,
                    vibrationEnabled = _uiState.value.vibrationEnabled
                )

            } catch (e: Exception) {
                Log.e(tag, "Lỗi khởi tạo phiên luyện tập: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    status = PracticeSessionStatus.Error("Đã xảy ra sự cố khi tải câu hỏi: ${e.message}")
                )
            }
        }
    }

    /**
     * Bắt đầu nhanh cho bài học cụ thể
     */
    fun startPracticeForLesson(lessonId: String, count: Int = 10) {
        viewModelScope.launch {
            val lesson = curriculumRepository.getLesson(lessonId)
            val title = if (lesson != null) "Luyện tập: ${lesson.title}" else "Luyện tập bài học"
            startPracticeSession(lessonId = lessonId, count = count, title = title)
        }
    }

    /**
     * Bắt đầu luyện tập nhanh theo môn
     */
    fun startQuickPractice(subjectId: String?, count: Int = 10) {
        val title = when (subjectId) {
            "math" -> "Luyện tập Toán"
            "english" -> "Luyện tập Tiếng Anh"
            "literature" -> "Luyện tập Ngữ văn"
            else -> "Luyện tập nhanh"
        }
        startPracticeSession(subjectId = subjectId, count = count, title = title)
    }

    /**
     * Cập nhật câu trả lời của học sinh cho câu hiện tại
     */
    fun selectAnswer(answer: UserAnswer) {
        val current = _uiState.value
        if (current.stepState is QuestionStepState.Answering) {
            _uiState.value = current.copy(currentUserAnswer = answer)
        }
    }

    /**
     * Chọn nhanh chuỗi trả lời (Trắc nghiệm, Đúng/Sai, Điền khuyết)
     */
    fun selectAnswerText(text: String) {
        val current = _uiState.value
        val q = current.currentQuestion ?: return
        when (q) {
            is Question.MultipleChoice -> selectAnswer(UserAnswer.MultipleChoice(text))
            is Question.TrueFalse -> {
                val boolVal = text.trim().lowercase() == "true" || text.trim() == "Đúng" || text.trim() == "dung"
                selectAnswer(UserAnswer.TrueFalse(boolVal))
            }
            is Question.FillBlank -> selectAnswer(UserAnswer.FillBlank(text))
            is Question.ShortAnswer -> selectAnswer(UserAnswer.ShortAnswer(text))
            else -> {}
        }
    }

    /**
     * Bật / tắt gợi ý, ghi nhận việc đã dùng gợi ý
     */
    fun toggleHint() {
        val current = _uiState.value
        _uiState.value = current.copy(
            showHintDialog = !current.showHintDialog,
            usedHintInCurrentQuestion = true
        )
    }

    /**
     * Nộp bài và chấm điểm câu hiện tại
     */
    fun submitCurrentAnswer() {
        val current = _uiState.value
        val question = current.currentQuestion ?: return
        val answer = current.currentUserAnswer ?: return
        if (current.stepState !is QuestionStepState.Answering) return

        // Chấm điểm bằng QuestionGrader
        val gradeResult = QuestionGrader.grade(question, answer)
        val timeSpent = ((System.currentTimeMillis() - questionStartTimeMillis) / 1000).toInt().coerceAtLeast(1)

        val record = AttemptQuestionRecord(
            question = question,
            userAnswer = answer,
            gradeResult = gradeResult,
            usedHint = current.usedHintInCurrentQuestion,
            timeSpentSeconds = timeSpent
        )

        val updatedRecords = current.questionRecords + record
        val totalTime = ((System.currentTimeMillis() - sessionStartTimeMillis) / 1000)

        _uiState.value = current.copy(
            stepState = QuestionStepState.Graded(gradeResult),
            questionRecords = updatedRecords,
            totalTimeSpentSeconds = totalTime
        )

        // Lưu vào DB (AttemptEntity)
        viewModelScope.launch {
            val userAnswerStr = serializeUserAnswer(answer)
            practiceRepository.recordAttempt(
                questionId = question.id,
                isCorrect = gradeResult.isCorrect,
                score = gradeResult.score,
                timeSpentSeconds = timeSpent,
                userAnswer = userAnswerStr,
                usedHint = current.usedHintInCurrentQuestion
            )

            // Nếu trả lời sai hoặc đúng một phần (< 100%), ghi nhận vào Ngân hàng câu sai (F08)
            if (!gradeResult.isCorrect || gradeResult.score < 1.0f) {
                wrongQuestionRepository?.recordWrongAnswer(
                    questionId = question.id,
                    lessonId = question.lessonId,
                    wrongAnswer = userAnswerStr
                )
            } else {
                // Nếu người học làm đúng câu đó ở luyện tập thường thì cũng tính vào chuỗi đúng (F08)
                wrongQuestionRepository?.recordCorrectAnswer(question.id)
            }

            // Ghi nhận hoạt động vào tiến độ chung
            userProgressRepository.recordPracticeActivity(
                answeredCount = 1,
                correctCount = if (gradeResult.isCorrect) 1 else 0
            )
            userPreferencesManager.updateStreakOnActive()

            // Ghi nhận sự kiện Gamification (F10)
            if (gamificationRepository != null) {
                val reward = gamificationRepository.onLearningEvent(
                    com.example.domain.gamification.LearningEvent.PracticeQuestionAnswered(
                        questionId = question.id,
                        isCorrect = gradeResult.isCorrect,
                        usedHint = current.usedHintInCurrentQuestion,
                        consecutiveCorrectInSession = updatedRecords.count { it.gradeResult.isCorrect }
                    )
                )
                _uiState.value = _uiState.value.copy(
                    sessionEarnedXp = _uiState.value.sessionEarnedXp + reward.xpEarned
                )
            }
        }
    }

    /**
     * Tự chấm điểm cho câu hỏi Tự luận ngắn (Short Answer)
     */
    fun selfEvaluateShortAnswer(isCorrect: Boolean) {
        val current = _uiState.value
        val question = current.currentQuestion ?: return
        val step = current.stepState
        if (step !is QuestionStepState.Graded) return

        val oldResult = step.result
        val updatedResult = oldResult.copy(
            isCorrect = isCorrect,
            score = if (isCorrect) 1.0f else 0.0f,
            status = if (isCorrect) GradeStatus.CORRECT else GradeStatus.INCORRECT,
            feedbackMessage = if (isCorrect) "Bạn đã tự xác nhận câu trả lời chính xác!" else "Đã ghi nhận câu trả lời cần ôn lại."
        )

        val updatedRecords = current.questionRecords.toMutableList()
        val lastIdx = updatedRecords.lastIndex
        if (lastIdx >= 0) {
            val last = updatedRecords[lastIdx]
            updatedRecords[lastIdx] = last.copy(gradeResult = updatedResult)
        }

        _uiState.value = current.copy(
            stepState = QuestionStepState.Graded(updatedResult),
            questionRecords = updatedRecords
        )

        // Cập nhật lại trong DB
        viewModelScope.launch {
            val answerStr = serializeUserAnswer(current.currentUserAnswer)
            practiceRepository.recordAttempt(
                questionId = question.id,
                isCorrect = isCorrect,
                score = if (isCorrect) 1.0f else 0.0f,
                timeSpentSeconds = 5,
                userAnswer = answerStr,
                usedHint = current.usedHintInCurrentQuestion
            )
        }
    }

    /**
     * Chuyển sang câu tiếp theo hoặc kết thúc phiên
     */
    fun nextQuestion() {
        val current = _uiState.value
        val nextIdx = current.currentIndex + 1

        if (nextIdx < current.questions.size) {
            questionStartTimeMillis = System.currentTimeMillis()
            savedStateHandle["saved_current_index"] = nextIdx

            _uiState.value = current.copy(
                currentIndex = nextIdx,
                currentUserAnswer = null,
                stepState = QuestionStepState.Answering,
                showHintDialog = false,
                usedHintInCurrentQuestion = false
            )
        } else {
            // Hoàn thành phiên
            val totalTime = ((System.currentTimeMillis() - sessionStartTimeMillis) / 1000)
            savedStateHandle.remove<Int>("saved_current_index")
            _uiState.value = current.copy(
                status = PracticeSessionStatus.Completed,
                totalTimeSpentSeconds = totalTime
            )

            // Ghi nhận thưởng hoàn thành phiên luyện tập (F10)
            viewModelScope.launch {
                if (gamificationRepository != null) {
                    val reward = gamificationRepository.onLearningEvent(
                        com.example.domain.gamification.LearningEvent.PracticeSessionCompleted(
                            questionCount = current.questions.size,
                            correctCount = current.correctCount
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        sessionEarnedXp = _uiState.value.sessionEarnedXp + reward.xpEarned,
                        gamificationReward = reward
                    )
                }
            }
        }
    }

    /**
     * Làm lại các câu sai trong phiên hiện tại
     */
    fun retryWrongQuestions() {
        val current = _uiState.value
        val wrongQuestions = current.questionRecords
            .filter { !it.gradeResult.isCorrect }
            .map { it.question }

        if (wrongQuestions.isEmpty()) return

        sessionStartTimeMillis = System.currentTimeMillis()
        questionStartTimeMillis = System.currentTimeMillis()
        savedStateHandle["saved_current_index"] = 0

        _uiState.value = PracticeUiState(
            status = PracticeSessionStatus.InProgress,
            questions = wrongQuestions,
            currentIndex = 0,
            currentUserAnswer = null,
            stepState = QuestionStepState.Answering,
            questionRecords = emptyList(),
            showHintDialog = false,
            usedHintInCurrentQuestion = false,
            totalTimeSpentSeconds = 0L,
            sessionTitle = "Làm lại câu sai (${wrongQuestions.size} câu)",
            soundEnabled = current.soundEnabled,
            vibrationEnabled = current.vibrationEnabled
        )
    }

    /**
     * Thoát phiên luyện tập
     */
    fun exitSession() {
        savedStateHandle.remove<Int>("saved_current_index")
        _uiState.value = PracticeUiState(status = PracticeSessionStatus.Idle)
    }

    /**
     * Lấy danh sách chương theo môn học cho bộ chọn
     */
    fun getChaptersForSubject(subjectId: String): kotlinx.coroutines.flow.Flow<List<ChapterEntity>> {
        return curriculumRepository.getChapters(selectedGrade.value, subjectId)
    }

    /**
     * Lấy danh sách bài học theo chương
     */
    fun getLessonsForChapter(chapterId: String): kotlinx.coroutines.flow.Flow<List<LessonEntity>> {
        return curriculumRepository.getLessons(chapterId)
    }

    private fun serializeUserAnswer(answer: UserAnswer?): String {
        return when (answer) {
            is UserAnswer.MultipleChoice -> answer.selectedOption
            is UserAnswer.TrueFalse -> if (answer.selectedValue) "Đúng" else "Sai"
            is UserAnswer.FillBlank -> answer.filledText
            is UserAnswer.Matching -> answer.userPairs.joinToString("; ") { "${it.left} -> ${it.right}" }
            is UserAnswer.Ordering -> answer.userOrder.joinToString(" -> ")
            is UserAnswer.ShortAnswer -> answer.submittedText
            null -> ""
        }
    }
}
