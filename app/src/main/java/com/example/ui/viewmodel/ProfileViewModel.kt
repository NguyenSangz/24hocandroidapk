package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.XpEventEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.GamificationRepository
import com.example.domain.gamification.BadgeDisplayItem
import com.example.domain.gamification.LevelCalculator
import com.example.domain.gamification.LevelInfo
import com.example.domain.gamification.StreakResult
import com.example.domain.repository.AuthAccountState
import com.example.domain.repository.AuthRepository
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userPreferencesManager: UserPreferencesManager,
    private val gamificationRepository: GamificationRepository? = null,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    val accountState: StateFlow<AuthAccountState> = (authRepository?.accountState ?: kotlinx.coroutines.flow.flowOf(AuthAccountState()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthAccountState())

    val selectedGrade: StateFlow<Int> = userPreferencesManager.selectedGrade
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)

    val userName: StateFlow<String> = userPreferencesManager.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Học sinh THCS")

    val avatarId: StateFlow<Int> = userPreferencesManager.avatarId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val dailyGoalQuestions: StateFlow<Int> = userPreferencesManager.dailyGoalQuestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 15)

    val dailyGoalMinutes: StateFlow<Int> = userPreferencesManager.dailyGoalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    val streakDays: StateFlow<Int> = userPreferencesManager.streakDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val isDarkMode: StateFlow<Boolean> = userPreferencesManager.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val reminderEnabled: StateFlow<Boolean> = userPreferencesManager.reminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val reminderHour: StateFlow<Int> = userPreferencesManager.reminderHour
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 19)

    val reminderMinute: StateFlow<Int> = userPreferencesManager.reminderMinute
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    val soundEffectsEnabled: StateFlow<Boolean> = userPreferencesManager.soundEffectsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val vibrationEnabled: StateFlow<Boolean> = userPreferencesManager.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val levelUpDialogEnabled: StateFlow<Boolean> = userPreferencesManager.levelUpDialogEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // === GAMIFICATION (F10) ===

    val totalXp: StateFlow<Int> = (gamificationRepository?.totalXp ?: kotlinx.coroutines.flow.flowOf(0))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val levelInfo: StateFlow<LevelInfo> = (gamificationRepository?.levelInfo ?: kotlinx.coroutines.flow.flowOf(LevelCalculator.calculateLevel(0)))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LevelCalculator.calculateLevel(0))

    val streakResult: StateFlow<StreakResult?> = (gamificationRepository?.streakResult ?: kotlinx.coroutines.flow.flowOf(null))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val streakFreezeCount: StateFlow<Int> = userPreferencesManager.streakFreezeCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val allBadges: StateFlow<List<BadgeDisplayItem>> = (gamificationRepository?.allBadgesWithProgress ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentXpEvents: StateFlow<List<XpEventEntity>> = (gamificationRepository?.recentXpEvents ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayXp: StateFlow<Int> = (gamificationRepository?.todayXp ?: kotlinx.coroutines.flow.flowOf(0))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    fun purchaseStreakFreeze() {
        viewModelScope.launch {
            val repo = gamificationRepository ?: return@launch
            val success = repo.purchaseStreakFreeze()
            if (success) {
                _userMessage.emit("Đã mua thành công 1 lượt đóng băng streak (-200 XP)!")
            } else {
                _userMessage.emit("Không đủ 200 XP hoặc đã đạt tối đa 2 lượt đóng băng!")
            }
        }
    }

    fun toggleLevelUpDialog(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.setLevelUpDialogEnabled(enabled)
        }
    }

    fun toggleSoundEffects(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.setSoundEffects(enabled)
        }
    }

    fun toggleVibration(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.setVibration(enabled)
        }
    }

    fun updateName(name: String) {
        viewModelScope.launch {
            userPreferencesManager.setUserName(name)
        }
    }

    fun updateGrade(grade: Int) {
        viewModelScope.launch {
            userPreferencesManager.setSelectedGrade(grade)
        }
    }

    fun updateAvatar(id: Int) {
        viewModelScope.launch {
            userPreferencesManager.setAvatarId(id)
        }
    }

    fun updateDailyGoals(questions: Int, minutes: Int) {
        viewModelScope.launch {
            userPreferencesManager.setDailyGoals(questions, minutes)
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.setDarkMode(enabled)
        }
    }

    fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            userPreferencesManager.setReminder(enabled, hour, minute)
        }
    }

    fun testReminderNotification(context: Context) {
        NotificationHelper.showStudyReminderNotification(
            context = context,
            title = "⏰ Giờ học tập THCS rồi bạn ơi!",
            message = "Hãy mở app luyện ngay 5 câu hỏi để duy trì chuỗi ${streakResult.value?.currentStreak ?: 1} ngày chăm chỉ nhé!"
        )
    }
}
