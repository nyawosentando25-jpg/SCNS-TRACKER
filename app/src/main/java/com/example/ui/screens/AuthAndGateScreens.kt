package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.ChairpersonConfig
import com.example.data.model.ExecutiveMember
import com.example.data.model.ScnsCalculators
import com.example.data.model.ScnsConstants
import com.example.ui.components.AppDetailsDialog
import com.example.ui.components.ScnsBrandCrest
import com.example.ui.components.ScnsDropdownSelector
import com.example.ui.components.ScnsSafetyChipsRow
import com.example.ui.theme.ScnsAmber
import com.example.ui.theme.ScnsAmberContainer
import com.example.ui.theme.ScnsCrimson
import com.example.ui.theme.ScnsEmeraldDeep
import com.example.ui.theme.ScnsGold
import com.example.ui.theme.ScnsGoldBright
import com.example.ui.theme.ScnsNavy
import com.example.ui.theme.ScnsNavyDark
import com.example.ui.theme.ScnsNumericStyle

@Composable
fun GoogleAuthScreen(
    isChecking: Boolean,
    isSigningIn: Boolean,
    errorMessage: String?,
    onSignInWithGoogle: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(ScnsNavyDark, ScnsNavy, Color(0xFF8A2218))
                )
            )
            .safeDrawingPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            shape = MaterialTheme.shapes.extraLarge,
            color = Color.White.copy(alpha = 0.10f),
            border = BorderStroke(2.dp, ScnsGoldBright)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ScnsBrandCrest(size = 128.dp)
                Spacer(Modifier.height(14.dp))

                Text(
                    text = ScnsConstants.ORG_NAME,
                    style = MaterialTheme.typography.displaySmall,
                    color = ScnsGoldBright,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = ScnsConstants.ORG_SUBTITLE,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = ScnsGoldBright
                ) {
                    Text(
                        text = ScnsConstants.MOTTO_ZULU,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = ScnsNavy,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))
                ScnsSafetyChipsRow()
                Spacer(Modifier.height(18.dp))

                if (isChecking) {
                    CircularProgressIndicator(color = ScnsGoldBright)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Checking Google session...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                } else {
                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = Color(0xFFFFE4E1),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = ScnsCrimson,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onSignInWithGoogle,
                        enabled = !isSigningIn,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScnsGoldBright,
                            contentColor = ScnsNavy
                        ),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                color = ScnsNavy,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            text = if (isSigningIn) "SIGNING IN..." else "SIGN IN WITH GOOGLE • NGENA",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showDetails = true },
                        border = BorderStroke(1.5.dp, ScnsGold),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_app_details_button")
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("App Details • Creator • Permissions")
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "${ScnsConstants.MOTTO_ENGLISH}  •  Created by ${ScnsConstants.CREATOR_CREDIT}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showDetails) {
        AppDetailsDialog(
            learnerCount = 0,
            executiveCount = 0,
            chairConfig = ChairpersonConfig(),
            userEmail = null,
            onDismiss = { showDetails = false }
        )
    }
}

@Composable
fun ExecutiveGateScreen(
    executives: List<ExecutiveMember>,
    chairConfig: ChairpersonConfig,
    trialDaysLeft: Int,
    statusMessage: String?,
    canDismissModal: Boolean,
    onDismissModal: () -> Unit,
    onStartTrial: () -> Unit,
    onRegisterExecutive: (String, String, String, String, String, String, String) -> Unit,
    onLoginExecutive: (String, String) -> Unit,
    onRequestOtp: (String) -> Unit,
    onVerifyOtp: (String, String) -> Unit,
    onApproveOrOtp: (ExecutiveMember, Boolean) -> Unit,
    onRemoveExecutive: (String) -> Unit,
    onSaveChairConfig: (String, String, String) -> Unit,
    onSignOutGoogle: () -> Unit
) {
    var gateTab by rememberSaveable { mutableIntStateOf(0) }
    var showHowItWorks by rememberSaveable { mutableStateOf(false) }

    // Sign Up state
    var uName by rememberSaveable { mutableStateOf("") }
    var uSurname by rememberSaveable { mutableStateOf("") }
    var uId by rememberSaveable { mutableStateOf("") }
    var uCell by rememberSaveable { mutableStateOf("") }
    var uPin by rememberSaveable { mutableStateOf("") }
    var uRole by rememberSaveable { mutableStateOf("Secretary") }
    var uTerm by rememberSaveable { mutableStateOf("Term 1") }

    // Login state
    var loginCell by rememberSaveable { mutableStateOf("") }
    var loginPin by rememberSaveable { mutableStateOf("") }

    // OTP verification
    var otpCell by rememberSaveable { mutableStateOf("") }
    var otpCode by rememberSaveable { mutableStateOf("") }

    // Chair settings
    var chairNameInput by rememberSaveable(chairConfig.chairName) { mutableStateOf(chairConfig.chairName) }
    var chairCellInput by rememberSaveable(chairConfig.chairCell) { mutableStateOf(chairConfig.chairCell) }
    var otpSenderInput by rememberSaveable(chairConfig.otpSender) { mutableStateOf(chairConfig.otpSender) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScnsNavy)
            .safeDrawingPadding()
            .padding(14.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ScnsBrandCrest(size = 56.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = ScnsConstants.ORG_NAME,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ScnsNavy
                                )
                                Text(
                                    text = "Executive Access (${executives.size}/${ScnsConstants.MAX_EXECUTIVES} Users)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (canDismissModal) {
                            OutlinedButton(onClick = onDismissModal) {
                                Text("Back")
                            }
                        }
                    }

                    if (!statusMessage.isNullOrBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = ScnsAmberContainer,
                            border = BorderStroke(1.dp, ScnsAmber)
                        ) {
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ScnsNavyDark,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("Sign Up", "Login", "OTP", "Chair").forEachIndexed { idx, label ->
                            SegmentedButton(
                                selected = gateTab == idx,
                                onClick = { gateTab = idx },
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = 4)
                            ) {
                                Text(label, maxLines = 1)
                            }
                        }
                    }

                    when (gateTab) {
                        0 -> {
                            Text(
                                text = "Chairperson immediate access: ${chairConfig.chairName} (${chairConfig.chairCell})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = uName,
                                    onValueChange = { uName = it },
                                    label = { Text("Name *") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("exec_signup_name")
                                )
                                OutlinedTextField(
                                    value = uSurname,
                                    onValueChange = { uSurname = it },
                                    label = { Text("Surname *") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("exec_signup_surname")
                                )
                            }
                            OutlinedTextField(
                                value = uId,
                                onValueChange = { if (it.length <= 13) uId = it.filter(Char::isDigit) },
                                label = { Text("ID Number (13 digits, optional)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = uCell,
                                    onValueChange = { uCell = it },
                                    label = { Text("Cellphone *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.weight(1.3f).testTag("exec_signup_cell")
                                )
                                OutlinedTextField(
                                    value = uPin,
                                    onValueChange = { if (it.length <= 4) uPin = it.filter(Char::isDigit) },
                                    label = { Text("4-digit PIN *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("exec_signup_pin")
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ScnsDropdownSelector(
                                    label = "Executive Role",
                                    selectedValue = uRole,
                                    options = ScnsConstants.EXECUTIVE_ROLES.map { it to it },
                                    onSelected = { uRole = it },
                                    modifier = Modifier.weight(1.5f)
                                )
                                ScnsDropdownSelector(
                                    label = "Term",
                                    selectedValue = uTerm,
                                    options = ScnsConstants.TERMS.map { it to it },
                                    onSelected = { uTerm = it },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Button(
                                onClick = {
                                    onRegisterExecutive(uName, uSurname, uId, uCell, uPin, uRole, uTerm)
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("exec_request_access_button")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Request / Save Executive Access")
                            }
                        }

                        1 -> {
                            OutlinedTextField(
                                value = loginCell,
                                onValueChange = { loginCell = it },
                                label = { Text("Cellphone Number *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("exec_login_cell")
                            )
                            OutlinedTextField(
                                value = loginPin,
                                onValueChange = { if (it.length <= 4) loginPin = it.filter(Char::isDigit) },
                                label = { Text("4-digit PIN *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("exec_login_pin")
                            )
                            Button(
                                onClick = { onLoginExecutive(loginCell, loginPin) },
                                colors = ButtonDefaults.buttonColors(containerColor = ScnsEmeraldDeep),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("exec_login_button")
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Login to SCNS Records")
                            }
                            OutlinedButton(
                                onClick = {
                                    otpCell = loginCell
                                    onRequestOtp(loginCell)
                                    gateTab = 2
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Forgot PIN — Request OTP From Chairperson")
                            }
                        }

                        2 -> {
                            Text(
                                text = "Enter the 6-digit OTP shared by ${chairConfig.chairName} (${chairConfig.chairCell}).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = otpCell,
                                onValueChange = { otpCell = it },
                                label = { Text("Executive Cellphone") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = otpCode,
                                onValueChange = { if (it.length <= 6) otpCode = it.filter(Char::isDigit) },
                                label = { Text("6-digit OTP") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("exec_otp_input")
                            )
                            Button(
                                onClick = { onVerifyOtp(otpCell, otpCode) },
                                colors = ButtonDefaults.buttonColors(containerColor = ScnsEmeraldDeep),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("exec_verify_otp_button")
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Verify OTP & Enter App")
                            }
                        }

                        3 -> {
                            Text(
                                text = "Chairperson & Committee Management (Max 4 Executives)",
                                style = MaterialTheme.typography.titleSmall,
                                color = ScnsNavy
                            )
                            val prevActive = chairConfig.prevChairCell.isNotBlank() &&
                                System.currentTimeMillis() <= chairConfig.prevCellUntilMs
                            if (prevActive) {
                                Text(
                                    text = "Previous Chair cell (${chairConfig.prevChairCell}) stays valid during 7-day handover.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ScnsAmber
                                )
                            }
                            OutlinedTextField(
                                value = chairNameInput,
                                onValueChange = { chairNameInput = it },
                                label = { Text("Chairperson Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = chairCellInput,
                                    onValueChange = { chairCellInput = it },
                                    label = { Text("Chairperson Cell") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                ScnsDropdownSelector(
                                    label = "OTP Sender",
                                    selectedValue = otpSenderInput,
                                    options = ScnsConstants.OTP_SENDERS.map { it to it },
                                    onSelected = { otpSenderInput = it },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Button(
                                onClick = {
                                    onSaveChairConfig(chairNameInput, chairCellInput, otpSenderInput)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ScnsEmeraldDeep),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Chairperson & OTP Sender")
                            }

                            HorizontalDivider()
                            Text(
                                text = "Registered Executives (${executives.size}/4)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (executives.isEmpty()) {
                                Text(
                                    text = "No executives added yet. Sign up executives in tab 1.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                executives.forEach { exec ->
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${exec.name} ${exec.surname} (${exec.role})",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "${exec.cell} • ${exec.termLabel} • ${if (exec.approved) "Approved" else "Pending"}",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                                IconButton(onClick = { onRemoveExecutive(exec.id) }) {
                                                    Icon(
                                                        Icons.Default.DeleteOutline,
                                                        contentDescription = "Remove ${exec.name}",
                                                        tint = ScnsCrimson
                                                    )
                                                }
                                            }
                                            if (exec.otp.isNotBlank()) {
                                                Text(
                                                    text = "Active OTP: ${exec.otp}",
                                                    style = ScnsNumericStyle.copy(color = ScnsNavy)
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = { onApproveOrOtp(exec, true) },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Approve")
                                                }
                                                OutlinedButton(
                                                    onClick = { onApproveOrOtp(exec, false) },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Gen OTP")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7-Day Free Trial & How It Works Card
            Surface(
                shape = MaterialTheme.shapes.large,
                color = ScnsAmberContainer,
                border = BorderStroke(2.dp, ScnsAmber)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (trialDaysLeft > 0) "Free Trial Active — $trialDaysLeft day(s) left" else "Free Trial — 7 Days",
                        style = MaterialTheme.typography.titleMedium,
                        color = ScnsNavy,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = onStartTrial,
                        colors = ButtonDefaults.buttonColors(containerColor = ScnsEmeraldDeep),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("start_free_trial_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (trialDaysLeft > 0) "Continue in Free Trial Mode" else "Start Free Trial (7 Days)")
                    }
                    TextButton(onClick = { showHowItWorks = !showHowItWorks }) {
                        Text(if (showHowItWorks) "Hide Guide" else "Show Me How It Works First")
                    }
                    if (showHowItWorks) {
                        Text(
                            text = "1. Add learners or paste a batch list under Upload Photo.\n" +
                                "2. Track Saturday & Sunday attendance per Subject or Grade and deduplicate repeats in 1 tap.\n" +
                                "3. Record Monthly & Termly payments with automatic balance math (R2,400/subject + R100 reg - R500 for 3+ subjects).\n" +
                                "4. Enter 4 exam marks/100 and export One-Page School Progress Reports or full tables directly to Downloads & WhatsApp.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ScnsNavyDark
                        )
                    }
                    TextButton(onClick = onSignOutGoogle) {
                        Text("Switch / Sign Out Google Account", color = ScnsCrimson)
                    }
                }
            }
        }
    }
}
