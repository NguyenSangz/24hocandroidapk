package com.example.push

import com.example.data.push.PushPayloadParser
import com.example.domain.model.PushNotificationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * F20a – Kiểm tra payload FCM chỉ chấp nhận đúng whitelist.
 * Bảo đảm không có dữ liệu cá nhân nào (tên, điểm, email) đi qua FCM.
 */
class PushPayloadParserTest {

    @Test
    fun parse_acceptsValidTypeAndContentId() {
        val payload = PushPayloadParser.parse(
            mapOf(
                "type" to "study_reminder",
                "content_id" to "lesson_toan_6_bai_1"
            )
        )
        assertEquals(PushNotificationType.STUDY_REMINDER, payload?.type)
        assertEquals("lesson_toan_6_bai_1", payload?.contentId)
    }

    @Test
    fun parse_acceptsPayloadWithoutContentId() {
        val payload = PushPayloadParser.parse(mapOf("type" to "system"))
        assertEquals(PushNotificationType.SYSTEM, payload?.type)
        assertNull(payload?.contentId)
    }

    @Test
    fun parse_rejectsUnknownType() {
        assertNull(PushPayloadParser.parse(mapOf("type" to "promo_banner")))
        assertNull(PushPayloadParser.parse(mapOf("type" to "")))
        assertNull(PushPayloadParser.parse(emptyMap()))
    }

    @Test
    fun parse_dropsSensitiveExtraKeysCompletely() {
        val raw = mapOf(
            "type" to "assignment_due",
            "content_id" to "assign_42",
            "student_name" to "Nguyễn Văn A",
            "score" to "9.5",
            "email" to "a@example.com",
            "body" to "Bài tập sắp đến hạn"
        )
        val payload = PushPayloadParser.parse(raw)
        assertEquals(PushNotificationType.ASSIGNMENT_DUE, payload?.type)
        assertEquals("assign_42", payload?.contentId)

        // Bản đồ an toàn chỉ giữ 2 khóa cho phép.
        val safe = PushPayloadParser.safeDataMap(raw)
        assertEquals(setOf("type", "content_id"), safe.keys)
        assertTrue(safe.values.none { it.contains("@") || it.contains("Nguyễn") })
    }

    @Test
    fun parse_rejectsUnsafeOrTooLongContentId() {
        assertNull(
            PushPayloadParser.parse(
                mapOf("type" to "new_content", "content_id" to "abc def<script>")
            )
        )
        assertNull(
            PushPayloadParser.parse(
                mapOf("type" to "new_content", "content_id" to "a".repeat(129))
            )
        )
    }
}
