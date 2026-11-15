package com.example.domain.evaluator

import java.text.Normalizer
import java.util.regex.Pattern

/**
 * Tiện ích chuẩn hóa chuỗi tiếng Việt và văn bản học tập (F05)
 * - Xóa khoảng trắng thừa ở đầu, cuối và giữa các từ
 * - Chuẩn hóa ký tự Unicode tiếng Việt theo chuẩn NFC (tránh lỗi tổ hợp vs dựng sẵn)
 * - Chuyển chữ thường
 * - Bỏ dấu câu kết thúc (dấu chấm, dấu chấm than, dấu hỏi...)
 * - Hỗ trợ chuẩn hóa dấu phẩy/chấm cho số thập phân
 */
object AnswerNormalizer {

    private val MULTIPLE_SPACES = Pattern.compile("\\s+")
    private val TRAILING_PUNCTUATION = Pattern.compile("[.!?…]+$")

    /**
     * Chuẩn hóa văn bản trả lời nói chung
     */
    fun normalize(input: String?): String {
        if (input == null) return ""
        // 1. Chuẩn hóa Unicode NFC (đảm bảo dấu tiếng Việt thống nhất)
        val nfc = Normalizer.normalize(input, Normalizer.Form.NFC)
        // 2. Trim khoảng trắng đầu/cuối và thu gọn chuỗi khoảng trắng bên trong thành 1 khoảng trắng
        var trimmed = MULTIPLE_SPACES.matcher(nfc.trim()).replaceAll(" ")
        // 3. Bỏ dấu chấm/dấu câu cuối câu nếu có
        trimmed = TRAILING_PUNCTUATION.matcher(trimmed).replaceAll("").trim()
        // 4. Chuyển về chữ thường
        return trimmed.lowercase()
    }

    /**
     * So sánh 2 chuỗi có tương đương nhau sau khi chuẩn hóa văn bản
     */
    fun areEquivalent(text1: String?, text2: String?): Boolean {
        val norm1 = normalize(text1)
        val norm2 = normalize(text2)
        if (norm1 == norm2) return true

        // Thử so khớp biểu diễn số thập phân (ví dụ: "3.5" tương đương "3,5")
        val num1 = parseFlexibleNumber(norm1)
        val num2 = parseFlexibleNumber(norm2)
        if (num1 != null && num2 != null) {
            return Math.abs(num1 - num2) < 0.000001
        }

        return false
    }

    /**
     * Chuyển đổi một chuỗi số chấp nhận cả dấu phẩy và dấu chấm thành Double nếu hợp lệ
     */
    fun parseFlexibleNumber(text: String): Double? {
        val clean = text.trim().replace(',', '.')
        return clean.toDoubleOrNull()
    }
}
