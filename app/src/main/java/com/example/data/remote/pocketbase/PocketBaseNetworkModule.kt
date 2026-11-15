package com.example.data.remote.pocketbase

import com.example.BuildConfig
import com.example.data.local.security.TokenStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.CertificatePinner
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Khởi tạo Retrofit/OkHttp cho PocketBase:
 * - Bắt buộc HTTPS cho mọi tên miền công khai.
 * - Đính kèm Authorization Bearer JWT từ Keystore TokenStorage.
 * - Hỗ trợ Certificate Pinning tùy chọn (primary + backup pin để xoay khóa không gián đoạn).
 */
object PocketBaseNetworkModule {

    fun normalizeAndValidateBaseUrl(
        rawUrl: String,
        allowLocalhostHttpForTest: Boolean = false
    ): String {
        val trimmed = rawUrl.trim().trimEnd('/')
        val parsed = trimmed.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("URL máy chủ không hợp lệ.")
        val isLocalTestHost = allowLocalhostHttpForTest &&
            (parsed.host == "127.0.0.1" || parsed.host == "localhost")
        if (parsed.scheme != "https" && !isLocalTestHost) {
            throw IllegalArgumentException("Kết nối máy chủ bắt buộc phải dùng HTTPS.")
        }
        return "$trimmed/"
    }

    fun createMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun createOkHttpClient(
        baseUrl: String,
        tokenStorage: TokenStorage,
        primaryPin: String = BuildConfig.POCKETBASE_CERT_PIN_PRIMARY,
        backupPin: String = BuildConfig.POCKETBASE_CERT_PIN_BACKUP,
        isDebug: Boolean = BuildConfig.DEBUG
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val token = tokenStorage.getToken()
                val reqBuilder = original.newBuilder()
                    .header("Accept", "application/json")
                if (!token.isNullOrBlank() && original.header("Authorization") == null) {
                    reqBuilder.header("Authorization", "Bearer $token")
                }
                chain.proceed(reqBuilder.build())
            }

        val parsedHost = baseUrl.toHttpUrlOrNull()?.host
        val pins = listOf(primaryPin.trim(), backupPin.trim()).filter { it.startsWith("sha256/") }
        if (!parsedHost.isNullOrBlank() && pins.isNotEmpty()) {
            val pinnerBuilder = CertificatePinner.Builder()
            pins.forEach { pin -> pinnerBuilder.add(parsedHost, pin) }
            builder.certificatePinner(pinnerBuilder.build())
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (isDebug) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
            redactHeader("Cookie")
        }
        builder.addInterceptor(logging)

        return builder.build()
    }

    fun createPocketBaseApi(
        baseUrl: String = BuildConfig.POCKETBASE_BASE_URL,
        tokenStorage: TokenStorage,
        allowLocalhostHttpForTest: Boolean = false
    ): PocketBaseApi {
        val normalizedUrl = normalizeAndValidateBaseUrl(baseUrl, allowLocalhostHttpForTest)
        val moshi = createMoshi()
        val client = createOkHttpClient(normalizedUrl, tokenStorage)
        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PocketBaseApi::class.java)
    }
}
