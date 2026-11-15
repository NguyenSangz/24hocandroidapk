package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.WrongQuestionEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.PracticeRepository
import com.example.data.repository.UserProgressRepository
import com.example.data.repository.WrongQuestionRepository
import com.example.domain.model.Question
import com.example.domain.model.UserAnswer
import com.example.domain.model.WrongQuestionStatus
import com.example.ui.viewmodel.QuestionStepState
import com.example.ui.viewmodel.ReviewSessionStatus
import com.example.ui.viewmodel.ReviewSessionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit Test cho ReviewSessionViewModel (F08 - Phiên ôn câu sai).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReviewSessionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var wrongQuestionRepository: WrongQuestionRepository
    private lateinit var practiceRepository: PracticeRepository
    private lateinit var userProgressRepository: UserProgressRepository
    private lateinit var userPreferencesManager: UserPreferencesManager
    private lateinit var viewModel: ReviewSessionViewModel

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        wrongQuestionRepository = WrongQuestionRepository(database)
        practiceRepository = PracticeRepository(database)
        userProgressRepository = UserProgressRepository(database)
        userPreferencesManager = UserPreferencesManager(context)

        // Seed dữ liệu nền
        database.subjectDao().insertAll(
            listOf(SubjectEntity("math", "Toán", "calc", "#2563EB", "Toán"))
        )
        database.chapterDao().insertAll(
            listOf(ChapterEntity("ch_1", "math", 6, 1, "Chương 1", "Mô tả"))
        )
        database.lessonDao().insertAll(
            listOf(LessonEntity("les_1", "ch_1", 1, "Bài 1", "Tóm tắt", "Ví dụ", "Mẹo"))
        )
        database.questionDao().insertAll(
            listOf(
                QuestionEntity(
                    id = "q_1",
                    lessonId = "les_1",
                    type = "MULTIPLE_CHOICE",
                    content = "3 + 5 = ?",
                    optionsJson = "[\"7\", \"8\", \"9\", \"10\"]",
                    correctAnswer = "8",
                    hint = "Cộng hai số tự nhiên",
                    explanation = "3 + 5 = 8"
                ),
                QuestionEntity(
                    id = "q_2",
                    lessonId = "les_1",
                    type = "TRUE_FALSE",
                    content = "Số 0 là số nguyên?",
                    optionsJson = "[\"Đúng\", \"Sai\"]",
                    correctAnswer = "true",
                    hint = "Tập hợp Z",
                    explanation = "Số 0 thuộc tập hợp số nguyên"
                )
            )
        )

        // Đưa 2 câu vào ngân hàng câu sai
        database.wrongQuestionDao().insertAll(
            listOf(
                WrongQuestionEntity(
                    questionId = "q_1",
                    grade = 6,
                    subjectId = "math",
                    chapterId = "ch_1",
                    lessonId = "les_1",
                    errorCount = 1,
                    status = WrongQuestionStatus.NEEDS_REVIEW.code
                ),
                WrongQuestionEntity(
                    questionId = "q_2",
                    grade = 6,
                    subjectId = "math",
                    chapterId = "ch_1",
                    lessonId = "les_1",
                    errorCount = 1,
                    status = WrongQuestionStatus.NEEDS_REVIEW.code
                )
            )
        )

        viewModel = ReviewSessionViewModel(
            wrongQuestionRepository = wrongQuestionRepository,
            practiceRepository = practiceRepository,
            userProgressRepository = userProgressRepository,
            userPreferencesManager = userPreferencesManager
        )
    }

    @After
    fun tearDown() {
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun testStartReviewSessionLoadsQuestionsAndShufflesOptions() = runTest(testDispatcher) {
        viewModel.startReviewSession(grade = 6, count = 5)

        val state = viewModel.uiState.first { it.status is ReviewSessionStatus.InProgress }
        assertEquals(ReviewSessionStatus.InProgress, state.status)
        assertEquals(2, state.totalCount)
        assertNotNull(state.currentQuestion)

        // Kiểm tra câu trắc nghiệm q_1 có đủ 4 phương án
        val q1 = state.questions.find { it.id == "q_1" } as? Question.MultipleChoice
        assertNotNull(q1)
        assertEquals(4, q1?.options?.size)
        assertTrue(q1?.options?.contains("8") == true)
    }

    @Test
    fun testAnswerQuestionAndProgressCycle() = runTest(testDispatcher) {
        viewModel.startReviewSession(grade = 6, count = 5)
        viewModel.uiState.first { it.status is ReviewSessionStatus.InProgress }

        // Chọn đáp án đúng cho câu 1 ("8")
        viewModel.selectAnswer(UserAnswer.MultipleChoice("8"))
        val stateWithAnswer = viewModel.uiState.value
        assertEquals("8", (stateWithAnswer.currentUserAnswer as? UserAnswer.MultipleChoice)?.selectedOption)

        // Nộp bài
        viewModel.submitCurrentAnswer()

        val gradedState = viewModel.uiState.first { it.stepState is QuestionStepState.Graded }
        assertTrue(gradedState.stepState is QuestionStepState.Graded)
        val result = (gradedState.stepState as QuestionStepState.Graded).result
        assertTrue(result.isCorrect)
        assertEquals(1, gradedState.questionRecords.size)

        // Chuyển sang câu tiếp theo
        viewModel.nextQuestion()
        val stateNext = viewModel.uiState.first { it.currentIndex == 1 }
        assertEquals(1, stateNext.currentIndex)
        assertEquals(QuestionStepState.Answering, stateNext.stepState)

        // Câu 2: Trả lời sai ("false")
        viewModel.selectAnswer(UserAnswer.TrueFalse(false))
        viewModel.submitCurrentAnswer()
        viewModel.uiState.first { it.stepState is QuestionStepState.Graded }

        // Chuyển tiếp câu -> Hoàn thành phiên
        viewModel.nextQuestion()
        testScheduler.advanceUntilIdle()
        val completedState = viewModel.uiState.first { it.status is ReviewSessionStatus.Completed }
        assertEquals(ReviewSessionStatus.Completed, completedState.status)
        assertEquals(1, completedState.correctCount)
        assertEquals(1, completedState.stillNeedsReviewCount)
    }
}
