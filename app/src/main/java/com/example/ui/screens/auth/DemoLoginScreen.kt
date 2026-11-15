package com.example.ui.screens.auth

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.data.repository.FakeAuthRepository
import com.example.ui.screens.account.AccountViewModel
import com.example.ui.theme.HocTotTHCSTheme
import com.example.util.GoogleCredentialResult
import com.example.util.GoogleSignInHelper
import kotlinx.coroutines.launch

/**
 * Màn hình Đăng nhập Demo (Google / Facebook / Khách).
 * Đây là landing demo riêng biệt, tách khỏi [com.example.ui.screens.account.AccountScreen].
 * Tái sử dụng đúng [AccountViewModel] và [GoogleSignInHelper] hiện có — không nhân bản logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoLoginScreen(
    onNavigateBack: () -> Unit,
    onAuthSuccess: () -> Unit,
    viewModel: AccountViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accountState by viewModel.accountState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFacebookDialog by remember { mutableStateOf(false) }

    // Thu thập sự kiện (snackbar) giống AccountScreen.
    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Khi xác thực thành công (Google thật hoặc Facebook demo), báo về上层 điều hướng.
    LaunchedEffect(accountState.isAuthenticated) {
        if (accountState.isAuthenticated) {
            onAuthSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đăng nhập Demo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("demo_login_back_button")
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
                    .testTag("demo_login_list"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("demo_login_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Học Tốt THCS",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Đăng nhập để đồng bộ tiến độ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            // Google — tái sử dụng GoogleSignInHelper (Credential Manager) giống AccountScreen.kt.
                            OutlinedButton(
                                onClick = {
                                    if (!uiState.isLoading) {
                                        val activity = context as? Activity
                                        if (activity == null) {
                                            viewModel.cancelLoadingWithMessage(
                                                "Không tìm thấy cửa sổ Activity để đăng nhập Google."
                                            )
                                        } else {
                                            viewModel.beginLoading()
                                            scope.launch {
                                                when (val cred =
                                                    GoogleSignInHelper.requestInteractiveGoogleSignIn(activity)) {
                                                    is GoogleCredentialResult.Success -> {
                                                        viewModel.onGoogleIdTokenReceived(
                                                            cred.idToken,
                                                            cred.displayName
                                                        )
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
                                enabled = !uiState.isLoading,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("demo_google_button")
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Đang kết nối Google...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Đăng nhập với Google", fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // TODO(facebook): wire real Facebook SDK (com.facebook.android:facebook-login) + app id + key hash
                            OutlinedButton(
                                onClick = { showFacebookDialog = true },
                                enabled = !uiState.isLoading,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("demo_facebook_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Đăng nhập với Facebook (demo)", fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(
                                onClick = onNavigateBack,
                                enabled = !uiState.isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("demo_guest_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tiếp tục với tư cách Khách")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFacebookDialog) {
        AlertDialog(
            onDismissRequest = { showFacebookDialog = false },
            title = { Text("Facebook (chế độ demo)", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Facebook login đang ở chế độ demo — chưa kết nối Facebook SDK. " +
                        "Bấm 'Mô phỏng đăng nhập' để tiếp tục ở chế độ demo."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            showFacebookDialog = false
                            snackbarHostState.showSnackbar("Đăng nhập Facebook (demo)")
                            onAuthSuccess()
                        }
                    },
                    modifier = Modifier.testTag("demo_facebook_simulate_button")
                ) {
                    Text("Mô phỏng đăng nhập")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFacebookDialog = false }) {
                    Text("Đóng")
                }
            },
            modifier = Modifier.testTag("demo_facebook_dialog")
        )
    }
}

@Preview(name = "Demo Login - Light", showBackground = true)
@Composable
fun DemoLoginScreenLightPreview() {
    HocTotTHCSTheme(darkTheme = false) {
        DemoLoginScreen(
            onNavigateBack = {},
            onAuthSuccess = {},
            viewModel = AccountViewModel(FakeAuthRepository())
        )
    }
}

@Preview(name = "Demo Login - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DemoLoginScreenDarkPreview() {
    HocTotTHCSTheme(darkTheme = true) {
        DemoLoginScreen(
            onNavigateBack = {},
            onAuthSuccess = {},
            viewModel = AccountViewModel(FakeAuthRepository())
        )
    }
}
