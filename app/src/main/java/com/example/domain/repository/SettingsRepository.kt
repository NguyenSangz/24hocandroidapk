package com.example.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Domain interface quản lý các cài đặt người dùng và trạng thái chọn lớp.
 */
interface SettingsRepository {
    /**
     * Luồng phát ra lớp học hiện tại (6, 7, 8, 9). Mặc định là 6.
     */
    val selectedGrade: Flow<Int>

    /**
     * Luồng cho biết người dùng đã hoàn thành việc chọn lớp hay chưa.
     */
    val isGradeSelected: Flow<Boolean>

    /**
     * Luồng cho biết có phải lần đầu mở app hay không.
     */
    val isFirstLaunch: Flow<Boolean>

    /**
     * Luồng cài đặt chế độ giao diện tối (Dark mode).
     */
    val isDarkMode: Flow<Boolean>

    /**
     * Luồng tên người học.
     */
    val userName: Flow<String>

    /**
     * Luồng chế độ giao diện ("system", "light", "dark"). Mặc định là "system".
     */
    val themeMode: Flow<String>

    /**
     * Luồng giảm hiệu ứng chuyển động (phục vụ khả năng tiếp cận / thiết bị yếu).
     */
    val reduceMotion: Flow<Boolean>

    /**
     * Luồng kích cỡ chữ bài học ("small", "medium", "large").
     */
    val lessonFontSize: Flow<String>

    /**
     * Luồng bộ sách mặc định ("kntt", "ctst", "cd", "chung").
     */
    val defaultSeries: Flow<String>

    /**
     * Luồng số câu hỏi mặc định mỗi phiên luyện tập (5 đến 30).
     */
    val defaultQuestionsPerSession: Flow<Int>

    /**
     * Luồng tốc độ phát âm tiếng Anh TTS (0.5x đến 2.0x, mặc định 1.0x).
     */
    val ttsSpeechRate: Flow<Float>

    /**
     * Luồng âm thanh hiệu ứng khi làm bài.
     */
    val soundEffectsEnabled: Flow<Boolean>

    /**
     * Luồng rung phản hồi.
     */
    val vibrationEnabled: Flow<Boolean>

    /**
     * Luồng nhắc học hằng ngày.
     */
    val reminderEnabled: Flow<Boolean>
    val reminderHour: Flow<Int>
    val reminderMinute: Flow<Int>

    /**
     * F20a – Công tắc tổng thông báo đẩy trên thiết bị này (mặc định tắt).
     * Đây là cài đặt theo thiết bị, KHÔNG đồng bộ lên tài khoản.
     */
    val pushEnabled: Flow<Boolean>

    /**
     * Luồng mục tiêu học tập ngày.
     */
    val dailyGoalQuestions: Flow<Int>
    val dailyGoalMinutes: Flow<Int>

    /**
     * Luồng thông báo chúc mừng lên cấp.
     */
    val levelUpDialogEnabled: Flow<Boolean>

    /**
     * Lưu khối lớp được chọn và đánh dấu đã chọn lớp.
     * Cho phép đổi lớp bất cứ lúc nào (bảo toàn 100% dữ liệu lớp cũ).
     */
    suspend fun setGrade(grade: Int)

    /**
     * Đặt lại trạng thái chọn lớp (dùng khi muốn học sinh chọn lại từ đầu hoặc kiểm thử).
     */
    suspend fun resetGradeSelection()

    /**
     * Bật / tắt chế độ tối.
     */
    suspend fun setDarkMode(enabled: Boolean)

    /**
     * Thiết lập chế độ giao diện ("system", "light", "dark").
     */
    suspend fun setThemeMode(mode: String)

    /**
     * Bật / tắt giảm chuyển động.
     */
    suspend fun setReduceMotion(enabled: Boolean)

    /**
     * Cập nhật cỡ chữ bài học ("small", "medium", "large").
     */
    suspend fun setLessonFontSize(sizeKey: String)

    /**
     * Đổi bộ sách mặc định (kntt, ctst, cd, chung).
     */
    suspend fun setDefaultSeries(series: String)

    /**
     * Thiết lập số câu hỏi mỗi phiên mặc định.
     */
    suspend fun setDefaultQuestionsPerSession(count: Int)

    /**
     * Thiết lập tốc độ đọc TTS.
     */
    suspend fun setTtsSpeechRate(rate: Float)

    /**
     * Bật / tắt âm thanh hiệu ứng.
     */
    suspend fun setSoundEffects(enabled: Boolean)

    /**
     * Bật / tắt rung phản hồi.
     */
    suspend fun setVibration(enabled: Boolean)

    /**
     * Cài đặt nhắc học hằng ngày.
     */
    suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int)

    /**
     * F20a – Bật/tắt thông báo đẩy trên thiết bị này.
     */
    suspend fun setPushEnabled(enabled: Boolean)

    /**
     * Cài đặt mục tiêu học tập hằng ngày.
     */
    suspend fun setDailyGoals(questions: Int, minutes: Int)

    /**
     * Bật / tắt thông báo lên cấp.
     */
    suspend fun setLevelUpDialogEnabled(enabled: Boolean)

    /**
     * Cập nhật tên người học.
     */
    suspend fun setUserName(name: String)

    /**
     * Đánh dấu đã hoàn thành màn hình khởi động lần đầu.
     */
    suspend fun setFirstLaunchCompleted()

    /**
     * Đặt lại toàn bộ cài đặt ứng dụng về mặc định (KHÔNG đụng tới tiến độ bài học và lịch sử học tập).
     */
    suspend fun resetSettingsToDefault()

    /**
     * Xóa toàn bộ dữ liệu học tập cá nhân (tiến độ bài, điểm kiểm tra, câu sai, XP, streak, flashcard cá nhân).
     */
    suspend fun clearAllStudyData()
}
