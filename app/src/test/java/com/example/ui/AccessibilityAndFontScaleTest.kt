package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.domain.evaluator.TestScorePoint
import com.example.ui.components.chart.TestScoreLineChart
import com.example.ui.theme.HocTotTHCSTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI & Accessibility Test (F17a):
 * - Cỡ chữ lớn 200% (fontScale = 2.0f theo tiêu chuẩn Google Play / Android Accessibility)
 * - Vùng chạm tối thiểu ≥ 48dp cho các thành phần bấm (touch target size)
 * - TalkBack semantics & contentDescription cho các biểu đồ và icon
 * - Chế độ tối (Dark Theme) tương thích
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AccessibilityAndFontScaleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testFontScale200Percent_rendersCorrectlyWithoutCrash() {
        // Mô phỏng người dùng bật cỡ chữ tối đa trên thiết bị (200%)
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 2.0f, fontScale = 2.0f)
            ) {
                HocTotTHCSTheme(darkTheme = false) {
                    Box(modifier = Modifier.testTag("large_font_container")) {
                        Text(text = "Bài 1: Khái niệm về số hữu tỉ và số vô tỉ trong chương trình THCS.")
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Bài 1: Khái niệm về số hữu tỉ và số vô tỉ trong chương trình THCS.")
            .assertIsDisplayed()
    }

    @Test
    fun testTouchTargetSize_atLeast48dp() {
        // Đảm bảo nút bấm đạt kích thước tối thiểu 48x48dp theo tiêu chuẩn Accessibility
        composeTestRule.setContent {
            HocTotTHCSTheme {
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .testTag("accessible_back_button")
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại màn hình trước"
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("accessible_back_button")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        // Kiểm tra TalkBack contentDescription
        composeTestRule.onNodeWithContentDescription("Quay lại màn hình trước")
            .assertIsDisplayed()
    }

    @Test
    fun testChartTalkBackSemantics() {
        val samplePoints = listOf(
            TestScorePoint("b1", "Toán 15p", "toan", 8.5f, 1000L, "10/10"),
            TestScorePoint("b2", "Toán 15p", "toan", 9.5f, 2000L, "11/10")
        )

        composeTestRule.setContent {
            HocTotTHCSTheme {
                TestScoreLineChart(
                    points = samplePoints,
                    modifier = Modifier.testTag("test_score_chart")
                )
            }
        }

        // Kiểm tra biểu đồ có mô tả TalkBack ngữ nghĩa (semantics) đầy đủ cho học sinh khiếm thị
        val expectedTalkBackText = "Biểu đồ đường gồm 2 bài kiểm tra, điểm trung bình là 9.0 trên 10."
        composeTestRule.onNodeWithContentDescription(expectedTalkBackText)
            .assertIsDisplayed()
    }

    @Test
    fun testDarkTheme_rendersCorrectly() {
        composeTestRule.setContent {
            HocTotTHCSTheme(darkTheme = true) {
                Box(modifier = Modifier.testTag("dark_mode_box")) {
                    Text(text = "Giao diện chế độ tối bảo vệ mắt học sinh.")
                }
            }
        }

        composeTestRule.onNodeWithText("Giao diện chế độ tối bảo vệ mắt học sinh.")
            .assertIsDisplayed()
    }
}
