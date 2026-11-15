package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow

class ProgressRepositoryImpl(
    private val database: AppDatabase
) : ProgressRepository {

    override suspend fun setLessonCompleted(lessonId: String, isCompleted: Boolean) {
        database.lessonDao().updateLessonCompleted(lessonId, isCompleted)
        com.example.data.sync.SyncOutboxHelper.enqueueLessonProgress(database, lessonId, isCompleted)
    }

    override fun getCompletedLessonsCount(grade: Int): Flow<Int> {
        return database.lessonDao().getCompletedLessonCount(grade)
    }

    override fun getTotalLessonsCount(grade: Int): Flow<Int> {
        return database.lessonDao().getTotalLessonCount(grade)
    }
}
