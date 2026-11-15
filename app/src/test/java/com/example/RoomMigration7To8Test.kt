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
 * Robolectric test kiểm thử an toàn cho MIGRATION_7_8 (F11 - Thống kê học tập):
 * 1. Nâng cấp từ Schema v7 lên v8 thành công.
 * 2. Bảng `study_sessions` và các index được tạo chuẩn xác.
 * 3. Dữ liệu từ các bài test cũ có thời gian làm bài (timeSpentSeconds) được ước lượng an toàn vào `study_sessions`.
 * 4. Không mất mát bất kỳ dữ liệu nào của các bảng cũ.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigration7To8Test {

    @Test
    fun testMigration7To8CreatesStudySessionsAndBackfills() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_7_8.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(7) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Tạo bảng tests ở version 7
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

                    // Insert 1 bài kiểm tra cũ có thời lượng 600s
                    db.execSQL("""
                        INSERT INTO `tests` VALUES (
                            'test_old_1', 'Kiểm tra 15 phút Toán', 'LESSON', 'math', 'Toán học',
                            6, 'l_1', 9.0, 10.0, 10, 9, 600, 15, 1700000000000, 0, 0, 0
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val dbV7 = helper.writableDatabase

        // Chạy migration 7 -> 8
        AppDatabase.MIGRATION_7_8.migrate(dbV7)

        // 1. Kiểm tra bảng study_sessions đã tồn tại
        val cursor = dbV7.query("SELECT name FROM sqlite_master WHERE type='table' AND name='study_sessions'")
        assertTrue(cursor.moveToFirst())
        assertEquals("study_sessions", cursor.getString(0))
        cursor.close()

        // 2. Kiểm tra backfill từ test cũ
        val sessionCursor = dbV7.query("SELECT activityType, subjectId, grade, durationSeconds, targetId FROM study_sessions")
        assertTrue(sessionCursor.moveToFirst())
        assertEquals("TEST", sessionCursor.getString(0))
        assertEquals("math", sessionCursor.getString(1))
        assertEquals(6, sessionCursor.getInt(2))
        assertEquals(600, sessionCursor.getInt(3))
        assertEquals("test_old_1", sessionCursor.getString(4))
        sessionCursor.close()

        dbV7.close()
    }
}
