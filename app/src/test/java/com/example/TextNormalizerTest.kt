package com.example

import com.example.domain.search.TextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.Normalizer

class TextNormalizerTest {

    @Test
    fun testRemoveDiacritics_handlesVietnameseAccentsAndD() {
        assertEquals("phan so", TextNormalizer.removeDiacritics("phân số"))
        assertEquals("tap hop", TextNormalizer.removeDiacritics("tập hợp"))
        assertEquals("Dai so", TextNormalizer.removeDiacritics("Đại số"))
        assertEquals("duong thang", TextNormalizer.removeDiacritics("đường thẳng"))
        assertEquals("Hinh hoc truc quan", TextNormalizer.removeDiacritics("Hình học trực quan"))
    }

    @Test
    fun testNfcAndNfdEquivalence() {
        val nfc = "tiếng việt"
        val nfd = Normalizer.normalize(nfc, Normalizer.Form.NFD)

        // Văn bản đầu vào dù là tổ hợp NFD hay dựng sẵn NFC đều cho kết quả không dấu đồng nhất
        val unaccentedNfc = TextNormalizer.toUnaccentedSearchTerm(nfc)
        val unaccentedNfd = TextNormalizer.toUnaccentedSearchTerm(nfd)

        assertEquals("tieng viet", unaccentedNfc)
        assertEquals("tieng viet", unaccentedNfd)
        assertEquals(unaccentedNfc, unaccentedNfd)
    }

    @Test
    fun testStripLatex_preservesTextAndNumbers() {
        val formula1 = "\\frac{a}{b} là một phân số"
        val stripped1 = TextNormalizer.stripLatex(formula1)
        assertTrue(stripped1.contains("a b"))
        assertTrue(stripped1.contains("phân số"))
        assertFalse(stripped1.contains("\\frac"))

        val formula2 = "\\sqrt{x + 1} = 5"
        val stripped2 = TextNormalizer.stripLatex(formula2)
        assertEquals("x + 1 = 5", stripped2)

        val formula3 = "\$S = a \\cdot b\$ diện tích hình chữ nhật"
        val stripped3 = TextNormalizer.stripLatex(formula3)
        assertTrue(stripped3.contains("S = a b"))
        assertTrue(stripped3.contains("diện tích hình chữ nhật"))
        assertFalse(stripped3.contains("$"))
    }

    @Test
    fun testExtractSearchTokens_sanitizesSpecialCharactersAndSqlKeywords() {
        val dirtyInput = "  \"phân số\" * (tam giác) - 1/2 ^2 AND OR NOT NEAR :  "
        val tokens = TextNormalizer.extractSearchTokens(dirtyInput)

        assertTrue(tokens.contains("phân"))
        assertTrue(tokens.contains("số"))
        assertTrue(tokens.contains("tam"))
        assertTrue(tokens.contains("giác"))
        assertTrue(tokens.contains("1"))
        assertTrue(tokens.contains("2"))

        assertFalse(tokens.contains("AND"))
        assertFalse(tokens.contains("OR"))
        assertFalse(tokens.contains("NOT"))
        assertFalse(tokens.contains("NEAR"))
        assertFalse(tokens.contains("\""))
        assertFalse(tokens.contains("*"))
    }

    @Test
    fun testHighlightText_marksMatchingTokensAccurately() {
        val text = "Tập hợp các số tự nhiên và phân số"
        val query = "tap hop phan so"

        val segments = TextNormalizer.highlightText(text, query)
        assertTrue(segments.isNotEmpty())

        val highlightedWords = segments.filter { it.second }.map { it.first.trim() }
        assertTrue(highlightedWords.any { it.contains("Tập", ignoreCase = true) })
        assertTrue(highlightedWords.any { it.contains("hợp", ignoreCase = true) })
        assertTrue(highlightedWords.any { it.contains("phân", ignoreCase = true) })
        assertTrue(highlightedWords.any { it.contains("số", ignoreCase = true) })
    }
}
