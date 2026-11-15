package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Hàng đợi Outbox ngoại tuyến (F16b - Offline-First Sync).
 * Mọi thay đổi dữ liệu học tập cục bộ được ghi ngay vào Room và xếp vào bảng này
 * với khóa ổn định `(collectionName, entityKey)` để gộp (coalesce) thao tác trùng lặp.
 */
@Entity(
    tableName = "sync_outbox",
    indices = [
        Index(value = ["collectionName", "entityKey"], unique = true),
        Index(value = ["status", "retryCount"]),
        Index(value = ["createdAt"])
    ]
)
data class SyncOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectionName: String,
    val entityKey: String,
    val sourceType: String = "",
    val sourceId: String = "",
    val payloadJson: String,
    val deleted: Boolean = false,
    val clientUpdatedAt: Long,
    val retryCount: Int = 0,
    val lastAttemptAt: Long = 0L,
    val lastError: String = "",
    val status: String = STATUS_PENDING,
    val createdAt: Long = clientUpdatedAt
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_IN_FLIGHT = "IN_FLIGHT"
        const val STATUS_FAILED_PERMANENT = "FAILED_PERMANENT"
    }
}

/**
 * Lưu trạng thái đồng bộ & con trỏ delta `lastDeltaCursor` (server `updated` timestamp)
 * riêng cho từng tài khoản người dùng (hoặc guest_local).
 */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val uid: String,
    val lastDeltaCursor: String = "",
    val lastSyncedAt: Long? = null,
    val lastSyncStatus: String = SYNC_STATUS_IDLE,
    val lastErrorMessage: String = "",
    val syncWifiOnly: Boolean = false,
    val updatedAt: Long = 0L
) {
    companion object {
        const val SYNC_STATUS_IDLE = "IDLE"
        const val SYNC_STATUS_SYNCING = "SYNCING"
        const val SYNC_STATUS_OFFLINE = "OFFLINE"
        const val SYNC_STATUS_PAUSED_METERED = "PAUSED_METERED"
        const val SYNC_STATUS_SUCCESS = "SUCCESS"
        const val SYNC_STATUS_RATE_LIMITED = "RATE_LIMITED"
        const val SYNC_STATUS_ERROR = "ERROR"
    }
}
