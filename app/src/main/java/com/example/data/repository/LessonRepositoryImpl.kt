package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.domain.model.ChapterWithProgress
import com.example.domain.model.LessonFontSize
import com.example.domain.model.LessonNavigationInfo
import com.example.domain.repository.LessonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class LessonRepositoryImpl(
    private val database: AppDatabase,
    private val userPreferencesManager: UserPreferencesManager
) : LessonRepository {

    override suspend fun getSubjectById(subjectId: String): SubjectEntity? {
        return database.subjectDao().getSubjectById(subjectId)
    }

    override fun getAllSubjects(): Flow<List<SubjectEntity>> {
        return database.subjectDao().getAllSubjects()
    }

    override fun getChapters(grade: Int, subjectId: String): Flow<List<ChapterEntity>> {
        return database.chapterDao().getChaptersByGradeAndSubject(grade, subjectId)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getChaptersWithProgress(grade: Int, subjectId: String): Flow<List<ChapterWithProgress>> {
        return database.chapterDao().getChaptersByGradeAndSubject(grade, subjectId).flatMapLatest { chapters ->
            if (chapters.isEmpty()) {
                flowOf(emptyList())
            } else {
                val flows = chapters.map { chapter ->
                    database.lessonDao().getLessonsByChapter(chapter.id).map { lessons ->
                        val completedCount = lessons.count { it.isCompleted }
                        ChapterWithProgress(
                            chapter = chapter,
                            lessons = lessons,
                            completedLessonCount = completedCount,
                            totalLessonCount = lessons.size
                        )
                    }
                }
                combine(flows) { it.toList() }
            }
        }
    }

    override fun getLessonsByChapter(chapterId: String): Flow<List<LessonEntity>> {
        return database.lessonDao().getLessonsByChapter(chapterId)
    }

    override suspend fun getLessonById(lessonId: String): LessonEntity? {
        return database.lessonDao().getLessonById(lessonId)
    }

    override fun getLessonFlow(lessonId: String): Flow<LessonEntity?> {
        return database.lessonDao().getLessonFlowById(lessonId)
    }

    override suspend fun getLessonNavigation(
        grade: Int,
        subjectId: String,
        currentLessonId: String
    ): LessonNavigationInfo {
        val chapters = database.chapterDao().getChaptersByGradeAndSubject(grade, subjectId).first()
        val allLessonsInSubject = mutableListOf<LessonEntity>()

        for (ch in chapters) {
            val lessonsInChapter = database.lessonDao().getLessonsByChapter(ch.id).first()
            allLessonsInSubject.addAll(lessonsInChapter)
        }

        val currentIndex = allLessonsInSubject.indexOfFirst { it.id == currentLessonId }
        val prevId = if (currentIndex > 0) allLessonsInSubject[currentIndex - 1].id else null
        val nextId = if (currentIndex >= 0 && currentIndex < allLessonsInSubject.size - 1) {
            allLessonsInSubject[currentIndex + 1].id
        } else null

        return LessonNavigationInfo(
            previousLessonId = prevId,
            nextLessonId = nextId,
            currentLessonIndex = if (currentIndex >= 0) currentIndex + 1 else 1,
            totalLessonsInSubject = allLessonsInSubject.size
        )
    }

    override suspend fun getFirstUncompletedLessonId(grade: Int, subjectId: String): String? {
        val chapters = database.chapterDao().getChaptersByGradeAndSubject(grade, subjectId).first()
        for (ch in chapters) {
            val lessons = database.lessonDao().getLessonsByChapter(ch.id).first()
            val uncompleted = lessons.firstOrNull { !it.isCompleted }
            if (uncompleted != null) {
                return uncompleted.id
            }
        }
        // Nếu đã học xong hết hoặc không có bài nào chưa học, trả về bài đầu tiên
        for (ch in chapters) {
            val lessons = database.lessonDao().getLessonsByChapter(ch.id).first()
            if (lessons.isNotEmpty()) {
                return lessons.first().id
            }
        }
        return null
    }

    override val lessonFontSize: Flow<LessonFontSize> =
        userPreferencesManager.lessonFontSize.map { LessonFontSize.fromKey(it) }

    override suspend fun setLessonFontSize(fontSize: LessonFontSize) {
        userPreferencesManager.setLessonFontSize(fontSize.key)
    }
}
