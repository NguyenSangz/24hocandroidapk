package com.example.fake

import com.example.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeProgressRepository : ProgressRepository {
    private val completedLessons = mutableSetOf<String>()
    private val _completedCountFlow = MutableStateFlow(0)
    private val _totalCountFlow = MutableStateFlow(10)

    override suspend fun setLessonCompleted(lessonId: String, isCompleted: Boolean) {
        if (isCompleted) {
            completedLessons.add(lessonId)
        } else {
            completedLessons.remove(lessonId)
        }
        _completedCountFlow.value = completedLessons.size
    }

    override fun getCompletedLessonsCount(grade: Int): Flow<Int> = _completedCountFlow.asStateFlow()

    override fun getTotalLessonsCount(grade: Int): Flow<Int> = _totalCountFlow.asStateFlow()

    fun isCompleted(lessonId: String): Boolean = completedLessons.contains(lessonId)
}
