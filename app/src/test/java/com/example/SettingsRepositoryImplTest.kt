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
import com.example.data.local.entity.WrongQuestionEntity
import com.example.data.local.entity.XpEventEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit Test cho SettingsRepositoryImpl với Room DB thực tế và DataStore (F15a).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var preferencesManager: UserPreferencesManager
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferencesManager = UserPreferencesManager(context)
        repository = SettingsRepositoryImpl(preferencesManager, database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testSettingsReadAndWritePersistent() = runTest {
        repository.setThemeMode("dark")
        assertEquals("dark", repository.themeMode.first())
        assertTrue(repository.isDarkMode.first())

        repository.setReduceMotion(true)
        assertTrue(repository.reduceMotion.first())

        repository.setLessonFontSize("small")
        assertEquals("small", repository.lessonFontSize.first())

        repository.setDefaultSeries("cd")
        assertEquals("cd", repository.defaultSeries.first())

        repository.setDefaultQuestionsPerSession(15)
        assertEquals(15, repository.defaultQuestionsPerSession.first())

        repository.setTtsSpeechRate(0.85f)
        assertEquals(0.85f, repository.ttsSpeechRate.first(), 0.01f)

        repository.setReminder(true, 21, 0)
        assertTrue(repository.reminderEnabled.first())
        assertEquals(21, repository.reminderHour.first())
        assertEquals(0, repository.reminderMinute.first())
    }

    @Test
    fun testChangeGradePreservesDataAcrossGrades() = runTest {
        // Học sinh lớp 6 làm bài
        database.subjectDao().insertAll(listOf(SubjectEntity("math", "Toán", "calc", "#2563EB", "Toán")))
        database.chapterDao().insertAll(listOf(ChapterEntity("ch_6", "math", 6, 1, "Chương 1", "Mô tả")))
        database.lessonDao().insertAll(listOf(LessonEntity("les_6", "ch_6", 1, "Bài 1", "Tóm tắt", "Ví dụ", "Mẹo")))
        database.questionDao().insertAll(listOf(
            QuestionEntity("q_6", "les_6", "MULTIPLE_CHOICE", "1+1=?", "[\"2\"]", "2", "Gợi ý", "Giải thích")
        ))
        database.attemptDao().insertAttempt(
            AttemptEntity(
                questionId = "q_6",
                userAnswer = "2",
                isCorrect = true,
                score = 10.0f,
                timeSpentSeconds = 12,
                timestamp = System.currentTimeMillis()
            )
        )

        repository.setGrade(6)
        assertEquals(6, repository.selectedGrade.first())
        assertEquals(1, database.attemptDao().getAllAttempts().first().size)

        // Chuyển sang lớp 7
        repository.setGrade(7)
        assertEquals(7, repository.selectedGrade.first())

        // Dữ liệu bài làm của lớp 6 vẫn còn nguyên vẹn 100%
        val attempts = database.attemptDao().getAllAttempts().first()
        assertEquals(1, attempts.size)
        assertEquals("q_6", attempts[0].questionId)
        assertTrue(attempts[0].isCorrect)
    }

    @Test
    fun testResetSettingsToDefaultLeavesDatabaseUntouched() = runTest {
        database.attemptDao().insertAttempt(
            AttemptEntity(
                questionId = "q_test",
                userAnswer = "A",
                isCorrect = true,
                score = 10.0f,
                timeSpentSeconds = 10,
                timestamp = System.currentTimeMillis()
            )
        )

        repository.setThemeMode("dark")
        repository.setLessonFontSize("large")
        repository.setReduceMotion(true)

        repository.resetSettingsToDefault()

        // Cài đặt trở về mặc định
        assertEquals("system", repository.themeMode.first())
        assertEquals("medium", repository.lessonFontSize.first())
        assertFalse(repository.reduceMotion.first())

        // Database không bị xóa
        assertEquals(1, database.attemptDao().getAllAttempts().first().size)
    }

    @Test
    fun testClearAllStudyDataClearsUserTablesAndKeepsCurriculum() = runTest {
        // Nạp dữ liệu giáo trình gốc
        database.subjectDao().insertAll(listOf(SubjectEntity("math", "Toán", "calc", "#2563EB", "Toán")))
        database.chapterDao().insertAll(listOf(ChapterEntity("ch_1", "math", 6, 1, "Chương 1", "Mô tả")))
        database.lessonDao().insertAll(listOf(LessonEntity("les_1", "ch_1", 1, "Bài 1", "Tóm tắt", "Ví dụ", "Mẹo")))
        database.questionDao().insertAll(listOf(
            QuestionEntity("q_1", "les_1", "MULTIPLE_CHOICE", "2+3=?", "[\"5\"]", "5", "Gợi ý", "Giải thích")
        ))

        // Nạp dữ liệu cá nhân của học sinh
        database.attemptDao().insertAttempt(
            AttemptEntity(
                questionId = "q_1",
                userAnswer = "5",
                isCorrect = true,
                score = 10.0f,
                timeSpentSeconds = 8,
                timestamp = System.currentTimeMillis()
            )
        )
        database.wrongQuestionDao().insertOrUpdate(
            WrongQuestionEntity(
                questionId = "q_1",
                grade = 6,
                subjectId = "math",
                chapterId = "ch_1",
                lessonId = "les_1",
                questionType = "MULTIPLE_CHOICE",
                errorCount = 1,
                lastWrongAnswer = "4"
            )
        )
        database.gamificationDao().insertXpEvent(
            XpEventEntity(
                eventType = "LESSON_COMPLETED",
                eventKey = "lesson_1",
                xpAmount = 10,
                timestamp = System.currentTimeMillis(),
                metadata = "Hoàn thành bài 1"
            )
        )

        // Xác nhận dữ liệu cá nhân đã có trước khi xóa
        assertEquals(1, database.attemptDao().getAllAttempts().first().size)
        assertEquals(1, database.wrongQuestionDao().getAllWrongQuestions().first().size)
        assertEquals(10, database.gamificationDao().getTotalXpSync())

        // Thực hiện xóa toàn bộ dữ liệu học tập cá nhân
        repository.clearAllStudyData()

        // 1. Dữ liệu cá nhân bị xóa sạch
        assertEquals(0, database.attemptDao().getAllAttempts().first().size)
        assertEquals(0, database.wrongQuestionDao().getAllWrongQuestions().first().size)
        assertEquals(0, database.gamificationDao().getTotalXpSync())

        // 2. Giáo trình bài học gốc VẪN CÒN NGUYÊN VẸN
        assertEquals(1, database.subjectDao().getAllSubjects().first().size)
        assertEquals(1, database.chapterDao().getChaptersByGradeAndSubject(6, "math").first().size)
        assertEquals(1, database.lessonDao().getLessonsByChapter("ch_1").first().size)
        assertEquals(1, database.questionDao().getQuestionsByLesson("les_1").size)
    }
}
