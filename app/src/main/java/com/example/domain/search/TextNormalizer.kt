package com.example.domain.search

import java.text.Normalizer
import java.util.Locale
import java.util.regex.Pattern

/**
 * Bộ công cụ chuẩn hóa văn bản thuần Kotlin phục vụ tìm kiếm toàn ứng dụng (F13):
 * - Chuẩn hóa Unicode NFC & chữ thường.
 * - Loại bỏ dấu tiếng Việt chuẩn xác (bao gồm đ/Đ -> d/D).
 * - Làm sạch mã công thức toán học LaTeX: loại bỏ lệnh macro, giữ lại số và từ khóa chữ.
 * - Làm sạch truy vấn đầu vào, ngăn ngừa crash do lỗi cú pháp FTS4/SQLite.
 */
object TextNormalizer {

    private val DIACRITICS_PATTERN: Pattern =
        Pattern.compile("\\p{InCombiningDiacriticalMarks}+")

    private val LATEX_COMMAND_PATTERN: Pattern =
        Pattern.compile("\\\\[a-zA-Z]+(\\{([^}]*)\\})?")

    private val LATEX_SPECIAL_MATH_PATTERN: Pattern =
        Pattern.compile("[$^_{}\\\\]")

    private val FRAC_REGEX = Regex("""\\frac\{([^}]*)\}\{([^}]*)\}""")
    private val SQRT_REGEX = Regex("""\\sqrt(\[[^]]*\])?\{([^}]*)\}""")
    private val TEXT_STYLE_REGEX = Regex("""\\(text|textbf|textit|mathrm|mathbf)\{([^}]*)\}""")
    private val MULTI_SPACE_REGEX = Regex("\\s+")
    private val SAFE_TOKENS_CLEAN_REGEX = Regex("[\"\'*^:()\\-+~/?\\\\<>{}\\[\\]!@#%&=|;]")

    /**
     * Chuẩn hóa Unicode NFC và đưa về chữ thường.
     */
    fun normalizeNfc(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return Normalizer.normalize(text.trim(), Normalizer.Form.NFC).lowercase(Locale.ROOT)
    }

    /**
     * Chuyển chuỗi tiếng Việt có dấu thành không dấu.
     * Quy tắc:
     * - đ -> d, Đ -> D
     * - Tách dấu Unicode NFD và loại bỏ toàn bộ Combining Diacritical Marks.
     * - Giữ nguyên ký tự chữ và số.
     */
    fun removeDiacritics(text: String?): String {
        if (text.isNullOrBlank()) return ""
        // Xử lý đ/Đ trước khi phân rã NFD
        val replacedD = text
            .replace('đ', 'd')
            .replace('Đ', 'D')
            .replace('đ', 'd')

        val nfd = Normalizer.normalize(replacedD, Normalizer.Form.NFD)
        val withoutMarks = DIACRITICS_PATTERN.matcher(nfd).replaceAll("")
        return Normalizer.normalize(withoutMarks, Normalizer.Form.NFC)
    }

    /**
     * Chuẩn hóa toàn diện để đưa vào cột tìm kiếm không dấu:
     * Chữ thường + Không dấu + Chuẩn hóa khoảng trắng.
     */
    fun toUnaccentedSearchTerm(text: String?): String {
        return removeDiacritics(normalizeNfc(text))
    }

    /**
     * Loại bỏ các mã/lệnh LaTeX, giữ lại chữ cái và số nguyên để học sinh tìm được nội dung toán học.
     * Ví dụ:
     * - `\frac{a}{b}` -> `a b`
     * - `\sqrt{x + 1}` -> `x + 1`
     * - `S = a \cdot b` -> `S = a b`
     * - `$\frac{3}{4}$ phân số` -> `3 4 phân số`
     */
    fun stripLatex(latexOrText: String?): String {
        if (latexOrText.isNullOrBlank()) return ""
        var result = latexOrText

        // 1. Chuyển các phân số dạng \frac{x}{y} thành "x y"
        result = FRAC_REGEX.replace(result) { matchResult ->
            "${matchResult.groupValues[1]} ${matchResult.groupValues[2]}"
        }

        // 2. Chuyển căn thức \sqrt{x} thành x
        result = SQRT_REGEX.replace(result) { matchResult ->
            matchResult.groupValues[2]
        }

        // 3. Xử lý \text{...}, \textbf{...}, \mathit{...}
        result = TEXT_STYLE_REGEX.replace(result) { matchResult ->
            matchResult.groupValues[2]
        }

        // 4. Bỏ các lệnh LaTeX còn lại (\alpha, \pm, \times, \cdot, ...)
        result = LATEX_COMMAND_PATTERN.matcher(result).replaceAll(" ")

        // 5. Bỏ các ký tự đặc biệt toán học LaTeX như $, ^, _, {, }, \
        result = LATEX_SPECIAL_MATH_PATTERN.matcher(result).replaceAll(" ")

        // 6. Rút gọn khoảng trắng
        return MULTI_SPACE_REGEX.replace(result, " ").trim()
    }

    /**
     * Làm sạch từ khóa tìm kiếm do người dùng nhập:
     * - Loại bỏ hoàn toàn các ký tự có thể gây lỗi cú pháp SQLite FTS4: ", ', *, ^, :, (, ), -, +, ~, /, \
     * - Tách lấy các token an toàn gồm chữ cái và số.
     */
    fun extractSearchTokens(rawQuery: String?): List<String> {
        if (rawQuery.isNullOrBlank()) return emptyList()
        val cleaned = SAFE_TOKENS_CLEAN_REGEX.replace(rawQuery, " ")
            .trim()

        if (cleaned.isBlank()) return emptyList()

        return MULTI_SPACE_REGEX.split(cleaned)
            .map { it.trim() }
            .filter { token ->
                token.isNotEmpty() &&
                        !token.equals("AND", ignoreCase = true) &&
                        !token.equals("OR", ignoreCase = true) &&
                        !token.equals("NOT", ignoreCase = true) &&
                        !token.equals("NEAR", ignoreCase = true)
            }
    }

    /**
     * Tìm vị trí xuất hiện của các từ khóa (hỗ trợ cả có dấu và không dấu)
     * để làm nổi bật (highlight) trong UI.
     * Trả về danh sách Pair(đoạn văn bản, có phải từ khóa được tô sáng hay không).
     */
    fun highlightText(fullText: String, query: String): List<Pair<String, Boolean>> {
        if (fullText.isEmpty()) return emptyList()
        val tokens = extractSearchTokens(query)
        if (tokens.isEmpty()) return listOf(Pair(fullText, false))

        // Chuẩn hóa văn bản gốc sang không dấu để so khớp vị trí chính xác
        val fullTextLower = fullText.lowercase(Locale.ROOT)
        val fullTextNoAccent = removeDiacritics(fullTextLower)

        // Đánh dấu các index được highlight
        val isMatched = BooleanArray(fullText.length)

        for (token in tokens) {
            val tokenClean = token.lowercase(Locale.ROOT)
            val tokenNoAccent = removeDiacritics(tokenClean)
            val tokenLen = token.length

            if (tokenLen == 0) continue

            // 1. Quét theo có dấu trước
            var idx = fullTextLower.indexOf(tokenClean)
            while (idx >= 0) {
                for (i in idx until (idx + tokenLen).coerceAtMost(fullText.length)) {
                    isMatched[i] = true
                }
                idx = fullTextLower.indexOf(tokenClean, idx + 1)
            }

            // 2. Quét theo không dấu
            var idxNo = fullTextNoAccent.indexOf(tokenNoAccent)
            while (idxNo >= 0) {
                for (i in idxNo until (idxNo + tokenNoAccent.length).coerceAtMost(fullText.length)) {
                    isMatched[i] = true
                }
                idxNo = fullTextNoAccent.indexOf(tokenNoAccent, idxNo + 1)
            }
        }

        // Gom các đoạn liên tiếp có cùng trạng thái highlight
        val segments = mutableListOf<Pair<String, Boolean>>()
        var currentStart = 0
        var currentHighlight = isMatched[0]

        for (i in 1 until fullText.length) {
            if (isMatched[i] != currentHighlight) {
                segments.add(Pair(fullText.substring(currentStart, i), currentHighlight))
                currentStart = i
                currentHighlight = isMatched[i]
            }
        }
        segments.add(Pair(fullText.substring(currentStart), currentHighlight))

        return segments
    }
}
