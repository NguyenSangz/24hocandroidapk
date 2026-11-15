package com.example.ui.screens.study

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.SubjectEntity
import com.example.domain.model.LessonFontSize
import com.example.domain.model.LessonNavigationInfo
import com.example.ui.components.FormattedFormulaText
import com.example.ui.components.MathFormulaBox
import com.example.ui.theme.CorrectGreen
import com.example.ui.theme.HocTotTHCSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailScreen(
    lessonId: String,
    viewModel: LessonViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAnotherLesson: (String) -> Unit,
    onStartPractice: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showFontMenu by remember { mutableStateOf(false) }

    when (val state = uiState) {
        is LessonUiState.Loading -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Đang tải bài học...", style = MaterialTheme.typography.titleMedium) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        is LessonUiState.Error -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Lỗi", style = MaterialTheme.typography.titleMedium) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        is LessonUiState.Success -> {
            val subjectColor = parseColor(state.subject?.color ?: "#2563EB")
            val currentLesson = state.lesson
            val fontScale = state.fontSize.scaleFactor

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = state.subject?.name ?: "Bài học",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Bài ${currentLesson.order} • Lớp ${state.grade}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("lesson_detail_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Quay lại"
                                )
                            }
                        },
                        actions = {
                            // Menu chỉnh cỡ chữ
                            Box {
                                IconButton(
                                    onClick = { showFontMenu = true },
                                    modifier = Modifier.testTag("font_size_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatSize,
                                        contentDescription = "Cỡ chữ"
                                    )
                                }
                                DropdownMenu(
                                    expanded = showFontMenu,
                                    onDismissRequest = { showFontMenu = false }
                                ) {
                                    LessonFontSize.entries.forEach { size ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${size.displayName} ${if (size == state.fontSize) "✓" else ""}",
                                                    fontWeight = if (size == state.fontSize) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                viewModel.setFontSize(size)
                                                showFontMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Đánh dấu hoàn thành
                            IconButton(
                                onClick = {
                                    viewModel.toggleLessonCompleted()
                                    val msg = if (!currentLesson.isCompleted) "Đã đánh dấu hoàn thành bài!" else "Đã bỏ đánh dấu hoàn thành"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("toggle_lesson_completed_button")
                            ) {
                                Icon(
                                    imageVector = if (currentLesson.isCompleted) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Đã học xong",
                                    tint = if (currentLesson.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                LessonDetailContent(
                    lesson = currentLesson,
                    subjectColor = subjectColor,
                    navigation = state.navigation,
                    fontScale = fontScale,
                    onToggleCompleted = { viewModel.toggleLessonCompleted() },
                    onNavigatePrevious = {
                        state.navigation.previousLessonId?.let { onNavigateToAnotherLesson(it) }
                    },
                    onNavigateNext = {
                        state.navigation.nextLessonId?.let { onNavigateToAnotherLesson(it) }
                    },
                    onStartPractice = {
                        onStartPractice(currentLesson.id)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
fun LessonDetailContent(
    lesson: LessonEntity,
    subjectColor: Color,
    navigation: LessonNavigationInfo,
    fontScale: Float,
    onToggleCompleted: () -> Unit,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    onStartPractice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Quản lý trạng thái mở/ẩn các bước giải của ví dụ
    val exampleExpanded = remember { mutableStateMapOf<Int, Boolean>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Banner tiêu đề bài học
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Bài số ${lesson.order}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (lesson.isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CorrectGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Đã học xong",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CorrectGreen
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = (19 * fontScale).sp,
                        lineHeight = (26 * fontScale).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 1. Mục tiêu bài học (Tạo điểm tựa nhận thức ban đầu)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = "Mục tiêu",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mục tiêu bài học",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Nắm vững kiến thức trọng tâm và các định nghĩa cơ bản.\n• Vận dụng linh hoạt vào bài tập thực tế và kỳ thi đánh giá năng lực.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (14 * fontScale).sp,
                        lineHeight = (22 * fontScale).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Nội dung tóm tắt lý thuyết (có hỗ trợ LaTeX $...$ và $$...$$)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Lý thuyết",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nội dung tóm tắt",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                FormattedFormulaText(
                    text = lesson.summary,
                    fontSizeScale = fontScale
                )

                // Nếu có công thức LaTeX độc lập trong trường formulasLatex
                if (lesson.formulasLatex.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    MathFormulaBox(formula = lesson.formulasLatex)
                }
            }
        }

        // 3. Ghi nhớ (Khung nhẹ nhàng, không chói)
        if (lesson.tips.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F3E9)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Ghi nhớ",
                        tint = Color(0xFFC08A52),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Ghi nhớ cốt lõi",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lesson.tips,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF78350F),
                                fontSize = (14 * fontScale).sp,
                                lineHeight = (22 * fontScale).sp
                            )
                        )
                    }
                }
            }
        }

        // 4. Ví dụ minh họa (Bấm để mở/ẩn từng bước giải)
        if (lesson.examples.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Ví dụ",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ví dụ minh họa & Phương pháp giải",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val rawExamples = lesson.examples.split("\n\nVí dụ")
                    rawExamples.forEachIndexed { index, part ->
                        val isExpanded = exampleExpanded[index] ?: (index == 0) // Mặc định mở ví dụ 1
                        val fullExampleText = if (index > 0 && !part.startsWith("Ví dụ")) "Ví dụ$part" else part

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            exampleExpanded[index] = !isExpanded
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ví dụ minh họa ${index + 1}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Ẩn lời giải" else "Xem lời giải",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        FormattedFormulaText(
                                            text = fullExampleText,
                                            fontSizeScale = fontScale
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Nút "Đã học xong" và nút "Luyện tập bài này"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onToggleCompleted,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (lesson.isCompleted) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (lesson.isCompleted) CorrectGreen else MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (lesson.isCompleted) Color(0xFF86EFAC) else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("toggle_completed_main_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (lesson.isCompleted) CorrectGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (lesson.isCompleted) "Đã hoàn thành" else "Đánh dấu đã học",
                    color = if (lesson.isCompleted) CorrectGreen else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onStartPractice,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("practice_lesson_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Luyện tập bài này",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 6. Nút điều hướng "Bài trước" / "Bài sau"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onNavigatePrevious,
                enabled = navigation.previousLessonId != null,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("previous_lesson_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bài trước")
            }

            Text(
                text = "${navigation.currentLessonIndex} / ${navigation.totalLessonsInSubject}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onNavigateNext,
                enabled = navigation.nextLessonId != null,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("next_lesson_button")
            ) {
                Text("Bài sau")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(name = "Lesson Detail Light", showBackground = true)
@Composable
fun LessonDetailPreview() {
    HocTotTHCSTheme(darkTheme = false) {
        val dummyLesson = LessonEntity(
            id = "l1",
            chapterId = "c1",
            order = 1,
            title = "Tập hợp các số tự nhiên",
            summary = "Tập hợp các số tự nhiên được ký hiệu là ℕ = {0, 1, 2, ...}.\nCông thức: \$a + b = c\$.",
            examples = "Ví dụ 1: Viết tập hợp B các số nhỏ hơn 5.\nLời giải: B = {0, 1, 2, 3, 4}.",
            tips = "Mẹo: Số 0 là số tự nhiên nhỏ nhất.",
            formulasLatex = "A = \\{x \\in \\mathbb{N} \\mid x < 10\\}",
            isCompleted = false
        )
        LessonDetailContent(
            lesson = dummyLesson,
            subjectColor = Color(0xFF4A7FA8),
            navigation = LessonNavigationInfo("p1", "n1", 1, 10),
            fontScale = 1.0f,
            onToggleCompleted = {},
            onNavigatePrevious = {},
            onNavigateNext = {},
            onStartPractice = {}
        )
    }
}
