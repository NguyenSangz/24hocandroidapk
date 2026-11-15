package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SearchFtsEntity

@Dao
interface SearchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SearchFtsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SearchFtsEntity)

    @Query("DELETE FROM search_fts WHERE targetId = :targetId")
    suspend fun deleteByTargetId(targetId: String)

    @Query("DELETE FROM search_fts WHERE type = :type")
    suspend fun deleteByType(type: String)

    @Query("DELETE FROM search_fts")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM search_fts")
    suspend fun getIndexCount(): Int

    @Query("SELECT COUNT(*) FROM search_fts WHERE targetId = :targetId")
    suspend fun hasTarget(targetId: String): Int

    /**
     * Tìm kiếm full-text bằng lệnh MATCH trên bảng ảo FTS4.
     * Trả về danh sách các bản ghi khớp với truy vấn từ khóa.
     */
    @Query("""
        SELECT rowid, targetId, type, grade, series, subjectId, phanMon, topicId, title, titleNoAccent, content, contentNoAccent
        FROM search_fts
        WHERE search_fts MATCH :matchQuery
    """)
    suspend fun searchAllMatches(matchQuery: String): List<SearchFtsEntity>

    /**
     * Tìm kiếm có phân trang LIMIT và OFFSET
     */
    @Query("""
        SELECT rowid, targetId, type, grade, series, subjectId, phanMon, topicId, title, titleNoAccent, content, contentNoAccent
        FROM search_fts
        WHERE search_fts MATCH :matchQuery
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchFtsPaged(matchQuery: String, limit: Int, offset: Int): List<SearchFtsEntity>
}
