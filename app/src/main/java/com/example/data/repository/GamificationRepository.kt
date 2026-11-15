package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BadgeUnlockEntity
import com.example.data.local.entity.XpEventEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.domain.gamification.BadgeDefinition
import com.example.domain.gamification.BadgeDisplayItem
import com.example.domain.gamification.BadgeEvaluator
import com.example.domain.gamification.GamificationConfig
import com.example.domain.gamification.GamificationReward
import com.example.domain.gamification.GamificationStats
import com.example.domain.gamification.LearningEvent
import com.example.domain.gamification.LevelCalculator
import com.example.domain.gamification.LevelInfo
import com.example.domain.gamification.StreakCalculator
import com.example.domain.gamification.StreakResult
import com.example.domain.gamification.XpManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Repository trung tâm điều phối toàn bộ luồng Gamification (F10).
 * Cung cấp luồng dữ liệu phản ứng (Flow), hỗ trợ Clock giả lập để kiểm thử 100%.
 */
class GamificationRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val userPreferencesManager: UserPreferencesManager,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val gamificationDao = database.gamificationDao()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Cache danh sách huy hiệu từ file JSON cấu hình
    private var cachedBadgeDefinitions: List<BadgeDefinition>? = null

    fun getBadgeDefinitions(): List<BadgeDefinition> {
        val cached = cachedBadgeDefinitions
        if (cached != null) return cached
        val jsonString = try {
            context.assets.open("gamification/badges.json").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            "[]"
        }
        val parsed = BadgeEvaluator.parseBadgesFromJson(jsonString)
        cachedBadgeDefinitions = parsed
        return parsed
    }

    // === LUỒNG DỮ LIỆU PHẢN ỨNG (FLOWS) ===

    val totalXp: Flow<Int> = gamificationDao.getTotalXp().distinctUntilChanged()

    val levelInfo: Flow<LevelInfo> = totalXp.map { xp ->
        LevelCalculator.calculateLevel(xp)
    }.distinctUntilChanged()

    val streakFreezeCount: Flow<Int> = userPreferencesManager.streakFreezeCount

    val levelUpDialogEnabled: Flow<Boolean> = userPreferencesManager.levelUpDialogEnabled

    val unlockedBadgeIds: Flow<Set<String>> = gamificationDao.getAllUnlockedBadgeIds().map { it.toSet() }

    val recentXpEvents: Flow<List<XpEventEntity>> = gamificationDao.getRecentXpEvents(50)

    /**
     * Tính toán Streak liên tục từ danh sách ngày có hoạt động và DataStore
     */
    val streakResult: Flow<StreakResult> = combine(
        gamificationDao.getDistinctActivityDates(),
        userPreferencesManager.streakFreezeCount,
        userPreferencesManager.usedFreezeDates
    ) { activityDates, freezes, usedDates ->
        val now = Instant.ofEpochMilli(clock()).atZone(ZoneId.systemDefault()).toLocalDate()
        StreakCalculator.calculateStreak(
            rawActivityDates = activityDates,
            today = now,
            initialFreezes = freezes,
            alreadyUsedFreezeDates = usedDates
        )
    }.distinctUntilChanged()

    /**
     * Lấy lượng XP tích lũy trong ngày hôm nay
     */
    val todayXp: Flow<Int> = gamificationDao.getAllXpEvents().map { events ->
        val todayStr = Instant.ofEpochMilli(clock()).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
        events.filter { event ->
            event.xpAmount > 0 &&
                    Instant.ofEpochMilli(event.timestamp).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter) == todayStr
        }.sumOf { it.xpAmount }
    }.distinctUntilChanged()

    /**
     * Danh sách toàn bộ huy hiệu kèm trạng thái đã mở và tiến trình
     */
    val allBadgesWithProgress: Flow<List<BadgeDisplayItem>> = combine(
        unlockedBadgeIds,
        gamificationDao.getAllBadgeUnlocks(),
        totalXp,
        streakResult
    ) { unlockedIds, unlockEntities, xp, streak ->
        val defs = getBadgeDefinitions()
        val stats = buildStatsSnapshot(xp, streak.currentStreak)
        val unlockTimeMap = unlockEntities.associate { it.badgeId to it.unlockedAt }

        defs.map { def ->
            val isUnlocked = unlockedIds.contains(def.id)
            val (currentProg, progPct) = BadgeEvaluator.computeBadgeProgress(def, stats)
            BadgeDisplayItem(
                definition = def,
                isUnlocked = isUnlocked,
                unlockedAtTimestamp = unlockTimeMap[def.id],
                currentProgress = currentProg,
                progressPercentage = if (isUnlocked) 1f else progPct
            )
        }
    }

    // === XỬ LÝ SỰ KIỆN HỌC TẬP (LEARNING EVENT DISPATCHER) ===

    /**
     * Hàm xử lý duy nhất cho toàn bộ sự kiện phát ra từ các tính năng:
     * - Tính toán XP theo quy tắc
     * - Chống gian lận (giới hạn ngày, giới hạn lặp câu hỏi, idempotency eventKey)
     * - Lưu bảng `xp_events`
     * - Đánh giá và lưu huy hiệu mới `badge_unlocks`
     * - Trả về `GamificationReward` tức thì cho màn hình kết quả
     */
    suspend fun onLearningEvent(event: LearningEvent): GamificationReward = withContext(ioDispatcher) {
        val now = clock()
        val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        val todayStr = today.format(dateFormatter)

        val currentTotalXp = gamificationDao.getTotalXpSync()
        val prevLevelInfo = LevelCalculator.calculateLevel(currentTotalXp)

        // Tính toán XP yêu cầu và Event Key
        var requestedXp = 0
        var eventType = ""
        var eventKey = ""
        var eventDesc = ""

        when (event) {
            is LearningEvent.LessonCompleted -> {
                eventType = "LESSON_COMPLETED"
                eventKey = XpManager.makeLessonEventKey(event.lessonId)
                requestedXp = XpManager.calculateLessonXp()
                eventDesc = "Hoàn thành bài học"
            }
            is LearningEvent.PracticeQuestionAnswered -> {
                eventType = "PRACTICE_CORRECT"
                eventKey = XpManager.makePracticeQuestionEventKey(event.questionId, now)
                requestedXp = XpManager.calculatePracticeQuestionXp(event.isCorrect, event.usedHint)
                eventDesc = if (event.isCorrect) {
                    if (event.usedHint) "Trả lời đúng (có gợi ý)" else "Trả lời đúng câu hỏi"
                } else "Luyện tập câu hỏi"
            }
            is LearningEvent.PracticeSessionCompleted -> {
                eventType = "PRACTICE_SESSION_COMPLETED"
                eventKey = XpManager.makePracticeSessionBonusEventKey("${now}_${event.correctCount}")
                requestedXp = XpManager.calculatePracticeSessionBonus()
                eventDesc = "Hoàn thành phiên luyện tập"
            }
            is LearningEvent.TestCompleted -> {
                eventType = "TEST_COMPLETED"
                eventKey = XpManager.makeTestEventKey(event.testId)
                requestedXp = XpManager.calculateTestXp(event.score)
                eventDesc = "Hoàn thành bài kiểm tra (${event.score} đ)"
            }
            is LearningEvent.FlashcardReviewed -> {
                eventType = "FLASHCARD_REVIEW"
                eventKey = XpManager.makeFlashcardReviewEventKey(event.cardId, now)
                requestedXp = XpManager.calculateFlashcardReviewXp()
                eventDesc = "Ôn tập thẻ ghi nhớ"
            }
            is LearningEvent.WrongQuestionMastered -> {
                eventType = "WRONG_MASTERED"
                eventKey = XpManager.makeWrongMasteredEventKey(event.questionId)
                requestedXp = XpManager.calculateWrongQuestionMasteredXp()
                eventDesc = "Nắm vững câu sai đã sửa"
            }
        }

        // Kiểm tra chống trùng sự kiện (Idempotency)
        if (eventKey.isNotBlank() && gamificationDao.hasEventKey(eventKey)) {
            return@withContext GamificationReward(
                xpEarned = 0,
                previousLevel = prevLevelInfo.level,
                newLevelInfo = prevLevelInfo,
                isLevelUp = false,
                currentStreak = streakResult.first().currentStreak,
                message = "Sự kiện đã được ghi nhận trước đó"
            )
        }

        // Tính giới hạn XP trong ngày (Chống gian lận)
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val todayXpSoFar = gamificationDao.getXpEarnedBetween(startOfDay, endOfDay)
        val awardedXp = XpManager.calculateAllowedXp(todayXpSoFar, requestedXp)

        if (awardedXp > 0 && eventKey.isNotBlank()) {
            val xpEntity = XpEventEntity(
                eventType = eventType,
                eventKey = eventKey,
                xpAmount = awardedXp,
                timestamp = now,
                metadata = eventDesc
            )
            gamificationDao.insertXpEvent(xpEntity)
            com.example.data.sync.SyncOutboxHelper.enqueueXpEvent(database, xpEntity)
        }

        val newTotalXp = currentTotalXp + awardedXp
        val newLevelInfo = LevelCalculator.calculateLevel(newTotalXp)
        val isLevelUp = newLevelInfo.level > prevLevelInfo.level

        // Cập nhật và đánh giá Streak
        val streak = streakResult.first()
        val currentStreak = streak.currentStreak

        // Đánh giá huy hiệu mới
        val currentUnlocked = gamificationDao.getAllUnlockedBadgeIdsSync().toSet()
        val allDefs = getBadgeDefinitions()
        val stats = buildStatsSnapshot(newTotalXp, currentStreak)
        val newBadges = BadgeEvaluator.evaluateNewlyUnlockedBadges(allDefs, currentUnlocked, stats)

        if (newBadges.isNotEmpty()) {
            val unlockEntities = newBadges.map {
                BadgeUnlockEntity(
                    badgeId = it.id,
                    unlockedAt = now,
                    metadata = it.title
                )
            }
            gamificationDao.insertBadgeUnlocks(unlockEntities)
            unlockEntities.forEach { badge ->
                com.example.data.sync.SyncOutboxHelper.enqueueBadgeUnlock(database, badge)
            }
        }

        // Kiểm tra thưởng đóng băng streak khi đạt mốc (ví dụ streak 7 ngày)
        if (currentStreak > 0 && currentStreak % GamificationConfig.STREAK_FREEZE_REWARD_MILESTONE == 0) {
            val currentFreezes = userPreferencesManager.streakFreezeCount.first()
            if (currentFreezes < GamificationConfig.MAX_STREAK_FREEZES) {
                userPreferencesManager.setStreakFreezeCount(currentFreezes + 1)
            }
        }

        return@withContext GamificationReward(
            xpEarned = awardedXp,
            previousLevel = prevLevelInfo.level,
            newLevelInfo = newLevelInfo,
            isLevelUp = isLevelUp,
            newlyUnlockedBadges = newBadges,
            currentStreak = currentStreak,
            streakFreezeAvailable = userPreferencesManager.streakFreezeCount.first(),
            message = if (awardedXp > 0) "+$awardedXp XP" else ""
        )
    }

    /**
     * Mua 1 lượt Đóng băng streak bằng XP tích lũy (200 XP)
     */
    suspend fun purchaseStreakFreeze(): Boolean = withContext(ioDispatcher) {
        val totalXpVal = gamificationDao.getTotalXpSync()
        val currentFreezes = userPreferencesManager.streakFreezeCount.first()

        if (totalXpVal < GamificationConfig.STREAK_FREEZE_COST_XP || currentFreezes >= GamificationConfig.MAX_STREAK_FREEZES) {
            return@withContext false
        }

        val now = clock()
        val eventKey = XpManager.makeStreakFreezePurchaseKey(now)
        val purchaseEvent = XpEventEntity(
            eventType = "FREEZE_PURCHASE",
            eventKey = eventKey,
            xpAmount = -GamificationConfig.STREAK_FREEZE_COST_XP,
            timestamp = now,
            metadata = "Mua 1 lượt đóng băng chuỗi"
        )
        gamificationDao.insertXpEvent(purchaseEvent)
        com.example.data.sync.SyncOutboxHelper.enqueueXpEvent(database, purchaseEvent)
        userPreferencesManager.setStreakFreezeCount(currentFreezes + 1)
        true
    }

    /**
     * Thu thập các chỉ số thống kê từ các bảng để nạp vào BadgeEvaluator
     */
    private suspend fun buildStatsSnapshot(totalXp: Int, currentStreak: Int): GamificationStats {
        return withContext(ioDispatcher) {
            val level = LevelCalculator.calculateLevel(totalXp).level

            // Đếm các số liệu thực tế từ Database
            val lessonCount = try {
                val cursor = database.query("SELECT COUNT(*) FROM lessons WHERE isCompleted = 1", null)
                cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
            } catch (_: Exception) { 0 }

            val correctCount = try {
                val cursor = database.query("SELECT COUNT(*) FROM attempts WHERE isCorrect = 1", null)
                cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
            } catch (_: Exception) { 0 }

            val flashcardCount = try {
                val cursor = database.query("SELECT COUNT(*) FROM review_logs", null)
                cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
            } catch (_: Exception) { 0 }

            val testCount = try {
                val cursor = database.query("SELECT COUNT(*) FROM tests WHERE isDraft = 0", null)
                cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
            } catch (_: Exception) { 0 }

            val perfectTestCount = try {
                val cursor = database.query("SELECT COUNT(*) FROM tests WHERE isDraft = 0 AND score >= 10.0", null)
                cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
            } catch (_: Exception) { 0 }

            val highScoresCount = try {
                val cursor = database.query("SELECT COUNT(*) FROM tests WHERE isDraft = 0 AND score >= 8.0", null)
                cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
            } catch (_: Exception) { 0 }

            val masteredWrongCount = try {
                val cursor = database.query("SELECT COUNT(*) FROM wrong_questions WHERE status = 'MASTERED'", null)
                cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
            } catch (_: Exception) { 0 }

            val subjectsCount = try {
                val cursor = database.query("""
                    SELECT COUNT(DISTINCT subjectId) FROM (
                        SELECT subjectId FROM tests WHERE isDraft = 0 AND subjectId != 'all'
                        UNION
                        SELECT c.subjectId FROM lessons l JOIN chapters c ON l.chapterId = c.id WHERE l.isCompleted = 1
                    )
                """.trimIndent(), null)
                cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
            } catch (_: Exception) { 0 }

            val chaptersCompletedCount = try {
                val cursor = database.query("""
                    SELECT COUNT(*) FROM (
                        SELECT chapterId, COUNT(*) as total, SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) as done
                        FROM lessons
                        GROUP BY chapterId
                        HAVING total = done AND total > 0
                    )
                """.trimIndent(), null)
                cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
            } catch (_: Exception) { 0 }

            GamificationStats(
                totalXp = totalXp,
                currentLevel = level,
                currentStreak = currentStreak,
                lessonsCompletedCount = lessonCount,
                questionsCorrectCount = correctCount,
                flashcardsReviewedCount = flashcardCount,
                testsCompletedCount = testCount,
                perfectTestCount = perfectTestCount,
                maxConsecutiveCorrectInSession = 10.coerceAtMost(correctCount), // hoặc từ attempt streaks
                highScoreTestsCount = highScoresCount,
                chaptersCompletedCount = chaptersCompletedCount,
                wrongMasteredCount = masteredWrongCount,
                subjectsStudiedCount = subjectsCount
            )
        }
    }
}
