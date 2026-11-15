package com.example.fake

import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.SubjectEntity
import com.example.domain.model.ChapterWithProgress
import com.example.domain.model.LessonFontSize
import com.example.domain.model.LessonNavigationInfo
import com.example.domain.repository.LessonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeLessonRepository(
    initialSubjects: List<SubjectEntity> = emptyList(),
    initialChapters: List<ChapterEntity> = emptyList(),
    initialLessons: List<LessonEntity> = emptyList()
) : LessonRepository {

    private val subjectsList = initialSubjects.toMutableList()
    private val chaptersList = initialChapters.toMutableList()
    private val lessonsList = initialLessons.toMutableList()

    private val _subjectsFlow = MutableStateFlow(subjectsList.toList())
    private val _fontSizeFlow = MutableStateFlow(LessonFontSize.MEDIUM)

    override suspend fun getSubjectById(subjectId: String): SubjectEntity? {
        return subjectsList.find { it.id == subjectId }
    }

    override fun getAllSubjects(): Flow<List<SubjectEntity>> = _subjectsFlow.asStateFlow()

    override fun getChapters(grade: Int, subjectId: String): Flow<List<ChapterEntity>> {
        return MutableStateFlow(chaptersList.filter { it.grade == grade && it.subjectId == subjectId })
    }

    override fun getChaptersWithProgress(grade: Int, subjectId: String): Flow<List<ChapterWithProgress>> {
        val filteredChapters = chaptersList.filter { it.grade == grade && it.subjectId == subjectId }
        val result = filteredChapters.map { ch ->
            val lessons = lessonsList.filter { it.chapterId == ch.id }
            val completed = lessons.count { it.isCompleted }
            ChapterWithProgress(
                chapter = ch,
                lessons = lessons,
                completedLessonCount = completed,
                totalLessonCount = lessons.size
            )
        }
        return MutableStateFlow(result)
    }

    override fun getLessonsByChapter(chapterId: String): Flow<List<LessonEntity>> {
        return MutableStateFlow(lessonsList.filter { it.chapterId == chapterId })
    }

    override suspend fun getLessonById(lessonId: String): LessonEntity? {
        return lessonsList.find { it.id == lessonId }
    }

    override fun getLessonFlow(lessonId: String): Flow<LessonEntity?> {
        return MutableStateFlow(lessonsList.find { it.id == lessonId })
    }

    override suspend fun getLessonNavigation(
        grade: Int,
        subjectId: String,
        currentLessonId: String
    ): LessonNavigationInfo {
        val chapters = chaptersList.filter { it.grade == grade && it.subjectId == subjectId }
            .sortedBy { it.order }
        val allLessons = mutableListOf<LessonEntity>()
        for (ch in chapters) {
            allLessons.addAll(lessonsList.filter { it.chapterId == ch.id }.sortedBy { it.order })
        }

        val idx = allLessons.indexOfFirst { it.id == currentLessonId }
        val prevId = if (idx > 0) allLessons[idx - 1].id else null
        val nextId = if (idx >= 0 && idx < allLessons.size - 1) allLessons[idx + 1].id else null

        return LessonNavigationInfo(
            previousLessonId = prevId,
            nextLessonId = nextId,
            currentLessonIndex = if (idx >= 0) idx + 1 else 1,
            totalLessonsInSubject = allLessons.size
        )
    }

    override suspend fun getFirstUncompletedLessonId(grade: Int, subjectId: String): String? {
        val chapters = chaptersList.filter { it.grade == grade && it.subjectId == subjectId }
            .sortedBy { it.order }
        for (ch in chapters) {
            val lessons = lessonsList.filter { it.chapterId == ch.id }.sortedBy { it.order }
            val uncompleted = lessons.find { !it.isCompleted }
            if (uncompleted != null) return uncompleted.id
        }
        for (ch in chapters) {
            val lessons = lessonsList.filter { it.chapterId == ch.id }.sortedBy { it.order }
            if (lessons.isNotEmpty()) return lessons.first().id
        }
        return null
    }

    override val lessonFontSize: Flow<LessonFontSize> = _fontSizeFlow.asStateFlow()

    override suspend fun setLessonFontSize(fontSize: LessonFontSize) {
        _fontSizeFlow.value = fontSize
    }

    fun updateLessonCompletion(lessonId: String, completed: Boolean) {
        val idx = lessonsList.indexOfFirst { it.id == lessonId }
        if (idx >= 0) {
            lessonsList[idx] = lessonsList[idx].copy(isCompleted = completed)
        }
    }
}
