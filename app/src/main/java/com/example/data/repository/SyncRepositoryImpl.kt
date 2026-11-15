package com.example.data.repository

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.FlashcardProgressEntity
import com.example.data.local.entity.SyncOutboxEntity
import com.example.data.local.entity.SyncStateEntity
import com.example.data.local.security.TokenStorage
import com.example.data.preferences.UserPreferencesManager
import com.example.data.remote.NetworkLogSanitizer
import com.example.data.remote.pocketbase.RemoteSyncResult
import com.example.data.remote.pocketbase.SyncPushItemRequest
import com.example.data.remote.pocketbase.SyncRemoteDataSource
import com.example.data.remote.pocketbase.SyncRemoteRecordDto
import com.example.data.sync.SyncCollections
import com.example.data.sync.SyncConflictResolver
import com.example.data.sync.SyncNetworkChecker
import com.example.data.sync.SyncOutboxHelper
import com.example.domain.repository.AccountMode
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.SyncOperationResult
import com.example.domain.repository.SyncRepository
import com.example.domain.repository.SyncStatus
import com.example.domain.repository.SyncUiSummary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Trình điều phối đồng bộ Offline-First (F16b):
 * - UI chỉ đọc Room; mọi thay đổi cục bộ xếp hàng vào `sync_outbox`.
 * - Đẩy theo lô tối đa 50 mục với idempotent upsert và exponential backoff khi lỗi mạng.
 * - Kéo delta theo con trỏ `updated` do PocketBase server cấp, phân giải xung đột xác định qua [SyncConflictResolver].
 * - Tuân thủ công tắc "Chỉ đồng bộ qua Wi-Fi", không khóa app khi mất mạng/rate limit/401.
 */
class SyncRepositoryImpl(
    private val database: AppDatabase,
    private val remoteDataSource: SyncRemoteDataSource,
    private val authRepository: AuthRepository,
    private val tokenStorage: TokenStorage,
    private val preferencesManager: UserPreferencesManager,
    private val networkChecker: SyncNetworkChecker,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SyncRepository {

    companion object {
        private const val TAG = "SyncRepository"
        private const val MAX_PUSH_BATCH_SIZE = 50
        private const val MAX_PULL_PAGE_SIZE = 100
        private const val MAX_RETRIES = 6
        private const val BASE_BACKOFF_MS = 30_000L
        private const val MAX_BACKOFF_MS = 15 * 60_000L
    }

    private val syncDao = database.syncDao()
    private val syncMutex = Mutex()
    private val activeSyncingFlow = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val syncSummary: Flow<SyncUiSummary> = combine(
        authRepository.accountState.flatMapLatest { acc ->
            syncDao.observeSyncState(acc.uid)
        },
        syncDao.observePendingCount(),
        syncDao.observePermanentFailedCount(),
        preferencesManager.syncWifiOnly,
        activeSyncingFlow
    ) { stateEntity, pendingCount, failedCount, wifiOnly, isSyncing ->
        val resolvedStatus = when {
            isSyncing -> SyncStatus.SYNCING
            else -> parseStatus(stateEntity?.lastSyncStatus)
        }
        val statusMsg = buildStatusMessage(resolvedStatus, pendingCount, failedCount, wifiOnly, stateEntity?.lastErrorMessage.orEmpty())
        SyncUiSummary(
            status = resolvedStatus,
            pendingChangesCount = pendingCount,
            permanentFailedCount = failedCount,
            lastSyncedAt = stateEntity?.lastSyncedAt,
            lastDeltaCursor = stateEntity?.lastDeltaCursor.orEmpty(),
            syncWifiOnly = stateEntity?.syncWifiOnly ?: wifiOnly,
            statusMessage = statusMsg,
            lastErrorMessage = stateEntity?.lastErrorMessage.orEmpty()
        )
    }

    override suspend fun setSyncWifiOnly(wifiOnly: Boolean) = withContext(ioDispatcher) {
        preferencesManager.setSyncWifiOnly(wifiOnly)
        val activeUid = authRepository.getCurrentAccount().uid
        val currentState = syncDao.getSyncStateSync(activeUid) ?: SyncStateEntity(uid = activeUid)
        syncDao.upsertSyncState(
            currentState.copy(
                syncWifiOnly = wifiOnly,
                updatedAt = clock()
            )
        )
        SyncOutboxHelper.enqueueSettingsSnapshot(
            database = database,
            snapshot = preferencesManager.exportSyncedSettingsSnapshot(),
            now = clock()
        )
    }

    override suspend fun retryFailedItems(): SyncOperationResult = withContext(ioDispatcher) {
        syncDao.resetPermanentFailedForRetry()
        syncNow(manual = true)
    }

    override suspend fun enqueueFullLocalStateSnapshotIfNeeded() = withContext(ioDispatcher) {
        val account = authRepository.getCurrentAccount()
        if (account.mode != AccountMode.AUTHENTICATED) return@withContext
        val state = syncDao.getSyncStateSync(account.uid)
        if (state != null && (state.lastSyncedAt != null || state.lastDeltaCursor.isNotBlank())) {
            return@withContext
        }
        enqueueAllLocalRecords()
    }

    private suspend fun enqueueAllLocalRecords() {
        val now = clock()

        database.lessonDao().getAllLessonsSync().filter { it.isCompleted }.forEach { lesson ->
            SyncOutboxHelper.enqueueLessonProgress(database, lesson.id, true, now)
        }

        database.attemptDao().getAllAttemptsSync().forEach { attempt ->
            SyncOutboxHelper.enqueueAttempt(database, attempt, attempt.timestamp)
        }

        database.testDao().getAllTestsSync().forEach { test ->
            val answers = database.testDao().getAnswersForTest(test.id)
            SyncOutboxHelper.enqueueTest(database, test, answers, deleted = false, now = test.timestamp)
        }

        database.wrongQuestionDao().getAllWrongQuestionsSync().forEach { wrong ->
            SyncOutboxHelper.enqueueWrongQuestion(database, wrong, deleted = false)
        }

        database.flashcardDao().getAllFlashcardProgressSync().forEach { prog ->
            SyncOutboxHelper.enqueueFlashcardProgress(database, prog)
        }

        database.gamificationDao().getAllXpEventsSync().forEach { xp ->
            SyncOutboxHelper.enqueueXpEvent(database, xp)
        }

        database.gamificationDao().getAllBadgeUnlocksSync().forEach { badge ->
            SyncOutboxHelper.enqueueBadgeUnlock(database, badge)
        }

        val settingsTs = preferencesManager.getSettingsUpdatedAtSync().let { if (it > 0L) it else now }
        SyncOutboxHelper.enqueueSettingsSnapshot(
            database = database,
            snapshot = preferencesManager.exportSyncedSettingsSnapshot(),
            now = settingsTs
        )

        val masteredVerbs = preferencesManager.masteredIrregularVerbs.first()
        if (masteredVerbs.isNotEmpty()) {
            SyncOutboxHelper.enqueueIrregularVerbs(database, masteredVerbs, now)
        }
    }

    override suspend fun syncNow(manual: Boolean): SyncOperationResult = withContext(ioDispatcher) {
        syncMutex.withLock {
            val account = authRepository.getCurrentAccount()
            if (account.mode != AccountMode.AUTHENTICATED || account.uid == AccountEntity.GUEST_UID) {
                return@withLock SyncOperationResult.Skipped(
                    SyncStatus.IDLE,
                    "Đang ở Chế độ Khách (dữ liệu lưu an toàn trên máy)."
                )
            }

            if (account.needsParentalConsent) {
                return@withLock SyncOperationResult.Skipped(
                    SyncStatus.IDLE,
                    "Đang chờ phụ huynh xác nhận qua email. Dữ liệu vẫn được lưu an toàn trên máy."
                )
            }

            if (account.sessionExpired || tokenStorage.getToken().isNullOrBlank()) {
                val cur = syncDao.getSyncStateSync(account.uid) ?: SyncStateEntity(uid = account.uid)
                syncDao.upsertSyncState(
                    cur.copy(
                        lastSyncStatus = SyncStateEntity.SYNC_STATUS_ERROR,
                        lastErrorMessage = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
                        updatedAt = clock()
                    )
                )
                return@withLock SyncOperationResult.Error(
                    "Phiên đăng nhập đã hết hạn. Bạn vẫn học offline bình thường; hãy đăng nhập lại khi sẵn sàng."
                )
            }

            val netState = networkChecker.currentState()
            if (!netState.isConnected) {
                updateSyncStatus(account.uid, SyncStateEntity.SYNC_STATUS_OFFLINE, "")
                return@withLock SyncOperationResult.Skipped(
                    SyncStatus.OFFLINE,
                    "Không có kết nối mạng. Thay đổi vẫn được lưu an toàn và sẽ tự đồng bộ khi có mạng."
                )
            }

            val wifiOnly = preferencesManager.syncWifiOnly.first()
            if (wifiOnly && !netState.isUnmeteredWifi) {
                updateSyncStatus(account.uid, SyncStateEntity.SYNC_STATUS_PAUSED_METERED, "")
                return@withLock SyncOperationResult.Skipped(
                    SyncStatus.PAUSED_METERED,
                    "Đang bật 'Chỉ đồng bộ qua Wi-Fi'. Dữ liệu sẽ tự đồng bộ ngay khi kết nối Wi-Fi."
                )
            }

            activeSyncingFlow.value = true
            try {
                // Nếu là lần đồng bộ đầu tiên sau khi nâng cấp tài khoản, đưa toàn bộ dữ liệu trên máy vào hàng đợi
                enqueueFullLocalStateSnapshotIfNeeded()

                var pushedTotal = 0
                val now = clock()

                // 1. ĐẨY OUTBOX THEO LÔ (<= 50 mục/request)
                while (true) {
                    val candidates = syncDao.getPendingOutboxBatch(maxRetries = MAX_RETRIES, limit = MAX_PUSH_BATCH_SIZE)
                    val readyItems = if (manual) {
                        candidates
                    } else {
                        candidates.filter { item -> isBackoffReady(item, now) }
                    }
                    if (readyItems.isEmpty()) break

                    val pushRequests = readyItems.map { item ->
                        SyncPushItemRequest(
                            collection = item.collectionName,
                            entityKey = item.entityKey,
                            sourceType = item.sourceType.takeIf { it.isNotBlank() },
                            sourceId = item.sourceId.takeIf { it.isNotBlank() },
                            payload = SyncOutboxHelper.jsonToMap(item.payloadJson),
                            deleted = item.deleted,
                            clientUpdatedAt = item.clientUpdatedAt
                        )
                    }

                    when (val pushRes = remoteDataSource.pushBatch(pushRequests)) {
                        is RemoteSyncResult.Success -> {
                            syncDao.deleteOutboxByIds(readyItems.map { it.id })
                            pushedTotal += pushRes.data.acceptedCount.coerceAtLeast(readyItems.size)
                        }
                        is RemoteSyncResult.Unauthorized -> {
                            authRepository.markSessionExpired()
                            updateSyncStatus(account.uid, SyncStateEntity.SYNC_STATUS_ERROR, pushRes.message)
                            return@withLock SyncOperationResult.Error(pushRes.message)
                        }
                        is RemoteSyncResult.RateLimited -> {
                            syncDao.markBatchFailed(readyItems.map { it.id }, clock(), pushRes.message, MAX_RETRIES)
                            updateSyncStatus(account.uid, SyncStateEntity.SYNC_STATUS_RATE_LIMITED, pushRes.message)
                            return@withLock SyncOperationResult.Error(pushRes.message, isRateLimited = true)
                        }
                        is RemoteSyncResult.NetworkError -> {
                            syncDao.markBatchFailed(readyItems.map { it.id }, clock(), pushRes.message, MAX_RETRIES)
                            updateSyncStatus(account.uid, SyncStateEntity.SYNC_STATUS_OFFLINE, pushRes.message)
                            return@withLock SyncOperationResult.Error(pushRes.message, isNetworkError = true)
                        }
                        is RemoteSyncResult.ValidationError,
                        is RemoteSyncResult.ServerError -> {
                            val errMsg = when (pushRes) {
                                is RemoteSyncResult.ValidationError -> pushRes.message
                                is RemoteSyncResult.ServerError -> pushRes.message
                                else -> "Lỗi đồng bộ máy chủ"
                            }
                            syncDao.markBatchFailed(readyItems.map { it.id }, clock(), errMsg, MAX_RETRIES)
                            updateSyncStatus(account.uid, SyncStateEntity.SYNC_STATUS_ERROR, errMsg)
                            return@withLock SyncOperationResult.Error(errMsg)
                        }
                    }
                }

                // 2. KÉO DELTA TỪ SERVER THEO CON TRỎ `updated`
                val syncStateBeforePull = syncDao.getSyncStateSync(account.uid) ?: SyncStateEntity(uid = account.uid)
                var cursor = syncStateBeforePull.lastDeltaCursor
                var pulledTotal = 0

                while (true) {
                    when (val pullRes = remoteDataSource.pullDelta(since = cursor, limit = MAX_PULL_PAGE_SIZE)) {
                        is RemoteSyncResult.Success -> {
                            val page = pullRes.data
                            if (page.items.isNotEmpty()) {
                                applyRemoteDeltaItems(page.items)
                                pulledTotal += page.items.size
                            }
                            val nextCursor = page.nextCursor.ifBlank { cursor }
                            val cursorChanged = nextCursor != cursor
                            cursor = nextCursor

                            syncDao.upsertSyncState(
                                (syncDao.getSyncStateSync(account.uid) ?: SyncStateEntity(uid = account.uid)).copy(
                                    lastDeltaCursor = cursor,
                                    updatedAt = clock()
                                )
                            )

                            if (!page.hasMore || !cursorChanged || page.items.isEmpty()) {
                                break
                            }
                        }
                        is RemoteSyncResult.Unauthorized -> {
                            authRepository.markSessionExpired()
                            updateSyncStatus(account.uid, SyncStateEntity.SYNC_STATUS_ERROR, pullRes.message)
                            return@withLock SyncOperationResult.Error(pullRes.message)
                        }
                        is RemoteSyncResult.RateLimited -> {
                            updateSyncStatus(account.uid, SyncStateEntity.SYNC_STATUS_RATE_LIMITED, pullRes.message)
                            return@withLock SyncOperationResult.Error(pullRes.message, isRateLimited = true)
                        }
                        is RemoteSyncResult.NetworkError -> {
                            updateSyncStatus(account.uid, SyncStateEntity.SYNC_STATUS_OFFLINE, pullRes.message)
                            return@withLock SyncOperationResult.Error(pullRes.message, isNetworkError = true)
                        }
                        is RemoteSyncResult.ValidationError,
                        is RemoteSyncResult.ServerError -> {
                            val errMsg = when (pullRes) {
                                is RemoteSyncResult.ValidationError -> pullRes.message
                                is RemoteSyncResult.ServerError -> pullRes.message
                                else -> "Lỗi máy chủ khi tải dữ liệu đồng bộ"
                            }
                            updateSyncStatus(account.uid, SyncStateEntity.SYNC_STATUS_ERROR, errMsg)
                            return@withLock SyncOperationResult.Error(errMsg)
                        }
                    }
                }

                val doneTime = clock()
                val currentState = syncDao.getSyncStateSync(account.uid) ?: SyncStateEntity(uid = account.uid)
                syncDao.upsertSyncState(
                    currentState.copy(
                        lastDeltaCursor = cursor,
                        lastSyncedAt = doneTime,
                        lastSyncStatus = SyncStateEntity.SYNC_STATUS_SUCCESS,
                        lastErrorMessage = "",
                        syncWifiOnly = wifiOnly,
                        updatedAt = doneTime
                    )
                )

                // Cập nhật lastSyncedAt lên bản ghi accounts để hiển thị đồng nhất trên hồ sơ
                database.accountDao().getAccountByUid(account.uid)?.let { accEntity ->
                    database.accountDao().upsertAccount(accEntity.copy(lastSyncedAt = doneTime))
                }

                Log.i(
                    TAG,
                    "Đồng bộ hoàn tất cho ${NetworkLogSanitizer.maskUid(account.uid)}: pushed=$pushedTotal, pulled=$pulledTotal"
                )
                val msg = if (pushedTotal == 0 && pulledTotal == 0) {
                    "Dữ liệu đã ở trạng thái mới nhất."
                } else {
                    "Đồng bộ thành công ($pushedTotal mục gửi lên, $pulledTotal mục cập nhật về)."
                }
                SyncOperationResult.Success(pushedTotal, pulledTotal, msg)
            } finally {
                activeSyncingFlow.value = false
            }
        }
    }

    private suspend fun applyRemoteDeltaItems(items: List<SyncRemoteRecordDto>) {
        val existingXpKeys = database.gamificationDao().getAllXpEventsSync().map { it.eventKey }.toMutableSet()

        for (item in items) {
            val payload = item.payload
            when (item.collection) {
                SyncCollections.LESSON_PROGRESS -> {
                    val lessonId = (payload["lessonId"] as? String)?.ifBlank { item.entityKey } ?: item.entityKey
                    val localLesson = database.lessonDao().getLessonById(lessonId)
                    if (localLesson != null) {
                        val remoteCompleted = (payload["completed"] as? Boolean) ?: false
                        val mergedCompleted = SyncConflictResolver.mergeLessonCompletion(
                            localCompleted = localLesson.isCompleted,
                            remoteCompleted = remoteCompleted
                        )
                        if (mergedCompleted != localLesson.isCompleted) {
                            database.lessonDao().updateLessonCompleted(lessonId, mergedCompleted)
                        }
                    }
                }

                SyncCollections.ATTEMPTS -> {
                    if (!item.deleted) {
                        val qId = (payload["questionId"] as? String).orEmpty()
                        val ts = (payload["timestamp"] as? Number)?.toLong() ?: 0L
                        if (qId.isNotBlank() && ts > 0L) {
                            val exists = database.attemptDao().countByQuestionAndTimestamp(qId, ts) > 0
                            if (!exists) {
                                database.attemptDao().insertAttempt(
                                    com.example.data.local.entity.AttemptEntity(
                                        questionId = qId,
                                        isCorrect = (payload["isCorrect"] as? Boolean) ?: false,
                                        score = (payload["score"] as? Number)?.toFloat() ?: 0f,
                                        timestamp = ts,
                                        timeSpentSeconds = (payload["timeSpentSeconds"] as? Number)?.toInt() ?: 0,
                                        userAnswer = (payload["userAnswer"] as? String).orEmpty(),
                                        usedHint = (payload["usedHint"] as? Boolean) ?: false
                                    )
                                )
                            }
                        }
                    }
                }

                SyncCollections.TESTS -> {
                    val localTest = database.testDao().getTestById(item.entityKey)
                    when (val decision = SyncConflictResolver.mergeTest(
                        local = localTest,
                        remoteEntityKey = item.entityKey,
                        remotePayload = payload,
                        remoteDeleted = item.deleted,
                        remoteClientUpdatedAt = item.clientUpdatedAt
                    )) {
                        is SyncConflictResolver.TestMergeDecision.Upsert -> {
                            database.testDao().insertTest(decision.test)
                            database.testDao().deleteAnswersByTestId(decision.test.id)
                            if (decision.answers.isNotEmpty()) {
                                database.testDao().insertAnswers(decision.answers)
                            }
                        }
                        is SyncConflictResolver.TestMergeDecision.DeleteLocal -> {
                            database.testDao().deleteTestById(decision.testId)
                            database.testDao().deleteAnswersByTestId(decision.testId)
                        }
                        SyncConflictResolver.TestMergeDecision.NoOp -> Unit
                    }
                }

                SyncCollections.WRONG_QUESTIONS -> {
                    val qId = (payload["questionId"] as? String)?.ifBlank { item.entityKey } ?: item.entityKey
                    val localWrong = database.wrongQuestionDao().getByQuestionId(qId)
                    when (val decision = SyncConflictResolver.mergeWrongQuestion(
                        local = localWrong,
                        remotePayload = payload,
                        remoteDeleted = item.deleted,
                        remoteClientUpdatedAt = item.clientUpdatedAt
                    )) {
                        is SyncConflictResolver.WrongQuestionMergeDecision.Upsert -> {
                            database.wrongQuestionDao().insertOrUpdate(decision.entity)
                        }
                        is SyncConflictResolver.WrongQuestionMergeDecision.DeleteLocal -> {
                            database.wrongQuestionDao().deleteByQuestionId(decision.questionId)
                        }
                        SyncConflictResolver.WrongQuestionMergeDecision.NoOp -> Unit
                    }
                }

                SyncCollections.FLASHCARDS -> {
                    val cardId = (payload["cardId"] as? String)?.ifBlank { item.entityKey } ?: item.entityKey
                    val localProg = database.flashcardDao().getProgressForCard(cardId)
                    val merged = SyncConflictResolver.mergeFlashcardProgress(
                        local = localProg,
                        remotePayload = payload,
                        remoteDeleted = item.deleted
                    )
                    if (merged != null) {
                        database.flashcardDao().insertProgress(merged)
                        val box = (merged.repetitions + 1).coerceIn(1, 5)
                        database.flashcardDao().updateCardReview(
                            id = merged.cardId,
                            box = box,
                            intervalDays = merged.intervalDays.coerceAtLeast(1),
                            nextReview = merged.nextReviewTimestamp,
                            easeFactor = merged.easeFactor,
                            repetitions = merged.repetitions
                        )
                    }
                }

                SyncCollections.XP_EVENTS -> {
                    if (!item.deleted) {
                        val xpEntity = SyncConflictResolver.parseXpEventIfNew(
                            remoteEntityKey = item.entityKey,
                            remotePayload = payload,
                            existingEventKeys = existingXpKeys
                        )
                        if (xpEntity != null) {
                            database.gamificationDao().insertXpEvent(xpEntity)
                            existingXpKeys.add(xpEntity.eventKey)
                        }
                    }
                }

                SyncCollections.BADGES -> {
                    if (!item.deleted) {
                        val localBadge = database.gamificationDao().getAllBadgeUnlocksSync()
                            .find { it.badgeId == item.entityKey }
                        val merged = SyncConflictResolver.mergeBadge(localBadge, item.entityKey, payload)
                        if (merged != null) {
                            database.gamificationDao().insertBadgeUnlock(merged)
                        }
                    }
                }

                SyncCollections.SETTINGS -> {
                    if (!item.deleted) {
                        val localUpdatedAt = preferencesManager.getSettingsUpdatedAtSync()
                        if (SyncConflictResolver.shouldApplySettings(localUpdatedAt, item.clientUpdatedAt)) {
                            preferencesManager.applySyncedSettingsSnapshot(payload, item.clientUpdatedAt)
                        }
                    }
                }

                SyncCollections.IRREGULAR_VERBS -> {
                    val localVerbs = preferencesManager.masteredIrregularVerbs.first()
                    val mergedVerbs = SyncConflictResolver.mergeIrregularVerbs(localVerbs, payload, item.deleted)
                    if (mergedVerbs != localVerbs) {
                        preferencesManager.setMasteredIrregularVerbs(mergedVerbs)
                    }
                }

                SyncCollections.VOCAB_PACKS -> {
                    // Nếu gói từ vựng có thông tin deckId + tiến độ thẻ, đồng bộ an toàn mà không làm mất thẻ cục bộ
                    val cardId = (payload["cardId"] as? String).orEmpty()
                    if (cardId.isNotBlank() && !item.deleted) {
                        val localProg = database.flashcardDao().getProgressForCard(cardId)
                        val merged = SyncConflictResolver.mergeFlashcardProgress(localProg, payload, false)
                        if (merged != null) {
                            database.flashcardDao().insertProgress(merged)
                        }
                    }
                }
            }
        }
    }

    private fun isBackoffReady(item: SyncOutboxEntity, now: Long): Boolean {
        if (item.retryCount <= 0 || item.lastAttemptAt <= 0L) return true
        val shift = (item.retryCount - 1).coerceIn(0, 5)
        val delay = (BASE_BACKOFF_MS shl shift).coerceAtMost(MAX_BACKOFF_MS)
        return (now - item.lastAttemptAt) >= delay
    }

    private suspend fun updateSyncStatus(uid: String, status: String, errorMessage: String) {
        val current = syncDao.getSyncStateSync(uid) ?: SyncStateEntity(uid = uid)
        syncDao.upsertSyncState(
            current.copy(
                lastSyncStatus = status,
                lastErrorMessage = errorMessage,
                updatedAt = clock()
            )
        )
    }

    private fun parseStatus(raw: String?): SyncStatus = when (raw) {
        SyncStateEntity.SYNC_STATUS_SYNCING -> SyncStatus.SYNCING
        SyncStateEntity.SYNC_STATUS_OFFLINE -> SyncStatus.OFFLINE
        SyncStateEntity.SYNC_STATUS_PAUSED_METERED -> SyncStatus.PAUSED_METERED
        SyncStateEntity.SYNC_STATUS_SUCCESS -> SyncStatus.SUCCESS
        SyncStateEntity.SYNC_STATUS_RATE_LIMITED -> SyncStatus.RATE_LIMITED
        SyncStateEntity.SYNC_STATUS_ERROR -> SyncStatus.ERROR
        else -> SyncStatus.IDLE
    }

    private fun buildStatusMessage(
        status: SyncStatus,
        pendingCount: Int,
        failedCount: Int,
        wifiOnly: Boolean,
        lastError: String
    ): String = when {
        status == SyncStatus.SYNCING -> "Đang đồng bộ tiến độ với máy chủ..."
        status == SyncStatus.PAUSED_METERED -> "Đang chờ Wi-Fi để đồng bộ ($pendingCount thay đổi đang chờ)"
        status == SyncStatus.OFFLINE -> if (pendingCount > 0) {
            "Ngoại tuyến • $pendingCount thay đổi đang chờ mạng"
        } else {
            "Đang ngoại tuyến (học tập bình thường)"
        }
        status == SyncStatus.RATE_LIMITED -> lastError.ifBlank { "Tạm nghỉ vài phút để tránh đồng bộ quá nhanh." }
        status == SyncStatus.ERROR -> lastError.ifBlank { "Đồng bộ tạm thời gặp lỗi; tiến độ trên máy vẫn an toàn." }
        pendingCount > 0 -> "Có $pendingCount thay đổi chờ đồng bộ"
        failedCount > 0 -> "Có $failedCount mục chưa đồng bộ được (chạm để thử lại)"
        wifiOnly && status == SyncStatus.SUCCESS -> "Đã đồng bộ xong (Chế độ chỉ qua Wi-Fi)"
        status == SyncStatus.SUCCESS -> "Đã đồng bộ mọi tiến độ"
        else -> "Sẵn sàng đồng bộ"
    }
}
