package com.example.ui.screens.practice

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChapterEntity
import com.example.ui.theme.HocTotTHCSTheme
import com.example.ui.viewmodel.PracticeViewModel

/**
 * Màn hình tab Luyện tập (F06):
 * - Bộ cấu hình phiên luyện tập: Chọn môn, Chọn chương/bài, Chọn số câu (5/10/15), Độ khó (Dễ/Vừa/Khó/Hỗn hợp), Bấm Bắt đầu.
 * - Danh mục luyện nhanh và flashcard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    viewModel: PracticeViewModel,
    onStartCustomPractice: (subjectId: String, chapterId: String, count: Int, difficulty: String) -> Unit,
    onStartSubjectQuiz: (String) -> Unit,
    onStartExam: (String, String) -> Unit,
    onOpenFlashcards: () -> Unit,
    onOpenWrongBank: () -> Unit = {},
    dueMistakesCount: Int = 0
) {
    val selectedGrade by viewModel.selectedGrade.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    // Trạng thái cấu hình phiên luyện tập
    var selectedSubjectId by remember(subjects) {
        mutableStateOf(subjects.firstOrNull()?.id ?: "math")
    }
    var selectedChapterId by remember { mutableStateOf("all") }
    var selectedCount by remember { mutableIntStateOf(10) }
    var selectedDifficulty by remember { mutableStateOf("MIXED") } // EASY, MEDIUM, HARD, MIXED

    // Danh sách chương của môn đang chọn
    var chapters by remember { mutableStateOf<List<ChapterEntity>>(emptyList()) }
    var isChapterDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSubjectId, selectedGrade) {
        viewModel.getChaptersForSubject(selectedSubjectId).collect { chapterList ->
            chapters = chapterList
            if (chapterList.none { it.id == selectedChapterId }) {
                selectedChapterId = "all"
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("practice_screen_list"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // Section: Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Luyện tập thông minh (Lớp $selectedGrade)",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Tự do chọn môn, chương bài, số lượng câu và độ khó theo mục tiêu của bạn",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Section: Bộ cấu hình phiên luyện tập (Theo yêu cầu F06)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("practice_config_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Tùy chỉnh phiên luyện tập",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Chọn Môn học
                    Text(
                        text = "1. Chọn môn học:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subjects.forEach { subject ->
                            val isSelected = selectedSubjectId == subject.id

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedSubjectId = subject.id
                                    selectedChapterId = "all"
                                },
                                label = {
                                    Text(
                                        subject.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
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
                                modifier = Modifier.testTag("filter_subject_${subject.id}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Chọn Chương / Bài
                    Text(
                        text = "2. Chọn chương bài:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = isChapterDropdownExpanded,
                        onExpandedChange = { isChapterDropdownExpanded = it }
                    ) {
                        val currentChapterTitle = if (selectedChapterId == "all") {
                            "Toàn bộ các chương của môn"
                        } else {
                            chapters.find { it.id == selectedChapterId }?.title ?: "Toàn bộ các chương"
                        }

                        OutlinedTextField(
                            value = currentChapterTitle,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isChapterDropdownExpanded) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("chapter_selector_field")
                        )

                        ExposedDropdownMenu(
                            expanded = isChapterDropdownExpanded,
                            onDismissRequest = { isChapterDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Toàn bộ các chương của môn", fontWeight = if (selectedChapterId == "all") FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedChapterId = "all"
                                    isChapterDropdownExpanded = false
                                }
                            )
                            chapters.forEach { chapter ->
                                DropdownMenuItem(
                                    text = { Text(chapter.title, fontWeight = if (selectedChapterId == chapter.id) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedChapterId = chapter.id
                                        isChapterDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Số câu hỏi (5 / 10 / 15)
                    Text(
                        text = "3. Số lượng câu hỏi:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15).forEach { countOption ->
                            val isSelected = selectedCount == countOption
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCount = countOption },
                                label = { Text("$countOption câu", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("count_chip_$countOption")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Độ khó (Dễ / Vừa / Khó / Hỗn hợp)
                    Text(
                        text = "4. Mức độ thử thách:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "MIXED" to "Hỗn hợp (4:4:2)",
                            "EASY" to "Dễ (Nhận biết)",
                            "MEDIUM" to "Vừa (Thông hiểu)",
                            "HARD" to "Khó (Vận dụng)"
                        ).forEach { (diffCode, diffLabel) ->
                            val isSelected = selectedDifficulty == diffCode
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDifficulty = diffCode },
                                label = { Text(diffLabel, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.testTag("diff_chip_$diffCode")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Nút "Bắt đầu luyện tập"
                    Button(
                        onClick = {
                            onStartCustomPractice(
                                selectedSubjectId,
                                selectedChapterId,
                                selectedCount,
                                selectedDifficulty
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_custom_practice_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bắt đầu luyện tập ngay",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Section: Các tiện ích bổ sung (Flashcard & Thi định kỳ)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp)
            ) {
                Text(
                    text = "Hình thức ôn luyện khác",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Ngân hàng câu sai (F08)
                PracticeFeatureCard(
                    title = "Ngân hàng câu sai",
                    subtitle = "Lặp lại ngắt quãng (Spaced Repetition) • Tự động gom câu sai • Ôn lại đến khi thuộc",
                    icon = Icons.Default.AutoAwesome,
                    accentColor = if (dueMistakesCount > 0) Color(0xFFC26161) else Color(0xFFEA580C),
                    tagText = if (dueMistakesCount > 0) "$dueMistakesCount câu đến hạn" else "Sổ tay câu sai",
                    onClick = onOpenWrongBank
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Flashcard Feature Card
                PracticeFeatureCard(
                    title = "Thẻ ghi nhớ Flashcard",
                    subtitle = "Thuật toán lặp lại ngắt quãng (Spaced Repetition) • Từ vựng & Công thức",
                    icon = Icons.Default.Style,
                    accentColor = Color(0xFF7C3AED),
                    tagText = "Ghi nhớ nhanh",
                    onClick = onOpenFlashcards
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Timed Exam Card
                PracticeFeatureCard(
                    title = "Kiểm tra định kỳ (Có bấm giờ)",
                    subtitle = "Đề tổng hợp 15 phút • Chấm điểm thang 10 • Xem lại câu sai chi tiết",
                    icon = Icons.Default.Alarm,
                    accentColor = Color(0xFFC26161),
                    tagText = "15 phút",
                    onClick = { onStartExam("all", "Đề kiểm tra tổng hợp Lớp $selectedGrade") }
                )
            }
        }

        // Section: Luyện nhanh theo môn
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp)
            ) {
                Text(
                    text = "Luyện tập nhanh",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                SubjectQuizRow(
                    title = "Toán học Lớp $selectedGrade",
                    subtitle = "10 câu hỏi ngẫu nhiên • Đại số & Hình học",
                    icon = Icons.Default.Calculate,
                    color = Color(0xFF4A7FA8),
                    onClick = { onStartSubjectQuiz("math") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                SubjectQuizRow(
                    title = "Tiếng Anh Lớp $selectedGrade",
                    subtitle = "10 câu hỏi ngẫu nhiên • Từ vựng & Ngữ pháp",
                    icon = Icons.Default.Translate,
                    color = Color(0xFF3F9E6E),
                    onClick = { onStartSubjectQuiz("english") }
                )
            }
        }
    }
}

@Composable
fun PracticeFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    tagText: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("practice_feature_card_$title")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = tagText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SubjectQuizRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("subject_quiz_row_$title")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
