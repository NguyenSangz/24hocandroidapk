package com.example.domain.evaluator

import com.example.domain.model.Question
import com.example.domain.model.TestBuildResult
import com.example.domain.model.TestType

/**
 * Module dựng đề kiểm tra theo cấu hình và loại đề (F07).
 * Logic thuần túy, an toàn, xử lý lỗi thiếu câu hỏi một cách thông minh.
 */
object TestBuilder {

    /**
     * Dựng đề kiểm tra từ danh sách câu hỏi nguồn
     * @param sourceQuestions Toàn bộ câu hỏi hợp lệ trong phạm vi đã chọn (môn/chương/bài)
     * @param requestedCount Số lượng câu hỏi mà người học yêu cầu
     * @param testType Loại đề kiểm tra
     * @param randomSeed Seed ngẫu nhiên (dùng khi test)
     */
    fun buildTest(
        sourceQuestions: List<Question>,
        requestedCount: Int,
        testType: TestType,
        randomSeed: Long? = null
    ): TestBuildResult {
        if (sourceQuestions.isEmpty()) {
            return TestBuildResult.InsufficientQuestions(
                availableCount = 0,
                requestedCount = requestedCount,
                suggestedCount = 0,
                message = "Hiện chưa có câu hỏi nào trong phạm vi được chọn. Vui lòng chọn môn hoặc nội dung khác."
            )
        }

        val availableCount = sourceQuestions.size

        // Nếu số câu trong kho ít hơn số câu yêu cầu: Báo rõ và đề xuất số câu tối đa có thể làm
        if (availableCount < requestedCount) {
            val suggestedCount = when {
                availableCount >= 20 -> 20
                availableCount >= 15 -> 15
                availableCount >= 10 -> 10
                availableCount >= 5 -> 5
                else -> availableCount
            }
            return TestBuildResult.InsufficientQuestions(
                availableCount = availableCount,
                requestedCount = requestedCount,
                suggestedCount = suggestedCount,
                message = "Nội dung này hiện có $availableCount câu hỏi, chưa đủ $requestedCount câu theo cấu hình. Bạn có thể giảm xuống $suggestedCount câu để làm bài ngay."
            )
        }

        // Chọn câu hỏi theo tỉ lệ độ khó chuẩn THCS GDPT 2018 (40% Nhận biết, 40% Thông hiểu, 20% Vận dụng)
        val selectedQuestions = QuestionSelector.selectQuestions(
            questions = sourceQuestions,
            totalCount = requestedCount,
            randomSeed = randomSeed
        )

        // Xáo trộn thứ tự các lựa chọn trong câu hỏi trắc nghiệm khách quan
        val finalizedQuestions = selectedQuestions.map { q ->
            if (q is Question.MultipleChoice) {
                QuestionSelector.shuffleMultipleChoiceOptions(q, randomSeed)
            } else {
                q
            }
        }

        return TestBuildResult.Success(finalizedQuestions)
    }
}
