package com.example

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric test kiểm thử an toàn cho MIGRATION_8_9 (F13 - Tìm kiếm toàn ứng dụng & Chỉ mục FTS4):
 * 1. Nâng cấp từ Schema v8 lên v9 thành công.
 * 2. Bảng ảo FTS4 `search_fts` được tạo chuẩn xác.
 * 3. Các cột series, phanMon, topicId, tier, objectives được bổ sung vào các bảng cũ.
 * 4. Không mất mát bất kỳ dữ liệu người dùng nào đã có từ trước.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigration8To9Test {

    @Test
    fun testMigration8To9CreatesSearchFtsAndPreservesData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_8_9.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(8) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Tạo bảng chapters ở version 8
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `chapters` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `subjectId` TEXT NOT NULL,
                            `grade` INTEGER NOT NULL,
                            `order` INTEGER NOT NULL,
                            `title` TEXT NOT NULL,
                            `description` TEXT NOT NULL
                        )
                    """.trimIndent())

                    // Tạo bảng lessons ở version 8
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `lessons` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `chapterId` TEXT NOT NULL,
                            `order` INTEGER NOT NULL,
                            `title` TEXT NOT NULL,
                            `summary` TEXT NOT NULL,
                            `examples` TEXT NOT NULL,
                            `tips` TEXT NOT NULL,
                            `formulasLatex` TEXT NOT NULL,
                            `tablesJson` TEXT NOT NULL,
                            `isCompleted` INTEGER NOT NULL
                        )
                    """.trimIndent())

                    // Tạo bảng questions ở version 8
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `questions` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `lessonId` TEXT NOT NULL,
                            `type` TEXT NOT NULL,
                            `content` TEXT NOT NULL,
                            `optionsJson` TEXT NOT NULL,
                            `correctAnswer` TEXT NOT NULL,
                            `hint` TEXT NOT NULL,
                            `explanation` TEXT NOT NULL,
                            `difficulty` INTEGER NOT NULL
                        )
                    """.trimIndent())

                    // Tạo bảng flashcards ở version 8
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `flashcards` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `deckId` TEXT NOT NULL,
                            `grade` INTEGER NOT NULL,
                            `subjectId` TEXT NOT NULL,
                            `front` TEXT NOT NULL,
                            `back` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `example` TEXT,
                            `notes` TEXT,
                            `word` TEXT,
                            `partOfSpeech` TEXT,
                            `phonetic` TEXT,
                            `meaning` TEXT,
                            `exampleSentence` TEXT,
                            `voiceCode` TEXT,
                            `box` INTEGER NOT NULL,
                            `intervalDays` INTEGER NOT NULL,
                            `nextReviewTimestamp` INTEGER NOT NULL,
                            `easeFactor` REAL NOT NULL,
                            `repetitions` INTEGER NOT NULL
                        )
                    """.trimIndent())

                    // Chèn dữ liệu mẫu vào v8
                    db.execSQL("INSERT INTO `chapters` VALUES ('c1', 'toan', 6, 1, 'Chương I', 'Mô tả')")
                    db.execSQL("INSERT INTO `lessons` VALUES ('l1', 'c1', 1, 'Bài 1: Tập hợp', 'Tóm tắt', 'Ví dụ', 'Mẹo', '', '', 1)")
                    db.execSQL("INSERT INTO `questions` VALUES ('q1', 'l1', 'MULTIPLE_CHOICE', 'Đề câu 1', '[]', 'A', '', '', 1)")
                    db.execSQL("INSERT INTO `flashcards` VALUES ('fc1', 'deck1', 6, 'toan', 'Trước', 'Sau', 'Chung', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'en-US', 1, 1, 0, 2.5, 0)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val dbV8 = helper.writableDatabase

        // Chạy migration 8 -> 9
        AppDatabase.MIGRATION_8_9.migrate(dbV8)

        // 1. Kiểm tra bảng ảo search_fts đã được tạo
        val cursorFts = dbV8.query("SELECT name FROM sqlite_master WHERE name='search_fts'")
        assertTrue(cursorFts.moveToFirst())
        assertEquals("search_fts", cursorFts.getString(0))
        cursorFts.close()

        // 2. Kiểm tra dữ liệu cũ trong lessons vẫn nguyên vẹn và có cột mới
        val cursorLesson = dbV8.query("SELECT id, title, isCompleted, series, topicId, tier FROM lessons WHERE id='l1'")
        assertTrue(cursorLesson.moveToFirst())
        assertEquals("l1", cursorLesson.getString(0))
        assertEquals("Bài 1: Tập hợp", cursorLesson.getString(1))
        assertEquals(1, cursorLesson.getInt(2))
        assertEquals("kntt", cursorLesson.getString(3)) // default series
        assertEquals("", cursorLesson.getString(4))
        assertEquals("tier_a", cursorLesson.getString(5)) // default tier
        cursorLesson.close()

        // 3. Kiểm tra chèn và truy vấn MATCH trên search_fts
        dbV8.execSQL("""
            INSERT INTO search_fts(targetId, type, grade, series, subjectId, phanMon, topicId, title, titleNoAccent, content, contentNoAccent)
            VALUES ('l1', 'LESSON', 6, 'kntt', 'toan', '', 'tap-hop', 'Bài 1: Tập hợp', 'bai 1: tap hop', 'Khái niệm tập hợp', 'khai niem tap hop')
        """.trimIndent())

        val cursorMatch = dbV8.query("SELECT targetId, title FROM search_fts WHERE search_fts MATCH 'tap*'")
        assertTrue(cursorMatch.moveToFirst())
        assertEquals("l1", cursorMatch.getString(0))
        cursorMatch.close()

        dbV8.close()
    }
}
