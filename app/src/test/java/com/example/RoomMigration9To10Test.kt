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
 * Kiểm thử an toàn cho MIGRATION_9_10 (F16a - Bảng `accounts` cục bộ):
 * 1. Nâng cấp schema từ v9 lên v10 thành công.
 * 2. Tạo bảng `accounts` đúng cột và ràng buộc.
 * 3. Bảo toàn 100% dữ liệu học tập (bài học, XP, phiên học) trước đó.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigration9To10Test {

    @Test
    fun testMigration9To10CreatesAccountsAndPreservesExistingStudyData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_9_10.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `chapters` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `subjectId` TEXT NOT NULL,
                            `grade` INTEGER NOT NULL,
                            `order` INTEGER NOT NULL,
                            `title` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `series` TEXT NOT NULL DEFAULT 'kntt',
                            `phanMon` TEXT NOT NULL DEFAULT ''
                        )
                    """.trimIndent())

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
                            `isCompleted` INTEGER NOT NULL,
                            `series` TEXT NOT NULL DEFAULT 'kntt',
                            `topicId` TEXT NOT NULL DEFAULT '',
                            `phanMon` TEXT NOT NULL DEFAULT '',
                            `tier` TEXT NOT NULL DEFAULT 'tier_a',
                            `objectives` TEXT NOT NULL DEFAULT ''
                        )
                    """.trimIndent())

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

                    db.execSQL("INSERT INTO `chapters` VALUES ('c1', 'toan', 7, 1, 'Số hữu tỉ', 'Mô tả', 'kntt', '')")
                    db.execSQL("INSERT INTO `lessons` VALUES ('l1', 'c1', 1, 'Bài 1', 'Tóm tắt', 'Ví dụ', 'Mẹo', '', '', 1, 'kntt', 'sh1', '', 'tier_a', '')")
                    db.execSQL("INSERT INTO `xp_events` (`eventType`, `eventKey`, `xpAmount`, `timestamp`, `metadata`) VALUES ('LESSON_COMPLETED', 'lesson_l1', 10, 1700000000000, 'Bài 1')")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val dbV9 = helper.writableDatabase

        AppDatabase.MIGRATION_9_10.migrate(dbV9)

        // 1. Kiểm tra bảng `accounts` đã được tạo
        val cursorTable = dbV9.query("SELECT name FROM sqlite_master WHERE type='table' AND name='accounts'")
        assertTrue(cursorTable.moveToFirst())
        assertEquals("accounts", cursorTable.getString(0))
        cursorTable.close()

        // 2. Có thể chèn và đọc hồ sơ tài khoản khách mặc định
        dbV9.execSQL("""
            INSERT INTO `accounts` (
                `uid`, `email`, `displayName`, `accountMode`, `authProvider`,
                `birthYear`, `isMinorUnder16`, `parentalConsentGranted`, `parentEmail`,
                `parentalConsentTimestamp`, `sessionExpired`, `createdAt`, `updatedAt`, `lastSyncedAt`
            ) VALUES (
                'guest_local', '', 'Học sinh THCS', 'GUEST', 'NONE',
                NULL, 0, 0, '', NULL, 0, 1700000000000, 1700000000000, NULL
            )
        """.trimIndent())

        val cursorAccount = dbV9.query("SELECT uid, displayName, accountMode FROM accounts WHERE uid='guest_local'")
        assertTrue(cursorAccount.moveToFirst())
        assertEquals("guest_local", cursorAccount.getString(0))
        assertEquals("Học sinh THCS", cursorAccount.getString(1))
        assertEquals("GUEST", cursorAccount.getString(2))
        cursorAccount.close()

        // 3. Dữ liệu học tập cũ còn nguyên vẹn
        val cursorLesson = dbV9.query("SELECT id, title, isCompleted FROM lessons WHERE id='l1'")
        assertTrue(cursorLesson.moveToFirst())
        assertEquals(1, cursorLesson.getInt(2))
        cursorLesson.close()

        val cursorXp = dbV9.query("SELECT SUM(xpAmount) FROM xp_events")
        assertTrue(cursorXp.moveToFirst())
        assertEquals(10, cursorXp.getInt(0))
        cursorXp.close()

        dbV9.close()
    }
}
