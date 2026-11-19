package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.ScnsCalculators
import com.example.data.model.ScnsConstants
import com.example.ui.components.AppDetailsDialog
import com.example.ui.components.ScnsBrandCrest
import com.example.ui.components.ScnsMetricTile
import com.example.ui.components.ScnsSafetyChipsRow
import com.example.ui.components.ScnsSectionCard
import com.example.ui.theme.ScnsAmber
import com.example.ui.theme.ScnsAmberContainer
import com.example.ui.theme.ScnsCrimson
import com.example.ui.theme.ScnsEmeraldDeep
import com.example.ui.theme.ScnsGoldBright
import com.example.ui.theme.ScnsNavy
import com.example.ui.theme.ScnsNavyDark
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.LearnersHubTab
import com.example.ui.viewmodel.ScnsUiState
import com.example.util.PdfExportScope

@Composable
fun HomeScreen(
    uiState: ScnsUiState,
    onNavigateRoute: (String) -> Unit,
    onSelectHubTab: (LearnersHubTab) -> Unit,
    onExportPdf: (PdfExportScope, Boolean) -> Unit,
    onOpenExecutiveModal: () -> Unit
) {
    var showAppDetails by remember { mutableStateOf(false) }
    val userEmail = (uiState.authState as? AuthUiState.SignedIn)?.email

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Branded SCNS Hero Card
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 4.dp,
            border = BorderStroke(2.dp, ScnsGoldBright)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ScnsNavyDark, ScnsNavy, Color(0xFF8C241B))
                        )
                    )
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScnsBrandCrest(size = 92.dp)
                    Text(
                        text = ScnsConstants.ORG_NAME,
                        style = MaterialTheme.typography.headlineMedium,
                        color = ScnsGoldBright,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = ScnsConstants.ORG_SUBTITLE,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = ScnsGoldBright
                    ) {
                        Text(
                            text = ScnsConstants.MOTTO_ZULU,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = ScnsNavy,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        text = ScnsConstants.MOTTO_ENGLISH,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                    ScnsSafetyChipsRow()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAppDetails = true },
                            border = BorderStroke(1.dp, ScnsGoldBright),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("home_app_details_button")
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("App Details", maxLines = 1)
                        }
                        Button(
                            onClick = onOpenExecutiveModal,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ScnsGoldBright,
                                contentColor = ScnsNavy
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("home_chair_users_button")
                        ) {
                            Text(
                                text = "Users (${uiState.executives.size}/4)",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Trial / Active Executive Status Banner
        if (uiState.isTrialActive || uiState.activeExecutive != null) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = ScnsAmberContainer,
                border = BorderStroke(1.dp, ScnsAmber)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val exec = uiState.activeExecutive
                    val text = if (exec != null) {
                        "Active: ${exec.name} ${exec.surname} (${exec.role})"
                    } else {
                        "Free Trial Mode • ${uiState.trialDaysLeft} day(s) remaining"
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        color = ScnsNavyDark,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Bank: ${ScnsConstants.BANK_REF.takeLast(10)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScnsNavy
                    )
                }
            }
        }

        // Live KPI Grid
        val m = uiState.metrics
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Live SCNS Overview",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScnsMetricTile(
                    label = "Total Learners",
                    value = m.totalLearners.toString(),
                    accentColor = ScnsNavy,
                    modifier = Modifier.weight(1f)
                )
                ScnsMetricTile(
                    label = "Accepted",
                    value = m.acceptedCount.toString(),
                    accentColor = ScnsEmeraldDeep,
                    modifier = Modifier.weight(1f)
                )
                ScnsMetricTile(
                    label = "Dep R700 Unpaid",
                    value = m.depositUnpaidCount.toString(),
                    accentColor = ScnsAmber,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScnsMetricTile(
                    label = "Total Collected",
                    value = ScnsCalculators.formatZar(m.totalCollected),
                    accentColor = ScnsEmeraldDeep,
                    modifier = Modifier.weight(1f)
                )
                ScnsMetricTile(
                    label = "Outstanding",
                    value = ScnsCalculators.formatZar(m.totalOutstanding),
                    accentColor = if (m.totalOutstanding > 0) ScnsCrimson else ScnsEmeraldDeep,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Module Shortcuts
        ScnsSectionCard(
            title = "SCNS Quick Workspace",
            subtitle = "Jump directly into registration, attendance, payments, photo lists, or term exams"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickNavTile(
                        title = "Learner List",
                        subtitle = "${m.totalLearners} registered",
                        icon = Icons.Default.Groups,
                        color = ScnsNavy,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_nav_learners"
                    ) {
                        onSelectHubTab(LearnersHubTab.LEARNERS)
                        onNavigateRoute("learners")
                    }
                    QuickNavTile(
                        title = "Attendance",
                        subtitle = "Sat & Sun logs",
                        icon = Icons.Default.EventAvailable,
                        color = ScnsEmeraldDeep,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_nav_attendance"
                    ) {
                        onSelectHubTab(LearnersHubTab.ATTENDANCE)
                        onNavigateRoute("learners")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickNavTile(
                        title = "Payments",
                        subtitle = "${m.owingCount} owing",
                        icon = Icons.Default.Payments,
                        color = ScnsCrimson,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_nav_payments"
                    ) {
                        onSelectHubTab(LearnersHubTab.PAYMENTS)
                        onNavigateRoute("learners")
                    }
                    QuickNavTile(
                        title = "Upload Photo",
                        subtitle = "Pick & batch add",
                        icon = Icons.Default.PhotoCamera,
                        color = ScnsAmber,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_nav_upload"
                    ) {
                        onNavigateRoute("upload")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickNavTile(
                        title = "Exams / Reports",
                        subtitle = "4 Tests & 1-pg PDF",
                        icon = Icons.Default.Assessment,
                        color = ScnsNavy,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_nav_exams"
                    ) {
                        onNavigateRoute("exams")
                    }
                    QuickNavTile(
                        title = "Save & Export",
                        subtitle = "PDF, CSV & Backups",
                        icon = Icons.Default.CloudDone,
                        color = ScnsEmeraldDeep,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_nav_export"
                    ) {
                        onNavigateRoute("export")
                    }
                }
            }
        }

        // Direct Top PDF Downloads & WhatsApp Share
        ScnsSectionCard(
            title = "Instant PDF Downloads & WhatsApp Share",
            subtitle = "Saves directly to Downloads/SCNS_Records and opens Share Sheet"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onExportPdf(PdfExportScope.ALL, true) },
                    colors = ButtonDefaults.buttonColors(containerColor = ScnsEmeraldDeep),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("home_pdf_all_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Download & Share PDF — All Stored Info")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onExportPdf(PdfExportScope.LEARNERS, false) },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("home_pdf_learners_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Learners PDF", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onExportPdf(PdfExportScope.PAYMENTS, false) },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("home_pdf_payments_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Payments PDF", maxLines = 1)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }

    if (showAppDetails) {
        AppDetailsDialog(
            learnerCount = uiState.learners.size,
            executiveCount = uiState.executives.size,
            chairConfig = uiState.chairConfig,
            userEmail = userEmail,
            onDismiss = { showAppDetails = false }
        )
    }
}

@Composable
private fun QuickNavTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.testTag(testTag),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = color.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
