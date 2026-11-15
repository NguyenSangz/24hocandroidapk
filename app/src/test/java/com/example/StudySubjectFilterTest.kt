package com.example

import com.example.data.local.entity.SubjectEntity
import com.example.ui.viewmodel.StudyViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test cho logic ẩn môn học chưa có nội dung (F17a).
 * `sample_curriculum.json` khai báo 15 môn nhưng hiện mới chỉ có Toán, Tiếng Anh
 * (và các môn được bổ sung qua gói nội dung). Môn chưa có gói phải bị ẩn khỏi
 * tab Học tập thay vì hiện chip rỗng.
 */
class StudySubjectFilterTest {

    private val allSubjects = listOf(
        SubjectEntity("math", "Toán", "calculate", "#2563EB", ""),
        SubjectEntity("english", "Tiếng Anh", "translate", "#059669", ""),
        SubjectEntity("gdcd", "GDCD", "balance", "#7C3AED", ""),
        SubjectEntity("physics", "Vật lí", "bolt", "#0EA5E9", ""),
        SubjectEntity("literature", "Ngữ văn", "menu_book", "#DC2626", "")
    )

    @Test
    fun filter_keepsOnlySubjectsWithContent() {
        val filtered = StudyViewModel.filterSubjectsWithContent(
            allSubjects = allSubjects,
            subjectIdsWithContent = listOf("math", "english", "gdcd")
        )
        assertEquals(listOf("math", "english", "gdcd"), filtered.map { it.id })
    }

    @Test
    fun filter_keepsOriginalOrderAndPinnedFirst() {
        // `allSubjects` đã được DAO sắp xếp isPinned DESC, name ASC; filter phải giữ nguyên thứ tự
        val sorted = allSubjects.sortedByDescending { it.isPinned }
        val filtered = StudyViewModel.filterSubjectsWithContent(
            allSubjects = sorted,
            subjectIdsWithContent = listOf("physics", "literature", "math")
        )
        assertEquals(listOf("math", "physics", "literature"), filtered.map { it.id })
    }

    @Test
    fun filter_emptyContentList_returnsEmptyList() {
        val filtered = StudyViewModel.filterSubjectsWithContent(allSubjects, emptyList())
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun filter_allSubjectsHaveContent_returnsAll() {
        val filtered = StudyViewModel.filterSubjectsWithContent(
            allSubjects = allSubjects,
            subjectIdsWithContent = allSubjects.map { it.id }
        )
        assertEquals(allSubjects.size, filtered.size)
    }
}
