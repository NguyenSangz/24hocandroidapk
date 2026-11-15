package com.example.domain.evaluator

import com.example.domain.model.AcademicRank
import com.example.domain.model.ChapterScoreAnalysis
import com.example.domain.model.GradeResult
import com.example.domain.model.GradeStatus
import com.example.domain.model.Question
import com.example.domain.model.TestAnswerDetail
import com.example.domain.model.TestScoreSummary
import com.example.domain.model.UserAnswer
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Module tính điểm và xếp loại thuần túy cho bài kiểm tra (F07).
 * Không phụ thuộc Android Context / Framework. Dễ dàng viết Unit Test.
 */
object TestScorer {

    /**
     * Chấm toàn bộ bài kiểm tra và tổng hợp kết quả chi tiết
     * @param details Danh sách các câu hỏi kèm câu trả lời của học sinh và chương bài
     * @param chapterTitlesMap Bản đồ chapterId -> tên chương để hiển thị điểm mạnh/yếu
     */
    fun scoreTest(
        details: List<TestAnswerDetail>,
        chapterTitlesMap: Map<String, String> = emptyMap()
    ): Pair<TestScoreSummary, List<TestAnswerDetail>> {
        val totalQuestions = details.size
        if (totalQuestions == 0) {
            return Pair(
                TestScoreSummary(
                    finalScore = 0.0f,
                    academicRank = AcademicRank.NEEDS_EFFORT,
                    totalQuestions = 0,
                    correctCount = 0,
                    partialCount = 0,
                    wrongCount = 0,
                    unansweredCount = 0,
                    chapterAnalysis = emptyList(),
                    praiseMessage = "Chưa có câu hỏi trong bài kiểm tra."
                ),
                emptyList()
            )
        }

        // Điểm tối đa mỗi câu hỏi trên thang điểm 10
        val pointsPerQuestion = 10.0f / totalQuestions.toFloat()

        var totalEarnedPoints = 0.0f
        var correctCount = 0
        var partialCount = 0
        var wrongCount = 0
        var unansweredCount = 0

        val gradedDetails = mutableListOf<TestAnswerDetail>()
        val chapterStatsMap = mutableMapOf<String, ChapterStats>()

        for (item in details) {
            val chapterId = item.question.lessonId // Hoặc lessonId / chapterId
            val chTitle = chapterTitlesMap[item.chapterTitle] ?: item.chapterTitle.ifBlank { "Chương trọng tâm" }
            val stats = chapterStatsMap.getOrPut(chTitle) {
                ChapterStats(chapterTitle = chTitle)
            }
            stats.totalQuestions++
            stats.maxPoints += pointsPerQuestion

            val answer = item.userAnswer
            if (answer == null || !item.isAnswered) {
                // Câu bỏ trống
                unansweredCount++
                stats.wrongQuestions++
                val gradeResult = GradeResult(
                    status = GradeStatus.INCORRECT,
                    score = 0.0f,
                    isCorrect = false,
                    correctDisplayAnswer = getDisplayCorrectAnswer(item.question),
                    explanation = item.question.explanation,
                    feedbackMessage = "Bạn đã bỏ trống câu này."
                )
                gradedDetails.add(item.copy(gradeResult = gradeResult, isAnswered = false))
            } else {
                // Câu đã trả lời: chấm bằng QuestionGrader
                val initialGrade = item.gradeResult ?: QuestionGrader.grade(item.question, answer)
                val earnedRatio = initialGrade.score.coerceIn(0.0f, 1.0f)
                val earnedPoints = earnedRatio * pointsPerQuestion
                totalEarnedPoints += earnedPoints
                stats.earnedPoints += earnedPoints

                when (initialGrade.status) {
                    GradeStatus.CORRECT -> {
                        correctCount++
                        stats.correctQuestions++
                    }
                    GradeStatus.PARTIALLY_CORRECT -> {
                        partialCount++
                        if (earnedRatio >= 0.5f) {
                            stats.correctQuestions++
                        } else {
                            stats.wrongQuestions++
                        }
                    }
                    GradeStatus.SELF_EVALUATION -> {
                        // Tự luận ngắn chờ học sinh tự đánh giá
                        if (item.isSelfEvaluated && initialGrade.isCorrect) {
                            correctCount++
                            stats.correctQuestions++
                        } else {
                            partialCount++
                            stats.wrongQuestions++
                        }
                    }
                    GradeStatus.INCORRECT -> {
                        wrongCount++
                        stats.wrongQuestions++
                    }
                }

                gradedDetails.add(item.copy(gradeResult = initialGrade, isAnswered = true))
            }
        }

        // Làm tròn điểm số thang 10 đến 1 chữ số thập phân
        val roundedScore = roundToOneDecimal(totalEarnedPoints.coerceIn(0.0f, 10.0f))
        val rank = evaluateRank(roundedScore)
        val praiseMessage = generatePraiseMessage(roundedScore, rank)

        // Phân tích điểm theo chương/chủ đề
        val chapterAnalysis = chapterStatsMap.values.map { cs ->
            ChapterScoreAnalysis(
                chapterId = cs.chapterTitle,
                chapterTitle = cs.chapterTitle,
                totalQuestions = cs.totalQuestions,
                correctCount = cs.correctQuestions,
                scoreEarned = roundToOneDecimal(cs.earnedPoints),
                maxScore = roundToOneDecimal(cs.maxPoints)
            )
        }.sortedByDescending { it.percentage }

        val summary = TestScoreSummary(
            finalScore = roundedScore,
            maxScore = 10.0f,
            academicRank = rank,
            totalQuestions = totalQuestions,
            correctCount = correctCount,
            partialCount = partialCount,
            wrongCount = wrongCount,
            unansweredCount = unansweredCount,
            chapterAnalysis = chapterAnalysis,
            praiseMessage = praiseMessage
        )

        return Pair(summary, gradedDetails)
    }

    /**
     * Cập nhật điểm khi học sinh tự đánh giá câu tự luận ngắn ở màn hình xem lại/kết quả
     */
    fun updateSelfEvaluation(
        details: List<TestAnswerDetail>,
        questionId: String,
        isCorrect: Boolean,
        chapterTitlesMap: Map<String, String> = emptyMap()
    ): Pair<TestScoreSummary, List<TestAnswerDetail>> {
        val updatedDetails = details.map { detail ->
            if (detail.question.id == questionId && detail.question is Question.ShortAnswer) {
                val currentResult = detail.gradeResult ?: QuestionGrader.grade(
                    detail.question,
                    detail.userAnswer ?: UserAnswer.ShortAnswer("")
                )
                val newStatus = if (isCorrect) GradeStatus.CORRECT else GradeStatus.INCORRECT
                val newScore = if (isCorrect) 1.0f else 0.0f
                val updatedGrade = currentResult.copy(
                    status = newStatus,
                    score = newScore,
                    isCorrect = isCorrect,
                    feedbackMessage = if (isCorrect) "Bạn đã tự đánh giá câu này Đúng." else "Bạn đã tự đánh giá câu này Chưa đúng."
                )
                detail.copy(
                    gradeResult = updatedGrade,
                    isSelfEvaluated = true
                )
            } else {
                detail
            }
        }
        return scoreTest(updatedDetails, chapterTitlesMap)
    }

    /**
     * Làm tròn 1 chữ số thập phân chuẩn nửa lên (HALF_UP)
     */
    fun roundToOneDecimal(value: Float): Float {
        return BigDecimal(value.toDouble())
            .setScale(1, RoundingMode.HALF_UP)
            .toFloat()
    }

    /**
     * Xếp loại học lực theo thang điểm 10
     */
    fun evaluateRank(score: Float): AcademicRank {
        return when {
            score >= 8.0f -> AcademicRank.EXCELLENT
            score >= 6.5f -> AcademicRank.GOOD
            score >= 5.0f -> AcademicRank.AVERAGE
            else -> AcademicRank.NEEDS_EFFORT
        }
    }

    private fun generatePraiseMessage(score: Float, rank: AcademicRank): String {
        return when (rank) {
            AcademicRank.EXCELLENT -> "Xuất sắc! Bạn nắm kiến thức vô cùng vững vàng! 🌟"
            AcademicRank.GOOD -> "Rất tốt! Bạn hiểu bài sâu sắc, tiếp tục phát huy nhé! 👏"
            AcademicRank.AVERAGE -> "Khá ổn! Hãy rà soát lại các câu sai để bứt phá điểm số nhé! 📚"
            AcademicRank.NEEDS_EFFORT -> "Đừng nản lòng! Hãy xem lại lời giải chi tiết và rèn luyện thêm nhé! 💪"
        }
    }

    private fun getDisplayCorrectAnswer(question: Question): String {
        return when (question) {
            is Question.MultipleChoice -> question.correctAnswer
            is Question.TrueFalse -> if (question.correctAnswer) "Đúng" else "Sai"
            is Question.FillBlank -> question.acceptedAnswers.joinToString(" hoặc ")
            is Question.Matching -> question.pairs.joinToString("; ") { "${it.left} ➔ ${it.right}" }
            is Question.Ordering -> question.correctOrder.joinToString(" ➔ ")
            is Question.ShortAnswer -> question.sampleAnswers.firstOrNull() ?: ""
        }
    }

    private class ChapterStats(
        val chapterTitle: String,
        var totalQuestions: Int = 0,
        var correctQuestions: Int = 0,
        var wrongQuestions: Int = 0,
        var earnedPoints: Float = 0.0f,
        var maxPoints: Float = 0.0f
    )
}
