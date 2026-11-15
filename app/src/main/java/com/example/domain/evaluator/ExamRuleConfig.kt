package com.example.domain.evaluator

import com.example.data.local.entity.ChapterEntity
import com.example.domain.model.TestType
import kotlin.math.ceil

/**
 * File cấu hình quy tắc bài kiểm tra (F07).
 * Khai báo tập trung, rõ ràng, dễ dàng tùy chỉnh theo từng phân phối chương trình GDPT 2018.
 */
object ExamRuleConfig {

    /**
     * Danh sách các tùy chọn số lượng câu hỏi mà người học có thể chọn
     */
    val availableQuestionCounts: List<Int> = listOf(5, 10, 15, 20, 25, 30, 40)

    /**
     * Danh sách các tùy chọn thời gian làm bài (tính bằng phút)
     */
    val availableDurationMinutes: List<Int> = listOf(10, 15, 20, 30, 45, 60, 90)

    /**
     * Lấy số lượng câu hỏi mặc định theo loại đề
     */
    fun getDefaultQuestionCount(type: TestType): Int = type.defaultQuestionCount

    /**
     * Lấy thời gian làm bài mặc định (phút) theo loại đề
     */
    fun getDefaultDurationMinutes(type: TestType): Int = type.defaultDurationMinutes

    /**
     * Quy tắc chọn các chương đủ điều kiện cho từng loại đề:
     * - Giữa kỳ (MID_TERM): lấy khoảng nửa số chương đầu tiên của môn học (tối thiểu 1 chương).
     * - Cuối kỳ (FINAL): lấy toàn bộ các chương của môn học.
     * - Theo chương (CHAPTER): chỉ lấy 1 chương chỉ định.
     * - Theo bài (LESSON): lấy theo bài chỉ định (không lọc ở cấp chương).
     */
    fun filterEligibleChapters(
        allChapters: List<ChapterEntity>,
        type: TestType,
        selectedChapterId: String? = null
    ): List<ChapterEntity> {
        val sortedChapters = allChapters.sortedBy { it.order }
        return when (type) {
            TestType.LESSON -> sortedChapters
            TestType.CHAPTER -> {
                if (selectedChapterId != null) {
                    sortedChapters.filter { it.id == selectedChapterId }
                } else {
                    sortedChapters.take(1)
                }
            }
            TestType.MID_TERM -> {
                // Lấy nửa đầu số chương (ceil(size / 2))
                val midTermCount = ceil(sortedChapters.size / 2.0).toInt().coerceAtLeast(1)
                sortedChapters.take(midTermCount)
            }
            TestType.FINAL -> {
                // Cuối kỳ bao quát toàn bộ chương
                sortedChapters
            }
        }
    }

    /**
     * Quy tắc làm bài hiển thị cho học sinh trước khi bắt đầu
     */
    val examRulesText: List<String> = listOf(
        "Không có gợi ý và trợ giúp trong suốt thời gian làm bài.",
        "Không hiển thị đáp án đúng cho đến khi bạn bấm Nộp bài.",
        "Đồng hồ đếm ngược liên tục; hệ thống sẽ tự động thu bài khi hết giờ.",
        "Bạn có thể chuyển qua lại giữa các câu và đánh dấu xem lại sau.",
        "Bài làm được tự động lưu nháp liên tục phòng trường hợp app bị gián đoạn."
    )
}
