package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TestAnswerEntity
import com.example.data.local.entity.TestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTest(test: TestEntity)

    @Update
    suspend fun updateTest(test: TestEntity)

    @Query("SELECT * FROM tests WHERE id = :id LIMIT 1")
    suspend fun getTestById(id: String): TestEntity?

    @Query("SELECT * FROM tests WHERE id = :id LIMIT 1")
    fun getTestFlowById(id: String): Flow<TestEntity?>

    /**
     * Tìm bài kiểm tra đang làm dở (isDraft = 1) của môn và lớp tương ứng
     */
    @Query("""
        SELECT * FROM tests 
        WHERE isDraft = 1 AND grade = :grade AND (:subjectId = 'all' OR subjectId = :subjectId)
        ORDER BY timestamp DESC LIMIT 1
    """)
    suspend fun getActiveDraftTest(grade: Int, subjectId: String = "all"): TestEntity?

    /**
     * Lấy toàn bộ các bài kiểm tra đã hoàn thành (isDraft = 0), xếp theo thời gian mới nhất
     */
    @Query("SELECT * FROM tests WHERE isDraft = 0 ORDER BY timestamp DESC")
    fun getAllCompletedTests(): Flow<List<TestEntity>>

    @Query("SELECT * FROM tests ORDER BY timestamp DESC")
    suspend fun getAllTestsSync(): List<TestEntity>

    /**
     * Lấy các bài kiểm tra đã hoàn thành theo lớp và môn
     */
    @Query("""
        SELECT * FROM tests 
        WHERE isDraft = 0 AND grade = :grade AND (:subjectId = 'all' OR subjectId = :subjectId)
        ORDER BY timestamp DESC
    """)
    fun getCompletedTestsByGradeAndSubject(grade: Int, subjectId: String = "all"): Flow<List<TestEntity>>

    @Query("""
        SELECT * FROM tests 
        WHERE isDraft = 0 AND grade = :grade 
          AND (:subjectId = 'all' OR subjectId = :subjectId)
          AND timestamp >= :fromTimestamp AND timestamp <= :toTimestamp
        ORDER BY timestamp ASC
    """)
    fun getCompletedTestsByGradeAndSubjectInPeriod(
        grade: Int,
        subjectId: String = "all",
        fromTimestamp: Long,
        toTimestamp: Long
    ): Flow<List<TestEntity>>

    @Query("DELETE FROM tests WHERE id = :id")
    suspend fun deleteTestById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<TestAnswerEntity>)

    @Query("SELECT * FROM test_answers WHERE testId = :testId ORDER BY questionIndex ASC")
    suspend fun getAnswersForTest(testId: String): List<TestAnswerEntity>

    @Query("SELECT * FROM test_answers WHERE testId = :testId ORDER BY questionIndex ASC")
    fun getAnswersFlowForTest(testId: String): Flow<List<TestAnswerEntity>>

    @Update
    suspend fun updateAnswer(answer: TestAnswerEntity)

    @Query("DELETE FROM test_answers WHERE testId = :testId")
    suspend fun deleteAnswersByTestId(testId: String)
}
