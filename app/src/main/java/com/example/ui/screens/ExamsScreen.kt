package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ExamEntry
import com.example.data.model.LearnerRecord
import com.example.data.model.ScnsCalculators
import com.example.data.model.ScnsConstants
import com.example.data.model.ScnsSubjectCatalog
import com.example.ui.components.ScnsDropdownSelector
import com.example.ui.components.ScnsSectionCard
import com.example.ui.theme.ScnsAmberContainer
import com.example.ui.theme.ScnsCrimson
import com.example.ui.theme.ScnsEmerald
import com.example.ui.theme.ScnsEmeraldDeep
import com.example.ui.theme.ScnsGold
import com.example.ui.theme.ScnsNavy
import com.example.ui.theme.ScnsNumericStyle
import com.example.ui.viewmodel.ScnsUiState
import com.example.util.PdfExportScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExamsScreen(
    uiState: ScnsUiState,
    onSaveExamRow: (LearnerRecord, ExamEntry) -> Unit,
    onSaveAllExams: (List<Pair<LearnerRecord, ExamEntry>>) -> Unit,
    onQuickAddLearner: (String, String, String, String, String, String, String, String, String) -> Unit,
    onDownloadProgressReport: (LearnerRecord, String, String, Boolean) -> Unit,
    onDownloadExamsPdf: (PdfExportScope, String, String, String, Boolean) -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var examMode by rememberSaveable { mutableStateOf("class") }
    var selectedGrade by rememberSaveable { mutableStateOf("") }
    var selectedSubject by rememberSaveable { mutableStateOf("Mathematics") }
    var selectedTerm by rememberSaveable { mutableStateOf("Term 1") }
    var examDate by rememberSaveable { mutableStateOf(today) }
    var examLearnerId by rememberSaveable { mutableStateOf("") }

    // Inline quick add
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }
    var qNames by rememberSaveable { mutableStateOf("") }
    var qSurname by rememberSaveable { mutableStateOf("") }
    var qSchool by rememberSaveable { mutableStateOf("") }
    var qSex by rememberSaveable { mutableStateOf("") }
    var qAge by rememberSaveable { mutableStateOf("") }
    var qParent by rememberSaveable { mutableStateOf("") }

    // Progress Report selector
    var reportLearnerId by rememberSaveable { mutableStateOf("") }
    var reportSubject by rememberSaveable { mutableStateOf("Mathematics") }
    var reportTerm by rememberSaveable { mutableStateOf("Term 1") }

    // Registered marks overview filter
    var marksFilterGrade by rememberSaveable { mutableStateOf("") }
    var marksFilterSubject by rememberSaveable { mutableStateOf("") }
    var showRegisteredOverview by rememberSaveable { mutableStateOf(false) }

    val activeLearners = remember(
        uiState.learners,
        examMode,
        examLearnerId,
        selectedGrade,
        selectedSubject
    ) {
        if (examMode == "individual") {
            uiState.learners.filter { it.id == examLearnerId }
        } else {
            uiState.learners.filter { l ->
                (selectedGrade.isBlank() || l.grade == selectedGrade) &&
                    ScnsCalculators.learnerMatchesSubject(l, selectedSubject)
            }
        }
    }

    val draftMarks = remember(selectedSubject, selectedTerm, examDate, uiState.learners) {
        mutableStateMapOf<String, ExamEntry>().apply {
            val key = ScnsCalculators.examKey(selectedSubject, selectedTerm)
            uiState.learners.forEach { l ->
                val existing = l.exams[key] ?: ExamEntry(
                    subject = ScnsSubjectCatalog.normalizeSubject(selectedSubject),
                    term = selectedTerm,
                    date = examDate
                )
                put(l.id, existing)
            }
        }
    }

    val reportLearner = remember(uiState.learners, reportLearnerId) {
        uiState.learners.firstOrNull { it.id == reportLearnerId } ?: uiState.learners.firstOrNull()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Controls Card
        item {
            ScnsSectionCard(
                title = "Exams / Tests & Grade Lock",
                subtitle = "Marks out of 100 each • Total / 400 • % = Total ÷ 4"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("class" to "Class / Subject", "individual" to "Individual Learner")
                            .forEachIndexed { idx, (code, label) ->
                                SegmentedButton(
                                    selected = examMode == code,
                                    onClick = { examMode = code },
                                    shape = SegmentedButtonDefaults.itemShape(idx, 2)
                                ) {
                                    Text(label, maxLines = 1)
                                }
                            }
                    }

                    if (examMode == "individual") {
                        ScnsDropdownSelector(
                            label = "Choose Learner",
                            selectedValue = examLearnerId,
                            options = listOf("" to "Select a Learner") + uiState.learners.map {
                                it.id to "${it.regNo} | ${it.names} ${it.surname} (Gr ${it.grade})"
                            },
                            onSelected = { examLearnerId = it }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScnsDropdownSelector(
                            label = "Grade Lock",
                            selectedValue = selectedGrade,
                            options = listOf("" to "All Grades") + ScnsConstants.GRADES.map { it to "Grade $it" },
                            onSelected = { newGrade ->
                                selectedGrade = newGrade
                                if (newGrade.isNotBlank() && !ScnsSubjectCatalog.isSubjectValidForGrade(selectedSubject, newGrade)) {
                                    selectedSubject = ScnsSubjectCatalog.subjectsForGrade(newGrade).first().canonical
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ScnsDropdownSelector(
                            label = "Term",
                            selectedValue = selectedTerm,
                            options = ScnsConstants.TERMS.map { it to it },
                            onSelected = { selectedTerm = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScnsDropdownSelector(
                            label = "Subject Lock",
                            selectedValue = selectedSubject,
                            options = ScnsSubjectCatalog.subjectsForGrade(selectedGrade).map {
                                it.canonical to "${it.shortName} (${it.gradeLabel})"
                            },
                            onSelected = { newSubj ->
                                selectedSubject = newSubj
                                if (selectedGrade.isNotBlank() && !ScnsSubjectCatalog.isSubjectValidForGrade(newSubj, selectedGrade)) {
                                    selectedGrade = ScnsSubjectCatalog.defaultGradeForSubject(newSubj)
                                }
                            },
                            modifier = Modifier.weight(1.3f)
                        )
                        OutlinedTextField(
                            value = examDate,
                            onValueChange = { examDate = it },
                            label = { Text("Date") },
                            singleLine = true,
                            modifier = Modifier.weight(0.9f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val batch = activeLearners.map { l ->
                                    val entry = draftMarks[l.id] ?: ExamEntry(
                                        subject = selectedSubject,
                                        term = selectedTerm,
                                        date = examDate
                                    )
                                    l to entry.copy(date = examDate)
                                }
                                onSaveAllExams(batch)
                            },
                            enabled = activeLearners.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = ScnsEmeraldDeep),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("save_all_exams_button")
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save All (${activeLearners.size})", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                onDownloadExamsPdf(PdfExportScope.EXAMS_RESULTS, selectedSubject, selectedGrade, selectedTerm, true)
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Results PDF", maxLines = 1)
                        }
                    }
                }
            }
        }

        // 2. Inline Quick Add Learner Card
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = ScnsEmerald.copy(alpha = 0.08f),
                border = BorderStroke(1.5.dp, ScnsEmerald)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Add Learner Here (In Exams Tab)",
                                style = MaterialTheme.typography.titleSmall,
                                color = ScnsNavy,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Locks to Grade ${selectedGrade.ifBlank { ScnsSubjectCatalog.defaultGradeForSubject(selectedSubject) }} • $selectedSubject",
                                style = MaterialTheme.typography.labelSmall,
                                color = ScnsEmeraldDeep
                            )
                        }
                        OutlinedButton(onClick = { showQuickAdd = !showQuickAdd }) {
                            Icon(
                                if (showQuickAdd) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Quick Add"
                            )
                        }
                    }

                    if (showQuickAdd) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = qNames,
                                onValueChange = { qNames = it },
                                label = { Text("Names *") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = qSurname,
                                onValueChange = { qSurname = it },
                                label = { Text("Surname *") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = qSchool,
                                onValueChange = { qSchool = it },
                                label = { Text("School") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f)
                            )
                            ScnsDropdownSelector(
                                label = "Sex",
                                selectedValue = qSex,
                                options = listOf("" to "Any", "Female" to "Female", "Male" to "Male"),
                                onSelected = { qSex = it },
                                modifier = Modifier.weight(0.9f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = qAge,
                                onValueChange = { qAge = it.filter(Char::isDigit).take(2) },
                                label = { Text("Age") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(0.6f)
                            )
                            OutlinedTextField(
                                value = qParent,
                                onValueChange = { qParent = it },
                                label = { Text("Parent + Cell") },
                                singleLine = true,
                                modifier = Modifier.weight(1.4f)
                            )
                        }
                        Button(
                            onClick = {
                                onQuickAddLearner(
                                    qNames, qSurname, selectedGrade, selectedSubject,
                                    qSchool, qSex, qAge, qParent, today
                                )
                                qNames = ""; qSurname = ""; qSchool = ""; qAge = ""; qParent = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ScnsEmeraldDeep),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add This Learner to Exams")
                        }
                    }
                }
            }
        }

        // 3. Marks Editor Rows
        if (activeLearners.isEmpty()) {
            item {
                ScnsSectionCard(
                    title = "No Learners Match Current Lock",
                    subtitle = "Switch Grade/Subject filter above or expand 'Add Learner Here' to enroll a learner into $selectedSubject."
                ) {}
            }
        } else {
            items(activeLearners, key = { "exam_${it.id}" }) { learner ->
                val currentEntry = draftMarks[learner.id] ?: ExamEntry(subject = selectedSubject, term = selectedTerm, date = examDate)
                val stats = ScnsCalculators.computeExamStats(currentEntry)

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${learner.names} ${learner.surname}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${learner.regNo} • Gr ${learner.grade} • ${ScnsSubjectCatalog.shortLabel(selectedSubject)} ($selectedTerm)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = when {
                                    stats.percentage >= 70 -> ScnsEmeraldDeep.copy(alpha = 0.14f)
                                    stats.percentage >= 50 -> Color(0xFFFFF3CC)
                                    else -> ScnsCrimson.copy(alpha = 0.14f)
                                }
                            ) {
                                Text(
                                    text = "${stats.total}/400 (${stats.percentage}%)",
                                    style = ScnsNumericStyle.copy(
                                        fontSize = MaterialTheme.typography.labelLarge.fontSize,
                                        color = when {
                                            stats.percentage >= 70 -> ScnsEmeraldDeep
                                            stats.percentage >= 50 -> ScnsNavy
                                            else -> ScnsCrimson
                                        }
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "T1" to currentEntry.t1,
                                "T2" to currentEntry.t2,
                                "T3" to currentEntry.t3,
                                "T4" to currentEntry.t4
                            ).forEachIndexed { testIdx, (lbl, mark) ->
                                OutlinedTextField(
                                    value = if (mark == 0) "" else mark.toString(),
                                    onValueChange = { raw ->
                                        val num = raw.filter(Char::isDigit).take(3).toIntOrNull()?.coerceIn(0, 100) ?: 0
                                        val updated = when (testIdx) {
                                            0 -> currentEntry.copy(t1 = num)
                                            1 -> currentEntry.copy(t2 = num)
                                            2 -> currentEntry.copy(t3 = num)
                                            else -> currentEntry.copy(t4 = num)
                                        }
                                        draftMarks[learner.id] = updated
                                    },
                                    label = { Text(lbl) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stats.ratingLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    onSaveExamRow(learner, currentEntry.copy(date = examDate))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ScnsNavy)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Save Tests")
                            }
                        }
                    }
                }
            }
        }

        // 4. Registered Learners Marks Overview
        item {
            ScnsSectionCard(
                title = "Registered Learners Marks",
                subtitle = "Inspect saved marks across Grades & Subjects or download Registered Marks PDF"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScnsDropdownSelector(
                            label = "Grade",
                            selectedValue = marksFilterGrade,
                            options = listOf("" to "All Grades") + ScnsConstants.GRADES.map { it to "Gr $it" },
                            onSelected = { marksFilterGrade = it },
                            modifier = Modifier.weight(1f)
                        )
                        ScnsDropdownSelector(
                            label = "Subject",
                            selectedValue = marksFilterSubject,
                            options = listOf("" to "All Subjects") + ScnsSubjectCatalog.ALL_SUBJECTS.map { it.canonical to it.shortName },
                            onSelected = { marksFilterSubject = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showRegisteredOverview = !showRegisteredOverview },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text(if (showRegisteredOverview) "Hide Marks" else "Show Marks", maxLines = 1)
                        }
                        Button(
                            onClick = {
                                onDownloadExamsPdf(PdfExportScope.REGISTERED_MARKS, marksFilterSubject, marksFilterGrade, selectedTerm, true)
                            },
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Marks PDF", maxLines = 1)
                        }
                    }

                    if (showRegisteredOverview) {
                        HorizontalDivider()
                        val rows = uiState.learners.filter { l ->
                            (marksFilterGrade.isBlank() || l.grade == marksFilterGrade) &&
                                (marksFilterSubject.isBlank() || ScnsCalculators.learnerMatchesSubject(l, marksFilterSubject))
                        }
                        rows.forEach { l ->
                            l.exams.values.filter { e ->
                                marksFilterSubject.isBlank() || ScnsSubjectCatalog.normalizeSubject(e.subject)
                                    .equals(ScnsSubjectCatalog.normalizeSubject(marksFilterSubject), true)
                            }.forEach { e ->
                                val st = ScnsCalculators.computeExamStats(e)
                                Text(
                                    text = "${l.regNo} ${l.names} ${l.surname} (Gr ${l.grade}) • ${ScnsSubjectCatalog.shortLabel(e.subject)} ${e.term}: ${e.t1}/${e.t2}/${e.t3}/${e.t4} = ${st.total}/400 (${st.percentage}%)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. One-Page Individual Progress Report PDF Card
        item {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = ScnsAmberContainer,
                border = BorderStroke(2.dp, ScnsGold)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "One-Page Individual School Progress Report",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ScnsNavy
                    )
                    Text(
                        text = "Official SCNS 1-page printable report with 4 test scores, % rating, tutor/parent comment lines & Office Authorisation.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    ScnsDropdownSelector(
                        label = "Choose Learner",
                        selectedValue = reportLearner?.id.orEmpty(),
                        options = uiState.learners.map {
                            it.id to "${it.regNo} | ${it.names} ${it.surname} (Gr ${it.grade})"
                        }.ifEmpty { listOf("" to "No learners added yet") },
                        onSelected = { id ->
                            reportLearnerId = id
                            val chosen = uiState.learners.firstOrNull { it.id == id }
                            if (chosen != null) {
                                reportSubject = ScnsCalculators.primarySubjectOf(chosen)
                            }
                        }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScnsDropdownSelector(
                            label = "Subject",
                            selectedValue = reportSubject,
                            options = ScnsSubjectCatalog.ALL_SUBJECTS.map { it.canonical to it.shortName },
                            onSelected = { reportSubject = it },
                            modifier = Modifier.weight(1.2f)
                        )
                        ScnsDropdownSelector(
                            label = "Term",
                            selectedValue = reportTerm,
                            options = ScnsConstants.TERMS.map { it to it },
                            onSelected = { reportTerm = it },
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                    Button(
                        onClick = {
                            if (reportLearner != null) {
                                onDownloadProgressReport(reportLearner, reportSubject, reportTerm, true)
                            }
                        },
                        enabled = reportLearner != null,
                        colors = ButtonDefaults.buttonColors(containerColor = ScnsEmeraldDeep),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("download_progress_report_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Download & Share 1-Page Report PDF")
                    }
                }
            }
        }
    }
}
