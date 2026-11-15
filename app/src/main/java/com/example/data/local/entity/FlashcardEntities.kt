package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Bảng thông tin bộ thẻ Flashcard (F09).
 */
@Entity(
    tableName = "flashcard_decks",
    indices = [
        Index(value = ["grade", "subjectId"]),
        Index(value = ["type"])
    ]
)
data class FlashcardDeckEntity(
    @PrimaryKey val id: String,
    val name: String,
    val grade: Int,
    val subjectId: String,
    val type: String, // TỪ VỰNG, CÔNG THỨC, KHÁI NIỆM, MỐC LỊCH SỬ
    val description: String = "",
    val cardCount: Int = 0
)

/**
 * Bảng trạng thái tiến độ học SM-2 của từng thẻ Flashcard (F09).
 */
@Entity(
    tableName = "flashcard_progress",
    indices = [
        Index(value = ["deckId"]),
        Index(value = ["nextReviewTimestamp"]),
        Index(value = ["state"])
    ]
)
data class FlashcardProgressEntity(
    @PrimaryKey val cardId: String,
    val deckId: String,
    val repetitions: Int = 0, // Số lần liên tiếp đánh giá >= 3
    val intervalDays: Int = 0, // Khoảng cách ngày hẹn ôn
    val easeFactor: Float = 2.5f, // Hệ số dễ SM-2 (tối thiểu 1.3)
    val consecutiveCorrect: Int = 0, // Số lần đúng liên tiếp
    val nextReviewTimestamp: Long = 0L, // Mốc thời gian đến hạn ôn (ms)
    val lastReviewTimestamp: Long = 0L, // Mốc thời gian ôn gần nhất (ms)
    val state: String = "NEW", // NEW, LEARNING, REVIEW, MASTERED
    val totalReviews: Int = 0,
    val lapses: Int = 0 // Số lần đánh giá Lại
)

/**
 * Bảng lịch sử ôn tập từng lượt thẻ Flashcard (F09).
 */
@Entity(
    tableName = "review_logs",
    indices = [
        Index(value = ["cardId"]),
        Index(value = ["deckId"]),
        Index(value = ["reviewTimestamp"])
    ]
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: String,
    val deckId: String,
    val rating: Int, // 0: Lại, 3: Khó, 4: Được, 5: Dễ
    val reviewTimestamp: Long,
    val scheduledDays: Int,
    val easeFactor: Float,
    val timeSpentMs: Long = 0L,
    val isCustomSession: Boolean = false // True nếu là chế độ học tự do
)

/**
 * Lớp ánh xạ kết hợp giữa Thẻ Flashcard và Tiến độ học SM-2 tương ứng.
 */
data class FlashcardWithProgress(
    @Embedded val card: FlashcardEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "cardId"
    )
    val progress: FlashcardProgressEntity?
)
