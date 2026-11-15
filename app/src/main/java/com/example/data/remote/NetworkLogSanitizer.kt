package com.example.data.remote

/**
 * Tiện ích ẩn dữ liệu cá nhân nhạy cảm (PII) và token trước khi ghi log chẩn đoán.
 */
object NetworkLogSanitizer {
    fun maskEmail(email: String?): String {
        if (email.isNullOrBlank()) return "***"
        val atIndex = email.indexOf('@')
        if (atIndex <= 1) return "***"
        return "${email.take(1)}***${email.substring(atIndex)}"
    }

    fun maskUid(uid: String?): String {
        if (uid.isNullOrBlank()) return "none"
        return if (uid.length <= 6) "***" else "${uid.take(3)}***${uid.takeLast(3)}"
    }

    fun maskToken(token: String?): String {
        if (token.isNullOrBlank()) return "none"
        return if (token.length <= 8) "***" else "${token.take(4)}***"
    }
}
