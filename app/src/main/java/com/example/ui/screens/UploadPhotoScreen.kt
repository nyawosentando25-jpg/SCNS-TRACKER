package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.ScnsCalculators
import com.example.data.model.ScnsConstants
import com.example.data.model.ScnsSubjectCatalog
import com.example.ui.components.ScnsDropdownSelector
import com.example.ui.components.ScnsSectionCard
import com.example.ui.theme.ScnsEmerald
import com.example.ui.theme.ScnsEmeraldDeep
import com.example.ui.theme.ScnsNavy
import com.example.ui.viewmodel.ScnsUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UploadPhotoScreen(
    uiState: ScnsUiState,
    onPhotoPicked: (Uri) -> Unit,
    onBatchImport: (String, String, String, String, String, String, Boolean, Boolean) -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var selectedPhotoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var grade by rememberSaveable { mutableStateOf("9") }
    var subject by rememberSaveable { mutableStateOf("Mathematics") }
    var school by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(today) }
    var day by rememberSaveable { mutableStateOf("Saturday") }
    var alsoAddAtt by rememberSaveable { mutableStateOf(true) }
    var alsoAddPay by rememberSaveable { mutableStateOf(true) }
    var rawText by rememberSaveable { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri.toString()
            onPhotoPicked(uri)
        }
    }

    val parsedPreview = remember(rawText) { ScnsCalculators.parseBatchLines(rawText) }
    val previewPhoto = selectedPhotoUri ?: uiState.photoHistory.firstOrNull()?.uriString

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScnsSectionCard(
            title = "Upload Register Photo & Batch List",
            subtitle = "Zero-permission Photo Picker saves a local reference while you paste or type learner names",
            borderColor = ScnsEmerald
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("pick_register_photo_button")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pick Register Photo from Gallery")
                }

                if (!previewPhoto.isNullOrBlank()) {
                    AsyncImage(
                        model = previewPhoto,
                        contentDescription = "Selected class register photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                }

                if (uiState.photoHistory.isNotEmpty()) {
                    Text(
                        text = "Recent Saved Register Photos (${uiState.photoHistory.size}/10)",
                        style = MaterialTheme.typography.labelMedium,
                        color = ScnsNavy
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(uiState.photoHistory, key = { it.id }) { item ->
                            Surface(
                                onClick = { selectedPhotoUri = item.uriString },
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(
                                    width = if (previewPhoto == item.uriString) 2.dp else 1.dp,
                                    color = if (previewPhoto == item.uriString) ScnsEmeraldDeep else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                AsyncImage(
                                    model = item.uriString,
                                    contentDescription = item.label,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScnsDropdownSelector(
                        label = "Grade",
                        selectedValue = grade,
                        options = ScnsConstants.GRADES.map { it to "Grade $it" },
                        onSelected = { grade = it },
                        modifier = Modifier.weight(0.9f)
                    )
                    ScnsDropdownSelector(
                        label = "Subject",
                        selectedValue = subject,
                        options = ScnsSubjectCatalog.ALL_SUBJECTS.map { it.canonical to it.shortName },
                        onSelected = { subject = it },
                        modifier = Modifier.weight(1.1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = school,
                        onValueChange = { school = it },
                        label = { Text("School (optional)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    ScnsDropdownSelector(
                        label = "Day",
                        selectedValue = day,
                        options = ScnsConstants.DAYS.map { it to it },
                        onSelected = { day = it },
                        modifier = Modifier.weight(0.9f)
                    )
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Admission / Attendance Date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(checked = alsoAddAtt, onCheckedChange = { alsoAddAtt = it })
                        Text("Add Attendance", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(checked = alsoAddPay, onCheckedChange = { alsoAddPay = it })
                        Text("Init Payments", style = MaterialTheme.typography.bodySmall)
                    }
                }

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Learner Names (one per line: Surname Names)") },
                    placeholder = { Text("1. Ndlovu Sipho\n2. Mkhize Thando") },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("batch_names_input")
                )

                if (parsedPreview.isNotEmpty()) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Preview: ${parsedPreview.size} learner(s) ready for Grade $grade • $subject",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ScnsNavy
                            )
                            val sample = parsedPreview.take(4).joinToString("\n") { (s, n) ->
                                "• Surname: $s  |  Names: $n"
                            }
                            Text(
                                text = sample,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        onBatchImport(rawText, grade, subject, school, date, day, alsoAddAtt, alsoAddPay)
                        rawText = ""
                    },
                    enabled = parsedPreview.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = ScnsEmeraldDeep),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("batch_import_button")
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add ${parsedPreview.size} Learners to Records")
                }
            }
        }
    }
}
