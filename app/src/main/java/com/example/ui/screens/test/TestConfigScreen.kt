package com.example.ui.screens.test

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.domain.evaluator.ExamRuleConfig
import com.example.domain.model.TestType
import com.example.ui.theme.HocTotTHCSTheme
import com.example.ui.viewmodel.TestUiState
import com.example.ui.viewmodel.TestViewModel

/**
 * Màn hình cấu hình bài kiểm tra và xác nhận trước khi làm bài (F07).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestConfigScreen(
    viewModel: TestViewModel,
    onNavigateBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onRetryWrongQuestions: ((List<String>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedGrade by viewModel.selectedGrade.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    when (val state = uiState) {
        is TestUiState.Configuring -> {
            var isChapterDropdownExpanded by remember { mutableStateOf(false) }
            var isLessonDropdownExpanded by remember { mutableStateOf(false) }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Thiết lập bài kiểm tra",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("test_config_back_button")) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                            }
                        },
                        actions = {
                            IconButton(onClick = onOpenHistory, modifier = Modifier.testTag("test_history_button")) {
                                Icon(imageVector = Icons.Default.History, contentDescription = "Lịch sử kiểm tra")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("test_config_screen"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cảnh báo bài thi đang làm dở nếu có
                    if (state.hasActiveDraft && state.draftTest != null) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("draft_warning_card")
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Bạn có bài kiểm tra đang làm dở!",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${state.draftTest.title} (Còn lại ${state.draftTest.remainingSeconds / 60} phút)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(
                                        onClick = { viewModel.resumeDraftTest(state.draftTest) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("resume_draft_button")
                                    ) {
                                        Text("Tiếp tục")
                                    }
                                }
                            }
                        }
                    }

                    // 1. Chọn môn học
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "1. Môn học (Lớp $selectedGrade)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    subjects.forEach { subj ->
                                        val isSelected = subj.id == state.selectedSubjectId
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.onSelectSubject(subj.id) },
                                            label = { Text(subj.name) },
                                            leadingIcon = if (isSelected) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.testTag("subject_chip_${subj.id}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Chọn loại đề
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "2. Loại đề kiểm tra",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TestType.entries.forEach { type ->
                                        val isSelected = state.selectedType == type
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { viewModel.onSelectTestType(type) }
                                                .testTag("test_type_${type.name}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = when (type) {
                                                            TestType.LESSON -> Icons.Default.Assignment
                                                            TestType.CHAPTER -> Icons.Default.DateRange
                                                            TestType.MID_TERM, TestType.FINAL -> Icons.Default.Alarm
                                                        },
                                                        contentDescription = null,
                                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = type.title,
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                        )
                                                    )
                                                    Text(
                                                        text = type.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Chọn phạm vi (nếu chọn theo chương hoặc theo bài)
                    if (state.selectedType == TestType.CHAPTER || state.selectedType == TestType.LESSON) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "3. Phạm vi nội dung",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Dropdown Chương
                                    ExposedDropdownMenuBox(
                                        expanded = isChapterDropdownExpanded,
                                        onExpandedChange = { isChapterDropdownExpanded = it }
                                    ) {
                                        val selectedChapterTitle = state.chapters.find { it.id == state.selectedChapterId }?.title
                                            ?: "Tất cả các chương"
                                        OutlinedTextField(
                                            value = selectedChapterTitle,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Chọn chương") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isChapterDropdownExpanded) },
                                            modifier = Modifier
                                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                                .fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = isChapterDropdownExpanded,
                                            onDismissRequest = { isChapterDropdownExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Tất cả các chương") },
                                                onClick = {
                                                    viewModel.onSelectChapter("all")
                                                    isChapterDropdownExpanded = false
                                                }
                                            )
                                            state.chapters.forEach { chapter ->
                                                DropdownMenuItem(
                                                    text = { Text(chapter.title) },
                                                    onClick = {
                                                        viewModel.onSelectChapter(chapter.id)
                                                        isChapterDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Dropdown Bài (nếu là LESSON)
                                    if (state.selectedType == TestType.LESSON && state.lessons.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        ExposedDropdownMenuBox(
                                            expanded = isLessonDropdownExpanded,
                                            onExpandedChange = { isLessonDropdownExpanded = it }
                                        ) {
                                            val selectedLessonTitle = state.lessons.find { it.id == state.selectedLessonId }?.title
                                                ?: "Tất cả bài học"
                                            OutlinedTextField(
                                                value = selectedLessonTitle,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Chọn bài học cụ thể") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isLessonDropdownExpanded) },
                                                modifier = Modifier
                                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                                    .fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            ExposedDropdownMenu(
                                                expanded = isLessonDropdownExpanded,
                                                onDismissRequest = { isLessonDropdownExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Tất cả bài học") },
                                                    onClick = {
                                                        viewModel.onSelectLesson("none")
                                                        isLessonDropdownExpanded = false
                                                    }
                                                )
                                                state.lessons.forEach { lesson ->
                                                    DropdownMenuItem(
                                                        text = { Text(lesson.title) },
                                                        onClick = {
                                                            viewModel.onSelectLesson(lesson.id)
                                                            isLessonDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Số câu & Thời gian
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "4. Số câu và Thời gian làm bài",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(text = "Số lượng câu hỏi:", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ExamRuleConfig.availableQuestionCounts.forEach { count ->
                                        val isSelected = count == state.selectedQuestionCount
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.onSelectQuestionCount(count) },
                                            label = { Text("$count câu") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text(text = "Thời gian đếm ngược:", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ExamRuleConfig.availableDurationMinutes.forEach { mins ->
                                        val isSelected = mins == state.selectedDurationMinutes
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.onSelectDurationMinutes(mins) },
                                            label = { Text("$mins phút") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Thông báo lỗi nếu thiếu câu hỏi
                    if (state.validationError != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Chưa đủ câu hỏi",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = state.validationError,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    if (state.suggestedCount != null && state.suggestedCount > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { viewModel.onSelectQuestionCount(state.suggestedCount) },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Giảm xuống ${state.suggestedCount} câu để làm bài")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Nút Bắt đầu
                    item {
                        Button(
                            onClick = { viewModel.proceedToConfirmation() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_exam_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Xem quy tắc & Bắt đầu",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Màn hình xác nhận quy tắc trước khi thi (Confirmation Screen)
        is TestUiState.Confirming -> {
            ConfirmingDialogOrScreen(
                confirming = state,
                onBack = { viewModel.resetToConfig() },
                onStart = { viewModel.startTestFromConfirmation() }
            )
        }

        is TestUiState.InProgress -> {
            TestTakingScreen(viewModel = viewModel, onNavigateBack = onNavigateBack)
        }

        is TestUiState.Submitted -> {
            TestResultScreen(
                viewModel = viewModel,
                onNavigateBack = onNavigateBack,
                onOpenHistory = onOpenHistory,
                onRetryWrongQuestions = onRetryWrongQuestions
            )
        }

        is TestUiState.Reviewing -> {
            TestReviewScreen(viewModel = viewModel, onNavigateBack = { viewModel.resetToConfig() })
        }

        is TestUiState.Error -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Thông báo") },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.resetToConfig() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.resetToConfig() }) {
                            Text("Quay về thiết lập")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Màn hình xác nhận trước khi làm bài thi
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmingDialogOrScreen(
    confirming: TestUiState.Confirming,
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quy tắc bài kiểm tra", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = confirming.testTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Môn: ${confirming.subjectName} • ${confirming.questionCount} câu hỏi • ${confirming.durationMinutes} phút",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Quy định làm bài:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    confirming.rules.forEachIndexed { index, rule ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = "${index + 1}. ",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = rule,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onStart,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_and_start_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bắt đầu làm bài ngay", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Điều chỉnh lại cấu hình")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TestConfigScreenPreview() {
    HocTotTHCSTheme {
        ConfirmingDialogOrScreen(
            confirming = TestUiState.Confirming(
                testTitle = "Kiểm tra giữa kỳ - Toán học",
                testType = TestType.MID_TERM,
                subjectName = "Toán học",
                questionCount = 20,
                durationMinutes = 45,
                rules = ExamRuleConfig.examRulesText
            ),
            onBack = {},
            onStart = {}
        )
    }
}
