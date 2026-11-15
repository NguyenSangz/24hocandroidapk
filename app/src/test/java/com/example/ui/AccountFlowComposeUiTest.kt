package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.domain.repository.AccountMode
import com.example.domain.repository.AuthAccountState
import com.example.ui.screens.account.AccountScreenContent
import com.example.ui.screens.account.AccountUiState
import com.example.ui.theme.HocTotTHCSTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI Test kiểm tra luồng Tài khoản & Bảo vệ trẻ em (F17a):
 * - Chế độ Khách (Guest mode): hiển thị rõ ràng, cam kết giữ 100% dữ liệu
 * - Form đăng nhập/tạo tài khoản
 * - Cổng phụ huynh (Parental Gate) cho học sinh dưới 16 tuổi theo Nghị định 13/2023/NĐ-CP
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccountFlowComposeUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testGuestModeDisplay() {
        val guestAccountState = AuthAccountState(
            uid = AuthAccountState.GUEST_UID,
            mode = AccountMode.GUEST
        )

        composeTestRule.setContent {
            HocTotTHCSTheme {
                AccountScreenContent(
                    accountState = guestAccountState,
                    uiState = AccountUiState(),
                    onNavigateBack = {},
                    onContinueGuestClick = {}
                )
            }
        }

        // Kiểm tra thông điệp Chế độ Khách
        composeTestRule.onNodeWithText("Đang học ở Chế độ Khách").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bạn không bắt buộc phải đăng nhập. Khi nâng cấp lên tài khoản (Email hoặc Google), toàn bộ điểm số, flashcard và tiến độ hiện tại trên máy sẽ được giữ nguyên 100%.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tạo tài khoản").assertExists()
    }

    @Test
    fun testParentalGateDialogFlow() {
        val guestAccountState = AuthAccountState(
            uid = AuthAccountState.GUEST_UID,
            mode = AccountMode.GUEST
        )

        var birthYearEntered by mutableStateOf("")
        var parentEmailEntered by mutableStateOf("")
        var consentSubmitted by mutableStateOf(false)

        composeTestRule.setContent {
            val parentalUiState = AccountUiState(
                showParentalConsentDialog = true,
                birthYearInput = birthYearEntered,
                parentEmailInput = parentEmailEntered,
                parentConsentChecked = true
            )
            HocTotTHCSTheme {
                AccountScreenContent(
                    accountState = guestAccountState,
                    uiState = parentalUiState,
                    challengeFactorA = 7,
                    challengeFactorB = 8,
                    onBirthYearChanged = { birthYearEntered = it },
                    onParentEmailChanged = { parentEmailEntered = it },
                    onSubmitParentalConsent = { consentSubmitted = true }
                )
            }
        }

        // Kiểm tra tiêu đề hộp thoại Cổng phụ huynh
        composeTestRule.onNodeWithText("Xác nhận của Cha mẹ / Người giám hộ").assertExists()
        composeTestRule.onNodeWithTag("parent_dialog_birth_year").assertExists().performTextInput("2012")
        assertEquals("2012", birthYearEntered)

        composeTestRule.onNodeWithTag("parent_email_input").assertExists().performTextInput("phuhuynh@example.com")
        assertEquals("phuhuynh@example.com", parentEmailEntered)

        // Kiểm tra checkbox và nút đồng ý tồn tại
        composeTestRule.onNodeWithTag("parent_consent_checkbox").assertExists()
        composeTestRule.onNodeWithTag("confirm_parental_consent_button").assertExists().performClick()
        assertTrue("Xác nhận đồng ý phải được submit", consentSubmitted)
    }

    @Test
    fun pendingParentalConsentShowsStatusCheckInsteadOfConsentForm() {
        var statusCheckRequested = false
        composeTestRule.setContent {
            HocTotTHCSTheme {
                AccountScreenContent(
                    accountState = AuthAccountState(
                        uid = "minor_user",
                        mode = AccountMode.AUTHENTICATED,
                        isMinorUnder16 = true,
                        parentEmail = "parent@example.com"
                    ),
                    uiState = AccountUiState(
                        showParentalConsentDialog = true,
                        parentConsentRequestPending = true
                    ),
                    onCheckParentalConsentStatus = { statusCheckRequested = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("parent_dialog_pending_message").assertIsDisplayed()
        composeTestRule.onNodeWithTag("parent_dialog_check_consent_button")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithTag("parent_dialog_resend_email_button").assertIsDisplayed()
        assertTrue(statusCheckRequested)
    }
}
