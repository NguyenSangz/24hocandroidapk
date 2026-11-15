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
import com.example.domain.evaluator.DayStudyBarData
import com.example.ui.theme.LocalReduceMotion
import com.example.ui.theme.SubjectColors

/**
 * Biểu đồ cột thời gian học theo từng ngày (Canvas tự vẽ trong Compose).
 * - Mỗi cột xếp chồng hoặc đổi màu theo từng môn.
 * - Chạm vào hiện tooltip thời gian và chi tiết.
 * - Có hỗ trợ TalkBack (contentDescription) đầy đủ.
 */
@Composable
fun DailyStudyBarChart(
    data: List<DayStudyBarData>,
    modifier: Modifier = Modifier,
    primaryBarColor: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animationProgress = remember { Animatable(0f) }
    val reduceMotion = LocalReduceMotion.current

    LaunchedEffect(data, reduceMotion) {
        if (reduceMotion) {
            animationProgress.snapTo(1f)
        } else {
            animationProgress.snapTo(0f)
            animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 650))
        }
    }

    val maxSeconds = remember(data) {
        val maxVal = data.maxOfOrNull { it.totalSeconds } ?: 0L
        if (maxVal > 0) maxVal.toFloat() else 3600f // Mặc định 1 giờ nếu chưa có số liệu
    }

    val chartTalkBackDescription = remember(data) {
        val totalSec = data.sumOf { it.totalSeconds }
        val hours = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        "Biểu đồ cột thời gian học: tổng cộng $hours giờ $mins phút trong ${data.size} ngày."
    }

    val highlightColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .semantics { contentDescription = chartTalkBackDescription }
            .testTag("daily_study_bar_chart")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 28.dp, start = 12.dp, end = 12.dp, top = 20.dp)
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        if (data.isEmpty()) return@detectTapGestures
                        val stepX = size.width / data.size
                        val index = (offset.x / stepX).toInt().coerceIn(0, data.size - 1)
                        selectedIndex = if (selectedIndex == index) null else index
                    }
                }
        ) {
            val chartWidth = size.width
            val chartHeight = size.height
            val count = data.size
            if (count == 0) return@Canvas

            val barSpacing = (chartWidth / count)
            val barWidth = (barSpacing * 0.6f).coerceIn(8.dp.toPx(), 28.dp.toPx())

            // Vẽ các đường lưới ngang nhẹ nhàng
            val gridLines = 3
            for (i in 0..gridLines) {
                val y = chartHeight * (i.toFloat() / gridLines)
                drawLine(
                    color = labelColor.copy(alpha = 0.12f),
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            data.forEachIndexed { index, item ->
                val centerX = index * barSpacing + (barSpacing / 2f)
                val left = centerX - (barWidth / 2f)

                val ratio = (item.totalSeconds.toFloat() / maxSeconds).coerceIn(0f, 1f)
                val targetHeight = (chartHeight * ratio * animationProgress.value).coerceAtLeast(3.dp.toPx())
                val top = chartHeight - targetHeight

                val isSelected = selectedIndex == index

                // Nếu có phân bổ môn học thì vẽ các đoạn xếp chồng màu môn
                if (item.subjectSegments.isNotEmpty() && item.totalSeconds > 0) {
                    var currentY = chartHeight
                    item.subjectSegments.forEach { (subId, sec) ->
                        val segRatio = (sec.toFloat() / item.totalSeconds.toFloat())
                        val segHeight = targetHeight * segRatio
                        val segTop = currentY - segHeight
                        val color = SubjectColors.getColorForSubject(subId)

                        drawRoundRect(
                            color = if (isSelected) color.copy(alpha = 1f) else color.copy(alpha = 0.85f),
                            topLeft = Offset(left, segTop),
                            size = Size(barWidth, segHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                        currentY = segTop
                    }
                } else {
                    // Cột cơ bản khi chưa có phân khúc
                    drawRoundRect(
                        color = if (item.totalSeconds == 0L) labelColor.copy(alpha = 0.12f) else primaryBarColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, targetHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }

                // Vẽ viền highlight nếu đang được chọn
                if (isSelected) {
                    drawRoundRect(
                        color = highlightColor,
                        topLeft = Offset(left - 2.dp.toPx(), top - 2.dp.toPx()),
                        size = Size(barWidth + 4.dp.toPx(), targetHeight + 4.dp.toPx()),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }

                // Nhãn ngày (T2, T3...) bên dưới trục X
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        this.color = labelColor.hashCode()
                        this.textSize = 10.sp.toPx()
                        this.textAlign = android.graphics.Paint.Align.CENTER
                        this.isAntiAlias = true
                        if (isSelected) this.isFakeBoldText = true
                    }
                    canvas.nativeCanvas.drawText(
                        item.dayLabel,
                        centerX,
                        chartHeight + 20.dp.toPx(),
                        paint
                    )
                }
            }
        }

        // Tooltip hiển thị thời gian khi người dùng chạm
        selectedIndex?.let { idx ->
            if (idx in data.indices) {
                val selectedItem = data[idx]
                val mins = selectedItem.totalSeconds / 60
                val hours = mins / 60
                val remainingMins = mins % 60
                val timeStr = if (hours > 0) "${hours}h ${remainingMins}p" else "${mins} phút"

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                ) {
                    Text(
                        text = "${selectedItem.dateString}: $timeStr",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
