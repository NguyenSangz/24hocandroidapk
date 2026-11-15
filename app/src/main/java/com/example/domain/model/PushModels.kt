package com.example.domain.model

/**
 * F20a – Các loại thông báo đẩy được hỗ trợ.
 *
 * NGUYÊN TẮC RIÊNG TƯ: payload FCM chỉ được chứa [wireValue] (mã loại) và
 * một mã nội dung (contentId) không nhạy cảm. Toàn bộ chữ hiển thị (tên bài,
 * điểm số, tên học sinh...) do app tự dựng từ dữ liệu cục bộ nên không bao giờ
 * đi qua Google FCM.
 */
enum class PushNotificationType(val wireValue: String) {
    /** Nhắc học — bổ sung cho nhắc học cục bộ (không thay thế, không gây trùng). */
    STUDY_REMINDER("study_reminder"),

    /** Bài tập giáo viên giao sắp đến hạn (F19). */
    ASSIGNMENT_DUE("assignment_due"),

    /** Báo cáo tuần của phụ huynh đã sẵn sàng (F18c). */
    WEEKLY_REPORT("weekly_report"),

    /** Có nội dung học mới (F16c). */
    NEW_CONTENT("new_content"),

    /** Thông báo hệ thống quan trọng (bảo trì, cập nhật bắt buộc...). */
    SYSTEM("system");

    companion object {
        /** Chỉ chấp nhận đúng các mã đã khai báo, mọi giá trị lạ bị loại bỏ. */
        fun fromWire(value: String?): PushNotificationType? =
            entries.firstOrNull { it.wireValue == value?.trim() }
    }
}

/**
 * Payload tối giản nhận từ FCM. Không có trường nào chứa dữ liệu cá nhân.
 */
data class PushPayload(
    val type: PushNotificationType,
    val contentId: String? = null
)

/** Kết quả đồng bộ token với server (idempotent theo token thiết bị). */
enum class PushTokenSyncResult {
    /** Đã gửi token lên server thành công. */
    REGISTERED,

    /** Đã báo server xóa token và xóa token cục bộ. */
    UNREGISTERED,

    /** Chế độ Khách: tuyệt đối không đăng ký token. */
    NO_OP_GUEST,

    /** Người dùng đã tắt thông báo đẩy (hoặc cờ tính năng đang tắt). */
    NO_OP_DISABLED,

    /** Máy không có Google Play Services / không hỗ trợ FCM. */
    UNSUPPORTED,

    /** Chưa lấy được token từ FCM. */
    MISSING_TOKEN,

    /** Lỗi mạng — sẽ thử lại ở lần đồng bộ sau. */
    NETWORK_ERROR,

    /** Phiên đăng nhập hết hạn — cần đăng nhập lại. */
    UNAUTHORIZED,

    /** Lỗi phía server. */
    SERVER_ERROR
}
