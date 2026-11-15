package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.domain.model.Question
import com.example.domain.model.UserAnswer
import com.example.ui.components.question.MultipleChoiceQuestionUi
import com.example.ui.components.question.TrueFalseQuestionUi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI Test sử dụng Robolectric và ComposeTestRule kiểm tra luồng trả lời câu hỏi:
 * - Render câu hỏi trắc nghiệm
 * - Bấm chọn đáp án
 * - Render câu hỏi đúng sai
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PracticeComposeUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testMultipleChoiceSelectionFlow() {
        var selectedResult = ""
        val sampleQuestion = Question.MultipleChoice(
            id = "q_test_1",
            lessonId = "l1",
            content = "Số nguyên tố chẵn duy nhất là số nào?",
            options = listOf("2", "4", "6", "8"),
            correctAnswer = "2"
        )

        composeTestRule.setContent {
            MultipleChoiceQuestionUi(
                question = sampleQuestion,
                userAnswer = if (selectedResult.isNotEmpty()) UserAnswer.MultipleChoice(selectedResult) else null,
                gradeResult = null,
                isReadOnly = false,
                onAnswerSelected = { selectedResult = it }
            )
        }

        // Kiểm tra phương án đầu tiên (index 0 - số 2) hiển thị và có thể bấm
        composeTestRule.onNodeWithTag("option_0_tag").assertIsDisplayed().performClick()
        assertEquals("2", selectedResult)

        // Bấm phương án thứ hai (index 1 - số 4)
        composeTestRule.onNodeWithTag("option_1_tag").assertIsDisplayed().performClick()
        assertEquals("4", selectedResult)
    }

    @Test
    fun testTrueFalseSelectionFlow() {
        var selectedBoolean: Boolean? = null
        val sampleTf = Question.TrueFalse(
            id = "tf_test_1",
            lessonId = "l1",
            content = "Hình thoi có hai đường chéo vuông góc.",
            correctAnswer = true
        )

        composeTestRule.setContent {
            TrueFalseQuestionUi(
                question = sampleTf,
                userAnswer = if (selectedBoolean != null) UserAnswer.TrueFalse(selectedBoolean!!) else null,
                gradeResult = null,
                isReadOnly = false,
                onAnswerSelected = { selectedBoolean = it }
            )
        }

        // Bấm nút "Đúng"
        composeTestRule.onNodeWithTag("tf_true_button").assertIsDisplayed().performClick()
        assertEquals(true, selectedBoolean)

        // Bấm nút "Sai"
        composeTestRule.onNodeWithTag("tf_false_button").assertIsDisplayed().performClick()
        assertEquals(false, selectedBoolean)
    }
}
