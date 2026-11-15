package com.example.domain.repository

import kotlinx.coroutines.flow.Flow

enum class SyncStatus {
    IDLE,
    SYNCING,
    OFFLINE,
    PAUSED_METERED,
    SUCCESS,
    RATE_LIMITED,
    ERROR
}

data class SyncUiSummary(
    val status: SyncStatus = SyncStatus.IDLE,
    val pendingChangesCount: Int = 0,
    val permanentFailedCount: Int = 0,
    val lastSyncedAt: Long? = null,
    val lastDeltaCursor: String = "",
    val syncWifiOnly: Boolean = false,
    val statusMessage: String = "Sẵn sàng đồng bộ",
    val lastErrorMessage: String = ""
)

sealed class SyncOperationResult {
    data class Success(
        val pushedCount: Int,
        val pulledCount: Int,
        val message: String
    ) : SyncOperationResult()

    data class Skipped(
        val reason: SyncStatus,
        val message: String
    ) : SyncOperationResult()

    data class Error(
        val message: String,
        val isNetworkError: Boolean = false,
        val isRateLimited: Boolean = false
    ) : SyncOperationResult()
}

interface SyncRepository {
    val syncSummary: Flow<SyncUiSummary>

    suspend fun syncNow(manual: Boolean = false): SyncOperationResult

    suspend fun setSyncWifiOnly(wifiOnly: Boolean)

    suspend fun retryFailedItems(): SyncOperationResult

    suspend fun enqueueFullLocalStateSnapshotIfNeeded()
}
