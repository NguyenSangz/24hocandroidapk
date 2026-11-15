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
 * Unit Test kiểm tra Room Migration từ Version 6 lên Version 7 (F10 - Gamification).
 * Xác minh tạo bảng xp_events, badge_unlocks và backfill dữ liệu an toàn.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigration6To7Test {

    @Test
    fun testMigration6To7CreatesTablesAndBackfills() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_6_7.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Tạo bảng tối thiểu của V6
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `lessons` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `chapterId` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `content` TEXT NOT NULL,
                            `orderIndex` INTEGER NOT NULL,
                            `isCompleted` INTEGER NOT NULL DEFAULT 0,
                            `completedAt` INTEGER
                        )
                    """.trimIndent())

                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `attempts` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `questionId` TEXT NOT NULL,
                            `isCorrect` INTEGER NOT NULL,
                            `score` REAL NOT NULL,
                            `timeSpentSeconds` INTEGER NOT NULL,
                            `userAnswer` TEXT NOT NULL,
                            `timestamp` INTEGER NOT NULL,
                            `usedHint` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())

                    db.execSQL("""
                        INSERT INTO `lessons` (`id`, `chapterId`, `title`, `content`, `orderIndex`, `isCompleted`, `completedAt`)
                        VALUES ('les_1', 'chap_1', 'Bài 1', 'Nội dung', 1, 1, 1700000000000)
                    """.trimIndent())

                    db.execSQL("""
                        INSERT INTO `attempts` (`questionId`, `isCorrect`, `score`, `timeSpentSeconds`, `userAnswer`, `timestamp`, `usedHint`)
                        VALUES ('q_1', 1, 1.0, 10, 'A', 1700000005000, 0)
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // Chạy migration 6 -> 7
        AppDatabase.MIGRATION_6_7.migrate(db)

        // Kiểm tra bảng xp_events tồn tại và có dữ liệu backfill
        val cursorXp = db.query("SELECT COUNT(*) FROM `xp_events`")
        assertTrue(cursorXp.moveToFirst())
        val xpCount = cursorXp.getInt(0)
        cursorXp.close()

        // 1 bài học (10 XP) + 1 câu đúng (2 XP) = ít nhất 2 sự kiện
        assertEquals(2, xpCount)

        // Kiểm tra bảng badge_unlocks tồn tại
        val cursorBadges = db.query("SELECT COUNT(*) FROM `badge_unlocks`")
        assertTrue(cursorBadges.moveToFirst())
        val badgeCount = cursorBadges.getInt(0)
        cursorBadges.close()
        assertEquals(0, badgeCount)

        db.close()
    }
}
