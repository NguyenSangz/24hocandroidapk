package com.example.fake

import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSettingsRepository(
    initialGrade: Int = 6,
    initialIsGradeSelected: Boolean = false,
    initialIsFirstLaunch: Boolean = true
) : SettingsRepository {

    private val _selectedGrade = MutableStateFlow(initialGrade)
    override val selectedGrade: Flow<Int> = _selectedGrade.asStateFlow()

    private val _isGradeSelected = MutableStateFlow(initialIsGradeSelected)
    override val isGradeSelected: Flow<Boolean> = _isGradeSelected.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow(initialIsFirstLaunch)
    override val isFirstLaunch: Flow<Boolean> = _isFirstLaunch.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    override val isDarkMode: Flow<Boolean> = _isDarkMode.asStateFlow()

    private val _userName = MutableStateFlow("Học sinh THCS")
    override val userName: Flow<String> = _userName.asStateFlow()

    private val _themeMode = MutableStateFlow("system")
    override val themeMode: Flow<String> = _themeMode.asStateFlow()

    private val _reduceMotion = MutableStateFlow(false)
    override val reduceMotion: Flow<Boolean> = _reduceMotion.asStateFlow()

    private val _lessonFontSize = MutableStateFlow("medium")
    override val lessonFontSize: Flow<String> = _lessonFontSize.asStateFlow()

    private val _defaultSeries = MutableStateFlow("kntt")
    override val defaultSeries: Flow<String> = _defaultSeries.asStateFlow()

    private val _defaultQuestionsPerSession = MutableStateFlow(10)
    override val defaultQuestionsPerSession: Flow<Int> = _defaultQuestionsPerSession.asStateFlow()

    private val _ttsSpeechRate = MutableStateFlow(1.0f)
    override val ttsSpeechRate: Flow<Float> = _ttsSpeechRate.asStateFlow()

    private val _soundEffectsEnabled = MutableStateFlow(true)
    override val soundEffectsEnabled: Flow<Boolean> = _soundEffectsEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(true)
    override val vibrationEnabled: Flow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _reminderEnabled = MutableStateFlow(true)
    override val reminderEnabled: Flow<Boolean> = _reminderEnabled.asStateFlow()

    private val _reminderHour = MutableStateFlow(19)
    override val reminderHour: Flow<Int> = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(30)
    override val reminderMinute: Flow<Int> = _reminderMinute.asStateFlow()

    private val _pushEnabled = MutableStateFlow(false)
    override val pushEnabled: Flow<Boolean> = _pushEnabled.asStateFlow()

    private val _dailyGoalQuestions = MutableStateFlow(15)
    override val dailyGoalQuestions: Flow<Int> = _dailyGoalQuestions.asStateFlow()

    private val _dailyGoalMinutes = MutableStateFlow(30)
    override val dailyGoalMinutes: Flow<Int> = _dailyGoalMinutes.asStateFlow()

    private val _levelUpDialogEnabled = MutableStateFlow(true)
    override val levelUpDialogEnabled: Flow<Boolean> = _levelUpDialogEnabled.asStateFlow()

    override suspend fun setGrade(grade: Int) {
        _selectedGrade.value = grade
        _isGradeSelected.value = true
        _isFirstLaunch.value = false
    }

    override suspend fun resetGradeSelection() {
        _isGradeSelected.value = false
    }

    override suspend fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        _themeMode.value = if (enabled) "dark" else "light"
    }

    override suspend fun setThemeMode(mode: String) {
        _themeMode.value = mode
        _isDarkMode.value = (mode == "dark")
    }

    override suspend fun setReduceMotion(enabled: Boolean) {
        _reduceMotion.value = enabled
    }

    override suspend fun setLessonFontSize(sizeKey: String) {
        _lessonFontSize.value = sizeKey
    }

    override suspend fun setDefaultSeries(series: String) {
        _defaultSeries.value = series
    }

    override suspend fun setDefaultQuestionsPerSession(count: Int) {
        _defaultQuestionsPerSession.value = count
    }

    override suspend fun setTtsSpeechRate(rate: Float) {
        _ttsSpeechRate.value = rate
    }

    override suspend fun setSoundEffects(enabled: Boolean) {
        _soundEffectsEnabled.value = enabled
    }

    override suspend fun setVibration(enabled: Boolean) {
        _vibrationEnabled.value = enabled
    }

    override suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        _reminderEnabled.value = enabled
        _reminderHour.value = hour
        _reminderMinute.value = minute
    }

    override suspend fun setPushEnabled(enabled: Boolean) {
        _pushEnabled.value = enabled
    }

    override suspend fun setDailyGoals(questions: Int, minutes: Int) {
        _dailyGoalQuestions.value = questions
        _dailyGoalMinutes.value = minutes
    }

    override suspend fun setLevelUpDialogEnabled(enabled: Boolean) {
        _levelUpDialogEnabled.value = enabled
    }

    override suspend fun setUserName(name: String) {
        _userName.value = name
    }

    override suspend fun setFirstLaunchCompleted() {
        _isFirstLaunch.value = false
    }

    override suspend fun resetSettingsToDefault() {
        _themeMode.value = "system"
        _isDarkMode.value = false
        _reduceMotion.value = false
        _lessonFontSize.value = "medium"
        _defaultSeries.value = "kntt"
        _defaultQuestionsPerSession.value = 10
        _ttsSpeechRate.value = 1.0f
        _soundEffectsEnabled.value = true
        _vibrationEnabled.value = true
        _reminderEnabled.value = true
        _reminderHour.value = 19
        _reminderMinute.value = 30
        _levelUpDialogEnabled.value = true
    }

    var wasClearAllStudyDataCalled = false
    override suspend fun clearAllStudyData() {
        wasClearAllStudyDataCalled = true
    }
}
