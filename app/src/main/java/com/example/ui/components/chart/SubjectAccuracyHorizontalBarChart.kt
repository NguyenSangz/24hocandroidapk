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
import com.example.domain.evaluator.SubjectAccuracyItem
import com.example.ui.theme.SubjectColors

/**
 * Biểu đồ thanh ngang tỉ lệ đúng theo từng môn học (Canvas tự vẽ trong Compose).
 * - Mỗi môn hiển thị đúng màu đặc trưng theo chuẩn GDPT 2018.
 * - Hiển thị nhãn môn học, tỉ lệ % đúng và số câu đã làm khi chạm.
 */
@Composable
fun SubjectAccuracyHorizontalBarChart(
    items: List<SubjectAccuracyItem>,
    modifier: Modifier = Modifier,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(items) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 650))
    }

    val chartHeight = remember(items) {
        (items.size * 44).coerceAtLeast(120).dp
    }

    val talkBackDesc = remember(items) {
        val summary = items.joinToString("; ") { "${it.subjectName}: ${it.accuracyPercent.toInt()}% đúng" }
        "Biểu đồ tỉ lệ đúng theo môn: $summary"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
            .semantics { contentDescription = talkBackDesc }
            .testTag("subject_accuracy_horizontal_bar_chart")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .pointerInput(items) {
                    detectTapGestures { offset ->
                        if (items.isEmpty()) return@detectTapGestures
                        val rowHeight = size.height / items.size
                        val idx = (offset.y / rowHeight).toInt().coerceIn(0, items.size - 1)
                        selectedIndex = if (selectedIndex == idx) null else idx
                    }
                }
        ) {
            val count = items.size
            if (count == 0) return@Canvas

            val rowHeight = size.height / count
            val barHeight = (rowHeight * 0.45f).coerceIn(10.dp.toPx(), 20.dp.toPx())
            val labelWidth = 80.dp.toPx()
            val percentLabelWidth = 46.dp.toPx()
            val maxBarWidth = size.width - labelWidth - percentLabelWidth

            items.forEachIndexed { index, item ->
                val topY = index * rowHeight
                val centerY = topY + (rowHeight / 2f)
                val barTop = centerY - (barHeight / 2f)
                val barLeft = labelWidth
                val subjectColor = SubjectColors.getColorForSubject(item.subjectId)

                // 1. Tên môn học bên trái
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        this.color = labelColor.hashCode()
                        this.textSize = 12.sp.toPx()
                        this.textAlign = android.graphics.Paint.Align.LEFT
                        this.isAntiAlias = true
                        this.isFakeBoldText = (selectedIndex == index)
                    }
                    val textY = centerY + (paint.textSize / 3f)
                    val truncatedName = if (item.subjectName.length > 8) item.subjectName.take(7) + "…" else item.subjectName
                    canvas.nativeCanvas.drawText(truncatedName, 0f, textY, paint)
                }

                // 2. Nền thanh xám (100%)
                drawRoundRect(
                    color = backgroundColor.copy(alpha = 0.5f),
                    topLeft = Offset(barLeft, barTop),
                    size = Size(maxBarWidth, barHeight),
                    cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
                )

                // 3. Thanh phần trăm đúng theo màu môn
                val barProgressWidth = (maxBarWidth * (item.accuracyPercent / 100f) * animationProgress.value).coerceAtLeast(barHeight)
                drawRoundRect(
                    color = subjectColor,
                    topLeft = Offset(barLeft, barTop),
                    size = Size(barProgressWidth, barHeight),
                    cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
                )

                // 4. Nhãn tỉ lệ % bên phải
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        this.color = labelColor.hashCode()
                        this.textSize = 12.sp.toPx()
                        this.textAlign = android.graphics.Paint.Align.RIGHT
                        this.isAntiAlias = true
                        this.isFakeBoldText = true
                    }
                    val textY = centerY + (paint.textSize / 3f)
                    val pctText = "${item.accuracyPercent.toInt()}%"
                    canvas.nativeCanvas.drawText(pctText, size.width, textY, paint)
                }
            }
        }

        // Tooltip chi tiết khi chạm
        selectedIndex?.let { idx ->
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
                        text = "${it.subjectName}: đúng ${it.totalCorrect}/${it.totalAnswered} câu (${it.accuracyPercent.toInt()}%)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
