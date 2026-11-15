package com.example.data.remote.pocketbase

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit REST interface kết nối tới PocketBase server tự host.
 */
interface PocketBaseApi {

    @POST("api/collections/users/records")
    suspend fun registerUser(
        @Body body: RegisterUserRequest
    ): PocketBaseUserDto

    @POST("api/collections/users/auth-with-password")
    suspend fun authWithPassword(
        @Body body: PasswordAuthRequest
    ): PocketBaseAuthResponse

    @POST("api/collections/users/auth-refresh")
    suspend fun authRefresh(): PocketBaseAuthResponse

    @POST("api/collections/users/request-password-reset")
    suspend fun requestPasswordReset(
        @Body body: PasswordResetRequest
    ): Response<Unit>

    @PATCH("api/collections/users/records/{id}")
    suspend fun updateUserRecord(
        @Path("id") userId: String,
        @Body body: UpdateDisplayNameRequest
    ): PocketBaseUserDto

    @POST("api/custom/google-signin")
    suspend fun signInWithGoogleIdToken(
        @Body body: GoogleSignInRequest
    ): PocketBaseAuthResponse

    @POST("api/custom/parental-consent/request")
    suspend fun requestParentalConsent(
        @Body body: ParentalConsentRequest
    ): ParentalConsentRequestResponse

    @POST("api/custom/parental-consent/confirm")
    suspend fun confirmParentalConsent(
        @Body body: ParentalConsentConfirmationRequest
    ): ParentalConsentConfirmationResponse

    @DELETE("api/custom/account")
    suspend fun deleteAccount(): DeleteAccountResponse

    @POST("api/custom/sync/push")
    suspend fun pushSyncBatch(
        @Body body: SyncPushRequest
    ): SyncPushResponse

    @GET("api/custom/sync/pull")
    suspend fun pullSyncDelta(
        @Query("since") since: String,
        @Query("limit") limit: Int = 100
    ): SyncPullResponse

    // F20a – Thông báo đẩy: đăng ký/xóa token thiết bị (idempotent, theo tài khoản).
    @POST("api/custom/push/register")
    suspend fun registerDeviceToken(
        @Body body: RegisterDeviceTokenRequest
    ): PushTokenResponse

    @POST("api/custom/push/unregister")
    suspend fun unregisterDeviceToken(
        @Body body: UnregisterDeviceTokenRequest
    ): PushTokenResponse
}
