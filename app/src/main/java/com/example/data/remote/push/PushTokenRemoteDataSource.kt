package com.example.data.remote.push

/**
 * Kết quả gọi server khi đăng ký/xóa token thiết bị.
 * Tách khỏi Retrofit để repository/UI test được bằng bản giả.
 */
sealed interface PushTokenRemoteResult {
    data object Success : PushTokenRemoteResult
    data class NetworkError(val message: String) : PushTokenRemoteResult
    data object Unauthorized : PushTokenRemoteResult
    data class ServerError(val message: String) : PushTokenRemoteResult
}

/**
 * F20a – Lớp mạng sau interface. Không chứa logic nghiệp vụ.
 */
interface PushTokenRemoteDataSource {
    /** Gửi token lên server; server upsert theo token nên gọi lặp lại là idempotent. */
    suspend fun registerToken(token: String, platform: String = "android"): PushTokenRemoteResult

    /** Báo server xóa token của thiết bị này. */
    suspend fun unregisterToken(token: String): PushTokenRemoteResult
}
