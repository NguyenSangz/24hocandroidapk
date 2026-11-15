package com.example.domain.model

/**
 * Khối lớp THCS theo chương trình Giáo dục phổ thông 2018
 */
enum class Grade(val value: Int, val displayName: String, val subtitle: String) {
    GRADE_6(6, "Lớp 6", "Khởi đầu cấp THCS - Tiếp cận phương pháp mới"),
    GRADE_7(7, "Lớp 7", "Mở rộng tư duy - Khám phá tự nhiên & xã hội"),
    GRADE_8(8, "Lớp 8", "Tăng tốc kiến thức - Chuẩn bị nền tảng"),
    GRADE_9(9, "Lớp 9", "Ôn luyện bứt phá - Chinh phục kỳ thi vào 10");

    companion object {
        fun fromValue(value: Int): Grade = entries.find { it.value == value } ?: GRADE_6
    }
}

/**
 * 6 dạng câu hỏi theo chuẩn đánh giá năng lực GDPT 2018
 */
enum class QuestionType(val code: String, val label: String) {
    MULTIPLE_CHOICE("MULTIPLE_CHOICE", "Trắc nghiệm 4 lựa chọn"),
    TRUE_FALSE("TRUE_FALSE", "Đúng / Sai"),
    FILL_BLANK("FILL_BLANK", "Điền khuyết"),
    MATCHING("MATCHING", "Nối cặp tương ứng"),
    SORTING("SORTING", "Sắp xếp theo thứ tự"),
    SHORT_ESSAY("SHORT_ESSAY", "Tự luận ngắn");

    companion object {
        fun fromCode(code: String): QuestionType =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: MULTIPLE_CHOICE
    }
}

/**
 * 15 môn học chính thức theo chương trình GDPT 2018 cấp THCS
 */
data class SubjectInfo(
    val id: String,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val description: String,
    val category: String // Tự nhiên, Xã hội, Năng khiếu, Trải nghiệm
)

object StandardCurriculumSubjects {
    val ALL_15_SUBJECTS = listOf(
        SubjectInfo("math", "Toán", "calculate", "#2563EB", "Đại số, Hình học và Xác suất thống kê", "Khoa học tự nhiên"),
        SubjectInfo("literature", "Ngữ văn", "menu_book", "#D97706", "Đọc hiểu văn bản, Tiếng Việt và Viết bài", "Khoa học xã hội"),
        SubjectInfo("english", "Tiếng Anh", "translate", "#059669", "Từ vựng, Ngữ pháp, Nghe - Nói - Đọc - Viết", "Ngoại ngữ"),
        SubjectInfo("physics", "Vật lý", "bolt", "#4F46E5", "Cơ, Nhiệt, Quang và Điện từ học", "Khoa học tự nhiên"),
        SubjectInfo("chemistry", "Hóa học", "science", "#7C3AED", "Chất, Nguyên tử, Phản ứng hóa học", "Khoa học tự nhiên"),
        SubjectInfo("biology", "Sinh học", "eco", "#16A34A", "Tế bào, Di truyền, Sinh thái và Cơ thể sống", "Khoa học tự nhiên"),
        SubjectInfo("history", "Lịch sử", "history_edu", "#DC2626", "Lịch sử Việt Nam & Thế giới qua các thời kỳ", "Khoa học xã hội"),
        SubjectInfo("geography", "Địa lý", "public", "#0D9488", "Địa lý tự nhiên, Dân cư và Kinh tế", "Khoa học xã hội"),
        SubjectInfo("civic", "GDCD", "favorite", "#E11D48", "Đạo đức, Kỹ năng sống và Pháp luật", "Khoa học xã hội"),
        SubjectInfo("informatics", "Tin học", "computer", "#0284C7", "Khoa học máy tính, Thuật toán và Ứng dụng số", "Công nghệ"),
        SubjectInfo("technology", "Công nghệ", "precision_manufacturing", "#EA580C", "Nông nghiệp, Cơ khí, Điện tử và Thiết kế", "Công nghệ"),
        SubjectInfo("pe", "Giáo dục thể chất", "fitness_center", "#10B981", "Rèn luyện thể lực, Vận động và Sức khỏe", "Thể chất & Nghệ thuật"),
        SubjectInfo("music", "Âm nhạc", "music_note", "#8B5CF6", "Nhạc lý, Hát, Nhạc cụ và Thưởng thức âm nhạc", "Thể chất & Nghệ thuật"),
        SubjectInfo("art", "Mỹ thuật", "palette", "#EC4899", "Hội họa, Điêu khắc, Thiết kế mỹ thuật", "Thể chất & Nghệ thuật"),
        SubjectInfo("experiential", "Hoạt động trải nghiệm", "explore", "#F59E0B", "Hướng nghiệp, Kỹ năng thực tế và Hoạt động xã hội", "Trải nghiệm")
    )
}

data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean,
    val progress: Float = 0f,
    val requirementText: String
)
