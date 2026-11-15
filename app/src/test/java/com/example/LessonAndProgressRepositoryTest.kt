package com.example

import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.SubjectEntity
import com.example.domain.model.LessonFontSize
import com.example.fake.FakeLessonRepository
import com.example.fake.FakeProgressRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests cho LessonRepository, ProgressRepository, logic tính tiến độ và chọn bài kế tiếp (F04)
 */
class LessonAndProgressRepositoryTest {

    private lateinit var lessonRepository: FakeLessonRepository
    private lateinit var progressRepository: FakeProgressRepository

    private val mathSubject = SubjectEntity("math", "Toán", "calculate", "#2563EB", "Toán học THCS")
    private val chapter1 = ChapterEntity("g6_math_c1", "math", 6, 1, "Chương 1: Số tự nhiên", "Mô tả 1")
    private val chapter2 = ChapterEntity("g6_math_c2", "math", 6, 2, "Chương 2: Số nguyên", "Mô tả 2")

    private val lesson1 = LessonEntity("g6_m_c1_l1", "g6_math_c1", 1, "Bài 1", "Tóm tắt 1", "Ví dụ 1", "Mẹo 1", isCompleted = true)
    private val lesson2 = LessonEntity("g6_m_c1_l2", "g6_math_c1", 2, "Bài 2", "Tóm tắt 2", "Ví dụ 2", "Mẹo 2", isCompleted = false)
    private val lesson3 = LessonEntity("g6_m_c2_l1", "g6_math_c2", 1, "Bài 3", "Tóm tắt 3", "Ví dụ 3", "Mẹo 3", isCompleted = false)

    @Before
    fun setup() {
        lessonRepository = FakeLessonRepository(
            initialSubjects = listOf(mathSubject),
            initialChapters = listOf(chapter1, chapter2),
            initialLessons = listOf(lesson1, lesson2, lesson3)
        )
        progressRepository = FakeProgressRepository()
    }

    @Test
    fun testGetSubjectAndChapters() = runTest {
        val subject = lessonRepository.getSubjectById("math")
        assertNotNull(subject)
        assertEquals("Toán", subject?.name)

        val chapters = lessonRepository.getChapters(6, "math").first()
        assertEquals(2, chapters.size)
        assertEquals("g6_math_c1", chapters[0].id)
        assertEquals("g6_math_c2", chapters[1].id)
    }

    @Test
    fun testChaptersWithProgressCalculation() = runTest {
        val chaptersWithProgress = lessonRepository.getChaptersWithProgress(6, "math").first()
        assertEquals(2, chaptersWithProgress.size)

        // Chương 1 có 2 bài, 1 bài đã hoàn thành
        val ch1 = chaptersWithProgress[0]
        assertEquals(2, ch1.totalLessonCount)
        assertEquals(1, ch1.completedLessonCount)
        assertEquals(0.5f, ch1.progressPercent, 0.01f)
        assertFalse(ch1.isAllCompleted)

        // Chương 2 có 1 bài, 0 bài đã hoàn thành
        val ch2 = chaptersWithProgress[1]
        assertEquals(1, ch2.totalLessonCount)
        assertEquals(0, ch2.completedLessonCount)
        assertEquals(0.0f, ch2.progressPercent, 0.01f)
        assertFalse(ch2.isAllCompleted)
    }

    @Test
    fun testFirstUncompletedLessonLogic() = runTest {
        // Bài 1 đã hoàn thành, bài chưa học đầu tiên phải là bài 2
        val firstUncompleted = lessonRepository.getFirstUncompletedLessonId(6, "math")
        assertEquals("g6_m_c1_l2", firstUncompleted)

        // Khi hoàn thành bài 2, bài kế tiếp phải là bài 3 (thuộc chương 2)
        lessonRepository.updateLessonCompletion("g6_m_c1_l2", true)
        val nextUncompleted = lessonRepository.getFirstUncompletedLessonId(6, "math")
        assertEquals("g6_m_c2_l1", nextUncompleted)

        // Khi hoàn thành nốt bài 3 (toàn bộ đã hoàn thành), fallback về bài đầu tiên
        lessonRepository.updateLessonCompletion("g6_m_c2_l1", true)
        val allDoneFallback = lessonRepository.getFirstUncompletedLessonId(6, "math")
        assertEquals("g6_m_c1_l1", allDoneFallback)
    }

    @Test
    fun testLessonNavigationAcrossChapters() = runTest {
        // Bài 1 (đầu tiên)
        val nav1 = lessonRepository.getLessonNavigation(6, "math", "g6_m_c1_l1")
        assertNull(nav1.previousLessonId)
        assertEquals("g6_m_c1_l2", nav1.nextLessonId)
        assertEquals(1, nav1.currentLessonIndex)
        assertEquals(3, nav1.totalLessonsInSubject)

        // Bài 2 (cuối chương 1): bài sau phải nhảy sang Bài 1 của chương 2 (lesson3)
        val nav2 = lessonRepository.getLessonNavigation(6, "math", "g6_m_c1_l2")
        assertEquals("g6_m_c1_l1", nav2.previousLessonId)
        assertEquals("g6_m_c2_l1", nav2.nextLessonId)
        assertEquals(2, nav2.currentLessonIndex)

        // Bài 3 (cuối cùng của môn)
        val nav3 = lessonRepository.getLessonNavigation(6, "math", "g6_m_c2_l1")
        assertEquals("g6_m_c1_l2", nav3.previousLessonId)
        assertNull(nav3.nextLessonId)
        assertEquals(3, nav3.currentLessonIndex)
    }

    @Test
    fun testProgressRepositoryCompletedToggle() = runTest {
        assertFalse(progressRepository.isCompleted("lesson_test"))

        progressRepository.setLessonCompleted("lesson_test", true)
        assertTrue(progressRepository.isCompleted("lesson_test"))
        assertEquals(1, progressRepository.getCompletedLessonsCount(6).first())

        progressRepository.setLessonCompleted("lesson_test", false)
        assertFalse(progressRepository.isCompleted("lesson_test"))
        assertEquals(0, progressRepository.getCompletedLessonsCount(6).first())
    }

    @Test
    fun testLessonFontSizeChange() = runTest {
        assertEquals(LessonFontSize.MEDIUM, lessonRepository.lessonFontSize.first())

        lessonRepository.setLessonFontSize(LessonFontSize.LARGE)
        assertEquals(LessonFontSize.LARGE, lessonRepository.lessonFontSize.first())

        lessonRepository.setLessonFontSize(LessonFontSize.SMALL)
        assertEquals(LessonFontSize.SMALL, lessonRepository.lessonFontSize.first())
    }
}
