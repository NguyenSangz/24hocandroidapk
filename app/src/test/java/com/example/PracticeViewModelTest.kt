package com.example

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.CurriculumRepository
import com.example.data.repository.PracticeRepository
import com.example.data.repository.UserProgressRepository
import com.example.data.repository.WrongQuestionRepository
import com.example.domain.model.GradeStatus
import com.example.domain.model.Question
import com.example.domain.model.UserAnswer
import com.example.ui.viewmodel.PracticeSessionStatus
import com.example.ui.viewmodel.PracticeViewModel
import com.example.ui.viewmodel.QuestionStepState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit test toàn diện cho PracticeViewModel (F06):
 * - Vòng đời chuyển trạng thái: Idle -> Loading -> InProgress -> Graded -> Completed
 * - Ghi nhận sử dụng gợi ý (Hint)
 * - Tính toán kết quả phiên (số câu đúng/tổng, thời gian, bản ghi)
 * - Làm lại các câu sai (Retry wrong questions)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PracticeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var practiceRepository: PracticeRepository
    private lateinit var curriculumRepository: CurriculumRepository
    private lateinit var userProgressRepository: UserProgressRepository
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var wrongQuestionRepository: WrongQuestionRepository
    private lateinit var viewModel: PracticeViewModel

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)

        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        practiceRepository = PracticeRepository(database)
        curriculumRepository = CurriculumRepository(database)
        userProgressRepository = UserProgressRepository(database)
        userPreferencesManager = UserPreferencesManager(context)
        wrongQuestionRepository = WrongQuestionRepository(database)

        // Khởi tạo dữ liệu mẫu vào Room in-memory
        database.chapterDao().insertAll(
            listOf(
                ChapterEntity("c1", "math", 6, 1, "Chương 1", "Mô tả 1")
            )
        )
        database.lessonDao().insertAll(
            listOf(
                LessonEntity("l1", "c1", 1, "Bài 1", "Tóm tắt", "Ví dụ", "Mẹo")
            )
        )
        database.questionDao().insertAll(
            listOf(
                QuestionEntity(
                    id = "q1",
                    lessonId = "l1",
                    type = "MULTIPLE_CHOICE",
                    content = "Số nguyên tố chẵn duy nhất là?",
                    optionsJson = "[\"2\", \"4\", \"6\", \"8\"]",
                    correctAnswer = "2",
                    hint = "Số tự nhiên nhỏ nhất lớn hơn 1.",
                    explanation = "2 là số nguyên tố chẵn duy nhất.",
                    difficulty = 1
                ),
                QuestionEntity(
                    id = "q2",
                    lessonId = "l1",
                    type = "TRUE_FALSE",
                    content = "Số 0 là số tự nhiên.",
                    optionsJson = "[\"Đúng\", \"Sai\"]",
                    correctAnswer = "Đúng",
                    hint = "Tập N chứa số 0.",
                    explanation = "0 thuộc N.",
                    difficulty = 1
                ),
                QuestionEntity(
                    id = "q3",
                    lessonId = "l1",
                    type = "FILL_BLANK",
                    content = "Tính 5 + 7 = ___",
                    optionsJson = "[]",
                    correctAnswer = "12",
                    hint = "Cộng hai số.",
                    explanation = "5 + 7 = 12.",
                    difficulty = 1
                )
            )
        )

        viewModel = PracticeViewModel(
            practiceRepository = practiceRepository,
            curriculumRepository = curriculumRepository,
            userProgressRepository = userProgressRepository,
            userPreferencesManager = userPreferencesManager,
            wrongQuestionRepository = wrongQuestionRepository,
            savedStateHandle = SavedStateHandle()
        )
        advanceUntilIdle()
    }

    @After
    fun tearDown() {
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        Dispatchers.resetMain()
        database.close()
    }

    private fun answerCurrentQuestion(isCorrect: Boolean) {
        val q = viewModel.uiState.value.currentQuestion ?: return
        if (isCorrect) {
            when (q) {
                is Question.MultipleChoice -> viewModel.selectAnswer(UserAnswer.MultipleChoice(q.correctAnswer))
                is Question.TrueFalse -> viewModel.selectAnswer(UserAnswer.TrueFalse(q.correctAnswer))
                is Question.FillBlank -> viewModel.selectAnswer(UserAnswer.FillBlank(q.acceptedAnswers.firstOrNull() ?: "12"))
                else -> {}
            }
        } else {
            when (q) {
                is Question.MultipleChoice -> viewModel.selectAnswer(UserAnswer.MultipleChoice("SAI_HOAN_TOAN"))
                is Question.TrueFalse -> viewModel.selectAnswer(UserAnswer.TrueFalse(!q.correctAnswer))
                is Question.FillBlank -> viewModel.selectAnswer(UserAnswer.FillBlank("SAI"))
                else -> {}
            }
        }
    }

    @Test
    fun testSessionStartAndStateTransitions() = runTest(testDispatcher) {
        // 1. Khởi động phiên luyện tập
        viewModel.startPracticeSession(lessonId = "l1", count = 3)
        var state = viewModel.uiState.first { it.status is PracticeSessionStatus.InProgress }

        assertEquals(PracticeSessionStatus.InProgress, state.status)
        assertEquals(3, state.questions.size)
        assertEquals(0, state.currentIndex)
        assertTrue(state.stepState is QuestionStepState.Answering)

        // 2. Chọn câu trả lời đúng và nộp
        val q1 = state.currentQuestion
        assertNotNull(q1)
        answerCurrentQuestion(isCorrect = true)

        viewModel.submitCurrentAnswer()
        state = viewModel.uiState.first { it.stepState is QuestionStepState.Graded }

        // 3. Kiểm tra trạng thái đã chấm (Graded)
        assertTrue(state.stepState is QuestionStepState.Graded)
        val gradedResult = (state.stepState as QuestionStepState.Graded).result
        assertTrue(gradedResult.isCorrect)
        assertEquals(GradeStatus.CORRECT, gradedResult.status)
        assertEquals(1, state.questionRecords.size)
        assertEquals(1, state.correctCount)

        // 4. Kiểm tra dữ liệu được lưu vào bảng attempts
        val attempts = database.attemptDao().getAllAttempts().first { it.isNotEmpty() }
        assertEquals(1, attempts.size)
        assertEquals(q1?.id, attempts[0].questionId)
        assertTrue(attempts[0].isCorrect)
    }

    @Test
    fun testHintUsageTracking() = runTest(testDispatcher) {
        viewModel.startPracticeSession(lessonId = "l1", count = 2)
        var state = viewModel.uiState.first { it.status is PracticeSessionStatus.InProgress }
        assertFalse(state.showHintDialog)
        assertFalse(state.usedHintInCurrentQuestion)

        // Bấm mở gợi ý
        viewModel.toggleHint()

        state = viewModel.uiState.value
        assertTrue(state.showHintDialog)
        assertTrue(state.usedHintInCurrentQuestion)

        // Nộp câu trả lời sau khi dùng gợi ý
        answerCurrentQuestion(isCorrect = true)
        viewModel.submitCurrentAnswer()
        state = viewModel.uiState.first { it.stepState is QuestionStepState.Graded }

        val record = state.questionRecords.first()
        assertTrue(record.usedHint)

        // Kiểm tra lưu DB có ghi nhận usedHint
        val attempts = database.attemptDao().getAllAttempts().first { it.isNotEmpty() }
        assertTrue(attempts.first().usedHint)
    }

    @Test
    fun testSessionCompletionAndScoreCalculation() = runTest(testDispatcher) {
        viewModel.startPracticeSession(lessonId = "l1", count = 2)
        viewModel.uiState.first { it.status is PracticeSessionStatus.InProgress }

        // Câu 1: Làm đúng
        answerCurrentQuestion(isCorrect = true)
        viewModel.submitCurrentAnswer()
        viewModel.uiState.first { it.stepState is QuestionStepState.Graded }
        viewModel.nextQuestion()

        var state = viewModel.uiState.value
        assertEquals(1, state.currentIndex)
        assertTrue(state.stepState is QuestionStepState.Answering)

        // Câu 2: Làm sai
        answerCurrentQuestion(isCorrect = false)
        viewModel.submitCurrentAnswer()
        viewModel.uiState.first { it.stepState is QuestionStepState.Graded }
        viewModel.nextQuestion()

        // Hoàn thành phiên
        state = viewModel.uiState.first { it.status is PracticeSessionStatus.Completed }
        assertEquals(PracticeSessionStatus.Completed, state.status)
        assertEquals(2, state.questions.size)
        assertEquals(1, state.correctCount)
        assertEquals(2, state.questionRecords.size)

        // Kiểm tra câu sai được ghi vào WrongQuestionDao
        val wrongQuestions = database.wrongQuestionDao().getAllWrongQuestions().first { it.isNotEmpty() }
        assertEquals(1, wrongQuestions.size)
    }

    @Test
    fun testRetryWrongQuestions() = runTest(testDispatcher) {
        viewModel.startPracticeSession(lessonId = "l1", count = 2)
        viewModel.uiState.first { it.status is PracticeSessionStatus.InProgress }

        // Câu 1: Đúng
        answerCurrentQuestion(isCorrect = true)
        viewModel.submitCurrentAnswer()
        viewModel.uiState.first { it.stepState is QuestionStepState.Graded }
        viewModel.nextQuestion()

        // Câu 2: Sai
        answerCurrentQuestion(isCorrect = false)
        viewModel.submitCurrentAnswer()
        viewModel.uiState.first { it.stepState is QuestionStepState.Graded }
        viewModel.nextQuestion()

        var state = viewModel.uiState.first { it.status is PracticeSessionStatus.Completed }
        assertEquals(PracticeSessionStatus.Completed, state.status)

        // Bấm "Làm lại các câu sai"
        viewModel.retryWrongQuestions()

        state = viewModel.uiState.first { it.status is PracticeSessionStatus.InProgress }
        assertEquals(1, state.questions.size) // Chỉ còn 1 câu sai
        assertEquals(0, state.currentIndex)
        assertEquals(0, state.correctCount)
        assertTrue(state.stepState is QuestionStepState.Answering)
    }
}
