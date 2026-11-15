package com.example.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.core.graphics.toColorInt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CrossSeriesSuggestion
import com.example.domain.model.SearchFilter
import com.example.domain.model.SearchGroupedSection
import com.example.domain.model.SearchResult
import com.example.domain.model.SearchResultType
import com.example.domain.model.SearchSuggestion
import com.example.domain.model.SuggestionType
import com.example.domain.search.TextNormalizer
import com.example.ui.theme.CorrectGreen
import com.example.ui.theme.PrimaryBlue

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLesson: (lessonId: String) -> Unit,
    onNavigateToChapter: (subjectId: String) -> Unit,
    onStartPracticeQuiz: (lessonId: String, subjectId: String) -> Unit,
    onNavigateToTool: (route: String) -> Unit = {}
) {
    val query by viewModel.query.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isIndexing by viewModel.isIndexing.collectAsState()

    var selectedQuestionResult by remember { mutableStateOf<SearchResult?>(null) }
    var selectedFlashcardResult by remember { mutableStateOf<SearchResult?>(null) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        // Tự động focus vào ô tìm kiếm khi mở màn hình
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Header thanh tìm kiếm
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("search_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }

                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChanged(it) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .testTag("search_text_input"),
                        placeholder = {
                            Text(
                                text = "Tìm bài học, công thức, câu hỏi, từ vựng...",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Tìm kiếm",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onQueryChanged("") },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Xóa từ khóa",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                viewModel.onQuerySubmitted()
                            }
                        )
                    )
                }

                // Thanh tiến độ dựng chỉ mục nền nếu đang chạy
                AnimatedVisibility(visible = isIndexing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Đang tối ưu chỉ mục tìm kiếm nền...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                        )
                    }
                }

                // Bộ lọc chip nằm ngang
                SearchFilterChipsBar(
                    filter = filter,
                    onGradeChanged = { viewModel.onFilterGradeChanged(it) },
                    onSeriesChanged = { viewModel.onFilterSeriesChanged(it) },
                    onSubjectChanged = { viewModel.onFilterSubjectChanged(it) },
                    onPhanMonChanged = { viewModel.onFilterPhanMonChanged(it) },
                    onTypeChanged = { viewModel.onFilterTypeChanged(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is SearchUiState.Idle -> {
                    SearchIdleContent(
                        state = state,
                        onSelectQuery = { viewModel.onQuerySubmitted(it) },
                        onDeleteRecent = { viewModel.deleteRecentSearch(it) },
                        onClearAllRecents = { viewModel.clearAllRecentSearches() },
                        onToggleHistory = { viewModel.toggleHistoryEnabled(it) }
                    )
                }
                is SearchUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Đang tìm kiếm...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is SearchUiState.Success -> {
                    SearchResultsList(
                        state = state,
                        query = query,
                        onResultClick = { result ->
                            when (result.type) {
                                SearchResultType.LESSON -> onNavigateToLesson(result.targetId)
                                SearchResultType.CHAPTER -> onNavigateToChapter(result.subjectId)
                                SearchResultType.QUESTION -> selectedQuestionResult = result
                                SearchResultType.FLASHCARD -> selectedFlashcardResult = result
                                SearchResultType.SUBJECT -> onNavigateToChapter(result.targetId)
                                SearchResultType.TOOL -> onNavigateToTool(result.targetId)
                            }
                        },
                        onSeriesSwitch = { viewModel.switchSeries(it) }
                    )
                }
                is SearchUiState.Empty -> {
                    SearchEmptyContent(
                        state = state,
                        onSwitchSeries = { viewModel.switchSeries(it) }
                    )
                }
                is SearchUiState.Error -> {
                    SearchErrorContent(
                        message = state.errorMessage,
                        onRetry = { viewModel.retry() }
                    )
                }
            }
        }
    }

    // Dialog xem trước câu hỏi
    selectedQuestionResult?.let { questionResult ->
        QuestionDetailDialog(
            result = questionResult,
            onDismiss = { selectedQuestionResult = null },
            onPractice = {
                selectedQuestionResult = null
                onStartPracticeQuiz(questionResult.targetId, questionResult.subjectId)
            }
        )
    }

    // Dialog xem trước thẻ Flashcard
    selectedFlashcardResult?.let { flashcardResult ->
        FlashcardDetailDialog(
            result = flashcardResult,
            onDismiss = { selectedFlashcardResult = null }
        )
    }
}

/**
 * Thanh bộ lọc chip cuộn ngang
 */
@Composable
fun SearchFilterChipsBar(
    filter: SearchFilter,
    onGradeChanged: (Int) -> Unit,
    onSeriesChanged: (String) -> Unit,
    onSubjectChanged: (String) -> Unit,
    onPhanMonChanged: (String) -> Unit,
    onTypeChanged: (SearchResultType?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Chip Lớp
        item {
            FilterChip(
                selected = false,
                onClick = {
                    val nextGrade = if (filter.grade >= 9) 6 else filter.grade + 1
                    onGradeChanged(nextGrade)
                },
                label = { Text("Lớp ${filter.grade}") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Chọn lớp",
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("filter_chip_grade")
            )
        }

        // 2. Chip Bộ sách
        item {
            val seriesLabel = when (filter.series.uppercase()) {
                "CURRENT" -> "Bộ đang học"
                "KNTT" -> "Kết nối tri thức"
                "CD" -> "Cánh Diều"
                "CTST" -> "Chân trời"
                "CHUNG" -> "Chung"
                "ALL" -> "Tất cả bộ sách"
                else -> filter.series
            }
            FilterChip(
                selected = filter.series != "CURRENT",
                onClick = {
                    val nextSeries = when (filter.series.uppercase()) {
                        "CURRENT" -> "kntt"
                        "KNTT" -> "cd"
                        "CD" -> "ctst"
                        "CTST" -> "chung"
                        "CHUNG" -> "ALL"
                        else -> "CURRENT"
                    }
                    onSeriesChanged(nextSeries)
                },
                label = { Text(seriesLabel) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("filter_chip_series")
            )
        }

        // 3. Chip Loại kết quả
        item {
            val typeLabel = filter.resultType?.displayName ?: "Mọi loại kết quả"
            FilterChip(
                selected = filter.resultType != null,
                onClick = {
                    val nextType = when (filter.resultType) {
                        null -> SearchResultType.LESSON
                        SearchResultType.LESSON -> SearchResultType.QUESTION
                        SearchResultType.QUESTION -> SearchResultType.FLASHCARD
                        SearchResultType.FLASHCARD -> SearchResultType.CHAPTER
                        SearchResultType.CHAPTER -> SearchResultType.TOOL
                        SearchResultType.TOOL -> null
                        else -> null
                    }
                    onTypeChanged(nextType)
                },
                label = { Text(typeLabel) },
                modifier = Modifier.testTag("filter_chip_type")
            )
        }

        // 4. Chip Môn học
        item {
            val subjectLabel = if (filter.subjectId == "ALL") "Tất cả môn" else filter.subjectId.uppercase()
            FilterChip(
                selected = filter.subjectId != "ALL",
                onClick = {
                    val nextSubject = when (filter.subjectId) {
                        "ALL" -> "toan"
                        "toan" -> "van"
                        "van" -> "tienganh"
                        "tienganh" -> "khtn"
                        "khtn" -> "lsdl"
                        else -> "ALL"
                    }
                    onSubjectChanged(nextSubject)
                },
                label = { Text(subjectLabel) },
                modifier = Modifier.testTag("filter_chip_subject")
            )
        }

        // 5. Chip Phần môn (nếu đang ở KHTN hoặc LSDL)
        if (filter.subjectId.equals("khtn", ignoreCase = true) || filter.subjectId.equals("lsdl", ignoreCase = true)) {
            item {
                val phanMonLabel = if (filter.phanMon == "ALL") "Toàn bộ phân môn" else filter.phanMon.uppercase()
                FilterChip(
                    selected = filter.phanMon != "ALL",
                    onClick = {
                        val nextPhanMon = if (filter.subjectId.equals("khtn", ignoreCase = true)) {
                            when (filter.phanMon) {
                                "ALL" -> "ly"
                                "ly" -> "hoa"
                                "hoa" -> "sinh"
                                else -> "ALL"
                            }
                        } else {
                            when (filter.phanMon) {
                                "ALL" -> "lichsu"
                                "lichsu" -> "diali"
                                else -> "ALL"
                            }
                        }
                        onPhanMonChanged(nextPhanMon)
                    },
                    label = { Text(phanMonLabel) },
                    modifier = Modifier.testTag("filter_chip_phanmon")
                )
            }
        }
    }
}

/**
 * Giao diện khi chưa gõ từ khóa (Idle):
 * - Tìm kiếm gần đây (tối đa 10 mục)
 * - Chủ đề đang học dở
 * - Môn học yêu thích
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchIdleContent(
    state: SearchUiState.Idle,
    onSelectQuery: (String) -> Unit,
    onDeleteRecent: (String) -> Unit,
    onClearAllRecents: () -> Unit,
    onToggleHistory: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Tìm kiếm gần đây
        if (state.recentSearches.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Lịch sử tìm kiếm",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tìm kiếm gần đây",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = onClearAllRecents,
                        modifier = Modifier.testTag("clear_recent_searches_button")
                    ) {
                        Text(
                            text = "Xóa tất cả",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    state.recentSearches.forEach { searchItem ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { onSelectQuery(searchItem) }
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = searchItem,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onDeleteRecent(searchItem) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Xóa $searchItem",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Gợi ý chủ đề đang học và môn yêu thích
        if (state.suggestions.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gợi ý",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Khám phá & Học tiếp",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(state.suggestions) { suggestion ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectQuery(suggestion.text) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when (suggestion.type) {
                                        SuggestionType.PINNED_SUBJECT -> MaterialTheme.colorScheme.primaryContainer
                                        SuggestionType.IN_PROGRESS_TOPIC -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (suggestion.type) {
                                    SuggestionType.PINNED_SUBJECT -> Icons.Default.Book
                                    SuggestionType.IN_PROGRESS_TOPIC -> Icons.Default.PlayArrow
                                    else -> Icons.Default.Search
                                },
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = suggestion.text,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (suggestion.extraInfo.isNotBlank()) {
                                Text(
                                    text = suggestion.extraInfo,
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

/**
 * Danh sách kết quả tìm kiếm đã phân nhóm (Success)
 */
@Composable
fun SearchResultsList(
    state: SearchUiState.Success,
    query: String,
    onResultClick: (SearchResult) -> Unit,
    onSeriesSwitch: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_results_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner gợi ý bộ sách khác nếu có
        if (state.crossSeriesSuggestions.isNotEmpty()) {
            item {
                CrossSeriesSuggestionBanner(
                    suggestions = state.crossSeriesSuggestions,
                    onSwitch = onSeriesSwitch
                )
            }
        }

        // Nhóm từng loại kết quả
        state.groupedSections.forEach { section ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "${section.title}, gồm ${section.items.size} kết quả"
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (section.type) {
                            SearchResultType.LESSON -> Icons.Default.MenuBook
                            SearchResultType.QUESTION -> Icons.Default.HelpOutline
                            SearchResultType.FLASHCARD -> Icons.Default.Style
                            SearchResultType.CHAPTER -> Icons.Default.Book
                            SearchResultType.SUBJECT -> Icons.Default.Book
                            SearchResultType.TOOL -> Icons.Default.Calculate
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            items(section.items, key = { "${it.type}_${it.targetId}_${it.rowId}" }) { item ->
                SearchResultRow(
                    result = item,
                    query = query,
                    onClick = { onResultClick(item) }
                )
            }
        }
    }
}

/**
 * Một dòng kết quả hiển thị chi tiết theo quy định F13
 */
@Composable
fun SearchResultRow(
    result: SearchResult,
    query: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("search_result_item_${result.targetId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Hàng trên: Nhãn môn, nhãn bộ sách, nhãn lớp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nhãn môn học
                Surface(
                    color = parseSubjectColor(result.subjectColor).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = result.subjectName,
                        style = MaterialTheme.typography.labelSmall,
                        color = parseSubjectColor(result.subjectColor),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Nhãn bộ sách
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = result.seriesDisplayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Nhãn lớp
                if (result.grade > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Lớp ${result.grade}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Trạng thái đã học / chưa học
                if (result.type == SearchResultType.LESSON) {
                    Icon(
                        imageVector = if (result.isCompleted) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                        contentDescription = if (result.isCompleted) "Đã hoàn thành" else "Chưa học",
                        tint = if (result.isCompleted) CorrectGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tiêu đề có highlight từ khóa
            Text(
                text = highlightAnnotatedText(result.title, query),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Đoạn trích (snippet) có highlight từ khóa
            if (result.snippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = highlightAnnotatedText(result.snippet, query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Nhãn hiển thị nếu bài học có ở nhiều bộ sách (khi gộp cùng topicId)
            if (result.matchedSeriesList.size > 1) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Có ở các bộ sách: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    result.matchedSeriesList.forEach { sCode ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = sCode.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Banner gợi ý thông minh khi bộ sách khác có kết quả
 */
@Composable
fun CrossSeriesSuggestionBanner(
    suggestions: List<CrossSeriesSuggestion>,
    onSwitch: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            suggestions.forEach { sug ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Có ${sug.resultCount} kết quả ở bộ ${sug.seriesDisplayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }

                    Button(
                        onClick = { onSwitch(sug.targetSeries) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Xem ngay",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Trạng thái rỗng thân thiện (Empty)
 */
@Composable
fun SearchEmptyContent(
    state: SearchUiState.Empty,
    onSwitchSeries: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Không tìm thấy kết quả phù hợp",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Từ khóa: \"${state.query}\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = state.helpfulTips,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (state.crossSeriesSuggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            CrossSeriesSuggestionBanner(
                suggestions = state.crossSeriesSuggestions,
                onSwitch = onSwitchSeries
            )
        }
    }
}

/**
 * Trạng thái lỗi (Error)
 */
@Composable
fun SearchErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Lỗi tìm kiếm",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRetry) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Thử lại")
        }
    }
}

/**
 * Dialog hiển thị chi tiết câu hỏi trắc nghiệm khi người học bấm vào kết quả
 */
@Composable
fun QuestionDetailDialog(
    result: SearchResult,
    onDismiss: () -> Unit,
    onPractice: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Chi tiết câu hỏi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = parseSubjectColor(result.subjectColor).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${result.subjectName} • ${result.seriesDisplayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = parseSubjectColor(result.subjectColor),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = result.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                if (result.extraDetails.isNotBlank()) {
                    HorizontalDivider()
                    Text(
                        text = "Lời giải & Gợi ý:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = result.extraDetails,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onPractice) {
                Text("Luyện tập bài này")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

/**
 * Dialog hiển thị thẻ Flashcard có thể lật xem
 */
@Composable
fun FlashcardDetailDialog(
    result: SearchResult,
    onDismiss: () -> Unit
) {
    var isFlipped by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Thẻ ghi nhớ Flashcard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isFlipped = !isFlipped },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFlipped) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (!isFlipped) "MẶT TRƯỚC (Bấm để lật)" else "MẶT SAU",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFlipped) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (!isFlipped) result.title else result.extraDetails.ifBlank { result.snippet },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isFlipped) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Lật thẻ",
                        tint = if (isFlipped) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Đã hiểu")
            }
        }
    )
}

/**
 * Biến chuỗi văn bản thành AnnotatedString với các từ khóa được highlight đậm và màu Primary
 */
fun highlightAnnotatedText(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    val segments = TextNormalizer.highlightText(text, query)
    return buildAnnotatedString {
        segments.forEach { (chunk, isHighlight) ->
            if (isHighlight) {
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryBlue,
                        background = PrimaryBlue.copy(alpha = 0.15f)
                    )
                ) {
                    append(chunk)
                }
            } else {
                append(chunk)
            }
        }
    }
}

fun parseSubjectColor(colorHex: String): Color {
    return try {
        Color(colorHex.toColorInt())
    } catch (_: Exception) {
        PrimaryBlue
    }
}

// === PREVIEWS THEO YÊU CẦU F13 ===

@Preview(showBackground = true, name = "Trạng thái rỗng / Chưa gõ")
@Composable
fun PreviewSearchIdle() {
    SearchIdleContent(
        state = SearchUiState.Idle(
            recentSearches = listOf("phân số", "tập hợp", "tam giác đều"),
            suggestions = listOf(
                SearchSuggestion("Bài 1: Tập hợp", SuggestionType.IN_PROGRESS_TOPIC),
                SearchSuggestion("Toán", SuggestionType.PINNED_SUBJECT)
            )
        ),
        onSelectQuery = {},
        onDeleteRecent = {},
        onClearAllRecents = {},
        onToggleHistory = {}
    )
}

@Preview(showBackground = true, name = "Trạng thái có kết quả")
@Composable
fun PreviewSearchSuccess() {
    val sampleResults = listOf(
        SearchResult(
            targetId = "l1",
            type = SearchResultType.LESSON,
            grade = 6,
            series = "kntt",
            subjectId = "toan",
            subjectName = "Toán",
            title = "Bài 1: Tập hợp các số tự nhiên",
            snippet = "Làm quen với tập hợp, phần tử thuộc tập hợp và cách ghi số tự nhiên...",
            relevanceScore = 150
        ),
        SearchResult(
            targetId = "q1",
            type = SearchResultType.QUESTION,
            grade = 6,
            series = "kntt",
            subjectId = "toan",
            subjectName = "Toán",
            title = "Cho tập hợp A = {1, 2, 3}. Khẳng định nào sau đây là đúng?",
            snippet = "Phương pháp giải: Xem lại định nghĩa phần tử thuộc tập hợp...",
            relevanceScore = 80
        )
    )

    SearchResultsList(
        state = SearchUiState.Success(
            query = "tap hop",
            filter = SearchFilter(grade = 6),
            results = sampleResults,
            groupedSections = listOf(
                SearchGroupedSection(SearchResultType.LESSON, "Bài học (1)", listOf(sampleResults[0])),
                SearchGroupedSection(SearchResultType.QUESTION, "Câu hỏi (1)", listOf(sampleResults[1]))
            )
        ),
        query = "tap hop",
        onResultClick = {},
        onSeriesSwitch = {}
    )
}

@Preview(showBackground = true, name = "Trạng thái không tìm thấy kết quả")
@Composable
fun PreviewSearchEmpty() {
    SearchEmptyContent(
        state = SearchUiState.Empty(
            query = "khongcotukhoanay",
            filter = SearchFilter(grade = 6),
            crossSeriesSuggestions = listOf(
                CrossSeriesSuggestion("cd", "Cánh Diều", 3)
            )
        ),
        onSwitchSeries = {}
    )
}

@Preview(showBackground = true, name = "Trạng thái lỗi")
@Composable
fun PreviewSearchError() {
    SearchErrorContent(
        message = "Không thể đọc dữ liệu cơ sở dữ liệu FTS.",
        onRetry = {}
    )
}
