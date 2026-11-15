package com.example

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Kiểm thử an toàn cho MIGRATION_10_11 (F16b - `sync_outbox` và `sync_state`):
 * 1. Nâng cấp schema từ v10 lên v11 thành công.
 * 2. Tạo bảng `sync_outbox` (kèm unique index collectionName+entityKey) và `sync_state`.
 * 3. Bảo toàn 100% dữ liệu tài khoản và học tập v10 trước đó.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigration10To11Test {

    @Test
    fun testMigration10To11CreatesSyncTablesAndPreservesExistingData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_10_11.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(10) {
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

        AppDatabase.MIGRATION_10_11.migrate(db)

        // 1. Kiểm tra cả 2 bảng mới đã tồn tại
        val tableNames = mutableSetOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type='table'").use { c ->
            while (c.moveToNext()) {
                tableNames.add(c.getString(0))
            }
        }
        assertTrue(tableNames.contains("sync_outbox"))
        assertTrue(tableNames.contains("sync_state"))

        // 2. Kiểm tra unique index trên (collectionName, entityKey)
        db.execSQL("""
            INSERT INTO `sync_outbox` (
                `collectionName`, `entityKey`, `sourceType`, `sourceId`, `payloadJson`,
                `deleted`, `clientUpdatedAt`, `retryCount`, `lastAttemptAt`, `lastError`, `status`, `createdAt`
            ) VALUES (
                'sync_lesson_progress', 'l8_1', '', '', '{"completed":true}',
                0, 1700000001000, 0, 0, '', 'PENDING', 1700000001000
            )
        """.trimIndent())

        try {
            db.execSQL("""
                INSERT INTO `sync_outbox` (
                    `collectionName`, `entityKey`, `sourceType`, `sourceId`, `payloadJson`,
                    `deleted`, `clientUpdatedAt`, `retryCount`, `lastAttemptAt`, `lastError`, `status`, `createdAt`
                ) VALUES (
                    'sync_lesson_progress', 'l8_1', '', '', '{"completed":false}',
                    0, 1700000002000, 0, 0, '', 'PENDING', 1700000002000
                )
            """.trimIndent())
            fail("Expected unique index constraint on (collectionName, entityKey)")
        } catch (_: SQLiteConstraintException) {
            // Đúng kỳ vọng
        }

        // 3. Kiểm tra đọc/ghi sync_state
        db.execSQL("""
            INSERT INTO `sync_state` VALUES (
                'pb_user_99', '2026-01-01 00:00:00.000Z', 1700000005000, 'SUCCESS', '', 1, 1700000005000
            )
        """.trimIndent())
        db.query("SELECT uid, syncWifiOnly, lastSyncStatus FROM sync_state WHERE uid='pb_user_99'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("pb_user_99", c.getString(0))
            assertEquals(1, c.getInt(1))
            assertEquals("SUCCESS", c.getString(2))
        }

        // 4. Dữ liệu v10 còn nguyên vẹn
        db.query("SELECT displayName, accountMode FROM accounts WHERE uid='pb_user_99'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Minh 8A", c.getString(0))
            assertEquals("AUTHENTICATED", c.getString(1))
        }
        db.query("SELECT isCompleted FROM lessons WHERE id='l8_1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(0))
        }
        db.query("SELECT SUM(xpAmount) FROM xp_events").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(10, c.getInt(0))
        }

        db.close()
    }
}
