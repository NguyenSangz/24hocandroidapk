package com.example.data.push

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * F20a – Triển khai thật của [FcmGateway].
 *
 * Mọi lời gọi đều được bọc try/catch: máy không có Google Play Services,
 * Firebase chưa cấu hình hoặc token chưa sẵn sàng thì trả về null/false thay vì
 * làm crash app. Không log token (chỉ log trạng thái).
 */
class FirebaseFcmGateway(
    private val context: Context
) : FcmGateway {

    private companion object {
        const val TAG = "FirebaseFcmGateway"
    }

    override fun isSupported(): Boolean {
        return try {
            val status = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context.applicationContext)
            status == ConnectionResult.SUCCESS
        } catch (t: Throwable) {
            Log.w(TAG, "Không kiểm tra được Google Play Services: ${t.javaClass.simpleName}")
            false
        }
    }

    override suspend fun getToken(): String? {
        if (!isSupported()) return null
        return try {
            suspendCancellableCoroutine<String?> { continuation ->
                FirebaseMessaging.getInstance().token
                    .addOnCompleteListener { task ->
                        if (!continuation.isActive) return@addOnCompleteListener
                        if (task.isSuccessful) {
                            val token = task.result
                            continuation.resume(token?.takeIf { it.isNotBlank() })
                        } else {
                            continuation.resume(null)
                        }
                    }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Không lấy được token FCM: ${t.javaClass.simpleName}")
            null
        }
    }

    override suspend fun deleteToken() {
        if (!isSupported()) return
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                FirebaseMessaging.getInstance().deleteToken()
                    .addOnCompleteListener {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Không xóa được token FCM: ${t.javaClass.simpleName}")
        }
    }
}
