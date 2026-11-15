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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.evaluator.TestScorePoint
import com.example.ui.theme.SubjectColors

/**
 * Biểu đồ đường điểm kiểm tra theo thời gian (Canvas tự vẽ trong Compose).
 * - Trục Y: 0..10 điểm (thang điểm chuẩn VN).
 * - Trục X: Các bài kiểm tra theo thứ tự thời gian.
 * - Chạm điểm tròn hiển thị tooltip điểm số và tên đề thi.
 * - TalkBack mô tả trực quan xu hướng điểm.
 */
@Composable
fun TestScoreLineChart(
    points: List<TestScorePoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(points) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 650))
    }

    val avgScore = remember(points) {
        if (points.isNotEmpty()) points.map { it.score }.average().toFloat() else 0f
    }

    val activePointColor = MaterialTheme.colorScheme.primary

    val talkBackDesc = remember(points, avgScore) {
        if (points.isEmpty()) "Chưa có bài kiểm tra nào được ghi nhận."
        else "Biểu đồ đường gồm ${points.size} bài kiểm tra, điểm trung bình là ${String.format(java.util.Locale.getDefault(), "%.1f", avgScore)} trên 10."
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics { contentDescription = talkBackDesc }
            .testTag("test_score_line_chart")
    ) {
        if (points.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Chưa có bài kiểm tra nào trong kỳ này",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            val count = points.size
                            if (count <= 1) {
                                selectedIndex = 0
                                return@detectTapGestures
                            }
                            val stepX = size.width / (count - 1)
                            val idx = ((offset.x + stepX / 2f) / stepX).toInt().coerceIn(0, count - 1)
                            selectedIndex = if (selectedIndex == idx) null else idx
                        }
                    }
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                val count = points.size

                // Vẽ các vạch ngang mức điểm chuẩn 2.5, 5.0, 7.5, 10.0
                val levels = listOf(0.0f, 2.5f, 5.0f, 7.5f, 10.0f)
                levels.forEach { level ->
                    val y = chartHeight - (level / 10f) * chartHeight
                    drawLine(
                        color = gridColor.copy(alpha = 0.35f),
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Điểm tọa độ
                val screenPoints = points.mapIndexed { index, p ->
                    val x = if (count == 1) chartWidth / 2f else (index.toFloat() / (count - 1)) * chartWidth
                    val scoreRatio = (p.score / 10f).coerceIn(0f, 1f)
                    val targetY = chartHeight - (scoreRatio * chartHeight)
                    val animatedY = chartHeight - ((chartHeight - targetY) * animationProgress.value)
                    Offset(x, animatedY)
                }

                // Vẽ đường gấp khúc mượt
                if (screenPoints.size > 1) {
                    val path = Path().apply {
                        moveTo(screenPoints.first().x, screenPoints.first().y)
                        for (i in 1 until screenPoints.size) {
                            lineTo(screenPoints[i].x, screenPoints[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Vẽ các nút tròn tại từng điểm kiểm tra
                screenPoints.forEachIndexed { index, offset ->
                    val p = points[index]
                    val isSelected = selectedIndex == index
                    val pointColor = SubjectColors.getColorForSubject(p.subjectId)

                    // Vòng ngoài
                    drawCircle(
                        color = if (isSelected) activePointColor else pointColor,
                        radius = if (isSelected) 7.dp.toPx() else 5.dp.toPx(),
                        center = offset
                    )
                    // Vòng trắng trong
                    drawCircle(
                        color = Color.White,
                        radius = if (isSelected) 3.5.dp.toPx() else 2.5.dp.toPx(),
                        center = offset
                    )
                }
            }

            // Tooltip khi chạm điểm thi
            selectedIndex?.let { idx ->
                if (idx in points.indices) {
                    val pt = points[idx]
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = "${pt.title}: ${pt.score}/10 điểm (${pt.formattedDate})",
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
