package com.example.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.CurriculumRepository
import com.example.data.repository.WrongQuestionRepository
import com.example.ui.screens.wrongbank.WrongBankScreen
import com.example.ui.theme.HocTotTHCSTheme
import com.example.ui.viewmodel.WrongBankViewModel
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI Test kiểm tra luồng Ngân hàng câu sai (F17a):
 * - Trạng thái rỗng: hiển thị thông điệp khích lệ khi chưa có câu sai
 * - Thanh tiêu đề & nút làm mới dữ liệu
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WrongBankFlowComposeUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: AppDatabase
    private lateinit var wrongQuestionRepo: WrongQuestionRepository
    private lateinit var curriculumRepo: CurriculumRepository
    private lateinit var userPreferencesManager: UserPreferencesManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        wrongQuestionRepo = WrongQuestionRepository(database)
        curriculumRepo = CurriculumRepository(database)
        userPreferencesManager = UserPreferencesManager(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testEmptyMistakeStateDisplay() {
        var backClicked = false
        val viewModel = WrongBankViewModel(
            wrongQuestionRepository = wrongQuestionRepo,
            curriculumRepository = curriculumRepo,
            userPreferencesManager = userPreferencesManager
        )

        composeTestRule.setContent {
            HocTotTHCSTheme {
                WrongBankScreen(
                    viewModel = viewModel,
                    onNavigateBack = { backClicked = true },
                    onStartReviewSession = { _, _, _, _, _ -> }
                )
            }
        }

        // Chờ ViewModel nạp xong trạng thái ban đầu.
        // Robolectric giữ main looper ở chế độ PAUSED, nên phải chủ động idle
        // để coroutine DataStore/Room được resume (waitUntil thuần không đẩy looper).
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            !viewModel.uiState.value.isLoading
        }

        // Kiểm tra thông điệp khích lệ khi chưa có câu sai
        composeTestRule.onNodeWithText("Tuyệt vời, bạn chưa có câu sai nào!").assertIsDisplayed()
        composeTestRule.onNodeWithTag("empty_go_practice_button").assertExists().performClick()
        assertTrue("Bấm nút luyện tập phải kích hoạt callback quay về", backClicked)
    }

    @Test
    fun testTopBarAndRefreshAction() {
        val viewModel = WrongBankViewModel(
            wrongQuestionRepository = wrongQuestionRepo,
            curriculumRepository = curriculumRepo,
            userPreferencesManager = userPreferencesManager
        )

        composeTestRule.setContent {
            HocTotTHCSTheme {
                WrongBankScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                    onStartReviewSession = { _, _, _, _, _ -> }
                )
            }
        }

        // Kiểm tra tiêu đề màn hình hiển thị đúng
        composeTestRule.onNodeWithText("Ngân hàng câu sai").assertIsDisplayed()

        // Kiểm tra nút làm mới hiển thị và có thể bấm
        composeTestRule.onNodeWithTag("wrong_bank_refresh_button").assertIsDisplayed().performClick()
    }
}
