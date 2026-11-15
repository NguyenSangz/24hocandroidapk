package com.example.data.push

import android.util.Log
import com.example.domain.repository.AccountMode
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.PushSettingsController
import com.example.domain.repository.PushTokenRepository
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * F20a – Điều phối vòng đời token thông báo đẩy.
 *
 * Ứng dụng:
 * - Khi đăng nhập + bật thông báo: đăng ký token.
 * - Khi đăng xuất/xóa tài khoản: [onSessionEnding] được AuthRepository gọi TRƯỚC khi
 *   xóa phiên để còn quyền báo server xóa token.
 * - Khi tắt công tắc: xóa token ngay.
 * - Khi FCM cấp token mới: đăng ký lại nếu đủ điều kiện.
 *
 * Khách không đăng ký token (repository tự chặn).
 */
class PushTokenManager(
    private val repository: PushTokenRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) : PushSettingsController {

    private companion object {
        const val TAG = "PushTokenManager"
    }

    private val _isSupported = MutableStateFlow(false)
    override val isSupported: StateFlow<Boolean> = _isSupported.asStateFlow()

    private val _isSignedIn = MutableStateFlow(false)
    override val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private var started = false

    /** Bắt đầu quan sát; gọi một lần trong Application.onCreate. An toàn khi gọi lặp. */
    fun start(scope: CoroutineScope) {
        if (started) return
        started = true

        scope.launch { refresh() }

        // Đăng nhập/đăng xuất: đồng bộ token tương ứng.
        scope.launch {
            authRepository.accountState.collect { state ->
                val signedIn = state.mode == AccountMode.AUTHENTICATED
                _isSignedIn.value = signedIn
                if (signedIn && settingsRepository.pushEnabled.first()) {
                    repository.registerCurrentToken()
                } else if (!signedIn) {
                    // Dọn token sót lại sau khi đăng xuất (thường đã được xóa trước đó).
                    repository.unregisterCurrentDevice()
                }
            }
        }

        // Bật/tắt công tắc: đăng ký hoặc xóa token ngay.
        scope.launch {
            settingsRepository.pushEnabled.collect { enabled ->
                if (enabled) {
                    if (_isSignedIn.value) repository.registerCurrentToken()
                } else {
                    repository.unregisterCurrentDevice()
                }
            }
        }
    }

    override suspend fun refresh() {
        _isSupported.value = try {
            repository.isSupported()
        } catch (t: Throwable) {
            Log.w(TAG, "Không kiểm tra được hỗ trợ thông báo đẩy: ${t.javaClass.simpleName}")
            false
        }
        _isSignedIn.value = try {
            authRepository.getCurrentAccount().mode == AccountMode.AUTHENTICATED
        } catch (t: Throwable) {
            false
        }
    }

    override suspend fun setEnabled(enabled: Boolean) {
        settingsRepository.setPushEnabled(enabled)
        if (enabled) {
            repository.registerCurrentToken()
        } else {
            repository.unregisterCurrentDevice()
        }
    }

    /** FCM cấp token mới. */
    suspend fun onNewToken(token: String) {
        repository.registerCurrentToken(tokenOverride = token)
    }

    /**
     * Gọi TRƯỚC khi xóa phiên đăng nhập để còn Authorization gọi server.
     */
    suspend fun onSessionEnding() {
        try {
            repository.unregisterCurrentDevice()
        } catch (t: Throwable) {
            Log.w(TAG, "Không hủy đăng ký token khi kết thúc phiên: ${t.javaClass.simpleName}")
        }
    }
}
