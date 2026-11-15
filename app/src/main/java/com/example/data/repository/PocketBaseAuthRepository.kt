package com.example.data.repository

import android.util.Log
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.SyncDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.security.TokenStorage
import com.example.data.preferences.UserPreferencesManager
import com.example.data.remote.NetworkLogSanitizer
import com.example.data.remote.pocketbase.AuthRemoteDataSource
import com.example.data.remote.pocketbase.PocketBaseAuthRemoteDataSource
import com.example.data.remote.pocketbase.PocketBaseAuthResponse
import com.example.data.remote.pocketbase.RemoteAuthResult
import com.example.domain.repository.AccountInputValidator
import com.example.domain.repository.AccountMode
import com.example.domain.repository.AuthAccountState
import com.example.domain.repository.AuthOperationResult
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Triển khai AuthRepository kết nối tới máy chủ PocketBase tự host:
 * - Room (`accounts`) là nguồn dữ liệu duy nhất mà UI đọc (Offline-First).
 * - Token phiên được mã hóa bằng AndroidKeyStore qua `TokenStorage`.
 * - Không bao giờ lưu mật khẩu trên thiết bị.
 */
class PocketBaseAuthRepository(
    private val accountDao: AccountDao,
    private val remoteDataSource: AuthRemoteDataSource,
    private val tokenStorage: TokenStorage,
    private val settingsRepository: SettingsRepository,
    private val preferencesManager: UserPreferencesManager,
    private val syncDao: SyncDao? = null,
    /**
     * F20a – Gọi TRƯỚC khi xóa phiên để hủy đăng ký token thông báo đẩy
     * (còn Authorization). Mặc định no-op để không phá vỡ các test cũ.
     */
    private val onSessionEnding: (suspend () -> Unit)? = null
) : AuthRepository {

    companion object {
        private const val TAG = "PocketBaseAuthRepo"
    }

    override val accountState: Flow<AuthAccountState> = accountDao.observeActiveAccount()
        .map { entity -> entity?.toDomain() ?: AuthAccountState() }

    override suspend fun getCurrentAccount(): AuthAccountState {
        return accountDao.getActiveAccountSync()?.toDomain() ?: AuthAccountState()
    }

    override suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String,
        birthYear: Int?
    ): AuthOperationResult {
        val cleanEmail = email.trim()
        if (!AccountInputValidator.isValidEmail(cleanEmail)) {
            return AuthOperationResult.Error("Địa chỉ email không hợp lệ.")
        }
        if (!AccountInputValidator.isValidPassword(password)) {
            return AuthOperationResult.Error("Mật khẩu phải từ 8 đến 72 ký tự.")
        }
        val cleanName = displayName.trim().ifBlank { "Học sinh THCS" }
        if (!AccountInputValidator.isValidDisplayName(cleanName)) {
            return AuthOperationResult.Error("Biệt danh phải từ 2 đến 50 ký tự.")
        }
        if (birthYear != null && !AccountInputValidator.isValidBirthYear(birthYear)) {
            return AuthOperationResult.Error("Năm sinh không hợp lệ.")
        }

        val currentGrade = settingsRepository.selectedGrade.first()
        val currentSeries = settingsRepository.defaultSeries.first()
        val isMinor = AccountInputValidator.isMinorUnder16(birthYear)

        return when (
            val remote = remoteDataSource.registerWithEmail(
                email = cleanEmail,
                password = password,
                displayName = cleanName,
                birthYear = birthYear,
                isMinorUnder16 = isMinor,
                parentalConsentGranted = false,
                selectedGrade = currentGrade,
                defaultSeries = currentSeries
            )
        ) {
            is RemoteAuthResult.Success -> persistAuthenticatedSession(remote.data, fallbackName = cleanName, provider = "EMAIL")
            is RemoteAuthResult.NetworkError -> AuthOperationResult.Error(remote.message, isNetworkError = true)
            is RemoteAuthResult.Unauthorized -> AuthOperationResult.Error(remote.message)
            is RemoteAuthResult.ValidationError -> AuthOperationResult.Error(remote.message)
            is RemoteAuthResult.ServerError -> AuthOperationResult.Error(remote.message)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): AuthOperationResult {
        val cleanEmail = email.trim()
        if (!AccountInputValidator.isValidEmail(cleanEmail)) {
            return AuthOperationResult.Error("Địa chỉ email không hợp lệ.")
        }
        if (password.isBlank()) {
            return AuthOperationResult.Error("Vui lòng nhập mật khẩu.")
        }

        return when (val remote = remoteDataSource.loginWithEmail(cleanEmail, password)) {
            is RemoteAuthResult.Success -> persistAuthenticatedSession(remote.data, fallbackName = null, provider = "EMAIL")
            is RemoteAuthResult.NetworkError -> AuthOperationResult.Error(remote.message, isNetworkError = true)
            is RemoteAuthResult.Unauthorized -> AuthOperationResult.Error("Email hoặc mật khẩu chưa chính xác.")
            is RemoteAuthResult.ValidationError -> AuthOperationResult.Error(remote.message)
            is RemoteAuthResult.ServerError -> AuthOperationResult.Error(remote.message)
        }
    }

    override suspend fun requestPasswordReset(email: String): AuthOperationResult {
        val cleanEmail = email.trim()
        if (!AccountInputValidator.isValidEmail(cleanEmail)) {
            return AuthOperationResult.Error("Vui lòng nhập đúng email để nhận liên kết đặt lại mật khẩu.")
        }

        return when (val remote = remoteDataSource.requestPasswordReset(cleanEmail)) {
            is RemoteAuthResult.Success -> AuthOperationResult.Success(
                getCurrentAccount(),
                "Nếu email tồn tại trên hệ thống, liên kết đặt lại mật khẩu đã được gửi tới hộp thư của bạn."
            )
            is RemoteAuthResult.NetworkError -> AuthOperationResult.Error(remote.message, isNetworkError = true)
            is RemoteAuthResult.Unauthorized -> AuthOperationResult.Error(remote.message)
            is RemoteAuthResult.ValidationError -> AuthOperationResult.Error(remote.message)
            is RemoteAuthResult.ServerError -> AuthOperationResult.Error(remote.message)
        }
    }

    override suspend fun signInWithGoogleIdToken(
        idToken: String,
        birthYear: Int?,
        fallbackDisplayName: String?
    ): AuthOperationResult {
        if (idToken.isBlank()) {
            return AuthOperationResult.Error("Thiếu Google ID Token.")
        }

        val currentGrade = settingsRepository.selectedGrade.first()
        val currentSeries = settingsRepository.defaultSeries.first()
        val localName = preferencesManager.userName.first()
        val desiredName = fallbackDisplayName?.takeIf { it.isNotBlank() }
            ?: localName.takeIf { it.isNotBlank() }

        return when (
            val remote = remoteDataSource.signInWithGoogleIdToken(
                idToken = idToken,
                birthYear = birthYear,
                displayName = desiredName,
                selectedGrade = currentGrade,
                defaultSeries = currentSeries
            )
        ) {
            is RemoteAuthResult.Success -> persistAuthenticatedSession(remote.data, fallbackName = desiredName, provider = "GOOGLE")
            is RemoteAuthResult.NetworkError -> AuthOperationResult.Error(remote.message, isNetworkError = true)
            is RemoteAuthResult.Unauthorized -> AuthOperationResult.Error(remote.message)
            is RemoteAuthResult.ValidationError -> AuthOperationResult.Error(remote.message)
            is RemoteAuthResult.ServerError -> AuthOperationResult.Error(remote.message)
        }
    }

    private suspend fun persistAuthenticatedSession(
        authResponse: PocketBaseAuthResponse,
        fallbackName: String?,
        provider: String
    ): AuthOperationResult {
        val record = authResponse.record
        if (authResponse.token.isBlank() || record.id.isBlank()) {
            return AuthOperationResult.Error("Máy chủ trả về phiên đăng nhập không hợp lệ.")
        }

        // Lưu JWT vào Android Keystore (không lưu mật khẩu)
        tokenStorage.saveSession(authResponse.token, record.id)

        val guestEntity = accountDao.getAccountByUid(AccountEntity.GUEST_UID)
        val existingAuthEntity = accountDao.getAccountByUid(record.id)
        val now = System.currentTimeMillis()

        val resolvedName = record.displayName?.takeIf { it.isNotBlank() }
            ?: fallbackName?.takeIf { it.isNotBlank() }
            ?: existingAuthEntity?.displayName
            ?: "Học sinh THCS"

        val effectiveBirthYear = record.birthYear ?: existingAuthEntity?.birthYear ?: guestEntity?.birthYear
        val isMinor = record.isMinorUnder16 ?: AccountInputValidator.isMinorUnder16(effectiveBirthYear)
        val consentGranted = record.parentalConsentGranted ?: !isMinor
        val consentTimestamp = PocketBaseAuthRemoteDataSource.parsePocketBaseTimestamp(record.parentConsentAt)
            ?: existingAuthEntity?.parentalConsentTimestamp

        val entity = AccountEntity(
            uid = record.id,
            email = record.email.orEmpty(),
            displayName = resolvedName,
            accountMode = AccountEntity.MODE_AUTHENTICATED,
            authProvider = record.authProvider?.takeIf { it.isNotBlank() } ?: provider,
            birthYear = effectiveBirthYear,
            isMinorUnder16 = isMinor,
            parentalConsentGranted = consentGranted,
            parentEmail = record.parentEmail.orEmpty(),
            parentalConsentTimestamp = consentTimestamp,
            sessionExpired = false,
            createdAt = existingAuthEntity?.createdAt ?: now,
            updatedAt = now,
            lastSyncedAt = now
        )
        accountDao.upsertAccount(entity)
        settingsRepository.setUserName(entity.displayName)

        Log.i(TAG, "Đăng nhập PocketBase thành công: ${NetworkLogSanitizer.maskUid(entity.uid)} (${NetworkLogSanitizer.maskEmail(entity.email)})")
        val msg = if (guestEntity != null) {
            "Đã nâng cấp sang tài khoản và giữ nguyên 100% dữ liệu học tập trên máy!"
        } else {
            "Đăng nhập thành công!"
        }
        return AuthOperationResult.Success(entity.toDomain(), msg)
    }

    override suspend fun submitParentalConsent(
        birthYear: Int,
        parentEmail: String,
        currentYear: Int
    ): AuthOperationResult {
        if (!AccountInputValidator.isValidBirthYear(birthYear, currentYear)) {
            return AuthOperationResult.Error("Năm sinh không hợp lệ (ví dụ: 2012).")
        }
        val isMinor = AccountInputValidator.isMinorUnder16(birthYear, currentYear)
        if (isMinor && !AccountInputValidator.isValidEmail(parentEmail)) {
            return AuthOperationResult.Error("Vui lòng nhập email phụ huynh/người giám hộ hợp lệ.")
        }

        val active = accountDao.getActiveAccountSync()
            ?: return AuthOperationResult.Error("Vui lòng đăng nhập trước khi gửi yêu cầu xác nhận.")
        if (active.accountMode != AccountEntity.MODE_AUTHENTICATED || tokenStorage.getToken().isNullOrBlank()) {
            return AuthOperationResult.Error("Vui lòng đăng nhập lại để gửi yêu cầu xác nhận phụ huynh.")
        }

        val cleanParentEmail = if (isMinor) parentEmail.trim() else ""

        val request = when (val res = remoteDataSource.submitParentalConsent(birthYear, cleanParentEmail)) {
            is RemoteAuthResult.Success -> res.data
            is RemoteAuthResult.Unauthorized -> {
                markSessionExpired()
                return AuthOperationResult.Error(res.message)
            }
            is RemoteAuthResult.ValidationError -> return AuthOperationResult.Error(res.message)
            is RemoteAuthResult.NetworkError -> return AuthOperationResult.Error(res.message, isNetworkError = true)
            is RemoteAuthResult.ServerError -> return AuthOperationResult.Error(res.message)
        }

        val updated = active.copy(
            birthYear = birthYear,
            isMinorUnder16 = isMinor || request.isMinorUnder16,
            parentalConsentGranted = !request.verificationRequired,
            parentEmail = cleanParentEmail,
            parentalConsentTimestamp = null,
            updatedAt = System.currentTimeMillis()
        )
        accountDao.upsertAccount(updated)
        val message = if (request.verificationRequired) {
            "Đã gửi liên kết xác nhận tới email phụ huynh. Chỉ bật đồng bộ sau khi phụ huynh mở liên kết."
        } else {
            "Đã cập nhật thông tin độ tuổi."
        }
        return AuthOperationResult.Success(updated.toDomain(), message)
    }

    override suspend fun updateDisplayName(newName: String): AuthOperationResult {
        val trimmed = newName.trim()
        if (!AccountInputValidator.isValidDisplayName(trimmed)) {
            return AuthOperationResult.Error("Biệt danh phải từ 2 đến 50 ký tự.")
        }
        val active = accountDao.getActiveAccountSync() ?: AccountEntity(uid = AccountEntity.GUEST_UID)

        if (active.accountMode == AccountEntity.MODE_AUTHENTICATED && !tokenStorage.getToken().isNullOrBlank()) {
            when (val res = remoteDataSource.updateDisplayName(active.uid, trimmed)) {
                is RemoteAuthResult.Unauthorized -> markSessionExpired()
                is RemoteAuthResult.ValidationError -> return AuthOperationResult.Error(res.message)
                else -> Unit
            }
        }

        val updated = active.copy(displayName = trimmed, updatedAt = System.currentTimeMillis())
        accountDao.upsertAccount(updated)
        settingsRepository.setUserName(trimmed)
        return AuthOperationResult.Success(updated.toDomain(), "Đã cập nhật biệt danh.")
    }

    override suspend fun refreshSessionIfNeeded(): AuthAccountState {
        val local = accountDao.getActiveAccountSync() ?: return AuthAccountState()
        if (local.accountMode != AccountEntity.MODE_AUTHENTICATED) return local.toDomain()

        val token = tokenStorage.getToken()
        if (token.isNullOrBlank()) {
            val expired = local.copy(sessionExpired = true, updatedAt = System.currentTimeMillis())
            accountDao.upsertAccount(expired)
            return expired.toDomain()
        }

        return when (val res = remoteDataSource.refreshSession()) {
            is RemoteAuthResult.Success -> {
                tokenStorage.saveSession(res.data.token, res.data.record.id.ifBlank { local.uid })
                val refreshed = local.copy(
                    email = res.data.record.email ?: local.email,
                    displayName = res.data.record.displayName?.takeIf { it.isNotBlank() } ?: local.displayName,
                    birthYear = res.data.record.birthYear ?: local.birthYear,
                    isMinorUnder16 = res.data.record.isMinorUnder16 ?: local.isMinorUnder16,
                    parentalConsentGranted = res.data.record.parentalConsentGranted ?: local.parentalConsentGranted,
                    parentEmail = res.data.record.parentEmail ?: local.parentEmail,
                    parentalConsentTimestamp = PocketBaseAuthRemoteDataSource.parsePocketBaseTimestamp(res.data.record.parentConsentAt)
                        ?: local.parentalConsentTimestamp,
                    sessionExpired = false,
                    updatedAt = System.currentTimeMillis(),
                    lastSyncedAt = System.currentTimeMillis()
                )
                accountDao.upsertAccount(refreshed)
                refreshed.toDomain()
            }
            is RemoteAuthResult.Unauthorized -> {
                val expired = local.copy(sessionExpired = true, updatedAt = System.currentTimeMillis())
                accountDao.upsertAccount(expired)
                expired.toDomain()
            }
            is RemoteAuthResult.NetworkError,
            is RemoteAuthResult.ServerError,
            is RemoteAuthResult.ValidationError -> {
                // Mất mạng / server tạm nghỉ: KHÔNG đánh dấu hết phiên và KHÔNG chặn học offline
                local.toDomain()
            }
        }
    }

    override suspend fun markSessionExpired() {
        val local = accountDao.getActiveAccountSync() ?: return
        if (local.accountMode == AccountEntity.MODE_AUTHENTICATED) {
            accountDao.updateSessionExpired(local.uid, true, System.currentTimeMillis())
        }
    }

    override suspend fun signOutToGuest(): AuthOperationResult {
        val local = accountDao.getActiveAccountSync()
        // F20a: hủy token thông báo TRƯỚC khi xóa phiên (còn Authorization).
        try {
            onSessionEnding?.invoke()
        } catch (t: Throwable) {
            Log.w(TAG, "Không hủy được token thông báo khi đăng xuất: ${t.javaClass.simpleName}")
        }
        tokenStorage.clearSession()
        syncDao?.clearAllOutbox()
        if (local != null && local.uid != AccountEntity.GUEST_UID) {
            accountDao.deleteAccountByUid(local.uid)
        }
        val guest = AccountEntity(
            uid = AccountEntity.GUEST_UID,
            displayName = local?.displayName ?: "Học sinh THCS",
            accountMode = AccountEntity.MODE_GUEST,
            authProvider = AccountEntity.PROVIDER_NONE
        )
        accountDao.upsertAccount(guest)
        return AuthOperationResult.Success(guest.toDomain(), "Đã đăng xuất. Dữ liệu học tập vẫn được giữ an toàn trên máy.")
    }

    override suspend fun deleteAccount(deleteLocalData: Boolean): AuthOperationResult {
        val active = accountDao.getActiveAccountSync()
        if (active != null && active.uid != AccountEntity.GUEST_UID) {
            if (tokenStorage.getToken().isNullOrBlank()) {
                return AuthOperationResult.Error("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại trước khi xóa tài khoản vĩnh viễn.")
            }
            // F20a: hủy token thông báo trước khi xóa tài khoản (còn Authorization).
            try {
                onSessionEnding?.invoke()
            } catch (t: Throwable) {
                Log.w(TAG, "Không hủy được token thông báo khi xóa tài khoản: ${t.javaClass.simpleName}")
            }
            when (val res = remoteDataSource.deleteAccount()) {
                is RemoteAuthResult.Success -> Unit
                is RemoteAuthResult.NetworkError -> return AuthOperationResult.Error(
                    "Cần kết nối mạng để xóa tài khoản trên máy chủ.",
                    isNetworkError = true
                )
                is RemoteAuthResult.Unauthorized -> return AuthOperationResult.Error(
                    "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại rồi thử xóa tài khoản."
                )
                is RemoteAuthResult.ValidationError -> return AuthOperationResult.Error(res.message)
                is RemoteAuthResult.ServerError -> return AuthOperationResult.Error(res.message)
            }
        }

        tokenStorage.clearSession()
        accountDao.clearAllAccounts()
        syncDao?.clearAllOutbox()
        if (active != null && active.uid != AccountEntity.GUEST_UID) {
            syncDao?.deleteSyncState(active.uid)
        }
        if (deleteLocalData) {
            settingsRepository.clearAllStudyData()
        }
        val guest = AccountEntity(uid = AccountEntity.GUEST_UID)
        accountDao.upsertAccount(guest)

        val msg = if (deleteLocalData) {
            "Đã xóa tài khoản, dữ liệu máy chủ và dữ liệu cục bộ trên máy."
        } else {
            "Đã xóa tài khoản trên máy chủ. Tiến độ học cục bộ được giữ lại ở Chế độ Khách."
        }
        return AuthOperationResult.Success(guest.toDomain(), msg)
    }
}

private fun AccountEntity.toDomain(): AuthAccountState = AuthAccountState(
    uid = uid,
    email = email,
    displayName = displayName,
    mode = if (accountMode == AccountEntity.MODE_AUTHENTICATED) AccountMode.AUTHENTICATED else AccountMode.GUEST,
    authProvider = authProvider,
    birthYear = birthYear,
    isMinorUnder16 = isMinorUnder16,
    parentalConsentGranted = parentalConsentGranted,
    parentEmail = parentEmail,
    parentalConsentTimestamp = parentalConsentTimestamp,
    sessionExpired = sessionExpired,
    lastSyncedAt = lastSyncedAt
)
