package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AttemptEntity
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TestAnswerEntity
import com.example.data.local.entity.TestEntity
import com.example.data.local.entity.WrongQuestionEntity
import com.example.data.repository.WrongQuestionRepository
import com.example.domain.model.WrongQuestionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit Test cho WrongQuestionRepository (F08).
 * Kiểm tra:
 * 1. Không trùng lặp câu hỏi (PrimaryKey questionId duy nhất, cập nhật errorCount).
 * 2. Gom câu sai từ bảng AttemptEntity và TestAnswerEntity khi đồng bộ (syncExistingMistakes).
 * 3. Xử lý bản ghi mồ côi: bỏ qua và dọn dẹp khi câu hỏi gốc bị xóa/đổi ID, không crash.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WrongQuestionRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: WrongQuestionRepository

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = WrongQuestionRepository(database)

        // Khởi tạo dữ liệu nền mẫu
        database.subjectDao().insertAll(
            listOf(SubjectEntity("math", "Toán học", "calc", "#2563EB", "Toán"))
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
                    content = "2 + 2 = ?",
                    optionsJson = "[\"3\", \"4\", \"5\", \"6\"]",
                    correctAnswer = "4",
                    hint = "Phép cộng cơ bản",
                    explanation = "2 + 2 = 4"
                ),
                QuestionEntity(
                    id = "q_2",
                    lessonId = "les_1",
                    type = "TRUE_FALSE",
                    content = "10 là số chẵn?",
                    optionsJson = "[\"Đúng\", \"Sai\"]",
                    correctAnswer = "true",
                    hint = "Số chia hết cho 2",
                    explanation = "10 chia hết cho 2 nên là số chẵn"
                )
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testRecordWrongAnswerNoDuplicates() = runTest {
        // Ghi nhận câu sai lần 1
        repository.recordWrongAnswer("q_1", "les_1", "3")

        val list1 = repository.allWrongQuestions.first()
        assertEquals(1, list1.size)
        assertEquals("q_1", list1[0].questionId)
        assertEquals(1, list1[0].errorCount)
        assertEquals("3", list1[0].lastWrongAnswer)
        assertEquals("math", list1[0].subjectId)
        assertEquals("ch_1", list1[0].chapterId)
        assertEquals(6, list1[0].grade)

        // Ghi nhận câu sai lần 2 cùng questionId
        repository.recordWrongAnswer("q_1", "les_1", "5")

        val list2 = repository.allWrongQuestions.first()
        assertEquals("Không được tạo bản ghi trùng lặp", 1, list2.size)
        assertEquals(2, list2[0].errorCount)
        assertEquals("5", list2[0].lastWrongAnswer)
    }

    @Test
    fun testSyncExistingMistakesFromAttemptsAndTestAnswers() = runTest {
        // Thêm bản ghi attempt sai cho q_1
        database.attemptDao().insertAttempt(
            AttemptEntity(
                questionId = "q_1",
                isCorrect = false,
                score = 0.0f,
                userAnswer = "3"
            )
        )

        // Thêm bản ghi test_answers sai cho q_2
        database.testDao().insertTest(
            TestEntity(
                id = "test_1",
                title = "Kiểm tra 15 phút",
                type = "LESSON",
                subjectId = "math",
                subjectName = "Toán",
                grade = 6,
                score = 5.0f,
                totalQuestions = 2,
                correctCount = 1,
                timeSpentSeconds = 600,
                durationMinutes = 15,
                isDraft = false
            )
        )
        database.testDao().insertAnswers(
            listOf(
                TestAnswerEntity(
                    testId = "test_1",
                    questionId = "q_2",
                    questionIndex = 0,
                    userAnswerJson = "Sai",
                    isCorrect = false,
                    score = 0.0f
                )
            )
        )

        // Thực hiện đồng bộ
        repository.syncExistingMistakes()

        val allWrong = repository.allWrongQuestions.first()
        assertEquals(2, allWrong.size)

        val q1Wrong = allWrong.find { it.questionId == "q_1" }
        assertNotNull(q1Wrong)
        assertEquals("3", q1Wrong?.lastWrongAnswer)

        val q2Wrong = allWrong.find { it.questionId == "q_2" }
        assertNotNull(q2Wrong)
        assertEquals("Sai", q2Wrong?.lastWrongAnswer)
    }

    @Test
    fun testOrphanRecordsOmittedAndCleanedUpSafely() = runTest {
        // Chèn trực tiếp một bản ghi câu sai mồ côi (questionId "q_ghost" không có trong questions)
        database.wrongQuestionDao().insertOrUpdate(
            WrongQuestionEntity(
                questionId = "q_ghost",
                grade = 6,
                subjectId = "math",
                chapterId = "ch_1",
                lessonId = "les_1",
                errorCount = 1,
                status = WrongQuestionStatus.NEEDS_REVIEW.code
            )
        )

        // getWrongQuestionDetails phải tự dọn dẹp và không crash
        val details = repository.getWrongQuestionDetails(6)
        assertTrue(details.none { it.entity.questionId == "q_ghost" })

        // Bản ghi mồ côi đã được xóa khỏi DB
        val ghost = database.wrongQuestionDao().getByQuestionId("q_ghost")
        assertNull(ghost)
    }

    @Test
    fun testRecordCorrectAnswerAcrossDays() = runTest {
        repository.recordWrongAnswer("q_1", "les_1", "3")

        val initial = database.wrongQuestionDao().getByQuestionId("q_1")
        assertNotNull(initial)
        assertEquals(0, initial?.consecutiveCorrectCount)

        // Trả lời đúng lần 1 (sau 1 ngày)
        val schedulerWithOffset = com.example.domain.evaluator.ReviewScheduler(
            clock = { System.currentTimeMillis() + (25 * 60 * 60 * 1000L) }
        )
        val repoWithScheduler = WrongQuestionRepository(database, schedulerWithOffset)

        val updated = repoWithScheduler.recordCorrectAnswer("q_1")
        assertNotNull(updated)
        assertEquals(1, updated?.consecutiveCorrectCount)
        assertEquals(WrongQuestionStatus.CONSOLIDATING.code, updated?.status)
    }
}
