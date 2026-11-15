package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.dao.AttemptChapterRow
import com.example.data.local.dao.ChapterAccuracyStat
import com.example.data.local.dao.SubjectAccuracyStat
import com.example.data.local.dao.SubjectDurationStat
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.TestEntity
import com.example.domain.evaluator.StatsAggregator
import com.example.domain.evaluator.StatsTimeRange
import com.example.domain.evaluator.StatsTimeRangeHelper
import com.example.domain.evaluator.StrengthWeaknessAnalysis
import com.example.domain.evaluator.StrengthWeaknessAnalyzer
import com.example.domain.evaluator.TimeRangeBounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Interface cho StatsRepository (F11 - Thống kê học tập)
 */
interface StatsRepository {
    suspend fun recordStudySession(session: StudySessionEntity): Long
    fun getSessionsInPeriod(grade: Int, fromTimestamp: Long, toTimestamp: Long): Flow<List<StudySessionEntity>>
    fun getCompletedTestsInPeriod(grade: Int, subjectId: String, fromTimestamp: Long, toTimestamp: Long): Flow<List<TestEntity>>
    fun getAccuracyBySubjectInPeriod(grade: Int, fromTimestamp: Long, toTimestamp: Long): Flow<List<SubjectAccuracyStat>>
    fun getAccuracyByChapterInPeriod(grade: Int, subjectId: String, fromTimestamp: Long, toTimestamp: Long): Flow<List<ChapterAccuracyStat>>
    fun getTotalStudySeconds(grade: Int, fromTimestamp: Long, toTimestamp: Long): Flow<Long>
    fun getSubjectStudySeconds(grade: Int, subjectId: String, fromTimestamp: Long, toTimestamp: Long): Flow<Long>
    fun getDistinctStudyDaysCount(grade: Int, fromTimestamp: Long, toTimestamp: Long): Flow<Int>
    suspend fun getStrengthWeaknessAnalysis(grade: Int, subjectId: String = "all"): StrengthWeaknessAnalysis
    fun getSubjectStudyDurations(grade: Int, fromTimestamp: Long, toTimestamp: Long): Flow<List<SubjectDurationStat>>
}

/**
 * Triển khai StatsRepositoryImpl kết nối trực tiếp cơ sở dữ liệu Room.
 */
class StatsRepositoryImpl(
    private val database: AppDatabase
) : StatsRepository {

    override suspend fun recordStudySession(session: StudySessionEntity): Long {
        if (session.durationSeconds < 5) return 0L
        return database.studySessionDao().insertSession(session)
    }

    override fun getSessionsInPeriod(
        grade: Int,
        fromTimestamp: Long,
        toTimestamp: Long
    ): Flow<List<StudySessionEntity>> {
        return database.studySessionDao().getSessionsInPeriod(grade, fromTimestamp, toTimestamp)
    }

    override fun getCompletedTestsInPeriod(
        grade: Int,
        subjectId: String,
        fromTimestamp: Long,
        toTimestamp: Long
    ): Flow<List<TestEntity>> {
        return database.testDao().getCompletedTestsByGradeAndSubjectInPeriod(grade, subjectId, fromTimestamp, toTimestamp)
    }

    override fun getAccuracyBySubjectInPeriod(
        grade: Int,
        fromTimestamp: Long,
        toTimestamp: Long
    ): Flow<List<SubjectAccuracyStat>> {
        return database.attemptDao().getAccuracyBySubjectInPeriod(grade, fromTimestamp, toTimestamp)
    }

    override fun getAccuracyByChapterInPeriod(
        grade: Int,
        subjectId: String,
        fromTimestamp: Long,
        toTimestamp: Long
    ): Flow<List<ChapterAccuracyStat>> {
        return database.attemptDao().getAccuracyByChapterInPeriod(grade, subjectId, fromTimestamp, toTimestamp)
    }

    override fun getTotalStudySeconds(
        grade: Int,
        fromTimestamp: Long,
        toTimestamp: Long
    ): Flow<Long> {
        return database.studySessionDao().getTotalStudySeconds(grade, fromTimestamp, toTimestamp)
    }

    override fun getSubjectStudySeconds(
        grade: Int,
        subjectId: String,
        fromTimestamp: Long,
        toTimestamp: Long
    ): Flow<Long> {
        return database.studySessionDao().getSubjectStudySeconds(grade, subjectId, fromTimestamp, toTimestamp)
    }

    override fun getDistinctStudyDaysCount(
        grade: Int,
        fromTimestamp: Long,
        toTimestamp: Long
    ): Flow<Int> {
        return database.studySessionDao().getDistinctStudyDaysCount(grade, fromTimestamp, toTimestamp)
    }

    override fun getSubjectStudyDurations(
        grade: Int,
        fromTimestamp: Long,
        toTimestamp: Long
    ): Flow<List<SubjectDurationStat>> {
        return database.studySessionDao().getDurationBySubject(grade, fromTimestamp, toTimestamp)
    }

    override suspend fun getStrengthWeaknessAnalysis(
        grade: Int,
        subjectId: String
    ): StrengthWeaknessAnalysis {
        val rows = database.attemptDao().getRecentAttemptsWithChapter(grade, subjectId, limit = 500)
        val subjects = database.subjectDao().getAllSubjects().first()
        val subjectNames = subjects.associate { it.id to it.name }

        val answerRecords = rows.map { row ->
            StrengthWeaknessAnalyzer.AnswerRecord(
                questionId = row.questionId,
                isCorrect = row.isCorrect,
                timestamp = row.timestamp,
                chapterId = row.chapterId,
                chapterTitle = row.chapterTitle,
                subjectId = row.subjectId
            )
        }

        return StrengthWeaknessAnalyzer.analyze(answerRecords, subjectNames)
    }
}
