package com.example.ui.screens.wrongbank

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.GradeResult
import com.example.domain.model.GradeStatus
import com.example.domain.model.Question
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
import com.example.ui.viewmodel.QuestionStepState
import com.example.ui.viewmodel.ReviewSessionStatus
import com.example.ui.viewmodel.ReviewSessionUiState
import com.example.ui.viewmodel.ReviewSessionViewModel
import com.example.util.SoundVibrationHelper

/**
 * Màn hình Phiên ôn tập câu sai (F08):
 * - Chọn nhanh cấu hình: tất cả đến hạn / theo môn / số câu (5/10/20)
 * - Tái sử dụng trọn vẹn 6 Composable câu hỏi (F05)
 * - Xáo trộn phương án trắc nghiệm chống học vẹt
 * - Gợi ý, lời giải chi tiết, phản hồi nhẹ khi đúng
 * - Thông báo và hiệu ứng chúc mừng "Câu này đã thuộc!" khi đạt ngưỡng 3 lần đúng cách ngày
 * - Kết quả phiên: số câu mới đã thuộc, số câu còn cần ôn, nút Ôn tiếp
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSessionScreen(
    grade: Int = 6,
    initialSubjectId: String? = null,
    initialChapterId: String? = null,
    initialCount: Int = 10,
    initialQuestionIds: List<String>? = null,
    initialTitle: String = "Ôn tập câu sai",
    viewModel: ReviewSessionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    // Tự động khởi tạo phiên ôn tập nếu được gọi kèm danh sách câu hỏi cụ thể hoặc tham số
    LaunchedEffect(initialSubjectId, initialChapterId, initialCount, initialQuestionIds) {
        if (uiState.status == ReviewSessionStatus.Idle) {
            viewModel.startReviewSession(
                grade = grade,
                subjectId = if (initialSubjectId != "none") initialSubjectId else null,
                chapterId = if (initialChapterId != "all") initialChapterId else null,
                count = initialCount,
                questionIds = initialQuestionIds,
                title = initialTitle
            )
        }
    }

    // Xử lý nút quay lại
    BackHandler(enabled = uiState.status == ReviewSessionStatus.InProgress) {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Dừng phiên ôn tập?", fontWeight = FontWeight.Bold) },
            text = { Text("Tiến độ các câu đã làm sẽ được lưu vào Ngân hàng câu sai. Bạn có chắc muốn dừng?") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.exitSession()
                        onNavigateBack()
                    },
                    modifier = Modifier.testTag("confirm_exit_review_button")
                ) {
                    Text("Dừng ôn tập")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Tiếp tục làm")
                }
            }
        )
    }

    // Hộp thoại gợi ý
    if (uiState.showHintDialog) {
        val currentQ = uiState.currentQuestion
        AlertDialog(
            onDismissRequest = { viewModel.toggleHint() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFC08A52),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Gợi ý ôn tập", fontWeight = FontWeight.Bold) },
            text = {
                val hint = currentQ?.hint?.ifBlank { "Hãy nhớ lại định nghĩa và phương pháp giải đã học trong bài." }
                    ?: "Hãy đọc kỹ đề bài."
                FormattedFormulaText(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.toggleHint() }) {
                    Text("Đã hiểu")
                }
            }
        )
    }

    when (uiState.status) {
        is ReviewSessionStatus.Loading -> {
            Scaffold { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.testTag("review_session_loading"))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Đang chuẩn bị câu hỏi ôn tập...")
                    }
                }
            }
        }

        is ReviewSessionStatus.Error -> {
            val errorMsg = (uiState.status as ReviewSessionStatus.Error).message
            Scaffold { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMsg,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.exitSession(); onNavigateBack() }) {
                            Text("Quay lại")
                        }
                    }
                }
            }
        }

        is ReviewSessionStatus.Completed -> {
            ReviewSessionResultScreen(
                uiState = uiState,
                onReviewAgain = {
                    viewModel.startReviewSession(
                        grade = grade,
                        subjectId = if (initialSubjectId != "none") initialSubjectId else null,
                        chapterId = if (initialChapterId != "all") initialChapterId else null,
                        count = initialCount,
                        questionIds = initialQuestionIds,
                        title = initialTitle
                    )
                },
                onGoWrongBank = {
                    viewModel.exitSession()
                    onNavigateBack()
                }
            )
        }

        is ReviewSessionStatus.InProgress -> {
            ReviewSessionInProgressContent(
                uiState = uiState,
                onClose = { showExitDialog = true },
                onSelectAnswer = { viewModel.selectAnswer(it) },
                onSubmitAnswer = {
                    viewModel.submitCurrentAnswer()
                    SoundVibrationHelper.playCorrectSound(uiState.soundEnabled)
                    SoundVibrationHelper.vibrateCorrect(context, uiState.vibrationEnabled)
                },
                onNextQuestion = { viewModel.nextQuestion() },
                onToggleHint = { viewModel.toggleHint() }
            )
        }

        is ReviewSessionStatus.Idle -> {
            // Trường hợp mở mà chưa cấu hình: hiển thị bộ chọn nhanh
            ReviewSessionQuickConfigScreen(
                grade = grade,
                onStartSession = { sId, cId, count, title ->
                    viewModel.startReviewSession(grade, sId, cId, count, null, title)
                },
                onNavigateBack = onNavigateBack
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewSessionInProgressContent(
    uiState: ReviewSessionUiState,
    onClose: () -> Unit,
    onSelectAnswer: (UserAnswer) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onToggleHint: () -> Unit
) {
    val context = LocalContext.current
    val currentQ = uiState.currentQuestion ?: return
    val progress = (uiState.currentIndex + 1).toFloat() / uiState.totalCount.toFloat()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.sessionTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Câu ${uiState.currentIndex + 1} / ${uiState.totalCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose, modifier = Modifier.testTag("review_close_button")) {
                        Icon(Icons.Default.Close, contentDescription = "Thoát phiên ôn")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleHint, modifier = Modifier.testTag("review_hint_button")) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Gợi ý",
                            tint = Color(0xFFC08A52)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    when (uiState.stepState) {
                        is QuestionStepState.Answering -> {
                            val canSubmit = uiState.currentUserAnswer != null
                            Button(
                                onClick = onSubmitAnswer,
                                enabled = canSubmit,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("review_submit_answer_button")
                            ) {
                                Text(
                                    text = "Kiểm tra đáp án",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        is QuestionStepState.Graded -> {
                            val isLast = uiState.currentIndex >= uiState.totalCount - 1
                            Button(
                                onClick = onNextQuestion,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("review_next_question_button")
                            ) {
                                Text(
                                    text = if (isLast) "Xem kết quả phiên ôn" else "Câu tiếp theo",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        else -> {}
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Thanh tiến độ làm bài
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Hiệu ứng / Banner thông báo "Câu này đã thuộc!" khi vừa đạt ngưỡng 3 lần đúng
            AnimatedVisibility(
                visible = uiState.isNewlyMasteredThisQuestion,
                enter = fadeIn() + scaleIn(spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut() + scaleOut()
            ) {
                Surface(
                    color = CorrectGreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_newly_mastered")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = GoldenStar,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tuyệt vời! Câu hỏi này đã thuộc!",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Thẻ câu hỏi
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        FormattedFormulaText(
                            text = currentQ.content,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 26.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Giao diện nhập câu trả lời tái sử dụng từ F05
                val isGraded = uiState.stepState is QuestionStepState.Graded
                val currentGradeResult = (uiState.stepState as? QuestionStepState.Graded)?.result

                when (currentQ) {
                    is Question.MultipleChoice -> {
                        MultipleChoiceQuestionUi(
                            question = currentQ,
                            userAnswer = uiState.currentUserAnswer as? UserAnswer.MultipleChoice,
                            gradeResult = currentGradeResult,
                            isReadOnly = isGraded,
                            onAnswerSelected = { option ->
                                SoundVibrationHelper.vibrateClick(context, uiState.vibrationEnabled)
                                onSelectAnswer(UserAnswer.MultipleChoice(option))
                            }
                        )
                    }

                    is Question.TrueFalse -> {
                        TrueFalseQuestionUi(
                            question = currentQ,
                            userAnswer = uiState.currentUserAnswer as? UserAnswer.TrueFalse,
                            gradeResult = currentGradeResult,
                            isReadOnly = isGraded,
                            onAnswerSelected = { boolVal ->
                                SoundVibrationHelper.vibrateClick(context, uiState.vibrationEnabled)
                                onSelectAnswer(UserAnswer.TrueFalse(boolVal))
                            }
                        )
                    }

                    is Question.FillBlank -> {
                        FillBlankQuestionUi(
                            question = currentQ,
                            userAnswer = uiState.currentUserAnswer as? UserAnswer.FillBlank,
                            gradeResult = currentGradeResult,
                            isReadOnly = isGraded,
                            onTextChanged = { onSelectAnswer(UserAnswer.FillBlank(it)) },
                            onSubmit = {
                                if (uiState.currentUserAnswer != null && !isGraded) {
                                    onSubmitAnswer()
                                }
                            }
                        )
                    }

                    is Question.Matching -> {
                        MatchingQuestionUi(
                            question = currentQ,
                            userAnswer = uiState.currentUserAnswer as? UserAnswer.Matching,
                            gradeResult = currentGradeResult,
                            isReadOnly = isGraded,
                            onPairsUpdated = { pairs ->
                                SoundVibrationHelper.vibrateClick(context, uiState.vibrationEnabled)
                                onSelectAnswer(UserAnswer.Matching(pairs))
                            }
                        )
                    }

                    is Question.Ordering -> {
                        OrderingQuestionUi(
                            question = currentQ,
                            userAnswer = uiState.currentUserAnswer as? UserAnswer.Ordering,
                            gradeResult = currentGradeResult,
                            isReadOnly = isGraded,
                            onOrderUpdated = { newOrder ->
                                SoundVibrationHelper.vibrateClick(context, uiState.vibrationEnabled)
                                onSelectAnswer(UserAnswer.Ordering(newOrder))
                            }
                        )
                    }

                    is Question.ShortAnswer -> {
                        ShortAnswerQuestionUi(
                            question = currentQ,
                            userAnswer = uiState.currentUserAnswer as? UserAnswer.ShortAnswer,
                            gradeResult = currentGradeResult,
                            isReadOnly = isGraded,
                            onTextChanged = { onSelectAnswer(UserAnswer.ShortAnswer(it)) },
                            onSelfEvaluate = { isCorrect ->
                                if (isCorrect) {
                                    SoundVibrationHelper.playCorrectSound(uiState.soundEnabled)
                                    SoundVibrationHelper.vibrateCorrect(context, uiState.vibrationEnabled)
                                } else {
                                    SoundVibrationHelper.playIncorrectSound(uiState.soundEnabled)
                                    SoundVibrationHelper.vibrateIncorrect(context, uiState.vibrationEnabled)
                                }
                            }
                        )
                    }
                }

                // Hiển thị lời giải sau khi chấm điểm
                if (uiState.stepState is QuestionStepState.Graded) {
                    val result = (uiState.stepState as QuestionStepState.Graded).result
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.isCorrect) CorrectGreen.copy(alpha = 0.1f) else IncorrectRed.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("review_answer_feedback_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (result.isCorrect) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (result.isCorrect) CorrectGreen else IncorrectRed
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (result.isCorrect) "Chính xác!" else "Chưa chính xác",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (result.isCorrect) CorrectGreen else IncorrectRed
                                    )
                                )
                            }

                            if (!result.isCorrect && result.correctDisplayAnswer.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Đáp án đúng: ${result.correctDisplayAnswer}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }

                            if (result.explanation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                FormattedFormulaText(
                                    text = "Lời giải: ${result.explanation}",
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ReviewSessionResultScreen(
    uiState: ReviewSessionUiState,
    onReviewAgain: () -> Unit,
    onGoWrongBank: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Hoàn thành phiên ôn tập!",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Bạn đã ôn lại ${uiState.totalCount} câu hỏi.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Thẻ thống kê kết quả phiên ôn
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CorrectGreen.copy(alpha = 0.12f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${uiState.newlyMasteredCount}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CorrectGreen
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Câu đã thuộc mới",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            textAlign = TextAlign.Center
                        )
                        if (uiState.newlyMasteredCount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CorrectGreen.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "+${uiState.newlyMasteredCount * 3} XP",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = CorrectGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${uiState.correctCount}/${uiState.totalCount}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Câu làm đúng",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Các nút hành động
            Button(
                onClick = onReviewAgain,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("review_again_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ôn tiếp các câu khác", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onGoWrongBank,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("back_to_wrong_bank_button")
            ) {
                Text("Về Ngân hàng câu sai", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewSessionQuickConfigScreen(
    grade: Int,
    onStartSession: (subjectId: String?, chapterId: String?, count: Int, title: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedCount by remember { mutableIntStateOf(10) }
    var selectedSubjectId by remember { mutableStateOf<String?>("all") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thiết lập phiên ôn tập", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Chọn số lượng câu ôn tập:",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(5, 10, 20).forEach { count ->
                    FilterChip(
                        selected = selectedCount == count,
                        onClick = { selectedCount = count },
                        label = { Text("$count câu") },
                        modifier = Modifier.testTag("config_count_$count")
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    onStartSession(
                        if (selectedSubjectId == "all") null else selectedSubjectId,
                        null,
                        selectedCount,
                        "Ôn tập $selectedCount câu"
                    )
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_quick_review_button")
            ) {
                Text("Bắt đầu ôn tập", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReviewSessionScreenPreview() {
    HocTotTHCSTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Preview ReviewSessionScreen")
        }
    }
}
