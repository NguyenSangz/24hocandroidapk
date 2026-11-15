package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.AttemptEntity
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.ExamResultEntity
import com.example.data.local.entity.FlashcardDeckEntity
import com.example.data.local.entity.FlashcardEntity
import com.example.data.local.entity.FlashcardProgressEntity
import com.example.data.local.entity.FlashcardWithProgress
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.ReviewLogEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.UserProgressEntity
import com.example.data.local.entity.WrongQuestionEntity
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.PersonalNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY isPinned DESC, name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: String): SubjectEntity?

    @Query("UPDATE subjects SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinned(id: String, isPinned: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subjects: List<SubjectEntity>)

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun getSubjectCount(): Int

    @Query("SELECT * FROM subjects")
    suspend fun getAllSubjectsSync(): List<SubjectEntity>
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE grade = :grade AND subjectId = :subjectId ORDER BY `order` ASC")
    fun getChaptersByGradeAndSubject(grade: Int, subjectId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE grade = :grade ORDER BY `order` ASC")
    fun getAllChaptersByGrade(grade: Int): Flow<List<ChapterEntity>>

    /**
     * F17a: Danh sách môn học THỰC SỰ có nội dung (có chương) cho một lớp.
     * Dùng để ẩn các môn "khai báo nhưng trống" (chưa có gói nội dung) khỏi UI,
     * tránh hiện chip môn mà bấm vào không có bài học nào.
     */
    @Query("SELECT DISTINCT subjectId FROM chapters WHERE grade = :grade")
    fun getSubjectIdsWithChapters(grade: Int): Flow<List<String>>

    @Query("SELECT * FROM chapters")
    suspend fun getAllChaptersSync(): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    suspend fun getChapterById(id: String): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<ChapterEntity>)
}

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE chapterId = :chapterId ORDER BY `order` ASC")
    fun getLessonsByChapter(chapterId: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    suspend fun getLessonById(lessonId: String): LessonEntity?

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    fun getLessonFlowById(lessonId: String): Flow<LessonEntity?>

    @Query("UPDATE lessons SET isCompleted = :completed WHERE id = :lessonId")
    suspend fun updateLessonCompleted(lessonId: String, completed: Boolean)

    @Query("""
        SELECT COUNT(l.id) FROM lessons l 
        INNER JOIN chapters c ON l.chapterId = c.id 
        WHERE c.grade = :grade AND l.isCompleted = 1
    """)
    fun getCompletedLessonCount(grade: Int): Flow<Int>

    @Query("""
        SELECT COUNT(l.id) FROM lessons l 
        INNER JOIN chapters c ON l.chapterId = c.id 
        WHERE c.grade = :grade
    """)
    fun getTotalLessonCount(grade: Int): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(lessons: List<LessonEntity>)

    @Query("SELECT * FROM lessons")
    suspend fun getAllLessonsSync(): List<LessonEntity>
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    suspend fun getQuestionById(id: String): QuestionEntity?

    @Query("SELECT * FROM questions WHERE lessonId = :lessonId")
    suspend fun getQuestionsByLesson(lessonId: String): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE lessonId = :lessonId")
    fun getQuestionsFlowByLesson(lessonId: String): Flow<List<QuestionEntity>>

    @Query("""
        SELECT q.* FROM questions q
        INNER JOIN lessons l ON q.lessonId = l.id
        INNER JOIN chapters c ON l.chapterId = c.id
        WHERE c.grade = :grade AND c.subjectId = :subjectId
        ORDER BY RANDOM() LIMIT :limit
    """)
    suspend fun getRandomQuestionsBySubject(grade: Int, subjectId: String, limit: Int): List<QuestionEntity>

    @Query("""
        SELECT q.* FROM questions q
        INNER JOIN lessons l ON q.lessonId = l.id
        INNER JOIN chapters c ON l.chapterId = c.id
        WHERE c.grade = :grade
        ORDER BY RANDOM() LIMIT :limit
    """)
    suspend fun getRandomQuestionsByGrade(grade: Int, limit: Int): List<QuestionEntity>

    @Query("""
        SELECT q.* FROM questions q
        INNER JOIN lessons l ON q.lessonId = l.id
        WHERE l.chapterId = :chapterId
    """)
    suspend fun getQuestionsByChapter(chapterId: String): List<QuestionEntity>

    @Query("""
        SELECT q.* FROM questions q
        INNER JOIN lessons l ON q.lessonId = l.id
        INNER JOIN chapters c ON l.chapterId = c.id
        WHERE c.grade = :grade AND c.subjectId = :subjectId
    """)
    suspend fun getAllQuestionsBySubject(grade: Int, subjectId: String): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getTotalQuestionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuestionEntity>)

    @Query("SELECT * FROM questions")
    suspend fun getAllQuestionsSync(): List<QuestionEntity>
}

@Dao
interface WrongQuestionDao {
    @Query("SELECT * FROM wrong_questions ORDER BY lastWrongTimestamp DESC")
    fun getAllWrongQuestions(): Flow<List<WrongQuestionEntity>>

    @Query("SELECT * FROM wrong_questions WHERE grade = :grade ORDER BY lastWrongTimestamp DESC")
    fun getWrongQuestionsByGrade(grade: Int): Flow<List<WrongQuestionEntity>>

    @Query("SELECT * FROM wrong_questions WHERE questionId = :questionId LIMIT 1")
    suspend fun getByQuestionId(questionId: String): WrongQuestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(wrongQuestion: WrongQuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wrongQuestions: List<WrongQuestionEntity>)

    @Query("DELETE FROM wrong_questions WHERE questionId = :questionId")
    suspend fun deleteByQuestionId(questionId: String)

    @Query("DELETE FROM wrong_questions WHERE questionId NOT IN (SELECT id FROM questions)")
    suspend fun deleteOrphanWrongQuestions(): Int

    @Query("SELECT COUNT(*) FROM wrong_questions WHERE status != 'MASTERED' AND (:grade = 0 OR grade = :grade)")
    fun getNeedsReviewCount(grade: Int = 0): Flow<Int>

    @Query("SELECT COUNT(*) FROM wrong_questions WHERE status != 'MASTERED' AND nextReviewTimestamp <= :currentTime AND (:grade = 0 OR grade = :grade)")
    fun getDueReviewCount(currentTime: Long, grade: Int = 0): Flow<Int>

    @Query("""
        SELECT * FROM wrong_questions 
        WHERE status != 'MASTERED' 
          AND (:grade = 0 OR grade = :grade)
          AND (:subjectId = 'all' OR subjectId = :subjectId)
        ORDER BY 
          CASE WHEN nextReviewTimestamp <= :currentTime THEN 0 ELSE 1 END ASC,
          errorCount DESC,
          nextReviewTimestamp ASC
        LIMIT :limit
    """)
    suspend fun getDueQuestionsForReview(
        currentTime: Long,
        grade: Int = 0,
        subjectId: String = "all",
        limit: Int = 20
    ): List<WrongQuestionEntity>

    @Query("SELECT * FROM wrong_questions WHERE questionId IN (:questionIds)")
    suspend fun getByQuestionIds(questionIds: List<String>): List<WrongQuestionEntity>

    @Query("SELECT * FROM wrong_questions")
    suspend fun getAllWrongQuestionsSync(): List<WrongQuestionEntity>

    @Query("UPDATE wrong_questions SET status = :status, consecutiveCorrectCount = :consecutive, nextReviewTimestamp = :nextReview, lastReviewTimestamp = :lastReview WHERE questionId = :questionId")
    suspend fun updateStatus(
        questionId: String,
        status: String,
        consecutive: Int,
        nextReview: Long,
        lastReview: Long
    )
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE grade = :grade ORDER BY id ASC")
    fun getFlashcardsByGrade(grade: Int): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE grade = :grade AND subjectId = :subjectId ORDER BY id ASC")
    fun getFlashcardsByGradeAndSubject(grade: Int, subjectId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE grade = :grade AND nextReviewTimestamp <= :currentTimestamp")
    suspend fun getFlashcardsDueForReview(grade: Int, currentTimestamp: Long): List<FlashcardEntity>

    @Query("""
        UPDATE flashcards 
        SET box = :box, intervalDays = :intervalDays, nextReviewTimestamp = :nextReview, easeFactor = :easeFactor, repetitions = :repetitions 
        WHERE id = :id
    """)
    suspend fun updateCardReview(id: String, box: Int, intervalDays: Int, nextReview: Long, easeFactor: Float, repetitions: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<FlashcardEntity>)

    @Query("SELECT * FROM flashcards")
    suspend fun getAllFlashcardsSync(): List<FlashcardEntity>

    @Query("SELECT COUNT(*) FROM flashcards WHERE grade = :grade AND box >= 3")
    fun getMasteredFlashcardCount(grade: Int): Flow<Int>

    // === CÁC PHƯƠNG THỨC MỚI CHO BỘ THẺ & SM-2 (F09) ===
    @Query("SELECT * FROM flashcard_decks WHERE grade = :grade ORDER BY name ASC")
    fun getDecksByGrade(grade: Int): Flow<List<FlashcardDeckEntity>>

    @Query("SELECT * FROM flashcard_decks WHERE grade = :grade AND subjectId = :subjectId ORDER BY name ASC")
    fun getDecksByGradeAndSubject(grade: Int, subjectId: String): Flow<List<FlashcardDeckEntity>>

    @Query("SELECT * FROM flashcard_decks WHERE id = :deckId LIMIT 1")
    fun getDeckById(deckId: String): Flow<FlashcardDeckEntity?>

    @Query("SELECT * FROM flashcard_decks WHERE id = :deckId LIMIT 1")
    suspend fun getDeckByIdSync(deckId: String): FlashcardDeckEntity?

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY id ASC")
    fun getCardsByDeck(deckId: String): Flow<List<FlashcardEntity>>

    @Transaction
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY id ASC")
    fun getCardsWithProgressByDeck(deckId: String): Flow<List<FlashcardWithProgress>>

    @Transaction
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY id ASC")
    suspend fun getCardsWithProgressByDeckSync(deckId: String): List<FlashcardWithProgress>

    @Transaction
    @Query("SELECT * FROM flashcards WHERE grade = :grade ORDER BY id ASC")
    fun getAllCardsWithProgressByGrade(grade: Int): Flow<List<FlashcardWithProgress>>

    @Transaction
    @Query("SELECT * FROM flashcards WHERE grade = :grade ORDER BY id ASC")
    suspend fun getAllCardsWithProgressByGradeSync(grade: Int): List<FlashcardWithProgress>

    @Query("SELECT * FROM flashcard_progress WHERE cardId = :cardId LIMIT 1")
    suspend fun getProgressForCard(cardId: String): FlashcardProgressEntity?

    @Query("SELECT * FROM flashcard_progress WHERE deckId = :deckId")
    suspend fun getProgressForDeck(deckId: String): List<FlashcardProgressEntity>

    @Query("SELECT * FROM flashcard_progress")
    suspend fun getAllFlashcardProgressSync(): List<FlashcardProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecks(decks: List<FlashcardDeckEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: FlashcardProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressList(progressList: List<FlashcardProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewLog(log: ReviewLogEntity)

    @Query("SELECT * FROM review_logs WHERE reviewTimestamp >= :startTimestamp ORDER BY reviewTimestamp DESC")
    suspend fun getReviewLogsSince(startTimestamp: Long): List<ReviewLogEntity>

    @Query("""
        SELECT COUNT(f.id) 
        FROM flashcards f
        LEFT JOIN flashcard_progress p ON f.id = p.cardId
        WHERE f.grade = :grade 
          AND (p.state = 'NEW' OR p.nextReviewTimestamp <= :currentTimestamp OR p.cardId IS NULL)
    """)
    fun getTotalDueCardsCountByGrade(grade: Int, currentTimestamp: Long): Flow<Int>

    @Query("""
        SELECT COUNT(f.id) 
        FROM flashcards f
        LEFT JOIN flashcard_progress p ON f.id = p.cardId
        WHERE f.deckId = :deckId 
          AND (p.state = 'NEW' OR p.cardId IS NULL)
    """)
    fun getNewCardsCountByDeck(deckId: String): Flow<Int>

    @Query("""
        SELECT COUNT(f.id) 
        FROM flashcards f
        JOIN flashcard_progress p ON f.id = p.cardId
        WHERE f.deckId = :deckId 
          AND (p.state != 'NEW' AND p.nextReviewTimestamp <= :currentTimestamp)
    """)
    fun getDueReviewCardsCountByDeck(deckId: String, currentTimestamp: Long): Flow<Int>

    @Query("""
        SELECT COUNT(f.id) 
        FROM flashcards f
        JOIN flashcard_progress p ON f.id = p.cardId
        WHERE f.deckId = :deckId 
          AND p.state = 'MASTERED'
    """)
    fun getMasteredCardsCountByDeck(deckId: String): Flow<Int>

    @Query("DELETE FROM flashcard_progress WHERE cardId NOT IN (SELECT id FROM flashcards)")
    suspend fun deleteOrphanProgress(): Int
}

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress WHERE date = :date LIMIT 1")
    suspend fun getProgressByDate(date: String): UserProgressEntity?

    @Query("SELECT * FROM user_progress WHERE date = :date LIMIT 1")
    fun getProgressFlowByDate(date: String): Flow<UserProgressEntity?>

    @Query("SELECT * FROM user_progress ORDER BY date DESC LIMIT 7")
    fun getRecentProgress(): Flow<List<UserProgressEntity>>

    @Query("SELECT * FROM user_progress ORDER BY date ASC")
    fun getAllProgress(): Flow<List<UserProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: UserProgressEntity)

    @Query("SELECT SUM(questionsAnswered) FROM user_progress")
    fun getTotalQuestionsAnswered(): Flow<Int?>

    @Query("SELECT SUM(questionsCorrect) FROM user_progress")
    fun getTotalQuestionsCorrect(): Flow<Int?>

    @Query("SELECT SUM(studyMinutes) FROM user_progress")
    fun getTotalStudyMinutes(): Flow<Int?>

    @Query("SELECT SUM(xpEarned) FROM user_progress")
    fun getTotalXp(): Flow<Int?>
}

@Dao
interface ExamResultDao {
    @Query("SELECT * FROM exam_results ORDER BY timestamp DESC")
    fun getAllExamResults(): Flow<List<ExamResultEntity>>

    @Query("SELECT * FROM exam_results WHERE grade = :grade ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentResults(grade: Int, limit: Int): Flow<List<ExamResultEntity>>

    @Query("SELECT AVG(score) FROM exam_results WHERE grade = :grade")
    fun getAverageScore(grade: Int): Flow<Float?>

    @Query("SELECT COUNT(*) FROM exam_results WHERE score >= 9.0")
    fun getHighScoringExamCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamResult(result: ExamResultEntity): Long
}

@Dao
interface AttemptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: AttemptEntity): Long

    @Query("SELECT * FROM attempts ORDER BY timestamp DESC")
    fun getAllAttempts(): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM attempts ORDER BY timestamp ASC")
    suspend fun getAllAttemptsSync(): List<AttemptEntity>

    @Query("SELECT COUNT(*) FROM attempts WHERE questionId = :questionId AND timestamp = :timestamp")
    suspend fun countByQuestionAndTimestamp(questionId: String, timestamp: Long): Int

    @Query("SELECT * FROM attempts WHERE questionId = :questionId ORDER BY timestamp DESC")
    fun getAttemptsByQuestion(questionId: String): Flow<List<AttemptEntity>>

    @Query("SELECT COUNT(*) FROM attempts")
    suspend fun getTotalAttemptCount(): Int

    @Query("""
        SELECT a.* FROM attempts a
        INNER JOIN questions q ON a.questionId = q.id
        INNER JOIN lessons l ON q.lessonId = l.id
        INNER JOIN chapters c ON l.chapterId = c.id
        WHERE c.grade = :grade AND (:subjectId = 'all' OR c.subjectId = :subjectId)
          AND a.timestamp >= :fromTimestamp AND a.timestamp <= :toTimestamp
        ORDER BY a.timestamp ASC
    """)
    fun getAttemptsInPeriod(
        grade: Int,
        subjectId: String = "all",
        fromTimestamp: Long,
        toTimestamp: Long
    ): Flow<List<AttemptEntity>>

    @Query("""
        SELECT c.subjectId, 
               COUNT(a.id) AS totalAnswered, 
               SUM(CASE WHEN a.isCorrect = 1 THEN 1 ELSE 0 END) AS totalCorrect
        FROM attempts a
        INNER JOIN questions q ON a.questionId = q.id
        INNER JOIN lessons l ON q.lessonId = l.id
        INNER JOIN chapters c ON l.chapterId = c.id
        WHERE c.grade = :grade 
          AND a.timestamp >= :fromTimestamp AND a.timestamp <= :toTimestamp
        GROUP BY c.subjectId
    """)
    fun getAccuracyBySubjectInPeriod(
        grade: Int,
        fromTimestamp: Long,
        toTimestamp: Long
    ): Flow<List<com.example.data.local.dao.SubjectAccuracyStat>>

    @Query("""
        SELECT c.id AS chapterId, 
               c.subjectId, 
               COUNT(a.id) AS totalAnswered, 
               SUM(CASE WHEN a.isCorrect = 1 THEN 1 ELSE 0 END) AS totalCorrect
        FROM attempts a
        INNER JOIN questions q ON a.questionId = q.id
        INNER JOIN lessons l ON q.lessonId = l.id
        INNER JOIN chapters c ON l.chapterId = c.id
        WHERE c.grade = :grade 
          AND (:subjectId = 'all' OR c.subjectId = :subjectId)
          AND a.timestamp >= :fromTimestamp AND a.timestamp <= :toTimestamp
        GROUP BY c.id, c.subjectId
    """)
    fun getAccuracyByChapterInPeriod(
        grade: Int,
        subjectId: String = "all",
        fromTimestamp: Long,
        toTimestamp: Long
    ): Flow<List<com.example.data.local.dao.ChapterAccuracyStat>>

    @Query("""
        SELECT a.questionId, a.isCorrect, a.timestamp, c.id AS chapterId, c.title AS chapterTitle, c.subjectId
        FROM attempts a
        INNER JOIN questions q ON a.questionId = q.id
        INNER JOIN lessons l ON q.lessonId = l.id
        INNER JOIN chapters c ON l.chapterId = c.id
        WHERE c.grade = :grade AND (:subjectId = 'all' OR c.subjectId = :subjectId)
        ORDER BY a.timestamp DESC
        LIMIT :limit
    """)
    suspend fun getRecentAttemptsWithChapter(
        grade: Int,
        subjectId: String = "all",
        limit: Int = 500
    ): List<AttemptChapterRow>
}

data class AttemptChapterRow(
    val questionId: String,
    val isCorrect: Boolean,
    val timestamp: Long,
    val chapterId: String,
    val chapterTitle: String,
    val subjectId: String
)

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE id = :id LIMIT 1")
    suspend fun getBookmarkById(id: String): BookmarkEntity?

    @Query("SELECT * FROM bookmarks WHERE id = :id LIMIT 1")
    fun getBookmarkFlow(id: String): Flow<BookmarkEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: String)
}

@Dao
interface PersonalNoteDao {
    @Query("SELECT * FROM personal_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<PersonalNoteEntity>>

    @Query("SELECT * FROM personal_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): PersonalNoteEntity?

    @Query("SELECT * FROM personal_notes WHERE id = :id LIMIT 1")
    fun getNoteFlow(id: String): Flow<PersonalNoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: PersonalNoteEntity)

    @Query("DELETE FROM personal_notes WHERE id = :id")
    suspend fun deleteNote(id: String)
}

