package com.example.ui.components.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.evaluator.SubjectComparisonItem

/**
 * Biểu đồ so sánh đa chiều giữa các môn học (Grouped Bar Chart - Cột nhóm).
 * So sánh 3 tiêu chí:
 * 1. Tỉ lệ đúng (Accuracy) - Màu Xanh dương
 * 2. Tiến độ bài học (Lesson Progress) - Màu Xanh ngọc
 * 3. Độ ổn định học tập (Consistency) - Màu Hổ phách
 */
@Composable
fun SubjectGroupedComparisonChart(
    items: List<SubjectComparisonItem>,
    modifier: Modifier = Modifier,
    accuracyColor: Color = Color(0xFF4A7FA8),
    progressColor: Color = Color(0xFF3F9E6E),
    consistencyColor: Color = Color(0xFFC08A52),
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    var selectedSubjectIndex by remember { mutableStateOf<Int?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(items) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 650))
    }

    val talkBackDesc = remember(items) {
        "Biểu đồ so sánh 3 tiêu chuẩn: Tỉ lệ đúng, tiến độ bài học và độ ổn định của ${items.size} môn."
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .semantics { contentDescription = talkBackDesc }
            .testTag("subject_grouped_comparison_chart")
    ) {
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Chưa có đủ dữ liệu để so sánh môn học",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 20.dp)
                    .pointerInput(items) {
                        detectTapGestures { offset ->
                            val count = items.size
                            if (count == 0) return@detectTapGestures
                            val groupWidth = size.width / count
                            val idx = (offset.x / groupWidth).toInt().coerceIn(0, count - 1)
                            selectedSubjectIndex = if (selectedSubjectIndex == idx) null else idx
                        }
                    }
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                val count = items.size
                val groupWidth = chartWidth / count

                // Lưới ngang 0%, 50%, 100%
                val gridLevels = listOf(0f, 0.5f, 1f)
                gridLevels.forEach { ratio ->
                    val y = chartHeight - (chartHeight * ratio)
                    drawLine(
                        color = labelColor.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val singleBarWidth = (groupWidth * 0.22f).coerceIn(6.dp.toPx(), 14.dp.toPx())
                val barGap = 3.dp.toPx()

                items.forEachIndexed { index, item ->
                    val groupCenterX = (index * groupWidth) + (groupWidth / 2f)
                    val isSelected = selectedSubjectIndex == index

                    val totalGroupBarWidth = (singleBarWidth * 3) + (barGap * 2)
                    val startX = groupCenterX - (totalGroupBarWidth / 2f)

                    // 1. Cột Tỉ lệ đúng
                    val accHeight = (chartHeight * (item.accuracyScore / 100f) * animationProgress.value).coerceAtLeast(2.dp.toPx())
                    drawRoundRect(
                        color = accuracyColor,
                        topLeft = Offset(startX, chartHeight - accHeight),
                        size = Size(singleBarWidth, accHeight),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )

                    // 2. Cột Tiến độ bài học
                    val progHeight = (chartHeight * (item.lessonProgressScore / 100f) * animationProgress.value).coerceAtLeast(2.dp.toPx())
                    drawRoundRect(
                        color = progressColor,
                        topLeft = Offset(startX + singleBarWidth + barGap, chartHeight - progHeight),
                        size = Size(singleBarWidth, progHeight),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )

                    // 3. Cột Độ ổn định
                    val consHeight = (chartHeight * (item.consistencyScore / 100f) * animationProgress.value).coerceAtLeast(2.dp.toPx())
                    drawRoundRect(
                        color = consistencyColor,
                        topLeft = Offset(startX + (singleBarWidth + barGap) * 2, chartHeight - consHeight),
                        size = Size(singleBarWidth, consHeight),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )

                    // Nhãn tên môn học dưới trục X
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            this.color = labelColor.hashCode()
                            this.textSize = 10.sp.toPx()
                            this.textAlign = android.graphics.Paint.Align.CENTER
                            this.isAntiAlias = true
                            if (isSelected) this.isFakeBoldText = true
                        }
                        val shortName = if (item.subjectName.length > 5) item.subjectName.take(4) + "…" else item.subjectName
                        canvas.nativeCanvas.drawText(shortName, groupCenterX, chartHeight + 18.dp.toPx(), paint)
                    }
                }
            }

            // Tooltip khi chọn môn
            selectedSubjectIndex?.let { idx ->
                if (idx in items.indices) {
                    val it = items[idx]
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 2.dp)
                    ) {
                        Text(
                            text = "${it.subjectName}: Đúng ${it.accuracyScore.toInt()}% • Tiến độ ${it.lessonProgressScore.toInt()}% • Ổn định ${it.consistencyScore.toInt()}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
