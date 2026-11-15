package com.example.ui.screens.test

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.evaluator.TestTimer
import com.example.domain.model.MatchingPair
import com.example.domain.model.Question
import com.example.domain.model.TestType
import com.example.domain.model.UserAnswer
import com.example.ui.components.FormattedFormulaText
import com.example.ui.components.question.FillBlankQuestionUi
import com.example.ui.components.question.MatchingQuestionUi
import com.example.ui.components.question.MultipleChoiceQuestionUi
import com.example.ui.components.question.OrderingQuestionUi
import com.example.ui.components.question.ShortAnswerQuestionUi
import com.example.ui.components.question.TrueFalseQuestionUi
import com.example.ui.theme.CorrectGreen
import com.example.ui.theme.GoldenStar
import com.example.ui.theme.HocTotTHCSTheme
import com.example.ui.theme.IncorrectRed
import com.example.ui.viewmodel.TestUiState
import com.example.ui.viewmodel.TestViewModel

/**
 * Màn hình làm bài kiểm tra chính thức (F07):
 * - Đồng hồ đếm ngược đổi màu khi còn < 20%
 * - Thông báo nhẹ khi còn 5 phút và 1 phút
 * - KHÔNG hiện gợi ý, KHÔNG chấm ngay
 * - Đánh dấu xem lại sau, lưới câu hỏi nhảy nhanh
 * - Tự lưu nháp liên tục, tự động nộp khi hết giờ
 * - Tái sử dụng các composable câu hỏi từ F06
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestTakingScreen(
    viewModel: TestViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val state = uiState as? TestUiState.InProgress ?: return

    var showQuestionGridSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Xử lý nút Back của hệ thống: hiển thị hộp thoại xác nhận thoát
    BackHandler {
        viewModel.setExitConfirmDialogVisible(true)
    }

    // Màu sắc đồng hồ đếm ngược: chuyển sang đỏ/cam khi dưới 20% thời gian
    val timerColor by animateColorAsState(
        targetValue = if (state.isCriticalTime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        label = "timer_color"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = timerColor.copy(alpha = 0.15f),
                            modifier = Modifier.testTag("exam_timer_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = "Đồng hồ đếm ngược",
                                    tint = timerColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = TestTimer.formatRemainingTime(state.remainingSeconds),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = timerColor
                                    ),
                                    modifier = Modifier.testTag("exam_timer_text")
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.setExitConfirmDialogVisible(true) },
                        modifier = Modifier.testTag("exam_exit_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Thoát bài kiểm tra")
                    }
                },
                actions = {
                    // Nút mở bảng lưới số câu hỏi
                    IconButton(
                        onClick = { showQuestionGridSheet = true },
                        modifier = Modifier.testTag("question_grid_button")
                    ) {
                        Icon(imageVector = Icons.Default.GridView, contentDescription = "Lưới câu hỏi")
                    }

                    // Nút Nộp bài
                    Button(
                        onClick = { viewModel.setSubmitConfirmDialogVisible(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("submit_exam_top_button")
                    ) {
                        Text("Nộp bài", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Thanh điều hướng câu: Câu trước, Lưới câu hỏi, Câu sau
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
                        onClick = { viewModel.previousQuestion() },
                        enabled = state.currentIndex > 0,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("exam_prev_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Câu trước")
                    }

                    TextButton(
                        onClick = { showQuestionGridSheet = true },
                        modifier = Modifier.testTag("exam_palette_label_button")
                    ) {
                        Text(
                            text = "${state.currentIndex + 1} / ${state.questions.size}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (state.currentIndex < state.questions.size - 1) {
                        Button(
                            onClick = { viewModel.nextQuestion() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("exam_next_button")
                        ) {
                            Text("Câu sau")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.setSubmitConfirmDialogVisible(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = CorrectGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("exam_finish_submit_button")
                        ) {
                            Text("Nộp bài", fontWeight = FontWeight.Bold, color = Color.White)
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
                .testTag("test_taking_screen")
        ) {
            // Thanh tiến trình làm bài
            val answeredCount = state.userAnswers.size
            val totalCount = state.questions.size
            val progress = if (totalCount > 0) answeredCount.toFloat() / totalCount.toFloat() else 0f

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .testTag("exam_progress_bar"),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Banner cảnh báo 5 phút hoặc 1 phút (nếu có)
            AnimatedVisibility(
                visible = state.warningMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                state.warningMessage?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.dismissWarningMessage() }) {
                                Icon(Icons.Default.Close, contentDescription = "Đóng thông báo")
                            }
                        }
                    }
                }
            }

            val currentQuestion = state.questions.getOrNull(state.currentIndex)
            if (currentQuestion != null) {
                val isFlagged = state.flaggedIndices.contains(state.currentIndex)

                // Header câu hỏi: Số thứ tự, Nhãn độ khó, Nút Đánh dấu xem lại sau
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${state.currentIndex + 1}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = currentQuestion.difficulty.vietnameseTitle,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Nút Đánh dấu xem lại sau (Bookmark/Flag)
                    OutlinedButton(
                        onClick = { viewModel.toggleFlagCurrentQuestion() },
                        shape = RoundedCornerShape(12.dp),
                        colors = if (isFlagged) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = GoldenStar.copy(alpha = 0.15f),
                                contentColor = GoldenStar
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                        modifier = Modifier.testTag("flag_question_button")
                    ) {
                        Icon(
                            imageVector = if (isFlagged) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isFlagged) "Bỏ đánh dấu" else "Đánh dấu xem lại",
                            tint = if (isFlagged) GoldenStar else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isFlagged) "Đã đánh dấu" else "Xem lại sau",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isFlagged) FontWeight.Bold else FontWeight.Normal,
                                color = if (isFlagged) GoldenStar else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Card Nội dung câu hỏi và công thức
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("exam_question_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        FormattedFormulaText(
                            text = currentQuestion.content,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 17.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Phần trả lời câu hỏi: TÁI SỬ DỤNG 100% các composable câu hỏi từ F06
                // Chế độ thi: isReadOnly = false, gradeResult = null, KHÔNG hiện đáp án đúng
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("exam_question_interaction_container")
                ) {
                    val currentAnswer = state.userAnswers[state.currentIndex]

                    when (currentQuestion) {
                        is Question.MultipleChoice -> {
                            val mcAnswer = currentAnswer as? UserAnswer.MultipleChoice
                            MultipleChoiceQuestionUi(
                                question = currentQuestion,
                                userAnswer = mcAnswer,
                                gradeResult = null,
                                isReadOnly = false,
                                onAnswerSelected = { selected ->
                                    viewModel.selectAnswerForCurrentQuestion(UserAnswer.MultipleChoice(selected))
                                }
                            )
                        }
                        is Question.TrueFalse -> {
                            val tfAnswer = currentAnswer as? UserAnswer.TrueFalse
                            TrueFalseQuestionUi(
                                question = currentQuestion,
                                userAnswer = tfAnswer,
                                gradeResult = null,
                                isReadOnly = false,
                                onAnswerSelected = { selectedBool ->
                                    viewModel.selectAnswerForCurrentQuestion(UserAnswer.TrueFalse(selectedBool))
                                }
                            )
                        }
                        is Question.FillBlank -> {
                            val fbAnswer = currentAnswer as? UserAnswer.FillBlank
                            FillBlankQuestionUi(
                                question = currentQuestion,
                                userAnswer = fbAnswer,
                                gradeResult = null,
                                isReadOnly = false,
                                onTextChanged = { newText ->
                                    viewModel.selectAnswerForCurrentQuestion(UserAnswer.FillBlank(newText))
                                },
                                onSubmit = {
                                    // Chuyển sang câu tiếp theo khi bấm Done trên bàn phím
                                    viewModel.nextQuestion()
                                }
                            )
                        }
                        is Question.Matching -> {
                            val matchAnswer = currentAnswer as? UserAnswer.Matching
                            MatchingQuestionUi(
                                question = currentQuestion,
                                userAnswer = matchAnswer,
                                gradeResult = null,
                                isReadOnly = false,
                                onPairsUpdated = { pairs ->
                                    viewModel.selectAnswerForCurrentQuestion(UserAnswer.Matching(pairs))
                                }
                            )
                        }
                        is Question.Ordering -> {
                            val orderAnswer = currentAnswer as? UserAnswer.Ordering
                            OrderingQuestionUi(
                                question = currentQuestion,
                                userAnswer = orderAnswer,
                                gradeResult = null,
                                isReadOnly = false,
                                onOrderUpdated = { newOrder ->
                                    viewModel.selectAnswerForCurrentQuestion(UserAnswer.Ordering(newOrder))
                                }
                            )
                        }
                        is Question.ShortAnswer -> {
                            val saAnswer = currentAnswer as? UserAnswer.ShortAnswer
                            ShortAnswerQuestionUi(
                                question = currentQuestion,
                                userAnswer = saAnswer,
                                gradeResult = null,
                                isReadOnly = false,
                                onTextChanged = { newText ->
                                    viewModel.selectAnswerForCurrentQuestion(UserAnswer.ShortAnswer(newText))
                                },
                                onSelfEvaluate = {
                                    // Trong lúc thi chưa tự đánh giá, chỉ lưu văn bản
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal BottomSheet: Lưới số câu hỏi (Palette)
    if (showQuestionGridSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuestionGridSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Bảng danh sách câu hỏi",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Chú thích màu sắc
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LegendItem(color = MaterialTheme.colorScheme.primary, text = "Đã làm (${state.userAnswers.size})")
                    LegendItem(color = Color.LightGray, text = "Chưa làm (${state.questions.size - state.userAnswers.size})")
                    LegendItem(color = GoldenStar, text = "Đánh dấu (${state.flaggedIndices.size})")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lưới câu hỏi 5 cột
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .testTag("question_palette_grid")
                ) {
                    items(state.questions.size) { index ->
                        val isAnswered = state.userAnswers.containsKey(index)
                        val isFlagged = state.flaggedIndices.contains(index)
                        val isCurrent = state.currentIndex == index

                        val itemColor = when {
                            isFlagged -> GoldenStar
                            isAnswered -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        val textColor = when {
                            isFlagged -> Color.White
                            isAnswered -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = itemColor,
                            border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.error) else null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.jumpToQuestion(index)
                                    showQuestionGridSheet = false
                                }
                                .testTag("palette_item_$index")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Hộp thoại xác nhận NỘP BÀI (cảnh báo nếu còn câu chưa làm)
    if (state.showSubmitConfirmDialog) {
        val unansweredCount = state.questions.size - state.userAnswers.size
        AlertDialog(
            onDismissRequest = { viewModel.setSubmitConfirmDialogVisible(false) },
            icon = {
                Icon(
                    imageVector = if (unansweredCount > 0) Icons.Default.Warning else Icons.Default.Alarm,
                    contentDescription = null,
                    tint = if (unansweredCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Xác nhận nộp bài?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    if (unansweredCount > 0) {
                        Text(
                            text = "Bạn còn $unansweredCount câu chưa trả lời!",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Các câu bỏ trống sẽ không được tính điểm. Bạn có chắc chắn muốn nộp bài ngay bây giờ không?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            text = "Bạn đã hoàn thành tất cả ${state.questions.size} câu hỏi! Bạn có muốn nộp bài để xem kết quả và lời giải chi tiết?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setSubmitConfirmDialogVisible(false)
                        viewModel.submitTest()
                    },
                    modifier = Modifier.testTag("confirm_submit_exam_button")
                ) {
                    Text("Đồng ý nộp bài")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.setSubmitConfirmDialogVisible(false) },
                    modifier = Modifier.testTag("cancel_submit_exam_button")
                ) {
                    Text("Làm tiếp")
                }
            }
        )
    }

    // Hộp thoại xác nhận THOÁT BÀI THI (báo sẽ lưu nháp)
    if (state.showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setExitConfirmDialogVisible(false) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Tạm dừng bài kiểm tra?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Bài làm và thời gian còn lại của bạn sẽ được lưu nháp tự động. Bạn có thể tiếp tục làm sau nếu còn thời gian.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setExitConfirmDialogVisible(false)
                        onNavigateBack()
                    },
                    modifier = Modifier.testTag("confirm_exit_exam_button")
                ) {
                    Text("Lưu nháp và Thoát")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.setExitConfirmDialogVisible(false) }
                ) {
                    Text("Ở lại làm tiếp")
                }
            }
        )
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.labelSmall)
    }
}

@Preview(showBackground = true)
@Composable
fun TestTakingScreenPreview() {
    HocTotTHCSTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Preview TestTakingScreen")
        }
    }
}
