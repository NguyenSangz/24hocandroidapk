package com.example.data.repository

import android.annotation.SuppressLint
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesManager
import com.example.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Triển khai SettingsRepository sử dụng DataStore Preferences qua UserPreferencesManager
 * và AppDatabase cho các tác vụ quản trị dữ liệu học tập.
 */
class SettingsRepositoryImpl(
    private val preferencesManager: UserPreferencesManager,
    private val database: AppDatabase? = null
) : SettingsRepository {

    override val selectedGrade: Flow<Int> = preferencesManager.selectedGrade
    override val isGradeSelected: Flow<Boolean> = preferencesManager.isGradeSelected
    override val isFirstLaunch: Flow<Boolean> = preferencesManager.isFirstLaunch
    override val isDarkMode: Flow<Boolean> = preferencesManager.isDarkMode
    override val userName: Flow<String> = preferencesManager.userName
    override val themeMode: Flow<String> = preferencesManager.themeMode
    override val reduceMotion: Flow<Boolean> = preferencesManager.reduceMotion
    override val lessonFontSize: Flow<String> = preferencesManager.lessonFontSize
    override val defaultSeries: Flow<String> = preferencesManager.defaultSeries
    override val defaultQuestionsPerSession: Flow<Int> = preferencesManager.defaultQuestionsPerSession
    override val ttsSpeechRate: Flow<Float> = preferencesManager.ttsSpeechRate
    override val soundEffectsEnabled: Flow<Boolean> = preferencesManager.soundEffectsEnabled
    override val vibrationEnabled: Flow<Boolean> = preferencesManager.vibrationEnabled
    override val reminderEnabled: Flow<Boolean> = preferencesManager.reminderEnabled
    override val reminderHour: Flow<Int> = preferencesManager.reminderHour
    override val reminderMinute: Flow<Int> = preferencesManager.reminderMinute
    override val pushEnabled: Flow<Boolean> = preferencesManager.pushEnabled
    override val dailyGoalQuestions: Flow<Int> = preferencesManager.dailyGoalQuestions
    override val dailyGoalMinutes: Flow<Int> = preferencesManager.dailyGoalMinutes
    override val levelUpDialogEnabled: Flow<Boolean> = preferencesManager.levelUpDialogEnabled

    private suspend fun enqueueSettingsChange() {
        val now = System.currentTimeMillis()
        preferencesManager.touchSettingsUpdatedAt(now)
        if (database != null) {
            val snapshot = preferencesManager.exportSyncedSettingsSnapshot()
            com.example.data.sync.SyncOutboxHelper.enqueueSettingsSnapshot(database, snapshot, now)
        }
    }

    override suspend fun setGrade(grade: Int) {
        preferencesManager.setSelectedGrade(grade)
        enqueueSettingsChange()
    }

    override suspend fun resetGradeSelection() {
        preferencesManager.resetGradeSelection()
        enqueueSettingsChange()
    }

    override suspend fun setDarkMode(enabled: Boolean) {
        preferencesManager.setDarkMode(enabled)
        enqueueSettingsChange()
    }

    override suspend fun setThemeMode(mode: String) {
        preferencesManager.setThemeMode(mode)
        enqueueSettingsChange()
    }

    override suspend fun setReduceMotion(enabled: Boolean) {
        preferencesManager.setReduceMotion(enabled)
        enqueueSettingsChange()
    }

    override suspend fun setLessonFontSize(sizeKey: String) {
        preferencesManager.setLessonFontSize(sizeKey)
        enqueueSettingsChange()
    }

    override suspend fun setDefaultSeries(series: String) {
        preferencesManager.setDefaultSeries(series)
        enqueueSettingsChange()
    }

    override suspend fun setDefaultQuestionsPerSession(count: Int) {
        preferencesManager.setDefaultQuestionsPerSession(count)
        enqueueSettingsChange()
    }

    override suspend fun setTtsSpeechRate(rate: Float) {
        preferencesManager.setTtsSpeechRate(rate)
        enqueueSettingsChange()
    }

    override suspend fun setSoundEffects(enabled: Boolean) {
        preferencesManager.setSoundEffects(enabled)
        enqueueSettingsChange()
    }

    override suspend fun setVibration(enabled: Boolean) {
        preferencesManager.setVibration(enabled)
        enqueueSettingsChange()
    }

    override suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        preferencesManager.setReminder(enabled, hour, minute)
        enqueueSettingsChange()
    }

    override suspend fun setPushEnabled(enabled: Boolean) {
        // Cài đặt theo thiết bị: KHÔNG đưa vào snapshot đồng bộ của tài khoản.
        preferencesManager.setPushEnabled(enabled)
    }

    override suspend fun setDailyGoals(questions: Int, minutes: Int) {
        preferencesManager.setDailyGoals(questions, minutes)
        enqueueSettingsChange()
    }

    override suspend fun setLevelUpDialogEnabled(enabled: Boolean) {
        preferencesManager.setLevelUpDialogEnabled(enabled)
        enqueueSettingsChange()
    }

    override suspend fun setUserName(name: String) {
        preferencesManager.setUserName(name)
        enqueueSettingsChange()
    }

    override suspend fun setFirstLaunchCompleted() {
        preferencesManager.setFirstLaunchCompleted()
    }

    override suspend fun resetSettingsToDefault() {
        preferencesManager.resetSettingsToDefault()
        enqueueSettingsChange()
    }

    // refreshVersionsSync là Room internal API: cần thiết để đánh thức observers
    // sau khi xóa dữ liệu bằng raw SQL (bypass DAO invalidation)
    @SuppressLint("RestrictedApi")
    override suspend fun clearAllStudyData() {
        database?.let { db ->
            try {
                val sdb = db.openHelper.writableDatabase
                sdb.beginTransaction()
                try {
                    sdb.execSQL("DELETE FROM attempts")
                    sdb.execSQL("DELETE FROM tests")
                    sdb.execSQL("DELETE FROM test_answers")
                    sdb.execSQL("DELETE FROM study_sessions")
                    sdb.execSQL("DELETE FROM wrong_questions")
                    sdb.execSQL("DELETE FROM flashcard_progress")
                    sdb.execSQL("DELETE FROM review_logs")
                    sdb.execSQL("DELETE FROM xp_events")
                    sdb.execSQL("DELETE FROM badge_unlocks")
                    sdb.execSQL("DELETE FROM user_progress")
                    sdb.execSQL("DELETE FROM exam_results")
                    sdb.setTransactionSuccessful()
                } finally {
                    sdb.endTransaction()
                }
                try {
                    db.invalidationTracker.refreshVersionsSync()
                } catch (_: Exception) {
                    // Ignore if already closed or in-memory runner
                }
            } catch (_: Exception) {
                // Tiếp tục an toàn nếu có ngoại lệ DB
            }
        }
        preferencesManager.clearUserPreferencesData()
    }
}

