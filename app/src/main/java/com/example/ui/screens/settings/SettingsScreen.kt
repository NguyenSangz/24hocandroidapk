package com.example.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.domain.model.LessonFontSize
import com.example.domain.repository.PushSettingsController
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAccount: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToRestore: () -> Unit = {},
    onNavigateToPdfReport: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToDemoLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val selectedGrade by viewModel.selectedGrade.collectAsState()
    val defaultSeries by viewModel.defaultSeries.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val reduceMotion by viewModel.reduceMotion.collectAsState()
    val lessonFontSize by viewModel.lessonFontSize.collectAsState()
    val defaultQuestions by viewModel.defaultQuestionsPerSession.collectAsState()
    val ttsSpeechRate by viewModel.ttsSpeechRate.collectAsState()
    val soundEffectsEnabled by viewModel.soundEffectsEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val reminderEnabled by viewModel.reminderEnabled.collectAsState()
    val reminderHour by viewModel.reminderHour.collectAsState()
    val reminderMinute by viewModel.reminderMinute.collectAsState()
    val dailyQuestions by viewModel.dailyGoalQuestions.collectAsState()
    val dailyMinutes by viewModel.dailyGoalMinutes.collectAsState()
    val levelUpDialogEnabled by viewModel.levelUpDialogEnabled.collectAsState()

    // ---- F20a: trạng thái thông báo đẩy ----
    val pushEnabled by viewModel.pushEnabled.collectAsState()
    val isPushSupported by viewModel.isPushSupported.collectAsState()
    val isPushSignedIn by viewModel.isPushSignedIn.collectAsState()

    val pendingGradeChange by viewModel.pendingGradeChange.collectAsState()
    val pendingSeriesChange by viewModel.pendingSeriesChange.collectAsState()
    val showResetSettingsDialog by viewModel.showResetSettingsDialog.collectAsState()
    val clearDataStep by viewModel.clearDataStep.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showPushRationaleDialog by remember { mutableStateOf(false) }

    // TTS phục vụ nghe thử tốc độ đọc phát âm tiếng Anh
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Sẵn sàng
            }
        }
        ttsInstance = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.testReminderNotification(context)
            }
        }
    )

    // F20a: xin quyền POST_NOTIFICATIONS đúng lúc người dùng chủ động bật thông báo đẩy.
    val pushPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                viewModel.setPushEnabled(true)
            } else {
                // Từ chối quyền: tắt công tắc và hướng dẫn mở Cài đặt hệ thống.
                viewModel.setPushEnabled(false)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Bạn đã từ chối quyền thông báo. Có thể bật lại trong Cài đặt hệ thống của ứng dụng.",
                        actionLabel = "Mở cài đặt",
                        withDismissAction = true
                    )
                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        openAppNotificationSettings(context)
                    }
                }
            }
        }
    )

    // Cập nhật trạng thái hỗ trợ/đăng nhập mỗi lần mở màn hình Cài đặt.
    LaunchedEffect(Unit) {
        viewModel.refreshPushState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cài đặt & Dữ liệu",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Tùy chỉnh ứng dụng & kiểm soát dữ liệu cá nhân",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. NHÓM GIAO DIỆN
            item {
                SettingsSectionCard(
                    title = "Giao diện",
                    icon = Icons.Default.DarkMode,
                    description = "Chế độ hiển thị, kích cỡ chữ và độ mượt hình ảnh"
                ) {
                    // Chế độ sáng / tối / theo hệ thống
                    Text(
                        text = "Chế độ giao diện",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionChip(
                            label = "Hệ thống",
                            isSelected = themeMode == "system",
                            modifier = Modifier.weight(1f).testTag("theme_system_chip"),
                            onClick = { viewModel.setThemeMode("system") }
                        )
                        ThemeOptionChip(
                            label = "Sáng",
                            isSelected = themeMode == "light",
                            modifier = Modifier.weight(1f).testTag("theme_light_chip"),
                            onClick = { viewModel.setThemeMode("light") }
                        )
                        ThemeOptionChip(
                            label = "Tối",
                            isSelected = themeMode == "dark",
                            modifier = Modifier.weight(1f).testTag("theme_dark_chip"),
                            onClick = { viewModel.setThemeMode("dark") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cỡ chữ bài học
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Cỡ chữ bài học", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LessonFontSize.entries.forEach { size ->
                            ThemeOptionChip(
                                label = size.displayName,
                                isSelected = lessonFontSize.equals(size.key, ignoreCase = true),
                                modifier = Modifier.weight(1f).testTag("font_size_${size.key}_chip"),
                                onClick = { viewModel.setLessonFontSize(size.key) }
                            )
                        }
                    }

                    // Xem trước văn bản mẫu
                    val sampleScale = LessonFontSize.fromKey(lessonFontSize).scaleFactor
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Xem trước cỡ chữ bài học:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Toán học là chiếc chìa khóa vạn năng mở ra những cánh cửa bí mật của tự nhiên.",
                                fontSize = (15 * sampleScale).sp,
                                lineHeight = (22 * sampleScale).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Giảm chuyển động
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.Animation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "Giảm hiệu ứng chuyển động", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    text = "Tối ưu tốc độ phản hồi cho máy yếu hoặc chống mỏi mắt",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = reduceMotion,
                            onCheckedChange = { viewModel.setReduceMotion(it) },
                            modifier = Modifier.testTag("reduce_motion_switch")
                        )
                    }
                }
            }

            // 2. NHÓM HỌC TẬP
            item {
                SettingsSectionCard(
                    title = "Học tập & Bộ sách",
                    icon = Icons.Default.School,
                    description = "Khối lớp, bộ giáo trình GDPT 2018 và cấu hình luyện tập"
                ) {
                    // Chọn khối lớp (6, 7, 8, 9)
                    Text(
                        text = "Khối lớp đang học (Toàn bộ tiến độ được bảo toàn khi đổi)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(6, 7, 8, 9).forEach { grade ->
                            ThemeOptionChip(
                                label = "Lớp $grade",
                                isSelected = selectedGrade == grade,
                                modifier = Modifier.weight(1f).testTag("select_grade_${grade}_chip"),
                                onClick = { viewModel.requestChangeGrade(grade) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bộ sách mặc định
                    Text(
                        text = "Bộ sách mặc định",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val seriesOptions = listOf(
                            "kntt" to "Kết nối tri thức với cuộc sống",
                            "ctst" to "Chân trời sáng tạo",
                            "cd" to "Cánh Diều",
                            "chung" to "Nội dung chuẩn chung THCS"
                        )
                        seriesOptions.forEach { (key, label) ->
                            SeriesSelectRow(
                                title = label,
                                isSelected = defaultSeries.equals(key, ignoreCase = true),
                                onClick = { viewModel.requestChangeSeries(key) },
                                testTag = "series_option_$key"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Số câu hỏi mặc định mỗi phiên luyện tập
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Số câu hỏi mỗi phiên luyện tập",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "$defaultQuestions câu",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Slider(
                        value = defaultQuestions.toFloat(),
                        onValueChange = { viewModel.setDefaultQuestionsPerSession(it.toInt()) },
                        valueRange = 5f..30f,
                        steps = 4,
                        modifier = Modifier.testTag("default_questions_slider")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Âm thanh khi làm bài
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Âm thanh hiệu ứng khi làm bài", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                        Switch(
                            checked = soundEffectsEnabled,
                            onCheckedChange = { viewModel.setSoundEffects(it) },
                            modifier = Modifier.testTag("sound_effects_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Rung phản hồi
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Rung phản hồi", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                        Switch(
                            checked = vibrationEnabled,
                            onCheckedChange = { viewModel.setVibration(it) },
                            modifier = Modifier.testTag("vibration_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tốc độ đọc phát âm tiếng Anh (TTS)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(text = "Tốc độ đọc phát âm Tiếng Anh (TTS)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                text = "Hiện tại: ${String.format(Locale.US, "%.2fx", ttsSpeechRate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                ttsInstance?.let { tts ->
                                    tts.language = Locale.US
                                    tts.setSpeechRate(ttsSpeechRate)
                                    tts.speak("Hello! Welcome to English practice.", TextToSpeech.QUEUE_FLUSH, null, "test_tts")
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("tts_test_button")
                        ) {
                            Text("Nghe thử")
                        }
                    }
                    Slider(
                        value = ttsSpeechRate,
                        onValueChange = { viewModel.setTtsSpeechRate((Math.round(it * 20) / 20f)) },
                        valueRange = 0.5f..1.5f,
                        steps = 9,
                        modifier = Modifier.testTag("tts_speech_rate_slider")
                    )
                }
            }

            // 3. NHÓM MỤC TIÊU VÀ NHẮC HỌC
            item {
                SettingsSectionCard(
                    title = "Mục tiêu & Nhắc nhở",
                    icon = Icons.Default.TrackChanges,
                    description = "Mục tiêu rèn luyện hàng ngày và thông báo học tập"
                ) {
                    // Mục tiêu số câu hỏi mỗi ngày
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Mục tiêu câu hỏi mỗi ngày", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        Text(text = "$dailyQuestions câu", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Slider(
                        value = dailyQuestions.toFloat(),
                        onValueChange = { viewModel.setDailyGoals(it.toInt(), dailyMinutes) },
                        valueRange = 5f..50f,
                        steps = 8,
                        modifier = Modifier.testTag("daily_questions_slider")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mục tiêu thời gian tự học
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Mục tiêu thời gian học mỗi ngày", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        Text(text = "$dailyMinutes phút", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Slider(
                        value = dailyMinutes.toFloat(),
                        onValueChange = { viewModel.setDailyGoals(dailyQuestions, it.toInt()) },
                        valueRange = 10f..120f,
                        steps = 10,
                        modifier = Modifier.testTag("daily_minutes_slider")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Nhắc học hằng ngày
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFC08A52))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "Nhắc học hằng ngày", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    text = "${String.format(Locale.getDefault(), "%02d:%02d", reminderHour, reminderMinute)} mỗi tối",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { viewModel.setReminder(it, reminderHour, reminderMinute) },
                            modifier = Modifier.testTag("reminder_switch")
                        )
                    }

                    if (reminderEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showTimePickerDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.weight(1f).testTag("change_time_button")
                            ) {
                                Text("Đổi giờ nhắc", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.testReminderNotification(context)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.weight(1f).testTag("test_notification_button")
                            ) {
                                Text("Thử thông báo", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Thông báo lên cấp
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Celebration, contentDescription = null, tint = Color(0xFFC08A52))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Chúc mừng khi lên cấp độ", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                        Switch(
                            checked = levelUpDialogEnabled,
                            onCheckedChange = { viewModel.setLevelUpDialogEnabled(it) },
                            modifier = Modifier.testTag("level_up_dialog_switch")
                        )
                    }
                }
            }

            // 3b. NHÓM THÔNG BÁO ĐẨY (F20a) – chỉ hiện khi thiết bị hỗ trợ và cờ bật.
            if (isPushSupported) {
                item {
                    SettingsSectionCard(
                        title = "Thông báo đẩy",
                        icon = Icons.Default.Notifications,
                        description = "Nhắc học, bài tập và báo cáo khi thiết bị có mạng"
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Nhận thông báo từ máy chủ",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Chỉ gửi mã loại và mã nội dung, không gửi tên hay điểm số của bạn.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = pushEnabled && isPushSignedIn,
                                enabled = isPushSignedIn,
                                onCheckedChange = { enabled ->
                                    if (!enabled) {
                                        viewModel.setPushEnabled(false)
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                    ) {
                                        // Giải thích trước khi mở hộp thoại quyền hệ thống.
                                        showPushRationaleDialog = true
                                    } else {
                                        viewModel.setPushEnabled(true)
                                    }
                                },
                                modifier = Modifier.testTag("push_notifications_switch")
                            )
                        }

                        if (!isPushSignedIn) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Bạn đang dùng chế độ Khách. Đăng nhập tài khoản để nhận thông báo đẩy — chế độ Khách không gửi mã thiết bị lên máy chủ.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("push_guest_explanation")
                            )
                        } else if (pushEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Quyền thông báo của hệ điều hành đang tắt. Nhấn để mở Cài đặt và bật lại.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clickable { openAppNotificationSettings(context) }
                                    .testTag("push_permission_denied_hint")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nhắc học cục bộ ở trên vẫn hoạt động độc lập, kể cả khi không có mạng.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4. NHÓM DỮ LIỆU & BÁO CÁO (Dẫn tới F15b, F15c)
            item {
                SettingsSectionCard(
                    title = "Dữ liệu & Báo cáo",
                    icon = Icons.Default.Backup,
                    description = "Sao lưu ngoại tuyến, khôi phục và xuất báo cáo học tập PDF"
                ) {
                    SettingActionRow(
                        title = "Tài khoản & Đồng bộ đám mây",
                        subtitle = "Chế độ Khách hoặc liên kết Google, xác nhận phụ huynh, xóa tài khoản",
                        icon = Icons.Default.Backup,
                        onClick = onNavigateToAccount,
                        testTag = "settings_account_row"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingActionRow(
                        title = "Đăng nhập Demo (Google / Facebook / Khách)",
                        subtitle = "Trang đăng nhập trình diễn với 3 nhà cung cấp",
                        icon = Icons.Default.AccountCircle,
                        onClick = onNavigateToDemoLogin,
                        testTag = "settings_demo_login_row"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingActionRow(
                        title = "Sao lưu dữ liệu cục bộ",
                        subtitle = "Xuất toàn bộ tiến độ, điểm thi, flashcard ra tệp .zip an toàn",
                        icon = Icons.Default.Backup,
                        onClick = onNavigateToBackup,
                        testTag = "backup_data_row"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingActionRow(
                        title = "Khôi phục dữ liệu từ tệp",
                        subtitle = "Khôi phục dữ liệu từ bản sao lưu trước đó (hỗ trợ gộp hoặc thay thế)",
                        icon = Icons.Default.Restore,
                        onClick = onNavigateToRestore,
                        testTag = "restore_data_row"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingActionRow(
                        title = "Xuất báo cáo học tập (PDF)",
                        subtitle = "Tạo tài liệu PDF tổng kết cho phụ huynh và thầy cô giáo",
                        icon = Icons.Default.PictureAsPdf,
                        onClick = onNavigateToPdfReport,
                        testTag = "export_pdf_report_row"
                    )
                }
            }

            // 5. NHÓM GIỚI THIỆU & PHÁP LÝ (Dẫn tới F15d)
            item {
                SettingsSectionCard(
                    title = "Giới thiệu & Pháp lý",
                    icon = Icons.Default.Info,
                    description = "Thông tin phiên bản, bản quyền dữ liệu và quyền riêng tư"
                ) {
                    SettingActionRow(
                        title = "Nguồn dữ liệu & Giấy phép",
                        subtitle = "Bản đồ, từ điển, bảng tuần hoàn, thư viện mã nguồn mở",
                        icon = Icons.Default.Description,
                        onClick = onNavigateToAbout,
                        testTag = "about_license_row"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingActionRow(
                        title = "Chính sách quyền riêng tư",
                        subtitle = "Offline-first mặc định • Thu thập tối thiểu khi đăng nhập • Bảo vệ trẻ em (<16t)",
                        icon = Icons.Default.Policy,
                        onClick = onNavigateToPrivacy,
                        testTag = "privacy_policy_row"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Học Tốt THCS Việt Nam",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Phiên bản 1.0 (GDPT 2018)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 6. NHÓM ĐẶT LẠI & QUẢN TRỊ DỮ LIỆU
            item {
                SettingsSectionCard(
                    title = "Đặt lại & Quản trị",
                    icon = Icons.Default.Refresh,
                    description = "Khôi phục cài đặt gốc hoặc dọn dẹp dữ liệu học tập"
                ) {
                    // Nút đặt lại cài đặt về mặc định
                    OutlinedButton(
                        onClick = { viewModel.showResetSettingsConfirmation() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đặt lại cài đặt về mặc định (Không mất bài học)")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Nút xóa toàn bộ dữ liệu học tập
                    Button(
                        onClick = { viewModel.startClearDataFlow() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_all_data_button")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Xóa toàn bộ dữ liệu học tập cá nhân")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // --- HỘP THOẠI XÁC NHẬN ĐỔI LỚP ---
    if (pendingGradeChange != null) {
        val targetGrade = pendingGradeChange!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissChangeGrade() },
            title = { Text("Đổi sang Lớp $targetGrade?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Toàn bộ tiến độ, điểm số và bài tập ở Lớp $selectedGrade vẫn được lưu giữ nguyên vẹn trên thiết bị của bạn. " +
                        "Bạn có thể chuyển lại Lớp $selectedGrade bất kỳ lúc nào mà không bị mất dữ liệu."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmChangeGrade() },
                    modifier = Modifier.testTag("confirm_change_grade_button")
                ) {
                    Text("Đồng ý đổi")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissChangeGrade() }) {
                    Text("Hủy")
                }
            }
        )
    }

    // --- HỘP THOẠI XÁC NHẬN ĐỔI BỘ SÁCH ---
    if (pendingSeriesChange != null) {
        val targetSeries = pendingSeriesChange!!
        val seriesTitle = when (targetSeries) {
            "kntt" -> "Kết nối tri thức"
            "ctst" -> "Chân trời sáng tạo"
            "cd" -> "Cánh Diều"
            else -> "Chuẩn chung THCS"
        }
        AlertDialog(
            onDismissRequest = { viewModel.dismissChangeSeries() },
            title = { Text("Đổi sang bộ sách $seriesTitle?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Ứng dụng sẽ ưu tiên hiển thị nội dung theo bộ sách $seriesTitle. " +
                        "Toàn bộ lịch sử làm bài, điểm thi và thẻ ghi nhớ của các bộ sách khác đều được lưu giữ đầy đủ."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmChangeSeries() },
                    modifier = Modifier.testTag("confirm_change_series_button")
                ) {
                    Text("Đồng ý đổi")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissChangeSeries() }) {
                    Text("Hủy")
                }
            }
        )
    }

    // --- HỘP THOẠI ĐẶT LẠI CÀI ĐẶT ---
    if (showResetSettingsDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissResetSettingsConfirmation() },
            title = { Text("Đặt lại cài đặt về mặc định?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Các tùy chọn về giao diện (chế độ sáng/tối, cỡ chữ), âm thanh và nhắc học sẽ trở về trạng thái ban đầu. " +
                        "Tiến độ học tập, điểm thi và huy hiệu của bạn KHÔNG bị ảnh hưởng."
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmResetSettings() },
                    modifier = Modifier.testTag("confirm_reset_settings_button")
                ) {
                    Text("Đặt lại")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissResetSettingsConfirmation() }) {
                    Text("Hủy")
                }
            }
        )
    }

    // --- QUY TRÌNH XÓA TOÀN BỘ DỮ LIỆU (2 BƯỚC AN TOÀN) ---
    if (clearDataStep == 1) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelClearDataFlow() },
            icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Bước 1/2: Xóa toàn bộ dữ liệu?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Thao tác này sẽ xóa vĩnh viễn: lịch sử luyện tập, điểm các bài kiểm tra, ngân hàng câu sai, " +
                        "tiến độ học bài và thẻ flashcard cá nhân. Bạn có muốn sao lưu dữ liệu trước khi xóa không?"
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.proceedToClearDataStep2() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("clear_data_step1_proceed")
                ) {
                    Text("Tôi hiểu, tiếp tục")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelClearDataFlow() }) {
                    Text("Hủy bỏ")
                }
            }
        )
    } else if (clearDataStep == 2) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelClearDataFlow() },
            icon = { Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Bước 2/2: Xác nhận lần cuối!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Text(
                    "Dữ liệu đã xóa sẽ KHÔNG THỂ KHÔI PHỤC trừ khi bạn đã xuất file sao lưu trước đó. " +
                        "Bạn có chắc chắn 100% muốn xóa sạch dữ liệu học tập cá nhân không?"
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmClearAllStudyData() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("clear_data_step2_confirm")
                ) {
                    Text("Xác nhận Xóa sạch")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelClearDataFlow() }) {
                    Text("Không xóa")
                }
            }
        )
    }

    // --- F20a: GIẢI THÍCH TRƯỚC KHI XIN QUYỀN THÔNG BÁO ---
    if (showPushRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPushRationaleDialog = false },
            icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
            title = { Text("Bật thông báo đẩy?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Ứng dụng cần quyền thông báo của hệ điều hành để gửi nhắc học, bài tập và báo cáo từ máy chủ. " +
                        "Nội dung thông báo chỉ chứa mã loại và mã nội dung — tên, điểm số và dữ liệu học tập của bạn " +
                        "được dựng ngay trên máy, không gửi qua Google FCM. Bạn có thể tắt bất cứ lúc nào trong Cài đặt."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPushRationaleDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pushPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setPushEnabled(true)
                        }
                    },
                    modifier = Modifier.testTag("push_rationale_confirm")
                ) {
                    Text("Tiếp tục")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPushRationaleDialog = false }) {
                    Text("Để sau")
                }
            }
        )
    }

    // --- TIME PICKER DIALOG ---
    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = { Text("Chọn giờ nhắc học mỗi tối", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setReminder(true, timePickerState.hour, timePickerState.minute)
                        showTimePickerDialog = false
                    },
                    modifier = Modifier.testTag("confirm_time_picker_button")
                ) {
                    Text("Lưu giờ nhắc")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
private fun ThemeOptionChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun SeriesSelectRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Đã chọn",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * F20a – Mở màn hình cài đặt thông báo của hệ điều hành cho ứng dụng này.
 * Dùng khi người dùng đã từ chối quyền POST_NOTIFICATIONS và cần bật lại.
 */
private fun openAppNotificationSettings(context: Context) {
    try {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (t: Throwable) {
        // Một số ROM không hỗ trợ màn hình này — im lặng, không làm app lỗi.
    }
}

@Preview(name = "SettingsScreen Light", showBackground = true)
@Composable
fun SettingsScreenLightPreview() {
    MaterialTheme {
        SettingsScreen(
            viewModel = SettingsViewModel(FakePreviewSettingsRepository()),
            onNavigateBack = {}
        )
    }
}

@Preview(name = "SettingsScreen Dark", showBackground = true)
@Composable
fun SettingsScreenDarkPreview() {
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
        SettingsScreen(
            viewModel = SettingsViewModel(FakePreviewSettingsRepository()),
            onNavigateBack = {}
        )
    }
}

@Preview(name = "SettingsScreen Push enabled", showBackground = true)
@Composable
fun SettingsScreenPushPreview() {
    MaterialTheme {
        SettingsScreen(
            viewModel = SettingsViewModel(
                settingsRepository = FakePreviewSettingsRepository(),
                pushSettings = FakePreviewPushSettingsController()
            ),
            onNavigateBack = {}
        )
    }
}

/** F20a – Bản giả cho @Preview: giả lập thiết bị hỗ trợ và đã đăng nhập. */
private class FakePreviewPushSettingsController : PushSettingsController {
    override val isSupported = MutableStateFlow(true)
    override val isSignedIn = MutableStateFlow(true)
    override suspend fun setEnabled(enabled: Boolean) {}
    override suspend fun refresh() {}
}

private class FakePreviewSettingsRepository : SettingsRepository {
    override val selectedGrade = flowOf(6)
    override val isGradeSelected = flowOf(true)
    override val isFirstLaunch = flowOf(false)
    override val isDarkMode = flowOf(false)
    override val userName = flowOf("Học sinh THCS")
    override val themeMode = flowOf("system")
    override val reduceMotion = flowOf(false)
    override val lessonFontSize = flowOf("medium")
    override val defaultSeries = flowOf("kntt")
    override val defaultQuestionsPerSession = flowOf(10)
    override val ttsSpeechRate = flowOf(1.0f)
    override val soundEffectsEnabled = flowOf(true)
    override val vibrationEnabled = flowOf(true)
    override val reminderEnabled = flowOf(true)
    override val reminderHour = flowOf(19)
    override val reminderMinute = flowOf(30)
    override val dailyGoalQuestions = flowOf(15)
    override val dailyGoalMinutes = flowOf(30)
    override val levelUpDialogEnabled = flowOf(true)
    override val pushEnabled = flowOf(false)

    override suspend fun setGrade(grade: Int) {}
    override suspend fun resetGradeSelection() {}
    override suspend fun setDarkMode(enabled: Boolean) {}
    override suspend fun setThemeMode(mode: String) {}
    override suspend fun setReduceMotion(enabled: Boolean) {}
    override suspend fun setLessonFontSize(sizeKey: String) {}
    override suspend fun setDefaultSeries(series: String) {}
    override suspend fun setDefaultQuestionsPerSession(count: Int) {}
    override suspend fun setTtsSpeechRate(rate: Float) {}
    override suspend fun setSoundEffects(enabled: Boolean) {}
    override suspend fun setVibration(enabled: Boolean) {}
    override suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) {}
    override suspend fun setPushEnabled(enabled: Boolean) {}
    override suspend fun setDailyGoals(questions: Int, minutes: Int) {}
    override suspend fun setLevelUpDialogEnabled(enabled: Boolean) {}
    override suspend fun setUserName(name: String) {}
    override suspend fun setFirstLaunchCompleted() {}
    override suspend fun resetSettingsToDefault() {}
    override suspend fun clearAllStudyData() {}
}
