package com.example.data.push

import com.example.domain.model.PushNotificationType
import com.example.domain.model.PushPayload

/**
 * F20a – Bộ phân tích payload FCM theo nguyên tắc dữ liệu tối thiểu.
 *
 * Chỉ chấp nhận đúng 2 khóa: `type` (mã loại đã khai báo) và `content_id`
 * (mã nội dung không nhạy cảm, tối đa 128 ký tự an toàn). Mọi khóa khác bị bỏ
 * qua hoàn toàn — kể cả nếu server/nhà cung cấp cố gửi tên, điểm, email...
 */
object PushPayloadParser {

    const val KEY_TYPE = "type"
    const val KEY_CONTENT_ID = "content_id"

    private const val MAX_CONTENT_ID_LENGTH = 128
    private const val TAG = "PushPayloadParser"

    private val SAFE_CONTENT_ID_CHARS = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf('_', '-', '.')

    /**
     * @return payload hợp lệ hoặc null nếu thiếu/không hợp lệ.
     */
    fun parse(data: Map<String, String>): PushPayload? {
        val type = PushNotificationType.fromWire(data[KEY_TYPE]) ?: return null
        val rawContentId = data[KEY_CONTENT_ID]?.trim().orEmpty()
        val contentId = when {
            rawContentId.isEmpty() -> null
            rawContentId.length > MAX_CONTENT_ID_LENGTH -> return null
            rawContentId.all { it in SAFE_CONTENT_ID_CHARS } -> rawContentId
            else -> return null
        }
        return PushPayload(type = type, contentId = contentId)
    }

    /**
     * Chỉ giữ các khóa được phép. Dùng khi cần log/kiểm tra để chắc chắn
     * không có trường dữ liệu cá nhân nào bị đọc nhầm.
     */
    fun safeDataMap(data: Map<String, String>): Map<String, String> =
        data.filterKeys { it == KEY_TYPE || it == KEY_CONTENT_ID }
}
