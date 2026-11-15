package com.example.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.PushSettingsController
import com.example.domain.repository.SettingsRepository
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ViewModel quản lý màn hình Cài đặt tổng hợp (F15a).
 * Sử dụng SettingsRepository làm điểm vào duy nhất, lưu trữ bền vững qua DataStore.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    /** F20a – null trong @Preview/test để ẩn phần thông báo đẩy. */
    private val pushSettings: PushSettingsController? = null
) : ViewModel() {

    val selectedGrade: StateFlow<Int> = settingsRepository.selectedGrade
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)

    val defaultSeries: StateFlow<String> = settingsRepository.defaultSeries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "kntt")

    val themeMode: StateFlow<String> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val reduceMotion: StateFlow<Boolean> = settingsRepository.reduceMotion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lessonFontSize: StateFlow<String> = settingsRepository.lessonFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "medium")

    val defaultQuestionsPerSession: StateFlow<Int> = settingsRepository.defaultQuestionsPerSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val ttsSpeechRate: StateFlow<Float> = settingsRepository.ttsSpeechRate
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val soundEffectsEnabled: StateFlow<Boolean> = settingsRepository.soundEffectsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val vibrationEnabled: StateFlow<Boolean> = settingsRepository.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val reminderEnabled: StateFlow<Boolean> = settingsRepository.reminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val reminderHour: StateFlow<Int> = settingsRepository.reminderHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 19)

    val reminderMinute: StateFlow<Int> = settingsRepository.reminderMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    val dailyGoalQuestions: StateFlow<Int> = settingsRepository.dailyGoalQuestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)

    val dailyGoalMinutes: StateFlow<Int> = settingsRepository.dailyGoalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    val levelUpDialogEnabled: StateFlow<Boolean> = settingsRepository.levelUpDialogEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // ---- F20a: Thông báo đẩy ----
    val pushEnabled: StateFlow<Boolean> = settingsRepository.pushEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPushSupported: StateFlow<Boolean> = pushSettings?.isSupported ?: MutableStateFlow(false)

    val isPushSignedIn: StateFlow<Boolean> = pushSettings?.isSignedIn ?: MutableStateFlow(false)

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Trạng thái các hộp thoại xác nhận bảo đảm an toàn dữ liệu
    private val _pendingGradeChange = MutableStateFlow<Int?>(null)
    val pendingGradeChange: StateFlow<Int?> = _pendingGradeChange.asStateFlow()

    private val _pendingSeriesChange = MutableStateFlow<String?>(null)
    val pendingSeriesChange: StateFlow<String?> = _pendingSeriesChange.asStateFlow()

    private val _showResetSettingsDialog = MutableStateFlow(false)
    val showResetSettingsDialog: StateFlow<Boolean> = _showResetSettingsDialog.asStateFlow()

    private val _clearDataStep = MutableStateFlow(0) // 0: không hiện, 1: bước 1, 2: bước 2 xác nhận cuối
    val clearDataStep: StateFlow<Int> = _clearDataStep.asStateFlow()

    fun requestChangeGrade(newGrade: Int) {
        if (newGrade == selectedGrade.value) return
        _pendingGradeChange.value = newGrade
    }

    fun confirmChangeGrade() {
        val grade = _pendingGradeChange.value ?: return
        viewModelScope.launch {
            settingsRepository.setGrade(grade)
            _pendingGradeChange.value = null
            _userMessage.emit("Đã chuyển sang Lớp $grade. Tiến độ các lớp khác vẫn được lưu giữ an toàn.")
        }
    }

    fun dismissChangeGrade() {
        _pendingGradeChange.value = null
    }

    fun requestChangeSeries(newSeries: String) {
        if (newSeries == defaultSeries.value) return
        _pendingSeriesChange.value = newSeries
    }

    fun confirmChangeSeries() {
        val series = _pendingSeriesChange.value ?: return
        viewModelScope.launch {
            settingsRepository.setDefaultSeries(series)
            _pendingSeriesChange.value = null
            _userMessage.emit("Đã đổi bộ sách mặc định. Toàn bộ tiến độ học tập vẫn được giữ nguyên.")
        }
    }

    fun dismissChangeSeries() {
        _pendingSeriesChange.value = null
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReduceMotion(enabled)
            _userMessage.emit(if (enabled) "Đã bật chế độ giảm chuyển động" else "Đã tắt chế độ giảm chuyển động")
        }
    }

    fun setLessonFontSize(sizeKey: String) {
        viewModelScope.launch {
            settingsRepository.setLessonFontSize(sizeKey)
        }
    }

    fun setDefaultQuestionsPerSession(count: Int) {
        viewModelScope.launch {
            settingsRepository.setDefaultQuestionsPerSession(count)
        }
    }

    fun setTtsSpeechRate(rate: Float) {
        viewModelScope.launch {
            settingsRepository.setTtsSpeechRate(rate)
        }
    }

    fun setSoundEffects(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSoundEffects(enabled)
        }
    }

    fun setVibration(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVibration(enabled)
        }
    }

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setReminder(enabled, hour, minute)
            if (enabled) {
                _userMessage.emit("Đã đặt nhắc học vào ${String.format(Locale.getDefault(), "%02d:%02d", hour, minute)} mỗi ngày")
            }
        }
    }

    fun setDailyGoals(questions: Int, minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setDailyGoals(questions, minutes)
        }
    }

    // ---- F20a: Thông báo đẩy ----

    /** Gọi khi mở màn hình Cài đặt để cập nhật trạng thái hỗ trợ/đăng nhập. */
    fun refreshPushState() {
        viewModelScope.launch {
            pushSettings?.refresh()
        }
    }

    fun setPushEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (pushSettings != null) {
                pushSettings.setEnabled(enabled)
            } else {
                settingsRepository.setPushEnabled(enabled)
            }
            _userMessage.emit(
                if (enabled) {
                    "Đã bật thông báo đẩy trên thiết bị này. Bạn có thể tắt bất cứ lúc nào."
                } else {
                    "Đã tắt thông báo đẩy. Nhắc học cục bộ (không cần mạng) vẫn hoạt động bình thường."
                }
            )
        }
    }

    fun setLevelUpDialogEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setLevelUpDialogEnabled(enabled)
        }
    }

    fun testReminderNotification(context: Context) {
        NotificationHelper.showStudyReminderNotification(
            context = context,
            title = "Đến giờ học bài rồi! \uD83D\uDCDA",
            message = "Mỗi ngày dành ra 15 phút ôn luyện để duy trì chuỗi học tập bạn nhé!"
        )
        viewModelScope.launch {
            _userMessage.emit("Đã gửi thông báo thử nghiệm!")
        }
    }

    fun showResetSettingsConfirmation() {
        _showResetSettingsDialog.value = true
    }

    fun dismissResetSettingsConfirmation() {
        _showResetSettingsDialog.value = false
    }

    fun confirmResetSettings() {
        viewModelScope.launch {
            settingsRepository.resetSettingsToDefault()
            _showResetSettingsDialog.value = false
            _userMessage.emit("Đã đặt lại toàn bộ cài đặt về mặc định. Tiến độ học của bạn vẫn được giữ nguyên!")
        }
    }

    fun startClearDataFlow() {
        _clearDataStep.value = 1
    }

    fun proceedToClearDataStep2() {
        _clearDataStep.value = 2
    }

    fun cancelClearDataFlow() {
        _clearDataStep.value = 0
    }

    fun confirmClearAllStudyData() {
        viewModelScope.launch {
            settingsRepository.clearAllStudyData()
            _clearDataStep.value = 0
            _userMessage.emit("Đã xóa toàn bộ dữ liệu học tập cá nhân.")
        }
    }
}
