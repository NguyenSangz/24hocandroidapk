package com.example.di

import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.LessonRepositoryImpl
import com.example.data.repository.ProgressRepositoryImpl
import com.example.domain.repository.LessonRepository
import com.example.domain.repository.ProgressRepository

/**
 * Dependency Injection Module cung cấp LessonRepository và ProgressRepository cho F04
 */
object LessonModule {

    fun provideLessonRepository(
        database: AppDatabase,
        userPreferencesManager: UserPreferencesManager
    ): LessonRepository {
        return LessonRepositoryImpl(database, userPreferencesManager)
    }

    fun provideProgressRepository(
        database: AppDatabase
    ): ProgressRepository {
        return ProgressRepositoryImpl(database)
    }
}
