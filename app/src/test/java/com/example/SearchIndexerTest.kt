package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.FlashcardEntity
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.search.SearchIndexer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchIndexerTest {

    private lateinit var database: AppDatabase
    private lateinit var indexer: SearchIndexer

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        indexer = SearchIndexer(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testCreateLessonFts_normalizesAndStripsLatex() {
        val chapter = ChapterEntity(
            id = "c1",
            subjectId = "toan",
            grade = 6,
            order = 1,
            title = "Chương I: Số tự nhiên",
            description = "Mô tả chương",
            series = "kntt"
        )
        val lesson = LessonEntity(
            id = "l1",
            chapterId = "c1",
            order = 1,
            title = "Bài 1: Tập hợp",
            summary = "Khái niệm tập hợp $\\in$ và $\\notin$",
            examples = "Ví dụ \$A = \\{1, 2\\}\$",
            tips = "Mẹo nhớ các phần tử",
            series = "kntt",
            topicId = "tap-hop"
        )

        val fts = indexer.createLessonFts(lesson, chapter)

        assertEquals("l1", fts.targetId)
        assertEquals("LESSON", fts.type)
        assertEquals(6, fts.grade)
        assertEquals("kntt", fts.series)
        assertEquals("toan", fts.subjectId)
        assertEquals("tap-hop", fts.topicId)

        // Tiêu đề có dấu và không dấu
        assertTrue(fts.title.contains("tập hợp", ignoreCase = true))
        assertTrue(fts.titleNoAccent.contains("tap hop", ignoreCase = true))

        // Nội dung đã loại bỏ ký hiệu LaTeX $
        assertFalse(fts.content.contains("$"))
    }

    @Test
    fun testCreateQuestionFts_extractsExplanationAndHint() {
        val question = QuestionEntity(
            id = "q1",
            lessonId = "l1",
            type = "MULTIPLE_CHOICE",
            content = "Số nguyên tố là gì?",
            optionsJson = "[\"A\", \"B\"]",
            correctAnswer = "A",
            hint = "Số chỉ chia hết cho 1 và chính nó",
            explanation = "Định nghĩa số nguyên tố lớp 6",
            series = "cd",
            topicId = "so-nguyen-to"
        )

        val fts = indexer.createQuestionFts(question, null, null)

        assertEquals("q1", fts.targetId)
        assertEquals("QUESTION", fts.type)
        assertEquals("cd", fts.series)
        assertEquals("so-nguyen-to", fts.topicId)
        assertTrue(fts.content.contains("chỉ chia hết", ignoreCase = true))
        assertTrue(fts.contentNoAccent.contains("chi chia het", ignoreCase = true))
    }

    @Test
    fun testCreateFlashcardFts_indexesWordAndMeaning() {
        val card = FlashcardEntity(
            id = "fc1",
            grade = 6,
            subjectId = "tienganh",
            front = "school",
            back = "trường học",
            word = "school",
            meaning = "trường học",
            example = "I go to school",
            category = "Vocabulary",
            series = "kntt",
            topicId = "unit-1-vocabulary"
        )

        val fts = indexer.createFlashcardFts(card)

        assertEquals("fc1", fts.targetId)
        assertEquals("FLASHCARD", fts.type)
        assertEquals("tienganh", fts.subjectId)
        assertTrue(fts.title.contains("school"))
        assertTrue(fts.content.contains("trường học"))
        assertTrue(fts.contentNoAccent.contains("truong hoc"))
    }
}
