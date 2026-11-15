package com.example.data.remote.pocketbase

import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.net.ssl.SSLPeerUnverifiedException

sealed interface RemoteAuthResult<out T> {
    data class Success<T>(val data: T) : RemoteAuthResult<T>
    data class NetworkError(val message: String) : RemoteAuthResult<Nothing>
    data class Unauthorized(val message: String) : RemoteAuthResult<Nothing>
    data class ValidationError(val message: String) : RemoteAuthResult<Nothing>
    data class ServerError(val message: String) : RemoteAuthResult<Nothing>
}

interface AuthRemoteDataSource {
    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
        birthYear: Int?,
        isMinorUnder16: Boolean,
        parentalConsentGranted: Boolean,
        selectedGrade: Int,
        defaultSeries: String
    ): RemoteAuthResult<PocketBaseAuthResponse>

    suspend fun loginWithEmail(email: String, password: String): RemoteAuthResult<PocketBaseAuthResponse>

    suspend fun signInWithGoogleIdToken(
        idToken: String,
        birthYear: Int?,
        displayName: String?,
        selectedGrade: Int,
        defaultSeries: String
    ): RemoteAuthResult<PocketBaseAuthResponse>

    suspend fun refreshSession(): RemoteAuthResult<PocketBaseAuthResponse>

    suspend fun requestPasswordReset(email: String): RemoteAuthResult<Unit>

    suspend fun updateDisplayName(userId: String, newName: String): RemoteAuthResult<PocketBaseUserDto>

    suspend fun submitParentalConsent(
        birthYear: Int,
        parentEmail: String
    ): RemoteAuthResult<ParentalConsentRequestResponse>

    suspend fun deleteAccount(): RemoteAuthResult<DeleteAccountResponse>
}

class PocketBaseAuthRemoteDataSource(
    private val api: PocketBaseApi,
    private val moshi: Moshi = PocketBaseNetworkModule.createMoshi()
) : AuthRemoteDataSource {

    override suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
        birthYear: Int?,
        isMinorUnder16: Boolean,
        parentalConsentGranted: Boolean,
        selectedGrade: Int,
        defaultSeries: String
    ): RemoteAuthResult<PocketBaseAuthResponse> = executeSafe {
        api.registerUser(
            RegisterUserRequest(
                email = email.trim(),
                password = password,
                passwordConfirm = password,
                displayName = displayName.trim().take(50),
                birthYear = birthYear,
                isMinorUnder16 = isMinorUnder16,
                parentalConsentGranted = false,
                authProvider = "EMAIL",
                selectedGrade = selectedGrade.coerceIn(6, 9),
                defaultSeries = defaultSeries
            )
        )
        // Sau khi đăng ký xong, đăng nhập ngay để lấy JWT session token
        api.authWithPassword(PasswordAuthRequest(identity = email.trim(), password = password))
    }

    override suspend fun loginWithEmail(
        email: String,
        password: String
    ): RemoteAuthResult<PocketBaseAuthResponse> = executeSafe {
        api.authWithPassword(PasswordAuthRequest(identity = email.trim(), password = password))
    }

    override suspend fun signInWithGoogleIdToken(
        idToken: String,
        birthYear: Int?,
        displayName: String?,
        selectedGrade: Int,
        defaultSeries: String
    ): RemoteAuthResult<PocketBaseAuthResponse> = executeSafe {
        api.signInWithGoogleIdToken(
            GoogleSignInRequest(
                idToken = idToken,
                birthYear = birthYear,
                displayName = displayName,
                selectedGrade = selectedGrade.coerceIn(6, 9),
                defaultSeries = defaultSeries
            )
        )
    }

    override suspend fun refreshSession(): RemoteAuthResult<PocketBaseAuthResponse> = executeSafe {
        api.authRefresh()
    }

    override suspend fun requestPasswordReset(email: String): RemoteAuthResult<Unit> = executeSafe {
        val response = api.requestPasswordReset(PasswordResetRequest(email = email.trim()))
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        Unit
    }

    override suspend fun updateDisplayName(userId: String, newName: String): RemoteAuthResult<PocketBaseUserDto> = executeSafe {
        api.updateUserRecord(userId, UpdateDisplayNameRequest(displayName = newName.trim().take(50)))
    }

    override suspend fun submitParentalConsent(
        birthYear: Int,
        parentEmail: String
    ): RemoteAuthResult<ParentalConsentRequestResponse> = executeSafe {
        api.requestParentalConsent(
            ParentalConsentRequest(birthYear = birthYear, parentEmail = parentEmail.trim())
        )
    }

    override suspend fun deleteAccount(): RemoteAuthResult<DeleteAccountResponse> = executeSafe {
        api.deleteAccount()
    }

    private suspend inline fun <T> executeSafe(crossinline block: suspend () -> T): RemoteAuthResult<T> {
        return try {
            RemoteAuthResult.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: SSLPeerUnverifiedException) {
            RemoteAuthResult.NetworkError("Chứng chỉ HTTPS máy chủ không khớp (Certificate Pinning). Vui lòng kiểm tra cấu hình SSL.")
        } catch (e: UnknownHostException) {
            RemoteAuthResult.NetworkError("Không kết nối được tới máy chủ. Bạn vẫn có thể học ở chế độ Khách.")
        } catch (e: SocketTimeoutException) {
            RemoteAuthResult.NetworkError("Kết nối máy chủ quá thời gian. Vui lòng thử lại khi mạng ổn định.")
        } catch (e: IOException) {
            RemoteAuthResult.NetworkError("Không có kết nối mạng. Mọi bài học offline vẫn hoạt động bình thường.")
        } catch (e: HttpException) {
            mapHttpError(e)
        } catch (e: Exception) {
            RemoteAuthResult.ServerError("Đã xảy ra lỗi kết nối máy chủ. Vui lòng thử lại sau.")
        }
    }

    private fun mapHttpError(e: HttpException): RemoteAuthResult<Nothing> {
        val rawBody = try {
            e.response()?.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        val parsedMsg = if (!rawBody.isNullOrBlank()) {
            try {
                moshi.adapter(PocketBaseErrorResponse::class.java).fromJson(rawBody)?.message
            } catch (_: Exception) {
                null
            }
        } else null

        return when (e.code()) {
            401, 403 -> RemoteAuthResult.Unauthorized(
                parsedMsg?.takeIf { it.isNotBlank() } ?: "Phiên đăng nhập đã hết hạn hoặc không có quyền truy cập."
            )
            400, 404, 422 -> RemoteAuthResult.ValidationError(
                when {
                    rawBody?.contains("Failed to authenticate", ignoreCase = true) == true ->
                        "Email hoặc mật khẩu chưa chính xác."
                    rawBody?.contains("validation_not_unique", ignoreCase = true) == true ->
                        "Email này đã được đăng ký trước đó."
                    !parsedMsg.isNullOrBlank() -> parsedMsg
                    else -> "Thông tin gửi lên chưa hợp lệ. Vui lòng kiểm tra lại."
                }
            )
            else -> RemoteAuthResult.ServerError("Máy chủ tạm thời bận (${e.code()}). Dữ liệu trên máy vẫn an toàn.")
        }
    }

    companion object {
        fun parsePocketBaseTimestamp(isoString: String?): Long? {
            if (isoString.isNullOrBlank()) return null
            val patterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss'Z'"
            )
            for (pattern in patterns) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val parsed = sdf.parse(isoString)
                    if (parsed != null) return parsed.time
                } catch (_: Exception) {
                }
            }
            return null
        }
    }
}
