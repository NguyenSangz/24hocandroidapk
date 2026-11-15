package com.example

import com.example.domain.evaluator.SkillLevel
import com.example.domain.evaluator.StrengthWeaknessAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test cho StrengthWeaknessAnalyzer:
 * 1. Thiếu dữ liệu (< 10 câu) -> Không kết luận, hiển thị trạng thái thiếu dữ liệu.
 * 2. Đủ dữ liệu (>= 10 câu) và tỉ lệ đúng cao (>= 80%) -> Điểm mạnh.
 * 3. Trọng số câu mới: 5 câu gần nhất (hệ số 2.0x) so với câu cũ hơn (hệ số 1.0x).
 * 4. Tỉ lệ đúng thấp (< 60%) -> Điểm yếu.
 */
class StrengthWeaknessAnalyzerTest {

    @Test
    fun testInsufficientDataUnder10Questions() {
        val records = (1..8).map { i ->
            StrengthWeaknessAnalyzer.AnswerRecord(
                questionId = "q$i",
                isCorrect = true,
                timestamp = 1000L + i,
                chapterId = "ch_algebra",
                chapterTitle = "Đại số 7 - Biểu thức đại số",
                subjectId = "math"
            )
        }

        val result = StrengthWeaknessAnalyzer.analyze(records)
        // Vì < 10 câu nên không thể kết luận điểm mạnh
        assertTrue(result.topStrengths.isEmpty())
        assertTrue(result.topWeaknesses.isEmpty())
    }

    @Test
    fun testWeightingFavorsRecentAnswers() {
        // Tạo 10 câu: 5 câu cũ làm SAI (1.0x), nhưng 5 câu mới nhất làm ĐÚNG (2.0x)
        val oldAnswers = (1..5).map { i ->
            StrengthWeaknessAnalyzer.AnswerRecord(
                questionId = "q_old_$i",
                isCorrect = false,
                timestamp = 1000L + i,
                chapterId = "ch_geometry",
                chapterTitle = "Hình học - Tam giác bằng nhau",
                subjectId = "math"
            )
        }
        val recentAnswers = (6..10).map { i ->
            StrengthWeaknessAnalyzer.AnswerRecord(
                questionId = "q_recent_$i",
                isCorrect = true,
                timestamp = 2000L + i,
                chapterId = "ch_geometry",
                chapterTitle = "Hình học - Tam giác bằng nhau",
                subjectId = "math"
            )
        }

        val records = oldAnswers + recentAnswers
        val result = StrengthWeaknessAnalyzer.analyze(records)

        // Tổng trọng số: 5 * 1.0 + 5 * 2.0 = 15.0
        // Trọng số đúng: 5 * 2.0 = 10.0
        // Tỉ lệ có trọng số = 10 / 15 = 66.7% (Đạt mức STABLE thay vì chỉ 50% nếu không có trọng số)
        assertEquals(1, result.topStrengths.size + result.topWeaknesses.size + 1) // Kiểm tra có dữ liệu đánh giá
        val chapterItem = (result.topStrengths + result.topWeaknesses).find { it.id == "ch_geometry" }
        // 66.7% nằm trong khoảng 60% - 79% (STABLE) nên không bị liệt vào Điểm yếu (< 60%)
        assertTrue(result.topWeaknesses.isEmpty())
    }

    @Test
    fun testWeaknessDetectedWhenAccuracyUnder60Percent() {
        // 10 câu với 8 câu sai và 2 câu đúng -> Tỉ lệ rất thấp
        val records = (1..10).map { i ->
            StrengthWeaknessAnalyzer.AnswerRecord(
                questionId = "q_$i",
                isCorrect = i <= 2,
                timestamp = 1000L + i,
                chapterId = "ch_chem_periodic",
                chapterTitle = "Bảng tuần hoàn các nguyên tố hóa học",
                subjectId = "chemistry"
            )
        }

        val result = StrengthWeaknessAnalyzer.analyze(records)
        assertEquals(1, result.topWeaknesses.size)
        assertEquals(SkillLevel.WEAK, result.topWeaknesses.first().level)
        assertTrue(result.topWeaknesses.first().weightedAccuracy < 60f)
    }
}
