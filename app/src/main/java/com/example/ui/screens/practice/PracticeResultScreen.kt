package com.example.ui.screens.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.domain.gamification.GamificationReward
import com.example.ui.components.gamification.LevelUpCelebrationDialog
import com.example.ui.components.gamification.NewBadgeUnlockedDialog
import com.example.domain.model.GradeResult
import com.example.domain.model.GradeStatus
import com.example.domain.model.Question
import com.example.domain.model.UserAnswer
import com.example.ui.components.FormattedFormulaText
import com.example.ui.theme.CorrectGreen
import com.example.ui.theme.GoldenStar
import com.example.ui.theme.HocTotTHCSTheme
import com.example.ui.theme.IncorrectRed

/**
 * Bản ghi kết quả một câu hỏi đã làm trong phiên
 */
data class AttemptQuestionRecord(
    val question: Question,
    val userAnswer: UserAnswer?,
    val gradeResult: GradeResult,
    val usedHint: Boolean,
    val timeSpentSeconds: Int
)

/**
 * Màn hình kết quả phiên luyện tập:
 * - Tổng số câu đúng / sai, %, thời gian làm bài
 * - Lời khen ngợi sinh động phù hợp mức điểm
 * - Danh sách câu hỏi kèm đáp án & lời giải chi tiết có thể bấm mở rộng
 * - Nút "Làm lại các câu sai", "Luyện tập tiếp", "Về trang chủ"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeResultScreen(
    correctCount: Int,
    totalCount: Int,
    totalTimeSpentSeconds: Long,
    records: List<AttemptQuestionRecord>,
    onRetryWrongQuestions: () -> Unit,
    onPracticeAgain: () -> Unit,
    onGoHome: () -> Unit,
    earnedXp: Int = 0,
    gamificationReward: GamificationReward? = null,
    modifier: Modifier = Modifier
) {
    val percentage = if (totalCount > 0) (correctCount.toFloat() / totalCount.toFloat()) * 100f else 0f
    val wrongRecords = records.filter { !it.gradeResult.isCorrect }

    // Bản đồ trạng thái mở rộng của từng câu hỏi
    val expandedMap = remember { mutableStateMapOf<Int, Boolean>() }
    var showLevelUpDialog by remember { mutableStateOf(gamificationReward?.isLevelUp == true) }
    var pendingBadges by remember { mutableStateOf(gamificationReward?.newlyUnlockedBadges ?: emptyList()) }

    if (showLevelUpDialog && gamificationReward?.newLevelInfo != null) {
        LevelUpCelebrationDialog(
            newLevelInfo = gamificationReward.newLevelInfo,
            onDismiss = { showLevelUpDialog = false }
        )
    } else if (pendingBadges.isNotEmpty()) {
        val currentBadge = pendingBadges.first()
        NewBadgeUnlockedDialog(
            badge = currentBadge,
            onDismiss = {
                pendingBadges = pendingBadges.drop(1)
            }
        )
    }

    val praiseMessage = when {
        percentage >= 90f -> "Xuất sắc! Bạn nắm kiến thức vô cùng vững vàng! 🌟"
        percentage >= 70f -> "Rất tốt! Bạn đã hiểu bài sâu sắc, tiếp tục phát huy nhé! 👏"
        percentage >= 50f -> "Khá lắm! Hãy ôn lại các câu chưa đúng để tiến bộ hơn nhé! 📚"
        else -> "Đừng nản lòng! Hãy xem lại lời giải chi tiết và thử lại nhé! 💪"
    }

    val minutes = totalTimeSpentSeconds / 60
    val seconds = totalTimeSpentSeconds % 60
    val timeText = if (minutes > 0) "${minutes}p ${seconds}s" else "${seconds}s"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kết quả luyện tập", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onGoHome, modifier = Modifier.testTag("result_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trang chủ")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("practice_result_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Thẻ tổng quan điểm số
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = GoldenStar,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = praiseMessage,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Hàng thống kê: Điểm số, %, XP, Thời gian
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatBox(
                                title = "Đúng",
                                value = "$correctCount / $totalCount",
                                color = CorrectGreen
                            )
                            StatBox(
                                title = "Tỷ lệ",
                                value = "${percentage.toInt()}%",
                                color = MaterialTheme.colorScheme.primary
                            )
                            StatBox(
                                title = "Kinh nghiệm",
                                value = "+$earnedXp XP",
                                color = Color(0xFFC08A52)
                            )
                            StatBox(
                                title = "Thời gian",
                                value = timeText,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }

            // 2. Các nút hành động chính
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Nút làm lại các câu sai (nếu có câu sai)
                    if (wrongRecords.isNotEmpty()) {
                        Button(
                            onClick = onRetryWrongQuestions,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC26161)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("retry_wrong_questions_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Làm lại ${wrongRecords.size} câu sai",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onPracticeAgain,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("practice_again_button")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Luyện tiếp",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        OutlinedButton(
                            onClick = onGoHome,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("result_go_home_button")
                        ) {
                            Icon(imageVector = Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Trang chủ",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // 3. Tiêu đề danh sách xem lại câu đã làm
            if (records.isNotEmpty()) {
                item {
                    Text(
                        text = "Chi tiết từng câu hỏi (${records.size} câu):",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                itemsIndexed(records) { index, record ->
                    val isExpanded = expandedMap[index] == true
                    val isCorrect = record.gradeResult.isCorrect
                    val statusText = when (record.gradeResult.status) {
                        GradeStatus.CORRECT -> "Đúng"
                        GradeStatus.PARTIALLY_CORRECT -> "Đúng một phần"
                        GradeStatus.INCORRECT -> "Chưa đúng"
                        GradeStatus.SELF_EVALUATION -> if (isCorrect) "Đúng (Tự chấm)" else "Chưa đúng"
                    }

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                expandedMap[index] = !isExpanded
                            }
                            .testTag("result_review_item_$index")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (isCorrect) CorrectGreen else IncorrectRed),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Câu ${index + 1}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    if (record.usedHint) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(Có dùng gợi ý)",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFFC08A52),
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCorrect) Color(0xFFDCFCE7) else Color(0xFFFFE4E6)
                                    ) {
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCorrect) CorrectGreen else IncorrectRed
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Thu gọn" else "Mở rộng",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Đề câu hỏi
                            FormattedFormulaText(
                                text = record.question.content,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )

                            // Nội dung chi tiết khi mở rộng
                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    // Đáp án đúng
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF0FDF4),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "Đáp án chuẩn:",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = CorrectGreen
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            FormattedFormulaText(
                                                text = record.gradeResult.correctDisplayAnswer,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF14532D)
                                                )
                                            )
                                        }
                                    }

                                    // Lời giải chi tiết
                                    if (record.gradeResult.explanation.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
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
                                                    tint = Color(0xFFC08A52),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = "Lời giải chi tiết:",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFB45309)
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    FormattedFormulaText(
                                                        text = record.gradeResult.explanation,
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.width(96.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = color
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PracticeResultScreenPreview() {
    HocTotTHCSTheme {
        val sampleQuestion = Question.MultipleChoice(
            id = "q1",
            lessonId = "l1",
            content = "Số đối của -7 là bao nhiêu?",
            options = listOf("7", "-7", "0", "14"),
            correctAnswer = "7",
            explanation = "Số đối của số nguyên âm -a là số nguyên dương a."
        )
        val sampleRecords = listOf(
            AttemptQuestionRecord(
                question = sampleQuestion,
                userAnswer = UserAnswer.MultipleChoice("7"),
                gradeResult = GradeResult(
                    status = GradeStatus.CORRECT,
                    score = 1.0f,
                    isCorrect = true,
                    correctDisplayAnswer = "7",
                    explanation = sampleQuestion.explanation
                ),
                usedHint = false,
                timeSpentSeconds = 15
            ),
            AttemptQuestionRecord(
                question = sampleQuestion,
                userAnswer = UserAnswer.MultipleChoice("-7"),
                gradeResult = GradeResult(
                    status = GradeStatus.INCORRECT,
                    score = 0.0f,
                    isCorrect = false,
                    correctDisplayAnswer = "7",
                    explanation = sampleQuestion.explanation
                ),
                usedHint = true,
                timeSpentSeconds = 25
            )
        )

        PracticeResultScreen(
            correctCount = 1,
            totalCount = 2,
            totalTimeSpentSeconds = 40,
            records = sampleRecords,
            onRetryWrongQuestions = {},
            onPracticeAgain = {},
            onGoHome = {}
        )
    }
}
