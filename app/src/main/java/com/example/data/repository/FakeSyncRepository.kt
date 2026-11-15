package com.example.data.repository

import com.example.domain.repository.SyncOperationResult
import com.example.domain.repository.SyncRepository
import com.example.domain.repository.SyncStatus
import com.example.domain.repository.SyncUiSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSyncRepository : SyncRepository {
    private val state = MutableStateFlow(SyncUiSummary())
    override val syncSummary: Flow<SyncUiSummary> = state

    override suspend fun syncNow(manual: Boolean): SyncOperationResult {
        state.value = state.value.copy(
            status = SyncStatus.SUCCESS,
            pendingChangesCount = 0,
            lastSyncedAt = System.currentTimeMillis(),
            statusMessage = "Đã đồng bộ mọi tiến độ"
        )
        return SyncOperationResult.Success(0, 0, "Đã đồng bộ xong.")
    }

    override suspend fun setSyncWifiOnly(wifiOnly: Boolean) {
        state.value = state.value.copy(syncWifiOnly = wifiOnly)
    }

    override suspend fun retryFailedItems(): SyncOperationResult = syncNow(manual = true)

    override suspend fun enqueueFullLocalStateSnapshotIfNeeded() {}
}
