package com.example.ui.components.question

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.evaluator.AnswerNormalizer
import com.example.domain.model.GradeResult
import com.example.domain.model.Question
import com.example.domain.model.UserAnswer
import com.example.ui.components.FormattedFormulaText
import com.example.ui.theme.CorrectGreen
import com.example.ui.theme.HocTotTHCSTheme
import com.example.ui.theme.IncorrectRed

/**
 * Giao diện hiển thị câu hỏi Trắc nghiệm khách quan 4 lựa chọn (hoặc N lựa chọn)
 * Tái sử dụng linh hoạt ở Luyện tập (F06) và Thi trắc nghiệm (F07).
 */
@Composable
fun MultipleChoiceQuestionUi(
    question: Question.MultipleChoice,
    userAnswer: UserAnswer.MultipleChoice?,
    gradeResult: GradeResult?,
    isReadOnly: Boolean,
    onAnswerSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedOption = userAnswer?.selectedOption ?: ""
    val isGraded = gradeResult != null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        question.options.forEachIndexed { index, option ->
            val optionLabel = ('A'.code + index).toChar().toString()
            val isSelected = AnswerNormalizer.areEquivalent(selectedOption, option)
            val isCorrectOption = isGraded && AnswerNormalizer.areEquivalent(question.correctAnswer, option)
            val isSelectedWrong = isGraded && isSelected && !isCorrectOption

            val containerColor = when {
                isGraded && isCorrectOption -> Color(0xFFDCFCE7) // Xanh lá nhạt
                isGraded && isSelectedWrong -> Color(0xFFFFE4E6) // Đỏ nhạt
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                else -> MaterialTheme.colorScheme.surface
            }

            val borderColor = when {
                isGraded && isCorrectOption -> CorrectGreen
                isGraded && isSelectedWrong -> IncorrectRed
                isSelected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = containerColor,
                border = BorderStroke(if (isSelected || (isGraded && isCorrectOption)) 2.dp else 1.dp, borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = !isReadOnly) {
                        onAnswerSelected(option)
                    }
                    .testTag("option_${index}_tag")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Huy hiệu chữ cái A, B, C, D
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isGraded && isCorrectOption -> CorrectGreen
                                    isGraded && isSelectedWrong -> IncorrectRed
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGraded && isCorrectOption) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Đáp án đúng",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (isGraded && isSelectedWrong) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Đã chọn sai",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = optionLabel,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Nội dung lựa chọn hỗ trợ công thức Toán LaTeX
                    Box(modifier = Modifier.weight(1f)) {
                        FormattedFormulaText(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected || (isGraded && isCorrectOption)) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MultipleChoiceQuestionPreview() {
    HocTotTHCSTheme {
        val sample = Question.MultipleChoice(
            id = "q1",
            lessonId = "l1",
            content = "Số nguyên tố chẵn duy nhất là số nào?",
            options = listOf("2", "4", "6", "0"),
            correctAnswer = "2"
        )
        MultipleChoiceQuestionUi(
            question = sample,
            userAnswer = UserAnswer.MultipleChoice("2"),
            gradeResult = null,
            isReadOnly = false,
            onAnswerSelected = {}
        )
    }
}
