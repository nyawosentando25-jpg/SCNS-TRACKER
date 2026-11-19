package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.LearnerRecord
import com.example.data.model.ScnsCalculators
import com.example.data.model.ScnsConstants
import com.example.data.model.ScnsSubjectCatalog
import com.example.ui.components.ScnsDropdownSelector
import com.example.ui.components.SubjectFilterChips
import com.example.ui.theme.ScnsEmeraldDeep
import com.example.ui.theme.ScnsNavy
import com.example.ui.theme.ScnsNumericStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LearnerEditorDialog(
    initialLearner: LearnerRecord?,
    nextRegNoPreview: String,
    onDismiss: () -> Unit,
    onSave: (LearnerRecord) -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var names by remember(initialLearner) { mutableStateOf(initialLearner?.names.orEmpty()) }
    var surname by remember(initialLearner) { mutableStateOf(initialLearner?.surname.orEmpty()) }
    var grade by remember(initialLearner) { mutableStateOf(initialLearner?.grade?.ifBlank { "10" } ?: "10") }
    var school by remember(initialLearner) { mutableStateOf(initialLearner?.school.orEmpty()) }
    var sex by remember(initialLearner) { mutableStateOf(initialLearner?.sex.orEmpty()) }
    var race by remember(initialLearner) { mutableStateOf(initialLearner?.race.orEmpty()) }
    var dob by remember(initialLearner) { mutableStateOf(initialLearner?.dob.orEmpty()) }
    var age by remember(initialLearner) { mutableStateOf(initialLearner?.age.orEmpty()) }
    var admDate by remember(initialLearner) { mutableStateOf(initialLearner?.admDate?.ifBlank { today } ?: today) }
    var subjects by remember(initialLearner) { mutableStateOf(initialLearner?.subjects?.ifBlank { "Mathematics" } ?: "Mathematics") }
    var status by remember(initialLearner) { mutableStateOf(initialLearner?.status?.ifBlank { "Accepted" } ?: "Accepted") }
    var depPaid by remember(initialLearner) { mutableStateOf(initialLearner?.depPaid ?: false) }
    var depDate by remember(initialLearner) { mutableStateOf(initialLearner?.depDate.orEmpty()) }
    var parent by remember(initialLearner) { mutableStateOf(initialLearner?.parent.orEmpty()) }
    var docRef by remember(initialLearner) { mutableStateOf(initialLearner?.docRef.orEmpty()) }

    val feeDuePreview = remember(subjects) { ScnsCalculators.totalDue(subjects) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (initialLearner == null) "Add New Learner" else "Edit Learner",
                            style = MaterialTheme.typography.headlineSmall,
                            color = ScnsNavy
                        )
                        Text(
                            text = "Reg No: ${initialLearner?.regNo?.ifBlank { nextRegNoPreview } ?: nextRegNoPreview}",
                            style = ScnsNumericStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close editor")
                    }
                }

                HorizontalDivider()

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = names,
                        onValueChange = { names = it },
                        label = { Text("Names *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("learner_names_input")
                    )
                    OutlinedTextField(
                        value = surname,
                        onValueChange = { surname = it },
                        label = { Text("Surname *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("learner_surname_input")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScnsDropdownSelector(
                        label = "Grade *",
                        selectedValue = grade,
                        options = ScnsConstants.GRADES.map { it to "Grade $it" },
                        onSelected = { grade = it },
                        modifier = Modifier.weight(1f),
                        testTag = "learner_grade_dropdown"
                    )
                    ScnsDropdownSelector(
                        label = "Sex",
                        selectedValue = sex,
                        options = listOf("" to "Select", "Female" to "Female", "Male" to "Male", "Other" to "Other"),
                        onSelected = { sex = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = school,
                        onValueChange = { school = it },
                        label = { Text("School Attending") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it.filter(Char::isDigit).take(2) },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(0.6f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = race,
                        onValueChange = { race = it },
                        label = { Text("Race") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("DOB (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Tap Subjects (Annual Fee: ${ScnsCalculators.formatZar(feeDuePreview)})",
                    style = MaterialTheme.typography.labelLarge,
                    color = ScnsNavy
                )
                SubjectFilterChips(
                    selectedSubjectsCsv = subjects,
                    grade = grade,
                    onToggleSubject = { canonical ->
                        val current = ScnsCalculators.parseSubjectList(subjects)
                            .map { ScnsSubjectCatalog.normalizeSubject(it) }
                            .toMutableList()
                        val existingIdx = current.indexOfFirst { it.equals(canonical, ignoreCase = true) }
                        if (existingIdx >= 0) {
                            current.removeAt(existingIdx)
                        } else {
                            current.add(canonical)
                        }
                        subjects = current.joinToString(", ")
                    }
                )
                OutlinedTextField(
                    value = subjects,
                    onValueChange = { subjects = it },
                    label = { Text("Subjects (comma-separated)") },
                    modifier = Modifier.fillMaxWidth().testTag("learner_subjects_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScnsDropdownSelector(
                        label = "Status",
                        selectedValue = status,
                        options = ScnsConstants.STATUSES.map { it to it },
                        onSelected = { status = it },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = admDate,
                        onValueChange = { admDate = it },
                        label = { Text("Admission Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = depPaid,
                            onCheckedChange = { checked ->
                                depPaid = checked
                                if (checked && depDate.isBlank()) depDate = today
                            }
                        )
                        Text(
                            text = "Deposit R700 Paid",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        if (depPaid) {
                            OutlinedTextField(
                                value = depDate,
                                onValueChange = { depDate = it },
                                label = { Text("Dep Date") },
                                singleLine = true,
                                modifier = Modifier.width(132.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = parent,
                    onValueChange = { parent = it },
                    label = { Text("Parent / Guardian + Cellphone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = docRef,
                    onValueChange = { docRef = it },
                    label = { Text("Document Ref / Folder") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val record = (initialLearner ?: LearnerRecord()).copy(
                                regNo = initialLearner?.regNo?.ifBlank { nextRegNoPreview } ?: nextRegNoPreview,
                                names = names.trim(),
                                surname = surname.trim(),
                                grade = grade,
                                school = school.trim(),
                                sex = sex,
                                race = race.trim(),
                                dob = dob.trim(),
                                age = age.trim(),
                                admDate = admDate.trim(),
                                subjects = subjects.trim(),
                                status = status,
                                depPaid = depPaid,
                                depDate = if (depPaid) depDate.trim() else "",
                                parent = parent.trim(),
                                docRef = docRef.trim()
                            )
                            onSave(record)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ScnsEmeraldDeep),
                        modifier = Modifier.weight(1.3f).height(48.dp).testTag("save_learner_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Learner")
                    }
                }
            }
        }
    }
}
