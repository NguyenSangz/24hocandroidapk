package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AttemptEntity
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.ExamResultEntity
import com.example.data.local.entity.FlashcardEntity
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.UserProgressEntity
import com.example.data.local.entity.WrongQuestionEntity
import com.example.data.sync.SyncOutboxHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CurriculumRepository(private val database: AppDatabase) {
    val allSubjects: Flow<List<SubjectEntity>> = database.subjectDao().getAllSubjects()

    suspend fun togglePinSubject(subjectId: String, isPinned: Boolean) {
        database.subjectDao().updatePinned(subjectId, isPinned)
    }

    fun getChapters(grade: Int, subjectId: String): Flow<List<ChapterEntity>> {
        return database.chapterDao().getChaptersByGradeAndSubject(grade, subjectId)
    }

    fun getAllChaptersForGrade(grade: Int): Flow<List<ChapterEntity>> {
        return database.chapterDao().getAllChaptersByGrade(grade)
    }

    /**
     * F17a: Danh sách id các môn có nội dung (có chương) cho lớp được chọn.
     * StudyViewModel dùng kết quả này để ẩn các môn chưa có gói nội dung.
     */
    fun getSubjectIdsWithChapters(grade: Int): Flow<List<String>> {
        return database.chapterDao().getSubjectIdsWithChapters(grade)
    }

    suspend fun getChapter(chapterId: String): ChapterEntity? {
        return database.chapterDao().getChapterById(chapterId)
    }

    fun getLessons(chapterId: String): Flow<List<LessonEntity>> {
        return database.lessonDao().getLessonsByChapter(chapterId)
    }

    suspend fun getLesson(lessonId: String): LessonEntity? {
        return database.lessonDao().getLessonById(lessonId)
    }

    fun getLessonFlow(lessonId: String): Flow<LessonEntity?> {
        return database.lessonDao().getLessonFlowById(lessonId)
    }

    suspend fun toggleLessonCompleted(lessonId: String, completed: Boolean) {
        database.lessonDao().updateLessonCompleted(lessonId, completed)
        SyncOutboxHelper.enqueueLessonProgress(database, lessonId, completed)
    }

    fun getCompletedLessonsCount(grade: Int): Flow<Int> {
        return database.lessonDao().getCompletedLessonCount(grade)
    }

    fun getTotalLessonsCount(grade: Int): Flow<Int> {
        return database.lessonDao().getTotalLessonCount(grade)
    }

    suspend fun getSubjectById(subjectId: String): SubjectEntity? {
        return database.subjectDao().getSubjectById(subjectId)
    }

    suspend fun getAllChaptersForGradeSync(grade: Int): List<ChapterEntity> {
        return database.chapterDao().getAllChaptersSync().filter { it.grade == grade }
    }

    suspend fun getLessonsForChapterSync(chapterId: String): List<LessonEntity> {
        return database.lessonDao().getLessonsByChapter(chapterId).firstOrNull() ?: emptyList()
    }
}

class PracticeRepository(private val database: AppDatabase) {
    suspend fun getQuestionsForLesson(lessonId: String): List<QuestionEntity> {
        return database.questionDao().getQuestionsByLesson(lessonId)
    }

    suspend fun getQuestionsByChapter(chapterId: String): List<QuestionEntity> {
        return database.questionDao().getQuestionsByChapter(chapterId)
    }

    suspend fun getAllQuestionsBySubject(grade: Int, subjectId: String): List<QuestionEntity> {
        return database.questionDao().getAllQuestionsBySubject(grade, subjectId)
    }

    suspend fun getQuestionById(questionId: String): QuestionEntity? {
        return database.questionDao().getQuestionById(questionId)
    }

    suspend fun recordAttempt(
        questionId: String,
        isCorrect: Boolean,
        score: Float,
        timeSpentSeconds: Int,
        userAnswer: String,
        usedHint: Boolean
    ): Long {
        val attempt = AttemptEntity(
            questionId = questionId,
            isCorrect = isCorrect,
            score = score,
            timestamp = System.currentTimeMillis(),
            timeSpentSeconds = timeSpentSeconds,
            userAnswer = userAnswer,
            usedHint = usedHint
        )
        val rowId = database.attemptDao().insertAttempt(attempt)
        SyncOutboxHelper.enqueueAttempt(database, attempt)
        return rowId
    }

    suspend fun getRandomExamQuestions(grade: Int, subjectId: String?, count: Int = 10): List<QuestionEntity> {
        return if (subjectId != null && subjectId != "all") {
            database.questionDao().getRandomQuestionsBySubject(grade, subjectId, count)
        } else {
            database.questionDao().getRandomQuestionsByGrade(grade, count)
        }
    }

    suspend fun saveExamResult(result: ExamResultEntity): Long {
        return database.examResultDao().insertExamResult(result)
    }

    fun getRecentExams(grade: Int, limit: Int = 10): Flow<List<ExamResultEntity>> {
        return database.examResultDao().getRecentResults(grade, limit)
    }

    fun getAverageScore(grade: Int): Flow<Float?> {
        return database.examResultDao().getAverageScore(grade)
    }

    fun getHighScoringCount(): Flow<Int> {
        return database.examResultDao().getHighScoringExamCount()
    }
}

class WrongQuestionRepository(
    private val database: AppDatabase,
    val scheduler: com.example.domain.evaluator.ReviewScheduler = com.example.domain.evaluator.ReviewScheduler()
) {
    val allWrongQuestions: Flow<List<WrongQuestionEntity>> =
        database.wrongQuestionDao().getAllWrongQuestions()

    fun getWrongQuestionsByGrade(grade: Int): Flow<List<WrongQuestionEntity>> =
        database.wrongQuestionDao().getWrongQuestionsByGrade(grade)

    fun getNeedsReviewCount(grade: Int = 0): Flow<Int> =
        database.wrongQuestionDao().getNeedsReviewCount(grade)

    fun getDueReviewCount(grade: Int = 0): Flow<Int> =
        database.wrongQuestionDao().getDueReviewCount(System.currentTimeMillis(), grade)

    /**
     * Ghi nhận câu làm sai (từ Luyện tập, Kiểm tra hoặc Phiên ôn):
     * Tự động lấy thông tin môn, chương, lớp từ bảng Question nếu chưa có;
     * Áp dụng Spaced Repetition (reset chuỗi đúng, tăng số lần sai, hẹn ôn lại 1 ngày sau).
     * Tuyệt đối không trùng lặp bản ghi (mỗi câu hỏi duy nhất 1 bản ghi).
     */
    suspend fun recordWrongAnswer(
        questionId: String,
        lessonId: String = "",
        wrongAnswer: String = ""
    ) {
        val questionEntity = database.questionDao().getQuestionById(questionId) ?: return // Bỏ qua nếu câu hỏi gốc không tồn tại (tránh mồ côi)
        val finalLessonId = lessonId.ifBlank { questionEntity.lessonId }
        val lesson = database.lessonDao().getLessonById(finalLessonId)
        val chapter = lesson?.let { database.chapterDao().getChapterById(it.chapterId) }

        val grade = chapter?.grade ?: 6
        val subjectId = chapter?.subjectId ?: ""
        val chapterId = lesson?.chapterId ?: ""
        val questionType = questionEntity.type

        val existing = database.wrongQuestionDao().getByQuestionId(questionId)
        val updated = scheduler.onAnswerWrong(
            existingEntity = existing,
            questionId = questionId,
            grade = grade,
            subjectId = subjectId,
            chapterId = chapterId,
            lessonId = finalLessonId,
            questionType = questionType,
            wrongAnswer = wrongAnswer
        )
        database.wrongQuestionDao().insertOrUpdate(updated)
        SyncOutboxHelper.enqueueWrongQuestion(database, updated)
    }

    /**
     * Ghi nhận khi học sinh trả lời đúng (ở Luyện tập thường hoặc trong Phiên ôn câu sai):
     * Nếu câu này có trong ngân hàng câu sai và cách lần ôn trước ít nhất 1 ngày -> Tăng chuỗi đúng.
     * Đủ 3 lần đúng -> Chuyển sang "Đã thuộc" (MASTERED).
     */
    suspend fun recordCorrectAnswer(questionId: String): WrongQuestionEntity? {
        val existing = database.wrongQuestionDao().getByQuestionId(questionId) ?: return null
        val updated = scheduler.onAnswerCorrect(existing)
        database.wrongQuestionDao().insertOrUpdate(updated)
        SyncOutboxHelper.enqueueWrongQuestion(database, updated)
        return updated
    }

    /**
     * Đánh dấu "Đã thuộc" thủ công theo yêu cầu của học sinh
     */
    suspend fun markMastered(questionId: String) {
        val existing = database.wrongQuestionDao().getByQuestionId(questionId) ?: return
        val updated = scheduler.markMasteredManually(existing)
        database.wrongQuestionDao().insertOrUpdate(updated)
        SyncOutboxHelper.enqueueWrongQuestion(database, updated)
    }

    /**
     * Xóa câu hỏi khỏi ngân hàng câu sai
     */
    suspend fun deleteWrongQuestion(questionId: String) {
        database.wrongQuestionDao().deleteByQuestionId(questionId)
        SyncOutboxHelper.enqueueWrongQuestionDeletion(database, questionId)
    }

    /**
     * Lấy danh sách câu hỏi đến hạn ôn tập cho học sinh
     */
    suspend fun getDueQuestions(
        grade: Int = 0,
        subjectId: String = "all",
        limit: Int = 20
    ): List<WrongQuestionEntity> {
        return database.wrongQuestionDao().getDueQuestionsForReview(
            currentTime = System.currentTimeMillis(),
            grade = grade,
            subjectId = subjectId,
            limit = limit
        )
    }

    /**
     * Lấy các câu hỏi để làm phiên ôn tập câu sai (đã xử lý loại bỏ bản ghi mồ côi)
     */
    suspend fun getQuestionsForReviewSession(
        grade: Int = 6,
        subjectId: String? = null,
        chapterId: String? = null,
        count: Int = 10,
        questionIds: List<String>? = null
    ): List<QuestionEntity> {
        // Dọn dẹp bản ghi mồ côi ngầm
        cleanupOrphanRecords()

        val candidateEntities: List<WrongQuestionEntity> = if (!questionIds.isNullOrEmpty()) {
            database.wrongQuestionDao().getByQuestionIds(questionIds)
        } else {
            val due = database.wrongQuestionDao().getDueQuestionsForReview(
                currentTime = System.currentTimeMillis(),
                grade = grade,
                subjectId = subjectId ?: "all",
                limit = count * 2
            )
            if (due.isNotEmpty()) due else {
                // Nếu không có câu đến hạn, lấy các câu chưa thuộc bất kỳ
                database.wrongQuestionDao().getDueQuestionsForReview(
                    currentTime = Long.MAX_VALUE,
                    grade = grade,
                    subjectId = subjectId ?: "all",
                    limit = count
                )
            }
        }

        val filteredEntities = candidateEntities.filter { entity ->
            (subjectId == null || subjectId == "all" || entity.subjectId == subjectId) &&
            (chapterId == null || chapterId == "all" || entity.chapterId == chapterId)
        }.take(count)

        // Tải QuestionEntity tương ứng từ database, bỏ qua bản ghi mồ côi
        val questions = mutableListOf<QuestionEntity>()
        for (w in filteredEntities) {
            val q = database.questionDao().getQuestionById(w.questionId)
            if (q != null) {
                questions.add(q)
            }
        }
        return questions
    }

    /**
     * Lấy thông tin chi tiết đầy đủ của các câu sai để hiển thị trên màn hình tổng quan
     */
    suspend fun getWrongQuestionDetails(grade: Int): List<com.example.domain.model.WrongQuestionDetail> {
        val now = System.currentTimeMillis()
        val list = mutableListOf<com.example.domain.model.WrongQuestionDetail>()
        val wrongEntities = database.wrongQuestionDao().getDueQuestionsForReview(
            currentTime = Long.MAX_VALUE, // lấy toàn bộ
            grade = grade,
            subjectId = "all",
            limit = 500
        )

        for (w in wrongEntities) {
            val qEntity = database.questionDao().getQuestionById(w.questionId)
            if (qEntity == null) {
                // Câu hỏi gốc đã bị xóa/đổi ID -> tự dọn dẹp bản ghi mồ côi
                database.wrongQuestionDao().deleteByQuestionId(w.questionId)
                continue
            }

            val question = try {
                com.example.domain.mapper.QuestionMapper.toDomain(qEntity)
            } catch (_: Exception) {
                null
            }

            val lesson = database.lessonDao().getLessonById(w.lessonId)
            val chapter = lesson?.let { database.chapterDao().getChapterById(it.chapterId) }
            val subject = chapter?.let { database.subjectDao().getSubjectById(it.subjectId) }

            list.add(
                com.example.domain.model.WrongQuestionDetail(
                    entity = w,
                    question = question,
                    lessonTitle = lesson?.title ?: "Bài học",
                    chapterTitle = chapter?.title ?: "Chương",
                    subjectName = subject?.name ?: "Môn học",
                    isDueForReview = scheduler.isDueForReview(w, now)
                )
            )
        }
        return list
    }

    /**
     * Dọn dẹp các bản ghi câu sai mà câu hỏi gốc không còn tồn tại trong bảng questions
     */
    suspend fun cleanupOrphanRecords(): Int {
        return try {
            database.wrongQuestionDao().deleteOrphanWrongQuestions()
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Bước đồng bộ một lần: Quét các câu làm sai (hoặc đúng một phần < 1.0) từ AttemptEntity và TestAnswerEntity
     * để tự động đưa vào ngân hàng câu sai nếu chưa có.
     */
    suspend fun syncExistingMistakes() {
        try {
            cleanupOrphanRecords()
            // 1. Quét từ bảng attempts (luyện tập)
            val attempts = database.attemptDao().getAllAttempts().firstOrNull() ?: emptyList()
            val wrongAttempts = attempts.filter { !it.isCorrect || it.score < 1.0f }
            for (attempt in wrongAttempts) {
                val existing = database.wrongQuestionDao().getByQuestionId(attempt.questionId)
                if (existing == null) {
                    recordWrongAnswer(
                        questionId = attempt.questionId,
                        wrongAnswer = attempt.userAnswer
                    )
                }
            }

            // 2. Quét từ bảng test_answers (kiểm tra)
            val tests = database.testDao().getAllCompletedTests().firstOrNull() ?: emptyList()
            for (test in tests) {
                val answers = database.testDao().getAnswersForTest(test.id)
                val wrongAnswers = answers.filter { !it.isCorrect || it.score < 1.0f }
                for (ans in wrongAnswers) {
                    val existing = database.wrongQuestionDao().getByQuestionId(ans.questionId)
                    if (existing == null) {
                        recordWrongAnswer(
                            questionId = ans.questionId,
                            wrongAnswer = ans.userAnswerJson
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Đảm bảo không làm gián đoạn ứng dụng
        }
    }
}

class FlashcardRepository(private val database: AppDatabase) {
    fun getFlashcards(grade: Int, subjectId: String? = null): Flow<List<FlashcardEntity>> {
        return if (subjectId == null || subjectId == "all") {
            database.flashcardDao().getFlashcardsByGrade(grade)
        } else {
            database.flashcardDao().getFlashcardsByGradeAndSubject(grade, subjectId)
        }
    }

    fun getMasteredCount(grade: Int): Flow<Int> {
        return database.flashcardDao().getMasteredFlashcardCount(grade)
    }

    /**
     * Thuật toán SM-2 (SuperMemo-2) cho Flashcard:
     * - quality: 0 (hoàn toàn quên), 3 (nhớ nhưng khó), 5 (nhớ hoàn hảo)
     */
    suspend fun recordReviewSm2(card: FlashcardEntity, quality: Int) {
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60 * 60 * 1000

        var newReps = card.repetitions
        var newInterval: Int
        var newEase = card.easeFactor + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
        if (newEase < 1.3f) newEase = 1.3f

        if (quality >= 3) {
            newInterval = when (newReps) {
                0 -> 1
                1 -> 6
                else -> (card.intervalDays * newEase).toInt().coerceAtLeast(1)
            }
            newReps += 1
        } else {
            newReps = 0
            newInterval = 1
        }

        val newBox = (newReps + 1).coerceAtMost(5)
        val nextReview = now + (newInterval * dayMillis)

        database.flashcardDao().updateCardReview(
            id = card.id,
            box = newBox,
            intervalDays = newInterval,
            nextReview = nextReview,
            easeFactor = newEase,
            repetitions = newReps
        )
        val existingProg = database.flashcardDao().getProgressForCard(card.id)
        val newProg = com.example.data.local.entity.FlashcardProgressEntity(
            cardId = card.id,
            deckId = card.deckId,
            repetitions = newReps,
            intervalDays = newInterval,
            easeFactor = newEase,
            consecutiveCorrect = if (quality >= 3) (existingProg?.consecutiveCorrect ?: 0) + 1 else 0,
            nextReviewTimestamp = nextReview,
            lastReviewTimestamp = now,
            state = if (newReps >= 3) "MASTERED" else "REVIEW",
            totalReviews = (existingProg?.totalReviews ?: 0) + 1,
            lapses = (existingProg?.lapses ?: 0) + if (quality < 3) 1 else 0
        )
        database.flashcardDao().insertProgress(newProg)
        SyncOutboxHelper.enqueueFlashcardProgress(database, newProg, now)
    }

    suspend fun recordReview(card: FlashcardEntity, remembered: Boolean) {
        val quality = if (remembered) 4 else 1
        recordReviewSm2(card, quality)
    }
}

class UserProgressRepository(private val database: AppDatabase) {
    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val todayProgress: Flow<UserProgressEntity?> = database.userProgressDao().getProgressFlowByDate(getTodayDateString())
    val recentProgressList: Flow<List<UserProgressEntity>> = database.userProgressDao().getRecentProgress()
    val allProgressList: Flow<List<UserProgressEntity>> = database.userProgressDao().getAllProgress()

    val totalQuestionsAnswered: Flow<Int?> = database.userProgressDao().getTotalQuestionsAnswered()
    val totalQuestionsCorrect: Flow<Int?> = database.userProgressDao().getTotalQuestionsCorrect()
    val totalStudyMinutes: Flow<Int?> = database.userProgressDao().getTotalStudyMinutes()
    val totalXp: Flow<Int?> = database.userProgressDao().getTotalXp()

    suspend fun recordPracticeActivity(answeredCount: Int, correctCount: Int, studyMinutes: Int = 1, xp: Int = 10) {
        val today = getTodayDateString()
        val current = database.userProgressDao().getProgressByDate(today)
        if (current == null) {
            database.userProgressDao().insertOrUpdate(
                UserProgressEntity(
                    date = today,
                    questionsAnswered = answeredCount,
                    questionsCorrect = correctCount,
                    studyMinutes = studyMinutes,
                    flashcardsReviewed = 0,
                    xpEarned = xp
                )
            )
        } else {
            database.userProgressDao().insertOrUpdate(
                current.copy(
                    questionsAnswered = current.questionsAnswered + answeredCount,
                    questionsCorrect = current.questionsCorrect + correctCount,
                    studyMinutes = current.studyMinutes + studyMinutes,
                    xpEarned = current.xpEarned + xp
                )
            )
        }
    }

    suspend fun recordFlashcardReview(count: Int = 1, xp: Int = 5) {
        val today = getTodayDateString()
        val current = database.userProgressDao().getProgressByDate(today)
        if (current == null) {
            database.userProgressDao().insertOrUpdate(
                UserProgressEntity(
                    date = today,
                    questionsAnswered = 0,
                    questionsCorrect = 0,
                    studyMinutes = 1,
                    flashcardsReviewed = count,
                    xpEarned = xp
                )
            )
        } else {
            database.userProgressDao().insertOrUpdate(
                current.copy(
                    flashcardsReviewed = current.flashcardsReviewed + count,
                    studyMinutes = current.studyMinutes + 1,
                    xpEarned = current.xpEarned + xp
                )
            )
        }
    }
}
