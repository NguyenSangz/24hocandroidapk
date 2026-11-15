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
 * Kiểm thử an toàn cho MIGRATION_11_12 (F17a - bảng `content_versions`):
 * 1. Nâng cấp schema từ v11 lên v12 thành công.
 * 2. Tạo bảng `content_versions` (packageId là PRIMARY KEY).
 * 3. Bảo toàn 100% dữ liệu học tập và tài khoản v11 trước đó.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigration11To12Test {

    @Test
    fun testMigration11To12CreatesContentVersionsTableAndPreservesExistingData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_11_12.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(11) {
                override fun onCreate(db: SupportSQLiteDatabase) {
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

                    db.execSQL("""
                        INSERT INTO `accounts` VALUES (
                            'pb_user_99', 'minh@example.com', 'Minh 8A', 'AUTHENTICATED', 'EMAIL',
                            2012, 1, 1, 'ph@example.com', 1700000000000, 0,
                            1700000000000, 1700000000000, NULL
                        )
                    """.trimIndent())
                    db.execSQL("INSERT INTO `lessons` VALUES ('l8_1', 'c8_1', 1, 'Hằng đẳng thức', 'Tóm tắt', '', '', '', '', 1, 'kntt', '', '', 'tier_a', '')")
                    db.execSQL("INSERT INTO `xp_events` (`eventType`, `eventKey`, `xpAmount`, `timestamp`, `metadata`) VALUES ('LESSON_COMPLETED', 'lesson_l8_1', 10, 1700000000000, 'Hằng đẳng thức')")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        AppDatabase.MIGRATION_11_12.migrate(db)

        // 1. Bảng content_versions đã tồn tại
        val tableNames = mutableSetOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type='table'").use { c ->
            while (c.moveToNext()) {
                tableNames.add(c.getString(0))
            }
        }
        assertTrue(tableNames.contains("content_versions"))

        // 2. Ghi nhận được phiên bản nội dung; nhập lại cùng packageId ghi đè (REPLACE)
        db.execSQL("INSERT INTO `content_versions` (`packageId`, `version`, `importedAtMillis`) VALUES ('gdcd_kntt_l6_v1', 1, 1700000000000)")
        db.execSQL("INSERT OR REPLACE INTO `content_versions` (`packageId`, `version`, `importedAtMillis`) VALUES ('gdcd_kntt_l6_v1', 2, 1700000001000)")
        db.query("SELECT version FROM content_versions WHERE packageId = 'gdcd_kntt_l6_v1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(2, c.getInt(0))
        }

        // 3. Dữ liệu cũ vẫn còn nguyên vẹn
        db.query("SELECT COUNT(*) FROM accounts").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM lessons WHERE isCompleted = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM xp_events").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }

        db.close()
    }
}
