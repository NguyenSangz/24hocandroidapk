package com.example.domain.evaluator

import kotlin.math.roundToInt

/**
 * Loại điểm đánh giá: Điểm mạnh, Điểm yếu hoặc Chưa đủ dữ liệu
 */
enum class SkillLevel {
    STRONG,   // >= 80%
    STABLE,   // 60% - 79%
    WEAK,     // < 60%
    INSUFFICIENT_DATA // < 10 câu
}

/**
 * Mục đánh giá một kỹ năng theo Chương hoặc theo Môn
 */
data class SkillEvaluationItem(
    val id: String, // chapterId hoặc subjectId
    val title: String, // Tên chương hoặc môn
    val subjectId: String,
    val subjectName: String,
    val totalQuestions: Int,
    val weightedAccuracy: Float, // 0..100%
    val level: SkillLevel,
    val isChapter: Boolean = true,
    val suggestionText: String = ""
)

/**
 * Kết quả phân tích Điểm mạnh và Điểm yếu nổi bật
 */
data class StrengthWeaknessAnalysis(
    val topStrengths: List<SkillEvaluationItem>,
    val topWeaknesses: List<SkillEvaluationItem>,
    val overallComment: String
)

/**
 * Bộ phân tích Điểm mạnh và Điểm yếu (StrengthWeaknessAnalyzer).
 * - Sử dụng công thức trọng số lũy tiến cho câu mới:
 *   5 câu gần nhất trọng số 2.0x, câu cũ hơn 1.0x.
 * - Chỉ kết luận khi có tối thiểu MIN_QUESTIONS_EVALUATION (10 câu).
 * - Sinh lời nhận xét thân thiện, tích cực, không gây áp lực phù hợp học sinh 11-15 tuổi.
 */
object StrengthWeaknessAnalyzer {

    /**
     * Dữ liệu một câu trả lời để phân tích
     */
    data class AnswerRecord(
        val questionId: String,
        val isCorrect: Boolean,
        val timestamp: Long,
        val chapterId: String,
        val chapterTitle: String,
        val subjectId: String
    )

    /**
     * Phân tích danh sách câu trả lời để tìm ra Top 3 điểm mạnh và Top 3 điểm yếu.
     */
    fun analyze(
        records: List<AnswerRecord>,
        subjectNames: Map<String, String> = emptyMap(),
        minQuestions: Int = StatsThresholdConfig.MIN_QUESTIONS_EVALUATION,
        strongThreshold: Float = StatsThresholdConfig.STRONG_ACCURACY_THRESHOLD,
        weakThreshold: Float = StatsThresholdConfig.WEAK_ACCURACY_THRESHOLD,
        recentWeightedCount: Int = StatsThresholdConfig.RECENT_WEIGHTED_COUNT,
        recentMultiplier: Float = StatsThresholdConfig.RECENT_WEIGHT_MULTIPLIER,
        baseMultiplier: Float = StatsThresholdConfig.BASE_WEIGHT_MULTIPLIER
    ): StrengthWeaknessAnalysis {
        if (records.isEmpty()) {
            return StrengthWeaknessAnalysis(
                topStrengths = emptyList(),
                topWeaknesses = emptyList(),
                overallComment = "Hãy bắt đầu làm bài luyện tập hoặc kiểm tra để khám phá điểm mạnh của bạn nhé!"
            )
        }

        // Nhóm câu trả lời theo chương
        val byChapter = records.groupBy { it.chapterId }

        val evaluatedItems = byChapter.map { (chapterId, answers) ->
            // Sắp xếp câu trả lời theo thời gian giảm dần (mới nhất lên đầu)
            val sortedAnswers = answers.sortedByDescending { it.timestamp }
            val firstRecord = sortedAnswers.first()
            val totalCount = sortedAnswers.size

            if (totalCount < minQuestions) {
                SkillEvaluationItem(
                    id = chapterId,
                    title = firstRecord.chapterTitle,
                    subjectId = firstRecord.subjectId,
                    subjectName = subjectNames[firstRecord.subjectId] ?: firstRecord.subjectId,
                    totalQuestions = totalCount,
                    weightedAccuracy = 0f,
                    level = SkillLevel.INSUFFICIENT_DATA,
                    isChapter = true,
                    suggestionText = "Cần làm thêm ít nhất ${minQuestions - totalCount} câu nữa để đánh giá chính xác."
                )
            } else {
                var totalWeight = 0.0f
                var weightedCorrect = 0.0f

                sortedAnswers.forEachIndexed { index, ans ->
                    val weight = if (index < recentWeightedCount) recentMultiplier else baseMultiplier
                    totalWeight += weight
                    if (ans.isCorrect) {
                        weightedCorrect += weight
                    }
                }

                val accuracyPercent = if (totalWeight > 0f) {
                    (weightedCorrect / totalWeight) * 100f
                } else {
                    0f
                }

                val level = when {
                    accuracyPercent >= (strongThreshold * 100f) -> SkillLevel.STRONG
                    accuracyPercent < (weakThreshold * 100f) -> SkillLevel.WEAK
                    else -> SkillLevel.STABLE
                }

                val suggestion = when (level) {
                    SkillLevel.STRONG -> "Bạn làm rất tốt phần này! Hãy tiếp tục duy trì phong độ nhé."
                    SkillLevel.WEAK -> "Phần kiến thức này cần củng cố thêm. Hãy thử ôn lại lý thuyết và làm câu sai."
                    SkillLevel.STABLE -> "Kiến thức khá ổn định. Ôn thêm một chút nữa là sẽ thành thế mạnh đấy!"
                    SkillLevel.INSUFFICIENT_DATA -> ""
                }

                SkillEvaluationItem(
                    id = chapterId,
                    title = firstRecord.chapterTitle,
                    subjectId = firstRecord.subjectId,
                    subjectName = subjectNames[firstRecord.subjectId] ?: firstRecord.subjectId,
                    totalQuestions = totalCount,
                    weightedAccuracy = (accuracyPercent * 10f).roundToInt() / 10f,
                    level = level,
                    isChapter = true,
                    suggestionText = suggestion
                )
            }
        }

        // Lọc top 3 điểm mạnh (độ chính xác cao nhất)
        val strengths = evaluatedItems
            .filter { it.level == SkillLevel.STRONG }
            .sortedByDescending { it.weightedAccuracy }
            .take(3)

        // Lọc top 3 điểm yếu (độ chính xác thấp nhất)
        val weaknesses = evaluatedItems
            .filter { it.level == SkillLevel.WEAK }
            .sortedBy { it.weightedAccuracy }
            .take(3)

        // Sinh lời nhận xét thân thiện, khích lệ lứa tuổi học sinh 11-15
        val comment = generateEncouragingComment(strengths.size, weaknesses.size, evaluatedItems.size)

        return StrengthWeaknessAnalysis(
            topStrengths = strengths,
            topWeaknesses = weaknesses,
            overallComment = comment
        )
    }

    /**
     * Sinh lời nhận xét hoàn toàn bằng quy tắc cố định, tích cực và khoa học.
     */
    private fun generateEncouragingComment(strongCount: Int, weakCount: Int, totalTopics: Int): String {
        return when {
            strongCount >= 3 && weakCount == 0 ->
                "Tuyệt vời! Bạn đang nắm rất vững các chuyên đề đã học. Hãy thử sức với những bài kiểm tra khó hơn nhé!"

            strongCount >= 1 && weakCount >= 1 ->
                "Bạn đã có những thế mạnh rất rõ nét! Hãy dành thêm 10 phút ôn các phần chưa tự tin để tiến bộ toàn diện nhé."

            strongCount == 0 && weakCount >= 2 ->
                "Mọi bước khởi đầu đều đáng quý! Luyện tập đều đặn mỗi ngày một ít sẽ giúp bạn nắm chắc kiến thức ngay thôi."

            strongCount >= 1 && weakCount == 0 ->
                "Bạn đang đi rất đúng hướng! Tiếp tục mở rộng sang các bài học mới để rèn luyện thêm nha."

            else ->
                "Kiến thức của bạn đang tiến triển đều đặn. Hãy tiếp tục giải thêm bài tập để xây dựng nền tảng vững chắc!"
        }
    }
}
