package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.AttemptDao
import com.example.data.local.dao.ChapterDao
import com.example.data.local.dao.ContentVersionDao
import com.example.data.local.dao.ExamResultDao
import com.example.data.local.dao.FlashcardDao
import com.example.data.local.dao.GamificationDao
import com.example.data.local.dao.LessonDao
import com.example.data.local.dao.QuestionDao
import com.example.data.local.dao.SearchDao
import com.example.data.local.dao.StudySessionDao
import com.example.data.local.dao.SubjectDao
import com.example.data.local.dao.SyncDao
import com.example.data.local.dao.TestDao
import com.example.data.local.dao.UserProgressDao
import com.example.data.local.dao.WrongQuestionDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AttemptEntity
import com.example.data.local.entity.BadgeUnlockEntity
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.ContentVersionEntity
import com.example.data.local.entity.Converters
import com.example.data.local.entity.ExamResultEntity
import com.example.data.local.entity.FlashcardDeckEntity
import com.example.data.local.entity.FlashcardEntity
import com.example.data.local.entity.FlashcardProgressEntity
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.local.entity.ReviewLogEntity
import com.example.data.local.entity.SearchFtsEntity
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.SyncOutboxEntity
import com.example.data.local.entity.SyncStateEntity
import com.example.data.local.entity.TestAnswerEntity
import com.example.data.local.entity.TestEntity
import com.example.data.local.entity.UserProgressEntity
import com.example.data.local.entity.WrongQuestionEntity
import com.example.data.local.entity.XpEventEntity
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.PersonalNoteEntity
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.dao.PersonalNoteDao

@Database(
    entities = [
        SubjectEntity::class,
        ChapterEntity::class,
        LessonEntity::class,
        QuestionEntity::class,
        WrongQuestionEntity::class,
        FlashcardDeckEntity::class,
        FlashcardEntity::class,
        FlashcardProgressEntity::class,
        ReviewLogEntity::class,
        UserProgressEntity::class,
        ExamResultEntity::class,
        AttemptEntity::class,
        TestEntity::class,
        TestAnswerEntity::class,
        XpEventEntity::class,
        BadgeUnlockEntity::class,
        StudySessionEntity::class,
        SearchFtsEntity::class,
        AccountEntity::class,
        SyncOutboxEntity::class,
        SyncStateEntity::class,
        ContentVersionEntity::class,
        BookmarkEntity::class,
        PersonalNoteEntity::class
    ],
    version = 14,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun lessonDao(): LessonDao
    abstract fun questionDao(): QuestionDao
    abstract fun wrongQuestionDao(): WrongQuestionDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun examResultDao(): ExamResultDao
    abstract fun attemptDao(): AttemptDao
    abstract fun testDao(): TestDao
    abstract fun gamificationDao(): GamificationDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun searchDao(): SearchDao
    abstract fun accountDao(): AccountDao
    abstract fun syncDao(): SyncDao
    abstract fun contentVersionDao(): ContentVersionDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun personalNoteDao(): PersonalNoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration từ Room Version 3 lên Version 4 (F07 - Chế độ kiểm tra)
         * Thêm 2 bảng mới `tests` và `test_answers`, bảo toàn 100% dữ liệu của các bảng cũ.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tạo bảng tests
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tests` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `title` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `subjectId` TEXT NOT NULL,
                        `subjectName` TEXT NOT NULL,
                        `grade` INTEGER NOT NULL,
                        `targetId` TEXT NOT NULL,
                        `score` REAL NOT NULL,
                        `maxScore` REAL NOT NULL,
                        `totalQuestions` INTEGER NOT NULL,
                        `correctCount` INTEGER NOT NULL,
                        `timeSpentSeconds` INTEGER NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `isDraft` INTEGER NOT NULL,
                        `remainingSeconds` INTEGER NOT NULL,
                        `endTimeEpochMillis` INTEGER NOT NULL
                    )
                """.trimIndent())

                // Tạo index cho bảng tests
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tests_subjectId_grade` ON `tests` (`subjectId`, `grade`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tests_timestamp` ON `tests` (`timestamp`)")

                // Tạo bảng test_answers
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `test_answers` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `testId` TEXT NOT NULL,
                        `questionId` TEXT NOT NULL,
                        `questionIndex` INTEGER NOT NULL,
                        `userAnswerJson` TEXT NOT NULL,
                        `isAnswered` INTEGER NOT NULL,
                        `isFlagged` INTEGER NOT NULL,
                        `score` REAL NOT NULL,
                        `isCorrect` INTEGER NOT NULL,
                        `gradeStatus` TEXT NOT NULL,
                        `selfEvaluated` INTEGER NOT NULL
                    )
                """.trimIndent())

                // Tạo index cho bảng test_answers
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_test_answers_testId` ON `test_answers` (`testId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_test_answers_questionId` ON `test_answers` (`questionId`)")
            }
        }

        /**
         * Migration từ Room Version 4 lên Version 5 (F08 - Ngân hàng câu sai & Spaced Repetition)
         * Nâng cấp schema bảng `wrong_questions`: bổ sung grade, subjectId, chapterId, questionType,
         * consecutiveCorrectCount, status, nextReviewTimestamp, lastReviewTimestamp.
         * Bảo toàn 100% dữ liệu câu sai đã có.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tạo bảng wrong_questions tạm thời với cấu trúc mới
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `wrong_questions_new` (
                        `questionId` TEXT NOT NULL PRIMARY KEY,
                        `grade` INTEGER NOT NULL DEFAULT 6,
                        `subjectId` TEXT NOT NULL DEFAULT '',
                        `chapterId` TEXT NOT NULL DEFAULT '',
                        `lessonId` TEXT NOT NULL DEFAULT '',
                        `questionType` TEXT NOT NULL DEFAULT '',
                        `errorCount` INTEGER NOT NULL DEFAULT 1,
                        `consecutiveCorrectCount` INTEGER NOT NULL DEFAULT 0,
                        `lastWrongTimestamp` INTEGER NOT NULL DEFAULT 0,
                        `lastWrongAnswer` TEXT NOT NULL DEFAULT '',
                        `status` TEXT NOT NULL DEFAULT 'NEEDS_REVIEW',
                        `nextReviewTimestamp` INTEGER NOT NULL DEFAULT 0,
                        `lastReviewTimestamp` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Chuyển dữ liệu cũ sang bảng mới
                db.execSQL("""
                    INSERT INTO `wrong_questions_new` (
                        `questionId`,
                        `grade`,
                        `subjectId`,
                        `chapterId`,
                        `lessonId`,
                        `questionType`,
                        `errorCount`,
                        `consecutiveCorrectCount`,
                        `lastWrongTimestamp`,
                        `lastWrongAnswer`,
                        `status`,
                        `nextReviewTimestamp`,
                        `lastReviewTimestamp`
                    )
                    SELECT 
                        `questionId`,
                        6,
                        '',
                        '',
                        COALESCE(`lessonId`, ''),
                        '',
                        COALESCE(`errorCount`, 1),
                        0,
                        COALESCE(`timestamp`, 0),
                        COALESCE(`lastWrongAnswer`, ''),
                        CASE WHEN `isResolved` = 1 THEN 'MASTERED' ELSE 'NEEDS_REVIEW' END,
                        0,
                        COALESCE(`timestamp`, 0)
                    FROM `wrong_questions`
                """.trimIndent())

                // Xóa bảng cũ và đổi tên bảng mới
                db.execSQL("DROP TABLE `wrong_questions`")
                db.execSQL("ALTER TABLE `wrong_questions_new` RENAME TO `wrong_questions`")

                // Tạo các chỉ mục tối ưu truy vấn
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wrong_questions_grade_subjectId` ON `wrong_questions` (`grade`, `subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wrong_questions_status` ON `wrong_questions` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wrong_questions_nextReviewTimestamp` ON `wrong_questions` (`nextReviewTimestamp`)")

                // Nạp thông tin ngữ cảnh môn, chương từ bảng câu hỏi nếu có
                try {
                    db.execSQL("""
                        UPDATE `wrong_questions`
                        SET 
                            `grade` = COALESCE((SELECT c.grade FROM `questions` q JOIN `lessons` l ON q.lessonId = l.id JOIN `chapters` c ON l.chapterId = c.id WHERE q.id = `wrong_questions`.questionId), 6),
                            `subjectId` = COALESCE((SELECT c.subjectId FROM `questions` q JOIN `lessons` l ON q.lessonId = l.id JOIN `chapters` c ON l.chapterId = c.id WHERE q.id = `wrong_questions`.questionId), ''),
                            `chapterId` = COALESCE((SELECT l.chapterId FROM `questions` q JOIN `lessons` l ON q.lessonId = l.id WHERE q.id = `wrong_questions`.questionId), ''),
                            `lessonId` = COALESCE((SELECT q.lessonId FROM `questions` q WHERE q.id = `wrong_questions`.questionId), `wrong_questions`.lessonId),
                            `questionType` = COALESCE((SELECT q.type FROM `questions` q WHERE q.id = `wrong_questions`.questionId), '')
                        WHERE EXISTS (SELECT 1 FROM `questions` WHERE id = `wrong_questions`.questionId)
                    """.trimIndent())
                } catch (_: Exception) {
                    // Tiếp tục an toàn nếu bảng questions chưa có dữ liệu tại thời điểm migrate
                }
            }
        }

        /**
         * Migration từ Room Version 5 lên Version 6 (F09 - Bộ thẻ Flashcard & SM-2)
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `flashcard_decks` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `grade` INTEGER NOT NULL,
                        `subjectId` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `cardCount` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcard_decks_grade_subjectId` ON `flashcard_decks` (`grade`, `subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcard_decks_type` ON `flashcard_decks` (`type`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `flashcard_progress` (
                        `cardId` TEXT NOT NULL PRIMARY KEY,
                        `deckId` TEXT NOT NULL,
                        `repetitions` INTEGER NOT NULL,
                        `intervalDays` INTEGER NOT NULL,
                        `easeFactor` REAL NOT NULL,
                        `consecutiveCorrect` INTEGER NOT NULL,
                        `nextReviewTimestamp` INTEGER NOT NULL,
                        `lastReviewTimestamp` INTEGER NOT NULL,
                        `state` TEXT NOT NULL,
                        `totalReviews` INTEGER NOT NULL,
                        `lapses` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcard_progress_deckId` ON `flashcard_progress` (`deckId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcard_progress_nextReviewTimestamp` ON `flashcard_progress` (`nextReviewTimestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_flashcard_progress_state` ON `flashcard_progress` (`state`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `review_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `cardId` TEXT NOT NULL,
                        `deckId` TEXT NOT NULL,
                        `rating` INTEGER NOT NULL,
                        `reviewTimestamp` INTEGER NOT NULL,
                        `scheduledDays` INTEGER NOT NULL,
                        `easeFactor` REAL NOT NULL,
                        `timeSpentMs` INTEGER NOT NULL,
                        `isCustomSession` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_review_logs_cardId` ON `review_logs` (`cardId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_review_logs_deckId` ON `review_logs` (`deckId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_review_logs_reviewTimestamp` ON `review_logs` (`reviewTimestamp`)")
            }
        }

        /**
         * Migration từ Room Version 6 lên Version 7 (F10 - XP, Level, Streak, Badges)
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `xp_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `eventKey` TEXT NOT NULL,
                        `xpAmount` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `metadata` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_xp_events_eventKey` ON `xp_events` (`eventKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_xp_events_timestamp` ON `xp_events` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_xp_events_eventType` ON `xp_events` (`eventType`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `badge_unlocks` (
                        `badgeId` TEXT NOT NULL PRIMARY KEY,
                        `unlockedAt` INTEGER NOT NULL,
                        `metadata` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_badge_unlocks_unlockedAt` ON `badge_unlocks` (`unlockedAt`)")

                // Backfill tính toán XP ban đầu từ dữ liệu đã có
                try {
                    db.execSQL("""
                        INSERT OR IGNORE INTO `xp_events` (`eventType`, `eventKey`, `xpAmount`, `timestamp`, `metadata`)
                        SELECT 'LESSON_COMPLETED', 'lesson_' || id, 10, 0, title
                        FROM `lessons` WHERE `isCompleted` = 1
                    """.trimIndent())

                    db.execSQL("""
                        INSERT OR IGNORE INTO `xp_events` (`eventType`, `eventKey`, `xpAmount`, `timestamp`, `metadata`)
                        SELECT 'PRACTICE_CORRECT', 'practice_attempt_' || id, 
                               CASE WHEN usedHint = 1 THEN 1 ELSE 2 END, 
                               timestamp, questionId
                        FROM `attempts` WHERE `isCorrect` = 1
                    """.trimIndent())

                    db.execSQL("""
                        INSERT OR IGNORE INTO `xp_events` (`eventType`, `eventKey`, `xpAmount`, `timestamp`, `metadata`)
                        SELECT 'TEST_COMPLETED', 'test_' || id, 
                               CAST(ROUND(score * 5) AS INTEGER) + CASE WHEN score >= 8.0 THEN 10 ELSE 0 END, 
                               timestamp, title
                        FROM `tests` WHERE `isDraft` = 0
                    """.trimIndent())

                    db.execSQL("""
                        INSERT OR IGNORE INTO `xp_events` (`eventType`, `eventKey`, `xpAmount`, `timestamp`, `metadata`)
                        SELECT 'WRONG_MASTERED', 'wrong_mastered_' || questionId, 3, COALESCE(lastReviewTimestamp, 0), questionId
                        FROM `wrong_questions` WHERE `status` = 'MASTERED'
                    """.trimIndent())
                } catch (_: Exception) {
                    // Tiếp tục an toàn nếu các bảng chưa có dữ liệu
                }
            }
        }

        /**
         * Migration từ Room Version 7 lên Version 8 (F11 - Thống kê học tập).
         * Thêm bảng `study_sessions` ghi nhận chính xác thời gian foreground của từng môn và phiên học.
         * Tự động ước lượng an toàn từ các bài kiểm tra đã làm trước đây (timeSpentSeconds).
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `study_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `activityType` TEXT NOT NULL,
                        `subjectId` TEXT NOT NULL,
                        `grade` INTEGER NOT NULL,
                        `startTimestamp` INTEGER NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `targetId` TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_sessions_startTimestamp` ON `study_sessions` (`startTimestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_sessions_grade_subjectId` ON `study_sessions` (`grade`, `subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_sessions_activityType` ON `study_sessions` (`activityType`)")

                // Ước lượng hợp lệ từ bảng `tests` đã có thời lượng timeSpentSeconds > 5s
                try {
                    db.execSQL("""
                        INSERT INTO `study_sessions` (`activityType`, `subjectId`, `grade`, `startTimestamp`, `durationSeconds`, `targetId`)
                        SELECT 'TEST', subjectId, grade, timestamp, timeSpentSeconds, id
                        FROM `tests`
                        WHERE isDraft = 0 AND timeSpentSeconds >= 5
                    """.trimIndent())
                } catch (_: Exception) {
                    // An toàn nếu bảng tests chưa có dữ liệu
                }
            }
        }

        /**
         * Migration từ Room Version 8 lên Version 9 (F13 - Tìm kiếm toàn ứng dụng & Chỉ mục FTS4).
         * Bổ sung các trường series, phanMon, topicId, tier, objectives cho các bảng hiện có;
         * Tạo bảng ảo FTS4 `search_fts` lưu trữ chỉ mục song song có dấu và không dấu;
         * Bảo toàn 100% dữ liệu người dùng.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Mở rộng cấu trúc các bảng thực thể
                db.execSQL("ALTER TABLE `chapters` ADD COLUMN `series` TEXT NOT NULL DEFAULT 'kntt'")
                db.execSQL("ALTER TABLE `chapters` ADD COLUMN `phanMon` TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE `lessons` ADD COLUMN `series` TEXT NOT NULL DEFAULT 'kntt'")
                db.execSQL("ALTER TABLE `lessons` ADD COLUMN `topicId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `lessons` ADD COLUMN `phanMon` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `lessons` ADD COLUMN `tier` TEXT NOT NULL DEFAULT 'tier_a'")
                db.execSQL("ALTER TABLE `lessons` ADD COLUMN `objectives` TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE `questions` ADD COLUMN `series` TEXT NOT NULL DEFAULT 'kntt'")
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `phanMon` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `questions` ADD COLUMN `topicId` TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE `flashcards` ADD COLUMN `series` TEXT NOT NULL DEFAULT 'kntt'")
                db.execSQL("ALTER TABLE `flashcards` ADD COLUMN `phanMon` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `flashcards` ADD COLUMN `topicId` TEXT NOT NULL DEFAULT ''")

                // 2. Tạo bảng ảo FTS4
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS `search_fts` USING fts4(
                        `targetId`, `type`, `grade`, `series`, `subjectId`, `phanMon`, `topicId`,
                        `title`, `titleNoAccent`, `content`, `contentNoAccent`,
                        notindexed=`targetId`, notindexed=`type`, notindexed=`grade`, notindexed=`series`, notindexed=`subjectId`, notindexed=`phanMon`, notindexed=`topicId`
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration từ Room Version 9 lên Version 10 (F16a - Tài khoản cục bộ & trạng thái phụ huynh).
         * Thêm bảng `accounts`, bảo toàn 100% dữ liệu học tập cũ.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `accounts` (
                        `uid` TEXT NOT NULL PRIMARY KEY,
                        `email` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `accountMode` TEXT NOT NULL,
                        `authProvider` TEXT NOT NULL,
                        `birthYear` INTEGER,
                        `isMinorUnder16` INTEGER NOT NULL,
                        `parentalConsentGranted` INTEGER NOT NULL,
                        `parentEmail` TEXT NOT NULL,
                        `parentalConsentTimestamp` INTEGER,
                        `sessionExpired` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `lastSyncedAt` INTEGER
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration từ Room Version 10 lên Version 11 (F16b - Hàng đợi Outbox & Trạng thái đồng bộ).
         * Thêm 2 bảng `sync_outbox` và `sync_state`, bảo toàn 100% dữ liệu học tập và tài khoản.
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_outbox` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `collectionName` TEXT NOT NULL,
                        `entityKey` TEXT NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `sourceId` TEXT NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        `deleted` INTEGER NOT NULL,
                        `clientUpdatedAt` INTEGER NOT NULL,
                        `retryCount` INTEGER NOT NULL,
                        `lastAttemptAt` INTEGER NOT NULL,
                        `lastError` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sync_outbox_collectionName_entityKey` ON `sync_outbox` (`collectionName`, `entityKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_outbox_status_retryCount` ON `sync_outbox` (`status`, `retryCount`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_outbox_createdAt` ON `sync_outbox` (`createdAt`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_state` (
                        `uid` TEXT NOT NULL PRIMARY KEY,
                        `lastDeltaCursor` TEXT NOT NULL,
                        `lastSyncedAt` INTEGER,
                        `lastSyncStatus` TEXT NOT NULL,
                        `lastErrorMessage` TEXT NOT NULL,
                        `syncWifiOnly` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration từ Room Version 11 lên Version 12 (F17a - Pipeline nội dung mở rộng).
         * Thêm bảng `content_versions` ghi nhận phiên bản nội dung đã nhập của từng gói
         * (packageId + contentVersion) để seeder biết gói nào cần nhập lại khi có bản mới.
         * Bảng chỉ chứa dữ liệu kỹ thuật của app, không chứa dữ liệu cá nhân.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `content_versions` (
                        `packageId` TEXT NOT NULL PRIMARY KEY,
                        `version` INTEGER NOT NULL,
                        `importedAtMillis` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration từ Room Version 12 lên Version 13 (F15b/D - Đánh dấu bài học & Ghi chú cá nhân).
         * Thêm 2 bảng `bookmarks` và `personal_notes`, bảo toàn 100% dữ liệu trước đó.
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `bookmarks` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `targetType` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `grade` INTEGER NOT NULL,
                        `subjectId` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_grade_subjectId` ON `bookmarks` (`grade`, `subjectId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `personal_notes` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `targetType` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `noteContent` TEXT NOT NULL,
                        `grade` INTEGER NOT NULL,
                        `subjectId` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_personal_notes_grade_subjectId` ON `personal_notes` (`grade`, `subjectId`)")
            }
        }

        /**
         * Migration từ Room Version 13 lên Version 14 (E - Thêm Index cho lessons và questions).
         * Thêm index `chapterId` cho bảng `lessons` và `lessonId` cho bảng `questions`,
         * tăng tốc độ truy vấn học tập và bảo toàn 100% dữ liệu.
         */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_lessons_chapterId` ON `lessons` (`chapterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_questions_lessonId` ON `questions` (`lessonId`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "thcs_hoc_tot_database.db"
                )
                    .addMigrations(
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
