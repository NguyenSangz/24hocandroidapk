package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import org.json.JSONArray

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val description: String,
    val category: String = "Chung",
    val isPinned: Boolean = false
)

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val grade: Int,
    val order: Int,
    val title: String,
    val description: String,
    val series: String = "kntt",
    val phanMon: String = ""
)

@Entity(
    tableName = "lessons",
    indices = [Index(value = ["chapterId"])]
)
data class LessonEntity(
    @PrimaryKey val id: String,
    val chapterId: String,
    val order: Int,
    val title: String,
    val summary: String,
    val examples: String,
    val tips: String,
    val formulasLatex: String = "",
    val tablesJson: String = "",
    val isCompleted: Boolean = false,
    val series: String = "kntt",
    val topicId: String = "",
    val phanMon: String = "",
    val tier: String = "tier_a",
    val objectives: String = ""
)

@Entity(
    tableName = "questions",
    indices = [Index(value = ["lessonId"])]
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val lessonId: String,
    val type: String, // MULTIPLE_CHOICE, TRUE_FALSE, FILL_BLANK, MATCHING, SORTING, SHORT_ESSAY
    val content: String,
    val optionsJson: String, // JSON array string
    val correctAnswer: String,
    val hint: String,
    val explanation: String,
    val difficulty: Int = 1, // 1: Nhận biết, 2: Thông hiểu, 3: Vận dụng
    val series: String = "kntt",
    val phanMon: String = "",
    val topicId: String = ""
)

/**
 * Ngân hàng câu sai (F08) - Lưu vết câu làm sai theo phương pháp Spaced Repetition
 */
@Entity(
    tableName = "wrong_questions",
    indices = [
        Index(value = ["grade", "subjectId"]),
        Index(value = ["status"]),
        Index(value = ["nextReviewTimestamp"])
    ]
)
data class WrongQuestionEntity(
    @PrimaryKey val questionId: String,
    val grade: Int = 6,
    val subjectId: String = "",
    val chapterId: String = "",
    val lessonId: String = "",
    val questionType: String = "",
    val errorCount: Int = 1,
    val consecutiveCorrectCount: Int = 0,
    val lastWrongTimestamp: Long = System.currentTimeMillis(),
    val lastWrongAnswer: String = "",
    val status: String = "NEEDS_REVIEW", // NEEDS_REVIEW, CONSOLIDATING, MASTERED
    val nextReviewTimestamp: Long = 0L,
    val lastReviewTimestamp: Long = 0L
)

@Entity(
    tableName = "flashcards",
    indices = [
        Index(value = ["deckId"]),
        Index(value = ["grade", "subjectId"])
    ]
)
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val deckId: String = "deck_default",
    val grade: Int = 6,
    val subjectId: String = "",
    val front: String = "",
    val back: String = "",
    val category: String = "Chung",
    val series: String = "kntt",
    val phanMon: String = "",
    val topicId: String = "",
    val example: String? = null,
    val notes: String? = null,
    val word: String? = null,
    val partOfSpeech: String? = null,
    val phonetic: String? = null,
    val meaning: String? = null,
    val exampleSentence: String? = null,
    val voiceCode: String? = "en-US",
    val box: Int = 1,
    val intervalDays: Int = 1,
    val nextReviewTimestamp: Long = 0L,
    val easeFactor: Float = 2.5f,
    val repetitions: Int = 0
)

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val questionsAnswered: Int = 0,
    val questionsCorrect: Int = 0,
    val studyMinutes: Int = 0,
    val flashcardsReviewed: Int = 0,
    val xpEarned: Int = 0
)

@Entity(tableName = "exam_results")
data class ExamResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val grade: Int,
    val subjectId: String,
    val score: Float,
    val totalQuestions: Int,
    val correctCount: Int,
    val timeSpentSeconds: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Bảng lưu lịch sử làm bài thi/luyện tập từng câu hỏi (F05)
 * Ghi nhận chi tiết: câu hỏi nào, đúng/sai, số điểm đạt được, thời điểm làm, thời gian trả lời (giây).
 */
@Entity(tableName = "attempts")
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: String,
    val isCorrect: Boolean,
    val score: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val timeSpentSeconds: Int = 0,
    val userAnswer: String = "",
    val usedHint: Boolean = false
)

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return "[]"
        val array = JSONArray()
        value.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(value)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["grade", "subjectId"])]
)
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val targetType: String,
    val title: String,
    val grade: Int = 6,
    val subjectId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "personal_notes",
    indices = [Index(value = ["grade", "subjectId"])]
)
data class PersonalNoteEntity(
    @PrimaryKey val id: String,
    val targetType: String,
    val title: String,
    val noteContent: String,
    val grade: Int = 6,
    val subjectId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
