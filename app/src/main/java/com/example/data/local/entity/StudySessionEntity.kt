package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Bảng ghi nhận phiên học tập chi tiết của học sinh (F11 - Thống kê học tập).
 * Lưu lại chính xác thời gian foreground, môn học, lớp và phân loại hoạt động.
 */
@Entity(
    tableName = "study_sessions",
    indices = [
        Index(value = ["startTimestamp"]),
        Index(value = ["grade", "subjectId"]),
        Index(value = ["activityType"])
    ]
)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activityType: String, // LESSON, PRACTICE, TEST, FLASHCARD, WRONG_REVIEW
    val subjectId: String, // math, literature, english, physics, chemistry, biology, history, etc.
    val grade: Int, // 6, 7, 8, 9
    val startTimestamp: Long, // Epoch milliseconds
    val durationSeconds: Int, // Thời lượng thực tế (giây) hợp lệ sau khi lọc treo máy
    val targetId: String = "" // lessonId / chapterId / testId / deckId
)
