package com.example.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FakeSyncRepository
import com.example.domain.repository.AccountInputValidator
import com.example.domain.repository.AuthAccountState
import com.example.domain.repository.AuthOperationResult
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.SyncOperationResult
import com.example.domain.repository.SyncRepository
import com.example.domain.repository.SyncUiSummary
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Trạng thái UI màn hình Tài khoản (F16a - PocketBase Self-Hosted Offline-First):
 * Hỗ trợ Chế độ Khách mặc định, Email/Mật khẩu + Quên mật khẩu (SMTP),
 * Google Sign-In (Credential Manager -> PocketBase hook),
 * Cổng xác nhận phụ huynh (< 16 tuổi) và Xóa tài khoản 2 bước.
 */
data class AccountUiState(
    val isLoading: Boolean = false,
    val isOfflineBannerVisible: Boolean = false,
    val errorMessage: String? = null,
    val isRegisterMode: Boolean = false,
    val emailInput: String = "",
    val passwordInput: String = "",
    val registerNicknameInput: String = "",
    val showForgotPasswordDialog: Boolean = false,
    val resetEmailInput: String = "",
    val birthYearInput: String = "2012",
    val parentEmailInput: String = "",
    val parentConsentChecked: Boolean = false,
    val parentMathChallengeAnswer: String = "",
    val parentConsentRequestPending: Boolean = false,
    val showParentalConsentDialog: Boolean = false,
    val nicknameInput: String = "",
    val deleteStep: Int = 0, // 0: đóng, 1: chọn giữ/xóa dữ liệu máy, 2: xác nhận cuối
    val deleteLocalDataChoice: Boolean = false
)

class AccountViewModel(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository = FakeSyncRepository()
) : ViewModel() {

    val accountState: StateFlow<AuthAccountState> = authRepository.accountState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = AuthAccountState()
        )

    val syncSummary: StateFlow<SyncUiSummary> = syncRepository.syncSummary
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = SyncUiSummary()
        )

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    // Phép nhân đơn giản cho cổng kiểm tra người lớn (Parental Gate)
    val challengeFactorA = 7
    val challengeFactorB = 8
    val expectedChallengeAnswer = 56

    init {
        refreshSession()
    }

    fun refreshSession() {
        viewModelScope.launch {
            val state = authRepository.refreshSessionIfNeeded()
            if (state.displayName.isNotBlank() && _uiState.value.nicknameInput.isBlank()) {
                _uiState.update { it.copy(nicknameInput = state.displayName) }
            }
            state.birthYear?.let { year ->
                _uiState.update { it.copy(birthYearInput = year.toString()) }
            }
        }
    }

    fun checkParentalConsentStatus() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val state = authRepository.refreshSessionIfNeeded()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    parentConsentRequestPending = state.needsParentalConsent,
                    showParentalConsentDialog = if (state.parentalConsentGranted) {
                        false
                    } else {
                        it.showParentalConsentDialog
                    },
                    errorMessage = null
                )
            }
            if (state.parentalConsentGranted) {
                _events.emit("Đã xác nhận. Bạn có thể tiếp tục chọn lớp.")
                syncRepository.syncNow(manual = false)
            } else {
                _events.emit("Chưa thấy xác nhận của phụ huynh. Hãy mở liên kết trong email rồi thử lại.")
            }
        }
    }

    fun toggleRegisterMode(registerMode: Boolean) {
        _uiState.update { it.copy(isRegisterMode = registerMode, errorMessage = null) }
    }

    fun updateEmailInput(value: String) {
        _uiState.update { it.copy(emailInput = value, errorMessage = null) }
    }

    fun updatePasswordInput(value: String) {
        _uiState.update { it.copy(passwordInput = value, errorMessage = null) }
    }

    fun updateRegisterNicknameInput(value: String) {
        _uiState.update { it.copy(registerNicknameInput = value, errorMessage = null) }
    }

    fun updateBirthYearInput(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(4)
        _uiState.update { it.copy(birthYearInput = digitsOnly, errorMessage = null) }
    }

    fun updateParentEmailInput(value: String) {
        _uiState.update { it.copy(parentEmailInput = value, errorMessage = null) }
    }

    fun updateParentConsentChecked(checked: Boolean) {
        _uiState.update { it.copy(parentConsentChecked = checked, errorMessage = null) }
    }

    fun updateParentMathAnswer(value: String) {
        _uiState.update { it.copy(parentMathChallengeAnswer = value.filter { ch -> ch.isDigit() }.take(4), errorMessage = null) }
    }

    fun updateNicknameInput(value: String) {
        _uiState.update { it.copy(nicknameInput = value, errorMessage = null) }
    }

    fun openForgotPasswordDialog() {
        _uiState.update {
            it.copy(
                showForgotPasswordDialog = true,
                resetEmailInput = it.emailInput,
                errorMessage = null
            )
        }
    }

    fun dismissForgotPasswordDialog() {
        _uiState.update { it.copy(showForgotPasswordDialog = false) }
    }

    fun updateResetEmailInput(value: String) {
        _uiState.update { it.copy(resetEmailInput = value, errorMessage = null) }
    }

    fun submitPasswordReset() {
        if (_uiState.value.isLoading) return
        val email = _uiState.value.resetEmailInput
        if (!AccountInputValidator.isValidEmail(email)) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập email hợp lệ để đặt lại mật khẩu.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = authRepository.requestPasswordReset(email)) {
                is AuthOperationResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, showForgotPasswordDialog = false) }
                    _events.emit(res.message)
                }
                is AuthOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOfflineBannerVisible = res.isNetworkError || it.isOfflineBannerVisible,
                            errorMessage = res.message
                        )
                    }
                }
            }
        }
    }

    fun submitEmailAuth() {
        if (_uiState.value.isLoading) return
        val state = _uiState.value
        val email = state.emailInput.trim()
        val password = state.passwordInput

        if (!AccountInputValidator.isValidEmail(email)) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập địa chỉ email hợp lệ.") }
            return
        }
        if (!AccountInputValidator.isValidPassword(password)) {
            _uiState.update { it.copy(errorMessage = "Mật khẩu phải từ 8 đến 72 ký tự.") }
            return
        }

        if (state.isRegisterMode) {
            val parsedYear = state.birthYearInput.toIntOrNull()
            if (!AccountInputValidator.isValidBirthYear(parsedYear)) {
                _uiState.update { it.copy(errorMessage = "Vui lòng nhập năm sinh hợp lệ (ví dụ: 2012).") }
                return
            }
            val nickname = state.registerNicknameInput.trim().ifBlank { "Học sinh THCS" }
            if (!AccountInputValidator.isValidDisplayName(nickname)) {
                _uiState.update { it.copy(errorMessage = "Biệt danh phải từ 2 đến 50 ký tự.") }
                return
            }
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            viewModelScope.launch {
                when (val res = authRepository.registerWithEmail(email, password, nickname, parsedYear)) {
                    is AuthOperationResult.Success -> {
                        val acc = res.account
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                passwordInput = "",
                                nicknameInput = acc.displayName,
                                showParentalConsentDialog = acc.needsParentalConsent,
                                errorMessage = null
                            )
                        }
                        _events.emit(res.message)
                        viewModelScope.launch { syncRepository.syncNow(manual = false) }
                    }
                    is AuthOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isOfflineBannerVisible = res.isNetworkError || it.isOfflineBannerVisible,
                                errorMessage = res.message
                            )
                        }
                    }
                }
            }
        } else {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            viewModelScope.launch {
                when (val res = authRepository.signInWithEmail(email, password)) {
                    is AuthOperationResult.Success -> {
                        val acc = res.account
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                passwordInput = "",
                                nicknameInput = acc.displayName,
                                showParentalConsentDialog = acc.needsParentalConsent,
                                errorMessage = null
                            )
                        }
                        _events.emit(res.message)
                        viewModelScope.launch { syncRepository.syncNow(manual = false) }
                    }
                    is AuthOperationResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isOfflineBannerVisible = res.isNetworkError || it.isOfflineBannerVisible,
                                errorMessage = res.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun setOfflineBanner(isOffline: Boolean) {
        _uiState.update { it.copy(isOfflineBannerVisible = isOffline) }
    }

    fun beginLoading() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
    }

    fun cancelLoadingWithMessage(message: String? = null) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onGoogleIdTokenReceived(idToken: String, displayName: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val parsedYear = _uiState.value.birthYearInput.toIntOrNull()
            val validYear = if (AccountInputValidator.isValidBirthYear(parsedYear)) parsedYear else null

            when (val result = authRepository.signInWithGoogleIdToken(idToken, validYear, displayName)) {
                is AuthOperationResult.Success -> {
                    val acc = result.account
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            passwordInput = "",
                            nicknameInput = acc.displayName,
                            showParentalConsentDialog = acc.needsParentalConsent,
                            errorMessage = null
                        )
                    }
                    _events.emit(result.message)
                    viewModelScope.launch { syncRepository.syncNow(manual = false) }
                }
                is AuthOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOfflineBannerVisible = result.isNetworkError || it.isOfflineBannerVisible,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun openParentalConsentDialog() {
        _uiState.update { it.copy(showParentalConsentDialog = true, errorMessage = null) }
    }

    fun dismissParentalConsentDialog() {
        _uiState.update { it.copy(showParentalConsentDialog = false, errorMessage = null) }
    }

    fun submitParentalConsent(
        currentYear: Int = java.time.Year.now(java.time.ZoneOffset.UTC).value
    ) {
        if (_uiState.value.isLoading) return
        val state = _uiState.value
        val birthYear = state.birthYearInput.toIntOrNull()
        if (!AccountInputValidator.isValidBirthYear(birthYear, currentYear)) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập năm sinh hợp lệ (ví dụ: 2012).") }
            return
        }
        val isMinor = AccountInputValidator.isMinorUnder16(birthYear, currentYear)
        if (isMinor) {
            if (!AccountInputValidator.isValidEmail(state.parentEmailInput)) {
                _uiState.update { it.copy(errorMessage = "Học sinh dưới 16 tuổi cần nhập email phụ huynh/người giám hộ hợp lệ.") }
                return
            }
            if (!state.parentConsentChecked) {
                _uiState.update { it.copy(errorMessage = "Vui lòng đánh dấu ô xác nhận có sự đồng ý của cha mẹ/người giám hộ.") }
                return
            }
            if (state.parentMathChallengeAnswer.toIntOrNull() != expectedChallengeAnswer) {
                _uiState.update { it.copy(errorMessage = "Câu trả lời kiểm tra phụ huynh ($challengeFactorA × $challengeFactorB) chưa chính xác.") }
                return
            }
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val res = authRepository.submitParentalConsent(birthYear!!, state.parentEmailInput, currentYear)) {
                is AuthOperationResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            parentConsentRequestPending = res.account.needsParentalConsent,
                            errorMessage = null
                        )
                    }
                    _events.emit(res.message)
                }
                is AuthOperationResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
            }
        }
    }

    fun saveNickname() {
        if (_uiState.value.isLoading) return
        val name = _uiState.value.nicknameInput
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = authRepository.updateDisplayName(name)) {
                is AuthOperationResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, nicknameInput = res.account.displayName) }
                    _events.emit(res.message)
                }
                is AuthOperationResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
            }
        }
    }

    fun signOutToGuest() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = authRepository.signOutToGuest()) {
                is AuthOperationResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, passwordInput = "") }
                    _events.emit(res.message)
                }
                is AuthOperationResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
            }
        }
    }

    fun startDeleteAccountFlow() {
        _uiState.update { it.copy(deleteStep = 1, deleteLocalDataChoice = false, errorMessage = null) }
    }

    fun setDeleteLocalDataChoice(deleteLocal: Boolean) {
        _uiState.update { it.copy(deleteLocalDataChoice = deleteLocal) }
    }

    fun proceedToFinalDeleteConfirmation() {
        _uiState.update { it.copy(deleteStep = 2) }
    }

    fun cancelDeleteAccountFlow() {
        _uiState.update { it.copy(deleteStep = 0) }
    }

    fun confirmDeleteAccount() {
        if (_uiState.value.isLoading) return
        val deleteLocal = _uiState.value.deleteLocalDataChoice
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, deleteStep = 0, errorMessage = null) }
            when (val res = authRepository.deleteAccount(deleteLocal)) {
                is AuthOperationResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, passwordInput = "") }
                    _events.emit(res.message)
                }
                is AuthOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOfflineBannerVisible = res.isNetworkError || it.isOfflineBannerVisible,
                            errorMessage = res.message
                        )
                    }
                }
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            when (val res = syncRepository.syncNow(manual = true)) {
                is SyncOperationResult.Success -> _events.emit(res.message)
                is SyncOperationResult.Skipped -> _events.emit(res.message)
                is SyncOperationResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isOfflineBannerVisible = res.isNetworkError || it.isOfflineBannerVisible
                        )
                    }
                    _events.emit(res.message)
                }
            }
        }
    }

    fun setSyncWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            syncRepository.setSyncWifiOnly(wifiOnly)
            _events.emit(
                if (wifiOnly) "Đã bật: Chỉ đồng bộ khi có Wi-Fi."
                else "Đã tắt giới hạn Wi-Fi: Cho phép đồng bộ qua dữ liệu di động."
            )
        }
    }

    fun retryFailedSync() {
        viewModelScope.launch {
            when (val res = syncRepository.retryFailedItems()) {
                is SyncOperationResult.Success -> _events.emit(res.message)
                is SyncOperationResult.Skipped -> _events.emit(res.message)
                is SyncOperationResult.Error -> _events.emit(res.message)
            }
        }
    }
}
