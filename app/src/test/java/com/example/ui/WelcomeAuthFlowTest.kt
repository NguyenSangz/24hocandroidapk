package com.example.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.domain.repository.AccountMode
import com.example.domain.repository.AuthAccountState
import com.example.ui.screens.account.AccountUiState
import com.example.ui.screens.auth.ParentalConsentContent
import com.example.ui.screens.auth.WelcomeAuthContent
import com.example.ui.theme.HocTotTHCSTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Kiểm thử giao diện và hành vi luồng Chào mừng, Đăng nhập & Xác nhận Phụ huynh (Phần 1):
 * - WelcomeAuthScreen: Nút Google Sign-In, Nút Khách (Offline), Form Email/Mật khẩu.
 * - ParentalConsentScreen: Cổng phụ huynh cho học sinh < 16t, phép toán thử thách, email phụ huynh.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WelcomeAuthFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testWelcomeAuthScreen_displaysPrimaryActions() {
        var guestClicked = false
        var googleSignInClicked = false

        composeTestRule.setContent {
            HocTotTHCSTheme {
                WelcomeAuthContent(
                    uiState = AccountUiState(),
                    accountState = AuthAccountState(mode = AccountMode.GUEST),
                    snackbarHostState = remember { SnackbarHostState() },
                    onGoogleSignInClick = { googleSignInClicked = true },
                    onToggleRegisterMode = {},
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onNicknameChanged = {},
                    onBirthYearChanged = {},
                    onSubmitEmailAuth = {},
                    onForgotPasswordClick = {},
                    onDismissForgotPassword = {},
                    onResetEmailChanged = {},
                    onSubmitPasswordReset = {},
                    onContinueAsGuest = { guestClicked = true },
                    onNavigateToPrivacy = {}
                )
            }
        }

        // 1. Tiêu đề và thông tin GDPT 2018
        composeTestRule.onNodeWithText("Học Tốt THCS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chương trình GDPT 2018 (Lớp 6 – 9)").assertIsDisplayed()

        // 2. Nút Đăng nhập Google
        val googleBtn = composeTestRule.onNodeWithTag("welcome_google_sign_in_button")
        googleBtn.assertExists().performClick()
        assertTrue("Nút Google Sign-In phải kích hoạt callback", googleSignInClicked)

        // 3. Cuộn đến nút Tiếp tục với tư cách Khách
        composeTestRule.onNodeWithTag("welcome_auth_screen_list")
            .performScrollToNode(hasTestTag("welcome_continue_as_guest_button"))
        val guestBtn = composeTestRule.onNodeWithTag("welcome_continue_as_guest_button")
        guestBtn.assertExists().performClick()
        assertTrue("Nút Khách phải kích hoạt callback", guestClicked)

        // 4. Cuộn đến nút Dùng Email mở rộng
        composeTestRule.onNodeWithTag("welcome_auth_screen_list")
            .performScrollToNode(hasTestTag("toggle_email_form_button"))
        composeTestRule.onNodeWithTag("toggle_email_form_button").assertExists().performClick()
        composeTestRule.onNodeWithTag("welcome_email_input").assertExists()
        composeTestRule.onNodeWithTag("welcome_password_input").assertExists()
    }

    @Test
    fun testParentalConsentScreen_minorFlow() {
        var consentSubmitted by mutableStateOf(false)
        var birthYearValue by mutableStateOf("2012")
        var parentEmailValue by mutableStateOf("")
        var parentMathValue by mutableStateOf("")

        composeTestRule.setContent {
            HocTotTHCSTheme {
                ParentalConsentContent(
                    uiState = AccountUiState(
                        birthYearInput = birthYearValue,
                        parentEmailInput = parentEmailValue,
                        parentMathChallengeAnswer = parentMathValue,
                        parentConsentChecked = true,
                        parentConsentRequestPending = true
                    ),
                    accountState = AuthAccountState(isMinorUnder16 = true),
                    snackbarHostState = remember { SnackbarHostState() },
                    challengeFactorA = 7,
                    challengeFactorB = 8,
                    onBirthYearChanged = { birthYearValue = it },
                    onParentEmailChanged = { parentEmailValue = it },
                    onParentConsentChecked = {},
                    onParentMathAnswerChanged = { parentMathValue = it },
                    onSubmitConsent = { consentSubmitted = true },
                    onSkipOrLater = {},
                    onNavigateToPrivacy = {}
                )
            }
        }

        // 1. Tiêu đề Cổng phụ huynh
        composeTestRule.onNodeWithText("Xác nhận của Phụ huynh").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parent_consent_check_status_button").assertIsDisplayed()

        // 2. Nhập email phụ huynh và phép toán
        composeTestRule.onNodeWithTag("parent_consent_parent_email_input")
            .assertExists()
            .performTextInput("me@example.com")

        composeTestRule.onNodeWithTag("parent_consent_gate_answer_input")
            .assertExists()
            .performTextInput("56")

        // 3. Nút xác nhận đồng ý
        composeTestRule.onNodeWithTag("parental_consent_list")
            .performScrollToNode(hasTestTag("parent_consent_submit_button"))
        val submitBtn = composeTestRule.onNodeWithTag("parent_consent_submit_button")
        submitBtn.assertExists().performClick()
        assertTrue("Callback xác nhận đồng ý phải được gọi", consentSubmitted)
    }
}
