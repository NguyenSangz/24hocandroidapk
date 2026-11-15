package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.CurriculumRepository
import com.example.data.repository.WrongQuestionRepository
import com.example.domain.model.WrongBankSort
import com.example.domain.model.WrongBankStats
import com.example.domain.model.WrongQuestionDetail
import com.example.domain.model.WrongQuestionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Trạng thái UI tổng thể của màn hình Ngân hàng câu sai (F08)
 */
data class WrongBankUiState(
    val isLoading: Boolean = true,
    val selectedGrade: Int = 6,
    val stats: WrongBankStats = WrongBankStats(),
    val rawQuestions: List<WrongQuestionDetail> = emptyList(),
    val filteredQuestions: List<WrongQuestionDetail> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val chapters: List<ChapterEntity> = emptyList(),
    val selectedSubjectId: String = "all",
    val selectedChapterId: String = "all",
    val selectedStatus: String = "all", // "all", "NEEDS_REVIEW", "CONSOLIDATING", "MASTERED"
    val selectedQuestionType: String = "all",
    val sortOrder: WrongBankSort = WrongBankSort.BY_DUE_DATE,
    val selectedQuestionForDetail: WrongQuestionDetail? = null,
    val questionToDelete: WrongQuestionDetail? = null,
    val notificationMessage: String? = null
)

class WrongBankViewModel(
    private val wrongQuestionRepository: WrongQuestionRepository,
    private val curriculumRepository: CurriculumRepository,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WrongBankUiState())
    val uiState: StateFlow<WrongBankUiState> = _uiState.asStateFlow()

    val selectedGrade: StateFlow<Int> = userPreferencesManager.selectedGrade
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)

    init {
        viewModelScope.launch {
            userPreferencesManager.selectedGrade.collect { grade ->
                _uiState.value = _uiState.value.copy(selectedGrade = grade)
                loadData(grade)
            }
        }
    }

    fun refresh() {
        val grade = _uiState.value.selectedGrade
        loadData(grade)
    }

    private fun loadData(grade: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Nạp danh sách môn học
            val subjectsList = curriculumRepository.allSubjects.firstOrNull() ?: emptyList()

            // Nạp chi tiết câu sai
            val details = wrongQuestionRepository.getWrongQuestionDetails(grade)

            // Tính toán thống kê
            val needsReview = details.count { it.entity.status == WrongQuestionStatus.NEEDS_REVIEW.code }
            val consolidating = details.count { it.entity.status == WrongQuestionStatus.CONSOLIDATING.code }
            val mastered = details.count { it.entity.status == WrongQuestionStatus.MASTERED.code }
            val dueReview = details.count { it.isDueForReview }

            val subjectMap = mutableMapOf<String, Int>()
            details.forEach { d ->
                val sId = d.entity.subjectId.ifBlank { "other" }
                subjectMap[sId] = (subjectMap[sId] ?: 0) + 1
            }

            val stats = WrongBankStats(
                needsReviewCount = needsReview,
                consolidatingCount = consolidating,
                masteredCount = mastered,
                dueReviewCount = dueReview,
                subjectCounts = subjectMap
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                stats = stats,
                rawQuestions = details,
                subjects = subjectsList
            )

            updateFilteredQuestions()
            loadChaptersForSelectedSubject()
        }
    }

    private fun loadChaptersForSelectedSubject() {
        val subjectId = _uiState.value.selectedSubjectId
        val grade = _uiState.value.selectedGrade
        if (subjectId == "all") {
            _uiState.value = _uiState.value.copy(chapters = emptyList(), selectedChapterId = "all")
            return
        }
        viewModelScope.launch {
            val chapterList = curriculumRepository.getChapters(grade, subjectId).firstOrNull() ?: emptyList()
            _uiState.value = _uiState.value.copy(chapters = chapterList)
        }
    }

    fun setSubjectFilter(subjectId: String) {
        _uiState.value = _uiState.value.copy(
            selectedSubjectId = subjectId,
            selectedChapterId = "all"
        )
        loadChaptersForSelectedSubject()
        updateFilteredQuestions()
    }

    fun setChapterFilter(chapterId: String) {
        _uiState.value = _uiState.value.copy(selectedChapterId = chapterId)
        updateFilteredQuestions()
    }

    fun setStatusFilter(status: String) {
        _uiState.value = _uiState.value.copy(selectedStatus = status)
        updateFilteredQuestions()
    }

    fun setQuestionTypeFilter(type: String) {
        _uiState.value = _uiState.value.copy(selectedQuestionType = type)
        updateFilteredQuestions()
    }

    fun setSortOrder(sort: WrongBankSort) {
        _uiState.value = _uiState.value.copy(sortOrder = sort)
        updateFilteredQuestions()
    }

    private fun updateFilteredQuestions() {
        val current = _uiState.value
        val now = System.currentTimeMillis()

        var filtered = current.rawQuestions.filter { detail ->
            val matchSubject = current.selectedSubjectId == "all" || detail.entity.subjectId == current.selectedSubjectId
            val matchChapter = current.selectedChapterId == "all" || detail.entity.chapterId == current.selectedChapterId
            val matchStatus = when (current.selectedStatus) {
                "all" -> true
                "DUE" -> detail.isDueForReview
                else -> detail.entity.status == current.selectedStatus
            }
            val matchType = current.selectedQuestionType == "all" || detail.entity.questionType == current.selectedQuestionType
            matchSubject && matchChapter && matchStatus && matchType
        }

        filtered = when (current.sortOrder) {
            WrongBankSort.BY_DUE_DATE -> filtered.sortedWith(
                compareBy<WrongQuestionDetail> {
                    if (it.entity.status == WrongQuestionStatus.MASTERED.code) 2
                    else if (it.isDueForReview) 0
                    else 1
                }.thenBy { it.entity.nextReviewTimestamp }
            )
            WrongBankSort.BY_ERROR_COUNT -> filtered.sortedByDescending { it.entity.errorCount }
            WrongBankSort.BY_LATEST -> filtered.sortedByDescending { it.entity.lastWrongTimestamp }
        }

        _uiState.value = current.copy(filteredQuestions = filtered)
    }

    fun showDetail(detail: WrongQuestionDetail) {
        _uiState.value = _uiState.value.copy(selectedQuestionForDetail = detail)
    }

    fun hideDetail() {
        _uiState.value = _uiState.value.copy(selectedQuestionForDetail = null)
    }

    fun promptDelete(detail: WrongQuestionDetail) {
        _uiState.value = _uiState.value.copy(questionToDelete = detail)
    }

    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(questionToDelete = null)
    }

    fun executeDelete() {
        val target = _uiState.value.questionToDelete ?: return
        viewModelScope.launch {
            wrongQuestionRepository.deleteWrongQuestion(target.entity.questionId)
            _uiState.value = _uiState.value.copy(
                questionToDelete = null,
                selectedQuestionForDetail = null,
                notificationMessage = "Đã xóa câu hỏi khỏi ngân hàng câu sai"
            )
            loadData(_uiState.value.selectedGrade)
        }
    }

    fun markMastered(questionId: String) {
        viewModelScope.launch {
            wrongQuestionRepository.markMastered(questionId)
            _uiState.value = _uiState.value.copy(
                selectedQuestionForDetail = null,
                notificationMessage = "Đã đánh dấu câu hỏi thành Đã thuộc"
            )
            loadData(_uiState.value.selectedGrade)
        }
    }

    fun clearNotification() {
        _uiState.value = _uiState.value.copy(notificationMessage = null)
    }
}
