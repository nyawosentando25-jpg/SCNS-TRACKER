package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ScnsCalculators
import com.example.ui.components.ScnsSectionCard
import com.example.ui.theme.ScnsCrimson
import com.example.ui.theme.ScnsEmerald
import com.example.ui.theme.ScnsEmeraldDeep
import com.example.ui.theme.ScnsNavy
import com.example.ui.viewmodel.ScnsUiState
import com.example.util.CsvExportScope
import com.example.util.ExportedFileResult
import com.example.util.PdfExportScope

@Composable
fun ExportSaveScreen(
    uiState: ScnsUiState,
    onExportPdf: (PdfExportScope, Boolean) -> Unit,
    onExportCsv: (CsvExportScope, Boolean) -> Unit,
    onSaveManualNow: () -> Unit,
    onDeleteManualSave: (String) -> Unit,
    onOpenExportedFile: (ExportedFileResult) -> Unit,
    onShareExportedFile: (ExportedFileResult) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        uiState.lastExportedFile?.let { file ->
            Surface(
                shape = MaterialTheme.shapes.large,
                color = ScnsEmerald.copy(alpha = 0.12f),
                border = BorderStroke(1.5.dp, ScnsEmeraldDeep)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Latest Export Ready",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ScnsEmeraldDeep
                    )
                    Text(
                        text = file.displayLocation,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onOpenExportedFile(file) },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open")
                        }
                        Button(
                            onClick = { onShareExportedFile(file) },
                            colors = ButtonDefaults.buttonColors(containerColor = ScnsNavy),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share / WhatsApp")
                        }
                    }
                }
            }
        }

        ScnsSectionCard(
            title = "Printable SCNS PDF Reports",
            subtitle = "Saves to Downloads/SCNS_Records and lets you send immediately on WhatsApp"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    PdfExportScope.ALL to "Everything — All Stored Info (PDF)",
                    PdfExportScope.LEARNERS to "Learner List (PDF)",
                    PdfExportScope.ATTENDANCE to "Attendance Register (PDF)",
                    PdfExportScope.PAYMENTS to "Payment Tracker (PDF)",
                    PdfExportScope.EXAMS_RESULTS to "Exams Results Table (PDF)",
                    PdfExportScope.EXAMS_PROGRESS to "Exams Progress Summary (PDF)"
                ).forEach { (scope, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onExportPdf(scope, false) },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("export_pdf_${scope.name.lowercase()}")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(label, maxLines = 1, modifier = Modifier.weight(1f))
                        }
                        Button(
                            onClick = { onExportPdf(scope, true) },
                            colors = ButtonDefaults.buttonColors(containerColor = ScnsEmeraldDeep),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share $label", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        ScnsSectionCard(
            title = "Spreadsheet CSV Exports",
            subtitle = "Open in Excel, Google Sheets, or share on WhatsApp"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CsvExportScope.entries.forEach { scope ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onExportCsv(scope, false) },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("export_csv_${scope.name.lowercase()}")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(scope.label, maxLines = 1, modifier = Modifier.weight(1f))
                        }
                        Button(
                            onClick = { onExportCsv(scope, true) },
                            colors = ButtonDefaults.buttonColors(containerColor = ScnsNavy),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share ${scope.label}", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        ScnsSectionCard(
            title = "Cloud Manual Save Snapshots",
            subtitle = "All changes auto-sync to Cloud Firestore. Create a point-in-time snapshot anytime."
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSaveManualNow,
                    colors = ButtonDefaults.buttonColors(containerColor = ScnsEmeraldDeep),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("manual_save_now_button")
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Manual Save Snapshot Now")
                }

                if (uiState.manualSaves.isEmpty()) {
                    Text(
                        text = "No manual snapshots saved yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    HorizontalDivider()
                    uiState.manualSaves.forEach { snap ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = snap.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${snap.savedAtDisplay}  •  Collected: ${ScnsCalculators.formatZar(snap.totalCollected)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteManualSave(snap.id) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete snapshot",
                                        tint = ScnsCrimson
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
