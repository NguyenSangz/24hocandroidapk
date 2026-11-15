package com.example.domain.gamification

/**
 * Tệp cấu hình tập trung toàn bộ tham số của Hệ thống Gamification (F10).
 * Cho phép dễ dàng tinh chỉnh số XP, cấp độ, giới hạn chống gian lận và streak.
 */
object GamificationConfig {
    // === 1. QUY TẮC CỘNG XP ===
    const val XP_LESSON_COMPLETED = 10                  // Hoàn thành bài học lý thuyết lần đầu
    const val XP_PRACTICE_CORRECT = 2                   // Trả lời đúng 1 câu hỏi luyện tập
    const val XP_PRACTICE_HINT_PENALTY = 1             // Trừ XP nếu sử dụng gợi ý cho câu đó
    const val XP_PRACTICE_SESSION_BONUS = 5             // Thưởng hoàn thành trọn vẹn phiên luyện tập
    const val XP_FLASHCARD_REVIEW = 1                   // Ôn tập 1 thẻ flashcard
    const val XP_WRONG_QUESTION_MASTERED = 3            // Chuyển 1 câu sai sang trạng thái Đã thuộc
    const val XP_TEST_SCORE_MULTIPLIER = 5              // Hệ số nhân điểm bài thi thang 10 (vd: 8.5 đ * 5 = 43 XP)
    const val XP_TEST_HIGH_SCORE_BONUS = 10             // Thưởng thêm nếu bài thi đạt từ 8.0 điểm trở lên
    const val TEST_HIGH_SCORE_THRESHOLD = 8.0f          // Ngưỡng điểm để nhận thưởng thêm

    // === 2. CHỐNG GIAN LẬN & GIỚI HẠN ===
    const val DAILY_MAX_XP = 300                        // Giới hạn XP tối đa kiếm được trong một ngày dương lịch
    const val MAX_REPEATS_PER_QUESTION_PER_DAY = 3      // Tối đa số lần làm lại cùng một câu trong ngày được cộng XP

    // === 3. CẤP ĐỘ (LEVEL) ===
    const val MAX_LEVEL = 50                            // Cấp độ tối đa
    const val LEVEL_EXPONENT = 1.5                      // Số mũ đường cong tăng trưởng XP
    const val LEVEL_BASE_MULTIPLIER = 100.0             // Hệ số cơ sở: 100 * (level - 1)^1.5

    // === 4. STREAK & ĐÓNG BĂNG STREAK (STREAK FREEZE) ===
    const val MAX_STREAK_FREEZES = 2                    // Tối đa 2 lượt tích lũy đóng băng chuỗi
    const val STREAK_FREEZE_COST_XP = 200               // Chi phí XP để đổi 1 lượt đóng băng chuỗi
    const val STREAK_FREEZE_REWARD_MILESTONE = 7        // Thưởng 1 lượt đóng băng khi đạt mốc chuỗi 7 ngày liên tiếp

    // Điều kiện tối thiểu để 1 ngày được tính là "Có học":
    // Đạt ít nhất một trong các tiêu chí:
    const val MIN_DAILY_LESSONS = 1                     // Hoàn thành ít nhất 1 bài học lý thuyết
    const val MIN_DAILY_PRACTICE_QUESTIONS = 5          // Hoặc trả lời tối thiểu 5 câu luyện tập
    const val MIN_DAILY_FLASHCARDS = 10                 // Hoặc ôn tối thiểu 10 thẻ flashcard
    const val MIN_DAILY_TESTS = 1                       // Hoặc hoàn thành tối thiểu 1 bài kiểm tra
}
