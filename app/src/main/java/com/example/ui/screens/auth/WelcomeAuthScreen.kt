package com.example.ui.screens.auth

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.repository.AuthAccountState
import com.example.ui.screens.account.AccountUiState
import com.example.ui.screens.account.AccountViewModel
import com.example.ui.theme.HocTotTHCSTheme
import com.example.util.GoogleCredentialResult
import com.example.util.GoogleSignInHelper
import kotlinx.coroutines.launch

/**
 * Màn hình Chào mừng & Đăng nhập (WelcomeAuthScreen) — Phần 1:
 * Đặt trước màn hình Chọn lớp.
 * Luồng mới: Chào mừng -> Đăng nhập Google (Credential Manager) -> Tuổi/Phụ huynh nếu < 16 -> Chọn lớp -> Trang chủ.
 * Hỗ trợ phương án dự phòng Email + Mật khẩu cho học sinh chưa có tài khoản Google.
 * Hỗ trợ Chế độ Khách (Offline) với thông báo rõ ràng về quyền lợi và tính năng.
 */
@Composable
fun WelcomeAuthScreen(
    viewModel: AccountViewModel,
    onAuthSuccess: (needsParentalConsent: Boolean) -> Unit,
    onContinueAsGuest: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountState by viewModel.accountState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(accountState.isAuthenticated) {
        if (accountState.isAuthenticated) {
            onAuthSuccess(accountState.needsParentalConsent)
        }
    }

    WelcomeAuthContent(
        uiState = uiState,
        accountState = accountState,
        snackbarHostState = snackbarHostState,
        onGoogleSignInClick = {
            val activity = context as? Activity
            if (activity != null) {
                viewModel.beginLoading()
                scope.launch {
                    when (val result = GoogleSignInHelper.requestInteractiveGoogleSignIn(activity)) {
                        is GoogleCredentialResult.Success -> {
                            viewModel.onGoogleIdTokenReceived(result.idToken, result.displayName)
                        }
                        is GoogleCredentialResult.Cancelled -> {
                            viewModel.cancelLoadingWithMessage(null)
                        }
                        is GoogleCredentialResult.Error -> {
                            viewModel.cancelLoadingWithMessage(result.message)
                        }
                    }
                }
            } else {
                viewModel.cancelLoadingWithMessage("Không thể kết nối dịch vụ Google Sign-In.")
            }
        },
        onToggleRegisterMode = { isRegister -> viewModel.toggleRegisterMode(isRegister) },
        onEmailChanged = { viewModel.updateEmailInput(it) },
        onPasswordChanged = { viewModel.updatePasswordInput(it) },
        onNicknameChanged = { viewModel.updateRegisterNicknameInput(it) },
        onBirthYearChanged = { viewModel.updateBirthYearInput(it) },
        onSubmitEmailAuth = { viewModel.submitEmailAuth() },
        onForgotPasswordClick = { viewModel.openForgotPasswordDialog() },
        onDismissForgotPassword = { viewModel.dismissForgotPasswordDialog() },
        onResetEmailChanged = { viewModel.updateResetEmailInput(it) },
        onSubmitPasswordReset = { viewModel.submitPasswordReset() },
        onContinueAsGuest = onContinueAsGuest,
        onNavigateToPrivacy = onNavigateToPrivacy
    )
}

@Composable
fun WelcomeAuthContent(
    uiState: AccountUiState,
    accountState: AuthAccountState,
    snackbarHostState: SnackbarHostState,
    onGoogleSignInClick: () -> Unit,
    onToggleRegisterMode: (Boolean) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onNicknameChanged: (String) -> Unit,
    onBirthYearChanged: (String) -> Unit,
    onSubmitEmailAuth: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onDismissForgotPassword: () -> Unit,
    onResetEmailChanged: (String) -> Unit,
    onSubmitPasswordReset: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    var showEmailForm by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .testTag("welcome_auth_screen_list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Header & Hero giới thiệu ứng dụng
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(76.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "Học Tốt THCS Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Học Tốt THCS",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Chương trình GDPT 2018 (Lớp 6 – 9)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Đăng nhập để đồng bộ tiến độ học tập trên mọi thiết bị và mở khóa trợ lý gia sư AI.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // 2. Banner Offline / Lỗi nếu có
                if (uiState.isOfflineBannerVisible || uiState.errorMessage != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.errorMessage != null)
                                    MaterialTheme.colorScheme.errorContainer
                                else
                                    MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (uiState.errorMessage != null)
                                        Icons.Default.Security
                                    else
                                        Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = if (uiState.errorMessage != null)
                                        MaterialTheme.colorScheme.onErrorContainer
                                    else
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = uiState.errorMessage
                                        ?: "Đang ở chế độ ngoại tuyến. Bạn vẫn có thể tiếp tục với chế độ Khách.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.errorMessage != null)
                                        MaterialTheme.colorScheme.onErrorContainer
                                    else
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                // 3. Card Đăng nhập Google (Ưu tiên số 1)
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Bắt đầu học tập",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Phương thức nhanh chóng và an toàn nhất cho học sinh",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Nút Google Sign-In
                            Button(
                                onClick = onGoogleSignInClick,
                                enabled = !uiState.isLoading,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("welcome_google_sign_in_button")
                            ) {
                                if (uiState.isLoading && !showEmailForm) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = "Google Icon",
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Đăng nhập bằng Google",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Lợi ích khi đăng nhập
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Đồng bộ đám mây", style = MaterialTheme.typography.labelSmall)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Gia sư AI lớp học", style = MaterialTheme.typography.labelSmall)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Bảo vệ dữ liệu", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                // 4. Khối tùy chọn Email (Phương án thay thế cho học sinh chưa có Google ID)
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Học sinh chưa có tài khoản Google?",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Đăng nhập hoặc đăng ký bằng Email và Mật khẩu",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(
                                    onClick = { showEmailForm = !showEmailForm },
                                    modifier = Modifier.testTag("toggle_email_form_button")
                                ) {
                                    Text(if (showEmailForm) "Thu gọn" else "Dùng Email")
                                }
                            }

                            AnimatedVisibility(
                                visible = showEmailForm,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    TabRow(
                                        selectedTabIndex = if (uiState.isRegisterMode) 1 else 0,
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ) {
                                        Tab(
                                            selected = !uiState.isRegisterMode,
                                            onClick = { onToggleRegisterMode(false) },
                                            text = { Text("Đăng nhập") }
                                        )
                                        Tab(
                                            selected = uiState.isRegisterMode,
                                            onClick = { onToggleRegisterMode(true) },
                                            text = { Text("Tạo tài khoản") }
                                        )
                                    }

                                    if (uiState.isRegisterMode) {
                                        OutlinedTextField(
                                            value = uiState.registerNicknameInput,
                                            onValueChange = onNicknameChanged,
                                            label = { Text("Biệt danh / Tên học sinh") },
                                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("welcome_register_nickname_input")
                                        )

                                        OutlinedTextField(
                                            value = uiState.birthYearInput,
                                            onValueChange = onBirthYearChanged,
                                            label = { Text("Năm sinh (ví dụ: 2012)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("welcome_birth_year_input")
                                        )
                                    }

                                    OutlinedTextField(
                                        value = uiState.emailInput,
                                        onValueChange = onEmailChanged,
                                        label = { Text("Địa chỉ Email") },
                                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("welcome_email_input")
                                    )

                                    OutlinedTextField(
                                        value = uiState.passwordInput,
                                        onValueChange = onPasswordChanged,
                                        label = { Text("Mật khẩu (tối thiểu 8 ký tự)") },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                        trailingIcon = {
                                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                Icon(
                                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = "Hiện/Ẩn mật khẩu"
                                                )
                                            }
                                        },
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("welcome_password_input")
                                    )

                                    if (!uiState.isRegisterMode) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = onForgotPasswordClick,
                                                modifier = Modifier.testTag("welcome_forgot_password_button")
                                            ) {
                                                Text("Quên mật khẩu?", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = onSubmitEmailAuth,
                                        enabled = !uiState.isLoading,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("welcome_submit_email_button")
                                    ) {
                                        if (uiState.isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        } else {
                                            Text(if (uiState.isRegisterMode) "Đăng ký tài khoản" else "Đăng nhập với Email")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Chế độ Khách (Offline-only)
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedButton(
                            onClick = onContinueAsGuest,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("welcome_continue_as_guest_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OfflineBolt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tiếp tục với tư cách Khách (Chế độ Ngoại tuyến)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "💡 Lưu ý: Chế độ Khách vẫn học đầy đủ các bài đã tải trên máy. Tính năng trực tuyến và Gia sư AI sẽ tạm thời được ẩn.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }

                // 6. Liên kết Pháp lý / Quyền riêng tư
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onNavigateToPrivacy,
                        modifier = Modifier.testTag("welcome_privacy_policy_button")
                    ) {
                        Text(
                            text = "Chính sách quyền riêng tư & Bảo vệ trẻ em",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews cho màn hình Chào mừng & Đăng nhập (Sáng, Tối, Đang tải, Lỗi)
// ---------------------------------------------------------------------------

@Preview(name = "Sáng - Mặc định", showBackground = true)
@Composable
fun WelcomeAuthScreenPreviewLight() {
    HocTotTHCSTheme(darkTheme = false) {
        WelcomeAuthContent(
            uiState = AccountUiState(),
            accountState = AuthAccountState(),
            snackbarHostState = remember { SnackbarHostState() },
            onGoogleSignInClick = {},
            onToggleRegisterMode = {},
            onEmailChanged = {},
            onPasswordChanged = {},
            onNicknameChanged = {},
            onBirthYearChanged = {},
            onSubmitEmailAuth = {},
            onForgotPasswordClick = {},
            onDismissForgotPassword = {},
            onResetEmailChanged = {},
            onSubmitPasswordReset = {},
            onContinueAsGuest = {},
            onNavigateToPrivacy = {}
        )
    }
}

@Preview(name = "Tối - Dark mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun WelcomeAuthScreenPreviewDark() {
    HocTotTHCSTheme(darkTheme = true) {
        WelcomeAuthContent(
            uiState = AccountUiState(),
            accountState = AuthAccountState(),
            snackbarHostState = remember { SnackbarHostState() },
            onGoogleSignInClick = {},
            onToggleRegisterMode = {},
            onEmailChanged = {},
            onPasswordChanged = {},
            onNicknameChanged = {},
            onBirthYearChanged = {},
            onSubmitEmailAuth = {},
            onForgotPasswordClick = {},
            onDismissForgotPassword = {},
            onResetEmailChanged = {},
            onSubmitPasswordReset = {},
            onContinueAsGuest = {},
            onNavigateToPrivacy = {}
        )
    }
}

@Preview(name = "Đang tải - Loading", showBackground = true)
@Composable
fun WelcomeAuthScreenPreviewLoading() {
    HocTotTHCSTheme {
        WelcomeAuthContent(
            uiState = AccountUiState(isLoading = true),
            accountState = AuthAccountState(),
            snackbarHostState = remember { SnackbarHostState() },
            onGoogleSignInClick = {},
            onToggleRegisterMode = {},
            onEmailChanged = {},
            onPasswordChanged = {},
            onNicknameChanged = {},
            onBirthYearChanged = {},
            onSubmitEmailAuth = {},
            onForgotPasswordClick = {},
            onDismissForgotPassword = {},
            onResetEmailChanged = {},
            onSubmitPasswordReset = {},
            onContinueAsGuest = {},
            onNavigateToPrivacy = {}
        )
    }
}

@Preview(name = "Lỗi / Offline", showBackground = true)
@Composable
fun WelcomeAuthScreenPreviewError() {
    HocTotTHCSTheme {
        WelcomeAuthContent(
            uiState = AccountUiState(
                isOfflineBannerVisible = true,
                errorMessage = "Không thể kết nối máy chủ. Vui lòng kiểm tra Wi-Fi/4G hoặc dùng chế độ Khách."
            ),
            accountState = AuthAccountState(),
            snackbarHostState = remember { SnackbarHostState() },
            onGoogleSignInClick = {},
            onToggleRegisterMode = {},
            onEmailChanged = {},
            onPasswordChanged = {},
            onNicknameChanged = {},
            onBirthYearChanged = {},
            onSubmitEmailAuth = {},
            onForgotPasswordClick = {},
            onDismissForgotPassword = {},
            onResetEmailChanged = {},
            onSubmitPasswordReset = {},
            onContinueAsGuest = {},
            onNavigateToPrivacy = {}
        )
    }
}
