package com.example.data.seed

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Interface chịu trách nhiệm nạp dữ liệu nội dung học tập vào cơ sở dữ liệu Room.
 * Gọi trên luồng nền IO khi khởi động ứng dụng (Splash screen).
 */
interface ContentLoader {
    suspend fun loadIfEmpty(): Result<Unit>
}

/**
 * Triển khai ContentLoader sử dụng CurriculumDataSeeder và Room Database.
 */
class ContentLoaderImpl(
    private val context: Context,
    private val database: AppDatabase,
    private val seeder: CurriculumDataSeeder = CurriculumDataSeeder(context, database),
    private val searchIndexer: com.example.data.search.SearchIndexer = com.example.data.search.SearchIndexer(database)
) : ContentLoader {

    companion object {
        private const val TAG = "ContentLoader"
    }

    override suspend fun loadIfEmpty(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Kiểm tra và nạp dữ liệu chương trình học nếu chưa có...")
            seeder.seedDatabaseIfNeeded()
            // Dựng và đồng bộ chỉ mục FTS4 nền
            searchIndexer.syncAllIfNeeded()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi nạp dữ liệu bài học: ${e.message}", e)
            Result.failure(e)
        }
    }
}
