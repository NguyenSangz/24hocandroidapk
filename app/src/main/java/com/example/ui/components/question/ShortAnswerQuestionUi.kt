package com.example.ui.components.question

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.GradeResult
import com.example.domain.model.GradeStatus
import com.example.domain.model.Question
import com.example.domain.model.UserAnswer
import com.example.ui.components.FormattedFormulaText
import com.example.ui.theme.CorrectGreen
import com.example.ui.theme.HocTotTHCSTheme
import com.example.ui.theme.IncorrectRed

/**
 * Giao diện câu hỏi Tự luận ngắn (Short Answer / Short Essay)
 * Học sinh gõ câu trả lời, sau đó đối chiếu với đáp án mẫu và tự đánh giá "Mình làm đúng" hoặc "Chưa đúng".
 */
@Composable
fun ShortAnswerQuestionUi(
    question: Question.ShortAnswer,
    userAnswer: UserAnswer.ShortAnswer?,
    gradeResult: GradeResult?,
    isReadOnly: Boolean,
    onTextChanged: (String) -> Unit,
    onSelfEvaluate: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val submittedText = userAnswer?.submittedText ?: ""
    val isGraded = gradeResult != null

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = submittedText,
            onValueChange = { if (!isReadOnly) onTextChanged(it) },
            label = { Text("Nhập câu trả lời tự luận của bạn") },
            placeholder = { Text("Trình bày ngắn gọn câu trả lời hoặc cách tính...") },
            enabled = !isReadOnly,
            minLines = 3,
            maxLines = 6,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("short_answer_input_field")
        )

        // Khi đã kiểm tra hoặc cần tự đối chiếu
        if (isGraded) {
            Spacer(modifier = Modifier.height(14.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFC08A52),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đáp án mẫu để tự đối chiếu:",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    question.sampleAnswers.forEach { sample ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text(text = "• ", fontWeight = FontWeight.Bold)
                            FormattedFormulaText(
                                text = sample,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }

                    if (question.evaluationCriteria.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tiêu chí chấm: ${question.evaluationCriteria}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        )
                    }

                    // Nút tự đánh giá
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Bạn tự nhận xét câu trả lời của mình thế nào?",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onSelfEvaluate(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (gradeResult.isCorrect) CorrectGreen else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("self_eval_correct_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (gradeResult.isCorrect) Color.White else CorrectGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mình làm đúng",
                                color = if (gradeResult.isCorrect) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { onSelfEvaluate(false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!gradeResult.isCorrect && gradeResult.status != GradeStatus.SELF_EVALUATION) IncorrectRed else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("self_eval_incorrect_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = if (!gradeResult.isCorrect && gradeResult.status != GradeStatus.SELF_EVALUATION) Color.White else IncorrectRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Chưa đúng",
                                color = if (!gradeResult.isCorrect && gradeResult.status != GradeStatus.SELF_EVALUATION) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShortAnswerQuestionPreview() {
    HocTotTHCSTheme {
        val sample = Question.ShortAnswer(
            id = "sa1",
            lessonId = "l1",
            content = "Nêu định nghĩa hai góc kề bù.",
            sampleAnswers = listOf("Hai góc vừa kề nhau vừa bù nhau (có tổng số đo bằng 180 độ)"),
            evaluationCriteria = "Cần nêu đủ hai ý: kề nhau và có tổng số đo bằng 180 độ"
        )
        ShortAnswerQuestionUi(
            question = sample,
            userAnswer = UserAnswer.ShortAnswer("Là 2 góc có 1 cạnh chung và tổng bằng 180"),
            gradeResult = null,
            isReadOnly = false,
            onTextChanged = {},
            onSelfEvaluate = {}
        )
    }
}
