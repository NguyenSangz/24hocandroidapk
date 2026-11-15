package com.example.domain.model

/**
 * Mức độ nhận thức / độ khó câu hỏi theo chuẩn đánh giá năng lực GDPT 2018
 */
enum class Difficulty(val level: Int, val displayName: String) {
    RECOGNITION(1, "Nhận biết"),
    COMPREHENSION(2, "Thông hiểu"),
    APPLICATION(3, "Vận dụng"),
    HIGH_APPLICATION(4, "Vận dụng cao");

    val vietnameseTitle: String get() = displayName

    companion object {
        fun fromLevel(level: Int): Difficulty =
            entries.find { it.level == level } ?: RECOGNITION
    }
}
