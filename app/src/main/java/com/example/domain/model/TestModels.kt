package com.example.domain.model

/**
 * Loại đề kiểm tra theo chuẩn chương trình THCS GDPT 2018 (F07).
 */
enum class TestType(
    val title: String,
    val defaultQuestionCount: Int,
    val defaultDurationMinutes: Int,
    val description: String
) {
    LESSON(
        title = "Kiểm tra theo bài",
        defaultQuestionCount = 10,
        defaultDurationMinutes = 15,
        description = "Đánh giá mức độ nắm vững kiến thức trọng tâm của một bài học cụ thể."
    ),
    CHAPTER(
        title = "Kiểm tra theo chương",
        defaultQuestionCount = 20,
        defaultDurationMinutes = 30,
        description = "Hệ thống hóa toàn bộ kiến thức và kỹ năng trong phạm vi một chương."
    ),
    MID_TERM(
        title = "Kiểm tra giữa kỳ",
        defaultQuestionCount = 20,
        defaultDurationMinutes = 45,
        description = "Đề thi tổng hợp kiến thức nửa đầu học kỳ theo ma trận đề chuẩn."
    ),
    FINAL(
        title = "Kiểm tra cuối kỳ",
        defaultQuestionCount = 30,
        defaultDurationMinutes = 60,
        description = "Đề thi toàn diện bao quát tất cả các chương trong cả học kỳ."
    );

    companion object {
        fun fromString(value: String): TestType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: LESSON
        }
    }
}

/**
 * Trạng thái bài kiểm tra
 */
enum class TestStatus {
    IN_PROGRESS,
    COMPLETED,
    ABANDONED
}

/**
 * Xếp loại học lực bài kiểm tra theo thang điểm 10 (Thông tư 22/2021/BGDĐT)
 */
enum class AcademicRank(val title: String, val colorHex: Long) {
    EXCELLENT("Giỏi", 0xFF3F9E6E),       // Điểm >= 8.0
    GOOD("Khá", 0xFF4A7FA8),             // Điểm 6.5 - 7.9
    AVERAGE("Trung bình", 0xFFC08A52),    // Điểm 5.0 - 6.4
    NEEDS_EFFORT("Cần cố gắng", 0xFFC26161) // Điểm < 5.0
}

/**
 * Cảnh báo thời gian làm bài
 */
enum class TimerWarning {
    FIVE_MINUTES_LEFT,
    ONE_MINUTE_LEFT
}

/**
 * Phân tích điểm theo từng chương/chủ đề trong bài kiểm tra
 */
data class ChapterScoreAnalysis(
    val chapterId: String,
    val chapterTitle: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val scoreEarned: Float,
    val maxScore: Float
) {
    val percentage: Float
        get() = if (maxScore > 0f) (scoreEarned / maxScore) * 100f else 0f

    val isStrongPoint: Boolean
        get() = percentage >= 70f
}

/**
 * Tổng hợp điểm số và xếp loại bài kiểm tra
 */
data class TestScoreSummary(
    val finalScore: Float, // Thang điểm 10, làm tròn 1 chữ số thập phân
    val maxScore: Float = 10.0f,
    val academicRank: AcademicRank,
    val totalQuestions: Int,
    val correctCount: Int,
    val partialCount: Int,
    val wrongCount: Int,
    val unansweredCount: Int,
    val chapterAnalysis: List<ChapterScoreAnalysis>,
    val praiseMessage: String
)

/**
 * Kết quả tạo đề thi từ TestBuilder
 */
sealed class TestBuildResult {
    data class Success(val questions: List<Question>) : TestBuildResult()
    data class InsufficientQuestions(
        val availableCount: Int,
        val requestedCount: Int,
        val suggestedCount: Int,
        val message: String
    ) : TestBuildResult()
    data class Error(val message: String) : TestBuildResult()
}

/**
 * Chi tiết từng câu hỏi trong bài kiểm tra (phục vụ làm bài và xem lại)
 */
data class TestAnswerDetail(
    val question: Question,
    val userAnswer: UserAnswer?,
    val isFlagged: Boolean = false,
    val isAnswered: Boolean = false,
    val gradeResult: GradeResult? = null,
    val isSelfEvaluated: Boolean = false,
    val chapterTitle: String = ""
)
