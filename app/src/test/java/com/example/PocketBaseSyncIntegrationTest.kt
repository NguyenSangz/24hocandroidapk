package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.FlashcardProgressEntity
import com.example.data.local.entity.LessonEntity
import com.example.data.local.entity.WrongQuestionEntity
import com.example.data.local.entity.XpEventEntity
import com.example.data.local.security.InMemoryTokenStorage
import com.example.data.preferences.UserPreferencesManager
import com.example.data.remote.pocketbase.PocketBaseAuthRemoteDataSource
import com.example.data.remote.pocketbase.PocketBaseNetworkModule
import com.example.data.remote.pocketbase.PocketBaseSyncRemoteDataSource
import com.example.data.repository.PocketBaseAuthRepository
import com.example.data.repository.SettingsRepositoryImpl
import com.example.data.repository.SyncRepositoryImpl
import com.example.data.sync.SyncNetworkChecker
import com.example.data.sync.SyncNetworkState
import com.example.data.sync.SyncOutboxHelper
import com.example.domain.repository.SyncOperationResult
import com.example.domain.repository.SyncStatus
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicLong

/**
 * Kiểm thử tích hợp Offline-First Sync End-to-End (F16b) dùng Robolectric + Mock PocketBase HttpServer:
 * 1. Thiết bị A học offline -> vào sync_outbox -> đồng bộ lên server -> xóa outbox, cập nhật cursor.
 * 2. Chống cộng đúp XP khi đồng bộ lặp lại nhiều lần.
 * 3. Thiết bị B kéo delta từ server theo con trỏ `updated` -> cập nhật Room (bài học, XP, SM-2 flashcard).
 * 4. Đồng bộ xóa mềm (tombstone) câu sai lan truyền sang thiết bị B.
 * 5. Tuân thủ công tắc "Chỉ đồng bộ qua Wi-Fi", HTTP 429 rate limit và mất mạng không chặn thao tác Room.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PocketBaseSyncIntegrationTest {

    private lateinit var server: HttpServer
    private lateinit var context: Context
    private val serverRecords = linkedMapOf<String, ServerSyncRecord>()
    private val serverTick = AtomicLong(1000L)
    private var forceHttp429 = false
    private var pushCallsCount = 0

    data class ServerSyncRecord(
        val collection: String,
        val entityKey: String,
        val sourceType: String?,
        val sourceId: String?,
        val payload: JSONObject,
        val deleted: Boolean,
        val clientUpdatedAt: Long,
        val updated: String
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        serverRecords.clear()
        forceHttp429 = false
        pushCallsCount = 0

        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/custom/sync/push") { ex ->
            pushCallsCount++
            if (forceHttp429) {
                val err = """{"message":"Bạn đồng bộ quá nhanh, vui lòng thử lại sau ít phút."}""".toByteArray()
                ex.responseHeaders.add("Content-Type", "application/json")
                ex.sendResponseHeaders(429, err.size.toLong())
                ex.responseBody.use { it.write(err) }
                return@createContext
            }

            val reqJson = JSONObject(ex.requestBody.bufferedReader().readText())
            val items = reqJson.getJSONArray("items")
            val results = JSONArray()
            var lastTime = ""

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val col = item.getString("collection")
                val key = item.getString("entityKey")
                val deleted = item.optBoolean("deleted", false)
                val clientTs = item.optLong("clientUpdatedAt", 0L)
                val payload = item.optJSONObject("payload") ?: JSONObject()
                val sourceType = if (item.isNull("sourceType")) null else item.optString("sourceType")
                val sourceId = if (item.isNull("sourceId")) null else item.optString("sourceId")
                val tick = serverTick.incrementAndGet()
                val updatedStamp = "2026-03-01 10:${String.format("%02d", (tick / 60) % 60)}:${String.format("%02d", tick % 60)}.${String.format("%03d", tick % 1000)}Z"
                lastTime = updatedStamp

                val compositeKey = "$col::$key"
                val existing = serverRecords[compositeKey]
                // Server-side dedup for XP events
                if (col == "sync_xp_events" && existing != null) {
                    results.put(JSONObject().apply {
                        put("collection", col)
                        put("entityKey", key)
                        put("serverUpdated", existing.updated)
                        put("status", "deduplicated")
                    })
                } else {
                    serverRecords[compositeKey] = ServerSyncRecord(
                        collection = col,
                        entityKey = key,
                        sourceType = sourceType,
                        sourceId = sourceId,
                        payload = payload,
                        deleted = deleted,
                        clientUpdatedAt = clientTs,
                        updated = updatedStamp
                    )
                    results.put(JSONObject().apply {
                        put("collection", col)
                        put("entityKey", key)
                        put("serverUpdated", updatedStamp)
                        put("status", if (existing == null) "created" else "merged")
                    })
                }
            }

            val resp = JSONObject().apply {
                put("acceptedCount", items.length())
                put("serverTime", lastTime)
                put("results", results)
            }.toString().toByteArray()
            ex.responseHeaders.add("Content-Type", "application/json")
            ex.sendResponseHeaders(200, resp.size.toLong())
            ex.responseBody.use { it.write(resp) }
        }

        server.createContext("/api/custom/sync/pull") { ex ->
            val query = ex.requestURI.rawQuery.orEmpty()
            val sinceParam = query.split("&")
                .firstOrNull { it.startsWith("since=") }
                ?.substringAfter("since=")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                .orEmpty()

            val matched = serverRecords.values
                .filter { sinceParam.isBlank() || it.updated > sinceParam }
                .sortedBy { it.updated }

            val arr = JSONArray()
            matched.forEach { rec ->
                arr.put(JSONObject().apply {
                    put("collection", rec.collection)
                    put("id", "pb_${rec.entityKey}")
                    put("entityKey", rec.entityKey)
                    if (rec.sourceType != null) put("sourceType", rec.sourceType)
                    if (rec.sourceId != null) put("sourceId", rec.sourceId)
                    put("payload", rec.payload)
                    put("deleted", rec.deleted)
                    put("clientUpdatedAt", rec.clientUpdatedAt)
                    put("updated", rec.updated)
                })
            }

            val nextCursor = matched.lastOrNull()?.updated ?: sinceParam
            val resp = JSONObject().apply {
                put("items", arr)
                put("nextCursor", nextCursor)
                put("hasMore", false)
                put("serverTime", nextCursor)
            }.toString().toByteArray()

            ex.responseHeaders.add("Content-Type", "application/json")
            ex.sendResponseHeaders(200, resp.size.toLong())
            ex.responseBody.use { it.write(resp) }
        }

        server.start()
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    private fun createDeviceStack(
        baseUrl: String = "http://127.0.0.1:${server.address.port}",
        networkState: SyncNetworkState = SyncNetworkState(isConnected = true, isUnmeteredWifi = true)
    ): DeviceStack {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = UserPreferencesManager(context)
        val settingsRepo = SettingsRepositoryImpl(prefs, db)
        val tokenStorage = InMemoryTokenStorage()
        tokenStorage.saveSession("jwt_sync_test_token", "pb_sync_user_1")

        val api = PocketBaseNetworkModule.createPocketBaseApi(
            baseUrl = baseUrl,
            tokenStorage = tokenStorage,
            allowLocalhostHttpForTest = true
        )
        val authRepo = PocketBaseAuthRepository(
            accountDao = db.accountDao(),
            remoteDataSource = PocketBaseAuthRemoteDataSource(api),
            tokenStorage = tokenStorage,
            settingsRepository = settingsRepo,
            preferencesManager = prefs,
            syncDao = db.syncDao()
        )
        val netChecker = object : SyncNetworkChecker {
            var state = networkState
            override fun currentState(): SyncNetworkState = state
        }
        val syncRepo = SyncRepositoryImpl(
            database = db,
            remoteDataSource = PocketBaseSyncRemoteDataSource(api),
            authRepository = authRepo,
            tokenStorage = tokenStorage,
            preferencesManager = prefs,
            networkChecker = netChecker
        )
        return DeviceStack(db, prefs, tokenStorage, authRepo, syncRepo, netChecker)
    }

    private suspend fun seedCommonCurriculum(db: AppDatabase) {
        db.chapterDao().insertAll(
            listOf(ChapterEntity(id = "c_toan6_1", subjectId = "toan", grade = 6, order = 1, title = "Số tự nhiên", description = ""))
        )
        db.lessonDao().insertAll(
            listOf(
                LessonEntity(
                    id = "l_toan6_1",
                    chapterId = "c_toan6_1",
                    order = 1,
                    title = "Tập hợp",
                    summary = "Khái niệm tập hợp",
                    examples = "",
                    tips = "",
                    isCompleted = false
                )
            )
        )
        db.accountDao().upsertAccount(
            AccountEntity(
                uid = "pb_sync_user_1",
                email = "sync@example.com",
                displayName = "Lan 6A",
                accountMode = AccountEntity.MODE_AUTHENTICATED,
                authProvider = "EMAIL"
            )
        )
    }

    data class DeviceStack(
        val db: AppDatabase,
        val prefs: UserPreferencesManager,
        val tokenStorage: InMemoryTokenStorage,
        val authRepo: PocketBaseAuthRepository,
        val syncRepo: SyncRepositoryImpl,
        val netChecker: SyncNetworkChecker
    )

    @Test
    fun `two devices sync lessons xp flashcards and soft deletes without duplicate xp`() = runBlocking {
        val deviceA = createDeviceStack()
        val deviceB = createDeviceStack()
        seedCommonCurriculum(deviceA.db)
        seedCommonCurriculum(deviceB.db)

        // Thiết bị B ban đầu có 1 câu sai q_wrong_1
        deviceB.db.wrongQuestionDao().insertOrUpdate(
            WrongQuestionEntity(
                questionId = "q_wrong_1",
                grade = 6,
                subjectId = "toan",
                errorCount = 2,
                lastWrongTimestamp = 1_000L,
                status = "NEEDS_REVIEW"
            )
        )

        // Thiết bị A học: hoàn thành bài học l_toan6_1, nhận XP và học Flashcard SM-2
        deviceA.db.lessonDao().updateLessonCompleted("l_toan6_1", true)
        SyncOutboxHelper.enqueueLessonProgress(deviceA.db, "l_toan6_1", true, now = 2_000L)

        val xp = XpEventEntity(
            eventType = "LESSON_COMPLETED",
            eventKey = "lesson_l_toan6_1",
            xpAmount = 10,
            timestamp = 2_000L,
            metadata = "Tập hợp"
        )
        deviceA.db.gamificationDao().insertXpEvent(xp)
        SyncOutboxHelper.enqueueXpEvent(deviceA.db, xp)

        val fcProg = FlashcardProgressEntity(
            cardId = "fc_en_1",
            deckId = "deck_en6",
            repetitions = 3,
            intervalDays = 6,
            easeFactor = 2.6f,
            consecutiveCorrect = 3,
            nextReviewTimestamp = 900_000L,
            lastReviewTimestamp = 2_500L,
            state = "REVIEW",
            totalReviews = 4,
            lapses = 0
        )
        deviceA.db.flashcardDao().insertProgress(fcProg)
        SyncOutboxHelper.enqueueFlashcardProgress(deviceA.db, fcProg, now = 2_500L)

        // Đồng bộ A lên server
        val resA = deviceA.syncRepo.syncNow(manual = true)
        assertTrue(resA is SyncOperationResult.Success)
        assertEquals(0, deviceA.db.syncDao().getPendingCountSync())
        assertTrue(deviceA.db.syncDao().getSyncStateSync("pb_sync_user_1")!!.lastDeltaCursor.isNotBlank())

        // Đồng bộ lại trên A lần nữa: tổng XP không đổi
        val resA2 = deviceA.syncRepo.syncNow(manual = true)
        assertTrue(resA2 is SyncOperationResult.Success)
        assertEquals(10, deviceA.db.gamificationDao().getTotalXpSync())

        // Thiết bị B đồng bộ kéo delta về
        val resB = deviceB.syncRepo.syncNow(manual = true)
        assertTrue(resB is SyncOperationResult.Success)

        val lessonOnB = deviceB.db.lessonDao().getLessonById("l_toan6_1")
        assertNotNull(lessonOnB)
        assertTrue(lessonOnB!!.isCompleted)
        assertEquals(10, deviceB.db.gamificationDao().getTotalXpSync())

        val fcOnB = deviceB.db.flashcardDao().getProgressForCard("fc_en_1")
        assertNotNull(fcOnB)
        assertEquals(4, fcOnB!!.totalReviews)
        assertEquals(6, fcOnB.intervalDays)

        // Thiết bị A xóa mềm câu sai q_wrong_1 và đồng bộ -> Thiết bị B kéo về và xóa cục bộ
        SyncOutboxHelper.enqueueWrongQuestionDeletion(deviceA.db, "q_wrong_1", now = 5_000L)
        assertTrue(deviceA.syncRepo.syncNow(manual = true) is SyncOperationResult.Success)
        assertTrue(deviceB.syncRepo.syncNow(manual = true) is SyncOperationResult.Success)
        assertNull(deviceB.db.wrongQuestionDao().getByQuestionId("q_wrong_1"))

        deviceA.db.close()
        deviceB.db.close()
    }

    @Test
    fun `wifi only pauses on metered network and preserves outbox`() = runBlocking {
        val device = createDeviceStack(
            networkState = SyncNetworkState(isConnected = true, isUnmeteredWifi = false)
        )
        seedCommonCurriculum(device.db)
        device.syncRepo.setSyncWifiOnly(true)
        SyncOutboxHelper.enqueueLessonProgress(device.db, "l_toan6_1", true)

        val res = device.syncRepo.syncNow(manual = true)
        assertTrue(res is SyncOperationResult.Skipped)
        assertEquals(SyncStatus.PAUSED_METERED, (res as SyncOperationResult.Skipped).reason)
        assertEquals(0, pushCallsCount)
        assertTrue(device.db.syncDao().getPendingCountSync() >= 1)

        val summary = device.syncRepo.syncSummary.first()
        assertEquals(SyncStatus.PAUSED_METERED, summary.status)
        assertTrue(summary.syncWifiOnly)

        device.db.close()
    }

    @Test
    fun `minor without parental consent cannot sync and keeps local outbox`() = runBlocking {
        val device = createDeviceStack()
        seedCommonCurriculum(device.db)
        device.db.accountDao().upsertAccount(
            AccountEntity(
                uid = "pb_sync_user_1",
                email = "sync@example.com",
                displayName = "Lan 6A",
                accountMode = AccountEntity.MODE_AUTHENTICATED,
                birthYear = 2013,
                isMinorUnder16 = true,
                parentalConsentGranted = false
            )
        )
        SyncOutboxHelper.enqueueLessonProgress(device.db, "l_toan6_1", true, now = 2_500L)

        val result = device.syncRepo.syncNow(manual = true)

        assertTrue(result is SyncOperationResult.Skipped)
        assertEquals(0, pushCallsCount)
        assertEquals(1, device.db.syncDao().getPendingCountSync())
        device.db.close()
    }

    @Test
    fun `rate limit HTTP 429 and unreachable server preserve outbox and increment retry`() = runBlocking {
        val device = createDeviceStack()
        seedCommonCurriculum(device.db)
        SyncOutboxHelper.enqueueLessonProgress(device.db, "l_toan6_1", true)

        // Giả lập server trả về HTTP 429
        forceHttp429 = true
        val res429 = device.syncRepo.syncNow(manual = true)
        assertTrue(res429 is SyncOperationResult.Error)
        assertTrue((res429 as SyncOperationResult.Error).isRateLimited)

        val outboxAfter429 = device.db.syncDao().getAllOutboxSync()
        assertTrue(outboxAfter429.isNotEmpty())
        assertEquals(1, outboxAfter429.first().retryCount)

        // Mở cổng chết để kiểm tra NetworkError không làm hỏng Room
        val closedPort = ServerSocket(0).use { it.localPort }
        val offlineDevice = createDeviceStack(baseUrl = "http://127.0.0.1:$closedPort")
        seedCommonCurriculum(offlineDevice.db)
        offlineDevice.db.lessonDao().updateLessonCompleted("l_toan6_1", true)
        SyncOutboxHelper.enqueueLessonProgress(offlineDevice.db, "l_toan6_1", true)

        val netErr = offlineDevice.syncRepo.syncNow(manual = true)
        assertTrue(netErr is SyncOperationResult.Error)
        assertTrue((netErr as SyncOperationResult.Error).isNetworkError)
        assertTrue(offlineDevice.db.lessonDao().getLessonById("l_toan6_1")!!.isCompleted)
        assertTrue(offlineDevice.db.syncDao().getPendingCountSync() >= 1)

        device.db.close()
        offlineDevice.db.close()
    }
}
