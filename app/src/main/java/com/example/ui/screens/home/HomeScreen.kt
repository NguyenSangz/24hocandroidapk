package com.example.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AppBottomNavBar
import com.example.ui.components.AppTopBar
import com.example.ui.components.BottomTab
import com.example.ui.screens.practice.PracticeScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.stats.StatsScreen
import com.example.ui.screens.study.StudyScreen
import com.example.ui.viewmodel.AppViewModelProvider
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PracticeViewModel
import com.example.ui.viewmodel.ProfileViewModel
import com.example.ui.viewmodel.StatsViewModel
import com.example.ui.viewmodel.StudyViewModel
import com.example.ui.viewmodel.WrongBankViewModel

@Composable
fun HomeScreen(
    mainViewModel: MainViewModel,
    onNavigateToLesson: (String) -> Unit,
    onNavigateToSubjectChapterList: (String) -> Unit,
    onStartPracticeQuiz: (lessonId: String, subjectId: String, chapterId: String, count: Int, difficulty: String) -> Unit,
    onStartExam: (String, String) -> Unit,
    onOpenFlashcards: () -> Unit,
    onOpenWrongBank: () -> Unit,
    onNavigateToSelectGrade: () -> Unit,
    onNavigateToSearch: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToAiTutor: () -> Unit = {}
) {
    val selectedGrade by mainViewModel.selectedGrade.collectAsState()
    var currentTab by remember { mutableStateOf(BottomTab.STUDY) }

    val studyViewModel: StudyViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val practiceViewModel: PracticeViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val statsViewModel: StatsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val profileViewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val wrongBankViewModel: WrongBankViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val accountViewModel: com.example.ui.screens.account.AccountViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val accountState by accountViewModel.accountState.collectAsState()
    val wrongBankUiState by wrongBankViewModel.uiState.collectAsState()
    val dueCount = wrongBankUiState.stats.dueReviewCount

    val todayXp by profileViewModel.todayXp.collectAsState()
    val streakResult by profileViewModel.streakResult.collectAsState()
    val levelInfo by profileViewModel.levelInfo.collectAsState()

    var showGuestAiDialog by remember { mutableStateOf(false) }

    if (showGuestAiDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showGuestAiDialog = false },
            title = {
                androidx.compose.material3.Text(
                    text = "Gia sư AI học tập",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                androidx.compose.material3.Text(
                    text = "Tính năng Gia sư AI trực tuyến yêu cầu đăng nhập tài khoản để bảo đảm an toàn cho học sinh và hỗ trợ kiến thức đúng chuẩn GDPT 2018. Bạn có muốn đăng nhập ngay bây giờ không?"
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showGuestAiDialog = false
                        onNavigateToAccount()
                    }
                ) {
                    androidx.compose.material3.Text("Đăng nhập ngay")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showGuestAiDialog = false }
                ) {
                    androidx.compose.material3.Text("Để sau")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = when (currentTab) {
                    BottomTab.STUDY -> "Trang chủ - Lớp $selectedGrade"
                    BottomTab.PRACTICE -> "Luyện tập"
                    BottomTab.STATS -> "Thống kê & Thành tích"
                    BottomTab.PROFILE -> "Hồ sơ cá nhân"
                },
                gradeText = "Lớp $selectedGrade",
                canNavigateBack = false,
                onGradeClick = onNavigateToSelectGrade,
                actions = {
                    IconButton(
                        onClick = {
                            if (accountState.isAuthenticated) {
                                onNavigateToAiTutor()
                            } else {
                                showGuestAiDialog = true
                            }
                        },
                        modifier = Modifier.testTag("top_ai_tutor_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Trợ lý Học tập AI",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { onNavigateToSearch("") },
                        modifier = Modifier.testTag("top_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Tìm kiếm toàn ứng dụng"
                        )
                    }
                }
            )
        },
        bottomBar = {
            AppBottomNavBar(
                selectedTab = currentTab,
                onTabSelected = { currentTab = it },
                practiceBadgeCount = dueCount
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (accountState.sessionExpired) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_expired_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⚠️ Phiên đăng nhập đã hết hạn. Bạn vẫn có thể học tiếp phần đã tải.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.TextButton(
                            onClick = onNavigateToAccount,
                            modifier = Modifier.testTag("session_expired_relogin_button")
                        ) {
                            Text(
                                text = "Đăng nhập lại",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
            when (currentTab) {
                BottomTab.STUDY -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_gamification_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Whatshot,
                                        contentDescription = null,
                                        tint = Color(0xFFEA580C),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Chuỗi ${streakResult?.currentStreak ?: 1} ngày",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = Color(0xFFC08A52),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "+$todayXp XP",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "Cấp ${levelInfo.level}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            StudyScreen(
                                viewModel = studyViewModel,
                                onNavigateToLesson = onNavigateToLesson,
                                onNavigateToGradeSelect = onNavigateToSelectGrade,
                                onNavigateToSubjectChapterList = onNavigateToSubjectChapterList,
                                onNavigateToSearch = onNavigateToSearch
                            )
                        }
                    }
                }
                BottomTab.PRACTICE -> {
                    PracticeScreen(
                        viewModel = practiceViewModel,
                        onStartCustomPractice = { subjectId, chapterId, count, difficulty ->
                            onStartPracticeQuiz("none", subjectId, chapterId, count, difficulty)
                        },
                        onStartSubjectQuiz = { subjectId ->
                            onStartPracticeQuiz("none", subjectId, "all", 10, "MIXED")
                        },
                        onStartExam = onStartExam,
                        onOpenFlashcards = onOpenFlashcards,
                        onOpenWrongBank = onOpenWrongBank,
                        dueMistakesCount = dueCount
                    )
                }
                BottomTab.STATS -> {
                    StatsScreen(
                        viewModel = statsViewModel,
                        onNavigateToChapterPractice = { subjectId, chapterId ->
                            onStartPracticeQuiz("none", subjectId, chapterId, 10, "MIXED")
                        },
                        onNavigateToWrongBankChapter = { _, _ ->
                            onOpenWrongBank()
                        },
                        onNavigateToSubjectDetail = { subjectId ->
                            onNavigateToSubjectChapterList(subjectId)
                        }
                    )
                }
                BottomTab.PROFILE -> {
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onNavigateToGradeSelect = onNavigateToSelectGrade,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToAccount = onNavigateToAccount
                    )
                }
            }
        }
        }
    }
}
