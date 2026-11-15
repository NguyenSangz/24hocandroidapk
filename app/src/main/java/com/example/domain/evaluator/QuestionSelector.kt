package com.example.domain.evaluator

import com.example.domain.model.Difficulty
import com.example.domain.model.Question
import java.util.Random

/**
 * Thuật toán chọn ngẫu nhiên N câu hỏi theo tỉ lệ độ khó (F05)
 * Hỗ trợ tham số seed để unit test và tái hiện kết quả (reproducible).
 * Hỗ trợ xáo trộn phương án trắc nghiệm mà vẫn giữ toàn vẹn đáp án đúng.
 */
object QuestionSelector {

    /**
     * Tỉ lệ phân bổ câu hỏi theo độ khó chuẩn THCS GDPT 2018:
     * - Nhận biết (40%): 4 câu
     * - Thông hiểu (40%): 4 câu
     * - Vận dụng / Vận dụng cao (20%): 2 câu
     */
    data class DifficultyRatio(
        val recognitionCount: Int = 4,
        val comprehensionCount: Int = 4,
        val applicationCount: Int = 2
    ) {
        val totalCount: Int get() = recognitionCount + comprehensionCount + applicationCount
    }

    /**
     * Chọn N câu hỏi từ ngân hàng câu hỏi đầu vào theo tỉ lệ độ khó mong muốn
     * @param questions Danh sách câu hỏi nguồn
     * @param totalCount Tổng số câu cần lấy (nếu không dùng ratio cụ thể)
     * @param ratio Tỉ lệ độ khó mong muốn (nếu null sẽ tính theo tỉ lệ 4:4:2 hoặc phân bổ đều)
     * @param randomSeed Seed ngẫu nhiên để deterministic test
     */
    fun selectQuestions(
        questions: List<Question>,
        totalCount: Int,
        ratio: DifficultyRatio? = null,
        randomSeed: Long? = null
    ): List<Question> {
        if (questions.isEmpty() || totalCount <= 0) return emptyList()
        val rng = if (randomSeed != null) Random(randomSeed) else Random()

        // Phân loại câu hỏi theo độ khó
        val recognitionPool = questions.filter { it.difficulty == Difficulty.RECOGNITION }.shuffled(rng).toMutableList()
        val comprehensionPool = questions.filter { it.difficulty == Difficulty.COMPREHENSION }.shuffled(rng).toMutableList()
        val applicationPool = questions.filter { it.difficulty == Difficulty.APPLICATION || it.difficulty == Difficulty.HIGH_APPLICATION }.shuffled(rng).toMutableList()

        val selected = mutableListOf<Question>()

        if (ratio != null) {
            // Lấy theo ratio chỉ định
            selected.addAll(takeUpTo(recognitionPool, ratio.recognitionCount))
            selected.addAll(takeUpTo(comprehensionPool, ratio.comprehensionCount))
            selected.addAll(takeUpTo(applicationPool, ratio.applicationCount))
        } else {
            // Tỉ lệ mặc định 40% - 40% - 20%
            val recTarget = Math.round(totalCount * 0.4).toInt()
            val compTarget = Math.round(totalCount * 0.4).toInt()
            val appTarget = (totalCount - recTarget - compTarget).coerceAtLeast(0)

            selected.addAll(takeUpTo(recognitionPool, recTarget))
            selected.addAll(takeUpTo(comprehensionPool, compTarget))
            selected.addAll(takeUpTo(applicationPool, appTarget))
        }

        // Nếu các pool chuyên biệt chưa đủ số lượng, bù đắp từ các câu còn lại trong mọi pool
        if (selected.size < totalCount) {
            val alreadySelectedIds = selected.map { it.id }.toSet()
            val remainingQuestions = questions.filter { !alreadySelectedIds.contains(it.id) }.shuffled(rng)
            val needed = totalCount - selected.size
            selected.addAll(remainingQuestions.take(needed))
        }

        // Xáo trộn danh sách cuối cùng để thứ tự không bị lộ theo khối độ khó
        return selected.shuffled(rng).take(totalCount)
    }

    /**
     * Xáo trộn thứ tự các lựa chọn trong câu trắc nghiệm (Multiple Choice)
     * Đảm bảo đáp án đúng (correctAnswer) vẫn được giữ nguyên vẹn để việc chấm điểm hoàn toàn chính xác.
     */
    fun shuffleMultipleChoiceOptions(
        question: Question.MultipleChoice,
        randomSeed: Long? = null
    ): Question.MultipleChoice {
        val rng = if (randomSeed != null) Random(randomSeed) else Random()
        val shuffled = question.options.shuffled(rng)
        return question.copy(options = shuffled)
    }

    private fun takeUpTo(pool: MutableList<Question>, count: Int): List<Question> {
        val takeCount = minOf(count, pool.size)
        val result = pool.take(takeCount)
        repeat(takeCount) { pool.removeAt(0) }
        return result
    }
}
