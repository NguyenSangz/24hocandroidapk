package com.example.ui.screens.practice

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.domain.model.MatchingPair
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
import com.example.ui.viewmodel.PracticeSessionStatus
import com.example.ui.viewmodel.PracticeUiState
import com.example.ui.viewmodel.PracticeViewModel
import com.example.ui.viewmodel.QuestionStepState
import com.example.util.SoundVibrationHelper

/**
 * Màn hình làm bài luyện tập từng câu hỏi (F06)
 * Quản lý vòng đời câu hỏi, phản hồi nhẹ nhàng, hộp thoại xác nhận thoát, âm thanh và rung.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeQuizScreen(
    lessonId: String = "none",
    subjectId: String = "none",
    chapterId: String = "all",
    count: Int = 10,
    difficulty: String = "MIXED",
    viewModel: PracticeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReviewSession: ((List<String>) -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }

    // Tự động khởi tạo phiên luyện tập nếu chưa chạy
    LaunchedEffect(lessonId, subjectId, chapterId, count, difficulty) {
        if (uiState.status == PracticeSessionStatus.Idle) {
            viewModel.startPracticeSession(
                lessonId = if (lessonId != "none") lessonId else null,
                subjectId = if (subjectId != "none") subjectId else null,
                chapterId = if (chapterId != "all") chapterId else null,
                count = count,
                difficultyFilter = difficulty
            )
        }
    }

    // Xử lý nút Back của hệ thống
    BackHandler(enabled = uiState.status == PracticeSessionStatus.InProgress) {
        showExitDialog = true
    }

    // Hộp thoại xác nhận thoát
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = "Dừng phiên luyện tập?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Tiến độ của các câu chưa nộp sẽ không được lưu. Bạn có chắc muốn thoát không?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        viewModel.exitSession()
                        onNavigateBack()
                    },
                    modifier = Modifier.testTag("confirm_exit_session_button")
                ) {
                    Text("Dừng & Thoát", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showExitDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("cancel_exit_session_button")
                ) {
                    Text("Tiếp tục làm")
                }
            }
        )
    }

    // Hộp thoại Gợi ý
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
            title = {
                Text(
                    text = "Gợi ý cho bạn",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                val hintText = currentQ?.hint?.ifBlank { "Hãy đọc kỹ đề bài và vận dụng kiến thức lý thuyết đã học." }
                    ?: "Hãy đọc kỹ đề bài."
                FormattedFormulaText(
                    text = hintText,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.toggleHint() },
                    modifier = Modifier.testTag("close_hint_dialog_button")
                ) {
                    Text("Đã hiểu")
                }
            }
        )
    }

    when (uiState.status) {
        is PracticeSessionStatus.Loading -> {
            Scaffold { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Đang chuẩn bị câu hỏi luyện tập...")
                    }
                }
            }
        }

        is PracticeSessionStatus.Error -> {
            val errorMsg = (uiState.status as PracticeSessionStatus.Error).message
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

        is PracticeSessionStatus.Completed -> {
            PracticeResultScreen(
                correctCount = uiState.correctCount,
                totalCount = uiState.questions.size,
                totalTimeSpentSeconds = uiState.totalTimeSpentSeconds,
                records = uiState.questionRecords,
                earnedXp = uiState.sessionEarnedXp,
                gamificationReward = uiState.gamificationReward,
                onRetryWrongQuestions = {
                    val wrongIds = uiState.questionRecords
                        .filter { !it.gradeResult.isCorrect || it.gradeResult.score < 1.0f }
                        .map { it.question.id }
                    if (onNavigateToReviewSession != null && wrongIds.isNotEmpty()) {
                        onNavigateToReviewSession(wrongIds)
                    } else {
                        viewModel.retryWrongQuestions()
                    }
                },
                onPracticeAgain = {
                    viewModel.startPracticeSession(
                        lessonId = if (lessonId != "none") lessonId else null,
                        subjectId = if (subjectId != "none") subjectId else null,
                        chapterId = if (chapterId != "all") chapterId else null,
                        count = count,
                        difficultyFilter = difficulty
                    )
                },
                onGoHome = {
                    viewModel.exitSession()
                    onNavigateBack()
                }
            )
        }

        is PracticeSessionStatus.InProgress -> {
            val currentQuestion = uiState.currentQuestion
            if (currentQuestion == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy câu hỏi.")
                }
                return
            }

            val isGraded = uiState.stepState is QuestionStepState.Graded
            val currentGradeResult = (uiState.stepState as? QuestionStepState.Graded)?.result

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Câu ${uiState.currentIndex + 1} / ${uiState.questions.size}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { showExitDialog = true },
                                modifier = Modifier.testTag("practice_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Thoát phiên luyện tập"
                                )
                            }
                        },
                        actions = {
                            // Nút Gợi ý
                            if (currentQuestion.hint.isNotBlank()) {
                                IconButton(
                                    onClick = { viewModel.toggleHint() },
                                    modifier = Modifier.testTag("practice_hint_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = "Xem gợi ý",
                                        tint = Color(0xFFC08A52)
                                    )
                                }
                            }

                            // Huy hiệu số câu đúng
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = GoldenStar,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${uiState.correctCount} đúng",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    // Thanh tiến độ phiên luyện tập
                    val targetProgress = (uiState.currentIndex.toFloat()) / (uiState.questions.size.toFloat().coerceAtLeast(1f))
                    val animatedProgress by animateFloatAsState(targetValue = targetProgress, label = "progress")

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Độ khó & nhãn dạng câu hỏi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = when (currentQuestion) {
                                    is Question.MultipleChoice -> "Trắc nghiệm"
                                    is Question.TrueFalse -> "Đúng / Sai"
                                    is Question.FillBlank -> "Điền khuyết"
                                    is Question.Matching -> "Nối cặp"
                                    is Question.Ordering -> "Sắp xếp thứ tự"
                                    is Question.ShortAnswer -> "Tự luận ngắn"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = currentQuestion.difficulty.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Khung đề câu hỏi
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("question_content_card")
                    ) {
                        Box(modifier = Modifier.padding(18.dp)) {
                            FormattedFormulaText(
                                text = currentQuestion.content,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 24.sp,
                                    fontSize = 17.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Giao diện nhập câu trả lời tương ứng với 6 dạng câu hỏi
                    when (currentQuestion) {
                        is Question.MultipleChoice -> {
                            MultipleChoiceQuestionUi(
                                question = currentQuestion,
                                userAnswer = uiState.currentUserAnswer as? UserAnswer.MultipleChoice,
                                gradeResult = currentGradeResult,
                                isReadOnly = isGraded,
                                onAnswerSelected = { option ->
                                    SoundVibrationHelper.vibrateClick(context, uiState.vibrationEnabled)
                                    viewModel.selectAnswer(UserAnswer.MultipleChoice(option))
                                }
                            )
                        }
                        is Question.TrueFalse -> {
                            TrueFalseQuestionUi(
                                question = currentQuestion,
                                userAnswer = uiState.currentUserAnswer as? UserAnswer.TrueFalse,
                                gradeResult = currentGradeResult,
                                isReadOnly = isGraded,
                                onAnswerSelected = { boolVal ->
                                    SoundVibrationHelper.vibrateClick(context, uiState.vibrationEnabled)
                                    viewModel.selectAnswer(UserAnswer.TrueFalse(boolVal))
                                }
                            )
                        }
                        is Question.FillBlank -> {
                            FillBlankQuestionUi(
                                question = currentQuestion,
                                userAnswer = uiState.currentUserAnswer as? UserAnswer.FillBlank,
                                gradeResult = currentGradeResult,
                                isReadOnly = isGraded,
                                onTextChanged = { viewModel.selectAnswer(UserAnswer.FillBlank(it)) },
                                onSubmit = {
                                    if (uiState.currentUserAnswer != null && !isGraded) {
                                        viewModel.submitCurrentAnswer()
                                    }
                                }
                            )
                        }
                        is Question.Matching -> {
                            MatchingQuestionUi(
                                question = currentQuestion,
                                userAnswer = uiState.currentUserAnswer as? UserAnswer.Matching,
                                gradeResult = currentGradeResult,
                                isReadOnly = isGraded,
                                onPairsUpdated = { pairs ->
                                    SoundVibrationHelper.vibrateClick(context, uiState.vibrationEnabled)
                                    viewModel.selectAnswer(UserAnswer.Matching(pairs))
                                }
                            )
                        }
                        is Question.Ordering -> {
                            OrderingQuestionUi(
                                question = currentQuestion,
                                userAnswer = uiState.currentUserAnswer as? UserAnswer.Ordering,
                                gradeResult = currentGradeResult,
                                isReadOnly = isGraded,
                                onOrderUpdated = { newOrder ->
                                    SoundVibrationHelper.vibrateClick(context, uiState.vibrationEnabled)
                                    viewModel.selectAnswer(UserAnswer.Ordering(newOrder))
                                }
                            )
                        }
                        is Question.ShortAnswer -> {
                            ShortAnswerQuestionUi(
                                question = currentQuestion,
                                userAnswer = uiState.currentUserAnswer as? UserAnswer.ShortAnswer,
                                gradeResult = currentGradeResult,
                                isReadOnly = isGraded,
                                onTextChanged = { viewModel.selectAnswer(UserAnswer.ShortAnswer(it)) },
                                onSelfEvaluate = { isCorrect ->
                                    viewModel.selfEvaluateShortAnswer(isCorrect)
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // Nút "Kiểm tra đáp án" khi chưa chấm
                    if (!isGraded) {
                        val isAnswerProvided = when (val a = uiState.currentUserAnswer) {
                            is UserAnswer.MultipleChoice -> a.selectedOption.isNotBlank()
                            is UserAnswer.TrueFalse -> true
                            is UserAnswer.FillBlank -> a.filledText.isNotBlank()
                            is UserAnswer.Matching -> a.userPairs.isNotEmpty()
                            is UserAnswer.Ordering -> a.userOrder.isNotEmpty()
                            is UserAnswer.ShortAnswer -> a.submittedText.isNotBlank()
                            null -> false
                        }

                        Button(
                            onClick = {
                                viewModel.submitCurrentAnswer()
                                // Hiệu ứng âm thanh và rung
                                val res = (viewModel.uiState.value.stepState as? QuestionStepState.Graded)?.result
                                if (res?.isCorrect == true) {
                                    SoundVibrationHelper.playCorrectSound(uiState.soundEnabled)
                                    SoundVibrationHelper.vibrateCorrect(context, uiState.vibrationEnabled)
                                } else {
                                    SoundVibrationHelper.playIncorrectSound(uiState.soundEnabled)
                                    SoundVibrationHelper.vibrateIncorrect(context, uiState.vibrationEnabled)
                                }
                            },
                            enabled = isAnswerProvided,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("check_answer_button")
                        ) {
                            Text(
                                text = "Kiểm tra đáp án",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    // Khung phản hồi kết quả sau khi đã chấm
                    if (isGraded && currentGradeResult != null) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + scaleIn(
                                initialScale = 0.95f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                        ) {
                            PostAnswerFeedbackCard(
                                gradeResult = currentGradeResult,
                                isLastQuestion = uiState.currentIndex == uiState.questions.size - 1,
                                onNextQuestion = { viewModel.nextQuestion() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }

        PracticeSessionStatus.Idle -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Đang tải dữ liệu...")
            }
        }
    }
}

/**
 * Thẻ phản hồi kết quả sau khi chấm câu hỏi
 */
@Composable
private fun PostAnswerFeedbackCard(
    gradeResult: GradeResult,
    isLastQuestion: Boolean,
    onNextQuestion: () -> Unit
) {
    val isCorrect = gradeResult.isCorrect
    val accentColor = if (isCorrect) CorrectGreen else IncorrectRed
    val containerColor = if (isCorrect) Color(0xFFDCFCE7) else Color(0xFFFFE4E6)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_answer_feedback_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = accentColor,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = if (isCorrect) "Chính xác! Xuất sắc 🎉" else "Chưa chính xác, cố lên nhé! 💪",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        fontSize = 17.sp
                    )
                )
            }

            if (!isCorrect && gradeResult.correctDisplayAnswer.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Đáp án đúng:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                FormattedFormulaText(
                    text = gradeResult.correctDisplayAnswer,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF14532D)
                    )
                )
            }

            // Gợi ý khắc phục lỗi sai khi trả lời chưa chính xác
            if (!isCorrect) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 Gợi ý khắc phục lỗi sai:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Hãy đọc lại lý thuyết trọng tâm và ghi nhớ công thức hoặc quy tắc cốt lõi của dạng bài này để tránh lặp lại lỗi ở các câu sau nhé!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }

            // Lời giải chi tiết
            if (gradeResult.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = Color(0xFFC08A52),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lời giải chi tiết:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        FormattedFormulaText(
                            text = gradeResult.explanation,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nút "Câu tiếp theo" / "Xem kết quả"
            Button(
                onClick = onNextQuestion,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("next_question_button")
            ) {
                Text(
                    text = if (isLastQuestion) "Xem kết quả bài luyện" else "Câu tiếp theo",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedbackCardPreview() {
    HocTotTHCSTheme {
        PostAnswerFeedbackCard(
            gradeResult = GradeResult(
                status = GradeStatus.CORRECT,
                score = 1.0f,
                isCorrect = true,
                correctDisplayAnswer = "x = 5",
                explanation = "Ta chuyển vế đổi dấu: 2x = 10 suy ra x = 5."
            ),
            isLastQuestion = false,
            onNextQuestion = {}
        )
    }
}
