package com.example.ui.screens.grade

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Grade
import com.example.ui.theme.HocTotTHCSTheme

/**
 * Cấu hình màu sắc tươi sáng đặc trưng cho 4 khối lớp THCS
 */
private val Grade6Color = Color(0xFF4A7FA8) // Xanh dương
private val Grade7Color = Color(0xFF55897B) // Xanh ngọc lục bảo
private val Grade8Color = Color(0xFFC08A52) // Vàng cam ấm
private val Grade9Color = Color(0xFF7C3AED) // Tím nổi bật

private fun getGradeAccentColor(grade: Int): Color = when (grade) {
    6 -> Grade6Color
    7 -> Grade7Color
    8 -> Grade8Color
    9 -> Grade9Color
    else -> Grade6Color
}

/**
 * Màn hình Chọn khối lớp (6, 7, 8, 9):
 * - 4 thẻ lớn, màu tươi sáng, chữ lớn, có hoạt ảnh nhẹ khi chọn
 * - Lưu lựa chọn bằng DataStore
 * - Tích hợp SelectGradeViewModel
 */
@Composable
fun SelectGradeScreen(
    viewModel: SelectGradeViewModel,
    onGradeConfirmed: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SelectGradeContent(
        selectedGrade = uiState.selectedGrade,
        isSaving = uiState.isSaving,
        onSelectGrade = { viewModel.onGradeSelected(it) },
        onConfirm = {
            viewModel.confirmGradeSelection {
                onGradeConfirmed(uiState.selectedGrade)
            }
        }
    )
}

@Composable
fun SelectGradeContent(
    selectedGrade: Int,
    isSaving: Boolean,
    onSelectGrade: (Int) -> Unit,
    onConfirm: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Tiêu đề & Hướng dẫn
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Chọn Khối Lớp",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        modifier = Modifier.testTag("select_grade_title")
                    )
                    Text(
                        text = "Bạn có thể thay đổi lại lớp bất cứ lúc nào",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4 Thẻ lớn chọn khối lớp (Lớp 6, 7, 8, 9)
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Grade.entries.forEach { gradeItem ->
                    val isCurrent = gradeItem.value == selectedGrade
                    val accentColor = getGradeAccentColor(gradeItem.value)

                    // Hoạt ảnh scale nhẹ khi được chọn
                    val cardScale by animateFloatAsState(
                        targetValue = if (isCurrent) 1.02f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.7f),
                        label = "card_scale_anim"
                    )

                    // Hoạt ảnh viền màu
                    val borderColor by animateColorAsState(
                        targetValue = if (isCurrent) accentColor else Color.Transparent,
                        label = "card_border_anim"
                    )

                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) {
                                accentColor.copy(alpha = 0.12f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isCurrent) 4.dp else 1.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(cardScale)
                            .border(2.5.dp, borderColor, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onSelectGrade(gradeItem.value) }
                            .testTag("grade_card_${gradeItem.value}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            // Huy hiệu số lớp lớn nổi bật
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrent) accentColor
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${gradeItem.value}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 28.sp,
                                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = gradeItem.displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                        color = if (isCurrent) accentColor else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = gradeItem.subtitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Dấu tích xác nhận
                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(accentColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Đã chọn",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nút bấm xác nhận vào học
            Button(
                onClick = onConfirm,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = getGradeAccentColor(selectedGrade)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_grade_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Bắt đầu học Lớp $selectedGrade",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Select Grade Screen Light")
@Composable
private fun SelectGradeScreenPreview() {
    HocTotTHCSTheme {
        SelectGradeContent(
            selectedGrade = 7,
            isSaving = false,
            onSelectGrade = {},
            onConfirm = {}
        )
    }
}
