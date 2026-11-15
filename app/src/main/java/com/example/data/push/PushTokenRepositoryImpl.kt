package com.example.data.push

import android.util.Log
import com.example.data.local.entity.AccountEntity
import com.example.data.remote.push.PushTokenRemoteDataSource
import com.example.data.remote.push.PushTokenRemoteResult
import com.example.domain.model.PushTokenSyncResult
import com.example.domain.repository.PushTokenRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * F20a – Triển khai [PushTokenRepository].
 *
 * Quy tắc bất biến:
 * 1. Chế độ Khách KHÔNG BAO GIỜ đăng ký token.
 * 2. Chỉ đăng ký khi người dùng đã bật thông báo đẩy và cờ tính năng đang bật.
 * 3. Đăng ký idempotent: server upsert theo token; app chỉ gọi lại khi token đổi
 *    hoặc đã quá 12 giờ.
 * 4. Đăng xuất / xóa tài khoản / tắt thông báo: báo server xóa token VÀ xóa token
 *    cục bộ, kể cả khi đang mất mạng (server sẽ tự dọn token chết khi gửi).
 */
class PushTokenRepositoryImpl(
    private val activeAccount: suspend () -> AccountEntity?,
    private val localStore: PushLocalStore,
    private val remoteDataSource: PushTokenRemoteDataSource,
    private val fcmGateway: FcmGateway,
    private val featureEnabled: Boolean,
    private val now: () -> Long = System::currentTimeMillis
) : PushTokenRepository {

    private companion object {
        const val TAG = "PushTokenRepository"
        const val PLATFORM_ANDROID = "android"
        const val MIN_TOKEN_LENGTH = 20
        const val MAX_TOKEN_LENGTH = 4096
        const val REFRESH_INTERVAL_MS = 12L * 60L * 60L * 1000L
    }

    private val mutex = Mutex()

    override suspend fun isSupported(): Boolean =
        featureEnabled && fcmGateway.isSupported()

    override suspend fun registerCurrentToken(tokenOverride: String?): PushTokenSyncResult = mutex.withLock {
        if (!featureEnabled) return@withLock PushTokenSyncResult.NO_OP_DISABLED

        val account = activeAccount()
        if (account == null || account.uid == AccountEntity.GUEST_UID ||
            account.accountMode != AccountEntity.MODE_AUTHENTICATED
        ) {
            return@withLock PushTokenSyncResult.NO_OP_GUEST
        }

        if (!localStore.pushEnabled.first()) {
            return@withLock PushTokenSyncResult.NO_OP_DISABLED
        }

        if (!fcmGateway.isSupported()) {
            return@withLock PushTokenSyncResult.UNSUPPORTED
        }

        val token = tokenOverride
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: fcmGateway.getToken()

        if (token == null || token.length < MIN_TOKEN_LENGTH || token.length > MAX_TOKEN_LENGTH) {
            return@withLock PushTokenSyncResult.MISSING_TOKEN
        }

        val previousToken = localStore.getRegisteredPushToken()
        val previousAt = localStore.getPushRegisteredAt()
        val isSameToken = token == previousToken
        if (tokenOverride == null && isSameToken && now() - previousAt < REFRESH_INTERVAL_MS) {
            // Đã đăng ký gần đây — không gọi lại server (tránh đập rate limit).
            return@withLock PushTokenSyncResult.REGISTERED
        }

        return@withLock when (val result = remoteDataSource.registerToken(token, PLATFORM_ANDROID)) {
            is PushTokenRemoteResult.Success -> {
                localStore.setRegisteredPushToken(token)
                Log.i(TAG, "Đã đăng ký token thông báo cho tài khoản hiện tại (không log token).")
                PushTokenSyncResult.REGISTERED
            }
            is PushTokenRemoteResult.NetworkError -> {
                Log.w(TAG, "Mất mạng khi đăng ký token thông báo; sẽ thử lại sau.")
                PushTokenSyncResult.NETWORK_ERROR
            }
            is PushTokenRemoteResult.Unauthorized -> PushTokenSyncResult.UNAUTHORIZED
            is PushTokenRemoteResult.ServerError -> {
                Log.w(TAG, "Server tạm lỗi khi đăng ký token thông báo.")
                PushTokenSyncResult.SERVER_ERROR
            }
        }
    }

    override suspend fun unregisterCurrentDevice(): Boolean = mutex.withLock {
        val storedToken = localStore.getRegisteredPushToken()
        val account = activeAccount()
        val isGuest = account == null || account.uid == AccountEntity.GUEST_UID ||
            account.accountMode != AccountEntity.MODE_AUTHENTICATED

        // Khách và chưa từng đăng ký token: không cần sinh token mới chỉ để xóa.
        if (storedToken == null && isGuest) return@withLock true

        val token = storedToken ?: if (fcmGateway.isSupported()) fcmGateway.getToken() else null

        var remoteSuccess = true
        if (!token.isNullOrBlank() && token.length in MIN_TOKEN_LENGTH..MAX_TOKEN_LENGTH) {
            remoteSuccess = when (val result = remoteDataSource.unregisterToken(token)) {
                is PushTokenRemoteResult.Success -> true
                else -> {
                    Log.w(TAG, "Không xóa được token trên server; đã xóa cục bộ, server sẽ tự dọn token chết.")
                    false
                }
            }
        }

        // Luôn xóa cục bộ để không lưu định danh thiết bị sau khi đăng xuất/tắt thông báo.
        localStore.clearRegisteredPushToken()
        fcmGateway.deleteToken()
        remoteSuccess
    }
}
