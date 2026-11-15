package com.example.domain.evaluator

/**
 * Cấu hình ngưỡng đánh giá điểm mạnh, điểm yếu và xu hướng (F11).
 * Tập trung tại một file, dễ dàng điều chỉnh cấu hình theo tiêu chuẩn sư phạm GDPT 2018.
 */
object StatsThresholdConfig {
    /** Số câu tối thiểu để kết luận Điểm mạnh / Điểm yếu (tránh kết luận thiếu dữ liệu) */
    const val MIN_QUESTIONS_EVALUATION: Int = 10

    /** Ngưỡng tỉ lệ đúng để coi là Điểm mạnh (>= 80%) */
    const val STRONG_ACCURACY_THRESHOLD: Float = 0.80f

    /** Ngưỡng tỉ lệ đúng để coi là Điểm yếu (< 60%) */
    const val WEAK_ACCURACY_THRESHOLD: Float = 0.60f

    /** Số câu gần nhất áp dụng trọng số cao hơn */
    const val RECENT_WEIGHTED_COUNT: Int = 5

    /** Hệ số nhân trọng số cho câu mới nhất */
    const val RECENT_WEIGHT_MULTIPLIER: Float = 2.0f

    /** Hệ số nhân trọng số cho câu cũ hơn */
    const val BASE_WEIGHT_MULTIPLIER: Float = 1.0f

    /** Thời lượng tối thiểu (giây) của một phiên học để được ghi nhận vào database */
    const val MIN_VALID_SESSION_SECONDS: Int = 5

    /** Giới hạn thời gian không tương tác (giây) - 30 phút. Quá mốc này sẽ ngừng tính thời gian "treo máy" */
    const val MAX_INACTIVE_LIMIT_SECONDS: Int = 1800
}
