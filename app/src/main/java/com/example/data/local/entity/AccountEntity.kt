package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Thực thể Room lưu trữ trạng thái tài khoản cục bộ (Offline-First).
 * Mặc định có một bản ghi khách ("guest_local") để toàn bộ tính năng học tập hoạt động không cần đăng nhập.
 * Khi nâng cấp lên tài khoản Google, tiến độ cục bộ vẫn được giữ nguyên và gắn sang tài khoản mới.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val uid: String,
    val email: String = "",
    val displayName: String = "Học sinh THCS",
    val accountMode: String = MODE_GUEST, // GUEST | AUTHENTICATED
    val authProvider: String = PROVIDER_NONE, // NONE | GOOGLE
    val birthYear: Int? = null,
    val isMinorUnder16: Boolean = false,
    val parentalConsentGranted: Boolean = false,
    val parentEmail: String = "",
    val parentalConsentTimestamp: Long? = null,
    val sessionExpired: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long? = null
) {
    companion object {
        const val GUEST_UID = "guest_local"
        const val MODE_GUEST = "GUEST"
        const val MODE_AUTHENTICATED = "AUTHENTICATED"
        const val PROVIDER_NONE = "NONE"
        const val PROVIDER_GOOGLE = "GOOGLE"
    }
}
