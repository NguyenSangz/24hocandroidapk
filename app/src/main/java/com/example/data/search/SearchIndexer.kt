package com.example.data.search

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.FlashcardEntity
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.SearchFtsEntity
import com.example.data.local.entity.SubjectEntity
import com.example.domain.search.TextNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Quản lý dựng và đồng bộ chỉ mục FTS4 (F13):
 * - Dựng nền trên luồng IO, cập nhật tăng dần (incremental), không dựng lại từ đầu mỗi lần mở app.
 * - Có chỉ báo tiến độ StateFlow, tìm kiếm vẫn dùng được với phần dữ liệu đã nạp xong.
 * - Tự động bóc tách công thức LaTeX, tạo bản song song có dấu và không dấu.
 */
class SearchIndexer(private val database: AppDatabase) {

    companion object {
        private const val TAG = "SearchIndexer"
        private const val BATCH_SIZE = 100
    }

    private val _isIndexing = MutableStateFlow(false)
    val isIndexing: StateFlow<Boolean> = _isIndexing.asStateFlow()

    private val _indexingProgress = MutableStateFlow(1.0f)
    val indexingProgress: StateFlow<Float> = _indexingProgress.asStateFlow()

    /**
     * Đồng bộ toàn bộ dữ liệu hiện có trong Room vào FTS nếu chỉ mục chưa đầy đủ.
     * Hoạt động an toàn trên luồng Dispatchers.IO.
     */
    suspend fun syncAllIfNeeded(forceRebuild: Boolean = false) = withContext(Dispatchers.IO) {
        val searchDao = database.searchDao()
        val currentIndexCount = searchDao.getIndexCount()

        val lessonsCount = database.lessonDao().getCompletedLessonCount(6) // kiểm tra cơ bản
        if (!forceRebuild && currentIndexCount > 0) {
            Log.d(TAG, "Chỉ mục tìm kiếm đã sẵn sàng ($currentIndexCount bản ghi). Bỏ qua dựng lại.")
            return@withContext
        }

        _isIndexing.value = true
        _indexingProgress.value = 0.05f
        Log.d(TAG, "Bắt đầu dựng chỉ mục FTS4 nền...")

        try {
            if (forceRebuild) {
                searchDao.clearAll()
            }

            // 1. Tải trước các thực thể cơ sở
            val subjects = database.subjectDao().getAllSubjectsSync()
            val chapters = database.chapterDao().getAllChaptersSync()
            val lessons = database.lessonDao().getAllLessonsSync()
            val questions = database.questionDao().getAllQuestionsSync()
            val flashcards = database.flashcardDao().getAllFlashcardsSync()

            val chapterMap = chapters.associateBy { it.id }
            val subjectMap = subjects.associateBy { it.id }
            val lessonMap = lessons.associateBy { it.id }

            val totalWork = subjects.size + chapters.size + lessons.size + questions.size + flashcards.size + com.example.domain.tool.ToolRegistry.ALL_TOOLS.size
            var processed = 0

            val ftsBatch = mutableListOf<SearchFtsEntity>()

            // 1. Chỉ mục Môn học
            for (sub in subjects) {
                ftsBatch.add(createSubjectFts(sub))
                processed++
            }

            // 1b. Chỉ mục Công cụ học tập (F14)
            for (tool in com.example.domain.tool.ToolRegistry.ALL_TOOLS) {
                ftsBatch.add(createToolFts(tool))
                processed++
            }

            // 2. Chỉ mục Chương học
            for (ch in chapters) {
                ftsBatch.add(createChapterFts(ch))
                processed++
            }

            // 3. Chỉ mục Bài học
            for (l in lessons) {
                val ch = chapterMap[l.chapterId]
                ftsBatch.add(createLessonFts(l, ch))
                processed++
                if (ftsBatch.size >= BATCH_SIZE) {
                    searchDao.insertAll(ftsBatch)
                    ftsBatch.clear()
                    _indexingProgress.value = processed.toFloat() / totalWork
                }
            }

            // 4. Chỉ mục Câu hỏi
            for (q in questions) {
                val l = lessonMap[q.lessonId]
                val ch = l?.let { chapterMap[it.chapterId] }
                ftsBatch.add(createQuestionFts(q, l, ch))
                processed++
                if (ftsBatch.size >= BATCH_SIZE) {
                    searchDao.insertAll(ftsBatch)
                    ftsBatch.clear()
                    _indexingProgress.value = processed.toFloat() / totalWork
                }
            }

            // 5. Chỉ mục Flashcard
            for (fc in flashcards) {
                ftsBatch.add(createFlashcardFts(fc))
                processed++
                if (ftsBatch.size >= BATCH_SIZE) {
                    searchDao.insertAll(ftsBatch)
                    ftsBatch.clear()
                    _indexingProgress.value = processed.toFloat() / totalWork
                }
            }

            if (ftsBatch.isNotEmpty()) {
                searchDao.insertAll(ftsBatch)
                ftsBatch.clear()
            }

            _indexingProgress.value = 1.0f
            Log.d(TAG, "Đã hoàn thành dựng chỉ mục FTS4 với $processed bản ghi.")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi dựng chỉ mục FTS: ${e.message}", e)
        } finally {
            _isIndexing.value = false
        }
    }

    /**
     * Cập nhật tăng dần (incremental) cho 1 bài học khi có thay đổi
     */
    suspend fun indexLessonIncremental(lesson: LessonEntity, chapter: ChapterEntity?) = withContext(Dispatchers.IO) {
        val fts = createLessonFts(lesson, chapter)
        database.searchDao().deleteByTargetId(lesson.id)
        database.searchDao().insert(fts)
    }

    /**
     * Cập nhật tăng dần cho 1 câu hỏi
     */
    suspend fun indexQuestionIncremental(question: QuestionEntity, lesson: LessonEntity?, chapter: ChapterEntity?) = withContext(Dispatchers.IO) {
        val fts = createQuestionFts(question, lesson, chapter)
        database.searchDao().deleteByTargetId(question.id)
        database.searchDao().insert(fts)
    }

    /**
     * Cập nhật tăng dần cho 1 thẻ flashcard
     */
    suspend fun indexFlashcardIncremental(card: FlashcardEntity) = withContext(Dispatchers.IO) {
        val fts = createFlashcardFts(card)
        database.searchDao().deleteByTargetId(card.id)
        database.searchDao().insert(fts)
    }

    /**
     * Xóa 1 bản ghi khỏi chỉ mục FTS
     */
    suspend fun removeTargetFromIndex(targetId: String) = withContext(Dispatchers.IO) {
        database.searchDao().deleteByTargetId(targetId)
    }

    // --- Helpers tạo SearchFtsEntity chuẩn hóa ---

    fun createLessonFts(lesson: LessonEntity, chapter: ChapterEntity?): SearchFtsEntity {
        val rawTitle = lesson.title
        val titleClean = TextNormalizer.normalizeNfc(rawTitle)
        val titleNoAccent = TextNormalizer.toUnaccentedSearchTerm(rawTitle)

        val rawContent = listOf(
            lesson.summary,
            lesson.tips,
            lesson.objectives,
            lesson.examples
        ).filter { it.isNotBlank() }.joinToString(" ")

        val contentStripped = TextNormalizer.stripLatex(rawContent)
        val contentClean = TextNormalizer.normalizeNfc(contentStripped)
        val contentNoAccent = TextNormalizer.toUnaccentedSearchTerm(contentStripped)

        return SearchFtsEntity(
            targetId = lesson.id,
            type = "LESSON",
            grade = chapter?.grade ?: 6,
            series = lesson.series.ifBlank { chapter?.series ?: "kntt" },
            subjectId = chapter?.subjectId ?: "",
            phanMon = lesson.phanMon.ifBlank { chapter?.phanMon ?: "" },
            topicId = lesson.topicId,
            title = titleClean,
            titleNoAccent = titleNoAccent,
            content = contentClean,
            contentNoAccent = contentNoAccent
        )
    }

    fun createQuestionFts(
        question: QuestionEntity,
        lesson: LessonEntity?,
        chapter: ChapterEntity?
    ): SearchFtsEntity {
        val rawTitle = question.content
        val titleStripped = TextNormalizer.stripLatex(rawTitle)
        val titleClean = TextNormalizer.normalizeNfc(titleStripped)
        val titleNoAccent = TextNormalizer.toUnaccentedSearchTerm(titleStripped)

        val rawContent = listOf(
            question.content,
            question.explanation,
            question.hint,
            lesson?.title ?: ""
        ).filter { it.isNotBlank() }.joinToString(" ")

        val contentStripped = TextNormalizer.stripLatex(rawContent)
        val contentClean = TextNormalizer.normalizeNfc(contentStripped)
        val contentNoAccent = TextNormalizer.toUnaccentedSearchTerm(contentStripped)

        return SearchFtsEntity(
            targetId = question.id,
            type = "QUESTION",
            grade = chapter?.grade ?: 6,
            series = question.series.ifBlank { lesson?.series ?: chapter?.series ?: "kntt" },
            subjectId = chapter?.subjectId ?: "",
            phanMon = question.phanMon.ifBlank { lesson?.phanMon ?: chapter?.phanMon ?: "" },
            topicId = question.topicId.ifBlank { lesson?.topicId ?: "" },
            title = titleClean,
            titleNoAccent = titleNoAccent,
            content = contentClean,
            contentNoAccent = contentNoAccent
        )
    }

    fun createFlashcardFts(card: FlashcardEntity): SearchFtsEntity {
        val rawTitle = listOfNotNull(card.front, card.word).filter { it.isNotBlank() }.joinToString(" ")
        val titleClean = TextNormalizer.normalizeNfc(rawTitle)
        val titleNoAccent = TextNormalizer.toUnaccentedSearchTerm(rawTitle)

        val rawContent = listOfNotNull(
            card.back,
            card.meaning,
            card.example,
            card.category,
            card.notes
        ).filter { it.isNotBlank() }.joinToString(" ")

        val contentStripped = TextNormalizer.stripLatex(rawContent)
        val contentClean = TextNormalizer.normalizeNfc(contentStripped)
        val contentNoAccent = TextNormalizer.toUnaccentedSearchTerm(contentStripped)

        return SearchFtsEntity(
            targetId = card.id,
            type = "FLASHCARD",
            grade = card.grade,
            series = card.series.ifBlank { "kntt" },
            subjectId = card.subjectId,
            phanMon = card.phanMon,
            topicId = card.topicId,
            title = titleClean,
            titleNoAccent = titleNoAccent,
            content = contentClean,
            contentNoAccent = contentNoAccent
        )
    }

    fun createChapterFts(chapter: ChapterEntity): SearchFtsEntity {
        val titleClean = TextNormalizer.normalizeNfc(chapter.title)
        val titleNoAccent = TextNormalizer.toUnaccentedSearchTerm(chapter.title)

        val contentClean = TextNormalizer.normalizeNfc(chapter.description)
        val contentNoAccent = TextNormalizer.toUnaccentedSearchTerm(chapter.description)

        return SearchFtsEntity(
            targetId = chapter.id,
            type = "CHAPTER",
            grade = chapter.grade,
            series = chapter.series,
            subjectId = chapter.subjectId,
            phanMon = chapter.phanMon,
            topicId = chapter.id,
            title = titleClean,
            titleNoAccent = titleNoAccent,
            content = contentClean,
            contentNoAccent = contentNoAccent
        )
    }

    fun createSubjectFts(subject: SubjectEntity): SearchFtsEntity {
        val titleClean = TextNormalizer.normalizeNfc(subject.name)
        val titleNoAccent = TextNormalizer.toUnaccentedSearchTerm(subject.name)

        val contentClean = TextNormalizer.normalizeNfc(subject.description)
        val contentNoAccent = TextNormalizer.toUnaccentedSearchTerm(subject.description)

        return SearchFtsEntity(
            targetId = subject.id,
            type = "SUBJECT",
            grade = 0, // Dùng chung các khối
            series = "all",
            subjectId = subject.id,
            phanMon = "",
            topicId = subject.id,
            title = titleClean,
            titleNoAccent = titleNoAccent,
            content = contentClean,
            contentNoAccent = contentNoAccent
        )
    }

    fun createToolFts(tool: com.example.domain.tool.SubjectToolItem): SearchFtsEntity {
        val titleClean = TextNormalizer.normalizeNfc(tool.name)
        val titleNoAccent = TextNormalizer.toUnaccentedSearchTerm(tool.name)

        val contentCombined = "${tool.shortDescription} ${tool.tags.joinToString(" ")}"
        val contentClean = TextNormalizer.normalizeNfc(contentCombined)
        val contentNoAccent = TextNormalizer.toUnaccentedSearchTerm(contentCombined)

        return SearchFtsEntity(
            targetId = tool.route,
            type = "TOOL",
            grade = 0, // Dùng chung cho tất cả các khối
            series = "all",
            subjectId = tool.relatedSubjectIds.firstOrNull() ?: "math",
            phanMon = tool.relatedPhanMon.firstOrNull() ?: "",
            topicId = tool.id,
            title = titleClean,
            titleNoAccent = titleNoAccent,
            content = contentClean,
            contentNoAccent = contentNoAccent
        )
    }
}
