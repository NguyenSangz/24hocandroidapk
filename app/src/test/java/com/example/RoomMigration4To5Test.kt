package com.example

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit Test kiểm tra Room Migration từ Version 4 lên Version 5 (F08).
 * Bảo toàn 100% dữ liệu bảng wrong_questions cũ và chuyển đổi trạng thái chính xác.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigration4To5Test {

    @Test
    fun testMigration4To5PreservesDataAndUpgradesSchema() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_4_5.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Tạo bảng wrong_questions phiên bản 4
                    db.execSQL("""
                        CREATE TABLE `wrong_questions` (
                            `questionId` TEXT NOT NULL PRIMARY KEY,
                            `lessonId` TEXT NOT NULL,
                            `lastWrongAnswer` TEXT NOT NULL,
                            `errorCount` INTEGER NOT NULL,
                            `timestamp` INTEGER NOT NULL,
                            `isResolved` INTEGER NOT NULL
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // Sẽ test migrate riêng
                }
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        // Chèn dữ liệu phiên bản 4
        db.execSQL("""
            INSERT INTO `wrong_questions` (`questionId`, `lessonId`, `lastWrongAnswer`, `errorCount`, `timestamp`, `isResolved`)
            VALUES ('q_v4_1', 'les_old_1', 'Đáp án sai A', 2, 1700000000000, 0)
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `wrong_questions` (`questionId`, `lessonId`, `lastWrongAnswer`, `errorCount`, `timestamp`, `isResolved`)
            VALUES ('q_v4_2', 'les_old_2', 'Đáp án sai B', 1, 1700001000000, 1)
        """.trimIndent())

        // Thực hiện chạy Migration 4 -> 5
        AppDatabase.MIGRATION_4_5.migrate(db)

        // Kiểm tra dữ liệu sau khi migrate
        val cursor = db.query("SELECT * FROM `wrong_questions` ORDER BY `questionId` ASC")
        assertNotNull(cursor)
        assertEquals(2, cursor.count)

        // Bản ghi 1: chưa resolved -> NEEDS_REVIEW
        assertTrue(cursor.moveToNext())
        val q1Id = cursor.getString(cursor.getColumnIndexOrThrow("questionId"))
        val q1LessonId = cursor.getString(cursor.getColumnIndexOrThrow("lessonId"))
        val q1WrongAnswer = cursor.getString(cursor.getColumnIndexOrThrow("lastWrongAnswer"))
        val q1ErrorCount = cursor.getInt(cursor.getColumnIndexOrThrow("errorCount"))
        val q1Status = cursor.getString(cursor.getColumnIndexOrThrow("status"))
        val q1Consecutive = cursor.getInt(cursor.getColumnIndexOrThrow("consecutiveCorrectCount"))

        assertEquals("q_v4_1", q1Id)
        assertEquals("les_old_1", q1LessonId)
        assertEquals("Đáp án sai A", q1WrongAnswer)
        assertEquals(2, q1ErrorCount)
        assertEquals("NEEDS_REVIEW", q1Status)
        assertEquals(0, q1Consecutive)

        // Bản ghi 2: đã resolved -> MASTERED
        assertTrue(cursor.moveToNext())
        val q2Id = cursor.getString(cursor.getColumnIndexOrThrow("questionId"))
        val q2LessonId = cursor.getString(cursor.getColumnIndexOrThrow("lessonId"))
        val q2WrongAnswer = cursor.getString(cursor.getColumnIndexOrThrow("lastWrongAnswer"))
        val q2ErrorCount = cursor.getInt(cursor.getColumnIndexOrThrow("errorCount"))
        val q2Status = cursor.getString(cursor.getColumnIndexOrThrow("status"))

        assertEquals("q_v4_2", q2Id)
        assertEquals("les_old_2", q2LessonId)
        assertEquals("Đáp án sai B", q2WrongAnswer)
        assertEquals(1, q2ErrorCount)
        assertEquals("MASTERED", q2Status)

        cursor.close()
        db.close()
        context.deleteDatabase(dbName)
    }
}
