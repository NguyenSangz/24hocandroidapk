package com.example

import com.example.domain.search.SearchQueryBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryBuilderTest {

    @Test
    fun testBuildFts4MatchQuery_singleWord() {
        val query = "toan"
        val matchClause = SearchQueryBuilder.buildFts4MatchQuery(query)
        assertEquals("toan*", matchClause)
    }

    @Test
    fun testBuildFts4MatchQuery_accentedWordGeneratesDualClause() {
        val query = "phân"
        val matchClause = SearchQueryBuilder.buildFts4MatchQuery(query)
        assertEquals("phan*", matchClause)
    }

    @Test
    fun testBuildFts4MatchQuery_multiWordHasAndLogic() {
        val query = "tập hợp"
        val matchClause = SearchQueryBuilder.buildFts4MatchQuery(query)
        assertEquals("tap* hop*", matchClause)
    }

    @Test
    fun testBuildFts4MatchQuery_dangerousCharactersAreRemoved() {
        val dangerous = "SELECT * FROM \"users\"; -- * ^ : ( ) AND"
        val matchClause = SearchQueryBuilder.buildFts4MatchQuery(dangerous)
        assertFalse(matchClause.contains("\""))
        assertFalse(matchClause.contains(";"))
        assertFalse(matchClause.contains("--"))
        assertFalse(matchClause.contains("^"))
        assertFalse(matchClause.contains(":"))
    }

    @Test
    fun testBuildFts4MatchQuery_emptyOrWhitespaceReturnsBlank() {
        assertEquals("", SearchQueryBuilder.buildFts4MatchQuery(""))
        assertEquals("", SearchQueryBuilder.buildFts4MatchQuery("   "))
        assertEquals("", SearchQueryBuilder.buildFts4MatchQuery("*** \"\" :::"))
    }

    @Test
    fun testHasDiacritics() {
        assertTrue(SearchQueryBuilder.hasDiacritics("phân số"))
        assertTrue(SearchQueryBuilder.hasDiacritics("đại số"))
        assertFalse(SearchQueryBuilder.hasDiacritics("phan so"))
        assertFalse(SearchQueryBuilder.hasDiacritics("toan 6"))
    }
}
