package com.example.ui.screens.auth

import android.content.res.Configuration
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
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.domain.repository.AccountInputValidator
import com.example.domain.repository.AuthAccountState
import com.example.R
import com.example.ui.screens.account.AccountUiState
import com.example.ui.screens.account.AccountViewModel
import com.example.ui.theme.HocTotTHCSTheme

/**
 * CẦN LUẬT SƯ:
 * Màn hình Cổng xác nhận của Cha mẹ / Người giám hộ (Parental Gate)
 * Tuân thủ Nghị định 13/2023/NĐ-CP Điều 20 (bảo vệ dữ liệu cá nhân của trẻ em)
 * và Chính sách Google Play Families Policy dành cho ứng dụng học sinh THCS.
 *
 * Màn hình này xuất hiện trong luồng Onboarding sau khi học sinh đăng nhập bằng Google hoặc Email
 * nếu học sinh dưới 16 tuổi.
 */
@Composable
fun ParentalConsentScreen(
    viewModel: AccountViewModel,
    onConsentSuccess: () -> Unit,
    onSkipOrLater: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val accountState by viewModel.accountState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val consentRequestPending = uiState.parentConsentRequestPending ||
        (accountState.needsParentalConsent && accountState.parentEmail.isNotBlank())

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(accountState.parentalConsentGranted) {
        if (accountState.parentalConsentGranted) {
            onConsentSuccess()
        }
    }

    ParentalConsentContent(
        uiState = uiState.copy(parentConsentRequestPending = consentRequestPending),
        accountState = accountState,
        snackbarHostState = snackbarHostState,
        challengeFactorA = viewModel.challengeFactorA,
        challengeFactorB = viewModel.challengeFactorB,
        onBirthYearChanged = { viewModel.updateBirthYearInput(it) },
        onParentEmailChanged = { viewModel.updateParentEmailInput(it) },
        onParentConsentChecked = { viewModel.updateParentConsentChecked(it) },
        onParentMathAnswerChanged = { viewModel.updateParentMathAnswer(it) },
        onSubmitConsent = { viewModel.submitParentalConsent() },
        onCheckConsentStatus = { viewModel.checkParentalConsentStatus() },
        onSkipOrLater = onSkipOrLater,
        onNavigateToPrivacy = onNavigateToPrivacy
    )
}

@Composable
fun ParentalConsentContent(
    uiState: AccountUiState,
    accountState: AuthAccountState,
    snackbarHostState: SnackbarHostState,
    challengeFactorA: Int = 7,
    challengeFactorB: Int = 8,
    onBirthYearChanged: (String) -> Unit,
    onParentEmailChanged: (String) -> Unit,
    onParentConsentChecked: (Boolean) -> Unit,
    onParentMathAnswerChanged: (String) -> Unit,
    onSubmitConsent: () -> Unit,
    onSkipOrLater: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onCheckConsentStatus: () -> Unit = {}
) {
    val birthYearInt = uiState.birthYearInput.toIntOrNull()
    val isMinor = AccountInputValidator.isMinorUnder16(birthYearInt)

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
                    .testTag("parental_consent_list"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Header & Icon bảo vệ trẻ em
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FamilyRestroom,
                                    contentDescription = "Bảo vệ gia đình",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Xác nhận của Phụ huynh",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Bảo vệ an toàn thông tin học sinh theo Nghị định 13/2023/NĐ-CP & Google Play",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // 2. Banner Thông báo lỗi nếu có
                if (uiState.errorMessage != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = uiState.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // 3. Form Xác nhận của Cha mẹ
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Thông tin học sinh & Phụ huynh",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            Text(
                                text = "Học sinh THCS (dưới 16 tuổi) cần có sự đồng ý của cha mẹ hoặc người giám hộ hợp pháp khi tạo tài khoản đồng bộ trực tuyến.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Năm sinh học sinh
                            OutlinedTextField(
                                value = uiState.birthYearInput,
                                onValueChange = onBirthYearChanged,
                                label = { Text("Năm sinh học sinh (ví dụ: 2012)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("parent_consent_birth_year_input")
                            )

                            if (isMinor) {
                                // Email phụ huynh
                                OutlinedTextField(
                                    value = uiState.parentEmailInput,
                                    onValueChange = onParentEmailChanged,
                                    label = { Text("Email Cha mẹ / Người giám hộ") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("parent_consent_parent_email_input")
                                )

                                // Phép tính chỉ là bước kiểm tra giao diện; xác nhận thật diễn ra qua email.
                                OutlinedTextField(
                                    value = uiState.parentMathChallengeAnswer,
                                    onValueChange = onParentMathAnswerChanged,
                                    label = { Text("Cổng kiểm tra phụ huynh: $challengeFactorA × $challengeFactorB = ?") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("parent_consent_gate_answer_input")
                                )

                                // Checkbox đồng ý
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = uiState.parentConsentChecked,
                                        onCheckedChange = onParentConsentChecked,
                                        modifier = Modifier.testTag("parent_consent_agree_checkbox")
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.parent_consent_checkbox),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            } else {
                                Text(
                                    text = "✓ Học sinh từ 16 tuổi trở lên có thể tự quản lý tài khoản học tập theo quy định.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                // 4. Nút xác nhận
                item {
                    Button(
                        onClick = onSubmitConsent,
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("parent_consent_submit_button")
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.parent_consent_submit))
                        }
                    }
                }

                if (uiState.parentConsentRequestPending) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.parent_consent_pending),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = onCheckConsentStatus,
                                enabled = !uiState.isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("parent_consent_check_status_button")
                            ) {
                                Text(stringResource(R.string.parent_consent_check_status))
                            }
                        }
                    }
                }

                // 5. Nút Để sau / Chế độ giới hạn
                item {
                    OutlinedButton(
                        onClick = onSkipOrLater,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("parent_consent_skip_button")
                    ) {
                        Text("Để sau (Tiếp tục học ở chế độ ngoại tuyến)")
                    }
                }

                // 5. Liên kết Chính sách quyền riêng tư
                item {
                    TextButton(
                        onClick = onNavigateToPrivacy,
                        modifier = Modifier.testTag("parent_consent_privacy_link")
                    ) {
                        Text(
                            text = "Xem chi tiết Chính sách quyền riêng tư & Bảo vệ trẻ em",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews cho màn hình Xác nhận phụ huynh
// ---------------------------------------------------------------------------

@Preview(name = "Sáng - Học sinh dưới 16 tuổi", showBackground = true)
@Composable
fun ParentalConsentScreenPreviewLightMinor() {
    HocTotTHCSTheme(darkTheme = false) {
        ParentalConsentContent(
            uiState = AccountUiState(birthYearInput = "2012"),
            accountState = AuthAccountState(isMinorUnder16 = true),
            snackbarHostState = remember { SnackbarHostState() },
            onBirthYearChanged = {},
            onParentEmailChanged = {},
            onParentConsentChecked = {},
            onParentMathAnswerChanged = {},
            onSubmitConsent = {},
            onSkipOrLater = {},
            onNavigateToPrivacy = {}
        )
    }
}

@Preview(name = "Tối - Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ParentalConsentScreenPreviewDark() {
    HocTotTHCSTheme(darkTheme = true) {
        ParentalConsentContent(
            uiState = AccountUiState(birthYearInput = "2012"),
            accountState = AuthAccountState(isMinorUnder16 = true),
            snackbarHostState = remember { SnackbarHostState() },
            onBirthYearChanged = {},
            onParentEmailChanged = {},
            onParentConsentChecked = {},
            onParentMathAnswerChanged = {},
            onSubmitConsent = {},
            onSkipOrLater = {},
            onNavigateToPrivacy = {}
        )
    }
}

@Preview(name = "Sáng - Học sinh từ 16 tuổi", showBackground = true)
@Composable
fun ParentalConsentScreenPreviewAdult() {
    HocTotTHCSTheme {
        ParentalConsentContent(
            uiState = AccountUiState(birthYearInput = "2009"),
            accountState = AuthAccountState(isMinorUnder16 = false),
            snackbarHostState = remember { SnackbarHostState() },
            onBirthYearChanged = {},
            onParentEmailChanged = {},
            onParentConsentChecked = {},
            onParentMathAnswerChanged = {},
            onSubmitConsent = {},
            onSkipOrLater = {},
            onNavigateToPrivacy = {}
        )
    }
}
