package com.example.data.remote.push

import com.example.data.remote.pocketbase.PocketBaseApi
import com.example.data.remote.pocketbase.RegisterDeviceTokenRequest
import com.example.data.remote.pocketbase.UnregisterDeviceTokenRequest
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLPeerUnverifiedException
import kotlin.coroutines.cancellation.CancellationException

/**
 * F20a – Triển khai gọi PocketBase. Không log token hay nội dung thông báo.
 */
class PocketBasePushTokenRemoteDataSource(
    private val api: PocketBaseApi
) : PushTokenRemoteDataSource {

    override suspend fun registerToken(token: String, platform: String): PushTokenRemoteResult =
        executeSafe {
            api.registerDeviceToken(RegisterDeviceTokenRequest(token = token, platform = platform))
        }

    override suspend fun unregisterToken(token: String): PushTokenRemoteResult =
        executeSafe {
            api.unregisterDeviceToken(UnregisterDeviceTokenRequest(token = token))
        }

    private suspend inline fun <T> executeSafe(crossinline block: suspend () -> T): PushTokenRemoteResult {
        return try {
            block()
            PushTokenRemoteResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: SSLPeerUnverifiedException) {
            PushTokenRemoteResult.NetworkError("Chứng chỉ HTTPS máy chủ không hợp lệ.")
        } catch (e: UnknownHostException) {
            PushTokenRemoteResult.NetworkError("Không kết nối được tới máy chủ.")
        } catch (e: SocketTimeoutException) {
            PushTokenRemoteResult.NetworkError("Kết nối máy chủ quá thời gian.")
        } catch (e: IOException) {
            PushTokenRemoteResult.NetworkError("Không có kết nối mạng.")
        } catch (e: HttpException) {
            when (e.code()) {
                401, 403 -> PushTokenRemoteResult.Unauthorized
                429 -> PushTokenRemoteResult.NetworkError("Máy chủ đang giới hạn tần suất, sẽ thử lại sau.")
                in 500..599 -> PushTokenRemoteResult.ServerError("Máy chủ tạm thời không phản hồi.")
                else -> PushTokenRemoteResult.ServerError("Yêu cầu đăng ký thông báo không hợp lệ.")
            }
        } catch (e: Exception) {
            PushTokenRemoteResult.ServerError("Đã xảy ra lỗi kết nối máy chủ.")
        }
    }
}
