package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Bảng ghi nhận từng lần cộng/trừ XP của người học (F10).
 * Cho phép truy vết minh bạch, chống gian lận và tính tổng XP chính xác.
 */
@Entity(
    tableName = "xp_events",
    indices = [
        Index(value = ["eventKey"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["eventType"])
    ]
)
data class XpEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String, // LESSON_COMPLETED, PRACTICE_CORRECT, PRACTICE_SESSION_COMPLETED, TEST_COMPLETED, FLASHCARD_REVIEW, WRONG_MASTERED, FREEZE_PURCHASE
    val eventKey: String, // Khóa duy nhất chống cộng trùng lặp
    val xpAmount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: String = "" // Mô tả hoặc JSON thông số phụ
)

/**
 * Bảng lưu trữ danh sách huy hiệu đã mở khóa (F10).
 */
@Entity(
    tableName = "badge_unlocks",
    indices = [
        Index(value = ["unlockedAt"])
    ]
)
data class BadgeUnlockEntity(
    @PrimaryKey val badgeId: String,
    val unlockedAt: Long = System.currentTimeMillis(),
    val metadata: String = ""
)
