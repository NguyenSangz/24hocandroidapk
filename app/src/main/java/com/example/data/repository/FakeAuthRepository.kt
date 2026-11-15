package com.example.data.repository

import com.example.domain.repository.AccountInputValidator
import com.example.domain.repository.AccountMode
import com.example.domain.repository.AuthAccountState
import com.example.domain.repository.AuthOperationResult
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bản giả AuthRepository phục vụ Unit Test, UI Test và Compose @Preview (Offline-First).
 */
class FakeAuthRepository(
    initialState: AuthAccountState = AuthAccountState()
) : AuthRepository {

    private val _accountState = MutableStateFlow(initialState)
    override val accountState: Flow<AuthAccountState> = _accountState.asStateFlow()

    var shouldFailNetwork: Boolean = false
    var serverDeletedCount: Int = 0
    var localDataClearedCount: Int = 0
    var passwordResetRequests: Int = 0
    var emailSignInCount: Int = 0
    var emailRegistrationCount: Int = 0

    override suspend fun getCurrentAccount(): AuthAccountState = _accountState.value

    override suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
        birthYear: Int?
    ): AuthOperationResult {
        emailRegistrationCount++
        if (shouldFailNetwork) {
            return AuthOperationResult.Error("Không có kết nối mạng.", isNetworkError = true)
        }
        if (!AccountInputValidator.isValidEmail(email)) {
            return AuthOperationResult.Error("Địa chỉ email không hợp lệ.")
        }
        if (!AccountInputValidator.isValidPassword(password)) {
            return AuthOperationResult.Error("Mật khẩu phải từ 8 đến 72 ký tự.")
        }
        val isMinor = AccountInputValidator.isMinorUnder16(birthYear)
        val next = AuthAccountState(
            uid = "pb_fake_email_001",
            email = email.trim(),
            displayName = displayName.trim().ifBlank { "Học sinh THCS" },
            mode = AccountMode.AUTHENTICATED,
            authProvider = "EMAIL",
            birthYear = birthYear,
            isMinorUnder16 = isMinor,
            parentalConsentGranted = !isMinor,
            sessionExpired = false,
            lastSyncedAt = System.currentTimeMillis()
        )
        _accountState.value = next
        return AuthOperationResult.Success(next, "Đăng ký thành công!")
    }

    override suspend fun signInWithEmail(email: String, password: String): AuthOperationResult {
        emailSignInCount++
        if (shouldFailNetwork) {
            return AuthOperationResult.Error("Không có kết nối mạng.", isNetworkError = true)
        }
        if (!AccountInputValidator.isValidEmail(email) || password.isBlank()) {
            return AuthOperationResult.Error("Email hoặc mật khẩu chưa hợp lệ.")
        }
        val current = _accountState.value
        val next = current.copy(
            uid = "pb_fake_email_001",
            email = email.trim(),
            mode = AccountMode.AUTHENTICATED,
            authProvider = "EMAIL",
            sessionExpired = false,
            lastSyncedAt = System.currentTimeMillis()
        )
        _accountState.value = next
        return AuthOperationResult.Success(next, "Đăng nhập thành công!")
    }

    override suspend fun requestPasswordReset(email: String): AuthOperationResult {
        if (shouldFailNetwork) {
            return AuthOperationResult.Error("Không có kết nối mạng.", isNetworkError = true)
        }
        if (!AccountInputValidator.isValidEmail(email)) {
            return AuthOperationResult.Error("Email không hợp lệ.")
        }
        passwordResetRequests++
        return AuthOperationResult.Success(_accountState.value, "Đã gửi email đặt lại mật khẩu.")
    }

    override suspend fun signInWithGoogleIdToken(
        idToken: String,
        birthYear: Int?,
        fallbackDisplayName: String?
    ): AuthOperationResult {
        if (shouldFailNetwork) {
            return AuthOperationResult.Error("Không có kết nối mạng.", isNetworkError = true)
        }
        if (idToken.isBlank()) {
            return AuthOperationResult.Error("Token xác thực không hợp lệ.")
        }
        val current = _accountState.value
        val effectiveYear = birthYear ?: current.birthYear
        val isMinor = AccountInputValidator.isMinorUnder16(effectiveYear)
        val next = AuthAccountState(
            uid = "pb_fake_google_001",
            email = "hocsinh@example.com",
            displayName = fallbackDisplayName?.takeIf { it.isNotBlank() } ?: current.displayName,
            mode = AccountMode.AUTHENTICATED,
            authProvider = "GOOGLE",
            birthYear = effectiveYear,
            isMinorUnder16 = isMinor,
            parentalConsentGranted = current.parentalConsentGranted || !isMinor,
            parentEmail = current.parentEmail,
            parentalConsentTimestamp = current.parentalConsentTimestamp,
            sessionExpired = false,
            lastSyncedAt = System.currentTimeMillis()
        )
        _accountState.value = next
        return AuthOperationResult.Success(next, "Đăng nhập giả lập thành công!")
    }

    override suspend fun submitParentalConsent(
        birthYear: Int,
        parentEmail: String,
        currentYear: Int
    ): AuthOperationResult {
        if (shouldFailNetwork) {
            return AuthOperationResult.Error("Không có kết nối mạng.", isNetworkError = true)
        }
        if (!AccountInputValidator.isValidBirthYear(birthYear, currentYear)) {
            return AuthOperationResult.Error("Năm sinh không hợp lệ.")
        }
        val isMinor = AccountInputValidator.isMinorUnder16(birthYear, currentYear)
        if (isMinor && !AccountInputValidator.isValidEmail(parentEmail)) {
            return AuthOperationResult.Error("Email phụ huynh không hợp lệ.")
        }
        val updated = _accountState.value.copy(
            birthYear = birthYear,
            isMinorUnder16 = isMinor,
            parentalConsentGranted = !isMinor,
            parentEmail = if (isMinor) parentEmail.trim() else "",
            parentalConsentTimestamp = null
        )
        _accountState.value = updated
        return AuthOperationResult.Success(
            updated,
            if (isMinor) "Đã gửi liên kết xác nhận tới email phụ huynh." else "Đã cập nhật thông tin độ tuổi."
        )
    }

    fun simulateParentalConsentConfirmation() {
        val current = _accountState.value
        _accountState.value = current.copy(
            parentalConsentGranted = true,
            parentalConsentTimestamp = System.currentTimeMillis()
        )
    }

    override suspend fun updateDisplayName(newName: String): AuthOperationResult {
        val trimmed = newName.trim()
        if (!AccountInputValidator.isValidDisplayName(trimmed)) {
            return AuthOperationResult.Error("Biệt danh phải từ 2 đến 50 ký tự.")
        }
        val updated = _accountState.value.copy(displayName = trimmed)
        _accountState.value = updated
        return AuthOperationResult.Success(updated, "Đã đổi biệt danh.")
    }

    override suspend fun refreshSessionIfNeeded(): AuthAccountState = _accountState.value

    override suspend fun markSessionExpired() {
        if (_accountState.value.isAuthenticated) {
            _accountState.value = _accountState.value.copy(sessionExpired = true)
        }
    }

    override suspend fun signOutToGuest(): AuthOperationResult {
        val guest = AuthAccountState(
            displayName = _accountState.value.displayName,
            mode = AccountMode.GUEST
        )
        _accountState.value = guest
        return AuthOperationResult.Success(guest, "Đã đăng xuất về chế độ Khách.")
    }

    override suspend fun deleteAccount(deleteLocalData: Boolean): AuthOperationResult {
        if (shouldFailNetwork) {
            return AuthOperationResult.Error("Cần kết nối mạng để xóa tài khoản trên máy chủ.", isNetworkError = true)
        }
        serverDeletedCount++
        if (deleteLocalData) {
            localDataClearedCount++
        }
        val guest = AuthAccountState(mode = AccountMode.GUEST)
        _accountState.value = guest
        return AuthOperationResult.Success(guest, "Đã xóa tài khoản.")
    }
}
