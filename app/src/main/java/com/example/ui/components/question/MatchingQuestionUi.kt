package com.example.ui.components.question

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.domain.model.MatchingPair
import com.example.domain.model.Question
import com.example.domain.model.UserAnswer
import com.example.ui.components.FormattedFormulaText
import com.example.ui.theme.CorrectGreen
import com.example.ui.theme.HocTotTHCSTheme
import com.example.ui.theme.IncorrectRed

// Bảng màu cho từng cặp nối
private val PairColors = listOf(
    Color(0xFF4A7FA8), // Xanh dương
    Color(0xFF3F9E6E), // Xanh ngọc
    Color(0xFFC08A52), // Vàng hổ phách
    Color(0xFF7C3AED), // Tím
    Color(0xFFDB2777), // Hồng
    Color(0xFF0891B2)  // Xanh lục lam
)

/**
 * Giao diện câu hỏi Nối cặp (Matching)
 * Người học bấm vế trái rồi vế phải để nối cặp, thể hiện màu sắc và huy hiệu rõ ràng, cho phép sửa trước khi nộp.
 */
@Composable
fun MatchingQuestionUi(
    question: Question.Matching,
    userAnswer: UserAnswer.Matching?,
    gradeResult: GradeResult?,
    isReadOnly: Boolean,
    onPairsUpdated: (List<MatchingPair>) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPairs = userAnswer?.userPairs ?: emptyList()
    var selectedLeft by remember { mutableStateOf<String?>(null) }
    val isGraded = gradeResult != null

    // Danh sách vế trái và vế phải cố định để hiển thị
    val leftItems = remember(question.pairs) { question.pairs.map { it.left } }
    // Vế phải xáo trộn nhẹ để học sinh chủ động tìm
    val rightItems = remember(question.pairs) {
        val rights = question.pairs.map { it.right }
        if (rights.size > 1) rights.reversed() else rights
    }

    // Bản đồ tìm cặp hiện tại của từng item
    // left -> pairIndex
    val leftToPairMap = remember(currentPairs) {
        currentPairs.mapIndexed { idx, p -> p.left to idx }.toMap()
    }
    // right -> pairIndex
    val rightToPairMap = remember(currentPairs) {
        currentPairs.mapIndexed { idx, p -> p.right to idx }.toMap()
    }

    // Bản đồ đáp án chuẩn để đối chiếu khi đã chấm
    val correctLeftToRight = remember(question.pairs) {
        question.pairs.associate { AnswerNormalizer.normalize(it.left) to AnswerNormalizer.normalize(it.right) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Bấm chọn 1 mục ở Vế A rồi bấm mục tương ứng ở Vế B để nối:",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // CỘT VẾ A (BÊN TRÁI)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Vế A",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                leftItems.forEachIndexed { idx, leftText ->
                    val pairIdx = leftToPairMap[leftText]
                    val isCurrentlySelected = selectedLeft == leftText
                    val pairColor = if (pairIdx != null) PairColors[pairIdx % PairColors.size] else MaterialTheme.colorScheme.primary

                    // Trạng thái đúng sai sau khi chấm
                    val isPairCorrect = if (isGraded && pairIdx != null) {
                        val pairedRight = currentPairs.getOrNull(pairIdx)?.right ?: ""
                        correctLeftToRight[AnswerNormalizer.normalize(leftText)] == AnswerNormalizer.normalize(pairedRight)
                    } else null

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isGraded && isPairCorrect == true -> Color(0xFFDCFCE7)
                            isGraded && isPairCorrect == false -> Color(0xFFFFE4E6)
                            pairIdx != null -> pairColor.copy(alpha = 0.12f)
                            isCurrentlySelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            if (isCurrentlySelected || pairIdx != null) 2.dp else 1.dp,
                            when {
                                isGraded && isPairCorrect == true -> CorrectGreen
                                isGraded && isPairCorrect == false -> IncorrectRed
                                isCurrentlySelected -> MaterialTheme.colorScheme.primary
                                pairIdx != null -> pairColor
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isReadOnly) {
                                selectedLeft = if (selectedLeft == leftText) null else leftText
                            }
                            .testTag("matching_left_$idx")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (pairIdx != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isGraded && isPairCorrect != null) {
                                        if (isPairCorrect) CorrectGreen else IncorrectRed
                                    } else pairColor,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${pairIdx + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                FormattedFormulaText(
                                    text = leftText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (pairIdx != null || isCurrentlySelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // CỘT VẾ B (BÊN PHẢI)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Vế B",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                rightItems.forEachIndexed { idx, rightText ->
                    val pairIdx = rightToPairMap[rightText]
                    val pairColor = if (pairIdx != null) PairColors[pairIdx % PairColors.size] else MaterialTheme.colorScheme.secondary

                    val isPairCorrect = if (isGraded && pairIdx != null) {
                        val pairedLeft = currentPairs.getOrNull(pairIdx)?.left ?: ""
                        correctLeftToRight[AnswerNormalizer.normalize(pairedLeft)] == AnswerNormalizer.normalize(rightText)
                    } else null

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            isGraded && isPairCorrect == true -> Color(0xFFDCFCE7)
                            isGraded && isPairCorrect == false -> Color(0xFFFFE4E6)
                            pairIdx != null -> pairColor.copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            if (pairIdx != null) 2.dp else 1.dp,
                            when {
                                isGraded && isPairCorrect == true -> CorrectGreen
                                isGraded && isPairCorrect == false -> IncorrectRed
                                pairIdx != null -> pairColor
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isReadOnly) {
                                val currentLeft = selectedLeft
                                if (currentLeft != null) {
                                    // Tạo hoặc cập nhật cặp mới: (currentLeft -> rightText)
                                    val filtered = currentPairs.filter { it.left != currentLeft && it.right != rightText }
                                    val updated = filtered + MatchingPair(left = currentLeft, right = rightText)
                                    onPairsUpdated(updated)
                                    selectedLeft = null
                                } else if (pairIdx != null) {
                                    // Bấm để hủy cặp
                                    val updated = currentPairs.filter { it.right != rightText }
                                    onPairsUpdated(updated)
                                }
                            }
                            .testTag("matching_right_$idx")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (pairIdx != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isGraded && isPairCorrect != null) {
                                        if (isPairCorrect) CorrectGreen else IncorrectRed
                                    } else pairColor,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${pairIdx + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                FormattedFormulaText(
                                    text = rightText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (pairIdx != null) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Hướng dẫn hoặc nút xóa ghép cặp
        if (currentPairs.isNotEmpty() && !isReadOnly) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onPairsUpdated(emptyList()) }) {
                    Icon(
                        imageVector = Icons.Default.LinkOff,
                        contentDescription = "Gỡ toàn bộ nối cặp",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    text = "Đã nối ${currentPairs.size}/${question.pairs.size} cặp",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.outline)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MatchingQuestionPreview() {
    HocTotTHCSTheme {
        val sample = Question.Matching(
            id = "m1",
            lessonId = "l1",
            content = "Nối mỗi số với số đối tương ứng:",
            pairs = listOf(
                MatchingPair("5", "-5"),
                MatchingPair("-12", "12"),
                MatchingPair("0", "0")
            )
        )
        MatchingQuestionUi(
            question = sample,
            userAnswer = UserAnswer.Matching(listOf(MatchingPair("5", "-5"))),
            gradeResult = null,
            isReadOnly = false,
            onPairsUpdated = {}
        )
    }
}
