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
import com.example.data.local.entity.FlashcardEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.FlashcardRepository
import com.example.data.repository.UserProgressRepository
import com.example.ui.screens.flashcard.FlashcardStudyScreen
import com.example.ui.theme.HocTotTHCSTheme
import com.example.ui.viewmodel.FlashcardViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI Test kiểm tra luồng Thẻ ghi nhớ / Flashcard (F17a):
 * - Hiển thị thẻ học ban đầu (mặt trước)
 * - Thao tác lật thẻ (xem giải thích mặt sau)
 * - Đánh dấu Đã nhớ / Cần ôn lại
 * - Hoàn thành phiên ôn tập và kiểm tra màn hình tổng kết
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FlashcardFlowComposeUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: AppDatabase
    private lateinit var flashcardRepo: FlashcardRepository
    private lateinit var userProgressRepo: UserProgressRepository
    private lateinit var userPreferencesManager: UserPreferencesManager

    private val fakeCards = listOf(
        FlashcardEntity(
            id = "fc_1",
            subjectId = "toan",
            grade = 6,
            front = "Định lý số nguyên tố chẵn duy nhất?",
            back = "Số 2 là số nguyên tố chẵn duy nhất.",
            category = "Số học",
            box = 1
        ),
        FlashcardEntity(
            id = "fc_2",
            subjectId = "toan",
            grade = 6,
            front = "Công thức diện tích hình chữ nhật?",
            back = "S = a x b (dài x rộng)",
            category = "Hình học",
            box = 1
        )
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        flashcardRepo = FlashcardRepository(database)
        userProgressRepo = UserProgressRepository(database)
        userPreferencesManager = UserPreferencesManager(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testFlashcardStudyFlow() {
        val viewModel = FlashcardViewModel(
            flashcardRepository = flashcardRepo,
            userProgressRepository = userProgressRepo,
            userPreferencesManager = userPreferencesManager
        )

        viewModel.startStudySession(fakeCards)

        composeTestRule.setContent {
            HocTotTHCSTheme {
                FlashcardStudyScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }

        // Kiểm tra mặt trước thẻ 1 hiển thị
        composeTestRule.onNodeWithText("Định lý số nguyên tố chẵn duy nhất?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Thẻ 1 / 2").assertIsDisplayed()

        // Thao tác lật thẻ
        composeTestRule.onNodeWithTag("flashcard_flip_surface").performClick()
        assertTrue("Thẻ phải ở trạng thái đã lật", viewModel.studyState.value.isFlipped)

        // Bấm nút Đã nhớ
        composeTestRule.onNodeWithTag("flashcard_remembered_button").performClick()
        assertEquals(1, viewModel.studyState.value.currentIndex)
        assertEquals(1, viewModel.studyState.value.rememberedCount)

        // Thẻ 2 hiển thị
        composeTestRule.onNodeWithText("Công thức diện tích hình chữ nhật?").assertIsDisplayed()

        // Bấm nút Cần ôn lại cho thẻ 2
        composeTestRule.onNodeWithTag("flashcard_needs_review_button").performClick()

        // Kiểm tra màn hình kết thúc buổi ôn tập
        composeTestRule.onNodeWithText("Hoàn thành buổi ôn tập!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Đã nhớ: 1 thẻ • Cần ôn lại: 1 thẻ").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ôn tập lại từ đầu").assertIsDisplayed()
    }
}
