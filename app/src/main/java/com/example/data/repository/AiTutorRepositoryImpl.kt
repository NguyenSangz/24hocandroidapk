package com.example.data.repository

import com.example.domain.model.AiChatMessage
import com.example.domain.model.AiMessageSender
import com.example.domain.repository.AiTutorRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AiTutorRepositoryImpl : AiTutorRepository {

    private val _messages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                id = UUID.randomUUID().toString(),
                sender = AiMessageSender.TUTOR,
                text = "Xin chào! Mình là **Trợ lý Học tập AI** cho học sinh THCS. Mình có thể giúp bạn giải Toán từng bước, phân tích bài Ngữ Văn, sửa lỗi Tiếng Anh, giải thích Khoa học tự nhiên & Lịch sử - Địa lý.\n\nHãy chọn môn học hoặc nhập câu hỏi bên dưới nhé!",
                subjectId = "general",
                timestamp = System.currentTimeMillis()
            )
        )
    )

    override fun getChatMessages(): Flow<List<AiChatMessage>> = _messages.asStateFlow()

    override suspend fun sendMessage(userText: String, subjectId: String, selectedGrade: Int): AiChatMessage {
        val userMsg = AiChatMessage(
            id = UUID.randomUUID().toString(),
            sender = AiMessageSender.USER,
            text = userText.trim(),
            subjectId = subjectId,
            timestamp = System.currentTimeMillis()
        )

        // Add user message to list
        _messages.value = _messages.value + userMsg

        // Fast responsive delay
        delay(100)

        // Generate response using localized THCS AI engine
        val responseText = generateSmartResponse(userText.trim(), subjectId, selectedGrade)
        val suggestedActions = buildSuggestedActions(userText.trim(), subjectId)

        val aiMsg = AiChatMessage(
            id = UUID.randomUUID().toString(),
            sender = AiMessageSender.TUTOR,
            text = responseText,
            subjectId = subjectId,
            timestamp = System.currentTimeMillis(),
            suggestedActions = suggestedActions
        )

        _messages.value = _messages.value + aiMsg
        return aiMsg
    }

    override suspend fun clearHistory() {
        _messages.value = listOf(
            AiChatMessage(
                id = UUID.randomUUID().toString(),
                sender = AiMessageSender.TUTOR,
                text = "Đã làm mới cuộc hội thoại! Bạn muốn mình giúp gì tiếp theo?",
                subjectId = "general",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private fun generateSmartResponse(query: String, subjectId: String, grade: Int): String {
        val qLower = query.lowercase()

        // 1. MATH (TOÁN HỌC)
        if (subjectId == "math" || qLower.contains("toán") || qLower.contains("phương trình") || qLower.contains("bậc hai") || qLower.contains("phân số") || qLower.contains("tính")) {
            return when {
                qLower.contains("bậc hai") || qLower.contains("x^2") || qLower.contains("phương trình") -> {
                    """
                    ### 📐 Hướng dẫn Giải Phương trình Bậc hai (Lớp $grade)
                    
                    **Dạng tổng quát:** $$ ax^2 + bx + c = 0 $$ (với $$ a \neq 0 $$)
                    
                    **Các bước giải:**
                    1. **Tính Biệt thức Delta ($\Delta$):**
                       $$ \Delta = b^2 - 4ac $$
                    2. **Xét các trường hợp:**
                       - Nếu $$ \Delta < 0 $$: Phương trình vô nghiệm.
                       - Nếu $$ \Delta = 0 $$: Phương trình có nghiệm kép: $$ x_1 = x_2 = -\frac{b}{2a} $$
                       - Nếu $$ \Delta > 0 $$: Phương trình có 2 nghiệm phân biệt:
                         $$ x_1 = \frac{-b + \sqrt{\Delta}}{2a}, \quad x_2 = \frac{-b - \sqrt{\Delta}}{2a} $$
                    
                    💡 **Mẹo:** Bạn có thể mở công cụ **Máy tính khoa học** hoặc **Vẽ đồ thị** trong tab Công cụ để hỗ trợ tính nhanh nhé!
                    """.trimIndent()
                }
                qLower.contains("định lý pythagoras") || qLower.contains("pithago") || qLower.contains("tam giác vuông") -> {
                    """
                    ### 📐 Định lý Pythagoras (Hình học Lớp $grade)
                    
                    **Phát biểu:** Trong một tam giác vuông, bình phương cạnh huyền bằng tổng bình phương hai cạnh góc vuông.
                    
                    **Công thức:**
                    $$ c^2 = a^2 + b^2 $$
                    Trong đó:
                    - $$ c $$: Độ dài cạnh huyền.
                    - $$ a, b $$: Độ dài hai cạnh góc vuông.
                    
                    **Ví dụ:** Cho tam giác vuông có $$ a = 3\text{ cm} $$, $$ b = 4\text{ cm} $$. Tính cạnh huyền $$ c $$:
                    $$ c = \sqrt{a^2 + b^2} = \sqrt{3^2 + 4^2} = \sqrt{9 + 16} = \sqrt{25} = 5\text{ cm} $$
                    """.trimIndent()
                }
                else -> {
                    """
                    ### 💡 Hướng dẫn Học & Giải Toán Lớp $grade
                    
                    Để giải bài toán "$query", mình gợi ý quy trình 3 bước:
                    
                    1. **Phân tích đề bài:** Xác định các dữ kiện đã cho và yêu cầu cần tìm.
                    2. **Lựa chọn phương pháp:** Sử dụng công thức $$ a^2 - b^2 = (a-b)(a+b) $$ hoặc các định lý tương ứng trong chương trình Toán Lớp $grade.
                    3. **Trình bày từng bước:** Viết rõ lập luận, biến đổi đại số và kiểm tra lại điều kiện nghiệm.
                    
                    ✨ Bạn có thể sử dụng công cụ **Máy tính khoa học** trong mục **Công cụ** để tính toán chính xác hơn!
                    """.trimIndent()
                }
            }
        }

        // 2. LITERATURE (NGỮ VĂN)
        if (subjectId == "literature" || qLower.contains("văn") || qLower.contains("bài văn") || qLower.contains("dàn ý") || qLower.contains("tác phẩm") || qLower.contains("ngữ văn")) {
            return """
            ### 📝 Dàn ý & Phân tích Ngữ Văn Lớp $grade
            
            **Chủ đề:** $query
            
            **1. Mở bài:**
            - Giới thiệu tác giả, tác phẩm / đề tài phân tích.
            - Nêu nhận định chung hoặc cảm xúc chủ đạo.
            
            **2. Thân bài:**
            - **Luận điểm 1:** Phân tích bối cảnh, hoàn cảnh ra đời và hình ảnh trung tâm.
            - **Luận điểm 2:** Khai thác các biện pháp nghệ thuật tiêu biểu (so sánh, ẩn dụ, nhân hóa, điệp ngữ).
            - **Luận điểm 3:** Đánh giá nội dung tư tưởng, tình cảm của tác giả truyền tải.
            
            **3. Kết bài:**
            - Khẳng định lại giá trị nghệ thuật và nội dung của bài.
            - Rút ra bài học cho bản thân hoặc liên hệ thực tế.
            
            💡 *Lời khuyên:* Hãy kết hợp dẫn chứng cụ thể từ câu thơ hoặc trích đoạn để bài viết đạt điểm cao!
            """.trimIndent()
        }

        // 3. ENGLISH (TIẾNG ANH)
        if (subjectId == "english" || qLower.contains("tiếng anh") || qLower.contains("english") || qLower.contains("từ vựng") || qLower.contains("ngữ pháp") || qLower.contains("grammar")) {
            return """
            ### 🇬🇧 Hướng dẫn Tiếng Anh THCS (Lớp $grade)
            
            **Yêu cầu:** $query
            
            **1. Từ vựng & Cấu trúc ngữ pháp trọng tâm:**
            - **Present Perfect (Thì Hiện tại Hoàn thành):**
              $$\text{Subject} + \text{have/has} + \text{V3/V-ed}$$
              *Example:* She has lived in Hanoi for 3 years.
            
            - **Conditional Sentence Type 1 (Câu điều kiện loại 1):**
              $$\text{If} + \text{S} + \text{V(present)}, \text{S} + \text{will/can} + \text{V(bare)}$$
            
            **2. Mẹo luyện tập:**
            - Mở công cụ **Từ điển Anh - Việt** hoặc **Động từ bất quy tắc** trong ứng dụng để tra từ và nghe phát âm IPA nhé!
            """.trimIndent()
        }

        // 4. KHTN (KHOA HỌC TỰ NHIÊN - LÝ, HÓA, SINH)
        if (subjectId == "khtn" || qLower.contains("hóa") || qLower.contains("lý") || qLower.contains("vật lý") || qLower.contains("sinh") || qLower.contains("khtn")) {
            return """
            ### 🔬 Giải đáp Khoa học tự nhiên (Lớp $grade)
            
            **Chủ đề:** $query
            
            **1. Nguyên lý / Kiến thức cơ bản:**
            - Khối lượng riêng: $$ D = \frac{m}{V} $$ (trong đó $$ m $$ là khối lượng kg, $$ V $$ là thể tích m³).
            - Phương trình hóa học cơ bản:
              $$ 2\text{H}_2 + \text{O}_2 \xrightarrow{t^\circ} 2\text{H}_2\text{O} $$
            
            **2. Các bước giải bài tập KHTN:**
            1. Đổi đơn vị về chuẩn SI (kg, m, s, mol).
            2. Viết công thức / phương trình phản ứng.
            3. Thay số và kết luận kèm đơn vị.
            
            💡 Bạn có thể tra cứu **Bảng tuần hoàn 118 nguyên tố hóa học** trong mục **Công cụ môn học**!
            """.trimIndent()
        }

        // 5. GENERAL RESPONSES & TUTOR HELP
        return """
        ### 🤖 Trợ Lý Học Tập AI - Học Tốt THCS (Lớp $grade)
        
        **Câu hỏi của bạn:** "$query"
        
        Mình đã tiếp nhận câu hỏi của bạn! Dưới đây là kiến thức gợi ý:
        
        - 📌 **Tóm tắt:** Tập trung vào các khái niệm trọng tâm trong sách giáo khoa Lớp $grade.
        - 💡 **Phương pháp ghi nhớ:** Sử dụng sơ đồ tư duy hoặc thẻ nhớ Flashcard để ôn tập hiệu quả.
        - 🎯 **Luyện tập:** Hãy thử làm các câu hỏi trắc nghiệm tương tự trong phần **Luyện tập** để củng cố kiến thức!
        
        Nếu bạn muốn mình giải thích chi tiết hơn ở môn học nào (Toán, Ngữ Văn, Tiếng Anh, KHTN), hãy chọn tab môn học tương ứng nhé!
        """.trimIndent()
    }

    private fun buildSuggestedActions(userText: String, subjectId: String): List<String> {
        val actions = mutableListOf<String>()
        val qLower = userText.lowercase()

        if (subjectId == "math" || qLower.contains("toán")) {
            actions.add("Mở Máy tính khoa học")
            actions.add("Mở Vẽ đồ thị")
        }
        if (subjectId == "english" || qLower.contains("tiếng anh")) {
            actions.add("Tra từ điển Anh - Việt")
            actions.add("Bảng Động từ bất quy tắc")
        }
        if (subjectId == "khtn" || qLower.contains("hóa")) {
            actions.add("Bảng tuần hoàn Hóa học")
        }

        actions.add("Tạo Flashcard cho bài học")
        actions.add("Luyện trắc nghiệm môn này")
        return actions
    }
}
