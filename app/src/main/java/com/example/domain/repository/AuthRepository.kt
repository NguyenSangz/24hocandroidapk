package com.example.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Chế độ tài khoản hiện tại của người học.
 */
enum class AccountMode {
    GUEST,
    AUTHENTICATED
}

/**
 * Trạng thái tài khoản phản ứng cho tầng UI & Sync (Offline-First).
 */
data class AuthAccountState(
    val uid: String = GUEST_UID,
    val email: String = "",
    val displayName: String = "Học sinh THCS",
    val mode: AccountMode = AccountMode.GUEST,
    val authProvider: String = "NONE",
    val birthYear: Int? = null,
    val isMinorUnder16: Boolean = false,
    val parentalConsentGranted: Boolean = false,
    val parentEmail: String = "",
    val parentalConsentTimestamp: Long? = null,
    val sessionExpired: Boolean = false,
    val lastSyncedAt: Long? = null
) {
    val isAuthenticated: Boolean get() = mode == AccountMode.AUTHENTICATED && uid != GUEST_UID
    val needsParentalConsent: Boolean get() = isAuthenticated && isMinorUnder16 && !parentalConsentGranted

    companion object {
        const val GUEST_UID = "guest_local"
    }
}

/**
 * Kết quả xác thực / thao tác tài khoản với thông báo tiếng Việt thân thiện cho học sinh.
 */
sealed interface AuthOperationResult {
    data class Success(val account: AuthAccountState, val message: String = "") : AuthOperationResult
    data class Error(val message: String, val isNetworkError: Boolean = false) : AuthOperationResult
}

/**
 * Giao diện domain trừu tượng hóa nhà cung cấp xác thực và hồ sơ tài khoản (PocketBase REST).
 */
interface AuthRepository {

    /** Luồng trạng thái tài khoản hiện tại (luôn có giá trị, mặc định là Khách). */
    val accountState: Flow<AuthAccountState>

    /** Đọc nhanh trạng thái hiện tại. */
    suspend fun getCurrentAccount(): AuthAccountState

    /**
     * Đăng ký tài khoản mới bằng Email + Mật khẩu trên PocketBase (không bao giờ lưu mật khẩu trên máy).
     */
    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
        birthYear: Int?
    ): AuthOperationResult

    /**
     * Đăng nhập bằng Email + Mật khẩu trên PocketBase (không lưu mật khẩu, chỉ lưu JWT bằng Keystore).
     */
    suspend fun signInWithEmail(
        email: String,
        password: String
    ): AuthOperationResult

    /**
     * Yêu cầu gửi email đặt lại mật khẩu qua SMTP cấu hình trên PocketBase.
     */
    suspend fun requestPasswordReset(email: String): AuthOperationResult

    /**
     * Đăng nhập hoặc nâng cấp từ chế độ Khách bằng Google ID Token (Credential Manager -> PocketBase hook).
     * Dữ liệu học tập cục bộ trong Room được giữ nguyên 100%.
     */
    suspend fun signInWithGoogleIdToken(
        idToken: String,
        birthYear: Int?,
        fallbackDisplayName: String? = null
    ): AuthOperationResult

    /**
     * Yêu cầu server gửi liên kết xác nhận cho cha mẹ/người giám hộ. Lệnh này không
     * tự cấp đồng ý; trạng thái chỉ đổi sau khi server xác nhận liên kết.
     */
    suspend fun submitParentalConsent(
        birthYear: Int,
        parentEmail: String,
        currentYear: Int = java.time.Year.now(java.time.ZoneOffset.UTC).value
    ): AuthOperationResult

    /** Cập nhật tên hiển thị / biệt danh của học sinh. */
    suspend fun updateDisplayName(newName: String): AuthOperationResult

    /** Làm mới phiên PocketBase khi có mạng; nếu mất mạng hoặc hết hạn vẫn giữ nguyên quyền học offline. */
    suspend fun refreshSessionIfNeeded(): AuthAccountState

    /** Đánh dấu phiên hết hạn (không đăng xuất cưỡng bức, không xóa dữ liệu máy). */
    suspend fun markSessionExpired()

    /** Đăng xuất về chế độ Khách (xóa token Keystore, giữ dữ liệu học cục bộ). */
    suspend fun signOutToGuest(): AuthOperationResult

    /**
     * Xóa tài khoản và toàn bộ dữ liệu trên máy chủ PocketBase.
     * @param deleteLocalData true nếu xóa luôn dữ liệu học trên máy, false nếu giữ lại ở chế độ Khách.
     */
    suspend fun deleteAccount(deleteLocalData: Boolean): AuthOperationResult
}

/**
 * Bộ kiểm tra hợp lệ đầu vào tài khoản (email, mật khẩu, biệt danh, năm sinh).
 */
object AccountInputValidator {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotEmpty() && trimmed.length <= 120 && EMAIL_REGEX.matches(trimmed)
    }

    fun isValidPassword(password: String): Boolean {
        return password.length in 8..72
    }

    fun isValidDisplayName(name: String): Boolean {
        val trimmed = name.trim()
        return trimmed.length in 2..50
    }

    fun isValidBirthYear(
        year: Int?,
        currentYear: Int = java.time.Year.now(java.time.ZoneOffset.UTC).value
    ): Boolean {
        if (year == null) return false
        return year in 1940..currentYear
    }

    fun isMinorUnder16(
        birthYear: Int?,
        currentYear: Int = java.time.Year.now(java.time.ZoneOffset.UTC).value
    ): Boolean {
        if (birthYear == null) return true
        return (currentYear - birthYear) < 16
    }
}
