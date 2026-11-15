package com.example

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomMigration13To14Test {

    @Test
    fun testMigration13To14CreatesIndexesAndPreservesData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_13_14.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(13) {
                override fun onCreate(db: SupportSQLiteDatabase) {
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
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `questions` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `lessonId` TEXT NOT NULL,
                            `type` TEXT NOT NULL,
                            `content` TEXT NOT NULL,
                            `optionsJson` TEXT NOT NULL,
                            `correctAnswer` TEXT NOT NULL,
                            `hint` TEXT NOT NULL,
                            `explanation` TEXT NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("INSERT INTO `lessons` VALUES ('l1', 'c1', 1, 'Bài 1', 'sum', '', '', '', '', 0)")
                    db.execSQL("INSERT INTO `questions` VALUES ('q1', 'l1', 'MULTIPLE_CHOICE', 'Q1', '[]', 'A', 'hint', 'exp')")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        AppDatabase.MIGRATION_13_14.migrate(db)

        val indexes = mutableSetOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type='index'").use { c ->
            while (c.moveToNext()) {
                indexes.add(c.getString(0))
            }
        }
        assertTrue(indexes.contains("index_lessons_chapterId"))
        assertTrue(indexes.contains("index_questions_lessonId"))

        db.close()
    }
}
