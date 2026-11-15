package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TestAnswerEntity
import com.example.data.local.entity.TestEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.CurriculumRepository
import com.example.data.repository.PracticeRepository
import com.example.data.repository.TestRepository
import com.example.data.repository.UserProgressRepository
import com.example.domain.evaluator.ExamRuleConfig
import com.example.domain.evaluator.QuestionGrader
import com.example.domain.evaluator.TestBuilder
import com.example.domain.evaluator.TestScorer
import com.example.domain.evaluator.TestTimer
import com.example.domain.mapper.QuestionMapper
import com.example.domain.model.GradeResult
import com.example.domain.model.Question
import com.example.domain.model.TestAnswerDetail
import com.example.domain.model.TestBuildResult
import com.example.domain.model.TestScoreSummary
import com.example.domain.model.TestType
import com.example.domain.model.TimerWarning
import com.example.domain.model.UserAnswer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Trạng thái State Machine cho bài kiểm tra (F07)
 */
sealed class TestUiState {
    data class Configuring(
        val selectedType: TestType = TestType.MID_TERM,
        val selectedSubjectId: String = "math",
        val selectedChapterId: String = "all",
        val selectedLessonId: String = "none",
        val selectedQuestionCount: Int = 20,
        val selectedDurationMinutes: Int = 45,
        val chapters: List<ChapterEntity> = emptyList(),
        val lessons: List<LessonEntity> = emptyList(),
        val hasActiveDraft: Boolean = false,
        val draftTest: TestEntity? = null,
        val validationError: String? = null,
        val suggestedCount: Int? = null
    ) : TestUiState()

    data class Confirming(
        val testTitle: String,
        val testType: TestType,
        val subjectName: String,
        val questionCount: Int,
        val durationMinutes: Int,
        val rules: List<String>
    ) : TestUiState()

    data class InProgress(
        val testId: String,
        val title: String,
        val testType: TestType,
        val subjectId: String,
        val subjectName: String,
        val grade: Int,
        val questions: List<Question>,
        val currentIndex: Int = 0,
        val userAnswers: Map<Int, UserAnswer?> = emptyMap(),
        val flaggedIndices: Set<Int> = emptySet(),
        val totalDurationSeconds: Int,
        val remainingSeconds: Int,
        val isCriticalTime: Boolean = false,
        val warningMessage: String? = null,
        val showSubmitConfirmDialog: Boolean = false,
        val showExitConfirmDialog: Boolean = false,
        val isSavingDraft: Boolean = false
    ) : TestUiState()

    data class Submitted(
        val testId: String,
        val title: String,
        val testType: TestType,
        val subjectName: String,
        val grade: Int,
        val timeSpentSeconds: Int,
        val scoreSummary: TestScoreSummary,
        val answerDetails: List<TestAnswerDetail>,
        val earnedXp: Int = 0,
        val gamificationReward: com.example.domain.gamification.GamificationReward? = null
    ) : TestUiState()

    data class Reviewing(
        val testId: String,
        val title: String,
        val answerDetails: List<TestAnswerDetail>,
        val currentIndex: Int = 0,
        val filterOnlyWrong: Boolean = false,
        val scoreSummary: TestScoreSummary
    ) : TestUiState()

    data class Error(val message: String) : TestUiState()
}

class TestViewModel(
    private val testRepository: TestRepository,
    private val curriculumRepository: CurriculumRepository,
    private val practiceRepository: PracticeRepository,
    private val userProgressRepository: UserProgressRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val gamificationRepository: com.example.data.repository.GamificationRepository? = null,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
) : ViewModel() {

    private val tag = "TestViewModel"

    val selectedGrade: StateFlow<Int> = userPreferencesManager.selectedGrade
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)

    val subjects: StateFlow<List<SubjectEntity>> = curriculumRepository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow<TestUiState>(TestUiState.Configuring())
    val uiState: StateFlow<TestUiState> = _uiState.asStateFlow()

    private var activeTimer: TestTimer? = null
    private var timerJob: Job? = null
    private var draftSaveJob: Job? = null

    init {
        // Tải cấu hình mặc định và kiểm tra bài thi dở
        viewModelScope.launch {
            val grade = selectedGrade.value
            checkForActiveDraft(grade)
            loadChaptersForSubject("math", grade)
        }
    }

    /**
     * Kiểm tra có bài thi dở dang nào chưa nộp không
     */
    fun checkForActiveDraft(grade: Int = selectedGrade.value, subjectId: String = "all") {
        viewModelScope.launch {
            val draft = testRepository.getActiveDraft(grade, subjectId)
            val currentState = _uiState.value
            if (currentState is TestUiState.Configuring) {
                _uiState.value = currentState.copy(
                    hasActiveDraft = draft != null,
                    draftTest = draft
                )
            }
        }
    }

    /**
     * Cập nhật loại đề (theo bài, theo chương, giữa kỳ, cuối kỳ)
     */
    fun onSelectTestType(type: TestType) {
        val currentState = _uiState.value as? TestUiState.Configuring ?: return
        _uiState.value = currentState.copy(
            selectedType = type,
            selectedQuestionCount = ExamRuleConfig.getDefaultQuestionCount(type),
            selectedDurationMinutes = ExamRuleConfig.getDefaultDurationMinutes(type),
            validationError = null,
            suggestedCount = null
        )
    }

    /**
     * Cập nhật môn học
     */
    fun onSelectSubject(subjectId: String) {
        val currentState = _uiState.value as? TestUiState.Configuring ?: return
        _uiState.value = currentState.copy(
            selectedSubjectId = subjectId,
            selectedChapterId = "all",
            selectedLessonId = "none",
            validationError = null,
            suggestedCount = null
        )
        loadChaptersForSubject(subjectId, selectedGrade.value)
    }

    /**
     * Cập nhật chương
     */
    fun onSelectChapter(chapterId: String) {
        val currentState = _uiState.value as? TestUiState.Configuring ?: return
        _uiState.value = currentState.copy(
            selectedChapterId = chapterId,
            selectedLessonId = "none",
            validationError = null,
            suggestedCount = null
        )
        if (chapterId != "all") {
            loadLessonsForChapter(chapterId)
        }
    }

    /**
     * Cập nhật bài học
     */
    fun onSelectLesson(lessonId: String) {
        val currentState = _uiState.value as? TestUiState.Configuring ?: return
        _uiState.value = currentState.copy(
            selectedLessonId = lessonId,
            validationError = null,
            suggestedCount = null
        )
    }

    /**
     * Cập nhật số câu hỏi
     */
    fun onSelectQuestionCount(count: Int) {
        val currentState = _uiState.value as? TestUiState.Configuring ?: return
        _uiState.value = currentState.copy(
            selectedQuestionCount = count,
            validationError = null,
            suggestedCount = null
        )
    }

    /**
     * Cập nhật thời gian làm bài (phút)
     */
    fun onSelectDurationMinutes(minutes: Int) {
        val currentState = _uiState.value as? TestUiState.Configuring ?: return
        _uiState.value = currentState.copy(selectedDurationMinutes = minutes)
    }

    /**
     * Tiến hành xác nhận trước khi làm bài
     */
    fun proceedToConfirmation() {
        val config = _uiState.value as? TestUiState.Configuring ?: return
        viewModelScope.launch {
            val grade = selectedGrade.value
            val subject = subjects.value.find { it.id == config.selectedSubjectId }
            val subjectName = subject?.name ?: "Toán học"

            // Kiểm tra số lượng câu hỏi trước khi sang màn xác nhận
            val rawQuestions = loadRawQuestionsForConfig(config, grade)
            val buildResult = TestBuilder.buildTest(
                sourceQuestions = rawQuestions.map { QuestionMapper.toDomain(it) },
                requestedCount = config.selectedQuestionCount,
                testType = config.selectedType
            )

            when (buildResult) {
                is TestBuildResult.InsufficientQuestions -> {
                    _uiState.value = config.copy(
                        validationError = buildResult.message,
                        suggestedCount = buildResult.suggestedCount
                    )
                }
                is TestBuildResult.Error -> {
                    _uiState.value = config.copy(validationError = buildResult.message)
                }
                is TestBuildResult.Success -> {
                    val title = "${config.selectedType.title} - $subjectName"
                    _uiState.value = TestUiState.Confirming(
                        testTitle = title,
                        testType = config.selectedType,
                        subjectName = subjectName,
                        questionCount = config.selectedQuestionCount,
                        durationMinutes = config.selectedDurationMinutes,
                        rules = ExamRuleConfig.examRulesText
                    )
                }
            }
        }
    }

    /**
     * Bắt đầu làm bài kiểm tra từ màn hình xác nhận
     */
    fun startTestFromConfirmation() {
        val confirming = _uiState.value as? TestUiState.Confirming ?: return
        viewModelScope.launch {
            val grade = selectedGrade.value
            val subject = subjects.value.find { it.name == confirming.subjectName }
            val subjectId = subject?.id ?: "math"

            // Lấy lại cấu hình để build danh sách câu hỏi
            val rawQuestions = practiceRepository.getAllQuestionsBySubject(grade, subjectId)
            val domainQuestions = rawQuestions.map { QuestionMapper.toDomain(it) }

            val buildResult = TestBuilder.buildTest(
                sourceQuestions = domainQuestions,
                requestedCount = confirming.questionCount,
                testType = confirming.testType
            )

            if (buildResult is TestBuildResult.Success) {
                val questions = buildResult.questions
                val testId = UUID.randomUUID().toString()
                val totalSeconds = confirming.durationMinutes * 60

                // Khởi tạo timer với endTimeMillis chính xác
                val timer = TestTimer(totalDurationSeconds = totalSeconds)
                activeTimer = timer

                val inProgressState = TestUiState.InProgress(
                    testId = testId,
                    title = confirming.testTitle,
                    testType = confirming.testType,
                    subjectId = subjectId,
                    subjectName = confirming.subjectName,
                    grade = grade,
                    questions = questions,
                    currentIndex = 0,
                    userAnswers = emptyMap(),
                    flaggedIndices = emptySet(),
                    totalDurationSeconds = totalSeconds,
                    remainingSeconds = totalSeconds,
                    isCriticalTime = false
                )
                _uiState.value = inProgressState

                // Bắt đầu ticker đếm ngược
                startTimerTicker()

                // Lưu nháp ban đầu
                saveDraftInternal(inProgressState)
            } else {
                _uiState.value = TestUiState.Error("Không thể tạo bài kiểm tra do ngân hàng câu hỏi chưa sẵn sàng.")
            }
        }
    }

    /**
     * Tiếp tục bài kiểm tra đang làm dở
     */
    fun resumeDraftTest(draft: TestEntity) {
        viewModelScope.launch {
            val answers = testRepository.getDraftAnswers(draft.id)
            val nowMillis = System.currentTimeMillis()

            // Kiểm tra mốc endTimeEpochMillis
            val remainingSeconds = if (draft.endTimeEpochMillis > 0L) {
                val diff = (draft.endTimeEpochMillis - nowMillis) / 1000L
                diff.coerceAtLeast(0L).toInt()
            } else {
                draft.remainingSeconds
            }

            if (remainingSeconds <= 0) {
                // Hết giờ trong lúc app đóng: thông báo và tự nộp bài
                _uiState.value = TestUiState.Error("Bài kiểm tra dở trước đó đã hết giờ làm bài.")
                testRepository.deleteTest(draft.id)
                return@launch
            }

            // Tải lại các câu hỏi
            val rawQuestions = practiceRepository.getAllQuestionsBySubject(draft.grade, draft.subjectId)
            val questionMap = rawQuestions.associate { it.id to QuestionMapper.toDomain(it) }

            val orderedQuestions = answers.mapNotNull { questionMap[it.questionId] }
            if (orderedQuestions.isEmpty()) {
                _uiState.value = TestUiState.Error("Không tìm thấy dữ liệu câu hỏi của bài thi nháp.")
                return@launch
            }

            val totalDurationSeconds = draft.durationMinutes * 60
            val timer = TestTimer(
                totalDurationSeconds = totalDurationSeconds,
                initialEndTimeMillis = if (draft.endTimeEpochMillis > 0L) draft.endTimeEpochMillis else (nowMillis + remainingSeconds * 1000L)
            )
            activeTimer = timer

            // Khôi phục đáp án đã chọn
            val restoredAnswers = mutableMapOf<Int, UserAnswer?>()
            val restoredFlags = mutableSetOf<Int>()

            answers.forEachIndexed { index, ansEntity ->
                if (ansEntity.isFlagged) {
                    restoredFlags.add(index)
                }
                if (ansEntity.isAnswered && ansEntity.userAnswerJson.isNotBlank()) {
                    val q = orderedQuestions.getOrNull(index)
                    if (q != null) {
                        restoredAnswers[index] = deserializeUserAnswer(q, ansEntity.userAnswerJson)
                    }
                }
            }

            val inProgressState = TestUiState.InProgress(
                testId = draft.id,
                title = draft.title,
                testType = TestType.fromString(draft.type),
                subjectId = draft.subjectId,
                subjectName = draft.subjectName,
                grade = draft.grade,
                questions = orderedQuestions,
                currentIndex = 0,
                userAnswers = restoredAnswers,
                flaggedIndices = restoredFlags,
                totalDurationSeconds = totalDurationSeconds,
                remainingSeconds = remainingSeconds,
                isCriticalTime = timer.isCriticalTime(remainingSeconds)
            )
            _uiState.value = inProgressState
            startTimerTicker()
        }
    }

    /**
     * Vòng lặp đếm ngược chính xác, cập nhật remainingSeconds, cảnh báo 5p/1p, tự nộp khi hết giờ
     */
    private fun startTimerTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var previousRemaining = activeTimer?.calculateRemainingSeconds() ?: 0
            while (true) {
                delay(1000)
                val timer = activeTimer ?: break
                val currentRemaining = timer.calculateRemainingSeconds()

                val state = _uiState.value as? TestUiState.InProgress ?: break

                val isCritical = timer.isCriticalTime(currentRemaining)
                val warning = timer.checkWarningNotification(previousRemaining, currentRemaining)
                val warningMsg = when (warning) {
                    TimerWarning.FIVE_MINUTES_LEFT -> "Thời gian làm bài còn lại 5 phút! Hãy rà soát lại bài làm nhé."
                    TimerWarning.ONE_MINUTE_LEFT -> "Chỉ còn 1 phút cuối cùng! Hệ thống sẽ tự động nộp bài khi hết giờ."
                    null -> state.warningMessage
                }

                _uiState.value = state.copy(
                    remainingSeconds = currentRemaining,
                    isCriticalTime = isCritical,
                    warningMessage = warningMsg
                )

                previousRemaining = currentRemaining

                // Hết giờ: Tự động nộp bài
                if (currentRemaining <= 0) {
                    submitTest(isAutoSubmitOnTimeUp = true)
                    break
                }
            }
        }
    }

    /**
     * Chọn hoặc thay đổi câu trả lời cho câu hỏi hiện tại
     */
    fun selectAnswerForCurrentQuestion(answer: UserAnswer) {
        val state = _uiState.value as? TestUiState.InProgress ?: return
        val updatedAnswers = state.userAnswers.toMutableMap()
        updatedAnswers[state.currentIndex] = answer
        val newState = state.copy(userAnswers = updatedAnswers)
        _uiState.value = newState

        // Tự động lưu nháp sau khi người học trả lời (debounce nhẹ)
        debounceSaveDraft(newState)
    }

    /**
     * Bật/Tắt cờ "Đánh dấu xem lại sau" cho câu hỏi hiện tại
     */
    fun toggleFlagCurrentQuestion() {
        val state = _uiState.value as? TestUiState.InProgress ?: return
        val currentFlags = state.flaggedIndices.toMutableSet()
        if (currentFlags.contains(state.currentIndex)) {
            currentFlags.remove(state.currentIndex)
        } else {
            currentFlags.add(state.currentIndex)
        }
        val newState = state.copy(flaggedIndices = currentFlags)
        _uiState.value = newState
        debounceSaveDraft(newState)
    }

    /**
     * Chuyển đến câu hỏi theo chỉ số index (0..N-1)
     */
    fun jumpToQuestion(index: Int) {
        val state = _uiState.value as? TestUiState.InProgress ?: return
        if (index in state.questions.indices) {
            _uiState.value = state.copy(currentIndex = index)
        }
    }

    fun nextQuestion() {
        val state = _uiState.value as? TestUiState.InProgress ?: return
        if (state.currentIndex < state.questions.size - 1) {
            _uiState.value = state.copy(currentIndex = state.currentIndex + 1)
        }
    }

    fun previousQuestion() {
        val state = _uiState.value as? TestUiState.InProgress ?: return
        if (state.currentIndex > 0) {
            _uiState.value = state.copy(currentIndex = state.currentIndex - 1)
        }
    }

    /**
     * Hiển thị / Đóng hộp thoại xác nhận nộp bài
     */
    fun setSubmitConfirmDialogVisible(visible: Boolean) {
        val state = _uiState.value as? TestUiState.InProgress ?: return
        _uiState.value = state.copy(showSubmitConfirmDialog = visible)
    }

    /**
     * Hiển thị / Đóng hộp thoại xác nhận thoát bài thi
     */
    fun setExitConfirmDialogVisible(visible: Boolean) {
        val state = _uiState.value as? TestUiState.InProgress ?: return
        _uiState.value = state.copy(showExitConfirmDialog = visible)
    }

    /**
     * Xóa thông báo cảnh báo sau khi người dùng đã xem
     */
    fun dismissWarningMessage() {
        val state = _uiState.value as? TestUiState.InProgress ?: return
        _uiState.value = state.copy(warningMessage = null)
    }

    /**
     * Nộp bài kiểm tra:
     * - Dừng timer
     * - Chấm toàn bộ bài theo TestScorer
     * - Lưu TestEntity và TestAnswerEntity vào Room
     * - Ghi nhận vào AttemptDao và WrongQuestionRepository
     */
    fun submitTest(isAutoSubmitOnTimeUp: Boolean = false) {
        timerJob?.cancel()
        val state = _uiState.value as? TestUiState.InProgress ?: return

        viewModelScope.launch {
            val timer = activeTimer
            val timeSpentSeconds = timer?.calculateTimeSpentSeconds() ?: (state.totalDurationSeconds - state.remainingSeconds)

            // Dựng danh sách TestAnswerDetail
            val answerDetails = state.questions.mapIndexed { index, question ->
                val answer = state.userAnswers[index]
                val isAnswered = answer != null
                val isFlagged = state.flaggedIndices.contains(index)
                TestAnswerDetail(
                    question = question,
                    userAnswer = answer,
                    isFlagged = isFlagged,
                    isAnswered = isAnswered,
                    chapterTitle = question.lessonId
                )
            }

            // Chấm điểm toàn bài bằng TestScorer
            val (summary, gradedDetails) = TestScorer.scoreTest(answerDetails)

            // Lưu bài thi vào Room
            val testEntity = TestEntity(
                id = state.testId,
                title = state.title,
                type = state.testType.name,
                subjectId = state.subjectId,
                subjectName = state.subjectName,
                grade = state.grade,
                score = summary.finalScore,
                maxScore = 10.0f,
                totalQuestions = summary.totalQuestions,
                correctCount = summary.correctCount,
                timeSpentSeconds = timeSpentSeconds,
                durationMinutes = state.totalDurationSeconds / 60,
                timestamp = System.currentTimeMillis(),
                isDraft = false,
                remainingSeconds = 0,
                endTimeEpochMillis = 0L
            )

            testRepository.submitTest(testEntity, gradedDetails)

            // Ghi nhận sự kiện Gamification (F10)
            val gamificationReward = gamificationRepository?.onLearningEvent(
                com.example.domain.gamification.LearningEvent.TestCompleted(
                    testId = state.testId,
                    score = summary.finalScore,
                    totalQuestions = summary.totalQuestions,
                    correctCount = summary.correctCount,
                    subjectId = state.subjectId
                )
            )

            // Cập nhật UserProgress (XP, số câu làm được)
            userProgressRepository.recordPracticeActivity(
                answeredCount = summary.totalQuestions - summary.unansweredCount,
                correctCount = summary.correctCount,
                studyMinutes = (timeSpentSeconds / 60).coerceAtLeast(1),
                xp = gamificationReward?.xpEarned ?: (summary.finalScore * 10).toInt().coerceAtLeast(10)
            )

            _uiState.value = TestUiState.Submitted(
                testId = state.testId,
                title = state.title,
                testType = state.testType,
                subjectName = state.subjectName,
                grade = state.grade,
                timeSpentSeconds = timeSpentSeconds,
                scoreSummary = summary,
                answerDetails = gradedDetails,
                earnedXp = gamificationReward?.xpEarned ?: 0,
                gamificationReward = gamificationReward
            )
        }
    }

    /**
     * Mở chế độ xem lại chi tiết bài làm
     */
    fun openReviewMode() {
        val submitted = _uiState.value as? TestUiState.Submitted ?: return
        _uiState.value = TestUiState.Reviewing(
            testId = submitted.testId,
            title = submitted.title,
            answerDetails = submitted.answerDetails,
            currentIndex = 0,
            filterOnlyWrong = false,
            scoreSummary = submitted.scoreSummary
        )
    }

    /**
     * Chuyển bộ lọc: "Tất cả" hoặc "Chỉ câu sai"
     */
    fun toggleReviewFilter(onlyWrong: Boolean) {
        val reviewing = _uiState.value as? TestUiState.Reviewing ?: return
        _uiState.value = reviewing.copy(
            filterOnlyWrong = onlyWrong,
            currentIndex = 0
        )
    }

    fun jumpToReviewQuestion(index: Int) {
        val reviewing = _uiState.value as? TestUiState.Reviewing ?: return
        _uiState.value = reviewing.copy(currentIndex = index)
    }

    /**
     * Tự đánh giá câu tự luận ngắn ở màn hình xem lại và cập nhật điểm tức thì
     */
    fun selfEvaluateShortAnswer(questionId: String, isCorrect: Boolean) {
        val reviewing = _uiState.value as? TestUiState.Reviewing ?: return
        viewModelScope.launch {
            val (updatedSummary, updatedDetails) = TestScorer.updateSelfEvaluation(
                details = reviewing.answerDetails,
                questionId = questionId,
                isCorrect = isCorrect
            )

            // Cập nhật trong database
            testRepository.updateSelfEvaluation(
                testId = reviewing.testId,
                questionId = questionId,
                newScore = if (isCorrect) 1.0f else 0.0f,
                isCorrect = isCorrect,
                newOverallTestScore = updatedSummary.finalScore,
                newCorrectCount = updatedSummary.correctCount
            )

            _uiState.value = reviewing.copy(
                answerDetails = updatedDetails,
                scoreSummary = updatedSummary
            )
        }
    }

    /**
     * Quay lại màn hình cấu hình ban đầu
     */
    fun resetToConfig() {
        timerJob?.cancel()
        _uiState.value = TestUiState.Configuring()
        viewModelScope.launch {
            checkForActiveDraft(selectedGrade.value)
            loadChaptersForSubject("math", selectedGrade.value)
        }
    }

    // --- Private Helper Functions ---

    private fun debounceSaveDraft(state: TestUiState.InProgress) {
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(1000)
            saveDraftInternal(state)
        }
    }

    private suspend fun saveDraftInternal(state: TestUiState.InProgress) {
        val answerDetails = state.questions.mapIndexed { index, question ->
            val ans = state.userAnswers[index]
            TestAnswerDetail(
                question = question,
                userAnswer = ans,
                isFlagged = state.flaggedIndices.contains(index),
                isAnswered = ans != null
            )
        }

        val testEntity = TestEntity(
            id = state.testId,
            title = state.title,
            type = state.testType.name,
            subjectId = state.subjectId,
            subjectName = state.subjectName,
            grade = state.grade,
            score = 0.0f,
            maxScore = 10.0f,
            totalQuestions = state.questions.size,
            correctCount = 0,
            timeSpentSeconds = state.totalDurationSeconds - state.remainingSeconds,
            durationMinutes = state.totalDurationSeconds / 60,
            timestamp = System.currentTimeMillis(),
            isDraft = true,
            remainingSeconds = state.remainingSeconds,
            endTimeEpochMillis = activeTimer?.endTimeMillis ?: (System.currentTimeMillis() + state.remainingSeconds * 1000L)
        )

        testRepository.saveDraft(testEntity, answerDetails)
    }

    private fun loadChaptersForSubject(subjectId: String, grade: Int) {
        viewModelScope.launch {
            curriculumRepository.getChapters(grade, subjectId).collect { chapterList ->
                val currentState = _uiState.value
                if (currentState is TestUiState.Configuring) {
                    _uiState.value = currentState.copy(chapters = chapterList)
                }
            }
        }
    }

    private fun loadLessonsForChapter(chapterId: String) {
        viewModelScope.launch {
            curriculumRepository.getLessons(chapterId).collect { lessonList ->
                val currentState = _uiState.value
                if (currentState is TestUiState.Configuring) {
                    _uiState.value = currentState.copy(lessons = lessonList)
                }
            }
        }
    }

    private suspend fun loadRawQuestionsForConfig(config: TestUiState.Configuring, grade: Int): List<com.example.data.local.entity.QuestionEntity> {
        return when (config.selectedType) {
            TestType.LESSON -> {
                if (config.selectedLessonId != "none") {
                    practiceRepository.getQuestionsForLesson(config.selectedLessonId)
                } else {
                    practiceRepository.getAllQuestionsBySubject(grade, config.selectedSubjectId)
                }
            }
            TestType.CHAPTER -> {
                if (config.selectedChapterId != "all") {
                    practiceRepository.getQuestionsByChapter(config.selectedChapterId)
                } else {
                    practiceRepository.getAllQuestionsBySubject(grade, config.selectedSubjectId)
                }
            }
            TestType.MID_TERM, TestType.FINAL -> {
                practiceRepository.getAllQuestionsBySubject(grade, config.selectedSubjectId)
            }
        }
    }

    private fun deserializeUserAnswer(question: Question, raw: String): UserAnswer {
        return when (question) {
            is Question.MultipleChoice -> UserAnswer.MultipleChoice(raw)
            is Question.TrueFalse -> UserAnswer.TrueFalse(raw.toBooleanStrictOrNull() ?: true)
            is Question.FillBlank -> UserAnswer.FillBlank(raw)
            is Question.Matching -> {
                // Đọc json cặp nối
                val pairs = mutableListOf<com.example.domain.model.MatchingPair>()
                try {
                    val array = org.json.JSONArray(raw)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        pairs.add(com.example.domain.model.MatchingPair(obj.getString("l"), obj.getString("r")))
                    }
                } catch (_: Exception) {}
                UserAnswer.Matching(pairs)
            }
            is Question.Ordering -> {
                val order = mutableListOf<String>()
                try {
                    val array = org.json.JSONArray(raw)
                    for (i in 0 until array.length()) {
                        order.add(array.getString(i))
                    }
                } catch (_: Exception) {}
                UserAnswer.Ordering(order)
            }
            is Question.ShortAnswer -> UserAnswer.ShortAnswer(raw)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        draftSaveJob?.cancel()
    }
}
