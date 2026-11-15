package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.SearchFtsEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.SearchRepositoryImpl
import com.example.domain.model.SearchFilter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var preferencesManager: UserPreferencesManager
    private lateinit var repository: SearchRepositoryImpl

    private val sampleFtsItems = listOf(
        SearchFtsEntity(
            rowid = 1,
            targetId = "l1",
            type = "LESSON",
            grade = 6,
            series = "kntt",
            subjectId = "toan",
            phanMon = "",
            topicId = "tap-hop",
            title = "Bài 1: Tập hợp",
            titleNoAccent = "bai 1: tap hop",
            content = "Làm quen với tập hợp số tự nhiên",
            contentNoAccent = "lam quen voi tap hop so tu nhien"
        ),
        SearchFtsEntity(
            rowid = 2,
            targetId = "l2",
            type = "LESSON",
            grade = 6,
            series = "cd",
            subjectId = "toan",
            phanMon = "",
            topicId = "tap-hop",
            title = "Bài 1: Tập hợp các số tự nhiên",
            titleNoAccent = "bai 1: tap hop cac so tu nhien",
            content = "Tập hợp và các phần tử trong Cánh Diều",
            contentNoAccent = "tap hop va cac phan tu trong canh dieu"
        ),
        SearchFtsEntity(
            rowid = 3,
            targetId = "l3",
            type = "LESSON",
            grade = 7,
            series = "kntt",
            subjectId = "toan",
            phanMon = "",
            topicId = "so-huu-ti",
            title = "Bài 1: Tập hợp các số hữu tỉ",
            titleNoAccent = "bai 1: tap hop cac so huu ti",
            content = "Số hữu tỉ lớp 7",
            contentNoAccent = "so huu ti lop 7"
        ),
        SearchFtsEntity(
            rowid = 4,
            targetId = "q1",
            type = "QUESTION",
            grade = 6,
            series = "kntt",
            subjectId = "khtn",
            phanMon = "sinh",
            topicId = "te-bao",
            title = "Tế bào là gì?",
            titleNoAccent = "te bao la gi",
            content = "Tế bào là đơn vị cơ bản của sự sống",
            contentNoAccent = "te bao la don vi co ban cua su song"
        )
    )

    private val sampleSubjects = listOf(
        SubjectEntity(id = "toan", name = "Toán", icon = "calculate", color = "#2563EB", description = ""),
        SubjectEntity(id = "khtn", name = "KHTN", icon = "science", color = "#059669", description = "")
    )

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferencesManager = UserPreferencesManager(context)

        database.subjectDao().insertAll(sampleSubjects)
        database.searchDao().insertAll(sampleFtsItems)

        preferencesManager.setDefaultSeries("kntt")
        preferencesManager.setSearchHistoryEnabled(true)
        preferencesManager.addSearchQuery("tập hợp")

        repository = SearchRepositoryImpl(database, preferencesManager)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testFilterByGrade() = runBlocking {
        val filterGrade6 = SearchFilter(grade = 6, series = "ALL")
        val resultsGrade6 = repository.search("tap hop", filterGrade6)

        assertTrue(resultsGrade6.isNotEmpty())
        assertTrue(resultsGrade6.all { it.grade == 6 })
        assertTrue(resultsGrade6.none { it.targetId == "l3" }) // l3 là lớp 7
    }

    @Test
    fun testFilterBySeriesCurrent() = runBlocking {
        val filterCurrent = SearchFilter(grade = 6, series = "CURRENT")
        val results = repository.search("tap hop", filterCurrent)

        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.series == "kntt" })
    }

    @Test
    fun testGroupResultsByTopicIdWhenAllSeriesSelected() = runBlocking {
        val filterAll = SearchFilter(grade = 6, series = "ALL")
        val results = repository.search("tap hop", filterAll)

        val topicGroupItem = results.find { it.topicId == "tap-hop" }
        assertNotNull(topicGroupItem)
        assertTrue(topicGroupItem!!.matchedSeriesList.contains("kntt"))
        assertTrue(topicGroupItem.matchedSeriesList.contains("cd"))
    }

    @Test
    fun testAccentedSearchRanksAccentedMatchesHigher() = runBlocking {
        val filter = SearchFilter(grade = 6, series = "ALL")
        val results = repository.search("Tập hợp", filter)
        assertTrue(results.isNotEmpty())

        val topScore = results.first().relevanceScore
        assertTrue(topScore > 100)
    }

    @Test
    fun testFindCrossSeriesSuggestions() = runBlocking {
        val suggestions = repository.findCrossSeriesSuggestions("tap hop", currentSeries = "kntt", grade = 6)
        assertTrue(suggestions.isNotEmpty())
        val cdSuggestion = suggestions.find { it.targetSeries == "cd" }
        assertNotNull(cdSuggestion)
        assertEquals("Cánh Diều", cdSuggestion!!.seriesDisplayName)
    }
}
