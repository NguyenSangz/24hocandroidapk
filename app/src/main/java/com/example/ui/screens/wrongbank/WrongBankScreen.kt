package com.example.ui.screens.wrongbank

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Question
import com.example.domain.model.WrongBankSort
import com.example.domain.model.WrongQuestionDetail
import com.example.domain.model.WrongQuestionStatus
import com.example.ui.components.FormattedFormulaText
import com.example.ui.theme.CorrectGreen
import com.example.ui.theme.HocTotTHCSTheme
import com.example.ui.theme.IncorrectRed
import com.example.ui.viewmodel.WrongBankUiState
import com.example.ui.viewmodel.WrongBankViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Màn hình Ngân hàng câu sai (F08 - Mistake Notebook):
 * - Thống kê tổng quan: Cần ôn, Đang củng cố, Đã thuộc
 * - Nút lớn "Ôn ngay" ưu tiên câu đến hạn ôn và sai nhiều nhất
 * - Bộ lọc theo môn, chương, trạng thái, dạng câu hỏi
 * - Sắp xếp theo hạn ôn hoặc số lần sai
 * - Xem chi tiết đề bài, đáp án chuẩn, lời giải, câu sai đã chọn
 * - Đánh dấu Đã thuộc thủ công & Xóa câu hỏi
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongBankScreen(
    viewModel: WrongBankViewModel,
    onNavigateBack: () -> Unit,
    onStartReviewSession: (subjectId: String?, chapterId: String?, count: Int, questionIds: List<String>?, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.notificationMessage) {
        uiState.notificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Ngân hàng câu sai",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Lớp ${uiState.selectedGrade} • Spaced Repetition",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("wrong_bank_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.testTag("wrong_bank_refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Làm mới dữ liệu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.testTag("wrong_bank_loading"))
                }
            } else if (uiState.rawQuestions.isEmpty()) {
                // Trạng thái hoàn toàn chưa có câu sai nào
                EmptyMistakeState(
                    onPracticeClick = { onNavigateBack() },
                    modifier = Modifier.testTag("wrong_bank_empty_all")
                )
            } else {
                WrongBankContent(
                    uiState = uiState,
                    onStartReviewSession = onStartReviewSession,
                    onSelectSubjectFilter = { viewModel.setSubjectFilter(it) },
                    onSelectChapterFilter = { viewModel.setChapterFilter(it) },
                    onSelectStatusFilter = { viewModel.setStatusFilter(it) },
                    onSelectQuestionTypeFilter = { viewModel.setQuestionTypeFilter(it) },
                    onSelectSortOrder = { viewModel.setSortOrder(it) },
                    onShowDetail = { viewModel.showDetail(it) },
                    onPromptDelete = { viewModel.promptDelete(it) },
                    onMarkMastered = { viewModel.markMastered(it) }
                )
            }

            // Hộp thoại xác nhận xóa câu hỏi
            if (uiState.questionToDelete != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.cancelDelete() },
                    icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = IncorrectRed) },
                    title = { Text("Xóa khỏi Ngân hàng câu sai?", fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            text = "Câu hỏi này sẽ được gỡ bỏ khỏi danh sách ôn tập. Bạn có thể gặp lại câu này nếu làm sai ở các bài luyện tập hoặc bài thi tiếp theo.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.executeDelete() },
                            colors = ButtonDefaults.buttonColors(containerColor = IncorrectRed),
                            modifier = Modifier.testTag("confirm_delete_button")
                        ) {
                            Text("Xóa bỏ")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.cancelDelete() },
                            modifier = Modifier.testTag("cancel_delete_button")
                        ) {
                            Text("Hủy")
                        }
                    }
                )
            }

            // BottomSheet xem chi tiết câu hỏi
            if (uiState.selectedQuestionForDetail != null) {
                WrongQuestionDetailBottomSheet(
                    detail = uiState.selectedQuestionForDetail!!,
                    onDismiss = { viewModel.hideDetail() },
                    onMarkMastered = {
                        viewModel.markMastered(it)
                    },
                    onReviewSingle = { questionId ->
                        viewModel.hideDetail()
                        onStartReviewSession(null, null, 1, listOf(questionId), "Ôn câu hỏi đã chọn")
                    }
                )
            }
        }
    }
}

@Composable
private fun WrongBankContent(
    uiState: WrongBankUiState,
    onStartReviewSession: (subjectId: String?, chapterId: String?, count: Int, questionIds: List<String>?, title: String) -> Unit,
    onSelectSubjectFilter: (String) -> Unit,
    onSelectChapterFilter: (String) -> Unit,
    onSelectStatusFilter: (String) -> Unit,
    onSelectQuestionTypeFilter: (String) -> Unit,
    onSelectSortOrder: (WrongBankSort) -> Unit,
    onShowDetail: (WrongQuestionDetail) -> Unit,
    onPromptDelete: (WrongQuestionDetail) -> Unit,
    onMarkMastered: (String) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("wrong_bank_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Thẻ thống kê 3 trạng thái
        item {
            OverviewStatsCards(uiState = uiState)
        }

        // 2. Nút lớn "ÔN NGAY" (Luyện tập thông minh)
        item {
            HeroReviewNowCard(
                dueCount = uiState.stats.dueReviewCount,
                totalPending = uiState.stats.needsReviewCount + uiState.stats.consolidatingCount,
                onClick = {
                    val targetSubject = if (uiState.selectedSubjectId != "all") uiState.selectedSubjectId else null
                    val targetChapter = if (uiState.selectedChapterId != "all") uiState.selectedChapterId else null
                    onStartReviewSession(
                        targetSubject,
                        targetChapter,
                        10,
                        null,
                        if (uiState.stats.dueReviewCount > 0) "Ôn các câu đến hạn (${uiState.stats.dueReviewCount} câu)"
                        else "Luyện ôn ngân hàng câu sai"
                    )
                }
            )
        }

        // 3. Thanh bộ lọc môn & chương
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Bộ lọc & Phân loại",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                // Bộ lọc môn
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedSubjectId == "all",
                        onClick = { onSelectSubjectFilter("all") },
                        label = { Text("Tất cả môn (${uiState.stats.totalCount})") },
                        colors = FilterChipDefaults.filterChipColors(),
                        modifier = Modifier.testTag("filter_subject_all")
                    )

                    uiState.subjects.forEach { subject ->
                        val count = uiState.stats.subjectCounts[subject.id] ?: 0
                        FilterChip(
                            selected = uiState.selectedSubjectId == subject.id,
                            onClick = { onSelectSubjectFilter(subject.id) },
                            label = { Text("${subject.name} ($count)") },
                            modifier = Modifier.testTag("filter_subject_${subject.id}")
                        )
                    }
                }

                // Bộ lọc chương (nếu đã chọn môn)
                if (uiState.chapters.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.selectedChapterId == "all",
                            onClick = { onSelectChapterFilter("all") },
                            label = { Text("Tất cả chương") }
                        )

                        uiState.chapters.forEach { chapter ->
                            FilterChip(
                                selected = uiState.selectedChapterId == chapter.id,
                                onClick = { onSelectChapterFilter(chapter.id) },
                                label = { Text(chapter.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }

                // Bộ lọc trạng thái & Dạng câu hỏi & Sắp xếp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nhóm lọc trạng thái
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusFilterChip(
                            title = "Tất cả",
                            isSelected = uiState.selectedStatus == "all",
                            onClick = { onSelectStatusFilter("all") }
                        )
                        StatusFilterChip(
                            title = "Đến hạn",
                            isSelected = uiState.selectedStatus == "DUE",
                            color = IncorrectRed,
                            onClick = { onSelectStatusFilter("DUE") }
                        )
                        StatusFilterChip(
                            title = "Cần ôn",
                            isSelected = uiState.selectedStatus == WrongQuestionStatus.NEEDS_REVIEW.code,
                            color = Color(0xFFEA580C),
                            onClick = { onSelectStatusFilter(WrongQuestionStatus.NEEDS_REVIEW.code) }
                        )
                        StatusFilterChip(
                            title = "Đang củng cố",
                            isSelected = uiState.selectedStatus == WrongQuestionStatus.CONSOLIDATING.code,
                            color = Color(0xFF4A7FA8),
                            onClick = { onSelectStatusFilter(WrongQuestionStatus.CONSOLIDATING.code) }
                        )
                        StatusFilterChip(
                            title = "Đã thuộc",
                            isSelected = uiState.selectedStatus == WrongQuestionStatus.MASTERED.code,
                            color = CorrectGreen,
                            onClick = { onSelectStatusFilter(WrongQuestionStatus.MASTERED.code) }
                        )
                    }

                    // Nút sắp xếp
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("sort_button")
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "Sắp xếp danh sách")
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            WrongBankSort.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sort.displayName,
                                            fontWeight = if (uiState.sortOrder == sort) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onSelectSortOrder(sort)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Danh sách các câu hỏi sai
        if (uiState.filteredQuestions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Không có câu hỏi nào phù hợp với bộ lọc hiện tại",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.filteredQuestions, key = { it.entity.questionId }) { detail ->
                WrongQuestionCard(
                    detail = detail,
                    onClickDetail = { onShowDetail(detail) },
                    onMarkMastered = { onMarkMastered(detail.entity.questionId) },
                    onDelete = { onPromptDelete(detail) }
                )
            }
        }
    }
}

@Composable
private fun StatusFilterChip(
    title: String,
    isSelected: Boolean,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun OverviewStatsCards(uiState: WrongBankUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatBadgeCard(
            title = "Cần ôn",
            count = uiState.stats.needsReviewCount,
            color = Color(0xFFC26161),
            modifier = Modifier.weight(1f)
        )
        StatBadgeCard(
            title = "Đang củng cố",
            count = uiState.stats.consolidatingCount,
            color = Color(0xFF4A7FA8),
            modifier = Modifier.weight(1f)
        )
        StatBadgeCard(
            title = "Đã thuộc",
            count = uiState.stats.masteredCount,
            color = Color(0xFF3F9E6E),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatBadgeCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HeroReviewNowCard(
    dueCount: Int,
    totalPending: Int,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag("hero_review_now_button")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ÔN NGAY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    if (dueCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = IncorrectRed
                        ) {
                            Text(
                                text = "$dueCount câu đến hạn",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (dueCount > 0) "Ưu tiên các câu cần ôn gấp theo chu kỳ Spaced Repetition"
                    else "Luyện tập củng cố các câu còn chưa vững ($totalPending câu)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun WrongQuestionCard(
    detail: WrongQuestionDetail,
    onClickDetail: () -> Unit,
    onMarkMastered: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (detail.status) {
        WrongQuestionStatus.NEEDS_REVIEW -> Color(0xFFC26161)
        WrongQuestionStatus.CONSOLIDATING -> Color(0xFF4A7FA8)
        WrongQuestionStatus.MASTERED -> CorrectGreen
    }

    val dueText = when {
        detail.status == WrongQuestionStatus.MASTERED -> "Đã thuộc"
        detail.isDueForReview -> "Đến hạn ôn!"
        detail.entity.nextReviewTimestamp > 0L -> {
            val diffHours = ((detail.entity.nextReviewTimestamp - System.currentTimeMillis()) / (1000 * 60 * 60)).coerceAtLeast(1)
            if (diffHours >= 24) "Ôn lại sau ${(diffHours / 24).toInt()} ngày" else "Ôn lại sau $diffHours giờ"
        }
        else -> "Chưa lên lịch"
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wrong_question_card_${detail.entity.questionId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Môn, Chương, Trạng thái & Chuỗi đúng
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = detail.subjectName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Sai ${detail.entity.errorCount} lần",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Trạng thái tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (detail.status == WrongQuestionStatus.CONSOLIDATING)
                                "Củng cố (${detail.entity.consecutiveCorrectCount}/3)"
                            else detail.status.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nội dung câu hỏi (đề bài)
            val questionContent = detail.question?.content ?: "Câu hỏi #${detail.entity.questionId}"
            FormattedFormulaText(
                text = questionContent,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.sp
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Hạn ôn tập và các nút thao tác
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = if (detail.isDueForReview) IncorrectRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dueText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (detail.isDueForReview) IncorrectRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (detail.isDueForReview) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }

                // Các nút hành động
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Nút Đánh dấu đã thuộc thủ công
                    if (detail.status != WrongQuestionStatus.MASTERED) {
                        IconButton(
                            onClick = onMarkMastered,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("mark_mastered_${detail.entity.questionId}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Đánh dấu đã thuộc",
                                tint = CorrectGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Nút Xóa khỏi ngân hàng câu sai
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_${detail.entity.questionId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Xóa khỏi ngân hàng câu sai",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Nút Xem chi tiết
                    IconButton(
                        onClick = onClickDetail,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("view_detail_${detail.entity.questionId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Xem chi tiết câu hỏi",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WrongQuestionDetailBottomSheet(
    detail: WrongQuestionDetail,
    onDismiss: () -> Unit,
    onMarkMastered: (String) -> Unit,
    onReviewSingle: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("wrong_question_detail_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chi tiết câu sai",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Đóng")
                }
            }

            Text(
                text = "${detail.subjectName} • ${detail.chapterTitle} • ${detail.lessonTitle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Đề bài
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Đề bài:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FormattedFormulaText(
                        text = detail.question?.content ?: "Nội dung câu hỏi",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Câu trả lời sai gần nhất của học sinh (nếu có)
            if (detail.entity.lastWrongAnswer.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IncorrectRed.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = IncorrectRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lần trước bạn chọn: ${detail.entity.lastWrongAnswer}",
                            style = MaterialTheme.typography.bodySmall.copy(color = IncorrectRed, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Lời giải chi tiết
            val explanation = detail.question?.explanation ?: ""
            if (explanation.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CorrectGreen.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CorrectGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Lời giải chi tiết & Đáp án chuẩn:",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CorrectGreen
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        FormattedFormulaText(
                            text = explanation,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Các nút hành động bên dưới
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (detail.status != WrongQuestionStatus.MASTERED) {
                    OutlinedButton(
                        onClick = { onMarkMastered(detail.entity.questionId) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Đánh dấu Đã thuộc")
                    }
                }

                Button(
                    onClick = { onReviewSingle(detail.entity.questionId) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ôn câu này ngay")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EmptyMistakeState(
    onPracticeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = CorrectGreen.copy(alpha = 0.12f),
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = CorrectGreen,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Tuyệt vời, bạn chưa có câu sai nào!",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tất cả các câu hỏi bạn làm đều đạt điểm tuyệt đối. Hãy tiếp tục phát huy ở các bài học và bài kiểm tra mới nhé!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onPracticeClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("empty_go_practice_button")
            ) {
                Text("Luyện tập thêm kiến thức mới")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WrongBankScreenPreview() {
    HocTotTHCSTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text("Preview WrongBankScreen")
        }
    }
}
