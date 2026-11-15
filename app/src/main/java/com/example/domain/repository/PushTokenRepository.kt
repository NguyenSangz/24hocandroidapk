package com.example.domain.repository

import com.example.domain.model.PushTokenSyncResult
import kotlinx.coroutines.flow.StateFlow

/**
 * F20a – Lớp mạng/đăng ký token nằm sau interface để test bằng bản giả và
 * đổi backend mà không sửa UI.
 */
interface PushTokenRepository {
    /**
     * Thiết bị này có hỗ trợ thông báo đẩy không?
     * Trả về false khi cờ tính năng tắt, máy không có Google Play Services
     * hoặc thiếu cấu hình Firebase — khi đó app hoạt động y như trước.
     */
    suspend fun isSupported(): Boolean

    /**
     * Đăng ký token FCM hiện tại cho tài khoản đang đăng nhập.
     * Idempotent: gọi nhiều lần với cùng token không tạo bản ghi trùng.
     *
     * @param tokenOverride token mới do FCM cấp trong `onNewToken`.
     */
    suspend fun registerCurrentToken(tokenOverride: String? = null): PushTokenSyncResult

    /**
     * Xóa token khỏi server và xóa token cục bộ.
     * Bắt buộc gọi trước khi đăng xuất, xóa tài khoản hoặc tắt thông báo.
     */
    suspend fun unregisterCurrentDevice(): Boolean
}

/**
 * Điều khiển cài đặt thông báo đẩy cho UI (màn hình Cài đặt).
 * Tách khỏi repository để @Preview/test truyền bản giả.
 */
interface PushSettingsController {
    /** Thiết bị có hỗ trợ FCM và cờ tính năng đang bật. */
    val isSupported: StateFlow<Boolean>

    /** Người dùng đang đăng nhập tài khoản (chế độ Khách không đăng ký token). */
    val isSignedIn: StateFlow<Boolean>

    /** Bật/tắt thông báo đẩy; repository tự đăng ký hoặc xóa token tương ứng. */
    suspend fun setEnabled(enabled: Boolean)

    /** Đồng bộ lại trạng thái hỗ trợ/đăng nhập (gọi khi mở màn hình Cài đặt). */
    suspend fun refresh()
}
