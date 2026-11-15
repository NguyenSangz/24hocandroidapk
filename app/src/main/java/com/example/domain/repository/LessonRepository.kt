package com.example.domain.repository

import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.SubjectEntity
import com.example.domain.model.ChapterWithProgress
import com.example.domain.model.LessonFontSize
import com.example.domain.model.LessonNavigationInfo
import kotlinx.coroutines.flow.Flow

/**
 * Domain interface quản lý môn học, chương và bài học (F04)
 */
interface LessonRepository {
    /**
     * Lấy thông tin môn học theo id
     */
    suspend fun getSubjectById(subjectId: String): SubjectEntity?

    /**
     * Luồng danh sách môn học
     */
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    /**
     * Luồng danh sách chương học theo lớp và môn
     */
    fun getChapters(grade: Int, subjectId: String): Flow<List<ChapterEntity>>

    /**
     * Luồng các chương kèm bài học và tiến độ hoàn thành theo lớp và môn
     */
    fun getChaptersWithProgress(grade: Int, subjectId: String): Flow<List<ChapterWithProgress>>

    /**
     * Lấy danh sách bài học theo chương
     */
    fun getLessonsByChapter(chapterId: String): Flow<List<LessonEntity>>

    /**
     * Lấy chi tiết bài học theo id (dưới dạng suspend hoặc Flow)
     */
    suspend fun getLessonById(lessonId: String): LessonEntity?
    fun getLessonFlow(lessonId: String): Flow<LessonEntity?>

    /**
     * Lấy thông tin điều hướng bài trước / bài sau xuyên suốt các chương của môn
     */
    suspend fun getLessonNavigation(grade: Int, subjectId: String, currentLessonId: String): LessonNavigationInfo

    /**
     * Tìm bài học chưa hoàn thành đầu tiên trong môn học để phục vụ nút "Tiếp tục học"
     */
    suspend fun getFirstUncompletedLessonId(grade: Int, subjectId: String): String?

    /**
     * Cài đặt cỡ chữ hiển thị bài học
     */
    val lessonFontSize: Flow<LessonFontSize>
    suspend fun setLessonFontSize(fontSize: LessonFontSize)
}
