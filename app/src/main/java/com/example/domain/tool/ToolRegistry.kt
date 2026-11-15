package com.example.domain.tool

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Translate
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Định nghĩa một công cụ môn học (F14 - Khung chung).
 *
 * @param id Định danh duy nhất của công cụ (ví dụ: "calculator", "periodic_table")
 * @param name Tên hiển thị của công cụ (ví dụ: "Máy tính khoa học")
 * @param shortDescription Mô tả ngắn gọn tính năng của công cụ
 * @param iconName Tên định danh biểu tượng
 * @param colorHex Mã màu chủ đạo (dạng hex "#RRGGBB")
 * @param relatedSubjectIds Danh sách id các môn học liên quan (toán, hóa, khtn, lịch sử, ...)
 * @param relatedPhanMon Danh sách phân môn liên quan (ví dụ: "hoa", "lichsu", "diali")
 * @param route Đường dẫn màn hình Jetpack Navigation
 * @param tags Từ khóa phục vụ tìm kiếm toàn ứng dụng (F13)
 */
data class SubjectToolItem(
    val id: String,
    val name: String,
    val shortDescription: String,
    val iconName: String,
    val colorHex: String,
    val relatedSubjectIds: List<String>,
    val relatedPhanMon: List<String> = emptyList(),
    val route: String,
    val tags: List<String> = emptyList()
) {
    /**
     * Lấy biểu tượng Material Vector tương ứng theo iconName.
     */
    fun getVectorIcon(): ImageVector {
        return when (iconName) {
            "calculate" -> Icons.Default.Calculate
            "show_chart" -> Icons.Default.ShowChart
            "science" -> Icons.Default.Science
            "history_edu" -> Icons.Default.HistoryEdu
            "public", "map" -> Icons.Default.Public
            "translate" -> Icons.Default.Translate
            "spellcheck" -> Icons.Default.Spellcheck
            "psychology", "auto_awesome" -> Icons.Default.AutoAwesome
            else -> Icons.Default.MenuBook
        }
    }
}

/**
 * Kho đăng ký trung tâm cho toàn bộ 7 công cụ môn học (ToolRegistry).
 * Cho phép bổ sung, truy vấn theo môn học, phân môn và tìm kiếm mà không cần sửa giao diện.
 */
object ToolRegistry {

    val CALCULATOR = SubjectToolItem(
        id = "calculator",
        name = "Máy tính khoa học",
        shortDescription = "Phép tính phân số, lũy thừa, căn, lượng giác, logarit, lưu Ans",
        iconName = "calculate",
        colorHex = "#2563EB",
        relatedSubjectIds = listOf("math", "physics", "chemistry", "khtn"),
        relatedPhanMon = listOf("ly", "hoa"),
        route = "tool_calculator",
        tags = listOf("máy tính", "casio", "toán", "tính toán", "phân số", "căn", "lũy thừa", "sin", "cos", "tan", "log", "bậc hai")
    )

    val GRAPH_PLOTTER = SubjectToolItem(
        id = "graph_plotter",
        name = "Vẽ đồ thị hàm số",
        shortDescription = "Khảo sát và vẽ tối đa 3 đồ thị y = f(x), hàm bậc nhất, parabol",
        iconName = "show_chart",
        colorHex = "#0284C7",
        relatedSubjectIds = listOf("math"),
        relatedPhanMon = emptyList(),
        route = "tool_graph_plotter",
        tags = listOf("vẽ đồ thị", "hàm số", "parabol", "toán", "tọa độ", "hệ số", "bậc nhất", "khảo sát hàm số")
    )

    val PERIODIC_TABLE = SubjectToolItem(
        id = "periodic_table",
        name = "Bảng tuần hoàn hóa học",
        shortDescription = "118 nguyên tố hóa học, số hiệu, khối lượng, electron, đố vui",
        iconName = "science",
        colorHex = "#7C3AED",
        relatedSubjectIds = listOf("chemistry", "khtn"),
        relatedPhanMon = listOf("hoa"),
        route = "tool_periodic_table",
        tags = listOf("bảng tuần hoàn", "hóa học", "nguyên tố", "mendeleev", "nguyên tử", "khtn", "hóa", "kim loại", "phi kim")
    )

    val HISTORY_TIMELINE = SubjectToolItem(
        id = "history_timeline",
        name = "Dòng thời gian lịch sử",
        shortDescription = "Trục niên biểu lịch sử Việt Nam và thế giới lớp 6-9 kèm liên kết bài học",
        iconName = "history_edu",
        colorHex = "#DC2626",
        relatedSubjectIds = listOf("history", "lich_su_dia_li"),
        relatedPhanMon = listOf("lichsu"),
        route = "tool_history_timeline",
        tags = listOf("dòng thời gian", "lịch sử", "sự kiện", "mốc thời gian", "niên biểu", "khởi nghĩa", "chiến thắng")
    )

    val GEO_MAP = SubjectToolItem(
        id = "geo_map",
        name = "Bản đồ địa lí",
        shortDescription = "Bản đồ Việt Nam (Hoàng Sa, Trường Sa, Biển Đông) & thế giới, đố địa lí",
        iconName = "public",
        colorHex = "#0D9488",
        relatedSubjectIds = listOf("geography", "lich_su_dia_li"),
        relatedPhanMon = listOf("diali"),
        route = "tool_geo_map",
        tags = listOf("bản đồ", "địa lí", "hoàng sa", "trường sa", "biển đông", "tỉnh thành", "địa lý", "thế giới", "châu lục")
    )

    val DICTIONARY = SubjectToolItem(
        id = "dictionary",
        name = "Từ điển Anh - Việt",
        shortDescription = "Tra cứu từ vựng THCS offline, phiên âm IPA, nghe phát âm và thêm Flashcard",
        iconName = "translate",
        colorHex = "#059669",
        relatedSubjectIds = listOf("english"),
        relatedPhanMon = emptyList(),
        route = "tool_dictionary",
        tags = listOf("từ điển", "tiếng anh", "anh việt", "tra từ", "phát âm", "flashcard", "phiên âm", "ipa")
    )

    val IRREGULAR_VERBS = SubjectToolItem(
        id = "irregular_verbs",
        name = "Động từ bất quy tắc",
        shortDescription = "Bảng động từ bất quy tắc THCS (V1, V2, V3), nghe đọc và luyện tập",
        iconName = "spellcheck",
        colorHex = "#DB2777",
        relatedSubjectIds = listOf("english"),
        relatedPhanMon = emptyList(),
        route = "tool_irregular_verbs",
        tags = listOf("động từ bất quy tắc", "irregular verbs", "v1 v2 v3", "tiếng anh", "ngữ pháp", "quá khứ")
    )

    val AI_TUTOR = SubjectToolItem(
        id = "ai_tutor",
        name = "Trợ lý Học tập AI",
        shortDescription = "Hỏi đáp kiến thức, giải bài tập từng bước, sửa lỗi Tiếng Anh & dàn ý Ngữ văn",
        iconName = "auto_awesome",
        colorHex = "#8B5CF6",
        relatedSubjectIds = listOf("math", "literature", "english", "chemistry", "physics", "khtn", "history", "geography"),
        relatedPhanMon = emptyList(),
        route = "ai_tutor",
        tags = listOf("ai", "trợ lý", "học tập", "hỏi đáp", "giải bài tập", "gia sư", "thcs", "thông minh")
    )

    /**
     * Danh sách toàn bộ các công cụ đã được đăng ký.
     */
    val ALL_TOOLS: List<SubjectToolItem> = listOf(
        AI_TUTOR,
        CALCULATOR,
        GRAPH_PLOTTER,
        PERIODIC_TABLE,
        HISTORY_TIMELINE,
        GEO_MAP,
        DICTIONARY,
        IRREGULAR_VERBS
    )

    /**
     * Lấy danh sách công cụ theo ID môn học và phân môn (nếu có).
     */
    fun getToolsForSubject(subjectId: String, phanMon: String = ""): List<SubjectToolItem> {
        val normalizedSub = subjectId.trim().lowercase()
        val normalizedPm = phanMon.trim().lowercase()

        return ALL_TOOLS.filter { tool ->
            val matchSubject = tool.relatedSubjectIds.any { it.equals(normalizedSub, ignoreCase = true) }
            if (!matchSubject) return@filter false

            // Nếu là môn tích hợp có phân môn cụ thể
            if (normalizedPm.isNotBlank() && tool.relatedPhanMon.isNotEmpty()) {
                tool.relatedPhanMon.any { it.equals(normalizedPm, ignoreCase = true) }
            } else {
                true
            }
        }
    }

    /**
     * Tìm kiếm công cụ theo từ khóa tên hoặc thẻ.
     */
    fun searchTools(query: String): List<SubjectToolItem> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return ALL_TOOLS
        return ALL_TOOLS.filter { tool ->
            tool.name.lowercase().contains(q) ||
                tool.shortDescription.lowercase().contains(q) ||
                tool.tags.any { it.lowercase().contains(q) }
        }
    }

    /**
     * Tìm công cụ theo id.
     */
    fun getToolById(toolId: String): SubjectToolItem? {
        return ALL_TOOLS.find { it.id.equals(toolId, ignoreCase = true) }
    }
}
