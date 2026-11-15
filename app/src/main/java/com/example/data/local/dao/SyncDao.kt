package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SyncOutboxEntity
import com.example.data.local.entity.SyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOutboxItem(item: SyncOutboxEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOutboxItems(items: List<SyncOutboxEntity>)

    @Query("SELECT * FROM sync_outbox WHERE collectionName = :collectionName AND entityKey = :entityKey LIMIT 1")
    suspend fun getOutboxItem(collectionName: String, entityKey: String): SyncOutboxEntity?

    @Query("""
        SELECT * FROM sync_outbox 
        WHERE status != 'FAILED_PERMANENT' AND retryCount < :maxRetries
        ORDER BY createdAt ASC, id ASC
        LIMIT :limit
    """)
    suspend fun getPendingOutboxBatch(maxRetries: Int = 6, limit: Int = 50): List<SyncOutboxEntity>

    @Query("SELECT * FROM sync_outbox ORDER BY createdAt ASC, id ASC")
    suspend fun getAllOutboxSync(): List<SyncOutboxEntity>

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE status != 'FAILED_PERMANENT'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE status != 'FAILED_PERMANENT'")
    suspend fun getPendingCountSync(): Int

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE status = 'FAILED_PERMANENT'")
    fun observePermanentFailedCount(): Flow<Int>

    @Query("""
        UPDATE sync_outbox 
        SET retryCount = retryCount + 1,
            lastAttemptAt = :attemptAt,
            lastError = :error,
            status = CASE WHEN retryCount + 1 >= :maxRetries THEN 'FAILED_PERMANENT' ELSE 'PENDING' END
        WHERE id IN (:ids)
    """)
    suspend fun markBatchFailed(ids: List<Long>, attemptAt: Long, error: String, maxRetries: Int = 6)

    @Query("""
        UPDATE sync_outbox
        SET retryCount = 0, lastError = '', status = 'PENDING'
        WHERE status = 'FAILED_PERMANENT'
    """)
    suspend fun resetPermanentFailedForRetry()

    @Query("DELETE FROM sync_outbox WHERE id IN (:ids)")
    suspend fun deleteOutboxByIds(ids: List<Long>)

    @Query("DELETE FROM sync_outbox WHERE collectionName = :collectionName AND entityKey = :entityKey")
    suspend fun deleteOutboxByKey(collectionName: String, entityKey: String)

    @Query("DELETE FROM sync_outbox")
    suspend fun clearAllOutbox()

    @Query("SELECT * FROM sync_state WHERE uid = :uid LIMIT 1")
    fun observeSyncState(uid: String): Flow<SyncStateEntity?>

    @Query("SELECT * FROM sync_state WHERE uid = :uid LIMIT 1")
    suspend fun getSyncStateSync(uid: String): SyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncState(state: SyncStateEntity)

    @Query("DELETE FROM sync_state WHERE uid = :uid")
    suspend fun deleteSyncState(uid: String)
}
