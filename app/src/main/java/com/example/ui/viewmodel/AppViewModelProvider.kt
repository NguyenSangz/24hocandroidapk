package com.example.ui.viewmodel

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.THCSApplication
import com.example.ui.screens.grade.SelectGradeViewModel
import com.example.ui.screens.splash.SplashViewModel

object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = thcsApplication()
            SplashViewModel(
                contentLoader = app.container.contentLoader,
                settingsRepository = app.container.settingsRepository
            )
        }
        initializer {
            val app = thcsApplication()
            SelectGradeViewModel(
                settingsRepository = app.container.settingsRepository
            )
        }
        initializer {
            val app = thcsApplication()
            MainViewModel(
                userPreferencesManager = app.container.userPreferencesManager
            )
        }
        initializer {
            val app = thcsApplication()
            StudyViewModel(
                curriculumRepository = app.container.curriculumRepository,
                userPreferencesManager = app.container.userPreferencesManager,
                userProgressRepository = app.container.userProgressRepository
            )
        }
        initializer {
            val app = thcsApplication()
            PracticeViewModel(
                practiceRepository = app.container.practiceRepository,
                curriculumRepository = app.container.curriculumRepository,
                userProgressRepository = app.container.userProgressRepository,
                userPreferencesManager = app.container.userPreferencesManager,
                wrongQuestionRepository = app.container.wrongQuestionRepository,
                gamificationRepository = app.container.gamificationRepository,
                savedStateHandle = createSavedStateHandle()
            )
        }
        initializer {
            val app = thcsApplication()
            TestViewModel(
                testRepository = app.container.testRepository,
                curriculumRepository = app.container.curriculumRepository,
                practiceRepository = app.container.practiceRepository,
                userProgressRepository = app.container.userProgressRepository,
                userPreferencesManager = app.container.userPreferencesManager,
                gamificationRepository = app.container.gamificationRepository,
                savedStateHandle = createSavedStateHandle()
            )
        }
        initializer {
            val app = thcsApplication()
            ExamViewModel(
                practiceRepository = app.container.practiceRepository,
                userProgressRepository = app.container.userProgressRepository,
                userPreferencesManager = app.container.userPreferencesManager
            )
        }
        initializer {
            val app = thcsApplication()
            FlashcardViewModel(
                flashcardRepository = app.container.flashcardRepository,
                userProgressRepository = app.container.userProgressRepository,
                userPreferencesManager = app.container.userPreferencesManager,
                gamificationRepository = app.container.gamificationRepository
            )
        }
        initializer {
            val app = thcsApplication()
            StatsViewModel(
                statsRepository = app.container.statsRepository,
                curriculumRepository = app.container.curriculumRepository,
                userPreferencesManager = app.container.userPreferencesManager
            )
        }
        initializer {
            val app = thcsApplication()
            ProfileViewModel(
                userPreferencesManager = app.container.userPreferencesManager,
                gamificationRepository = app.container.gamificationRepository,
                authRepository = app.container.authRepository
            )
        }
        initializer {
            val app = thcsApplication()
            WrongBankViewModel(
                wrongQuestionRepository = app.container.wrongQuestionRepository,
                curriculumRepository = app.container.curriculumRepository,
                userPreferencesManager = app.container.userPreferencesManager
            )
        }
        initializer {
            val app = thcsApplication()
            ReviewSessionViewModel(
                wrongQuestionRepository = app.container.wrongQuestionRepository,
                practiceRepository = app.container.practiceRepository,
                userProgressRepository = app.container.userProgressRepository,
                userPreferencesManager = app.container.userPreferencesManager,
                gamificationRepository = app.container.gamificationRepository
            )
        }
        initializer {
            val app = thcsApplication()
            com.example.ui.screens.search.SearchViewModel(
                searchRepository = app.container.searchRepository,
                settingsRepository = app.container.settingsRepository,
                savedStateHandle = createSavedStateHandle()
            )
        }
        initializer {
            val app = thcsApplication()
            com.example.ui.screens.settings.SettingsViewModel(
                settingsRepository = app.container.settingsRepository,
                // F20a: null-safe – khi cờ ENABLE_PUSH tắt, controller tự no-op.
                pushSettings = app.container.pushTokenManager
            )
        }
        initializer {
            val app = thcsApplication()
            com.example.ui.screens.account.AccountViewModel(
                authRepository = app.container.authRepository,
                syncRepository = app.container.syncRepository
            )
        }
        initializer {
            val app = thcsApplication()
            com.example.ui.screens.ai.AiTutorViewModel(
                aiTutorRepository = app.container.aiTutorRepository,
                userPreferencesManager = app.container.userPreferencesManager
            )
        }
    }

    fun provideChapterListViewModel(subjectId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = thcsApplication()
            com.example.ui.screens.study.ChapterListViewModel(
                lessonRepository = app.container.lessonRepository,
                userPreferencesManager = app.container.userPreferencesManager,
                subjectId = subjectId
            )
        }
    }

    fun provideLessonViewModel(lessonId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = thcsApplication()
            com.example.ui.screens.study.LessonViewModel(
                lessonRepository = app.container.lessonRepository,
                progressRepository = app.container.progressRepository,
                userPreferencesManager = app.container.userPreferencesManager,
                lessonId = lessonId,
                gamificationRepository = app.container.gamificationRepository
            )
        }
    }
}

fun CreationExtras.thcsApplication(): THCSApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as THCSApplication)