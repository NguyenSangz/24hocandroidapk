package com.example.ui.screens.test

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.domain.model.GradeStatus
import com.example.domain.model.Question
import com.example.domain.model.TestAnswerDetail
import com.example.domain.model.UserAnswer
import com.example.ui.components.FormattedFormulaText
import com.example.ui.components.question.FillBlankQuestionUi
import com.example.ui.components.question.MatchingQuestionUi
import com.example.ui.components.question.MultipleChoiceQuestionUi
import com.example.ui.components.question.OrderingQuestionUi
import com.example.ui.components.question.ShortAnswerQuestionUi
import com.example.ui.components.question.TrueFalseQuestionUi
import com.example.ui.theme.CorrectGreen
import com.example.ui.theme.HocTotTHCSTheme
import com.example.ui.theme.IncorrectRed
import com.example.ui.viewmodel.TestUiState
import com.example.ui.viewmodel.TestViewModel

/**
 * Màn hình xem lại bài làm chi tiết (Review Mode - F07):
 * - Bộ lọc: "Tất cả câu hỏi" / "Chỉ câu sai"
 * - Duyệt từng câu kèm trạng thái đúng/sai, điểm số, lời giải chi tiết
 * - Cho phép tự đánh giá lại câu tự luận ngắn và cập nhật lại điểm số tức thì
 * - TÁI SỬ DỤNG toàn bộ composable câu hỏi từ F06 với isReadOnly = true.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestReviewScreen(
    viewModel: TestViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val state = uiState as? TestUiState.Reviewing ?: return

    val displayedDetails = if (state.filterOnlyWrong) {
        state.answerDetails.filter { it.gradeResult == null || !it.gradeResult.isCorrect }
    } else {
        state.answerDetails
    }

    val currentItem = displayedDetails.getOrNull(state.currentIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Xem lại bài làm", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("review_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại kết quả")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (displayedDetails.isNotEmpty()) {
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (state.currentIndex > 0) {
                                    viewModel.jumpToReviewQuestion(state.currentIndex - 1)
                                }
                            },
                            enabled = state.currentIndex > 0,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("review_prev_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Câu trước")
                        }

                        Text(
                            text = "${state.currentIndex + 1} / ${displayedDetails.size}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Button(
                            onClick = {
                                if (state.currentIndex < displayedDetails.size - 1) {
                                    viewModel.jumpToReviewQuestion(state.currentIndex + 1)
                                }
                            },
                            enabled = state.currentIndex < displayedDetails.size - 1,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("review_next_button")
                        ) {
                            Text("Câu sau")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .testTag("test_review_screen")
        ) {
            // Bộ lọc: Tất cả vs Chỉ câu sai
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !state.filterOnlyWrong,
                    onClick = { viewModel.toggleReviewFilter(false) },
                    label = { Text("Tất cả (${state.answerDetails.size})") },
                    leadingIcon = if (!state.filterOnlyWrong) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("filter_all_chip")
                )

                val wrongTotal = state.answerDetails.count { it.gradeResult == null || !it.gradeResult.isCorrect }
                FilterChip(
                    selected = state.filterOnlyWrong,
                    onClick = { viewModel.toggleReviewFilter(true) },
                    label = { Text("Chỉ câu sai ($wrongTotal)") },
                    leadingIcon = if (state.filterOnlyWrong) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.error,
                        selectedLabelColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("filter_wrong_chip")
                )
            }

            // Thanh nhảy nhanh câu hỏi bằng cuộn ngang
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                displayedDetails.forEachIndexed { idx, item ->
                    val isCorrect = item.gradeResult?.isCorrect ?: false
                    val isCurrent = idx == state.currentIndex
                    val color = if (isCorrect) CorrectGreen else IncorrectRed

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCurrent) color else color.copy(alpha = 0.2f),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.jumpToReviewQuestion(idx) }
                            .testTag("review_palette_$idx")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) Color.White else color
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (currentItem != null) {
                val q = currentItem.question
                val gr = currentItem.gradeResult
                val isCorrect = gr?.isCorrect ?: false

                // Card Trạng thái câu: Đúng / Sai / Điểm số
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isCorrect) CorrectGreen.copy(alpha = 0.12f) else IncorrectRed.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isCorrect) CorrectGreen else IncorrectRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isCorrect) "Chính xác (+${gr?.score ?: 1.0f} điểm)" else "Chưa chính xác (0 điểm)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrect) CorrectGreen else IncorrectRed
                                )
                            )
                            if (currentItem.isFlagged) {
                                Text(
                                    text = "★ Bạn đã từng đánh dấu xem lại câu này khi thi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card Nội dung câu hỏi
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FormattedFormulaText(
                            text = q.content,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hiển thị câu hỏi ở chế độ READ_ONLY với kết quả đã chấm
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    when (q) {
                        is Question.MultipleChoice -> {
                            MultipleChoiceQuestionUi(
                                question = q,
                                userAnswer = currentItem.userAnswer as? UserAnswer.MultipleChoice,
                                gradeResult = gr,
                                isReadOnly = true,
                                onAnswerSelected = {}
                            )
                        }
                        is Question.TrueFalse -> {
                            TrueFalseQuestionUi(
                                question = q,
                                userAnswer = currentItem.userAnswer as? UserAnswer.TrueFalse,
                                gradeResult = gr,
                                isReadOnly = true,
                                onAnswerSelected = {}
                            )
                        }
                        is Question.FillBlank -> {
                            FillBlankQuestionUi(
                                question = q,
                                userAnswer = currentItem.userAnswer as? UserAnswer.FillBlank,
                                gradeResult = gr,
                                isReadOnly = true,
                                onTextChanged = {},
                                onSubmit = {}
                            )
                        }
                        is Question.Matching -> {
                            MatchingQuestionUi(
                                question = q,
                                userAnswer = currentItem.userAnswer as? UserAnswer.Matching,
                                gradeResult = gr,
                                isReadOnly = true,
                                onPairsUpdated = {}
                            )
                        }
                        is Question.Ordering -> {
                            OrderingQuestionUi(
                                question = q,
                                userAnswer = currentItem.userAnswer as? UserAnswer.Ordering,
                                gradeResult = gr,
                                isReadOnly = true,
                                onOrderUpdated = {}
                            )
                        }
                        is Question.ShortAnswer -> {
                            Column {
                                ShortAnswerQuestionUi(
                                    question = q,
                                    userAnswer = currentItem.userAnswer as? UserAnswer.ShortAnswer,
                                    gradeResult = gr,
                                    isReadOnly = true,
                                    onTextChanged = {},
                                    onSelfEvaluate = { isSelfCorrect ->
                                        // Cho phép tự đánh giá lại câu tự luận ngắn và cập nhật điểm tức thì
                                        viewModel.selfEvaluateShortAnswer(q.id, isSelfCorrect)
                                    }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Hàng nút tự đánh giá cho tự luận ngắn
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Tự đánh giá lại câu tự luận ngắn:",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Button(
                                                onClick = { viewModel.selfEvaluateShortAnswer(q.id, true) },
                                                colors = ButtonDefaults.buttonColors(containerColor = CorrectGreen),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Mình làm đúng")
                                            }
                                            OutlinedButton(
                                                onClick = { viewModel.selfEvaluateShortAnswer(q.id, false) },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = IncorrectRed),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Chưa đúng")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Card Lời giải chi tiết (Explanation)
                if (q.explanation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("explanation_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Lời giải chi tiết & Ghi nhớ:",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            FormattedFormulaText(
                                text = q.explanation,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không có câu hỏi nào phù hợp với bộ lọc.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TestReviewScreenPreview() {
    HocTotTHCSTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Preview TestReviewScreen")
        }
    }
}
