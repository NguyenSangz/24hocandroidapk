package com.example.push

import com.example.data.local.entity.AccountEntity
import com.example.data.push.FcmGateway
import com.example.data.push.PushLocalStore
import com.example.data.push.PushTokenRepositoryImpl
import com.example.data.remote.push.PushTokenRemoteDataSource
import com.example.data.remote.push.PushTokenRemoteResult
import com.example.domain.model.PushTokenSyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * F20a – Kiểm tra logic đăng ký/xóa token:
 * - Khách không bao giờ đăng ký.
 * - Công tắc tắt/không hỗ trợ thì không gọi server.
 * - Đăng ký lặp lại cùng token không tạo bản ghi trùng.
 * - Đăng xuất/tắt thông báo xóa token cả server lẫn cục bộ.
 */
class PushTokenRepositoryTest {

    private class FakeLocalStore(
        enabled: Boolean = true,
        initialToken: String? = null,
        initialAt: Long = 0L
    ) : PushLocalStore {
        private val _enabled = MutableStateFlow(enabled)
        override val pushEnabled: Flow<Boolean> = _enabled.asStateFlow()
        var token: String? = initialToken
        var registeredAt: Long = initialAt

        override suspend fun getRegisteredPushToken(): String? = token
        override suspend fun getPushRegisteredAt(): Long = registeredAt
        override suspend fun setRegisteredPushToken(token: String) {
            this.token = token
            this.registeredAt = 1_000_000L
        }
        override suspend fun clearRegisteredPushToken() {
            token = null
            registeredAt = 0L
        }
    }

    private class FakeFcmGateway(
        private val supported: Boolean = true,
        var currentToken: String? = VALID_TOKEN
    ) : FcmGateway {
        var deleteTokenCalls = 0
        override fun isSupported(): Boolean = supported
        override suspend fun getToken(): String? = currentToken
        override suspend fun deleteToken() {
            deleteTokenCalls++
            currentToken = null
        }
    }

    private class FakeRemote : PushTokenRemoteDataSource {
        val registered = mutableListOf<String>()
        val unregistered = mutableListOf<String>()
        var registerResult: PushTokenRemoteResult = PushTokenRemoteResult.Success
        var unregisterResult: PushTokenRemoteResult = PushTokenRemoteResult.Success

        override suspend fun registerToken(token: String, platform: String): PushTokenRemoteResult {
            registered += token
            return registerResult
        }

        override suspend fun unregisterToken(token: String): PushTokenRemoteResult {
            unregistered += token
            return unregisterResult
        }
    }

    private companion object {
        const val VALID_TOKEN = "fcm_token_abcdefghijklmnopqrstuvwxyz_123456"
        val AUTHENTICATED = AccountEntity(
            uid = "user_1",
            email = "hocsinh@example.com",
            accountMode = AccountEntity.MODE_AUTHENTICATED
        )
        val GUEST = AccountEntity(uid = AccountEntity.GUEST_UID, accountMode = AccountEntity.MODE_GUEST)
    }

    private fun repository(
        account: AccountEntity?,
        store: FakeLocalStore,
        remote: FakeRemote,
        fcm: FakeFcmGateway,
        featureEnabled: Boolean = true,
        now: () -> Long = { 1_000_000L }
    ) = PushTokenRepositoryImpl(
        activeAccount = { account },
        localStore = store,
        remoteDataSource = remote,
        fcmGateway = fcm,
        featureEnabled = featureEnabled,
        now = now
    )

    @Test
    fun guest_neverRegistersToken() = runTest {
        val store = FakeLocalStore(enabled = true)
        val remote = FakeRemote()
        val repo = repository(GUEST, store, remote, FakeFcmGateway())

        val result = repo.registerCurrentToken()

        assertEquals(PushTokenSyncResult.NO_OP_GUEST, result)
        assertTrue(remote.registered.isEmpty())
        assertNull(store.token)
    }

    @Test
    fun disabledFeature_doesNothing() = runTest {
        val store = FakeLocalStore(enabled = true)
        val remote = FakeRemote()
        val repo = repository(AUTHENTICATED, store, remote, FakeFcmGateway(), featureEnabled = false)

        assertEquals(PushTokenSyncResult.NO_OP_DISABLED, repo.registerCurrentToken())
        assertFalse(repo.isSupported())
        assertTrue(remote.registered.isEmpty())
    }

    @Test
    fun userTurnedOffPush_doesNotRegister() = runTest {
        val store = FakeLocalStore(enabled = false)
        val remote = FakeRemote()
        val repo = repository(AUTHENTICATED, store, remote, FakeFcmGateway())

        assertEquals(PushTokenSyncResult.NO_OP_DISABLED, repo.registerCurrentToken())
        assertTrue(remote.registered.isEmpty())
    }

    @Test
    fun authenticatedAndEnabled_registersTokenAndStoresIt() = runTest {
        val store = FakeLocalStore(enabled = true)
        val remote = FakeRemote()
        val repo = repository(AUTHENTICATED, store, remote, FakeFcmGateway())

        val result = repo.registerCurrentToken()

        assertEquals(PushTokenSyncResult.REGISTERED, result)
        assertEquals(listOf(VALID_TOKEN), remote.registered)
        assertEquals(VALID_TOKEN, store.token)
    }

    @Test
    fun sameTokenWithin12h_doesNotCallServerAgain() = runTest {
        val store = FakeLocalStore(enabled = true, initialToken = VALID_TOKEN, initialAt = 900_000L)
        val remote = FakeRemote()
        val repo = repository(AUTHENTICATED, store, remote, FakeFcmGateway(), now = { 1_000_000L })

        val result = repo.registerCurrentToken()

        assertEquals(PushTokenSyncResult.REGISTERED, result)
        assertTrue("Không được gọi server lặp lại với cùng token", remote.registered.isEmpty())
    }

    @Test
    fun newTokenOverride_forcesReRegistration() = runTest {
        val store = FakeLocalStore(enabled = true, initialToken = VALID_TOKEN, initialAt = 900_000L)
        val remote = FakeRemote()
        val repo = repository(AUTHENTICATED, store, remote, FakeFcmGateway(), now = { 1_000_000L })

        val newToken = "fcm_token_NEW_abcdefghijklmnopqrstuvwxyz_987654"
        val result = repo.registerCurrentToken(tokenOverride = newToken)

        assertEquals(PushTokenSyncResult.REGISTERED, result)
        assertEquals(listOf(newToken), remote.registered)
        assertEquals(newToken, store.token)
    }

    @Test
    fun networkError_keepsLocalTokenEmptyForRetry() = runTest {
        val store = FakeLocalStore(enabled = true)
        val remote = FakeRemote().apply { registerResult = PushTokenRemoteResult.NetworkError("offline") }
        val repo = repository(AUTHENTICATED, store, remote, FakeFcmGateway())

        val result = repo.registerCurrentToken()

        assertEquals(PushTokenSyncResult.NETWORK_ERROR, result)
        assertNull(store.token)
    }

    @Test
    fun unregister_signOut_removesServerAndLocalToken() = runTest {
        val store = FakeLocalStore(enabled = true, initialToken = VALID_TOKEN, initialAt = 900_000L)
        val remote = FakeRemote()
        val fcm = FakeFcmGateway()
        val repo = repository(GUEST, store, remote, fcm)

        val ok = repo.unregisterCurrentDevice()

        assertTrue(ok)
        assertEquals(listOf(VALID_TOKEN), remote.unregistered)
        assertNull(store.token)
        assertEquals(1, fcm.deleteTokenCalls)
    }

    @Test
    fun unregister_guestWithoutToken_doesNotCreateToken() = runTest {
        val store = FakeLocalStore(enabled = false)
        val remote = FakeRemote()
        val fcm = FakeFcmGateway()
        val repo = repository(GUEST, store, remote, fcm)

        assertTrue(repo.unregisterCurrentDevice())
        assertTrue(remote.unregistered.isEmpty())
        assertEquals(0, fcm.deleteTokenCalls)
    }

    @Test
    fun unregister_serverFailure_stillClearsLocalToken() = runTest {
        val store = FakeLocalStore(enabled = true, initialToken = VALID_TOKEN, initialAt = 900_000L)
        val remote = FakeRemote().apply { unregisterResult = PushTokenRemoteResult.NetworkError("offline") }
        val fcm = FakeFcmGateway()
        val repo = repository(GUEST, store, remote, fcm)

        val ok = repo.unregisterCurrentDevice()

        assertFalse(ok)
        assertNull(store.token)
        assertEquals(1, fcm.deleteTokenCalls)
    }
}
