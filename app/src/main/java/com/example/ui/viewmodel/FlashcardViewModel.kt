package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.FlashcardEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.FlashcardRepository
import com.example.data.repository.UserProgressRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FlashcardStudyState(
    val cards: List<FlashcardEntity> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val isCompleted: Boolean = false,
    val rememberedCount: Int = 0,
    val needsReviewCount: Int = 0
)

class FlashcardViewModel(
    private val flashcardRepository: FlashcardRepository,
    private val userProgressRepository: UserProgressRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val gamificationRepository: com.example.data.repository.GamificationRepository? = null
) : ViewModel() {

    val selectedGrade: StateFlow<Int> = userPreferencesManager.selectedGrade
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 6)

    private val _studyState = MutableStateFlow(FlashcardStudyState())
    val studyState: StateFlow<FlashcardStudyState> = _studyState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val allCards: StateFlow<List<FlashcardEntity>> = selectedGrade.flatMapLatest { grade ->
        flashcardRepository.getFlashcards(grade)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val masteredCount: StateFlow<Int> = selectedGrade.flatMapLatest { grade ->
        flashcardRepository.getMasteredCount(grade)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun startStudySession(cards: List<FlashcardEntity>) {
        _studyState.value = FlashcardStudyState(
            cards = cards,
            currentIndex = 0,
            isFlipped = false,
            isCompleted = false,
            rememberedCount = 0,
            needsReviewCount = 0
        )
    }

    fun flipCard() {
        _studyState.value = _studyState.value.copy(
            isFlipped = !_studyState.value.isFlipped
        )
    }

    fun markCard(remembered: Boolean) {
        val state = _studyState.value
        val currentCard = state.cards.getOrNull(state.currentIndex) ?: return

        viewModelScope.launch {
            flashcardRepository.recordReview(currentCard, remembered)
            userProgressRepository.recordFlashcardReview(1)
            gamificationRepository?.onLearningEvent(
                com.example.domain.gamification.LearningEvent.FlashcardReviewed(
                    cardId = currentCard.id,
                    rating = if (remembered) 4 else 0,
                    subjectId = currentCard.subjectId
                )
            )
        }

        val newRemembered = if (remembered) state.rememberedCount + 1 else state.rememberedCount
        val newNeedsReview = if (!remembered) state.needsReviewCount + 1 else state.needsReviewCount

        if (state.currentIndex + 1 < state.cards.size) {
            _studyState.value = state.copy(
                currentIndex = state.currentIndex + 1,
                isFlipped = false,
                rememberedCount = newRemembered,
                needsReviewCount = newNeedsReview
            )
        } else {
            _studyState.value = state.copy(
                isCompleted = true,
                rememberedCount = newRemembered,
                needsReviewCount = newNeedsReview
            )
        }
    }
}
