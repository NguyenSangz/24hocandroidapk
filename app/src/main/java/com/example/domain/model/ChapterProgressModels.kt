package com.example.domain.model

import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.LessonEntity

/**
 * Domain model đại diện cho một chương học cùng danh sách bài học và tiến độ hoàn thành
 */
data class ChapterWithProgress(
    val chapter: ChapterEntity,
    val lessons: List<LessonEntity>,
    val completedLessonCount: Int,
    val totalLessonCount: Int
) {
    val progressPercent: Float
        get() = if (totalLessonCount == 0) 0f else completedLessonCount.toFloat() / totalLessonCount.toFloat()

    val isAllCompleted: Boolean
        get() = totalLessonCount > 0 && completedLessonCount == totalLessonCount
}

/**
 * Điều hướng bài học (Bài trước / Bài sau)
 */
data class LessonNavigationInfo(
    val previousLessonId: String?,
    val nextLessonId: String?,
    val currentLessonIndex: Int,
    val totalLessonsInSubject: Int
)
