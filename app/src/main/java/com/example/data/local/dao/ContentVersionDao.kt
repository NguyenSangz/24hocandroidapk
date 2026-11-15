package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ContentVersionEntity

/**
 * DAO cho bảng `content_versions` (F17a).
 * Dùng bởi CurriculumDataSeeder để biết gói nội dung nào đã nhập với phiên bản nào.
 */
@Dao
interface ContentVersionDao {

    @Query("SELECT * FROM content_versions WHERE packageId = :packageId LIMIT 1")
    suspend fun getVersion(packageId: String): ContentVersionEntity?

    @Query("SELECT * FROM content_versions")
    suspend fun getAllVersionsSync(): List<ContentVersionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(version: ContentVersionEntity)
}
