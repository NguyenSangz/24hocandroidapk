package com.example.push

import com.example.data.push.PushTokenManager
import com.example.domain.model.PushTokenSyncResult
import com.example.domain.repository.AccountMode
import com.example.domain.repository.AuthAccountState
import com.example.domain.repository.AuthOperationResult
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.PushTokenRepository
import com.example.fake.FakeSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * F20a – Kiểm tra thời điểm đăng ký/xóa token do PushTokenManager điều phối:
 * tắt công tắc, đăng xuất, FCM cấp token mới, và trạng thái hỗ trợ/đăng nhập.
 */
class PushTokenManagerTest {

    private class FakePushTokenRepository(
        private val supported: Boolean = true
    ) : PushTokenRepository {
        var registerCalls = 0
        var lastTokenOverride: String? = null
        var unregisterCalls = 0

        override suspend fun isSupported(): Boolean = supported

        override suspend fun registerCurrentToken(tokenOverride: String?): PushTokenSyncResult {
            registerCalls++
            lastTokenOverride = tokenOverride
            return PushTokenSyncResult.REGISTERED
        }

        override suspend fun unregisterCurrentDevice(): Boolean {
            unregisterCalls++
            return true
        }
    }

    private class FakeAuthRepository(
        authenticated: Boolean = true
    ) : AuthRepository {
        private val state = MutableStateFlow(
            if (authenticated) {
                AuthAccountState(
                    uid = "user_1",
                    email = "hocsinh@example.com",
                    mode = AccountMode.AUTHENTICATED
                )
            } else {
                AuthAccountState()
            }
        )
        override val accountState: Flow<AuthAccountState> = state.asStateFlow()

        override suspend fun getCurrentAccount(): AuthAccountState = state.value

        override suspend fun registerWithEmail(
            email: String,
            password: String,
            displayName: String,
            birthYear: Int?
        ): AuthOperationResult = throw NotImplementedError()

        override suspend fun signInWithEmail(email: String, password: String): AuthOperationResult =
            throw NotImplementedError()

        override suspend fun requestPasswordReset(email: String): AuthOperationResult =
            throw NotImplementedError()

        override suspend fun signInWithGoogleIdToken(
            idToken: String,
            birthYear: Int?,
            fallbackDisplayName: String?
        ): AuthOperationResult = throw NotImplementedError()

        override suspend fun submitParentalConsent(
            birthYear: Int,
            parentEmail: String,
            currentYear: Int
        ): AuthOperationResult = throw NotImplementedError()

        override suspend fun updateDisplayName(newName: String): AuthOperationResult =
            throw NotImplementedError()

        override suspend fun refreshSessionIfNeeded(): AuthAccountState = state.value

        override suspend fun markSessionExpired() {}

        override suspend fun signOutToGuest(): AuthOperationResult = throw NotImplementedError()

        override suspend fun deleteAccount(deleteLocalData: Boolean): AuthOperationResult =
            throw NotImplementedError()
    }

    @Test
    fun setEnabledFalse_unregistersImmediately() = runTest {
        val repo = FakePushTokenRepository()
        val settings = FakeSettingsRepository()
        val manager = PushTokenManager(repo, FakeAuthRepository(), settings)

        manager.setEnabled(false)

        assertEquals(1, repo.unregisterCalls)
        assertEquals(0, repo.registerCalls)
        assertFalse(settings.pushEnabled.first())
    }

    @Test
    fun setEnabledTrue_registersToken() = runTest {
        val repo = FakePushTokenRepository()
        val settings = FakeSettingsRepository()
        val manager = PushTokenManager(repo, FakeAuthRepository(), settings)

        manager.setEnabled(true)

        assertEquals(1, repo.registerCalls)
        assertEquals(0, repo.unregisterCalls)
        assertTrue(settings.pushEnabled.first())
    }

    @Test
    fun onNewToken_registersWithOverrideToken() = runTest {
        val repo = FakePushTokenRepository()
        val manager = PushTokenManager(repo, FakeAuthRepository(), FakeSettingsRepository())

        manager.onNewToken("fcm_token_new_value_1234567890")

        assertEquals(1, repo.registerCalls)
        assertEquals("fcm_token_new_value_1234567890", repo.lastTokenOverride)
    }

    @Test
    fun onSessionEnding_unregistersBeforeSessionCleared() = runTest {
        val repo = FakePushTokenRepository()
        val manager = PushTokenManager(repo, FakeAuthRepository(), FakeSettingsRepository())

        manager.onSessionEnding()

        assertEquals(1, repo.unregisterCalls)
        assertEquals(0, repo.registerCalls)
    }

    @Test
    fun refresh_reflectsSupportAndSignInState() = runTest {
        val repo = FakePushTokenRepository(supported = true)
        val manager = PushTokenManager(repo, FakeAuthRepository(authenticated = true), FakeSettingsRepository())

        manager.refresh()

        assertTrue(manager.isSupported.value)
        assertTrue(manager.isSignedIn.value)
    }

    @Test
    fun refresh_guestDevice_isNotSupportedWhenFcmMissing() = runTest {
        val repo = FakePushTokenRepository(supported = false)
        val manager = PushTokenManager(repo, FakeAuthRepository(authenticated = false), FakeSettingsRepository())

        manager.refresh()

        assertFalse(manager.isSupported.value)
        assertFalse(manager.isSignedIn.value)
    }
}
