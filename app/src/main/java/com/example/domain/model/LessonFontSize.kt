package com.example.domain.model

/**
 * Tùy chọn kích cỡ chữ cho bài học
 */
enum class LessonFontSize(val key: String, val displayName: String, val scaleFactor: Float) {
    SMALL("small", "Nhỏ", 0.85f),
    MEDIUM("medium", "Vừa", 1.0f),
    LARGE("large", "Lớn", 1.2f);

    companion object {
        fun fromKey(key: String): LessonFontSize =
            entries.find { it.key.equals(key, ignoreCase = true) } ?: MEDIUM
    }
}
