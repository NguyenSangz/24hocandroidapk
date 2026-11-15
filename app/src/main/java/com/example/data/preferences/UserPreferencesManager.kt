package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.util.DevicePerformanceUtils

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesManager(private val context: Context) : com.example.data.push.PushLocalStore {

    private object PreferencesKeys {
        val SELECTED_GRADE = intPreferencesKey("selected_grade")
        val USER_NAME = stringPreferencesKey("user_name")
        val AVATAR_ID = intPreferencesKey("avatar_id")
        val DAILY_GOAL_QUESTIONS = intPreferencesKey("daily_goal_questions")
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        val STREAK_DAYS = intPreferencesKey("streak_days")
        val LAST_ACTIVE_DATE = stringPreferencesKey("last_active_date")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val DEFAULT_QUESTIONS_PER_SESSION = intPreferencesKey("default_questions_per_session")
        val TTS_SPEECH_RATE = floatPreferencesKey("tts_speech_rate")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val HAS_SELECTED_GRADE = booleanPreferencesKey("has_selected_grade")
        val LESSON_FONT_SIZE = stringPreferencesKey("lesson_font_size")
        val SOUND_EFFECTS_ENABLED = booleanPreferencesKey("sound_effects_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val STREAK_FREEZE_COUNT = intPreferencesKey("streak_freeze_count")
        val LEVEL_UP_DIALOG_ENABLED = booleanPreferencesKey("level_up_dialog_enabled")
        val USED_FREEZE_DATES = stringPreferencesKey("used_freeze_dates")
        val SEARCH_HISTORY_QUERIES = stringPreferencesKey("search_history_queries")
        val SEARCH_HISTORY_ENABLED = booleanPreferencesKey("search_history_enabled")
        val DEFAULT_SERIES = stringPreferencesKey("default_series")
        val SUBJECT_SERIES_OVERRIDES = stringPreferencesKey("subject_series_overrides")
        val SETTINGS_UPDATED_AT = longPreferencesKey("settings_updated_at")
        val SYNC_WIFI_ONLY = booleanPreferencesKey("sync_wifi_only")
        val MASTERED_IRREGULAR_VERBS = stringPreferencesKey("mastered_irregular_verbs")

        // F20a – Thông báo đẩy: mặc định TẮT, không bao giờ đồng bộ token lên cài đặt học tập.
        val PUSH_ENABLED = booleanPreferencesKey("push_enabled")
        val PUSH_REGISTERED_TOKEN = stringPreferencesKey("push_registered_token")
        val PUSH_REGISTERED_AT = longPreferencesKey("push_registered_at")

        val LAST_STUDIED_LESSON_ID = stringPreferencesKey("last_studied_lesson_id")
    }

    val isSearchHistoryEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SEARCH_HISTORY_ENABLED] ?: true
    }

    val searchHistoryQueries: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.SEARCH_HISTORY_QUERIES] ?: ""
        if (raw.isBlank()) emptyList() else raw.split("\n").filter { it.isNotBlank() }
    }

    val defaultSeries: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_SERIES] ?: "kntt"
    }

    val subjectSeriesOverrides: Flow<Map<String, String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.SUBJECT_SERIES_OVERRIDES] ?: ""
        if (raw.isBlank()) emptyMap() else {
            raw.split(";").mapNotNull {
                val parts = it.split(":")
                if (parts.size == 2) parts[0] to parts[1] else null
            }.toMap()
        }
    }

    val streakFreezeCount: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.STREAK_FREEZE_COUNT] ?: 1
    }

    val levelUpDialogEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LEVEL_UP_DIALOG_ENABLED] ?: true
    }

    val usedFreezeDates: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.USED_FREEZE_DATES] ?: ""
        if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }

    val soundEffectsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SOUND_EFFECTS_ENABLED] ?: true
    }

    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.VIBRATION_ENABLED] ?: true
    }

    val lessonFontSize: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LESSON_FONT_SIZE] ?: "medium"
    }

    val isGradeSelected: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAS_SELECTED_GRADE] ?: false
    }

    val selectedGrade: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SELECTED_GRADE] ?: 6
    }

    val userName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_NAME] ?: "Học sinh THCS"
    }

    val avatarId: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AVATAR_ID] ?: 1
    }

    val dailyGoalQuestions: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DAILY_GOAL_QUESTIONS] ?: 15
    }

    val dailyGoalMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DAILY_GOAL_MINUTES] ?: 30
    }

    val streakDays: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.STREAK_DAYS] ?: 1
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_DARK_MODE] ?: false
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: if (preferences[PreferencesKeys.IS_DARK_MODE] == true) "dark" else "system"
    }

    val reduceMotion: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.REDUCE_MOTION] ?: DevicePerformanceUtils.isLowEndDevice(context)
    }

    val lastStudiedLessonId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_STUDIED_LESSON_ID]
    }

    suspend fun setLastStudiedLessonId(lessonId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_STUDIED_LESSON_ID] = lessonId
        }
    }

    val defaultQuestionsPerSession: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_QUESTIONS_PER_SESSION] ?: 10
    }

    val ttsSpeechRate: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TTS_SPEECH_RATE] ?: 1.0f
    }

    val reminderEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.REMINDER_ENABLED] ?: true
    }

    val reminderHour: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.REMINDER_HOUR] ?: 19
    }

    val reminderMinute: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.REMINDER_MINUTE] ?: 30
    }

    /** F20a – Công tắc tổng thông báo đẩy, mặc định TẮT. */
    override val pushEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PUSH_ENABLED] ?: false
    }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_FIRST_LAUNCH] ?: true
    }

    suspend fun setSelectedGrade(grade: Int) {
        context.dataStore.edit {
            it[PreferencesKeys.SELECTED_GRADE] = grade
            it[PreferencesKeys.HAS_SELECTED_GRADE] = true
            it[PreferencesKeys.IS_FIRST_LAUNCH] = false
        }
    }

    suspend fun resetGradeSelection() {
        context.dataStore.edit {
            it[PreferencesKeys.HAS_SELECTED_GRADE] = false
        }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[PreferencesKeys.USER_NAME] = name }
    }

    suspend fun setAvatarId(avatarId: Int) {
        context.dataStore.edit { it[PreferencesKeys.AVATAR_ID] = avatarId }
    }

    suspend fun setDailyGoals(questions: Int, minutes: Int) {
        context.dataStore.edit {
            it[PreferencesKeys.DAILY_GOAL_QUESTIONS] = questions
            it[PreferencesKeys.DAILY_GOAL_MINUTES] = minutes
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.IS_DARK_MODE] = enabled }
    }

    suspend fun setLessonFontSize(sizeKey: String) {
        context.dataStore.edit { it[PreferencesKeys.LESSON_FONT_SIZE] = sizeKey }
    }

    suspend fun setSoundEffects(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SOUND_EFFECTS_ENABLED] = enabled }
    }

    suspend fun setVibration(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[PreferencesKeys.REMINDER_ENABLED] = enabled
            it[PreferencesKeys.REMINDER_HOUR] = hour
            it[PreferencesKeys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { it[PreferencesKeys.IS_FIRST_LAUNCH] = false }
    }

    // ---- F20a: trạng thái token thông báo đẩy (chỉ cục bộ, không đồng bộ) ----

    suspend fun setPushEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.PUSH_ENABLED] = enabled
            if (!enabled) {
                // Tắt thông báo: quên token đã đăng ký để lần bật sau đăng ký lại từ đầu.
                prefs.remove(PreferencesKeys.PUSH_REGISTERED_TOKEN)
                prefs[PreferencesKeys.PUSH_REGISTERED_AT] = 0L
            }
        }
    }

    override suspend fun getRegisteredPushToken(): String? {
        val prefs = context.dataStore.data.first()
        return prefs[PreferencesKeys.PUSH_REGISTERED_TOKEN]?.takeIf { it.isNotBlank() }
    }

    override suspend fun getPushRegisteredAt(): Long {
        val prefs = context.dataStore.data.first()
        return prefs[PreferencesKeys.PUSH_REGISTERED_AT] ?: 0L
    }

    override suspend fun setRegisteredPushToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.PUSH_REGISTERED_TOKEN] = token
            prefs[PreferencesKeys.PUSH_REGISTERED_AT] = System.currentTimeMillis()
        }
    }

    override suspend fun clearRegisteredPushToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.PUSH_REGISTERED_TOKEN)
            prefs[PreferencesKeys.PUSH_REGISTERED_AT] = 0L
        }
    }

    suspend fun setStreakFreezeCount(count: Int) {
        context.dataStore.edit { it[PreferencesKeys.STREAK_FREEZE_COUNT] = count.coerceIn(0, 2) }
    }

    suspend fun setLevelUpDialogEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.LEVEL_UP_DIALOG_ENABLED] = enabled }
    }

    suspend fun recordUsedFreezeDate(dateStr: String) {
        context.dataStore.edit { prefs ->
            val existing = prefs[PreferencesKeys.USED_FREEZE_DATES] ?: ""
            val set = if (existing.isBlank()) mutableSetOf() else existing.split(",").toMutableSet()
            set.add(dateStr)
            prefs[PreferencesKeys.USED_FREEZE_DATES] = set.joinToString(",")
        }
    }

    suspend fun updateStreakOnActive() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        context.dataStore.edit { prefs ->
            val lastDate = prefs[PreferencesKeys.LAST_ACTIVE_DATE]
            val currentStreak = prefs[PreferencesKeys.STREAK_DAYS] ?: 1

            if (lastDate == null) {
                prefs[PreferencesKeys.LAST_ACTIVE_DATE] = today
                prefs[PreferencesKeys.STREAK_DAYS] = 1
            } else if (lastDate != today) {
                // Check if yesterday
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val lastDateTime = try { format.parse(lastDate)?.time ?: 0L } catch (_: Exception) { 0L }
                val todayTime = try { format.parse(today)?.time ?: 0L } catch (_: Exception) { 0L }
                val diffDays = (todayTime - lastDateTime) / (1000 * 60 * 60 * 24)

                if (diffDays == 1L) {
                    prefs[PreferencesKeys.STREAK_DAYS] = currentStreak + 1
                } else if (diffDays > 1L) {
                    prefs[PreferencesKeys.STREAK_DAYS] = 1
                }
                prefs[PreferencesKeys.LAST_ACTIVE_DATE] = today
            }
        }
    }

    suspend fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        context.dataStore.edit { prefs ->
            val enabled = prefs[PreferencesKeys.SEARCH_HISTORY_ENABLED] ?: true
            if (!enabled) return@edit
            val raw = prefs[PreferencesKeys.SEARCH_HISTORY_QUERIES] ?: ""
            val list = if (raw.isBlank()) mutableListOf() else raw.split("\n").filter { it.isNotBlank() }.toMutableList()
            list.remove(trimmed)
            list.add(0, trimmed)
            val trimmedList = list.take(10)
            prefs[PreferencesKeys.SEARCH_HISTORY_QUERIES] = trimmedList.joinToString("\n")
        }
    }

    suspend fun deleteSearchQuery(query: String) {
        val trimmed = query.trim()
        context.dataStore.edit { prefs ->
            val raw = prefs[PreferencesKeys.SEARCH_HISTORY_QUERIES] ?: ""
            val list = if (raw.isBlank()) mutableListOf() else raw.split("\n").filter { it.isNotBlank() }.toMutableList()
            list.remove(trimmed)
            prefs[PreferencesKeys.SEARCH_HISTORY_QUERIES] = list.joinToString("\n")
        }
    }

    suspend fun clearSearchHistory() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SEARCH_HISTORY_QUERIES] = ""
        }
    }

    suspend fun setSearchHistoryEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SEARCH_HISTORY_ENABLED] = enabled
            if (!enabled) {
                prefs[PreferencesKeys.SEARCH_HISTORY_QUERIES] = ""
            }
        }
    }

    suspend fun setDefaultSeries(series: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DEFAULT_SERIES] = series
        }
    }

    suspend fun setSubjectSeriesOverride(subjectId: String, series: String) {
        context.dataStore.edit { prefs ->
            val raw = prefs[PreferencesKeys.SUBJECT_SERIES_OVERRIDES] ?: ""
            val map = if (raw.isBlank()) mutableMapOf() else {
                raw.split(";").mapNotNull {
                    val parts = it.split(":")
                    if (parts.size == 2) parts[0] to parts[1] else null
                }.toMap().toMutableMap()
            }
            map[subjectId] = series
            prefs[PreferencesKeys.SUBJECT_SERIES_OVERRIDES] = map.entries.joinToString(";") { "${it.key}:${it.value}" }
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_MODE] = mode
            prefs[PreferencesKeys.IS_DARK_MODE] = (mode == "dark")
        }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.REDUCE_MOTION] = enabled
        }
    }

    suspend fun setDefaultQuestionsPerSession(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DEFAULT_QUESTIONS_PER_SESSION] = count.coerceIn(5, 50)
        }
    }

    suspend fun setTtsSpeechRate(rate: Float) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.TTS_SPEECH_RATE] = rate.coerceIn(0.5f, 2.0f)
        }
    }

    suspend fun resetSettingsToDefault() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_MODE] = "system"
            prefs[PreferencesKeys.IS_DARK_MODE] = false
            prefs[PreferencesKeys.REDUCE_MOTION] = false
            prefs[PreferencesKeys.LESSON_FONT_SIZE] = "medium"
            prefs[PreferencesKeys.DEFAULT_SERIES] = "kntt"
            prefs[PreferencesKeys.DEFAULT_QUESTIONS_PER_SESSION] = 10
            prefs[PreferencesKeys.TTS_SPEECH_RATE] = 1.0f
            prefs[PreferencesKeys.SOUND_EFFECTS_ENABLED] = true
            prefs[PreferencesKeys.VIBRATION_ENABLED] = true
            prefs[PreferencesKeys.REMINDER_ENABLED] = true
            prefs[PreferencesKeys.REMINDER_HOUR] = 19
            prefs[PreferencesKeys.REMINDER_MINUTE] = 30
            prefs[PreferencesKeys.LEVEL_UP_DIALOG_ENABLED] = true
        }
    }

    val syncWifiOnly: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SYNC_WIFI_ONLY] ?: false
    }

    val masteredIrregularVerbs: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.MASTERED_IRREGULAR_VERBS] ?: ""
        if (raw.isBlank()) emptySet() else raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    suspend fun setSyncWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SYNC_WIFI_ONLY] = wifiOnly
            prefs[PreferencesKeys.SETTINGS_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun setMasteredIrregularVerbs(verbs: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.MASTERED_IRREGULAR_VERBS] = verbs.sorted().joinToString(",")
        }
    }

    suspend fun touchSettingsUpdatedAt(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SETTINGS_UPDATED_AT] = timestamp
        }
    }

    suspend fun exportSyncedSettingsSnapshot(): Map<String, Any?> {
        val prefs = context.dataStore.data.first()
        val seriesOverridesRaw = prefs[PreferencesKeys.SUBJECT_SERIES_OVERRIDES] ?: ""
        return mapOf(
            "selectedGrade" to (prefs[PreferencesKeys.SELECTED_GRADE] ?: 6),
            "userName" to (prefs[PreferencesKeys.USER_NAME] ?: "Học sinh THCS"),
            "avatarId" to (prefs[PreferencesKeys.AVATAR_ID] ?: 1),
            "dailyGoalQuestions" to (prefs[PreferencesKeys.DAILY_GOAL_QUESTIONS] ?: 15),
            "dailyGoalMinutes" to (prefs[PreferencesKeys.DAILY_GOAL_MINUTES] ?: 30),
            "themeMode" to (prefs[PreferencesKeys.THEME_MODE] ?: if (prefs[PreferencesKeys.IS_DARK_MODE] == true) "dark" else "system"),
            "reduceMotion" to (prefs[PreferencesKeys.REDUCE_MOTION] ?: false),
            "lessonFontSize" to (prefs[PreferencesKeys.LESSON_FONT_SIZE] ?: "medium"),
            "defaultSeries" to (prefs[PreferencesKeys.DEFAULT_SERIES] ?: "kntt"),
            "subjectSeriesOverrides" to seriesOverridesRaw,
            "defaultQuestionsPerSession" to (prefs[PreferencesKeys.DEFAULT_QUESTIONS_PER_SESSION] ?: 10),
            "ttsSpeechRate" to (prefs[PreferencesKeys.TTS_SPEECH_RATE] ?: 1.0f).toDouble(),
            "soundEffectsEnabled" to (prefs[PreferencesKeys.SOUND_EFFECTS_ENABLED] ?: true),
            "vibrationEnabled" to (prefs[PreferencesKeys.VIBRATION_ENABLED] ?: true),
            "reminderEnabled" to (prefs[PreferencesKeys.REMINDER_ENABLED] ?: true),
            "reminderHour" to (prefs[PreferencesKeys.REMINDER_HOUR] ?: 19),
            "reminderMinute" to (prefs[PreferencesKeys.REMINDER_MINUTE] ?: 30),
            "levelUpDialogEnabled" to (prefs[PreferencesKeys.LEVEL_UP_DIALOG_ENABLED] ?: true),
            "streakFreezeCount" to (prefs[PreferencesKeys.STREAK_FREEZE_COUNT] ?: 1),
            "usedFreezeDates" to (prefs[PreferencesKeys.USED_FREEZE_DATES] ?: ""),
            "syncWifiOnly" to (prefs[PreferencesKeys.SYNC_WIFI_ONLY] ?: false),
            "updatedAt" to (prefs[PreferencesKeys.SETTINGS_UPDATED_AT] ?: 0L)
        )
    }

    suspend fun getSettingsUpdatedAtSync(): Long {
        val prefs = context.dataStore.data.first()
        return prefs[PreferencesKeys.SETTINGS_UPDATED_AT] ?: 0L
    }

    suspend fun applySyncedSettingsSnapshot(payload: Map<String, Any?>, serverClientUpdatedAt: Long) {
        context.dataStore.edit { prefs ->
            (payload["selectedGrade"] as? Number)?.toInt()?.takeIf { it in 6..9 }?.let {
                prefs[PreferencesKeys.SELECTED_GRADE] = it
                prefs[PreferencesKeys.HAS_SELECTED_GRADE] = true
            }
            (payload["userName"] as? String)?.takeIf { it.isNotBlank() }?.let {
                prefs[PreferencesKeys.USER_NAME] = it
            }
            (payload["avatarId"] as? Number)?.toInt()?.let {
                prefs[PreferencesKeys.AVATAR_ID] = it
            }
            (payload["dailyGoalQuestions"] as? Number)?.toInt()?.let {
                prefs[PreferencesKeys.DAILY_GOAL_QUESTIONS] = it.coerceIn(5, 100)
            }
            (payload["dailyGoalMinutes"] as? Number)?.toInt()?.let {
                prefs[PreferencesKeys.DAILY_GOAL_MINUTES] = it.coerceIn(5, 240)
            }
            (payload["themeMode"] as? String)?.takeIf { it in listOf("system", "light", "dark") }?.let {
                prefs[PreferencesKeys.THEME_MODE] = it
                prefs[PreferencesKeys.IS_DARK_MODE] = (it == "dark")
            }
            (payload["reduceMotion"] as? Boolean)?.let { prefs[PreferencesKeys.REDUCE_MOTION] = it }
            (payload["lessonFontSize"] as? String)?.takeIf { it.isNotBlank() }?.let {
                prefs[PreferencesKeys.LESSON_FONT_SIZE] = it
            }
            (payload["defaultSeries"] as? String)?.takeIf { it.isNotBlank() }?.let {
                prefs[PreferencesKeys.DEFAULT_SERIES] = it
            }
            (payload["subjectSeriesOverrides"] as? String)?.let {
                prefs[PreferencesKeys.SUBJECT_SERIES_OVERRIDES] = it
            }
            (payload["defaultQuestionsPerSession"] as? Number)?.toInt()?.let {
                prefs[PreferencesKeys.DEFAULT_QUESTIONS_PER_SESSION] = it.coerceIn(5, 50)
            }
            (payload["ttsSpeechRate"] as? Number)?.toFloat()?.let {
                prefs[PreferencesKeys.TTS_SPEECH_RATE] = it.coerceIn(0.5f, 2.0f)
            }
            (payload["soundEffectsEnabled"] as? Boolean)?.let { prefs[PreferencesKeys.SOUND_EFFECTS_ENABLED] = it }
            (payload["vibrationEnabled"] as? Boolean)?.let { prefs[PreferencesKeys.VIBRATION_ENABLED] = it }
            (payload["reminderEnabled"] as? Boolean)?.let { prefs[PreferencesKeys.REMINDER_ENABLED] = it }
            (payload["reminderHour"] as? Number)?.toInt()?.let { prefs[PreferencesKeys.REMINDER_HOUR] = it.coerceIn(0, 23) }
            (payload["reminderMinute"] as? Number)?.toInt()?.let { prefs[PreferencesKeys.REMINDER_MINUTE] = it.coerceIn(0, 59) }
            (payload["levelUpDialogEnabled"] as? Boolean)?.let { prefs[PreferencesKeys.LEVEL_UP_DIALOG_ENABLED] = it }
            (payload["streakFreezeCount"] as? Number)?.toInt()?.let { prefs[PreferencesKeys.STREAK_FREEZE_COUNT] = it.coerceIn(0, 2) }
            (payload["usedFreezeDates"] as? String)?.let { incoming ->
                val localSet = (prefs[PreferencesKeys.USED_FREEZE_DATES] ?: "").split(",").filter { it.isNotBlank() }.toSet()
                val remoteSet = incoming.split(",").filter { it.isNotBlank() }.toSet()
                prefs[PreferencesKeys.USED_FREEZE_DATES] = (localSet + remoteSet).sorted().joinToString(",")
            }
            (payload["syncWifiOnly"] as? Boolean)?.let { prefs[PreferencesKeys.SYNC_WIFI_ONLY] = it }
            prefs[PreferencesKeys.SETTINGS_UPDATED_AT] = serverClientUpdatedAt
        }
    }

    suspend fun clearUserPreferencesData() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.STREAK_DAYS] = 1
            prefs[PreferencesKeys.LAST_ACTIVE_DATE] = ""
            prefs[PreferencesKeys.STREAK_FREEZE_COUNT] = 1
            prefs[PreferencesKeys.USED_FREEZE_DATES] = ""
            prefs[PreferencesKeys.DAILY_GOAL_QUESTIONS] = 15
            prefs[PreferencesKeys.DAILY_GOAL_MINUTES] = 30
            prefs[PreferencesKeys.MASTERED_IRREGULAR_VERBS] = ""
            prefs[PreferencesKeys.SETTINGS_UPDATED_AT] = System.currentTimeMillis()
        }
    }
}
