package com.example.data.remote.pocketbase

import retrofit2.HttpException
import java.io.IOException
import javax.net.ssl.SSLPeerUnverifiedException

sealed class RemoteSyncResult<out T> {
    data class Success<T>(val data: T) : RemoteSyncResult<T>()
    data class NetworkError(val message: String) : RemoteSyncResult<Nothing>()
    data class Unauthorized(val message: String) : RemoteSyncResult<Nothing>()
    data class RateLimited(val message: String) : RemoteSyncResult<Nothing>()
    data class ValidationError(val message: String) : RemoteSyncResult<Nothing>()
    data class ServerError(val message: String) : RemoteSyncResult<Nothing>()
}

interface SyncRemoteDataSource {
    suspend fun pushBatch(items: List<SyncPushItemRequest>): RemoteSyncResult<SyncPushResponse>
    suspend fun pullDelta(since: String, limit: Int = 100): RemoteSyncResult<SyncPullResponse>
}

class PocketBaseSyncRemoteDataSource(
    private val api: PocketBaseApi
) : SyncRemoteDataSource {

    override suspend fun pushBatch(items: List<SyncPushItemRequest>): RemoteSyncResult<SyncPushResponse> {
        return safeSyncCall { api.pushSyncBatch(SyncPushRequest(items)) }
    }

    override suspend fun pullDelta(since: String, limit: Int): RemoteSyncResult<SyncPullResponse> {
        return safeSyncCall { api.pullSyncDelta(since = since, limit = limit) }
    }

    private suspend fun <T> safeSyncCall(block: suspend () -> T): RemoteSyncResult<T> {
        return try {
            RemoteSyncResult.Success(block())
        } catch (e: SSLPeerUnverifiedException) {
            RemoteSyncResult.NetworkError("Chứng chỉ HTTPS máy chủ không khớp. Vui lòng cập nhật ứng dụng.")
        } catch (e: IOException) {
            RemoteSyncResult.NetworkError("Không có kết nối mạng hoặc máy chủ đang tạm nghỉ.")
        } catch (e: HttpException) {
            val code = e.code()
            when (code) {
                401, 403 -> RemoteSyncResult.Unauthorized("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để tiếp tục đồng bộ.")
                429 -> RemoteSyncResult.RateLimited("Bạn đang đồng bộ hơi nhanh, ứng dụng sẽ tự thử lại sau ít phút.")
                400, 422 -> RemoteSyncResult.ValidationError("Dữ liệu đồng bộ không hợp lệ (HTTP $code).")
                else -> RemoteSyncResult.ServerError("Máy chủ đồng bộ đang bận (HTTP $code).")
            }
        } catch (e: Exception) {
            RemoteSyncResult.ServerError("Lỗi đồng bộ tạm thời: ${e.message ?: "Không xác định"}")
        }
    }
}
