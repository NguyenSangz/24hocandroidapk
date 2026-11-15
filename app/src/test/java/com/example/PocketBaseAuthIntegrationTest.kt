package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AttemptEntity
import com.example.data.local.security.InMemoryTokenStorage
import com.example.data.preferences.UserPreferencesManager
import com.example.data.remote.pocketbase.PocketBaseAuthRemoteDataSource
import com.example.data.remote.pocketbase.PocketBaseNetworkModule
import com.example.data.repository.PocketBaseAuthRepository
import com.example.data.repository.SettingsRepositoryImpl
import com.example.domain.repository.AccountMode
import com.example.domain.repository.AuthOperationResult
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress

/**
 * Kiểm thử tích hợp Retrofit + OkHttp + Moshi + Room cho PocketBaseAuthRepository (F16a).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PocketBaseAuthIntegrationTest {

    private lateinit var server: HttpServer
    private lateinit var database: AppDatabase
    private lateinit var tokenStorage: InMemoryTokenStorage
    private lateinit var repository: PocketBaseAuthRepository
    private var nextRefreshStatusCode = 200
    private var parentalConsentRequestStatusCode = 200
    private var parentalConsentConfirmed = false
    private var lastAuthHeaderSeen: String? = null
    private var lastRequestBodySeen: String? = null

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = UserPreferencesManager(context)
        val settingsRepo = SettingsRepositoryImpl(prefs, database)
        tokenStorage = InMemoryTokenStorage()

        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/collections/users/records") { ex ->
            lastAuthHeaderSeen = ex.requestHeaders.getFirst("Authorization")
            lastRequestBodySeen = ex.requestBody.bufferedReader().readText()
            val body = """
                {
                  "id": "pb_user_001",
                  "email": "hs@example.com",
                  "displayName": "Minh Anh",
                  "birthYear": 2012,
                  "isMinorUnder16": true,
                  "parentalConsentGranted": false,
                  "authProvider": "EMAIL",
                  "selectedGrade": 7,
                  "defaultSeries": "kntt"
                }
            """.trimIndent().toByteArray()
            ex.responseHeaders.add("Content-Type", "application/json")
            ex.sendResponseHeaders(200, body.size.toLong())
            ex.responseBody.use { it.write(body) }
        }

        server.createContext("/api/collections/users/auth-with-password") { ex ->
            lastRequestBodySeen = ex.requestBody.bufferedReader().readText()
            val body = """
                {
                  "token": "jwt_pb_secret_token_001",
                  "record": {
                    "id": "pb_user_001",
                    "email": "hs@example.com",
                    "displayName": "Minh Anh",
                    "birthYear": 2012,
                    "isMinorUnder16": true,
                    "parentalConsentGranted": false,
                    "authProvider": "EMAIL",
                    "selectedGrade": 7,
                    "defaultSeries": "kntt"
                  }
                }
            """.trimIndent().toByteArray()
            ex.responseHeaders.add("Content-Type", "application/json")
            ex.sendResponseHeaders(200, body.size.toLong())
            ex.responseBody.use { it.write(body) }
        }

        server.createContext("/api/custom/google-signin") { ex ->
            lastRequestBodySeen = ex.requestBody.bufferedReader().readText()
            val body = """
                {
                  "token": "jwt_pb_google_token_002",
                  "record": {
                    "id": "pb_google_002",
                    "email": "google_hs@gmail.com",
                    "displayName": "Huy Google",
                    "birthYear": 2013,
                    "isMinorUnder16": true,
                    "parentalConsentGranted": false,
                    "authProvider": "GOOGLE"
                  }
                }
            """.trimIndent().toByteArray()
            ex.responseHeaders.add("Content-Type", "application/json")
            ex.sendResponseHeaders(200, body.size.toLong())
            ex.responseBody.use { it.write(body) }
        }

        server.createContext("/api/custom/parental-consent/request") { ex ->
            lastAuthHeaderSeen = ex.requestHeaders.getFirst("Authorization")
            lastRequestBodySeen = ex.requestBody.bufferedReader().readText()
            val statusCode = parentalConsentRequestStatusCode
            val body = if (statusCode == 200) {
                """{"verificationRequired":true,"expiresAt":"2026-05-01T11:30:00.000Z"}""".toByteArray()
            } else {
                """{"code":$statusCode,"message":"Email unavailable"}""".toByteArray()
            }
            ex.responseHeaders.add("Content-Type", "application/json")
            ex.sendResponseHeaders(statusCode, body.size.toLong())
            ex.responseBody.use { it.write(body) }
        }

        server.createContext("/api/collections/users/auth-refresh") { ex ->
            lastAuthHeaderSeen = ex.requestHeaders.getFirst("Authorization")
            if (nextRefreshStatusCode == 401) {
                val err = """{"code":401,"message":"Token expired"}""".toByteArray()
                ex.sendResponseHeaders(401, err.size.toLong())
                ex.responseBody.use { it.write(err) }
            } else {
                val body = """
                    {
                      "token": "jwt_refreshed_999",
                      "record": {
                        "id": "pb_user_001",
                        "email": "hs@example.com",
                        "displayName": "Minh Anh",
                        "birthYear": 2012,
                        "isMinorUnder16": true,
                        "parentalConsentGranted": ${parentalConsentConfirmed},
                        "parentEmail": "bo@example.com",
                        "parentConsentAt": ${if (parentalConsentConfirmed) "\"2026-05-01T10:30:00.000Z\"" else "null"}
                      }
                    }
                """.trimIndent().toByteArray()
                ex.sendResponseHeaders(200, body.size.toLong())
                ex.responseBody.use { it.write(body) }
            }
        }

        server.createContext("/api/collections/users/request-password-reset") { ex ->
            lastRequestBodySeen = ex.requestBody.bufferedReader().readText()
            ex.sendResponseHeaders(204, -1)
            ex.close()
        }

        server.createContext("/api/custom/account") { ex ->
            lastAuthHeaderSeen = ex.requestHeaders.getFirst("Authorization")
            val body = """{"deleted":true,"deletedAt":"2026-05-01T11:00:00.000Z"}""".toByteArray()
            ex.responseHeaders.add("Content-Type", "application/json")
            ex.sendResponseHeaders(200, body.size.toLong())
            ex.responseBody.use { it.write(body) }
        }

        server.start()
        val api = PocketBaseNetworkModule.createPocketBaseApi(
            baseUrl = "http://127.0.0.1:${server.address.port}",
            tokenStorage = tokenStorage,
            allowLocalhostHttpForTest = true
        )
        repository = PocketBaseAuthRepository(
            accountDao = database.accountDao(),
            remoteDataSource = PocketBaseAuthRemoteDataSource(api),
            tokenStorage = tokenStorage,
            settingsRepository = settingsRepo,
            preferencesManager = prefs
        )
    }

    @After
    fun tearDown() {
        server.stop(0)
        database.close()
    }

    @Test
    fun httpsEnforcedForNonLocalUrls() {
        try {
            PocketBaseNetworkModule.normalizeAndValidateBaseUrl("http://api.tenmiencuatoi.vn")
            fail("Expected IllegalArgumentException for non-HTTPS public URL")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("HTTPS"))
        }
        assertEquals(
            "https://api.tenmiencuatoi.vn/",
            PocketBaseNetworkModule.normalizeAndValidateBaseUrl("https://api.tenmiencuatoi.vn/")
        )
    }

    @Test
    fun upgradeFromGuestToEmail_preservesLocalRoomProgressAndStoresOnlyJwt() = runBlocking {
        database.attemptDao().insertAttempt(
            AttemptEntity(
                questionId = "q_old_1",
                userAnswer = "A",
                isCorrect = true,
                score = 10f,
                timeSpentSeconds = 15,
                timestamp = 1700000000000L
            )
        )

        val res = repository.registerWithEmail("hs@example.com", "SecretPass123", "Minh Anh", 2012)
        assertTrue(res is AuthOperationResult.Success)
        val acc = (res as AuthOperationResult.Success).account
        assertEquals("pb_user_001", acc.uid)
        assertEquals("jwt_pb_secret_token_001", tokenStorage.getToken())
        // Dữ liệu học tập cũ trong Room còn nguyên 100%
        assertEquals(1, database.attemptDao().getAllAttempts().first().size)
    }

    @Test
    fun googleIdToken_parentalConsent_refresh401_andDeleteKeepLocalWorkEndToEnd() = runBlocking {
        // 1. Google ID Token sign-in gửi lên /api/custom/google-signin
        val googleRes = repository.signInWithGoogleIdToken("google_id_token_xyz", 2013, "Huy Google")
        assertTrue(googleRes is AuthOperationResult.Success)
        assertTrue(lastRequestBodySeen!!.contains("google_id_token_xyz"))
        assertEquals("jwt_pb_google_token_002", tokenStorage.getToken())

        // 2. Yêu cầu xác nhận -> chỉ gửi email; chưa được bật đồng bộ.
        val consentRes = repository.submitParentalConsent(2012, "bo@example.com")
        assertTrue(consentRes is AuthOperationResult.Success)
        assertEquals("Bearer jwt_pb_google_token_002", lastAuthHeaderSeen)
        val pendingConsent = (consentRes as AuthOperationResult.Success).account
        assertTrue(pendingConsent.needsParentalConsent)
        assertNull(pendingConsent.parentalConsentTimestamp)

        // Phụ huynh xác nhận qua liên kết; lần làm mới lấy trạng thái do server cấp.
        parentalConsentConfirmed = true
        val confirmedState = repository.refreshSessionIfNeeded()
        assertTrue(confirmedState.parentalConsentGranted)
        assertNotNull(confirmedState.parentalConsentTimestamp)

        // 3. Quên mật khẩu gọi đúng endpoint SMTP
        val resetRes = repository.requestPasswordReset("hs@example.com")
        assertTrue(resetRes is AuthOperationResult.Success)

        // 4. Refresh gặp 401 -> đánh dấu hết phiên nhẹ nhàng, không mất dữ liệu
        nextRefreshStatusCode = 401
        val expiredState = repository.refreshSessionIfNeeded()
        assertTrue(expiredState.sessionExpired)
        assertTrue(expiredState.isAuthenticated)

        // 5. Xóa tài khoản giữ dữ liệu máy
        database.attemptDao().insertAttempt(
            AttemptEntity(
                questionId = "q_keep",
                userAnswer = "B",
                isCorrect = true,
                score = 10f,
                timeSpentSeconds = 10,
                timestamp = 1700000000001L
            )
        )
        val deleteRes = repository.deleteAccount(deleteLocalData = false)
        assertTrue(deleteRes is AuthOperationResult.Success)
        assertNull(tokenStorage.getToken())
        assertEquals(AccountMode.GUEST, repository.getCurrentAccount().mode)
        assertEquals(1, database.attemptDao().getAllAttempts().first().size)
    }

    @Test
    fun parentalConsentEmailFailure_doesNotGrantConsentLocally() = runBlocking {
        repository.signInWithGoogleIdToken("google_id_token_xyz", 2013, "Huy Google")
        parentalConsentRequestStatusCode = 503

        val result = repository.submitParentalConsent(2013, "parent@example.com")

        assertTrue(result is AuthOperationResult.Error)
        assertTrue(repository.getCurrentAccount().needsParentalConsent)
        assertFalse(repository.getCurrentAccount().parentalConsentGranted)
    }
}
