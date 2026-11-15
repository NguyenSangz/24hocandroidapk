package com.example.data.push

import kotlinx.coroutines.flow.Flow

/**
 * F20a – Kho lưu trạng thái thông báo đẩy cục bộ.
 *
 * Tách interface khỏi `UserPreferencesManager` (DataStore) để test được logic
 * đăng ký/xóa token bằng bản giả, không cần Android Context.
 *
 * Toàn bộ dữ liệu ở đây chỉ nằm trên máy và KHÔNG được đưa vào snapshot đồng bộ.
 */
interface PushLocalStore {
    /** Công tắc tổng (mặc định tắt). */
    val pushEnabled: Flow<Boolean>

    /** Token đã đăng ký gần nhất (null nếu chưa/đã xóa). */
    suspend fun getRegisteredPushToken(): String?

    /** Thời điểm đăng ký token gần nhất (epoch millis, 0 nếu chưa). */
    suspend fun getPushRegisteredAt(): Long

    /** Ghi nhận token vừa đăng ký thành công. */
    suspend fun setRegisteredPushToken(token: String)

    /** Quên token cục bộ (khi tắt thông báo/đăng xuất/xóa tài khoản). */
    suspend fun clearRegisteredPushToken()
}
