package com.example.ui.screens.account

import android.app.Activity
import android.content.res.Configuration
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.domain.repository.AccountMode
import com.example.domain.repository.AuthAccountState
import com.example.domain.repository.SyncStatus
import com.example.domain.repository.SyncUiSummary
import com.example.ui.theme.HocTotTHCSTheme
import com.example.util.GoogleCredentialResult
import com.example.util.GoogleSignInHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountState by viewModel.accountState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val syncSummary by viewModel.syncSummary.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    AccountScreenContent(
        accountState = accountState,
        uiState = uiState,
        syncSummary = syncSummary,
        snackbarHostState = snackbarHostState,
        challengeFactorA = viewModel.challengeFactorA,
        challengeFactorB = viewModel.challengeFactorB,
        onNavigateBack = onNavigateBack,
        onNavigateToPrivacy = onNavigateToPrivacy,
        onToggleRegisterMode = viewModel::toggleRegisterMode,
        onEmailChanged = viewModel::updateEmailInput,
        onPasswordChanged = viewModel::updatePasswordInput,
        onRegisterNicknameChanged = viewModel::updateRegisterNicknameInput,
        onSubmitEmailAuth = viewModel::submitEmailAuth,
        onOpenForgotPassword = viewModel::openForgotPasswordDialog,
        onDismissForgotPassword = viewModel::dismissForgotPasswordDialog,
        onResetEmailChanged = viewModel::updateResetEmailInput,
        onSubmitPasswordReset = viewModel::submitPasswordReset,
        onBirthYearChanged = viewModel::updateBirthYearInput,
        onGoogleSignInClick = {
            if (!uiState.isLoading) {
                val activity = context as? Activity
                if (activity == null) {
                    viewModel.cancelLoadingWithMessage("Không tìm thấy cửa sổ Activity để đăng nhập Google.")
                } else {
                    viewModel.beginLoading()
                    scope.launch {
                        when (val cred = GoogleSignInHelper.requestInteractiveGoogleSignIn(activity)) {
                            is GoogleCredentialResult.Success -> {
                                viewModel.onGoogleIdTokenReceived(cred.idToken, cred.displayName)
                            }
                            GoogleCredentialResult.Cancelled -> {
                                viewModel.cancelLoadingWithMessage(null)
                            }
                            is GoogleCredentialResult.Error -> {
                                viewModel.cancelLoadingWithMessage(cred.message)
                            }
                        }
                    }
                }
            }
        },
        onContinueGuestClick = onNavigateBack,
        onNicknameChanged = viewModel::updateNicknameInput,
        onSaveNickname = viewModel::saveNickname,
        onOpenParentalConsent = viewModel::openParentalConsentDialog,
        onDismissParentalConsent = viewModel::dismissParentalConsentDialog,
        onParentEmailChanged = viewModel::updateParentEmailInput,
        onParentConsentChecked = viewModel::updateParentConsentChecked,
        onParentMathAnswerChanged = viewModel::updateParentMathAnswer,
        onSubmitParentalConsent = { viewModel.submitParentalConsent() },
        onCheckParentalConsentStatus = viewModel::checkParentalConsentStatus,
        onSignOutClick = {
            scope.launch {
                GoogleSignInHelper.clearCredentialStateSafely(context)
                viewModel.signOutToGuest()
            }
        },
        onStartDeleteAccount = viewModel::startDeleteAccountFlow,
        onDeleteLocalChoiceChanged = viewModel::setDeleteLocalDataChoice,
        onProceedDeleteStep2 = viewModel::proceedToFinalDeleteConfirmation,
        onCancelDeleteFlow = viewModel::cancelDeleteAccountFlow,
        onConfirmDeleteAccount = {
            scope.launch {
                GoogleSignInHelper.clearCredentialStateSafely(context)
                viewModel.confirmDeleteAccount()
            }
        },
        onSyncNow = viewModel::syncNow,
        onToggleWifiOnly = viewModel::setSyncWifiOnly,
        onRetryFailedSync = viewModel::retryFailedSync
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreenContent(
    accountState: AuthAccountState,
    uiState: AccountUiState,
    syncSummary: SyncUiSummary = SyncUiSummary(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    challengeFactorA: Int = 7,
    challengeFactorB: Int = 8,
    onNavigateBack: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onToggleRegisterMode: (Boolean) -> Unit = {},
    onEmailChanged: (String) -> Unit = {},
    onPasswordChanged: (String) -> Unit = {},
    onRegisterNicknameChanged: (String) -> Unit = {},
    onSubmitEmailAuth: () -> Unit = {},
    onOpenForgotPassword: () -> Unit = {},
    onDismissForgotPassword: () -> Unit = {},
    onResetEmailChanged: (String) -> Unit = {},
    onSubmitPasswordReset: () -> Unit = {},
    onBirthYearChanged: (String) -> Unit = {},
    onGoogleSignInClick: () -> Unit = {},
    onContinueGuestClick: () -> Unit = {},
    onNicknameChanged: (String) -> Unit = {},
    onSaveNickname: () -> Unit = {},
    onOpenParentalConsent: () -> Unit = {},
    onDismissParentalConsent: () -> Unit = {},
    onParentEmailChanged: (String) -> Unit = {},
    onParentConsentChecked: (Boolean) -> Unit = {},
    onParentMathAnswerChanged: (String) -> Unit = {},
    onSubmitParentalConsent: () -> Unit = {},
    onCheckParentalConsentStatus: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onStartDeleteAccount: () -> Unit = {},
    onDeleteLocalChoiceChanged: (Boolean) -> Unit = {},
    onProceedDeleteStep2: () -> Unit = {},
    onCancelDeleteFlow: () -> Unit = {},
    onConfirmDeleteAccount: () -> Unit = {},
    onSyncNow: () -> Unit = {},
    onToggleWifiOnly: (Boolean) -> Unit = {},
    onRetryFailedSync: () -> Unit = {}
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val consentRequestPending = uiState.parentConsentRequestPending ||
        (accountState.needsParentalConsent && accountState.parentEmail.isNotBlank())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (accountState.isAuthenticated) "Hồ sơ tài khoản" else "Tài khoản & Đồng bộ",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("account_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                    .testTag("account_screen_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (uiState.isOfflineBannerVisible || accountState.sessionExpired) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_offline_or_expired_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (accountState.sessionExpired) Icons.Default.Warning else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (accountState.sessionExpired) {
                                        "Phiên đăng nhập đã hết hạn. Bạn vẫn học và làm bài offline bình thường; hãy đăng nhập lại khi có mạng để đồng bộ."
                                    } else {
                                        "Đang ngoại tuyến hoặc không kết nối được máy chủ. Mọi tính năng học tập trên máy vẫn hoạt động bình thường."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                if (!uiState.errorMessage.isNullOrBlank()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_error_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Lỗi",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = uiState.errorMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                if (!accountState.isAuthenticated) {
                    item {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Đang học ở Chế độ Khách",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Bạn không bắt buộc phải đăng nhập. Khi nâng cấp lên tài khoản (Email hoặc Google), toàn bộ điểm số, flashcard và tiến độ hiện tại trên máy sẽ được giữ nguyên 100%.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = uiState.birthYearInput,
                                    onValueChange = onBirthYearChanged,
                                    label = { Text("Năm sinh học sinh (kiểm tra quy định trẻ em)") },
                                    placeholder = { Text("Ví dụ: 2012") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("birth_year_input")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    SegmentedButton(
                                        selected = !uiState.isRegisterMode,
                                        onClick = { onToggleRegisterMode(false) },
                                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                        modifier = Modifier.testTag("auth_mode_login_tab")
                                    ) {
                                        Text("Đăng nhập")
                                    }
                                    SegmentedButton(
                                        selected = uiState.isRegisterMode,
                                        onClick = { onToggleRegisterMode(true) },
                                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                        modifier = Modifier.testTag("auth_mode_register_tab")
                                    ) {
                                        Text("Tạo tài khoản")
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (uiState.isRegisterMode) {
                                    OutlinedTextField(
                                        value = uiState.registerNicknameInput,
                                        onValueChange = onRegisterNicknameChanged,
                                        label = { Text("Biệt danh học sinh") },
                                        placeholder = { Text("Ví dụ: Minh Anh 7A") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("register_nickname_input")
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                OutlinedTextField(
                                    value = uiState.emailInput,
                                    onValueChange = onEmailChanged,
                                    label = { Text("Email") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("email_input")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = uiState.passwordInput,
                                    onValueChange = onPasswordChanged,
                                    label = { Text("Mật khẩu (tối thiểu 8 ký tự)") },
                                    singleLine = true,
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("password_input")
                                )

                                if (!uiState.isRegisterMode) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = onOpenForgotPassword,
                                            modifier = Modifier.testTag("forgot_password_button")
                                        ) {
                                            Text("Quên mật khẩu?")
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                Button(
                                    onClick = onSubmitEmailAuth,
                                    enabled = !uiState.isLoading,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 50.dp)
                                        .testTag("submit_email_auth_button")
                                ) {
                                    if (uiState.isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Đang kết nối máy chủ...")
                                    } else {
                                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (uiState.isRegisterMode) "Đăng ký bằng Email" else "Đăng nhập bằng Email",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    HorizontalDivider(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "  hoặc  ",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    HorizontalDivider(modifier = Modifier.weight(1f))
                                }
                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedButton(
                                    onClick = onGoogleSignInClick,
                                    enabled = !uiState.isLoading,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 50.dp)
                                        .testTag("google_sign_in_button")
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Nâng cấp / Đăng nhập bằng Google", fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                TextButton(
                                    onClick = onContinueGuestClick,
                                    enabled = !uiState.isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("continue_guest_button")
                                ) {
                                    Text("Dùng thử không cần tài khoản (Tiếp tục học)")
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = accountState.displayName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.testTag("account_display_name")
                                        )
                                        Text(
                                            text = accountState.email.ifBlank { "Tài khoản đã liên kết" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.testTag("account_email")
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFDCFCE7)
                                    ) {
                                        Text(
                                            text = if (accountState.authProvider == "GOOGLE") "Google" else "Email",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF166534)
                                            ),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = uiState.nicknameInput,
                                    onValueChange = onNicknameChanged,
                                    label = { Text("Biệt danh học sinh") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("nickname_input")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = onSaveNickname,
                                    enabled = !uiState.isLoading,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("save_nickname_button")
                                ) {
                                    Text("Lưu biệt danh")
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (accountState.lastSyncedAt != null) {
                                    val timeStr = remember(accountState.lastSyncedAt) {
                                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                            .format(Date(accountState.lastSyncedAt))
                                    }
                                    Text(
                                        text = "Đồng bộ hồ sơ lần cuối: $timeStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        CloudSyncCard(
                            syncSummary = syncSummary,
                            isAccountBusy = uiState.isLoading,
                            onSyncNow = onSyncNow,
                            onToggleWifiOnly = onToggleWifiOnly,
                            onRetryFailedSync = onRetryFailedSync
                        )
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (accountState.needsParentalConsent) {
                                    Color(0xFFFFF7ED)
                                } else {
                                    Color(0xFFECFDF5)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (accountState.needsParentalConsent) {
                                            Icons.Default.FamilyRestroom
                                        } else {
                                            Icons.Default.CheckCircle
                                        },
                                        contentDescription = null,
                                        tint = if (accountState.needsParentalConsent) Color(0xFFC2410C) else Color(0xFF047857)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (accountState.needsParentalConsent) {
                                            "Cần xác nhận của Cha mẹ / Người giám hộ"
                                        } else {
                                            "Trạng thái bảo vệ trẻ em (<16 tuổi)"
                                        },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (accountState.needsParentalConsent) Color(0xFF9A3412) else Color(0xFF065F46)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = when {
                                        !accountState.isMinorUnder16 ->
                                            "Tài khoản từ 16 tuổi trở lên (Năm sinh: ${accountState.birthYear ?: "Chưa khai báo"})."
                                        accountState.parentalConsentGranted -> {
                                            val timeStr = accountState.parentalConsentTimestamp?.let {
                                                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it))
                                            } ?: "Đã lưu"
                                            "Phụ huynh (${accountState.parentEmail}) đã xác nhận đồng ý lúc $timeStr theo Nghị định 13/2023/NĐ-CP."
                                        }
                                        else ->
                                            "Theo Nghị định 13/2023/NĐ-CP và chính sách Google Play cho trẻ em, học sinh dưới 16 tuổi cần sự đồng ý của phụ huynh để lưu hồ sơ trực tuyến."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (accountState.needsParentalConsent) Color(0xFF9A3412) else Color(0xFF047857)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = onOpenParentalConsent,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("open_parental_consent_button")
                                ) {
                                    Text(
                                        if (accountState.needsParentalConsent) "Nhờ phụ huynh xác nhận ngay"
                                        else "Cập nhật thông tin độ tuổi / phụ huynh"
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onSignOutClick,
                                    enabled = !uiState.isLoading,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("sign_out_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Đăng xuất (Giữ dữ liệu học trên máy)")
                                }

                                Button(
                                    onClick = onStartDeleteAccount,
                                    enabled = !uiState.isLoading,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("delete_account_button")
                                ) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Xóa tài khoản & Dữ liệu máy chủ")
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Bảo mật & Quyền riêng tư học sinh",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Chỉ truyền tải qua HTTPS tới máy chủ PocketBase do nhà phát triển quản lý.\n• Token phiên mã hóa bằng Android Keystore; không lưu mật khẩu thô.\n• Không quảng cáo; hỗ trợ xóa tài khoản trong app hoặc qua web.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = onNavigateToPrivacy,
                                modifier = Modifier.testTag("account_privacy_policy_link")
                            ) {
                                Icon(Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Xem Chính sách quyền riêng tư & Hướng dẫn xóa dữ liệu")
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = onDismissForgotPassword,
            title = { Text("Quên mật khẩu", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Nhập email bạn đã đăng ký. Máy chủ sẽ gửi liên kết đặt lại mật khẩu qua SMTP.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = uiState.resetEmailInput,
                        onValueChange = onResetEmailChanged,
                        label = { Text("Email tài khoản") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_password_email_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onSubmitPasswordReset,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.testTag("confirm_reset_password_button")
                ) {
                    Text("Gửi email khôi phục")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissForgotPassword) {
                    Text("Đóng")
                }
            }
        )
    }

    if (uiState.showParentalConsentDialog) {
        AlertDialog(
            onDismissRequest = onDismissParentalConsent,
            title = {
                Text("Xác nhận của Cha mẹ / Người giám hộ", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Tài khoản học sinh dưới 16 tuổi cần phụ huynh/người giám hộ xác nhận qua email trước khi bật đồng bộ trực tuyến. Quy trình này cần được rà soát pháp lý trước khi phát hành.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (consentRequestPending) {
                        Text(
                            text = "Đang chờ phụ huynh xác nhận qua email. Đồng bộ vẫn tắt; bài học tiếp tục lưu trên thiết bị.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("parent_dialog_pending_message")
                        )
                        OutlinedButton(
                            onClick = onSubmitParentalConsent,
                            enabled = !uiState.isLoading,
                            modifier = Modifier.testTag("parent_dialog_resend_email_button")
                        ) {
                            Text("Gửi lại email xác nhận")
                        }
                    } else {
                        OutlinedTextField(
                            value = uiState.birthYearInput,
                            onValueChange = onBirthYearChanged,
                            label = { Text("Năm sinh học sinh") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parent_dialog_birth_year")
                        )
                        OutlinedTextField(
                            value = uiState.parentEmailInput,
                            onValueChange = onParentEmailChanged,
                            label = { Text("Email của Cha mẹ / Người giám hộ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parent_email_input")
                        )
                        OutlinedTextField(
                            value = uiState.parentMathChallengeAnswer,
                            onValueChange = onParentMathAnswerChanged,
                            label = { Text("Cổng phụ huynh: $challengeFactorA × $challengeFactorB = ?") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parent_gate_answer_input")
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = uiState.parentConsentChecked,
                                onCheckedChange = onParentConsentChecked,
                                modifier = Modifier.testTag("parent_consent_checkbox")
                            )
                            Text(
                                text = "Tôi sẽ nhờ cha mẹ/người giám hộ mở email và xác nhận trước khi bật đồng bộ.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (consentRequestPending) {
                    Button(
                        onClick = onCheckParentalConsentStatus,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.testTag("parent_dialog_check_consent_button")
                    ) {
                        Text("Tôi đã xác nhận — kiểm tra lại")
                    }
                } else {
                    Button(
                        onClick = onSubmitParentalConsent,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.testTag("confirm_parental_consent_button")
                    ) {
                        Text("Gửi email xác nhận")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissParentalConsent) {
                    Text(if (consentRequestPending) "Đóng" else "Để sau")
                }
            }
        )
    }

    if (uiState.deleteStep == 1) {
        AlertDialog(
            onDismissRequest = onCancelDeleteFlow,
            title = { Text("Xóa tài khoản (Bước 1/2)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Hành động này sẽ xóa vĩnh viễn tài khoản xác thực và toàn bộ dữ liệu đã đồng bộ trên máy chủ PocketBase.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Đối với dữ liệu đang lưu trên điện thoại này:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = !uiState.deleteLocalDataChoice,
                            onClick = { onDeleteLocalChoiceChanged(false) },
                            modifier = Modifier.testTag("keep_local_data_radio")
                        )
                        Text("Giữ lại tiến độ trên máy (chuyển về Chế độ Khách)", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = uiState.deleteLocalDataChoice,
                            onClick = { onDeleteLocalChoiceChanged(true) },
                            modifier = Modifier.testTag("delete_local_data_radio")
                        )
                        Text("Xóa luôn toàn bộ dữ liệu học tập trên máy", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onProceedDeleteStep2,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("delete_account_step1_next")
                ) {
                    Text("Tiếp tục")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDeleteFlow) {
                    Text("Hủy")
                }
            }
        )
    }

    if (uiState.deleteStep == 2) {
        AlertDialog(
            onDismissRequest = onCancelDeleteFlow,
            title = {
                Text(
                    text = "Xác nhận xóa vĩnh viễn (Bước 2/2)",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (uiState.deleteLocalDataChoice) {
                        "Tài khoản trên máy chủ VÀ toàn bộ tiến độ, điểm thi, flashcard trên điện thoại này sẽ bị xóa vĩnh viễn và không thể khôi phục."
                    } else {
                        "Tài khoản và dữ liệu trên máy chủ sẽ bị xóa vĩnh viễn. Tiến độ học hiện tại vẫn được giữ lại trên điện thoại ở Chế độ Khách."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmDeleteAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_account_final")
                ) {
                    Text("XÁC NHẬN XÓA")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDeleteFlow) {
                    Text("Quay lại")
                }
            }
        )
    }
}

@Composable
private fun CloudSyncCard(
    syncSummary: SyncUiSummary,
    isAccountBusy: Boolean,
    onSyncNow: () -> Unit,
    onToggleWifiOnly: (Boolean) -> Unit,
    onRetryFailedSync: () -> Unit
) {
    val isSyncing = syncSummary.status == SyncStatus.SYNCING
    val (badgeText, badgeBg, badgeFg, statusIcon) = when (syncSummary.status) {
        SyncStatus.SYNCING -> Quad("Đang đồng bộ", Color(0xFFE9F1F8), Color(0xFF3B6A92), Icons.Default.CloudSync)
        SyncStatus.SUCCESS -> Quad("Đã đồng bộ", Color(0xFFDCFCE7), Color(0xFF166534), Icons.Default.CloudDone)
        SyncStatus.OFFLINE -> Quad("Ngoại tuyến", Color(0xFFF3F4F6), Color(0xFF374151), Icons.Default.CloudOff)
        SyncStatus.PAUSED_METERED -> Quad("Chờ Wi-Fi", Color(0xFFFEF3C7), Color(0xFF92400E), Icons.Default.Wifi)
        SyncStatus.RATE_LIMITED -> Quad("Tạm nghỉ", Color(0xFFFFEDD5), Color(0xFF9A3412), Icons.Default.Warning)
        SyncStatus.ERROR -> Quad("Cần chú ý", Color(0xFFFEE2E2), Color(0xFF991B1B), Icons.Default.Warning)
        SyncStatus.IDLE -> Quad("Sẵn sàng", Color(0xFFE0F2FE), Color(0xFF0369A1), Icons.Default.CloudSync)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sync_status_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Đồng bộ đám mây",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = badgeFg
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = syncSummary.statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("sync_status_message")
            )

            if (isSyncing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }

            val lastSyncFormatted = remember(syncSummary.lastSyncedAt) {
                syncSummary.lastSyncedAt?.let {
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(it))
                } ?: "Chưa đồng bộ lần nào"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Lần cuối: $lastSyncFormatted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Đang chờ: ${syncSummary.pendingChangesCount}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (syncSummary.pendingChangesCount > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Button(
                onClick = onSyncNow,
                enabled = !isAccountBusy && !isSyncing,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("sync_now_button")
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đang đồng bộ...")
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đồng bộ ngay")
                }
            }

            if (syncSummary.permanentFailedCount > 0) {
                OutlinedButton(
                    onClick = onRetryFailedSync,
                    enabled = !isAccountBusy && !isSyncing,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("retry_failed_sync_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Thử lại ${syncSummary.permanentFailedCount} mục lỗi")
                }
            }

            HorizontalDivider()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Chỉ đồng bộ qua Wi-Fi",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = "Tiết kiệm 4G/5G; vẫn học bình thường khi ngoại tuyến",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = syncSummary.syncWifiOnly,
                    onCheckedChange = onToggleWifiOnly,
                    modifier = Modifier.testTag("sync_wifi_only_switch")
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Preview(name = "Guest Mode - Light", showBackground = true)
@Composable
fun AccountScreenGuestLightPreview() {
    HocTotTHCSTheme(darkTheme = false) {
        AccountScreenContent(
            accountState = AuthAccountState(mode = AccountMode.GUEST),
            uiState = AccountUiState(birthYearInput = "2012")
        )
    }
}

@Preview(name = "Authenticated - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AccountScreenAuthDarkPreview() {
    HocTotTHCSTheme(darkTheme = true) {
        AccountScreenContent(
            accountState = AuthAccountState(
                uid = "pb_preview_1",
                email = "minhanh@gmail.com",
                displayName = "Minh Anh 7A",
                mode = AccountMode.AUTHENTICATED,
                authProvider = "EMAIL",
                birthYear = 2012,
                isMinorUnder16 = true,
                parentalConsentGranted = true,
                parentEmail = "phuhuynh@gmail.com",
                parentalConsentTimestamp = 1700000000000L,
                lastSyncedAt = 1700000000000L
            ),
            uiState = AccountUiState(nicknameInput = "Minh Anh 7A")
        )
    }
}

@Preview(name = "Offline & Error", showBackground = true)
@Composable
fun AccountScreenOfflineErrorPreview() {
    HocTotTHCSTheme(darkTheme = false) {
        AccountScreenContent(
            accountState = AuthAccountState(
                uid = "pb_preview_2",
                email = "hocsinh@gmail.com",
                displayName = "Hoàng Nam",
                mode = AccountMode.AUTHENTICATED,
                sessionExpired = true
            ),
            uiState = AccountUiState(
                isOfflineBannerVisible = true,
                errorMessage = "Không có kết nối mạng. Tiến độ học vẫn được lưu an toàn trên thiết bị."
            )
        )
    }
}
