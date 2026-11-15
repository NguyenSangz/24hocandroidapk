package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Thống kê thời lượng học theo từng môn
 */
data class SubjectDurationStat(
    val subjectId: String,
    val totalSeconds: Long
)

/**
 * Thống kê thời lượng học theo ngày và theo môn
 */
data class DaySubjectDurationStat(
    val dayTimestamp: Long,
    val subjectId: String,
    val totalSeconds: Long
)

/**
 * Thống kê số câu đúng/sai theo chương và môn
 */
data class ChapterAccuracyStat(
    val chapterId: String,
    val subjectId: String,
    val totalAnswered: Int,
    val totalCorrect: Int
)

/**
 * Thống kê số câu đúng/sai theo môn
 */
data class SubjectAccuracyStat(
    val subjectId: String,
    val totalAnswered: Int,
    val totalCorrect: Int
)

/**
 * DAO xử lý truy vấn dữ liệu học tập tổng hợp bằng SQL hiệu năng cao (F11).
 */
@Dao
interface StudySessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<StudySessionEntity>)

    @Query("""
        SELECT * FROM study_sessions 
        WHERE grade = :grade AND startTimestamp >= :fromTimestamp AND startTimestamp <= :toTimestamp
        ORDER BY startTimestamp DESC
    """)
    fun getSessionsInPeriod(grade: Int, fromTimestamp: Long, toTimestamp: Long): Flow<List<StudySessionEntity>>

    @Query("""
        SELECT * FROM study_sessions 
        WHERE grade = :grade AND subjectId = :subjectId AND startTimestamp >= :fromTimestamp AND startTimestamp <= :toTimestamp
        ORDER BY startTimestamp DESC
    """)
    fun getSessionsBySubjectInPeriod(grade: Int, subjectId: String, fromTimestamp: Long, toTimestamp: Long): Flow<List<StudySessionEntity>>

    @Query("""
        SELECT COALESCE(SUM(durationSeconds), 0) FROM study_sessions 
        WHERE grade = :grade AND startTimestamp >= :fromTimestamp AND startTimestamp <= :toTimestamp
    """)
    fun getTotalStudySeconds(grade: Int, fromTimestamp: Long, toTimestamp: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(durationSeconds), 0) FROM study_sessions 
        WHERE grade = :grade AND subjectId = :subjectId AND startTimestamp >= :fromTimestamp AND startTimestamp <= :toTimestamp
    """)
    fun getSubjectStudySeconds(grade: Int, subjectId: String, fromTimestamp: Long, toTimestamp: Long): Flow<Long>

    @Query("""
        SELECT subjectId, COALESCE(SUM(durationSeconds), 0) AS totalSeconds
        FROM study_sessions
        WHERE grade = :grade AND startTimestamp >= :fromTimestamp AND startTimestamp <= :toTimestamp
        GROUP BY subjectId
    """)
    fun getDurationBySubject(grade: Int, fromTimestamp: Long, toTimestamp: Long): Flow<List<SubjectDurationStat>>

    @Query("""
        SELECT COUNT(DISTINCT (startTimestamp / 86400000)) FROM study_sessions 
        WHERE grade = :grade AND startTimestamp >= :fromTimestamp AND startTimestamp <= :toTimestamp
    """)
    fun getDistinctStudyDaysCount(grade: Int, fromTimestamp: Long, toTimestamp: Long): Flow<Int>
}
