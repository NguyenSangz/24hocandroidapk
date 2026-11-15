package com.example.ui.screens.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.theme.HocTotTHCSTheme
import kotlinx.coroutines.delay

/**
 * Màn hình Splash:
 * - Hiển thị logo, tên app, chương trình GDPT 2018
 * - Tự động gọi nạp dữ liệu ContentLoader trên luồng nền
 * - Hiển thị trạng thái Loading và xử lý lỗi nạp dữ liệu kèm nút "Thử lại"
 * - Chuyển sang Chọn lớp (nếu chưa chọn) hoặc Trang chủ (nếu đã chọn)
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToNext: (isGradeSelected: Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Khi nạp thành công, đợi 800ms để trải nghiệm mượt mà rồi chuyển trang
    LaunchedEffect(uiState) {
        if (uiState is SplashUiState.Success) {
            val state = uiState as SplashUiState.Success
            delay(800)
            onNavigateToNext(state.isGradeSelected)
        }
    }

    SplashContent(
        uiState = uiState,
        onRetry = { viewModel.retryLoading() },
        onStartClick = {
            if (uiState is SplashUiState.Success) {
                onNavigateToNext((uiState as SplashUiState.Success).isGradeSelected)
            }
        }
    )
}

@Composable
fun SplashContent(
    uiState: SplashUiState,
    onRetry: () -> Unit,
    onStartClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4A7FA8),
                        Color(0xFF35608C),
                        Color(0xFF1E2833)
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // App Logo
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(116.dp)
                    .padding(4.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_study_1791619286188),
                        contentDescription = "Học Tốt THCS Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(104.dp)
                            .clip(RoundedCornerShape(24.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Học Tốt THCS",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 32.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("splash_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFDFA34F)
            ) {
                Text(
                    text = "Chương trình GDPT 2018 (Lớp 6 - 9)",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78350F)
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tự tin nắm vững kiến thức trọng tâm\nLuyện tập thông minh • Hoàn toàn offline",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Khu vực hiển thị trạng thái nạp dữ liệu (Loading / Success / Error)
            when (uiState) {
                is SplashUiState.Loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.testTag("splash_loading_view")
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFDFA34F),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Đang chuẩn bị bài học & câu hỏi...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                is SplashUiState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF7F1D1D).copy(alpha = 0.6f))
                            .padding(16.dp)
                            .testTag("splash_error_view")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lỗi nạp dữ liệu",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCA5A5)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.9f)
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFFB91C1C)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("splash_retry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Thử lại",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                is SplashUiState.Success -> {
                    Button(
                        onClick = onStartClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF2C5578)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("splash_start_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Bắt đầu học ngay",
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
}

@Preview(showBackground = true, name = "Splash Loading")
@Composable
private fun SplashScreenLoadingPreview() {
    HocTotTHCSTheme {
        SplashContent(
            uiState = SplashUiState.Loading,
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, name = "Splash Error")
@Composable
private fun SplashScreenErrorPreview() {
    HocTotTHCSTheme {
        SplashContent(
            uiState = SplashUiState.Error("Không thể đọc tệp dữ liệu chương trình học."),
            onRetry = {}
        )
    }
}

@Preview(showBackground = true, name = "Splash Success")
@Composable
private fun SplashScreenSuccessPreview() {
    HocTotTHCSTheme {
        SplashContent(
            uiState = SplashUiState.Success(isGradeSelected = false, selectedGrade = 6),
            onRetry = {}
        )
    }
}
