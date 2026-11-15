package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.local.entity.LessonEntity
import com.example.domain.model.LessonNavigationInfo
import com.example.ui.screens.study.LessonDetailContent
import com.example.ui.theme.HocTotTHCSTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI Test kiểm tra luồng Học bài chi tiết (F17a):
 * - Chi tiết bài học (Tóm tắt kiến thức, ví dụ minh họa, mẹo ghi nhớ)
 * - Tương tác bấm Luyện tập trắc nghiệm bài này
 * - Tương tác bấm Bài tiếp theo
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StudyFlowComposeUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLessonDetailContentDisplay() {
        val sampleLesson = LessonEntity(
            id = "les_1",
            chapterId = "ch_1",
            order = 1,
            title = "Bài 1: Tập hợp các số tự nhiên",
            summary = "Tập hợp các số tự nhiên được ký hiệu là N = {0, 1, 2, 3, ...}",
            examples = "Ví dụ 1: Điền ký hiệu thích hợp: 5 thuộc N",
            tips = "Ghi nhớ: Số 0 là số tự nhiên nhỏ nhất",
            isCompleted = false,
        )

        val navigationInfo = LessonNavigationInfo(
            previousLessonId = null,
            nextLessonId = "les_2",
            currentLessonIndex = 1,
            totalLessonsInSubject = 10,
        )

        var practiceClicked = false
        var nextClicked = false

        composeTestRule.setContent {
            HocTotTHCSTheme {
                LessonDetailContent(
                    lesson = sampleLesson,
                    subjectColor = Color(0xFF2563EB),
                    navigation = navigationInfo,
                    fontScale = 1.0f,
                    onToggleCompleted = {},
                    onNavigatePrevious = {},
                    onNavigateNext = { nextClicked = true },
                    onStartPractice = { practiceClicked = true }
                )
            }
        }

        // Kiểm tra tiêu đề và nội dung bài học hiển thị trong viewport
        composeTestRule.onNodeWithText("Bài 1: Tập hợp các số tự nhiên").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tập hợp các số tự nhiên được ký hiệu là N = {0, 1, 2, 3, ...}").assertIsDisplayed()

        // Kiểm tra các phần tử trong danh sách cuộn tồn tại
        composeTestRule.onNodeWithText("Ghi nhớ: Số 0 là số tự nhiên nhỏ nhất").assertExists()

        // Cuộn tới và bấm nút Luyện tập trắc nghiệm bài này
        composeTestRule.onNodeWithTag("practice_lesson_button").performScrollTo().performClick()
        assertTrue("Callback bắt đầu luyện tập phải được gọi", practiceClicked)

        // Cuộn tới và bấm nút Bài sau
        composeTestRule.onNodeWithTag("next_lesson_button").performScrollTo().performClick()
        assertTrue("Callback chuyển sang bài kế tiếp phải được gọi", nextClicked)
    }
}
