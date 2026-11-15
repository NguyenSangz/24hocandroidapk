package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object SelectGrade : Screen("select_grade")
    data object Home : Screen("home")
    data object ChapterList : Screen("chapter_list/{subjectId}") {
        fun createRoute(subjectId: String) = "chapter_list/$subjectId"
    }
    data object LessonDetail : Screen("lesson_detail/{lessonId}") {
        fun createRoute(lessonId: String) = "lesson_detail/$lessonId"
    }
    data object PracticeQuiz : Screen("practice_quiz/{lessonId}/{subjectId}?count={count}&difficulty={difficulty}&chapterId={chapterId}") {
        fun createRoute(
            lessonId: String = "none",
            subjectId: String = "none",
            count: Int = 10,
            difficulty: String = "MIXED",
            chapterId: String = "all"
        ) = "practice_quiz/$lessonId/$subjectId?count=$count&difficulty=$difficulty&chapterId=$chapterId"
    }
    data object ExamTaking : Screen("exam_taking/{subjectId}/{title}") {
        fun createRoute(subjectId: String = "all", title: String = "Đề tổng hợp") = "exam_taking/$subjectId/$title"
    }
    data object FlashcardStudy : Screen("flashcard_study")
    data object WrongBank : Screen("wrong_bank")
    data object Search : Screen("search?query={query}") {
        fun createRoute(query: String = "") = "search?query=$query"
    }
    data object ReviewSession : Screen("review_session?subjectId={subjectId}&chapterId={chapterId}&count={count}&questionIds={questionIds}&title={title}") {
        fun createRoute(
            subjectId: String = "none",
            chapterId: String = "all",
            count: Int = 10,
            questionIds: String = "none",
            title: String = "Ôn tập câu sai"
        ) = "review_session?subjectId=$subjectId&chapterId=$chapterId&count=$count&questionIds=$questionIds&title=$title"
    }

    // AI Tutor
    data object AiTutor : Screen("ai_tutor")

    // F14: Các công cụ học tập môn học
    data object ToolsHub : Screen("tools_hub")
    data object ScientificCalculator : Screen("tool_calculator")
    data object GraphPlotter : Screen("tool_graph_plotter")
    data object PeriodicTable : Screen("tool_periodic_table")
    data object HistoryTimeline : Screen("tool_history_timeline")
    data object GeoMap : Screen("tool_geo_map")
    data object EnglishDictionary : Screen("tool_dictionary")
    data object IrregularVerbs : Screen("tool_irregular_verbs")

    // F15: Cài đặt và hoàn thiện
    data object Settings : Screen("settings")
    data object Backup : Screen("backup")
    data object Restore : Screen("restore")
    data object PdfReport : Screen("pdf_report")
    data object About : Screen("about")

    // F16: Tài khoản, đồng bộ và pháp lý
    data object Account : Screen("account")
    data object PrivacyPolicy : Screen("privacy_policy")
    data object DemoLogin : Screen("demo_login")
    data object WelcomeAuth : Screen("welcome_auth")
    data object ParentalConsent : Screen("parental_consent")
}
