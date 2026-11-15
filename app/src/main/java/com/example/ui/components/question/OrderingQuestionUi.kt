package com.example.ui.components.question

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * Giao diện câu hỏi Sắp xếp (Ordering / Sorting)
 * Học sinh dùng các nút Lên (▲) và Xuống (▼) để đổi vị trí, thao tác mượt mà và trực quan.
 */
@Composable
fun OrderingQuestionUi(
    question: Question.Ordering,
    userAnswer: UserAnswer.Ordering?,
    gradeResult: GradeResult?,
    isReadOnly: Boolean,
    onOrderUpdated: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOrder = userAnswer?.userOrder ?: question.correctOrder
    val isGraded = gradeResult != null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Bấm mũi tên ▲ ▼ để sắp xếp các mục theo đúng thứ tự logic:",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        currentOrder.forEachIndexed { index, itemText ->
            // Kiểm tra xem vị trí hiện tại có khớp với đáp án đúng không
            val isCorrectAtThisPosition = if (isGraded && index < question.correctOrder.size) {
                AnswerNormalizer.areEquivalent(itemText, question.correctOrder[index])
            } else null

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = when {
                    isGraded && isCorrectAtThisPosition == true -> Color(0xFFDCFCE7)
                    isGraded && isCorrectAtThisPosition == false -> Color(0xFFFFE4E6)
                    else -> MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    if (isGraded) 2.dp else 1.dp,
                    when {
                        isGraded && isCorrectAtThisPosition == true -> CorrectGreen
                        isGraded && isCorrectAtThisPosition == false -> IncorrectRed
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ordering_item_$index")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Số thứ tự 1, 2, 3...
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isGraded && isCorrectAtThisPosition == true -> CorrectGreen
                                    isGraded && isCorrectAtThisPosition == false -> IncorrectRed
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isGraded) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Nội dung mục
                    Box(modifier = Modifier.weight(1f)) {
                        FormattedFormulaText(
                            text = itemText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        )
                    }

                    // Các nút di chuyển Lên / Xuống
                    if (!isReadOnly) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val mutable = currentOrder.toMutableList()
                                        val temp = mutable[index]
                                        mutable[index] = mutable[index - 1]
                                        mutable[index - 1] = temp
                                        onOrderUpdated(mutable)
                                    }
                                },
                                enabled = index > 0,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("ordering_up_$index")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Di chuyển lên",
                                    tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (index < currentOrder.size - 1) {
                                        val mutable = currentOrder.toMutableList()
                                        val temp = mutable[index]
                                        mutable[index] = mutable[index + 1]
                                        mutable[index + 1] = temp
                                        onOrderUpdated(mutable)
                                    }
                                },
                                enabled = index < currentOrder.size - 1,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("ordering_down_$index")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Di chuyển xuống",
                                    tint = if (index < currentOrder.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else if (isGraded && isCorrectAtThisPosition != null) {
                        Icon(
                            imageVector = if (isCorrectAtThisPosition) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isCorrectAtThisPosition) CorrectGreen else IncorrectRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OrderingQuestionPreview() {
    HocTotTHCSTheme {
        val sample = Question.Ordering(
            id = "ord1",
            lessonId = "l1",
            content = "Sắp xếp các số nguyên sau theo thứ tự tăng dần:",
            correctOrder = listOf("-10", "-3", "0", "5", "12")
        )
        OrderingQuestionUi(
            question = sample,
            userAnswer = UserAnswer.Ordering(listOf("-3", "-10", "0", "5", "12")),
            gradeResult = null,
            isReadOnly = false,
            onOrderUpdated = {}
        )
    }
}
