package com.example

import com.example.fake.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var repository: FakeSettingsRepository

    @Before
    fun setup() {
        repository = FakeSettingsRepository()
    }

    @Test
    fun testDefaultSettings() = runTest {
        assertEquals(6, repository.selectedGrade.first())
        assertFalse(repository.isGradeSelected.first())
        assertTrue(repository.isFirstLaunch.first())
        assertFalse(repository.isDarkMode.first())
        assertEquals("Học sinh THCS", repository.userName.first())
    }

    @Test
    fun testSetGrade() = runTest {
        // Học sinh chọn lớp 8
        repository.setGrade(8)

        assertEquals(8, repository.selectedGrade.first())
        assertTrue(repository.isGradeSelected.first())
        assertFalse(repository.isFirstLaunch.first())

        // Đổi sang lớp 9
        repository.setGrade(9)
        assertEquals(9, repository.selectedGrade.first())
        assertTrue(repository.isGradeSelected.first())
    }

    @Test
    fun testResetGradeSelection() = runTest {
        repository.setGrade(7)
        assertTrue(repository.isGradeSelected.first())

        repository.resetGradeSelection()
        assertFalse(repository.isGradeSelected.first())
    }

    @Test
    fun testDarkModeAndUserName() = runTest {
        repository.setDarkMode(true)
        assertTrue(repository.isDarkMode.first())
        assertEquals("dark", repository.themeMode.first())

        repository.setUserName("Nguyễn Văn A")
        assertEquals("Nguyễn Văn A", repository.userName.first())
    }

    @Test
    fun testUnifiedSettingsReadAndWrite() = runTest {
        // Giao diện
        repository.setThemeMode("light")
        assertEquals("light", repository.themeMode.first())
        assertFalse(repository.isDarkMode.first())

        repository.setReduceMotion(true)
        assertTrue(repository.reduceMotion.first())

        repository.setLessonFontSize("large")
        assertEquals("large", repository.lessonFontSize.first())

        // Học tập & Bộ sách
        repository.setDefaultSeries("ctst")
        assertEquals("ctst", repository.defaultSeries.first())

        repository.setDefaultQuestionsPerSession(20)
        assertEquals(20, repository.defaultQuestionsPerSession.first())

        repository.setTtsSpeechRate(1.25f)
        assertEquals(1.25f, repository.ttsSpeechRate.first(), 0.01f)

        repository.setSoundEffects(false)
        assertFalse(repository.soundEffectsEnabled.first())

        repository.setVibration(false)
        assertFalse(repository.vibrationEnabled.first())

        // Nhắc học & Mục tiêu
        repository.setReminder(true, 20, 15)
        assertTrue(repository.reminderEnabled.first())
        assertEquals(20, repository.reminderHour.first())
        assertEquals(15, repository.reminderMinute.first())

        repository.setDailyGoals(25, 45)
        assertEquals(25, repository.dailyGoalQuestions.first())
        assertEquals(45, repository.dailyGoalMinutes.first())

        repository.setLevelUpDialogEnabled(false)
        assertFalse(repository.levelUpDialogEnabled.first())
    }

    @Test
    fun testResetSettingsToDefaultPreservesGradeAndUserName() = runTest {
        repository.setGrade(9)
        repository.setUserName("Lê Bảo Nam")
        repository.setThemeMode("dark")
        repository.setLessonFontSize("large")
        repository.setReduceMotion(true)
        repository.setDefaultQuestionsPerSession(25)

        // Đặt lại cài đặt về mặc định
        repository.resetSettingsToDefault()

        // Các cài đặt trở về mặc định
        assertEquals("system", repository.themeMode.first())
        assertFalse(repository.isDarkMode.first())
        assertEquals("medium", repository.lessonFontSize.first())
        assertFalse(repository.reduceMotion.first())
        assertEquals(10, repository.defaultQuestionsPerSession.first())
        assertEquals("kntt", repository.defaultSeries.first())

        // Nhưng khối lớp và tên người dùng KHÔNG bị mất
        assertEquals(9, repository.selectedGrade.first())
        assertEquals("Lê Bảo Nam", repository.userName.first())
    }

    @Test
    fun testClearAllStudyDataTrigger() = runTest {
        assertFalse(repository.wasClearAllStudyDataCalled)
        repository.clearAllStudyData()
        assertTrue(repository.wasClearAllStudyDataCalled)
    }
}
