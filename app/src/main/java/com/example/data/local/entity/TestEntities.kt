package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Bảng lưu trữ phiên kiểm tra (F07)
 */
@Entity(
    tableName = "tests",
    indices = [
        Index(value = ["subjectId", "grade"]),
        Index(value = ["timestamp"])
    ]
)
data class TestEntity(
    @PrimaryKey val id: String, // UUID
    val title: String,
    val type: String, // LESSON, CHAPTER, MID_TERM, FINAL
    val subjectId: String,
    val subjectName: String,
    val grade: Int,
    val targetId: String = "all", // lessonId hoặc chapterId hoặc "all"
    val score: Float, // Thang điểm 10 (làm tròn 1 chữ số thập phân)
    val maxScore: Float = 10.0f,
    val totalQuestions: Int,
    val correctCount: Int,
    val timeSpentSeconds: Int,
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isDraft: Boolean = false, // true nếu đang làm dở, false nếu đã nộp bài
    val remainingSeconds: Int = 0, // số giây còn lại khi lưu nháp
    val endTimeEpochMillis: Long = 0L // mốc kết thúc chính xác
)

/**
 * Bảng lưu trữ chi tiết từng câu trả lời trong bài kiểm tra (F07)
 */
@Entity(
    tableName = "test_answers",
    indices = [
        Index(value = ["testId"]),
        Index(value = ["questionId"])
    ]
)
data class TestAnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val testId: String,
    val questionId: String,
    val questionIndex: Int,
    val userAnswerJson: String = "", // Chuỗi json hoặc chuỗi định dạng câu trả lời
    val isAnswered: Boolean = false,
    val isFlagged: Boolean = false, // Đánh dấu "xem lại sau"
    val score: Float = 0.0f,
    val isCorrect: Boolean = false,
    val gradeStatus: String = "INCORRECT", // CORRECT, PARTIALLY_CORRECT, INCORRECT, SELF_EVALUATION
    val selfEvaluated: Boolean = false
)
