package com.example.ui.components.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SubjectColors

data class SubjectLessonProgressItem(
    val subjectId: String,
    val subjectName: String,
    val completedCount: Int,
    val totalCount: Int,
    val progressPercent: Float
)

/**
 * Danh sách thanh phần trăm tiến độ hoàn thành bài học theo từng môn (F11).
 */
@Composable
fun SubjectLessonProgressList(
    items: List<SubjectLessonProgressItem>,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(items) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 650))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("subject_lesson_progress_list")
    ) {
        items.forEach { item ->
            val subjectColor = SubjectColors.getColorForSubject(item.subjectId)
            val talkBackText = "${item.subjectName}: hoàn thành ${item.completedCount} trên ${item.totalCount} bài (${item.progressPercent.toInt()}%)"

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .semantics { contentDescription = talkBackText }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.subjectName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${item.completedCount}/${item.totalCount} bài (${item.progressPercent.toInt()}%)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                ) {
                    val fullWidth = size.width
                    val barHeight = size.height
                    val radius = barHeight / 2f

                    // Thanh nền
                    drawRoundRect(
                        color = backgroundColor.copy(alpha = 0.5f),
                        topLeft = Offset.Zero,
                        size = Size(fullWidth, barHeight),
                        cornerRadius = CornerRadius(radius, radius)
                    )

                    // Thanh hoàn thành
                    val filledWidth = (fullWidth * (item.progressPercent / 100f) * animationProgress.value).coerceAtLeast(0f)
                    if (filledWidth > 0f) {
                        drawRoundRect(
                            color = subjectColor,
                            topLeft = Offset.Zero,
                            size = Size(filledWidth, barHeight),
                            cornerRadius = CornerRadius(radius, radius)
                        )
                    }
                }
            }
        }
    }
}
