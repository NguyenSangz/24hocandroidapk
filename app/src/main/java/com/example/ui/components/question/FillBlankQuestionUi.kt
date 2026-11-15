package com.example.ui.components.question

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.GradeResult
import com.example.domain.model.Question
import com.example.domain.model.UserAnswer
import com.example.ui.theme.CorrectGreen
import com.example.ui.theme.HocTotTHCSTheme
import com.example.ui.theme.IncorrectRed

/**
 * Giao diện câu hỏi Điền khuyết (Fill-in-the-blank)
 * Tự động chọn kiểu bàn phím (Số hoặc Chữ) dựa trên đáp án dự kiến.
 */
@Composable
fun FillBlankQuestionUi(
    question: Question.FillBlank,
    userAnswer: UserAnswer.FillBlank?,
    gradeResult: GradeResult?,
    isReadOnly: Boolean,
    onTextChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textValue = userAnswer?.filledText ?: ""
    val isGraded = gradeResult != null

    // Nhận diện kiểu bàn phím: nếu tất cả đáp án đúng đều là số
    val isNumericAnswer = remember(question.acceptedAnswers) {
        question.acceptedAnswers.isNotEmpty() && question.acceptedAnswers.all { ans ->
            ans.trim().replace(",", ".").toDoubleOrNull() != null
        }
    }

    val keyboardType = if (isNumericAnswer) KeyboardType.Decimal else KeyboardType.Text

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = textValue,
            onValueChange = { if (!isReadOnly) onTextChanged(it) },
            label = { Text("Nhập từ/số còn thiếu vào chỗ trống") },
            placeholder = { Text(if (isNumericAnswer) "Ví dụ: 12 hoặc 3,5" else "Nhập câu trả lời...") },
            enabled = !isReadOnly,
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (textValue.isNotEmpty() && !isReadOnly) {
                    IconButton(onClick = { onTextChanged("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Xóa chữ đã nhập")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (textValue.isNotBlank() && !isReadOnly) {
                        onSubmit()
                    }
                }
            ),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isGraded) {
                    if (gradeResult.isCorrect) CorrectGreen else IncorrectRed
                } else MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (isGraded) {
                    if (gradeResult.isCorrect) CorrectGreen else IncorrectRed
                } else MaterialTheme.colorScheme.outline
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("fill_blank_input_field")
        )

        if (isGraded && !gradeResult.isCorrect) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Đáp án đúng: ${question.acceptedAnswers.joinToString(" hoặc ")}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CorrectGreen
                ),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FillBlankQuestionPreview() {
    HocTotTHCSTheme {
        val sample = Question.FillBlank(
            id = "fb1",
            lessonId = "l1",
            content = "Số nguyên nhỏ nhất có hai chữ số là ___.",
            acceptedAnswers = listOf("-99")
        )
        FillBlankQuestionUi(
            question = sample,
            userAnswer = UserAnswer.FillBlank("-99"),
            gradeResult = null,
            isReadOnly = false,
            onTextChanged = {},
            onSubmit = {}
        )
    }
}
