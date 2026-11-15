package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.BadgeUnlockEntity
import com.example.data.local.entity.XpEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GamificationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertXpEvent(event: XpEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertXpEvents(events: List<XpEventEntity>)

    @Query("SELECT COUNT(*) > 0 FROM xp_events WHERE eventKey = :eventKey LIMIT 1")
    suspend fun hasEventKey(eventKey: String): Boolean

    @Query("SELECT COALESCE(SUM(xpAmount), 0) FROM xp_events")
    fun getTotalXp(): Flow<Int>

    @Query("SELECT COALESCE(SUM(xpAmount), 0) FROM xp_events")
    suspend fun getTotalXpSync(): Int

    @Query("SELECT COALESCE(SUM(xpAmount), 0) FROM xp_events WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp AND xpAmount > 0")
    suspend fun getXpEarnedBetween(startTimestamp: Long, endTimestamp: Long): Int

    @Query("SELECT * FROM xp_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentXpEvents(limit: Int = 50): Flow<List<XpEventEntity>>

    @Query("SELECT * FROM xp_events ORDER BY timestamp DESC")
    fun getAllXpEvents(): Flow<List<XpEventEntity>>

    @Query("SELECT * FROM xp_events ORDER BY timestamp ASC")
    suspend fun getAllXpEventsSync(): List<XpEventEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadgeUnlock(badge: BadgeUnlockEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadgeUnlocks(badges: List<BadgeUnlockEntity>)

    @Query("SELECT badgeId FROM badge_unlocks")
    fun getAllUnlockedBadgeIds(): Flow<List<String>>

    @Query("SELECT badgeId FROM badge_unlocks")
    suspend fun getAllUnlockedBadgeIdsSync(): List<String>

    @Query("SELECT * FROM badge_unlocks ORDER BY unlockedAt DESC")
    fun getAllBadgeUnlocks(): Flow<List<BadgeUnlockEntity>>

    @Query("SELECT * FROM badge_unlocks ORDER BY unlockedAt ASC")
    suspend fun getAllBadgeUnlocksSync(): List<BadgeUnlockEntity>

    @Query("SELECT COUNT(*) > 0 FROM badge_unlocks WHERE badgeId = :badgeId LIMIT 1")
    suspend fun isBadgeUnlocked(badgeId: String): Boolean

    @Query("SELECT COUNT(*) FROM attempts WHERE questionId = :questionId AND timestamp >= :startTimestamp AND timestamp < :endTimestamp")
    suspend fun getQuestionAttemptsCountBetween(questionId: String, startTimestamp: Long, endTimestamp: Long): Int

    /**
     * Lấy các ngày dương lịch (dưới dạng mốc timestamp) có hoạt động học tập (từ xp_events và attempts và review_logs)
     */
    @Query("""
        SELECT DISTINCT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') AS dayStr
        FROM xp_events
        WHERE xpAmount > 0
        UNION
        SELECT DISTINCT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') AS dayStr
        FROM attempts
        UNION
        SELECT DISTINCT strftime('%Y-%m-%d', reviewTimestamp / 1000, 'unixepoch', 'localtime') AS dayStr
        FROM review_logs
        ORDER BY dayStr ASC
    """)
    fun getDistinctActivityDates(): Flow<List<String>>

    @Query("""
        SELECT DISTINCT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') AS dayStr
        FROM xp_events
        WHERE xpAmount > 0
        UNION
        SELECT DISTINCT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') AS dayStr
        FROM attempts
        UNION
        SELECT DISTINCT strftime('%Y-%m-%d', reviewTimestamp / 1000, 'unixepoch', 'localtime') AS dayStr
        FROM review_logs
        ORDER BY dayStr ASC
    """)
    suspend fun getDistinctActivityDatesSync(): List<String>
}
