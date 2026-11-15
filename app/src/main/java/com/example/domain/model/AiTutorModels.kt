package com.example.domain.model

enum class AiMessageSender {
    USER,
    TUTOR,
    SYSTEM
}

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: AiMessageSender,
    val text: String,
    val subjectId: String = "general",
    val timestamp: Long = System.currentTimeMillis(),
    val isThinking: Boolean = false,
    val isError: Boolean = false,
    val suggestedActions: List<String> = emptyList()
)

data class AiSubjectOption(
    val id: String,
    val name: String,
    val iconName: String,
    val description: String
)

object AiSubjectRegistry {
    val ALL_SUBJECTS = listOf(
        AiSubjectOption("general", "Chung / Tất cả", "psychology", "Trợ lý học tập tổng hợp THCS"),
        AiSubjectOption("math", "Toán học", "calculate", "Giải toán, công thức, vẽ hình & tính toán"),
        AiSubjectOption("literature", "Ngữ văn", "menu_book", "Phân tích tác phẩm, dàn ý & sửa văn"),
        AiSubjectOption("english", "Tiếng Anh", "translate", "Ngữ pháp, từ vựng & sửa lỗi câu"),
        AiSubjectOption("khtn", "Khoa học tự nhiên", "science", "Vật lý, Hóa học & Sinh học THCS"),
        AiSubjectOption("history_geo", "Lịch sử & Địa lý", "public", "Sự kiện lịch sử & bản đồ địa lý")
    )
}

data class AiPromptTemplate(
    val id: String,
    val subjectId: String,
    val label: String,
    val promptText: String
)

object AiPromptTemplates {
    val DEFAULT_PROMPTS = listOf(
        AiPromptTemplate("p1", "math", "Giải bài tập Toán", "Hãy giúp mình giải từng bước chi tiết bài toán sau: "),
        AiPromptTemplate("p2", "math", "Giải thích công thức", "Hãy giải thích ý nghĩa và cách ứng dụng công thức: "),
        AiPromptTemplate("p3", "literature", "Lập dàn ý bài văn", "Hãy lập dàn ý chi tiết cho đề văn sau: "),
        AiPromptTemplate("p4", "literature", "Phân tích tác phẩm", "Tóm tắt và phân tích các ý chính của tác phẩm: "),
        AiPromptTemplate("p5", "english", "Sửa lỗi Tiếng Anh", "Hãy sửa lỗi ngữ pháp và viết lại câu tiếng Anh sau cho tự nhiên: "),
        AiPromptTemplate("p6", "english", "Giải thích từ vựng", "Giải thích nghĩa, phiên âm IPA, loại từ và cho 3 ví dụ cho từ: "),
        AiPromptTemplate("p7", "khtn", "Cân bằng phương trình Hóa", "Hãy cân bằng và giải thích phản ứng hóa học sau: "),
        AiPromptTemplate("p8", "khtn", "Giải thích hiện tượng Lý", "Giải thích bản chất vật lý của hiện tượng: "),
        AiPromptTemplate("p9", "general", "Tạo bài tập tự luyện", "Hãy tạo 3 câu hỏi trắc nghiệm tự luyện kèm đáp án giải thích về chủ đề: ")
    )
}
