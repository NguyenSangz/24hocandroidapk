package com.example.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.SettingsRepositoryImpl
import com.example.data.seed.ContentLoader
import com.example.data.seed.ContentLoaderImpl
import com.example.domain.repository.SettingsRepository

/**
 * Module cung cấp các phụ thuộc liên quan đến Settings và ContentLoader.
 * Sẵn sàng tích hợp cho Hilt hoặc AppContainer DI.
 */
object SettingsModule {

    fun provideSettingsRepository(
        preferencesManager: UserPreferencesManager,
        database: AppDatabase? = null
    ): SettingsRepository {
        return SettingsRepositoryImpl(preferencesManager, database)
    }

    fun provideContentLoader(
        context: Context,
        database: AppDatabase
    ): ContentLoader {
        return ContentLoaderImpl(context, database)
    }
}
