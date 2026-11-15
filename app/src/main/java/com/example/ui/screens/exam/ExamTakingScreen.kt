package com.example.ui.screens.exam

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.QuestionType
import com.example.ui.components.FormattedFormulaText
import com.example.ui.theme.CorrectGreen
import com.example.ui.theme.GoldenStar
import com.example.ui.theme.IncorrectRed
import com.example.ui.viewmodel.ExamViewModel
import com.example.util.SoundVibrationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamTakingScreen(
    subjectId: String,
    title: String,
    viewModel: ExamViewModel,
    onNavigateBack: () -> Unit,
    onRetryWrongQuestions: ((List<String>) -> Unit)? = null
) {
    val context = LocalContext.current
    val state by viewModel.examState.collectAsState()
    var showSubmitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(subjectId, title) {
        viewModel.startExam(title = title, subjectId = subjectId, durationMinutes = 15, questionCount = 10)
    }

    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("Nộp bài thi?") },
            text = {
                val answeredCount = state.userAnswers.size
                Text("Bạn đã làm $answeredCount / ${state.questions.size} câu hỏi. Bạn có chắc chắn muốn nộp bài để chấm điểm không?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        viewModel.submitExam()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Nộp bài")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("Làm tiếp")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isSubmitted) "Kết quả bài kiểm tra" else state.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("exam_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Thoát")
                    }
                },
                actions = {
                    if (!state.isSubmitted && state.questions.isNotEmpty()) {
                        // Countdown Timer Pill
                        val minutes = state.remainingSeconds / 60
                        val seconds = state.remainingSeconds % 60
                        val timerText = String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
                        val timerColor = if (state.remainingSeconds < 120) IncorrectRed else MaterialTheme.colorScheme.primary

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = timerColor.copy(alpha = 0.12f),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = timerColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = timerText,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = timerColor
                                    ),
                                    modifier = Modifier.testTag("exam_timer_text")
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Đang chuẩn bị đề kiểm tra...")
                    }
                }
                state.questions.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Không tìm thấy câu hỏi cho đề kiểm tra này.")
                    }
                }
                state.isSubmitted -> {
                    // Result & Full Review View
                    ExamResultReview(
                        state = state,
                        onBack = onNavigateBack,
                        onRetryWrongQuestions = onRetryWrongQuestions
                    )
                }
                else -> {
                    // Active exam taking
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 80.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Question Selector Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(state.questions) { index, _ ->
                                val isCurrent = index == state.currentIndex
                                val isAnswered = state.userAnswers.containsKey(index)

                                val bgColor = when {
                                    isCurrent -> MaterialTheme.colorScheme.primary
                                    isAnswered -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                }
                                val textColor = when {
                                    isCurrent -> Color.White
                                    isAnswered -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = bgColor,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .clickable { viewModel.jumpToQuestion(index) }
                                        .testTag("exam_question_nav_$index")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val currentQ = state.questions[state.currentIndex]
                        val qType = QuestionType.fromCode(currentQ.type)
                        val options = viewModel.parseOptions(currentQ.optionsJson)
                        val selectedAns = state.userAnswers[state.currentIndex] ?: ""

                        // Question Card
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "Câu ${state.currentIndex + 1} (${qType.label})",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                FormattedFormulaText(
                                    text = currentQ.content,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        lineHeight = 24.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Answer Options
                        when (qType) {
                            QuestionType.MULTIPLE_CHOICE -> {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    options.forEach { opt ->
                                        val isSelected = selectedAns == opt
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.5.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable {
                                                    SoundVibrationHelper.vibrateClick(context)
                                                    viewModel.selectAnswerForCurrentQuestion(opt)
                                                }
                                                .testTag("exam_opt_$opt")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(14.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .border(
                                                            2.dp,
                                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                            CircleShape
                                                        )
                                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = opt,
                                                    style = MaterialTheme.typography.bodyLarge.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            QuestionType.TRUE_FALSE -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    listOf("Đúng", "Sai").forEach { choice ->
                                        val isSelected = selectedAns == choice
                                        val choiceColor = if (choice == "Đúng") CorrectGreen else IncorrectRed

                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) choiceColor else MaterialTheme.colorScheme.surface,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.5.dp,
                                                if (isSelected) choiceColor else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(64.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable {
                                                    SoundVibrationHelper.vibrateClick(context)
                                                    viewModel.selectAnswerForCurrentQuestion(choice)
                                                }
                                                .testTag("exam_tf_$choice")
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = choice,
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            QuestionType.FILL_BLANK,
                            QuestionType.MATCHING,
                            QuestionType.SORTING,
                            QuestionType.SHORT_ESSAY -> {
                                OutlinedTextField(
                                    value = selectedAns,
                                    onValueChange = { viewModel.selectAnswerForCurrentQuestion(it) },
                                    label = { Text("Nhập câu trả lời") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("exam_fill_blank_input")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Bottom Navigation Bar for Next/Prev/Submit
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.prevQuestion() },
                                enabled = state.currentIndex > 0,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Câu trước")
                            }

                            Button(
                                onClick = { showSubmitDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("exam_submit_button")
                            ) {
                                Text("Nộp bài")
                            }

                            Button(
                                onClick = { viewModel.nextQuestion() },
                                enabled = state.currentIndex + 1 < state.questions.size,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Câu sau")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExamResultReview(
    state: com.example.ui.viewmodel.ExamTakingState,
    onBack: () -> Unit,
    onRetryWrongQuestions: ((List<String>) -> Unit)? = null
) {
    val rating = when {
        state.score >= 9.0f -> "Xuất sắc"
        state.score >= 8.0f -> "Giỏi"
        state.score >= 6.5f -> "Khá"
        state.score >= 5.0f -> "Trung bình"
        else -> "Cần cố gắng"
    }
    val scoreColor = when {
        state.score >= 8.0f -> CorrectGreen
        state.score >= 6.5f -> Color(0xFF4A7FA8)
        state.score >= 5.0f -> Color(0xFFC08A52)
        else -> IncorrectRed
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Score Header Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = scoreColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(90.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${state.score}",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = scoreColor,
                                fontSize = 34.sp
                            ),
                            modifier = Modifier.testTag("exam_final_score_text")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = scoreColor
                ) {
                    Text(
                        text = "Xếp loại: $rating",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Đúng", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${state.correctCount}/${state.totalQuestions}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Thời gian", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val spent = state.totalTimeSeconds - state.remainingSeconds
                        Text(text = "${spent / 60}p ${spent % 60}s", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Xem lại chi tiết từng câu hỏi",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Question breakdown
        state.questions.forEachIndexed { index, question ->
            val userAns = state.userAnswers[index]?.trim() ?: "Chưa trả lời"
            val isCorrect = userAns.equals(question.correctAnswer.trim(), ignoreCase = true)

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Câu ${index + 1}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isCorrect) Color(0xFFDCFCE7) else Color(0xFFFFE4E6)
                        ) {
                            Text(
                                text = if (isCorrect) "Đúng" else "Sai",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrect) CorrectGreen else IncorrectRed
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = question.content, style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Đáp án của bạn: $userAns",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isCorrect) CorrectGreen else IncorrectRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    if (!isCorrect) {
                        Text(
                            text = "Đáp án đúng: ${question.correctAnswer}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CorrectGreen,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    if (question.explanation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = question.explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val wrongQuestions = state.questions.filterIndexed { index, q ->
            val userAns = state.userAnswers[index] ?: ""
            !userAns.equals(q.correctAnswer, ignoreCase = true)
        }

        if (wrongQuestions.isNotEmpty() && onRetryWrongQuestions != null) {
            Button(
                onClick = { onRetryWrongQuestions(wrongQuestions.map { it.id }) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IncorrectRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("exam_retry_wrong_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Làm lại ${wrongQuestions.size} câu sai trong đề thi",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Button(
            onClick = onBack,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("exam_return_button")
        ) {
            Text("Quay về trang Luyện tập", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
