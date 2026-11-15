package com.example.domain.gamification

import kotlin.math.pow

/**
 * Lớp logic thuần tính toán cấp độ và tiến trình thăng hạng (F10).
 * Không phụ thuộc Android framework hay DB, 100% testable.
 */
object LevelCalculator {

    /**
     * Tính số XP tích lũy tối thiểu cần có để đạt cấp độ [level] (1..MAX_LEVEL).
     * Cấp 1 cần 0 XP.
     * Cấp N (N >= 2) cần: floor(100 * (N - 1)^1.5) XP.
     */
    fun xpRequiredForLevel(level: Int): Int {
        if (level <= 1) return 0
        val target = level.coerceAtMost(GamificationConfig.MAX_LEVEL)
        return (GamificationConfig.LEVEL_BASE_MULTIPLIER * (target - 1.0).pow(GamificationConfig.LEVEL_EXPONENT)).toInt()
    }

    /**
     * Xác định thông tin cấp độ hoàn chỉnh từ tổng số XP tích lũy.
     */
    fun calculateLevel(totalXp: Int): LevelInfo {
        val safeXp = totalXp.coerceAtLeast(0)

        var currentLevel = 1
        for (lvl in 1..GamificationConfig.MAX_LEVEL) {
            val req = xpRequiredForLevel(lvl)
            if (safeXp >= req) {
                currentLevel = lvl
            } else {
                break
            }
        }

        val isMax = currentLevel >= GamificationConfig.MAX_LEVEL
        val currentBaseXp = xpRequiredForLevel(currentLevel)
        val nextTargetXp = if (isMax) currentBaseXp else xpRequiredForLevel(currentLevel + 1)

        val xpIntoCurrent = (safeXp - currentBaseXp).coerceAtLeast(0)
        val span = (nextTargetXp - currentBaseXp).coerceAtLeast(1)

        val progress = if (isMax) {
            1.0f
        } else {
            (xpIntoCurrent.toFloat() / span.toFloat()).coerceIn(0.0f, 1.0f)
        }

        val xpRemaining = if (isMax) 0 else (nextTargetXp - safeXp).coerceAtLeast(0)
        val group = getBadgeGroupForLevel(currentLevel)
        val title = getLevelTitle(currentLevel)

        return LevelInfo(
            level = currentLevel,
            title = title,
            totalXp = safeXp,
            currentLevelBaseXp = currentBaseXp,
            nextLevelTargetXp = nextTargetXp,
            xpIntoCurrentLevel = xpIntoCurrent,
            xpRequiredForNextLevel = xpRemaining,
            progress = progress,
            isMaxLevel = isMax,
            badgeGroup = group
        )
    }

    /**
     * Lấy nhóm danh hiệu và màu sắc theo khoảng cấp độ
     */
    fun getBadgeGroupForLevel(level: Int): LevelBadgeGroup {
        return when {
            level <= 5 -> LevelBadgeGroup.APPRENTICE
            level <= 10 -> LevelBadgeGroup.DILIGENT
            level <= 15 -> LevelBadgeGroup.ENERGETIC
            level <= 20 -> LevelBadgeGroup.PERSISTENT
            level <= 30 -> LevelBadgeGroup.WISE
            level <= 40 -> LevelBadgeGroup.SCHOLAR
            else -> LevelBadgeGroup.LEGEND
        }
    }

    /**
     * Tên danh xưng thân thiện với học sinh THCS (11-15 tuổi)
     */
    fun getLevelTitle(level: Int): String {
        return when (level) {
            1 -> "Tập sự khám phá"
            2 -> "Tân binh hiếu học"
            3 -> "Búp măng non"
            4 -> "Học viên siêng năng"
            5 -> "Tập sự ưu tú"

            6 -> "Chăm chỉ sơ cấp"
            7 -> "Chăm chỉ miệt mài"
            8 -> "Gương sáng học tập"
            9 -> "Kiến thức vững vàng"
            10 -> "Học sinh tiên tiến"

            11 -> "Chiến binh năng nổ"
            12 -> "Năng động tự tin"
            13 -> "Bứt phá giới hạn"
            14 -> "Thợ săn điểm tốt"
            15 -> "Chiến binh xuất sắc"

            16 -> "Kiên định bước đầu"
            17 -> "Vượt khó bền lòng"
            18 -> "Vững bước tiến lên"
            19 -> "Kỷ luật thép"
            20 -> "Bậc thầy kiên trì"

            in 21..25 -> "Nhà thông thái trẻ (Cấp $level)"
            in 26..30 -> "Thông thái bậc cao (Cấp $level)"
            in 31..35 -> "Học giả tinh anh (Cấp $level)"
            in 36..40 -> "Đại học sĩ THCS (Cấp $level)"
            in 41..49 -> "Cao thủ danh tiếng (Cấp $level)"
            50 -> "Huyền thoại THCS toàn năng"
            else -> "Học sinh THCS"
        }
    }
}
