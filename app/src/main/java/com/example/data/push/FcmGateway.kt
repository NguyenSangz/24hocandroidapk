package com.example.data.push

/**
 * F20a – Bọc FCM/Google Play Services sau interface để:
 * - Test được toàn bộ luồng đăng ký token bằng bản giả.
 * - Máy không có Google Play Services thì ẩn tính năng, không làm app lỗi.
 */
interface FcmGateway {
    /** Google Play Services có sẵn và cờ/kết nối Firebase hợp lệ. */
    fun isSupported(): Boolean

    /** Lấy token FCM hiện tại; trả null nếu không khả dụng. */
    suspend fun getToken(): String?

    /** Xóa token cục bộ (buộc FCM cấp token mới ở lần sau). */
    suspend fun deleteToken()
}
