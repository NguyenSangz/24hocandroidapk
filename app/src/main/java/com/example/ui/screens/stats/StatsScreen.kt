package com.example.ui.screens.stats

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.domain.evaluator.MetricTrend
import com.example.domain.evaluator.OverviewMetrics
import com.example.domain.evaluator.SkillEvaluationItem
import com.example.domain.evaluator.SkillLevel
import com.example.domain.evaluator.StatsTimeRange
import com.example.domain.evaluator.StrengthWeaknessAnalysis
import com.example.domain.evaluator.TrendDirection
import com.example.ui.components.chart.DailyStudyBarChart
import com.example.ui.components.chart.SubjectAccuracyHorizontalBarChart
import com.example.ui.components.chart.SubjectGroupedComparisonChart
import com.example.ui.components.chart.SubjectLessonProgressList
import com.example.ui.components.chart.TestScoreLineChart
import com.example.ui.theme.CorrectGreen
import com.example.ui.theme.SubjectColors
import com.example.ui.viewmodel.StatsUiState
import com.example.ui.viewmodel.StatsViewModel

/**
 * Màn hình Thống kê học tập (F11 - Tab Thống kê).
 */
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onNavigateToChapterPractice: ((subjectId: String, chapterId: String) -> Unit)? = null,
    onNavigateToWrongBankChapter: ((subjectId: String, chapterId: String) -> Unit)? = null,
    onNavigateToSubjectDetail: ((subjectId: String) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedRange by viewModel.selectedTimeRange.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("stats_screen_root")
    ) {
        // Bộ chọn khoảng thời gian (7 ngày, 30 ngày, học kỳ này, tất cả)
        TimeRangeSelectorBar(
            selectedRange = selectedRange,
            onSelectRange = { viewModel.setTimeRange(it) }
        )

        when (val state = uiState) {
            is StatsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.testTag("stats_loading_indicator"))
                }
            }

            is StatsUiState.Empty -> {
                StatsEmptyStateView(
                    onStartLearning = { /* Người học bấm để bắt đầu học */ }
                )
            }

            is StatsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            is StatsUiState.Success -> {
                StatsSuccessContent(
                    data = state,
                    onNavigateToChapterPractice = onNavigateToChapterPractice,
                    onNavigateToWrongBankChapter = onNavigateToWrongBankChapter,
                    onNavigateToSubjectDetail = onNavigateToSubjectDetail
                )
            }
        }
    }
}

@Composable
fun TimeRangeSelectorBar(
    selectedRange: StatsTimeRange,
    onSelectRange: (StatsTimeRange) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(StatsTimeRange.entries) { range ->
                val isSelected = range == selectedRange
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectRange(range) },
                    label = { Text(text = range.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.testTag("range_chip_${range.name}")
                )
            }
        }
    }
}

@Composable
fun StatsSuccessContent(
    data: StatsUiState.Success,
    onNavigateToChapterPractice: ((String, String) -> Unit)?,
    onNavigateToWrongBankChapter: ((String, String) -> Unit)?,
    onNavigateToSubjectDetail: ((String) -> Unit)?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("stats_content_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Thẻ tổng quan chỉ số và so sánh xu hướng
        item {
            OverviewSummaryCard(
                overview = data.overview,
                studyTimeTrend = data.studyTimeTrend,
                accuracyTrend = data.accuracyTrend
            )
        }

        // 2. Điểm mạnh và điểm yếu nổi bật + Nhận xét tích cực
        item {
            StrengthWeaknessCard(
                analysis = data.strengthWeakness,
                onNavigateToChapterPractice = onNavigateToChapterPractice,
                onNavigateToWrongBankChapter = onNavigateToWrongBankChapter
            )
        }

        // 3. Biểu đồ cột thời gian học theo ngày
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Thời gian học theo ngày",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    DailyStudyBarChart(data = data.studyBars)
                }
            }
        }

        // 4. Biểu đồ đường điểm kiểm tra
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Điểm kiểm tra theo thời gian",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TestScoreLineChart(points = data.testScores)
                }
            }
        }

        // 5. Biểu đồ thanh ngang tỉ lệ đúng theo môn
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tỉ lệ làm đúng theo môn học",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SubjectAccuracyHorizontalBarChart(items = data.subjectAccuracies)
                }
            }
        }

        // 6. Biểu đồ cột nhóm so sánh đa chiều các môn
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "So sánh tổng thể các môn học",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Xanh: Tỉ lệ đúng • Xanh lá: Tiến độ • Cam: Độ ổn định",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SubjectGroupedComparisonChart(items = data.subjectComparisons)
                }
            }
        }

        // 7. Tiến độ bài học từng môn
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tiến độ hoàn thành bài học",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SubjectLessonProgressList(items = data.lessonProgresses)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
fun OverviewSummaryCard(
    overview: OverviewMetrics,
    studyTimeTrend: MetricTrend,
    accuracyTrend: MetricTrend
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tổng quan hoạt động",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Lưới 4 chỉ số chính
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricItemBox(
                    title = "Thời gian học",
                    value = formatSecondsToHuman(overview.totalStudySeconds),
                    trend = studyTimeTrend,
                    modifier = Modifier.weight(1f)
                )
                MetricItemBox(
                    title = "Tỉ lệ đúng",
                    value = "${overview.overallAccuracyPercent.toInt()}%",
                    trend = accuracyTrend,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricItemBox(
                    title = "Câu đã làm",
                    value = "${overview.totalQuestionsAnswered}",
                    subText = "Đúng ${overview.totalQuestionsCorrect} câu",
                    modifier = Modifier.weight(1f)
                )
                MetricItemBox(
                    title = "Ngày có học",
                    value = "${overview.distinctStudyDays} ngày",
                    subText = "+${overview.totalXpEarned} XP kiếm được",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricItemBox(
    title: String,
    value: String,
    subText: String? = null,
    trend: MetricTrend? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            trend?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (it.direction) {
                        TrendDirection.INCREASED -> Icons.AutoMirrored.Filled.TrendingUp
                        TrendDirection.DECREASED -> Icons.AutoMirrored.Filled.TrendingDown
                        else -> Icons.Default.Info
                    }
                    val iconColor = when (it.direction) {
                        TrendDirection.INCREASED -> CorrectGreen
                        TrendDirection.DECREASED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = it.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = iconColor
                    )
                }
            }

            subText?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StrengthWeaknessCard(
    analysis: StrengthWeaknessAnalysis,
    onNavigateToChapterPractice: ((String, String) -> Unit)?,
    onNavigateToWrongBankChapter: ((String, String) -> Unit)?
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Điểm mạnh & Điểm cần cải thiện",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nhận xét thân thiện
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = analysis.overallComment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Điểm mạnh (Top 3)
            Text(
                text = "Thế mạnh của bạn:",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = CorrectGreen
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (analysis.topStrengths.isEmpty()) {
                Text(
                    text = "Chưa đủ dữ liệu (Cần làm thêm bài tập để xác định)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                analysis.topStrengths.forEach { item ->
                    SkillItemRow(item = item, isStrength = true, onPractice = onNavigateToChapterPractice, onReviewWrong = onNavigateToWrongBankChapter)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Điểm cần củng cố (Top 3)
            Text(
                text = "Cần luyện tập thêm:",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (analysis.topWeaknesses.isEmpty()) {
                Text(
                    text = "Tuyệt vời! Hiện tại bạn không có phần kiến thức nào bị yếu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                analysis.topWeaknesses.forEach { item ->
                    SkillItemRow(item = item, isStrength = false, onPractice = onNavigateToChapterPractice, onReviewWrong = onNavigateToWrongBankChapter)
                }
            }
        }
    }
}

@Composable
fun SkillItemRow(
    item: SkillEvaluationItem,
    isStrength: Boolean,
    onPractice: ((String, String) -> Unit)?,
    onReviewWrong: ((String, String) -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val icon = if (isStrength) Icons.Default.CheckCircle else Icons.Default.Warning
                    val color = if (isStrength) CorrectGreen else MaterialTheme.colorScheme.error
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${item.weightedAccuracy.toInt()}% đúng",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isStrength) CorrectGreen else MaterialTheme.colorScheme.error
                )
            }

            if (item.suggestionText.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.suggestionText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Nút hành động gợi ý nếu là điểm yếu
            if (!isStrength) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onPractice?.invoke(item.subjectId, item.id) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(text = "Ôn chương này", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = { onReviewWrong?.invoke(item.subjectId, item.id) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(text = "Xem câu sai", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun StatsEmptyStateView(
    onStartLearning: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("stats_empty_state"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Hãy học bài đầu tiên để xem thống kê!",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hoàn thành một bài học, bài luyện tập hoặc bài kiểm tra để theo dõi tiến độ học tập và phân tích điểm mạnh nhé.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun formatSecondsToHuman(seconds: Long): String {
    val mins = seconds / 60
    val hours = mins / 60
    val remMins = mins % 60
    return if (hours > 0) "${hours}h ${remMins}p" else "${mins} phút"
}

@Preview(showBackground = true)
@Composable
fun StatsEmptyPreview() {
    MaterialTheme {
        StatsEmptyStateView(onStartLearning = {})
    }
}
