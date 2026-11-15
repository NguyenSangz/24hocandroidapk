package com.example.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Domain interface quản lý tiến độ học tập của từng bài học (F04)
 */
interface ProgressRepository {
    /**
     * Cập nhật trạng thái hoàn thành bài học (bảng lessons / tiến độ)
     */
    suspend fun setLessonCompleted(lessonId: String, isCompleted: Boolean)

    /**
     * Đếm số bài đã hoàn thành theo khối lớp
     */
    fun getCompletedLessonsCount(grade: Int): Flow<Int>

    /**
     * Tổng số bài học theo khối lớp
     */
    fun getTotalLessonsCount(grade: Int): Flow<Int>
}
