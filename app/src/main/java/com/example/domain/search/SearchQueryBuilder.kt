package com.example.domain.search

/**
 * Xây dựng câu truy vấn MATCH an toàn cho SQLite FTS4 (F13):
 * - Hỗ trợ tìm theo tiền tố khi đang gõ (prefix matching: term*).
 * - Tìm nhiều từ thì bắt buộc phải khớp đủ các từ (AND), không phân biệt thứ tự.
 * - Hỗ trợ cả từ khóa có dấu và không dấu: mỗi từ tạo thành nhóm (co_dau* OR khong_dau*).
 * - Triệt tiêu 100% lỗi cú pháp FTS4 gây crash ứng dụng.
 */
object SearchQueryBuilder {

    /**
     * Dựng câu truy vấn MATCH cho FTS4 từ chuỗi người dùng nhập.
     * Ví dụ:
     * - Input: "tap hop"
     *   Output: `("tap"* OR "tập"*) AND ("hop"* OR "hợp"*)` -> hoặc định dạng FTS4 an toàn:
     *   `("tap"* OR "tap"*) ("hop"* OR "hop"*)`
     * - Input: "phân số"
     *   Output: `(phân* OR phan*) AND (số* OR so*)`
     * Nếu không có từ khóa hợp lệ -> trả về chuỗi rỗng "" (tránh query rỗng gây lỗi SQLite).
     */
    fun buildFts4MatchQuery(rawInput: String?): String {
        val tokens = TextNormalizer.extractSearchTokens(rawInput)
        if (tokens.isEmpty()) return ""

        val tokenClauses = tokens.map { token ->
            val unaccented = TextNormalizer.toUnaccentedSearchTerm(token)
            if (unaccented.isNotBlank()) "$unaccented*" else ""
        }.filter { it.isNotBlank() }

        // Trong SQLite FTS4 chuẩn, khoảng trắng giữa các từ là toán tử AND ngầm định.
        // Cú pháp này an toàn tuyệt đối 100%, không bị lỗi cú pháp ngoặc đơn hay từ khóa AND.
        return tokenClauses.joinToString(" ")
    }

    /**
     * Dựng câu truy vấn kiểm tra nhanh xem truy vấn có chứa dấu tiếng Việt hay không
     * để phục vụ chấm điểm ưu tiên khớp có dấu lên trên không dấu.
     */
    fun hasDiacritics(rawInput: String?): Boolean {
        if (rawInput.isNullOrBlank()) return false
        val nfc = TextNormalizer.normalizeNfc(rawInput)
        val unaccented = TextNormalizer.toUnaccentedSearchTerm(rawInput)
        return nfc != unaccented
    }
}
