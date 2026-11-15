package com.example.domain.evaluator

import com.example.domain.model.GradeResult
import com.example.domain.model.GradeStatus
import com.example.domain.model.MatchingPair
import com.example.domain.model.Question
import com.example.domain.model.UserAnswer
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Thuật toán và Logic chấm điểm thuần (Pure functions) cho 6 dạng câu hỏi (F05).
 * Dễ dàng viết Unit Test, không phụ thuộc Android Framework / Context.
 */
object QuestionGrader {

    /**
     * Chấm điểm một câu hỏi dựa trên câu trả lời của người dùng.
     */
    fun grade(question: Question, answer: UserAnswer): GradeResult {
        return when (question) {
            is Question.MultipleChoice -> gradeMultipleChoice(question, answer)
            is Question.TrueFalse -> gradeTrueFalse(question, answer)
            is Question.FillBlank -> gradeFillBlank(question, answer)
            is Question.Matching -> gradeMatching(question, answer)
            is Question.Ordering -> gradeOrdering(question, answer)
            is Question.ShortAnswer -> gradeShortAnswer(question, answer)
        }
    }

    /**
     * 1. Trắc nghiệm: so khớp đáp án (không phân biệt hoa/thường, chuẩn hóa khoảng trắng)
     */
    fun gradeMultipleChoice(
        question: Question.MultipleChoice,
        answer: UserAnswer
    ): GradeResult {
        val userAnswerText = when (answer) {
            is UserAnswer.MultipleChoice -> answer.selectedOption
            is UserAnswer.FillBlank -> answer.filledText
            else -> ""
        }

        val isMatch = AnswerNormalizer.areEquivalent(userAnswerText, question.correctAnswer)
        val score = if (isMatch) 1.0f else 0.0f
        val status = if (isMatch) GradeStatus.CORRECT else GradeStatus.INCORRECT

        return GradeResult(
            status = status,
            score = score,
            isCorrect = isMatch,
            correctDisplayAnswer = question.correctAnswer,
            explanation = question.explanation,
            feedbackMessage = if (isMatch) "Chính xác!" else "Chưa chính xác. Đáp án đúng là: ${question.correctAnswer}"
        )
    }

    /**
     * 2. Đúng / Sai: so khớp Boolean (hoặc chuỗi biểu diễn Đúng/Sai)
     */
    fun gradeTrueFalse(
        question: Question.TrueFalse,
        answer: UserAnswer
    ): GradeResult {
        val userVal: Boolean? = when (answer) {
            is UserAnswer.TrueFalse -> answer.selectedValue
            is UserAnswer.FillBlank -> parseBooleanText(answer.filledText)
            is UserAnswer.MultipleChoice -> parseBooleanText(answer.selectedOption)
            else -> null
        }

        val isMatch = (userVal != null && userVal == question.correctAnswer)
        val score = if (isMatch) 1.0f else 0.0f
        val status = if (isMatch) GradeStatus.CORRECT else GradeStatus.INCORRECT
        val display = if (question.correctAnswer) "Đúng" else "Sai"

        return GradeResult(
            status = status,
            score = score,
            isCorrect = isMatch,
            correctDisplayAnswer = display,
            explanation = question.explanation,
            feedbackMessage = if (isMatch) "Chính xác!" else "Chưa chính xác. Nhận định này là $display."
        )
    }

    /**
     * 3. Điền khuyết: Chuẩn hóa tiếng Việt, chấp nhận nhiều đáp án tương đương,
     * số thập phân chấp nhận cả dấu phẩy và chấm.
     */
    fun gradeFillBlank(
        question: Question.FillBlank,
        answer: UserAnswer
    ): GradeResult {
        val userText = when (answer) {
            is UserAnswer.FillBlank -> answer.filledText
            is UserAnswer.MultipleChoice -> answer.selectedOption
            is UserAnswer.ShortAnswer -> answer.submittedText
            else -> ""
        }

        val isMatch = question.acceptedAnswers.any { accepted ->
            AnswerNormalizer.areEquivalent(userText, accepted)
        }

        val score = if (isMatch) 1.0f else 0.0f
        val status = if (isMatch) GradeStatus.CORRECT else GradeStatus.INCORRECT
        val display = question.acceptedAnswers.joinToString(" hoặc ")

        return GradeResult(
            status = status,
            score = score,
            isCorrect = isMatch,
            correctDisplayAnswer = display,
            explanation = question.explanation,
            feedbackMessage = if (isMatch) "Tuyệt vời, câu trả lời chính xác!" else "Đáp án đúng là: $display"
        )
    }

    /**
     * 4. Nối cặp: Chấm theo tỉ lệ số cặp ghép đúng.
     * Đúng một phần được tính điểm lẻ tương ứng.
     */
    fun gradeMatching(
        question: Question.Matching,
        answer: UserAnswer
    ): GradeResult {
        val userPairs = when (answer) {
            is UserAnswer.Matching -> answer.userPairs
            else -> emptyList()
        }

        val totalPairs = question.pairs.size
        if (totalPairs == 0) {
            return GradeResult(
                status = GradeStatus.CORRECT,
                score = 1.0f,
                isCorrect = true,
                correctDisplayAnswer = "",
                explanation = question.explanation
            )
        }

        // Tạo bản đồ chuẩn hóa của các cặp đúng
        // map: norm(left) -> norm(right)
        val expectedMap = question.pairs.associate { pair ->
            AnswerNormalizer.normalize(pair.left) to AnswerNormalizer.normalize(pair.right)
        }

        var correctCount = 0
        userPairs.forEach { up ->
            val normLeft = AnswerNormalizer.normalize(up.left)
            val normRight = AnswerNormalizer.normalize(up.right)
            val expectedRight = expectedMap[normLeft]
            if (expectedRight != null && expectedRight == normRight) {
                correctCount++
            }
        }

        val rawScore = correctCount.toFloat() / totalPairs.toFloat()
        // Làm tròn đến 2 chữ số thập phân
        val roundedScore = BigDecimal(rawScore.toDouble())
            .setScale(2, RoundingMode.HALF_UP)
            .toFloat()

        val status = when {
            correctCount == totalPairs -> GradeStatus.CORRECT
            correctCount > 0 -> GradeStatus.PARTIALLY_CORRECT
            else -> GradeStatus.INCORRECT
        }

        val display = question.pairs.joinToString("; ") { "${it.left} ➔ ${it.right}" }
        val msg = when (status) {
            GradeStatus.CORRECT -> "Ghép nối chính xác tất cả các cặp!"
            GradeStatus.PARTIALLY_CORRECT -> "Bạn ghép đúng $correctCount/$totalPairs cặp (+$roundedScore điểm)."
            GradeStatus.INCORRECT -> "Chưa chính xác. Xem lại các cặp nối chuẩn bên dưới."
            else -> ""
        }

        return GradeResult(
            status = status,
            score = roundedScore,
            isCorrect = (correctCount == totalPairs),
            correctDisplayAnswer = display,
            explanation = question.explanation,
            feedbackMessage = msg
        )
    }

    /**
     * 5. Sắp xếp: So khớp thứ tự danh sách.
     * Tính điểm theo số vị trí được xếp đúng thứ tự tương ứng.
     */
    fun gradeOrdering(
        question: Question.Ordering,
        answer: UserAnswer
    ): GradeResult {
        val userOrder = when (answer) {
            is UserAnswer.Ordering -> answer.userOrder
            else -> emptyList()
        }

        val expected = question.correctOrder
        val totalItems = expected.size
        if (totalItems == 0) {
            return GradeResult(
                status = GradeStatus.CORRECT,
                score = 1.0f,
                isCorrect = true,
                correctDisplayAnswer = "",
                explanation = question.explanation
            )
        }

        var matchPositionCount = 0
        for (i in 0 until minOf(expected.size, userOrder.size)) {
            if (AnswerNormalizer.areEquivalent(userOrder[i], expected[i])) {
                matchPositionCount++
            }
        }

        val rawScore = matchPositionCount.toFloat() / totalItems.toFloat()
        val roundedScore = BigDecimal(rawScore.toDouble())
            .setScale(2, RoundingMode.HALF_UP)
            .toFloat()

        val status = when {
            matchPositionCount == totalItems && userOrder.size == totalItems -> GradeStatus.CORRECT
            matchPositionCount > 0 -> GradeStatus.PARTIALLY_CORRECT
            else -> GradeStatus.INCORRECT
        }

        val display = expected.joinToString(" ➔ ")
        val msg = when (status) {
            GradeStatus.CORRECT -> "Sắp xếp hoàn toàn chính xác!"
            GradeStatus.PARTIALLY_CORRECT -> "Đúng vị trí $matchPositionCount/$totalItems mục (+$roundedScore điểm)."
            GradeStatus.INCORRECT -> "Thứ tự sắp xếp chưa đúng."
            else -> ""
        }

        return GradeResult(
            status = status,
            score = roundedScore,
            isCorrect = (status == GradeStatus.CORRECT),
            correctDisplayAnswer = display,
            explanation = question.explanation,
            feedbackMessage = msg
        )
    }

    /**
     * 6. Tự luận ngắn: So khớp với danh sách đáp án mẫu chấp nhận được.
     * Nếu khớp: CORRECT (1.0 điểm).
     * Nếu không khớp: trả về trạng thái SELF_EVALUATION (cần tự đối chiếu)
     * với đáp án mẫu và hướng dẫn chấm để học sinh tự đánh giá.
     */
    fun gradeShortAnswer(
        question: Question.ShortAnswer,
        answer: UserAnswer
    ): GradeResult {
        val userText = when (answer) {
            is UserAnswer.ShortAnswer -> answer.submittedText
            is UserAnswer.FillBlank -> answer.filledText
            else -> ""
        }

        if (userText.isBlank()) {
            return GradeResult(
                status = GradeStatus.INCORRECT,
                score = 0.0f,
                isCorrect = false,
                correctDisplayAnswer = question.sampleAnswers.firstOrNull() ?: "",
                explanation = question.explanation,
                feedbackMessage = "Bạn chưa nhập câu trả lời tự luận."
            )
        }

        val exactMatch = question.sampleAnswers.any { sample ->
            AnswerNormalizer.areEquivalent(userText, sample)
        }

        val displayAnswer = buildString {
            append("Đáp án gợi ý:\n")
            question.sampleAnswers.forEachIndexed { idx, ans ->
                append("• ").append(ans).append("\n")
            }
            if (question.evaluationCriteria.isNotBlank()) {
                append("\nTiêu chí chấm: ").append(question.evaluationCriteria)
            }
        }.trim()

        return if (exactMatch) {
            GradeResult(
                status = GradeStatus.CORRECT,
                score = 1.0f,
                isCorrect = true,
                correctDisplayAnswer = displayAnswer,
                explanation = question.explanation,
                feedbackMessage = "Câu trả lời của bạn trùng khớp với đáp án chuẩn!"
            )
        } else {
            GradeResult(
                status = GradeStatus.SELF_EVALUATION,
                score = 0.5f, // Điểm ghi nhận tạm thời chờ tự đánh giá
                isCorrect = false,
                correctDisplayAnswer = displayAnswer,
                explanation = question.explanation,
                feedbackMessage = "Hãy đối chiếu câu trả lời của bạn với đáp án mẫu và tiêu chí bên dưới."
            )
        }
    }

    private fun parseBooleanText(text: String): Boolean? {
        val norm = AnswerNormalizer.normalize(text)
        return when {
            norm == "đúng" || norm == "dung" || norm == "true" || norm == "t" || norm == "1" -> true
            norm == "sai" || norm == "false" || norm == "f" || norm == "0" -> false
            else -> null
        }
    }
}
