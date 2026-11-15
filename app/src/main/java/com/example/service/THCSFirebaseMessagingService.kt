package com.example.service

import android.util.Log
import com.example.THCSApplication
import com.example.data.push.PushPayloadParser
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * F20a – Nhận token và payload FCM.
 *
 * LƯU Ý:
 * - FCM gửi dữ liệu dạng data-only (chỉ `type` + `content_id`), nên hệ điều hành
 *   KHÔNG tự hiển thị thông báo. Việc dựng chữ + hiển thị do F20b đảm nhiệm.
 * - Không bao giờ log token thật hay nội dung payload; chỉ log mã loại.
 */
class THCSFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private companion object {
        const val TAG = "THCSFcmService"
    }

    override fun onNewToken(token: String) {
        val app = application as? THCSApplication ?: return
        serviceScope.launch {
            try {
                app.container.pushTokenManager.onNewToken(token)
            } catch (t: Throwable) {
                Log.w(TAG, "Không đăng ký được token mới: ${t.javaClass.simpleName}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = PushPayloadParser.parse(message.data)
        if (payload == null) {
            // Payload lạ/không đúng whitelist: bỏ qua an toàn, không hiển thị gì.
            Log.w(TAG, "Bỏ qua payload FCM không hợp lệ (chỉ chấp nhận type + content_id).")
            return
        }
        // F20a chỉ xác thực payload. F20b sẽ dựng chữ cục bộ + hiển thị theo kênh.
        Log.i(TAG, "Nhận payload hợp lệ loại=${payload.type.wireValue} (không chứa dữ liệu cá nhân).")
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
