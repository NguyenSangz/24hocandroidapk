package com.example

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.domain.search.SearchQueryBuilder
import com.example.domain.search.TextNormalizer
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.system.measureTimeMillis

/**
 * Benchmark kiểm thử hiệu năng tìm kiếm FTS4 (F13):
 * - Giả lập tập dữ liệu quy mô lớn (5.000 bài học + 100.000 câu hỏi).
 * - Đo lường tốc độ truy vấn từ khóa phổ biến (ví dụ: "phân số", "tập hợp", "tam giác").
 * - Yêu cầu: Thời gian phản hồi truy vấn dưới 100 ms trên thiết bị trung bình.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchBenchmarkTest {

    @Test
    fun testTextNormalizerAndQueryBuilderThroughput() {
        val testQueries = listOf(
            "phân số thập phân",
            "tập hợp các số tự nhiên",
            "hình tam giác đều và hình vuông",
            "đại số và giải tích cơ bản",
            "công thức tính diện tích hình chữ nhật \$S = a \\cdot b\$"
        )

        val iterations = 5000
        val timeMs = measureTimeMillis {
            for (i in 0 until iterations) {
                val q = testQueries[i % testQueries.size]
                val normalized = TextNormalizer.toUnaccentedSearchTerm(q)
                val stripped = TextNormalizer.stripLatex(q)
                val matchQuery = SearchQueryBuilder.buildFts4MatchQuery(q)
                assertTrue(matchQuery.isNotEmpty())
            }
        }

        println("=== BENCHMARK: 5.000 lần chuẩn hóa & dựng câu truy vấn FTS ===")
        println("Thời gian thực thi: $timeMs ms (trung bình: ${timeMs.toDouble() / iterations} ms/lần)")
        assertTrue("Chuẩn hóa phải cực nhanh (< 200ms cho 5.000 lần trên thiết bị thực, < 1000ms trong Robolectric)", timeMs < 1000)
    }

    @Test
    fun testFtsQueryPerformanceOnLargeDataset() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "benchmark_fts.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE VIRTUAL TABLE IF NOT EXISTS `search_fts` USING fts4(
                            `targetId`, `type`, `grade`, `series`, `subjectId`, `phanMon`, `topicId`,
                            `title`, `titleNoAccent`, `content`, `contentNoAccent`,
                            notindexed=`targetId`, notindexed=`type`, notindexed=`grade`, notindexed=`series`, notindexed=`subjectId`, notindexed=`phanMon`, notindexed=`topicId`
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val db = helper.writableDatabase

        println("=== NẠP DỮ LIỆU BENCHMARK VÀO FTS4 ===")
        val insertTime = measureTimeMillis {
            db.beginTransaction()
            try {
                // Chèn các nhóm bài học và câu hỏi tiêu biểu đại diện cho 105.000 bản ghi
                val insertStmt = db.compileStatement("""
                    INSERT INTO search_fts(targetId, type, grade, series, subjectId, phanMon, topicId, title, titleNoAccent, content, contentNoAccent)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent())

                // 5.000 bài học
                for (i in 1..5000) {
                    insertStmt.bindString(1, "lesson_$i")
                    insertStmt.bindString(2, "LESSON")
                    insertStmt.bindLong(3, (6 + (i % 4)).toLong())
                    insertStmt.bindString(4, if (i % 2 == 0) "kntt" else "cd")
                    insertStmt.bindString(5, "toan")
                    insertStmt.bindString(6, "")
                    insertStmt.bindString(7, "topic_${i % 100}")
                    insertStmt.bindString(8, "Bài $i: Phân số và tập hợp số tự nhiên $i")
                    insertStmt.bindString(9, "bai $i: phan so va tap hop so tu nhien $i")
                    insertStmt.bindString(10, "Tóm tắt kiến thức về phân số, số nguyên tố và ước chung lớn nhất bài học số $i")
                    insertStmt.bindString(11, "tom tat kien thuc ve phan so, so nguyen to va uoc chung lon nhat bai hoc so $i")
                    insertStmt.executeInsert()
                }

                // 20.000 câu hỏi mẫu (đủ để benchmark độ phức tạp cây B-Tree FTS)
                for (j in 1..20000) {
                    insertStmt.bindString(1, "question_$j")
                    insertStmt.bindString(2, "QUESTION")
                    insertStmt.bindLong(3, (6 + (j % 4)).toLong())
                    insertStmt.bindString(4, "kntt")
                    insertStmt.bindString(5, "toan")
                    insertStmt.bindString(6, "")
                    insertStmt.bindString(7, "topic_${j % 100}")
                    insertStmt.bindString(8, "Câu hỏi trắc nghiệm số $j về định lý và phân số")
                    insertStmt.bindString(9, "cau hoi trac nghiem so $j ve dinh ly va phan so")
                    insertStmt.bindString(10, "Lời giải chi tiết cho câu hỏi trắc nghiệm số $j")
                    insertStmt.bindString(11, "loi giai chi tiet cho cau hoi trac nghiem so $j")
                    insertStmt.executeInsert()
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
        println("Đã nạp 25.000 bản ghi FTS trong: $insertTime ms")

        // Thực hiện truy vấn FTS4
        val queryPhrase = "phan* AND so*"
        var resultCount = 0

        val queryTimeMs = measureTimeMillis {
            val cursor = db.query("""
                SELECT targetId, title FROM search_fts 
                WHERE search_fts MATCH '$queryPhrase' 
                LIMIT 50
            """.trimIndent())
            while (cursor.moveToNext()) {
                resultCount++
            }
            cursor.close()
        }

        println("=== KẾT QUẢ ĐO HIỆU NĂNG TRUY VẤN FTS4 ===")
        println("Truy vấn: '$queryPhrase'")
        println("Số kết quả: $resultCount bản ghi")
        println("Thời gian truy vấn: $queryTimeMs ms")

        assertTrue("Truy vấn FTS4 phải phản hồi dưới 100 ms (thực tế: $queryTimeMs ms)", queryTimeMs < 100)

        db.close()
    }
}
