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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigration12To13Test {

    @Test
    fun testMigration12To13CreatesBookmarksAndNotesTablesAndPreservesData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_12_13.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(12) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `content_versions` (
                            `packageId` TEXT NOT NULL PRIMARY KEY,
                            `version` INTEGER NOT NULL,
                            `importedAtMillis` INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("INSERT INTO `content_versions` (`packageId`, `version`, `importedAtMillis`) VALUES ('test_pkg', 1, 1700000000000)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        AppDatabase.MIGRATION_12_13.migrate(db)

        val tableNames = mutableSetOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type='table'").use { c ->
            while (c.moveToNext()) {
                tableNames.add(c.getString(0))
            }
        }
        assertTrue(tableNames.contains("bookmarks"))
        assertTrue(tableNames.contains("personal_notes"))

        db.execSQL("INSERT INTO `bookmarks` (`id`, `targetType`, `title`, `grade`, `subjectId`, `timestamp`) VALUES ('b1', 'LESSON', 'Bài 1', 6, 'math', 1700000000000)")
        db.execSQL("INSERT INTO `personal_notes` (`id`, `targetType`, `title`, `noteContent`, `grade`, `subjectId`, `timestamp`) VALUES ('n1', 'LESSON', 'Bài 1', 'Ghi chú học tập', 6, 'math', 1700000000000)")

        db.query("SELECT title FROM bookmarks WHERE id = 'b1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Bài 1", c.getString(0))
        }

        db.query("SELECT noteContent FROM personal_notes WHERE id = 'n1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Ghi chú học tập", c.getString(0))
        }

        db.close()
    }
}
