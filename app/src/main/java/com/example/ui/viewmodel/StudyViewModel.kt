package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.CurriculumRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChapterWithLessons(
    val chapter: ChapterEntity,
    val lessons: List<LessonEntity>
)

data class ContinueLearningData(
    val lesson: LessonEntity,
    val chapterTitle: String,
    val subjectName: String,
    val subjectColor: String,
    val nextLesson: LessonEntity?
)

data class TodayGoalInfo(
    val targetQuestions: Int = 15,
    val answeredQuestions: Int = 0,
    val targetMinutes: Int = 30,
    val studyMinutes: Int = 0,
    val xpEarned: Int = 0
)

data class StudyUiState(
    val selectedSubjectId: String = "math",
    val selectedSubject: SubjectEntity? = null,
    val chapters: List<ChapterEntity> = emptyList(),
    val lessonsByChapter: Map<String, List<LessonEntity>> = emptyMap(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = false
)

class StudyViewModel(
    private val curriculumRepository: CurriculumRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val userProgressRepository: com.example.data.repository.UserProgressRepository? = null
) : ViewModel() {

    val selectedGrade: StateFlow<Int> = userPreferencesManager.selectedGrade
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)

    private val _continueLearningData = MutableStateFlow<ContinueLearningData?>(null)
    val continueLearningData: StateFlow<ContinueLearningData?> = _continueLearningData.asStateFlow()

    private val _todayGoalInfo = MutableStateFlow(TodayGoalInfo())
    val todayGoalInfo: StateFlow<TodayGoalInfo> = _todayGoalInfo.asStateFlow()

    /**
     * F17a: Chỉ hiển thị các môn THỰC SỰ có nội dung (có chương) cho lớp đang chọn.
     * Các môn được khai báo trong `sample_curriculum.json` nhưng chưa có gói nội dung
     * sẽ bị ẩn, tránh hiện chip môn mà bấm vào không có bài học nào.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val subjects: StateFlow<List<SubjectEntity>> = selectedGrade
        .flatMapLatest { grade ->
            combine(
                curriculumRepository.allSubjects,
                curriculumRepository.getSubjectIdsWithChapters(grade)
            ) { allSubjects, subjectIdsWithContent ->
                filterSubjectsWithContent(allSubjects, subjectIdsWithContent)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSubjectId = MutableStateFlow("math")
    val selectedSubjectId: StateFlow<String> = _selectedSubjectId.asStateFlow()

    init {
        // F17a: Nếu môn đang chọn không có nội dung ở lớp mới, tự chuyển sang môn đầu tiên có nội dung
        viewModelScope.launch {
            subjects.collect { availableSubjects ->
                val currentId = _selectedSubjectId.value
                if (availableSubjects.isNotEmpty() && availableSubjects.none { it.id == currentId }) {
                    _selectedSubjectId.value = availableSubjects.first().id
                }
            }
        }

        viewModelScope.launch {
            combine(
                selectedGrade,
                userPreferencesManager.lastStudiedLessonId
            ) { grade, lastLessonId ->
                Pair(grade, lastLessonId)
            }.collect { (grade, lastLessonId) ->
                loadContinueLearning(grade, lastLessonId)
            }
        }

        if (userProgressRepository != null) {
            viewModelScope.launch {
                combine(
                    userProgressRepository.todayProgress,
                    userPreferencesManager.dailyGoalQuestions,
                    userPreferencesManager.dailyGoalMinutes
                ) { progress, targetQ, targetM ->
                    val answered = progress?.questionsAnswered ?: 0
                    val minutes = progress?.studyMinutes ?: 0
                    val xp = progress?.xpEarned ?: 0
                    TodayGoalInfo(
                        targetQuestions = targetQ,
                        answeredQuestions = answered,
                        targetMinutes = targetM,
                        studyMinutes = minutes,
                        xpEarned = xp
                    )
                }.collect { goalInfo ->
                    _todayGoalInfo.value = goalInfo
                }
            }
        }
    }

    private suspend fun loadContinueLearning(grade: Int, lastLessonId: String?) {
        val chaptersList = curriculumRepository.getAllChaptersForGradeSync(grade)
        val allLessons = mutableListOf<Triple<LessonEntity, ChapterEntity, SubjectEntity>>()
        val subjectsList = curriculumRepository.allSubjects.firstOrNull() ?: emptyList()
        val subjectMap = subjectsList.associateBy { it.id }

        for (ch in chaptersList) {
            val lessons = curriculumRepository.getLessonsForChapterSync(ch.id)
            val sub = subjectMap[ch.subjectId] ?: SubjectEntity(ch.subjectId, "Môn học", "book", "#2563EB", "")
            for (l in lessons) {
                allLessons.add(Triple(l, ch, sub))
            }
        }

        if (allLessons.isEmpty()) {
            _continueLearningData.value = null
            return
        }

        var targetTriple = allLessons.firstOrNull { it.first.id == lastLessonId }
        if (targetTriple == null) {
            targetTriple = allLessons.firstOrNull { !it.first.isCompleted } ?: allLessons.first()
        }

        val (currentLesson, currentChapter, subject) = targetTriple
        val chapterLessons = curriculumRepository.getLessonsForChapterSync(currentChapter.id)
        val currentIndex = chapterLessons.indexOfFirst { it.id == currentLesson.id }
        val nextLesson = if (currentIndex in 0 until chapterLessons.size - 1) {
            chapterLessons[currentIndex + 1]
        } else {
            val sortedChapters = chaptersList.sortedBy { it.order }
            val chIndex = sortedChapters.indexOfFirst { it.id == currentChapter.id }
            if (chIndex in 0 until sortedChapters.size - 1) {
                val nextCh = sortedChapters[chIndex + 1]
                curriculumRepository.getLessonsForChapterSync(nextCh.id).firstOrNull()
            } else null
        }

        _continueLearningData.value = ContinueLearningData(
            lesson = currentLesson,
            chapterTitle = currentChapter.title,
            subjectName = subject.name,
            subjectColor = subject.color,
            nextLesson = nextLesson
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val chapters: StateFlow<List<ChapterEntity>> = combine(
        selectedGrade,
        _selectedSubjectId
    ) { grade, subjectId ->
        Pair(grade, subjectId)
    }.flatMapLatest { (grade, subjectId) ->
        curriculumRepository.getChapters(grade, subjectId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeLesson = MutableStateFlow<LessonEntity?>(null)
    val activeLesson: StateFlow<LessonEntity?> = _activeLesson.asStateFlow()

    fun selectSubject(subjectId: String) {
        _selectedSubjectId.value = subjectId
    }

    fun loadLessonDetail(lessonId: String) {
        viewModelScope.launch {
            val lesson = curriculumRepository.getLesson(lessonId)
            _activeLesson.value = lesson
        }
    }

    fun toggleLessonCompleted(lessonId: String, currentCompleted: Boolean) {
        viewModelScope.launch {
            val newStatus = !currentCompleted
            curriculumRepository.toggleLessonCompleted(lessonId, newStatus)
            _activeLesson.value = _activeLesson.value?.copy(isCompleted = newStatus)
        }
    }

    fun getLessonsForChapter(chapterId: String) = curriculumRepository.getLessons(chapterId)

    companion object {
        /**
         * F17a: Lọc danh sách môn, chỉ giữ môn có nội dung cho lớp hiện tại.
         * Hàm thuần (pure) để unit test trực tiếp logic ẩn môn trống.
         */
        fun filterSubjectsWithContent(
            allSubjects: List<SubjectEntity>,
            subjectIdsWithContent: List<String>
        ): List<SubjectEntity> {
            val available = subjectIdsWithContent.toSet()
            return allSubjects.filter { it.id in available }
        }
    }
}
