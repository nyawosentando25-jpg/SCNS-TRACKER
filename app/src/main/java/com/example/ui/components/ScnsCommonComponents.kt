package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.ChairpersonConfig
import com.example.data.model.ScnsCalculators
import com.example.data.model.ScnsConstants
import com.example.data.model.ScnsSubjectCatalog
import com.example.ui.theme.ScnsCrimson
import com.example.ui.theme.ScnsEmeraldDeep
import com.example.ui.theme.ScnsGold
import com.example.ui.theme.ScnsNavy
import com.example.ui.theme.ScnsNumericStyle

@Composable
fun ScnsBrandCrest(
    size: Dp = 96.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.scns_logo),
        contentDescription = stringResource(id = R.string.cd_scns_logo),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White)
            .border(3.dp, ScnsGold, CircleShape)
    )
}

@Composable
fun ScnsMetricTile(
    label: String,
    value: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = ScnsNumericStyle.copy(
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    color = accentColor
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ScnsSectionCard(
    title: String,
    subtitle: String? = null,
    borderColor: Color? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        border = borderColor?.let { BorderStroke(2.dp, it) }
            ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScnsDropdownSelector(
    label: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = options.firstOrNull { it.first == selectedValue }?.second ?: selectedValue

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, title) ->
                DropdownMenuItem(
                    text = { Text(title) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScnsSafetyChipsRow(modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ScnsConstants.SAFETY_CHIPS.forEach { chip ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, ScnsGold)
            ) {
                Text(
                    text = chip,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ScnsNavy,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
fun DeleteConfirmDialog(
    learnerName: String,
    regNo: String,
    onConfirmYes: () -> Unit,
    onDismissNo: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissNo,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = ScnsCrimson,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Delete Learner?",
                color = ScnsCrimson,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Remove $learnerName ($regNo) permanently from SCNS Records?",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Tap YES to delete, or NO to keep. Nothing is deleted until you tap YES.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmYes,
                colors = ButtonDefaults.buttonColors(containerColor = ScnsCrimson),
                modifier = Modifier.testTag("confirm_delete_yes_button")
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("YES - DELETE")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismissNo,
                modifier = Modifier.testTag("confirm_delete_no_button")
            ) {
                Text("NO - KEEP")
            }
        }
    )
}

@Composable
fun AppDetailsDialog(
    learnerCount: Int,
    executiveCount: Int,
    chairConfig: ChairpersonConfig,
    userEmail: String?,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ScnsBrandCrest(size = 48.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = ScnsConstants.ORG_NAME,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = ScnsConstants.ORG_SUBTITLE,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close App Details")
                    }
                }

                HorizontalDivider()

                DetailRow("Creator", ScnsConstants.CREATOR_CREDIT)
                DetailRow("Venue", ScnsConstants.VENUE)
                DetailRow("Chairperson", "${chairConfig.chairName} (${chairConfig.chairCell})")
                DetailRow("Bank Reference", ScnsConstants.BANK_REF)
                if (!userEmail.isNullOrBlank()) {
                    DetailRow("Signed-In Account", userEmail)
                }
                DetailRow("Synced Learners", "$learnerCount records")
                DetailRow("Registered Executives", "$executiveCount / ${ScnsConstants.MAX_EXECUTIVES}")

                HorizontalDivider()

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = ScnsEmeraldDeep,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Storage & Permissions",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "• Cloud Sync: Encrypted Firebase Firestore scoped to your Google account.\n" +
                        "• Photos: Uses Android's zero-permission Photo Picker (never scans your gallery).\n" +
                        "• Exports: Generates native PDFs & CSVs directly into Downloads/SCNS_Records or Share Sheet (WhatsApp).\n" +
                        "• OTPs: Generated in-app for manual Chairperson sharing — zero SMS permission required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = ScnsNavy,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Tuition Fee Formula",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Annual fee = (Subjects × R2,400) + R100 registration, minus R500 bundle discount for 3 or more subjects. Deposit: R700.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_app_details_button")
                ) {
                    Text("Close Details")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectFilterChips(
    selectedSubjectsCsv: String,
    grade: String,
    onToggleSubject: (String) -> Unit
) {
    val available = remember(grade) { ScnsSubjectCatalog.subjectsForGrade(grade) }
    val selectedList = remember(selectedSubjectsCsv) {
        ScnsCalculators.parseSubjectList(selectedSubjectsCsv).map { ScnsSubjectCatalog.normalizeSubject(it) }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        available.forEach { subj ->
            val isSelected = selectedList.any { it.equals(subj.canonical, ignoreCase = true) }
            Surface(
                onClick = { onToggleSubject(subj.canonical) },
                shape = RoundedCornerShape(999.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            ) {
                Text(
                    text = subj.shortName,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}
