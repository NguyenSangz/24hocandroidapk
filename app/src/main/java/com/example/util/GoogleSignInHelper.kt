package com.example.util

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Kết quả trả về từ Jetpack Credential Manager khi lấy Google ID Token.
 */
sealed interface GoogleCredentialResult {
    data class Success(val idToken: String, val displayName: String?) : GoogleCredentialResult
    data object Cancelled : GoogleCredentialResult
    data class Error(val message: String) : GoogleCredentialResult
}

/**
 * Trình hỗ trợ Google Sign-In qua Jetpack Credential Manager (trả về Google ID Token gửi lên PocketBase):
 * - Đọc `GOOGLE_WEB_CLIENT_ID` từ BuildConfig (.env / Secrets) hoặc resource dự phòng nếu có.
 * - Tách riêng `GetSignInWithGoogleOption` và `GetGoogleIdOption` (1 tùy chọn mỗi request).
 */
object GoogleSignInHelper {
    private const val TAG = "GoogleSignInHelper"

    private fun resolveWebClientId(context: Context): String {
        val fromBuildConfig = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (fromBuildConfig.isNotBlank()) return fromBuildConfig
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId != 0) context.getString(resId).trim() else ""
    }

    suspend fun requestInteractiveGoogleSignIn(activity: Activity): GoogleCredentialResult {
        val clientId = resolveWebClientId(activity)
        if (clientId.isBlank()) {
            Log.w(TAG, "Chưa cấu hình GOOGLE_WEB_CLIENT_ID trong BuildConfig")
            return GoogleCredentialResult.Error(
                "Chưa cấu hình GOOGLE_WEB_CLIENT_ID. Hãy điền Web Client ID hoặc dùng đăng nhập bằng Email."
            )
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val credentialManager = CredentialManager.create(activity)
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleCredentialResult.Success(
                    idToken = googleCred.idToken,
                    displayName = googleCred.displayName
                )
            } else {
                GoogleCredentialResult.Error("Loại thông tin xác thực Google không phù hợp.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
            GoogleCredentialResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.w(TAG, "Không tìm thấy tài khoản Google trên thiết bị: ${e.message}")
            GoogleCredentialResult.Error("Thiết bị chưa có tài khoản Google hoặc dịch vụ Google Play chưa sẵn sàng.")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi mở hộp thoại Google Sign-In", e)
            GoogleCredentialResult.Error("Không thể đăng nhập Google lúc này. Bạn vẫn có thể dùng Email hoặc chế độ Khách.")
        }
    }

    suspend fun attemptSilentGoogleIdToken(context: Context): GoogleCredentialResult? {
        val clientId = resolveWebClientId(context)
        if (clientId.isBlank()) return null

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val credentialManager = CredentialManager.create(context)
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleCredentialResult.Success(googleCred.idToken, googleCred.displayName)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun clearCredentialStateSafely(context: Context) {
        withContext(NonCancellable) {
            try {
                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(TAG, "Không thể xóa trạng thái Credential Manager: ${e.message}")
            }
        }
    }
}
