package com.example.ui.screens.test

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.domain.model.AcademicRank
import com.example.domain.model.ChapterScoreAnalysis
import com.example.domain.model.TestScoreSummary
import com.example.domain.model.TestType
import com.example.ui.components.gamification.LevelUpCelebrationDialog
import com.example.ui.components.gamification.NewBadgeUnlockedDialog
import com.example.ui.theme.CorrectGreen
import com.example.ui.theme.HocTotTHCSTheme
import com.example.ui.theme.IncorrectRed
import com.example.ui.viewmodel.TestUiState
import com.example.ui.viewmodel.TestViewModel

/**
 * Màn hình kết quả bài kiểm tra (F07):
 * - Điểm số thang 10, huy hiệu xếp loại
 * - Thống kê đúng / sai / bỏ trống, thời gian
 * - Đánh giá điểm mạnh / điểm yếu theo từng chương
 * - Nút "Xem lại bài thi", "Lịch sử kiểm tra", "Làm bài mới"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestResultScreen(
    viewModel: TestViewModel,
    onNavigateBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onRetryWrongQuestions: ((List<String>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val state = uiState as? TestUiState.Submitted ?: return
    val summary = state.scoreSummary

    val rankColor = Color(summary.academicRank.colorHex)
    val minutes = state.timeSpentSeconds / 60
    val seconds = state.timeSpentSeconds % 60
    val timeFormatted = if (minutes > 0) "${minutes} phút ${seconds} giây" else "${seconds} giây"

    var showLevelUpDialog by remember { mutableStateOf(state.gamificationReward?.isLevelUp == true) }
    var pendingBadges by remember { mutableStateOf(state.gamificationReward?.newlyUnlockedBadges ?: emptyList()) }

    if (showLevelUpDialog && state.gamificationReward?.newLevelInfo != null) {
        LevelUpCelebrationDialog(
            newLevelInfo = state.gamificationReward.newLevelInfo,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kết quả kiểm tra", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.resetToConfig() }, modifier = Modifier.testTag("result_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Về thiết lập")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHistory, modifier = Modifier.testTag("result_history_button")) {
                        Icon(Icons.Default.History, contentDescription = "Xem lịch sử")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("test_result_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Điểm số & Xếp loại nổi bật
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("score_summary_card")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = rankColor.copy(alpha = 0.12f),
                            modifier = Modifier.size(90.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = rankColor,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "${summary.finalScore} / 10.0",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = rankColor,
                                fontSize = 42.sp
                            ),
                            modifier = Modifier.testTag("final_score_text")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = rankColor
                            ) {
                                Text(
                                    text = "Xếp loại: ${summary.academicRank.title}",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    ),
                                    modifier = Modifier
                                        .padding(horizontal = 14.dp, vertical = 5.dp)
                                        .testTag("academic_rank_text")
                                )
                            }

                            if (state.earnedXp > 0) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = "+${state.earnedXp} XP",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC08A52)
                                        ),
                                        modifier = Modifier
                                            .padding(horizontal = 14.dp, vertical = 5.dp)
                                            .testTag("earned_xp_chip")
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = summary.praiseMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Hàng 4 thẻ số liệu thống kê: Đúng, Sai, Bỏ trống, Thời gian
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Đúng",
                        value = "${summary.correctCount}",
                        icon = Icons.Default.CheckCircle,
                        color = CorrectGreen
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Sai",
                        value = "${summary.wrongCount}",
                        icon = Icons.Default.Close,
                        color = IncorrectRed
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Bỏ trống",
                        value = "${summary.unansweredCount}",
                        icon = Icons.Default.Warning,
                        color = Color.Gray
                    )
                    StatCard(
                        modifier = Modifier.weight(1.3f),
                        title = "Thời gian",
                        value = if (minutes > 0) "${minutes}p ${seconds}s" else "${seconds}s",
                        icon = Icons.Default.Alarm,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Phân tích điểm theo từng chương / chủ đề (Điểm mạnh & Điểm yếu)
            if (summary.chapterAnalysis.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chapter_analysis_card")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Phân tích theo chủ đề & chương",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Nhận biết điểm mạnh và phần kiến thức cần củng cố thêm",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            summary.chapterAnalysis.forEach { ch ->
                                ChapterProgressRow(analysis = ch)
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }

            // Các nút hành động: Ôn lại câu sai, Xem lại chi tiết, Làm bài mới, Lịch sử kiểm tra
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val wrongIds = state.answerDetails.filter { it.gradeResult?.isCorrect != true }.map { it.question.id }
                    if (wrongIds.isNotEmpty() && onRetryWrongQuestions != null) {
                        Button(
                            onClick = { onRetryWrongQuestions(wrongIds) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC26161)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("retry_exam_wrong_questions_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ôn lại ${wrongIds.size} câu làm sai", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = { viewModel.openReviewMode() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("review_exam_button")
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Xem lại chi tiết bài làm & lời giải", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetToConfig() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("new_exam_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Làm bài kiểm tra mới")
                    }

                    OutlinedButton(
                        onClick = onOpenHistory,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("view_history_button")
                    ) {
                        Icon(Icons.Default.History, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Xem lại các bài đã làm (Lịch sử)")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = color))
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChapterProgressRow(analysis: ChapterScoreAnalysis) {
    val progressColor = if (analysis.isStrongPoint) CorrectGreen else MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = analysis.chapterTitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${analysis.scoreEarned}/${analysis.maxScore} điểm (${analysis.correctCount}/${analysis.totalQuestions} câu)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (analysis.percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TestResultScreenPreview() {
    HocTotTHCSTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Preview TestResultScreen")
        }
    }
}
