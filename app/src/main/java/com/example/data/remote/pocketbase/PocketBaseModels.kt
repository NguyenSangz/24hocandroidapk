package com.example.data.remote.pocketbase

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PocketBaseUserDto(
    val id: String = "",
    val email: String? = null,
    val displayName: String? = null,
    val birthYear: Int? = null,
    val isMinorUnder16: Boolean? = null,
    val parentalConsentGranted: Boolean? = null,
    val parentConsentAt: String? = null,
    val parentEmail: String? = null,
    val authProvider: String? = null,
    val selectedGrade: Int? = null,
    val defaultSeries: String? = null,
    val created: String? = null,
    val updated: String? = null
)

@JsonClass(generateAdapter = true)
data class PocketBaseAuthResponse(
    val token: String = "",
    val record: PocketBaseUserDto = PocketBaseUserDto()
)

@JsonClass(generateAdapter = true)
data class RegisterUserRequest(
    val email: String,
    val password: String,
    val passwordConfirm: String,
    val displayName: String,
    val birthYear: Int?,
    val isMinorUnder16: Boolean,
    val parentalConsentGranted: Boolean,
    val authProvider: String = "EMAIL",
    val selectedGrade: Int,
    val defaultSeries: String
)

@JsonClass(generateAdapter = true)
data class PasswordAuthRequest(
    val identity: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class GoogleSignInRequest(
    val idToken: String,
    val birthYear: Int?,
    val displayName: String?,
    val selectedGrade: Int,
    val defaultSeries: String
)

@JsonClass(generateAdapter = true)
data class PasswordResetRequest(
    val email: String
)

@JsonClass(generateAdapter = true)
data class UpdateDisplayNameRequest(
    val displayName: String
)

@JsonClass(generateAdapter = true)
data class ParentalConsentRequest(
    val birthYear: Int,
    val parentEmail: String
)

@JsonClass(generateAdapter = true)
data class ParentalConsentRequestResponse(
    val verificationRequired: Boolean = false,
    val isMinorUnder16: Boolean = false,
    val expiresAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ParentalConsentConfirmationRequest(
    val token: String
)

@JsonClass(generateAdapter = true)
data class ParentalConsentConfirmationResponse(
    val confirmed: Boolean = false
)

@JsonClass(generateAdapter = true)
data class DeleteAccountResponse(
    val deleted: Boolean = false,
    val deletedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class PocketBaseErrorResponse(
    val code: Int? = null,
    val message: String? = null
)

// F20a – DTO đăng ký/xóa token thiết bị cho thông báo đẩy.
// Payload chỉ chứa token thiết bị + nền tảng; KHÔNG chứa điểm, tên học sinh,
// email hay bất kỳ dữ liệu cá nhân nào.
@JsonClass(generateAdapter = true)
data class RegisterDeviceTokenRequest(
    val token: String,
    val platform: String = "android"
)

@JsonClass(generateAdapter = true)
data class UnregisterDeviceTokenRequest(
    val token: String
)

@JsonClass(generateAdapter = true)
data class PushTokenResponse(
    val registered: Boolean = false,
    val unregistered: Int = 0
)
