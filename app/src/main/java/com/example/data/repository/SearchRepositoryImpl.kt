package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.SearchFtsEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.data.search.SearchIndexer
import com.example.domain.model.CrossSeriesSuggestion
import com.example.domain.model.SearchFilter
import com.example.domain.model.SearchGroupedSection
import com.example.domain.model.SearchResult
import com.example.domain.model.SearchResultType
import com.example.domain.model.SearchSuggestion
import com.example.domain.model.SuggestionType
import com.example.domain.repository.SearchRepository
import com.example.domain.search.SearchQueryBuilder
import com.example.domain.search.TextNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Locale

class SearchRepositoryImpl(
    private val database: AppDatabase,
    private val preferencesManager: UserPreferencesManager,
    private val searchIndexer: SearchIndexer = SearchIndexer(database)
) : SearchRepository {

    override val isIndexing: Flow<Boolean> = searchIndexer.isIndexing

    override val isSearchHistoryEnabled: Flow<Boolean> = preferencesManager.isSearchHistoryEnabled

    override suspend fun syncIndexIfNeeded() {
        searchIndexer.syncAllIfNeeded(forceRebuild = false)
    }

    override suspend fun search(
        query: String,
        filter: SearchFilter,
        limit: Int,
        offset: Int
    ): List<SearchResult> {
        val ftsQuery = SearchQueryBuilder.buildFts4MatchQuery(query)
        if (ftsQuery.isBlank()) return emptyList()

        // Đảm bảo chỉ mục đã được nạp nếu DB mới khởi tạo
        if (database.searchDao().getIndexCount() == 0) {
            searchIndexer.syncAllIfNeeded()
        }

        // Lấy tất cả kết quả khớp từ bảng ảo FTS4
        val rawMatches = database.searchDao().searchAllMatches(ftsQuery)
        if (rawMatches.isEmpty()) return emptyList()

        // Lấy thông tin môn học để gán màu sắc và tên môn
        val subjects = database.subjectDao().getAllSubjectsSync().associateBy { it.id }

        // Lấy bộ sách mặc định hoặc ghi đè theo môn
        val defaultSeries = preferencesManager.defaultSeries.first()
        val overrides = preferencesManager.subjectSeriesOverrides.first()

        val tokens = TextNormalizer.extractSearchTokens(query)
        val queryNfc = TextNormalizer.normalizeNfc(query)
        val queryUnaccented = TextNormalizer.toUnaccentedSearchTerm(query)
        val isAccented = SearchQueryBuilder.hasDiacritics(query)

        // 1. Lọc theo bộ lọc chip
        val filteredList = rawMatches.filter { item ->
            // Lọc theo lớp (subject có grade=0 dùng chung mọi khối)
            if (filter.grade != 0 && item.grade != 0 && item.grade != filter.grade) {
                return@filter false
            }

            // Lọc theo loại kết quả
            if (filter.resultType != null && !item.type.equals(filter.resultType.name, ignoreCase = true)) {
                return@filter false
            }

            // Lọc theo môn học
            if (filter.subjectId != "ALL" && !item.subjectId.equals(filter.subjectId, ignoreCase = true)) {
                return@filter false
            }

            // Lọc theo phần môn (Lý, Hóa, Sinh, Lịch sử, Địa lí)
            if (filter.phanMon != "ALL" && !item.phanMon.equals(filter.phanMon, ignoreCase = true)) {
                return@filter false
            }

            // Lọc theo bộ sách
            when (filter.series.uppercase(Locale.ROOT)) {
                "ALL" -> true // Cho phép mọi bộ sách
                "CURRENT" -> {
                    val activeSeriesForSub = overrides[item.subjectId] ?: defaultSeries
                    item.series.equals(activeSeriesForSub, ignoreCase = true) ||
                            item.series.equals("chung", ignoreCase = true) ||
                            item.series.equals("all", ignoreCase = true)
                }
                else -> {
                    item.series.equals(filter.series, ignoreCase = true) ||
                            item.series.equals("chung", ignoreCase = true) ||
                            item.series.equals("all", ignoreCase = true)
                }
            }
        }

        // 2. Chấm điểm xếp hạng (Relevance Ranking Formula)
        val scoredList = filteredList.map { item ->
            var score = 0

            // Ưu tiên khớp chính xác nguyên cụm từ tiêu đề
            if (item.title.equals(queryNfc, ignoreCase = true)) {
                score += 120
            } else if (item.titleNoAccent.equals(queryUnaccented, ignoreCase = true)) {
                score += 80
            }

            // Ưu tiên khớp có dấu trên không dấu
            if (isAccented) {
                if (item.title.contains(queryNfc) || item.content.contains(queryNfc)) {
                    score += 50
                }
            }

            // Khớp các từ trong tiêu đề (trọng số cao x40)
            val titleMatches = tokens.count { token ->
                item.title.contains(token, ignoreCase = true) ||
                        item.titleNoAccent.contains(TextNormalizer.toUnaccentedSearchTerm(token))
            }
            score += titleMatches * 40

            // Khớp các từ trong nội dung (trọng số x15)
            val contentMatches = tokens.count { token ->
                item.content.contains(token, ignoreCase = true) ||
                        item.contentNoAccent.contains(TextNormalizer.toUnaccentedSearchTerm(token))
            }
            score += contentMatches * 15

            // Ưu tiên theo loại thực thể: Bài học/Chương/Môn > Câu hỏi/Flashcard
            when (item.type) {
                "LESSON" -> score += 60
                "CHAPTER" -> score += 50
                "SUBJECT" -> score += 45
                "QUESTION" -> score += 20
                "FLASHCARD" -> score += 20
                else -> score += 10
            }

            // Khớp nhiều từ khóa hơn -> điểm cao hơn
            score += (titleMatches + contentMatches) * 5

            val sub = subjects[item.subjectId]
            val typeEnum = SearchResultType.fromString(item.type)
            val snippet = generateSnippet(item.content, tokens)

            SearchResult(
                rowId = item.rowid.toLong(),
                targetId = item.targetId,
                type = typeEnum,
                grade = item.grade,
                series = item.series,
                subjectId = item.subjectId,
                subjectName = sub?.name ?: "Môn học",
                subjectColor = sub?.color ?: "#2563EB",
                phanMon = item.phanMon,
                topicId = item.topicId,
                title = item.title,
                snippet = snippet,
                isCompleted = false,
                relevanceScore = score,
                extraDetails = item.content
            )
        }

        // 3. Xử lý gộp kết quả có chung topicId khi chọn "Tất cả bộ sách" (filter.series == "ALL")
        val processedList = if (filter.series.equals("ALL", ignoreCase = true)) {
            groupResultsByTopicId(scoredList)
        } else {
            scoredList
        }

        // 4. Sắp xếp theo điểm số giảm dần và áp dụng phân trang (limit, offset)
        return processedList
            .sortedByDescending { it.relevanceScore }
            .drop(offset)
            .take(limit)
    }

    override suspend fun searchGrouped(
        query: String,
        filter: SearchFilter,
        limitPerGroup: Int
    ): List<SearchGroupedSection> {
        val results = search(query, filter, limit = 200, offset = 0)
        if (results.isEmpty()) return emptyList()

        return results.groupBy { it.type }
            .map { (type, items) ->
                SearchGroupedSection(
                    type = type,
                    title = "${type.displayName} (${items.size})",
                    items = items.take(limitPerGroup)
                )
            }
            .sortedBy { it.type.order }
    }

    override suspend fun findCrossSeriesSuggestions(
        query: String,
        currentSeries: String,
        grade: Int
    ): List<CrossSeriesSuggestion> {
        val ftsQuery = SearchQueryBuilder.buildFts4MatchQuery(query)
        if (ftsQuery.isBlank()) return emptyList()

        val rawMatches = database.searchDao().searchAllMatches(ftsQuery)
        val otherSeriesMatches = rawMatches.filter {
            (grade == 0 || it.grade == 0 || it.grade == grade) &&
                    !it.series.equals(currentSeries, ignoreCase = true) &&
                    !it.series.equals("chung", ignoreCase = true) &&
                    !it.series.equals("all", ignoreCase = true)
        }

        if (otherSeriesMatches.isEmpty()) return emptyList()

        val seriesNames = mapOf(
            "kntt" to "Kết nối tri thức",
            "cd" to "Cánh Diều",
            "ctst" to "Chân trời sáng tạo"
        )

        return otherSeriesMatches.groupBy { it.series.lowercase() }
            .map { (seriesId, items) ->
                CrossSeriesSuggestion(
                    targetSeries = seriesId,
                    seriesDisplayName = seriesNames[seriesId] ?: seriesId.uppercase(),
                    resultCount = items.size
                )
            }
            .sortedByDescending { it.resultCount }
    }

    override fun getSearchSuggestions(grade: Int): Flow<List<SearchSuggestion>> = flow {
        val list = mutableListOf<SearchSuggestion>()

        // 1. Lịch sử tìm kiếm gần đây
        val recentSearches = preferencesManager.searchHistoryQueries.first()
        for (q in recentSearches.take(5)) {
            list.add(SearchSuggestion(text = q, type = SuggestionType.RECENT_SEARCH))
        }

        // 2. Môn yêu thích / ghim
        val pinnedSubjects = database.subjectDao().getAllSubjectsSync().filter { it.isPinned }
        for (sub in pinnedSubjects.take(3)) {
            list.add(
                SearchSuggestion(
                    text = sub.name,
                    type = SuggestionType.PINNED_SUBJECT,
                    targetId = sub.id,
                    extraInfo = "Môn học yêu thích"
                )
            )
        }

        // 3. Chủ đề bài học đang học / gần đây
        val recentLessons = database.lessonDao().getAllLessonsSync().take(3)
        for (l in recentLessons) {
            list.add(
                SearchSuggestion(
                    text = l.title,
                    type = SuggestionType.IN_PROGRESS_TOPIC,
                    targetId = l.id,
                    extraInfo = "Chủ đề bài học"
                )
            )
        }

        emit(list)
    }

    override fun getRecentSearches(): Flow<List<String>> =
        preferencesManager.searchHistoryQueries

    override suspend fun saveSearchQuery(query: String) {
        preferencesManager.addSearchQuery(query)
    }

    override suspend fun deleteSearchQuery(query: String) {
        preferencesManager.deleteSearchQuery(query)
    }

    override suspend fun clearSearchHistory() {
        preferencesManager.clearSearchHistory()
    }

    override suspend fun setSearchHistoryEnabled(enabled: Boolean) {
        preferencesManager.setSearchHistoryEnabled(enabled)
    }

    // --- Private Helper Methods ---

    /**
     * Gộp các kết quả cùng topicId khi chọn "Tất cả bộ sách"
     */
    private fun groupResultsByTopicId(results: List<SearchResult>): List<SearchResult> {
        val nonTopicResults = mutableListOf<SearchResult>()
        val topicGroups = mutableMapOf<String, MutableList<SearchResult>>()

        for (item in results) {
            if (item.topicId.isBlank() || item.type != SearchResultType.LESSON) {
                nonTopicResults.add(item)
            } else {
                topicGroups.getOrPut(item.topicId) { mutableListOf() }.add(item)
            }
        }

        val groupedLessons = topicGroups.map { (_, list) ->
            val bestItem = list.maxByOrNull { it.relevanceScore } ?: list.first()
            val allSeries = list.map { it.series }.distinct()
            bestItem.copy(
                matchedSeriesList = allSeries
            )
        }

        return nonTopicResults + groupedLessons
    }

    /**
     * Tạo đoạn trích (snippet) 80-120 ký tự bao quanh từ khóa tìm kiếm
     */
    private fun generateSnippet(content: String, tokens: List<String>): String {
        if (content.isBlank()) return ""
        val clean = TextNormalizer.stripLatex(content)
        if (clean.length <= 100) return clean

        var firstIndex = -1
        val cleanLower = clean.lowercase(Locale.ROOT)
        val cleanNoAccent = TextNormalizer.removeDiacritics(cleanLower)

        for (t in tokens) {
            val tLower = t.lowercase(Locale.ROOT)
            val idx = cleanLower.indexOf(tLower)
            if (idx != -1) {
                firstIndex = idx
                break
            }
            val tNo = TextNormalizer.removeDiacritics(tLower)
            val idxNo = cleanNoAccent.indexOf(tNo)
            if (idxNo != -1) {
                firstIndex = idxNo
                break
            }
        }

        if (firstIndex == -1) {
            return clean.take(100) + "..."
        }

        val start = (firstIndex - 35).coerceAtLeast(0)
        val end = (firstIndex + 65).coerceAtMost(clean.length)
        val prefix = if (start > 0) "..." else ""
        val suffix = if (end < clean.length) "..." else ""

        return prefix + clean.substring(start, end).trim() + suffix
    }
}
