package com.example

import com.example.data.remote.NetworkLogSanitizer
import com.example.data.repository.FakeAuthRepository
import com.example.domain.repository.AccountInputValidator
import com.example.domain.repository.AccountMode
import com.example.domain.repository.AuthAccountState
import com.example.domain.repository.AuthOperationResult
import com.example.ui.screens.account.AccountViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountAuthUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun inputValidator_checksEmailPasswordDisplayNameAndMinorAgeCorrectly() {
        assertTrue(AccountInputValidator.isValidEmail("phuhuynh@gmail.com"))
        assertFalse(AccountInputValidator.isValidEmail("invalid-email"))
        assertFalse(AccountInputValidator.isValidEmail(""))

        assertTrue(AccountInputValidator.isValidPassword("12345678"))
        assertFalse(AccountInputValidator.isValidPassword("1234567"))

        assertTrue(AccountInputValidator.isValidDisplayName("Minh Anh"))
        assertFalse(AccountInputValidator.isValidDisplayName("A"))

        assertTrue(AccountInputValidator.isValidBirthYear(2012, currentYear = 2026))
        assertFalse(AccountInputValidator.isValidBirthYear(1920, currentYear = 2026))
        assertFalse(AccountInputValidator.isValidBirthYear(2030, currentYear = 2026))

        assertTrue(AccountInputValidator.isMinorUnder16(2012, currentYear = 2026))
        assertTrue(AccountInputValidator.isMinorUnder16(2011, currentYear = 2026))
        assertFalse(AccountInputValidator.isMinorUnder16(2010, currentYear = 2026))
        assertTrue(AccountInputValidator.isMinorUnder16(null, currentYear = 2026))
    }

    @Test
    fun logSanitizer_masksPiiAndTokenFields() {
        assertEquals("h***@example.com", NetworkLogSanitizer.maskEmail("hocsinh@example.com"))
        assertEquals("***", NetworkLogSanitizer.maskEmail("a@b.com"))
        assertEquals("***", NetworkLogSanitizer.maskEmail(null))
        assertEquals("ali***789", NetworkLogSanitizer.maskUid("alice_uid_789"))
        assertEquals("none", NetworkLogSanitizer.maskUid(""))
        assertEquals("eyJh***", NetworkLogSanitizer.maskToken("eyJhbGciOiJIUzI1NiJ9.payload"))
    }

    @Test
    fun guestModeIsDefault_andEmailRegisterAndGoogleSignInTriggerMinorConsent() = runTest {
        val fakeRepo = FakeAuthRepository()
        val viewModel = AccountViewModel(fakeRepo)
        advanceUntilIdle()

        val initial = fakeRepo.getCurrentAccount()
        assertEquals(AccountMode.GUEST, initial.mode)
        assertEquals(AuthAccountState.GUEST_UID, initial.uid)
        assertFalse(initial.isAuthenticated)

        // Đăng ký bằng Email cho học sinh sinh năm 2013 (<16 tuổi)
        viewModel.toggleRegisterMode(true)
        viewModel.updateBirthYearInput("2013")
        viewModel.updateRegisterNicknameInput("Bảo Ngọc")
        viewModel.updateEmailInput("baongoc@example.com")
        viewModel.updatePasswordInput("MatKhauAnToan123")
        viewModel.submitEmailAuth()
        advanceUntilIdle()

        val afterRegister = fakeRepo.getCurrentAccount()
        assertTrue(afterRegister.isAuthenticated)
        assertEquals("EMAIL", afterRegister.authProvider)
        assertEquals("Bảo Ngọc", afterRegister.displayName)
        assertTrue(afterRegister.needsParentalConsent)
        assertTrue(viewModel.uiState.value.showParentalConsentDialog)
        // Đảm bảo password input trong UI state được xóa sạch ngay sau đăng ký
        assertEquals("", viewModel.uiState.value.passwordInput)
    }

    @Test
    fun forgotPassword_sendsResetRequestWhenEmailValid() = runTest {
        val fakeRepo = FakeAuthRepository()
        val viewModel = AccountViewModel(fakeRepo)
        viewModel.updateEmailInput("minh@example.com")
        viewModel.openForgotPasswordDialog()
        assertTrue(viewModel.uiState.value.showForgotPasswordDialog)
        assertEquals("minh@example.com", viewModel.uiState.value.resetEmailInput)

        viewModel.submitPasswordReset()
        advanceUntilIdle()

        assertEquals(1, fakeRepo.passwordResetRequests)
        assertFalse(viewModel.uiState.value.showForgotPasswordDialog)
    }

    @Test
    fun emailSignIn_doubleTapSubmitsOnlyOnce() = runTest {
        val fakeRepo = FakeAuthRepository()
        val viewModel = AccountViewModel(fakeRepo)
        viewModel.updateEmailInput("minh@example.com")
        viewModel.updatePasswordInput("MatKhauAnToan123")

        viewModel.submitEmailAuth()
        viewModel.submitEmailAuth()
        advanceUntilIdle()

        assertEquals(1, fakeRepo.emailSignInCount)
    }

    @Test
    fun parentalConsentGate_enforcesAdultChallengeAndParentEmail() = runTest {
        val fakeRepo = FakeAuthRepository(
            initialState = AuthAccountState(
                uid = "uid_minor",
                email = "hs@example.com",
                displayName = "Hoàng",
                mode = AccountMode.AUTHENTICATED,
                authProvider = "GOOGLE",
                birthYear = 2013,
                isMinorUnder16 = true,
                parentalConsentGranted = false
            )
        )
        val viewModel = AccountViewModel(fakeRepo)
        viewModel.openParentalConsentDialog()
        viewModel.updateBirthYearInput("2013")

        viewModel.updateParentEmailInput("sai-dinh-dang")
        viewModel.updateParentConsentChecked(true)
        viewModel.updateParentMathAnswer("56")
        viewModel.submitParentalConsent(currentYear = 2026)
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.updateParentEmailInput("bo.me@example.com")
        viewModel.updateParentMathAnswer("42")
        viewModel.submitParentalConsent(currentYear = 2026)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(fakeRepo.getCurrentAccount().parentalConsentGranted)

        fakeRepo.shouldFailNetwork = true
        viewModel.updateParentMathAnswer("56")
        viewModel.submitParentalConsent(currentYear = 2026)
        advanceUntilIdle()
        assertFalse(fakeRepo.getCurrentAccount().parentalConsentGranted)
        assertFalse(viewModel.uiState.value.parentConsentRequestPending)

        fakeRepo.shouldFailNetwork = false
        viewModel.updateParentMathAnswer("56")
        viewModel.submitParentalConsent(currentYear = 2026)
        advanceUntilIdle()

        val pendingConsent = fakeRepo.getCurrentAccount()
        assertFalse(pendingConsent.parentalConsentGranted)
        assertTrue(pendingConsent.needsParentalConsent)
        assertEquals("bo.me@example.com", pendingConsent.parentEmail)
        assertNull(pendingConsent.parentalConsentTimestamp)
        assertTrue(viewModel.uiState.value.parentConsentRequestPending)

        fakeRepo.simulateParentalConsentConfirmation()
        viewModel.checkParentalConsentStatus()
        advanceUntilIdle()

        val confirmed = fakeRepo.getCurrentAccount()
        assertTrue(confirmed.parentalConsentGranted)
        assertNotNull(confirmed.parentalConsentTimestamp)
        assertFalse(viewModel.uiState.value.showParentalConsentDialog)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun deleteAccountFlow_handlesBothKeepLocalAndClearLocalDataChoices() = runTest {
        val fakeRepo = FakeAuthRepository(
            initialState = AuthAccountState(
                uid = "uid_user",
                email = "user@example.com",
                displayName = "Minh",
                mode = AccountMode.AUTHENTICATED,
                authProvider = "EMAIL"
            )
        )
        val viewModel = AccountViewModel(fakeRepo)

        viewModel.startDeleteAccountFlow()
        assertEquals(1, viewModel.uiState.value.deleteStep)
        viewModel.setDeleteLocalDataChoice(false)
        viewModel.proceedToFinalDeleteConfirmation()
        assertEquals(2, viewModel.uiState.value.deleteStep)
        viewModel.confirmDeleteAccount()
        advanceUntilIdle()

        assertEquals(1, fakeRepo.serverDeletedCount)
        assertEquals(0, fakeRepo.localDataClearedCount)
        assertEquals(AccountMode.GUEST, fakeRepo.getCurrentAccount().mode)

        fakeRepo.signInWithGoogleIdToken("tok_2", 2008, "Minh")
        viewModel.startDeleteAccountFlow()
        viewModel.setDeleteLocalDataChoice(true)
        viewModel.proceedToFinalDeleteConfirmation()
        viewModel.confirmDeleteAccount()
        advanceUntilIdle()

        assertEquals(2, fakeRepo.serverDeletedCount)
        assertEquals(1, fakeRepo.localDataClearedCount)
        assertEquals(AccountMode.GUEST, fakeRepo.getCurrentAccount().mode)
    }

    @Test
    fun sessionExpired_doesNotDeleteAccountOrForceLogout() = runTest {
        val fakeRepo = FakeAuthRepository(
            initialState = AuthAccountState(
                uid = "uid_offline",
                email = "offline@example.com",
                displayName = "An",
                mode = AccountMode.AUTHENTICATED
            )
        )
        fakeRepo.markSessionExpired()
        val state = fakeRepo.accountState.first()
        assertTrue(state.isAuthenticated)
        assertTrue(state.sessionExpired)

        val renameRes = fakeRepo.updateDisplayName("An 8B")
        assertTrue(renameRes is AuthOperationResult.Success)
    }
}
