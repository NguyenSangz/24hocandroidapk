package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.seed.CurriculumDataSeeder
import kotlinx.coroutines.runBlocking
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
 * Unit test cho CurriculumDataSeeder (F17a - pipeline gói nội dung + content version):
 * 1. Nhập gói mới -> chương/bài/câu hỏi xuất hiện trong Room, phiên bản được ghi nhận.
 * 2. Nhập lại cùng phiên bản -> bỏ qua (trả 0), không nhân đôi dữ liệu.
 * 3. Nhập phiên bản mới hơn -> nội dung được cập nhật, tiến độ bài học (isCompleted)
 *    và trạng thái SM-2 của flashcard được bảo toàn.
 * 4. JSON lỗi -> Result.failure, không crash, không ghi nhận phiên bản.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CurriculumSeederPackageTest {

    private lateinit var database: AppDatabase
    private lateinit var seeder: CurriculumDataSeeder

    private val packageV1 = """
        {
          "packageId": "test_pkg",
          "contentVersion": 1,
          "subjects": [],
          "grades": [
            {
              "grade": 6,
              "chapters": [
                {
                  "id": "test_c1",
                  "subjectId": "gdcd",
                  "grade": 6,
                  "order": 9,
                  "title": "Chương thử nghiệm",
                  "description": "Mô tả",
                  "lessons": [
                    {
                      "id": "test_c1_l1",
                      "chapterId": "test_c1",
                      "order": 1,
                      "title": "Bài thử nghiệm",
                      "summary": "Tóm tắt v1",
                      "examples": "Ví dụ",
                      "tips": "Mẹo"
                    }
                  ]
                }
              ]
            }
          ],
          "questions": [
            {
              "id": "q_test_1",
              "lessonId": "test_c1_l1",
              "type": "MULTIPLE_CHOICE",
              "content": "Câu hỏi v1?",
              "options": ["A", "B", "C", "D"],
              "correctAnswer": "A",
              "hint": "Gợi ý",
              "explanation": "Giải thích v1",
              "difficulty": 1
            }
          ],
          "flashcards": [
            {
              "id": "fc_test_1",
              "grade": 6,
              "subjectId": "gdcd",
              "front": "Thẻ v1",
              "back": "Đáp án v1"
            }
          ]
        }
    """.trimIndent()

    private val packageV2 = packageV1
        .replace("\"contentVersion\": 1", "\"contentVersion\": 2")
        .replace("Tóm tắt v1", "Tóm tắt v2")
        .replace("Câu hỏi v1?", "Câu hỏi v2?")
        .replace("Giải thích v1", "Giải thích v2")

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        seeder = CurriculumDataSeeder(context, database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun importNewPackage_insertsContentAndRecordsVersion() = runBlocking {
        val result = seeder.importPackage(packageV1, "test_pkg")
        assertTrue("Nhập gói phải thành công: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals(4, result.getOrNull()) // 1 chương + 1 bài + 1 câu hỏi + 1 thẻ + 0 môn

        val chapters = database.chapterDao().getAllChaptersSync()
        assertTrue(chapters.any { it.id == "test_c1" && it.subjectId == "gdcd" && it.grade == 6 })

        val lessons = database.lessonDao().getAllLessonsSync()
        assertEquals("Tóm tắt v1", lessons.first { it.id == "test_c1_l1" }.summary)

        val questions = database.questionDao().getAllQuestionsSync()
        assertTrue(questions.any { it.id == "q_test_1" && it.content == "Câu hỏi v1?" })

        val cards = database.flashcardDao().getAllFlashcardsSync()
        assertTrue(cards.any { it.id == "fc_test_1" })

        val version = database.contentVersionDao().getVersion("test_pkg")
        assertEquals(1, version?.version)
    }

    @Test
    fun reimportSameVersion_isSkipped_withoutDuplicates() = runBlocking {
        seeder.importPackage(packageV1, "test_pkg")
        val second = seeder.importPackage(packageV1, "test_pkg")

        assertTrue(second.isSuccess)
        assertEquals(0, second.getOrNull()) // bỏ qua vì đã nhập v1

        // Không nhân đôi dữ liệu
        assertEquals(1, database.chapterDao().getAllChaptersSync().count { it.id == "test_c1" })
        assertEquals(1, database.lessonDao().getAllLessonsSync().count { it.id == "test_c1_l1" })
        assertEquals(1, database.questionDao().getAllQuestionsSync().count { it.id == "q_test_1" })
    }

    @Test
    fun reimportNewerVersion_updatesContent_andPreservesUserState() = runBlocking {
        // Nhập v1
        seeder.importPackage(packageV1, "test_pkg")

        // Người dùng học xong bài và ôn flashcard lên Box 3
        database.lessonDao().updateLessonCompleted("test_c1_l1", true)
        database.flashcardDao().updateCardReview("fc_test_1", 3, 7, 123456789L, 2.8f, 3)

        // Cập nhật gói lên v2
        val result = seeder.importPackage(packageV2, "test_pkg")
        assertTrue("Nhập v2 phải thành công: ${result.exceptionOrNull()}", result.isSuccess)
        assertTrue("v2 phải được nhập lại (số phần tử > 0)", (result.getOrNull() ?: 0) > 0)

        // Nội dung mới
        val lesson = database.lessonDao().getLessonById("test_c1_l1")
        assertEquals("Tóm tắt v2", lesson?.summary)
        val question = database.questionDao().getQuestionById("q_test_1")
        assertEquals("Câu hỏi v2?", question?.content)

        // Tiến độ bài học được bảo toàn sau REPLACE
        assertEquals(true, lesson?.isCompleted)

        // Trạng thái SM-2 của thẻ được bảo toàn sau REPLACE
        val card = database.flashcardDao().getAllFlashcardsSync().first { it.id == "fc_test_1" }
        assertEquals(3, card.box)
        assertEquals(7, card.intervalDays)
        assertEquals(2.8f, card.easeFactor, 0.001f)
        assertEquals(3, card.repetitions)
        assertEquals(123456789L, card.nextReviewTimestamp)

        // Phiên bản ghi nhận là v2
        assertEquals(2, database.contentVersionDao().getVersion("test_pkg")?.version)
    }

    @Test
    fun invalidJson_returnsFailure_andDoesNotRecordVersion() = runBlocking {
        val result = seeder.importPackage("{ không phải JSON }", "broken_pkg")
        assertTrue(result.isFailure)
        assertFalse(database.contentVersionDao().getAllVersionsSync().any { it.packageId == "broken_pkg" })
    }

    @Test
    fun emptyPackage_isNotRecorded() = runBlocking {
        val empty = """{"packageId": "empty_pkg", "contentVersion": 1, "subjects": [], "grades": [], "questions": [], "flashcards": []}"""
        val result = seeder.importPackage(empty, "empty_pkg")
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
        assertFalse(database.contentVersionDao().getAllVersionsSync().any { it.packageId == "empty_pkg" })
    }
}
