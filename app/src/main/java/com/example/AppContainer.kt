package com.example

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.CurriculumRepository
import com.example.data.repository.FlashcardRepository
import com.example.data.repository.GamificationRepository
import com.example.data.repository.PracticeRepository
import com.example.data.repository.TestRepository
import com.example.data.repository.UserProgressRepository
import com.example.data.repository.WrongQuestionRepository
import com.example.data.seed.ContentLoader
import com.example.data.seed.ContentLoaderImpl
import com.example.data.seed.CurriculumDataSeeder
import com.example.di.LessonModule
import com.example.di.SettingsModule
import com.example.data.local.security.KeystoreTokenStorage
import com.example.data.local.security.TokenStorage
import com.example.data.remote.pocketbase.AuthRemoteDataSource
import com.example.data.remote.pocketbase.PocketBaseApi
import com.example.data.remote.pocketbase.PocketBaseAuthRemoteDataSource
import com.example.data.remote.pocketbase.PocketBaseNetworkModule
import com.example.data.remote.pocketbase.PocketBaseSyncRemoteDataSource
import com.example.data.remote.pocketbase.SyncRemoteDataSource
import com.example.data.remote.push.PocketBasePushTokenRemoteDataSource
import com.example.data.remote.push.PushTokenRemoteDataSource
import com.example.data.push.FcmGateway
import com.example.data.push.FirebaseFcmGateway
import com.example.data.push.PushTokenManager
import com.example.data.push.PushTokenRepositoryImpl
import com.example.data.repository.PocketBaseAuthRepository
import com.example.data.repository.SyncRepositoryImpl
import com.example.data.sync.AndroidSyncNetworkChecker
import com.example.data.sync.SyncNetworkChecker
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.LessonRepository
import com.example.domain.repository.ProgressRepository
import com.example.domain.repository.PushTokenRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.SyncRepository

/**
 * Dependency Injection Container quản lý toàn bộ các Repository, Database và DataStore.
 * Tuân thủ mô hình DI tinh gọn, không phụ thuộc nặng vào framework bên ngoài, đảm bảo ổn định 100%.
 */
interface AppContainer {
    val database: AppDatabase
    val curriculumRepository: CurriculumRepository
    val practiceRepository: PracticeRepository
    val wrongQuestionRepository: WrongQuestionRepository
    val flashcardRepository: FlashcardRepository
    val testRepository: TestRepository
    val userProgressRepository: UserProgressRepository
    val userPreferencesManager: UserPreferencesManager
    val settingsRepository: SettingsRepository
    val lessonRepository: LessonRepository
    val progressRepository: ProgressRepository
    val gamificationRepository: GamificationRepository
    val statsRepository: com.example.data.repository.StatsRepository
    val contentLoader: ContentLoader
    val dataSeeder: CurriculumDataSeeder
    val searchRepository: com.example.domain.repository.SearchRepository
    val searchIndexer: com.example.data.search.SearchIndexer
    val tokenStorage: TokenStorage
    val pocketBaseApi: PocketBaseApi
    val authRemoteDataSource: AuthRemoteDataSource
    val authRepository: AuthRepository
    val syncRemoteDataSource: SyncRemoteDataSource
    val syncNetworkChecker: SyncNetworkChecker
    val syncRepository: SyncRepository
    val aiTutorRepository: com.example.domain.repository.AiTutorRepository

    // F20a – Thông báo đẩy
    val pushTokenRemoteDataSource: PushTokenRemoteDataSource
    val fcmGateway: FcmGateway
    val pushTokenRepository: PushTokenRepository
    val pushTokenManager: PushTokenManager
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val userPreferencesManager: UserPreferencesManager by lazy {
        UserPreferencesManager(context)
    }

    override val statsRepository: com.example.data.repository.StatsRepository by lazy {
        com.example.data.repository.StatsRepositoryImpl(database)
    }

    override val gamificationRepository: GamificationRepository by lazy {
        GamificationRepository(context, database, userPreferencesManager)
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsModule.provideSettingsRepository(userPreferencesManager, database)
    }

    override val lessonRepository: LessonRepository by lazy {
        LessonModule.provideLessonRepository(database, userPreferencesManager)
    }

    override val progressRepository: ProgressRepository by lazy {
        LessonModule.provideProgressRepository(database)
    }

    override val contentLoader: ContentLoader by lazy {
        SettingsModule.provideContentLoader(context, database)
    }

    override val curriculumRepository: CurriculumRepository by lazy {
        CurriculumRepository(database)
    }

    override val practiceRepository: PracticeRepository by lazy {
        PracticeRepository(database)
    }

    override val wrongQuestionRepository: WrongQuestionRepository by lazy {
        WrongQuestionRepository(database)
    }

    override val flashcardRepository: FlashcardRepository by lazy {
        FlashcardRepository(database)
    }

    override val testRepository: TestRepository by lazy {
        TestRepository(database, wrongQuestionRepository)
    }

    override val userProgressRepository: UserProgressRepository by lazy {
        UserProgressRepository(database)
    }

    override val dataSeeder: CurriculumDataSeeder by lazy {
        CurriculumDataSeeder(context, database)
    }

    override val searchIndexer: com.example.data.search.SearchIndexer by lazy {
        com.example.data.search.SearchIndexer(database)
    }

    override val searchRepository: com.example.domain.repository.SearchRepository by lazy {
        com.example.data.repository.SearchRepositoryImpl(database, userPreferencesManager, searchIndexer)
    }

    override val tokenStorage: TokenStorage by lazy {
        KeystoreTokenStorage(context.applicationContext)
    }

    override val pocketBaseApi: PocketBaseApi by lazy {
        PocketBaseNetworkModule.createPocketBaseApi(
            baseUrl = BuildConfig.POCKETBASE_BASE_URL,
            tokenStorage = tokenStorage
        )
    }

    override val authRemoteDataSource: AuthRemoteDataSource by lazy {
        PocketBaseAuthRemoteDataSource(pocketBaseApi)
    }

    override val authRepository: AuthRepository by lazy {
        PocketBaseAuthRepository(
            accountDao = database.accountDao(),
            remoteDataSource = authRemoteDataSource,
            tokenStorage = tokenStorage,
            settingsRepository = settingsRepository,
            preferencesManager = userPreferencesManager,
            syncDao = database.syncDao(),
            // F20a: hủy token thông báo trước khi xóa phiên (đăng xuất/xóa tài khoản).
            onSessionEnding = { pushTokenManager.onSessionEnding() }
        )
    }

    override val syncRemoteDataSource: SyncRemoteDataSource by lazy {
        PocketBaseSyncRemoteDataSource(pocketBaseApi)
    }

    override val syncNetworkChecker: SyncNetworkChecker by lazy {
        AndroidSyncNetworkChecker(context.applicationContext)
    }

    override val syncRepository: SyncRepository by lazy {
        SyncRepositoryImpl(
            database = database,
            remoteDataSource = syncRemoteDataSource,
            authRepository = authRepository,
            tokenStorage = tokenStorage,
            preferencesManager = userPreferencesManager,
            networkChecker = syncNetworkChecker
        )
    }

    override val aiTutorRepository: com.example.domain.repository.AiTutorRepository by lazy {
        com.example.data.repository.AiTutorRepositoryImpl()
    }

    // ---- F20a: Thông báo đẩy ----

    override val fcmGateway: FcmGateway by lazy {
        FirebaseFcmGateway(context.applicationContext)
    }

    override val pushTokenRemoteDataSource: PushTokenRemoteDataSource by lazy {
        PocketBasePushTokenRemoteDataSource(pocketBaseApi)
    }

    override val pushTokenRepository: PushTokenRepository by lazy {
        PushTokenRepositoryImpl(
            activeAccount = { database.accountDao().getActiveAccountSync() },
            localStore = userPreferencesManager,
            remoteDataSource = pushTokenRemoteDataSource,
            fcmGateway = fcmGateway,
            featureEnabled = BuildConfig.ENABLE_PUSH
        )
    }

    override val pushTokenManager: PushTokenManager by lazy {
        PushTokenManager(
            repository = pushTokenRepository,
            authRepository = authRepository,
            settingsRepository = settingsRepository
        )
    }
}
