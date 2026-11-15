package com.example.data.remote.pocketbase

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncPushItemRequest(
    val collection: String,
    val entityKey: String,
    val sourceType: String? = null,
    val sourceId: String? = null,
    val payload: Map<String, Any?> = emptyMap(),
    val deleted: Boolean = false,
    val clientUpdatedAt: Long
)

@JsonClass(generateAdapter = true)
data class SyncPushRequest(
    val items: List<SyncPushItemRequest>
)

@JsonClass(generateAdapter = true)
data class SyncPushAckItem(
    val collection: String = "",
    val entityKey: String = "",
    val serverUpdated: String = "",
    val status: String = ""
)

@JsonClass(generateAdapter = true)
data class SyncPushResponse(
    val acceptedCount: Int = 0,
    val serverTime: String = "",
    val results: List<SyncPushAckItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SyncRemoteRecordDto(
    val collection: String = "",
    val id: String = "",
    val entityKey: String = "",
    val sourceType: String? = null,
    val sourceId: String? = null,
    val payload: Map<String, Any?> = emptyMap(),
    val deleted: Boolean = false,
    val clientUpdatedAt: Long = 0L,
    val updated: String = ""
)

@JsonClass(generateAdapter = true)
data class SyncPullResponse(
    val items: List<SyncRemoteRecordDto> = emptyList(),
    val nextCursor: String = "",
    val hasMore: Boolean = false,
    val serverTime: String = ""
)
