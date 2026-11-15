package com.example.domain.model

/**
 * Domain model đại diện cho 6 dạng câu hỏi theo chuẩn GDPT 2018 (F05).
 * Tách biệt hoàn toàn khỏi Room Entity. Dữ liệu đã được bóc tách có cấu trúc an toàn.
 */
sealed class Question {
    abstract val id: String
    abstract val lessonId: String
    abstract val content: String
    abstract val hint: String
    abstract val explanation: String
    abstract val difficulty: Difficulty

    /**
     * Dạng 1: Trắc nghiệm khách quan 4 lựa chọn (hoặc N lựa chọn)
     */
    data class MultipleChoice(
        override val id: String,
        override val lessonId: String,
        override val content: String,
        val options: List<String>,
        val correctAnswer: String,
        override val hint: String = "",
        override val explanation: String = "",
        override val difficulty: Difficulty = Difficulty.RECOGNITION
    ) : Question()

    /**
     * Dạng 2: Đúng / Sai
     */
    data class TrueFalse(
        override val id: String,
        override val lessonId: String,
        override val content: String,
        val correctAnswer: Boolean,
        override val hint: String = "",
        override val explanation: String = "",
        override val difficulty: Difficulty = Difficulty.RECOGNITION
    ) : Question()

    /**
     * Dạng 3: Điền khuyết (chấp nhận nhiều đáp án tương đương, phân biệt/không phân biệt dấu số thập phân)
     */
    data class FillBlank(
        override val id: String,
        override val lessonId: String,
        override val content: String,
        val acceptedAnswers: List<String>,
        override val hint: String = "",
        override val explanation: String = "",
        override val difficulty: Difficulty = Difficulty.COMPREHENSION
    ) : Question()

    /**
     * Dạng 4: Nối cặp (Matching: ghép vế A với vế B)
     */
    data class Matching(
        override val id: String,
        override val lessonId: String,
        override val content: String,
        val pairs: List<MatchingPair>,
        override val hint: String = "",
        override val explanation: String = "",
        override val difficulty: Difficulty = Difficulty.COMPREHENSION
    ) : Question()

    /**
     * Dạng 5: Sắp xếp theo thứ tự (Ordering: đưa các mục về đúng chuỗi logic/thời gian)
     */
    data class Ordering(
        override val id: String,
        override val lessonId: String,
        override val content: String,
        val correctOrder: List<String>,
        override val hint: String = "",
        override val explanation: String = "",
        override val difficulty: Difficulty = Difficulty.APPLICATION
    ) : Question()

    /**
     * Dạng 6: Tự luận ngắn (Short Answer: so khớp với các đáp án mẫu hoặc tự đối chiếu)
     */
    data class ShortAnswer(
        override val id: String,
        override val lessonId: String,
        override val content: String,
        val sampleAnswers: List<String>,
        val evaluationCriteria: String = "",
        override val hint: String = "",
        override val explanation: String = "",
        override val difficulty: Difficulty = Difficulty.APPLICATION
    ) : Question()
}

/**
 * Một cặp nối tương ứng (vế trái - vế phải)
 */
data class MatchingPair(
    val left: String,
    val right: String
)

/**
 * Câu trả lời của học sinh cho từng dạng câu hỏi tương ứng
 */
sealed class UserAnswer {
    data class MultipleChoice(val selectedOption: String) : UserAnswer()
    data class TrueFalse(val selectedValue: Boolean) : UserAnswer()
    data class FillBlank(val filledText: String) : UserAnswer()
    data class Matching(val userPairs: List<MatchingPair>) : UserAnswer()
    data class Ordering(val userOrder: List<String>) : UserAnswer()
    data class ShortAnswer(val submittedText: String) : UserAnswer()
}

/**
 * Đánh giá trạng thái chấm điểm của một câu trả lời
 */
enum class GradeStatus {
    CORRECT,        // Đúng hoàn toàn (1.0 điểm)
    PARTIALLY_CORRECT, // Đúng một phần (0.0 < điểm < 1.0)
    INCORRECT,      // Sai (0.0 điểm)
    SELF_EVALUATION // Cần tự đối chiếu với đáp án mẫu (Tự luận ngắn khi không so khớp tuyệt đối)
}

/**
 * Kết quả chấm điểm chi tiết trả về cho người học
 */
data class GradeResult(
    val status: GradeStatus,
    val score: Float, // Từ 0.0f đến 1.0f
    val isCorrect: Boolean,
    val correctDisplayAnswer: String,
    val explanation: String,
    val feedbackMessage: String = ""
)
