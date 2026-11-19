package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceEntry
import com.example.data.model.LearnerRecord
import com.example.data.model.ScnsCalculators
import com.example.data.model.ScnsConstants
import com.example.data.model.ScnsSubjectCatalog
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.ScnsDropdownSelector
import com.example.ui.components.ScnsMetricTile
import com.example.ui.components.ScnsSectionCard
import com.example.ui.theme.ScnsAmber
import com.example.ui.theme.ScnsCrimson
import com.example.ui.theme.ScnsEmeraldDeep
import com.example.ui.theme.ScnsNavy
import com.example.ui.theme.ScnsNumericStyle
import com.example.ui.viewmodel.LearnersHubTab
import com.example.ui.viewmodel.ScnsUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LearnersHubScreen(
    uiState: ScnsUiState,
    onSetHubTab: (LearnersHubTab) -> Unit,
    onUpdateSearch: (String) -> Unit,
    onUpdateFilters: (String, String, String, String, String) -> Unit,
    onClearFilters: () -> Unit,
    onSaveLearner: (LearnerRecord, () -> Unit) -> Unit,
    onDeleteLearner: (LearnerRecord) -> Unit,
    onAddSingleAttendance: (LearnerRecord, String, String, String, String) -> Unit,
    onBulkAttendance: (Boolean, String, String, String, String, String) -> Unit,
    onClearAttendanceScope: (String, String, String, String) -> Unit,
    onClearRepeatedAttendance: () -> Unit,
    onExportFilteredAttendancePdf: (String, String, List<Pair<LearnerRecord, AttendanceEntry>>, Boolean) -> Unit,
    onAddPayment: (LearnerRecord, String, String, String, String, Long, String) -> Unit,
    onExportIndividualPdf: (LearnerRecord, Boolean, Boolean) -> Unit,
    onExportAllPaymentsPdf: (Boolean) -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var showFiltersPanel by rememberSaveable { mutableStateOf(false) }
    var editingLearner by remember { mutableStateOf<LearnerRecord?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingDeleteLearner by remember { mutableStateOf<LearnerRecord?>(null) }

    // Attendance state
    var attDate by rememberSaveable { mutableStateOf(today) }
    var attDay by rememberSaveable { mutableStateOf("Saturday") }
    var attStatus by rememberSaveable { mutableStateOf("Present") }
    var attSubject by rememberSaveable { mutableStateOf("Mathematics") }
    var attViewMode by rememberSaveable { mutableStateOf("all") }
    var attClearDate by rememberSaveable { mutableStateOf("") }

    // Payment quick form state
    var paymentLearner by remember { mutableStateOf<LearnerRecord?>(null) }
    var payType by rememberSaveable { mutableStateOf("Monthly") }
    var payMonth by rememberSaveable { mutableStateOf(ScnsConstants.MONTHS_2027.first()) }
    var payTerm by rememberSaveable { mutableStateOf(ScnsConstants.TERMS.first()) }
    var payAmount by rememberSaveable { mutableStateOf("250") }
    var payDate by rememberSaveable { mutableStateOf(today) }
    var payNote by rememberSaveable { mutableStateOf("") }

    val filteredLearners = remember(
        uiState.learners,
        uiState.searchQuery,
        uiState.statusFilter,
        uiState.gradeFilter,
        uiState.subjectFilter,
        uiState.individualLearnerId
    ) {
        val q = uiState.searchQuery.trim().lowercase()
        uiState.learners.filter { l ->
            val matchesQuery = q.isEmpty() ||
                l.regNo.lowercase().contains(q) ||
                l.names.lowercase().contains(q) ||
                l.surname.lowercase().contains(q) ||
                l.grade.lowercase().contains(q) ||
                l.subjects.lowercase().contains(q)
            val matchesStatus = when (uiState.statusFilter) {
                "All" -> true
                "Owing" -> ScnsCalculators.isOwing(l)
                "Deposit Not Paid" -> !l.depPaid
                else -> l.status.equals(uiState.statusFilter, ignoreCase = true)
            }
            val matchesGrade = uiState.gradeFilter.isBlank() || l.grade == uiState.gradeFilter
            val matchesSubj = ScnsCalculators.learnerMatchesSubject(l, uiState.subjectFilter)
            val matchesInd = uiState.individualLearnerId.isBlank() || l.id == uiState.individualLearnerId
            matchesQuery && matchesStatus && matchesGrade && matchesSubj && matchesInd
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Segmented Tabs + Search + Filter Toggle
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        LearnersHubTab.entries.forEachIndexed { idx, tab ->
                            SegmentedButton(
                                selected = uiState.hubTab == tab,
                                onClick = { onSetHubTab(tab) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = idx,
                                    count = LearnersHubTab.entries.size
                                ),
                                modifier = Modifier.testTag("hub_tab_${tab.name.lowercase()}")
                            ) {
                                Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onUpdateSearch,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            label = { Text("Search Reg No / name / grade") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("learners_search_input")
                        )
                        OutlinedButton(
                            onClick = { showFiltersPanel = !showFiltersPanel },
                            modifier = Modifier.height(54.dp).testTag("toggle_filters_button")
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Toggle Filters")
                        }
                    }

                    // Quick Status Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Accepted", "Waiting List", "Rejected", "Owing", "Deposit Not Paid")
                            .forEach { st ->
                                FilterChip(
                                    selected = uiState.statusFilter == st,
                                    onClick = {
                                        onUpdateFilters(
                                            st,
                                            uiState.gradeFilter,
                                            uiState.subjectFilter,
                                            uiState.dayFilter,
                                            uiState.individualLearnerId
                                        )
                                    },
                                    label = { Text(st) }
                                )
                            }
                    }

                    if (showFiltersPanel) {
                        ScnsSectionCard(
                            title = "Top Controls (Grade • Subject • Individual)",
                            borderColor = ScnsCrimson
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ScnsDropdownSelector(
                                        label = "Grade",
                                        selectedValue = uiState.gradeFilter,
                                        options = listOf("" to "All Grades") + ScnsConstants.GRADES.map { it to "Grade $it" },
                                        onSelected = {
                                            onUpdateFilters(uiState.statusFilter, it, uiState.subjectFilter, uiState.dayFilter, uiState.individualLearnerId)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    ScnsDropdownSelector(
                                        label = "Subject",
                                        selectedValue = uiState.subjectFilter,
                                        options = listOf("" to "All Subjects") + ScnsSubjectCatalog.ALL_SUBJECTS.map { it.canonical to it.shortName },
                                        onSelected = {
                                            onUpdateFilters(uiState.statusFilter, uiState.gradeFilter, it, uiState.dayFilter, uiState.individualLearnerId)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ScnsDropdownSelector(
                                        label = "Individual Learner",
                                        selectedValue = uiState.individualLearnerId,
                                        options = listOf("" to "All Learners") + uiState.learners.map {
                                            it.id to "${it.regNo} | ${it.names} ${it.surname}"
                                        },
                                        onSelected = {
                                            onUpdateFilters(uiState.statusFilter, uiState.gradeFilter, uiState.subjectFilter, uiState.dayFilter, it)
                                        },
                                        modifier = Modifier.weight(1.4f)
                                    )
                                    ScnsDropdownSelector(
                                        label = "Day",
                                        selectedValue = uiState.dayFilter,
                                        options = listOf("" to "Sat + Sun") + ScnsConstants.DAYS.map { it to it },
                                        onSelected = {
                                            onUpdateFilters(uiState.statusFilter, uiState.gradeFilter, uiState.subjectFilter, it, uiState.individualLearnerId)
                                        },
                                        modifier = Modifier.weight(0.9f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = onClearFilters,
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("Clear All Filters")
                                    }
                                    val indLearner = uiState.learners.firstOrNull { it.id == uiState.individualLearnerId }
                                    if (indLearner != null) {
                                        Button(
                                            onClick = { onExportIndividualPdf(indLearner, false, true) },
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        ) {
                                            Text("Learner PDF", maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Active Sub-Tab Body
            when (uiState.hubTab) {
                LearnersHubTab.LEARNERS -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Showing ${filteredLearners.size} of ${uiState.learners.size} learners",
                                style = MaterialTheme.typography.titleSmall,
                                color = ScnsNavy
                            )
                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ScnsEmeraldDeep),
                                modifier = Modifier.testTag("add_learner_top_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Learner")
                            }
                        }
                    }

                    if (filteredLearners.isEmpty()) {
                        item {
                            ScnsSectionCard(
                                title = "No Learners Found",
                                subtitle = if (uiState.learners.isEmpty()) {
                                    "Add your first learner or import a batch list from Upload Photo."
                                } else {
                                    "Try clearing search or filter chips above."
                                }
                            ) {
                                Button(
                                    onClick = {
                                        if (uiState.learners.isEmpty()) showCreateDialog = true else onClearFilters()
                                    }
                                ) {
                                    Text(if (uiState.learners.isEmpty()) "+ Add First Learner" else "Reset Filters")
                                }
                            }
                        }
                    } else {
                        items(filteredLearners, key = { it.id }) { learner ->
                            LearnerItemCard(
                                learner = learner,
                                onEdit = { editingLearner = learner },
                                onDeleteRequest = { pendingDeleteLearner = learner },
                                onExportPdf = { onExportIndividualPdf(learner, false, true) }
                            )
                        }
                    }
                }

                LearnersHubTab.ATTENDANCE -> {
                    item {
                        ScnsSectionCard(
                            title = "Attendance Register Controls",
                            subtitle = "Mark Saturday / Sunday per Subject, bulk mark Grade/Subject, or deduplicate repeats"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = attDate,
                                        onValueChange = { attDate = it },
                                        label = { Text("Date") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ScnsDropdownSelector(
                                        label = "Day",
                                        selectedValue = attDay,
                                        options = ScnsConstants.DAYS.map { it to it },
                                        onSelected = { attDay = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ScnsDropdownSelector(
                                        label = "Subject",
                                        selectedValue = attSubject,
                                        options = ScnsSubjectCatalog.ALL_SUBJECTS.map { it.canonical to it.shortName },
                                        onSelected = { attSubject = it },
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    ScnsDropdownSelector(
                                        label = "Mark As",
                                        selectedValue = attStatus,
                                        options = listOf("Present" to "Present", "Absent" to "Absent"),
                                        onSelected = { attStatus = it },
                                        modifier = Modifier.weight(0.9f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            onBulkAttendance(
                                                true,
                                                uiState.gradeFilter,
                                                attSubject,
                                                attDate,
                                                attDay,
                                                attStatus
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ScnsEmeraldDeep),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("+ Bulk Grade", maxLines = 1)
                                    }
                                    Button(
                                        onClick = {
                                            onBulkAttendance(
                                                false,
                                                uiState.gradeFilter,
                                                attSubject,
                                                attDate,
                                                attDay,
                                                attStatus
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ScnsNavy),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("+ Bulk Subject", maxLines = 1)
                                    }
                                }

                                HorizontalDivider()

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ScnsDropdownSelector(
                                        label = "Register View",
                                        selectedValue = attViewMode,
                                        options = listOf(
                                            "all" to "All Attendance",
                                            "individual" to "Individual Learner",
                                            "subject" to "Per Subject",
                                            "grade" to "Per Grade"
                                        ),
                                        onSelected = { attViewMode = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = attClearDate,
                                        onValueChange = { attClearDate = it },
                                        label = { Text("Clear Date (blank=All)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                val registerEntries = remember(
                                    filteredLearners,
                                    attViewMode,
                                    uiState.dayFilter,
                                    uiState.subjectFilter,
                                    attSubject
                                ) {
                                    buildList {
                                        for (l in filteredLearners) {
                                            val fallback = ScnsCalculators.primarySubjectOf(l)
                                            for (a in l.att) {
                                                val dayOk = uiState.dayFilter.isBlank() || a.day.equals(uiState.dayFilter, true)
                                                val normSubj = ScnsSubjectCatalog.normalizeSubject(a.subject.ifBlank { fallback })
                                                val subjOk = when {
                                                    uiState.subjectFilter.isNotBlank() ->
                                                        normSubj.equals(ScnsSubjectCatalog.normalizeSubject(uiState.subjectFilter), true)
                                                    attViewMode == "subject" ->
                                                        normSubj.equals(ScnsSubjectCatalog.normalizeSubject(attSubject), true)
                                                    else -> true
                                                }
                                                if (dayOk && subjOk) add(l to a)
                                            }
                                        }
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = onClearRepeatedAttendance,
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("clear_repeated_att_button")
                                    ) {
                                        Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Clear Repeats", maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onClearAttendanceScope(
                                                uiState.individualLearnerId,
                                                uiState.gradeFilter,
                                                if (attViewMode == "subject") attSubject else uiState.subjectFilter,
                                                attClearDate
                                            )
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ScnsCrimson),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("Clear Scope", maxLines = 1)
                                    }
                                }

                                Button(
                                    onClick = {
                                        onExportFilteredAttendancePdf(
                                            "SCNS Attendance (${attViewMode.uppercase()})",
                                            "${registerEntries.size} entries • Day: ${uiState.dayFilter.ifBlank { "Sat + Sun" }}",
                                            registerEntries,
                                            true
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Attendance PDF (${registerEntries.size} rows)")
                                }
                            }
                        }
                    }

                    if (filteredLearners.isEmpty()) {
                        item {
                            ScnsSectionCard(
                                title = if (uiState.learners.isEmpty()) "No Learners Yet" else "No Matching Learners",
                                subtitle = if (uiState.learners.isEmpty()) {
                                    "Add learners first to start marking Saturday & Sunday attendance."
                                } else {
                                    "Clear search or filter chips above to view attendance learners."
                                }
                            ) {
                                Button(
                                    onClick = {
                                        if (uiState.learners.isEmpty()) showCreateDialog = true else onClearFilters()
                                    }
                                ) {
                                    Text(if (uiState.learners.isEmpty()) "+ Add First Learner" else "Reset Filters")
                                }
                            }
                        }
                    } else {
                        items(filteredLearners, key = { "att_${it.id}" }) { learner ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                                text = "${learner.regNo} • Gr ${learner.grade} • ${learner.att.size} logs",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = {
                                                    onAddSingleAttendance(learner, attDate, attDay, "Present", attSubject)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ScnsEmeraldDeep),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("Present")
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    onAddSingleAttendance(learner, attDate, attDay, "Absent", attSubject)
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ScnsCrimson),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("Absent")
                                            }
                                        }
                                    }
                                    if (learner.att.isNotEmpty()) {
                                        val latest = learner.att.takeLast(3).reversed().joinToString("  •  ") {
                                            "${it.d} (${it.day.take(3)}) ${ScnsSubjectCatalog.shortLabel(it.subject)}: ${it.s}"
                                        }
                                        Text(
                                            text = latest,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                LearnersHubTab.PAYMENTS -> {
                    item {
                        val filteredCollected = remember(filteredLearners) {
                            filteredLearners.sumOf { ScnsCalculators.paidTotal(it) }
                        }
                        val filteredOutstanding = remember(filteredLearners) {
                            filteredLearners.sumOf { ScnsCalculators.outstanding(it) }
                        }
                        val filteredOwingCount = remember(filteredLearners) {
                            filteredLearners.count { ScnsCalculators.isOwing(it) }
                        }
                        val filteredDepUnpaid = remember(filteredLearners) {
                            filteredLearners.count { !it.depPaid }
                        }

                        ScnsSectionCard(
                            title = "Payments Tracker & Fee Controls",
                            subtitle = "Showing ${filteredLearners.size} learner(s) • ${ScnsConstants.BANK_REF}",
                            borderColor = ScnsNavy
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ScnsMetricTile(
                                        label = "Collected",
                                        value = ScnsCalculators.formatZar(filteredCollected),
                                        accentColor = ScnsEmeraldDeep,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ScnsMetricTile(
                                        label = "Outstanding",
                                        value = ScnsCalculators.formatZar(filteredOutstanding),
                                        accentColor = if (filteredOutstanding > 0L) ScnsCrimson else ScnsEmeraldDeep,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ScnsMetricTile(
                                        label = "Learners Owing",
                                        value = filteredOwingCount.toString(),
                                        accentColor = ScnsCrimson,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ScnsMetricTile(
                                        label = "Dep R700 Unpaid",
                                        value = filteredDepUnpaid.toString(),
                                        accentColor = ScnsAmber,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(
                                    text = "Fee Formula: R2,400/subject + R100 registration (−R500 bundle discount for 3+ subjects) • Deposit R700.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val candidate = paymentLearner
                                                ?: filteredLearners.firstOrNull()
                                                ?: uiState.learners.firstOrNull()
                                            if (candidate != null) {
                                                paymentLearner = candidate
                                            } else {
                                                showCreateDialog = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ScnsEmeraldDeep),
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("quick_record_payment_button")
                                    ) {
                                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (uiState.learners.isEmpty()) "+ Add Learner" else "+ Record Payment",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onExportAllPaymentsPdf(true) },
                                        enabled = uiState.learners.isNotEmpty(),
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("export_all_payments_pdf_button")
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Payments PDF", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }

                    if (paymentLearner != null) {
                        item {
                            val target = uiState.learners.firstOrNull { it.id == paymentLearner!!.id } ?: paymentLearner!!
                            ScnsSectionCard(
                                title = "Record Payment — ${target.names} ${target.surname}",
                                subtitle = "${target.regNo} • Owing: ${ScnsCalculators.formatZar(ScnsCalculators.outstanding(target))}",
                                borderColor = ScnsEmeraldDeep
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (uiState.learners.size > 1) {
                                        ScnsDropdownSelector(
                                            label = "Switch Learner",
                                            selectedValue = target.id,
                                            options = uiState.learners.map {
                                                it.id to "${it.regNo} | ${it.names} ${it.surname}"
                                            },
                                            onSelected = { pickedId ->
                                                uiState.learners.firstOrNull { it.id == pickedId }?.let {
                                                    paymentLearner = it
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        ScnsDropdownSelector(
                                            label = "Payment Type",
                                            selectedValue = payType,
                                            options = listOf("Monthly" to "Monthly", "Termly" to "Termly"),
                                            onSelected = { payType = it },
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (payType == "Monthly") {
                                            ScnsDropdownSelector(
                                                label = "Month (2027)",
                                                selectedValue = payMonth,
                                                options = ScnsConstants.MONTHS_2027.map { it to it },
                                                onSelected = { payMonth = it },
                                                modifier = Modifier.weight(1f)
                                            )
                                        } else {
                                            ScnsDropdownSelector(
                                                label = "Term",
                                                selectedValue = payTerm,
                                                options = ScnsConstants.TERMS.map { it to it },
                                                onSelected = { payTerm = it },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = payAmount,
                                            onValueChange = { payAmount = it.filter(Char::isDigit) },
                                            label = { Text("Amount (ZAR)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f).testTag("payment_amount_input")
                                        )
                                        OutlinedTextField(
                                            value = payDate,
                                            onValueChange = { payDate = it },
                                            label = { Text("Date") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    OutlinedTextField(
                                        value = payNote,
                                        onValueChange = { payNote = it },
                                        label = { Text("Receipt / Note (optional)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { paymentLearner = null },
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        ) {
                                            Text("Close")
                                        }
                                        Button(
                                            onClick = {
                                                val amt = payAmount.toLongOrNull() ?: 0L
                                                onAddPayment(target, payDate, payType, payMonth, payTerm, amt, payNote)
                                                paymentLearner = null
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ScnsEmeraldDeep),
                                            modifier = Modifier.weight(1.3f).height(48.dp).testTag("save_payment_button")
                                        ) {
                                            Text("Save Payment")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (filteredLearners.isEmpty()) {
                        item {
                            ScnsSectionCard(
                                title = if (uiState.learners.isEmpty()) "No Learners Yet" else "No Matching Payment Records",
                                subtitle = if (uiState.learners.isEmpty()) {
                                    "Add your first learner to track deposits (R700), monthly/termly instalments, and balances."
                                } else {
                                    "No learners match the current search or filter chips above."
                                }
                            ) {
                                Button(
                                    onClick = {
                                        if (uiState.learners.isEmpty()) showCreateDialog = true else onClearFilters()
                                    }
                                ) {
                                    Text(if (uiState.learners.isEmpty()) "+ Add First Learner" else "Reset Filters")
                                }
                            }
                        }
                    } else {
                        items(filteredLearners, key = { "pay_${it.id}" }) { learner ->
                            val due = ScnsCalculators.totalDue(learner.subjects)
                            val mPaid = ScnsCalculators.monthlyPaid(learner)
                            val tPaid = ScnsCalculators.termlyPaid(learner)
                            val totalPaid = ScnsCalculators.paidTotal(learner)
                            val bal = (due - totalPaid).coerceAtLeast(0L)

                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                                text = "${learner.regNo} • Gr ${learner.grade} • Deposit: ${if (learner.depPaid) "Paid" else "Unpaid"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = if (bal == 0L) "SETTLED" else "Owing R$bal",
                                            style = ScnsNumericStyle.copy(
                                                color = if (bal == 0L) ScnsEmeraldDeep else ScnsCrimson
                                            )
                                        )
                                    }
                                    Text(
                                        text = "Monthly: R$mPaid  |  Termly: R$tPaid  |  Paid: R$totalPaid / Due: R$due",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ScnsNavy
                                    )
                                    if (learner.pay.isNotEmpty()) {
                                        val recentPayments = learner.pay.takeLast(3).reversed().joinToString("  •  ") { p ->
                                            val period = if (p.t.equals("Monthly", ignoreCase = true)) {
                                                p.month.ifBlank { "Monthly" }
                                            } else {
                                                p.term.ifBlank { "Termly" }
                                            }
                                            "${p.d} ($period): R${p.amt}"
                                        }
                                        Text(
                                            text = "Recent: $recentPayments",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { paymentLearner = learner },
                                            colors = ButtonDefaults.buttonColors(containerColor = ScnsEmeraldDeep),
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        ) {
                                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("+ Pay")
                                        }
                                        OutlinedButton(
                                            onClick = { onExportIndividualPdf(learner, true, true) },
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Payment PDF")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }

        if (uiState.hubTab == LearnersHubTab.LEARNERS) {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = ScnsNavy,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("fab_add_learner")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Learner")
            }
        }
    }

    if (showCreateDialog || editingLearner != null) {
        LearnerEditorDialog(
            initialLearner = editingLearner,
            nextRegNoPreview = ScnsCalculators.nextRegNo(uiState.learners),
            onDismiss = {
                showCreateDialog = false
                editingLearner = null
            },
            onSave = { updated ->
                onSaveLearner(updated) {
                    showCreateDialog = false
                    editingLearner = null
                }
            }
        )
    }

    pendingDeleteLearner?.let { victim ->
        DeleteConfirmDialog(
            learnerName = "${victim.names} ${victim.surname}",
            regNo = victim.regNo,
            onConfirmYes = {
                onDeleteLearner(victim)
                pendingDeleteLearner = null
            },
            onDismissNo = { pendingDeleteLearner = null }
        )
    }
}

@Composable
private fun LearnerItemCard(
    learner: LearnerRecord,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
    onExportPdf: () -> Unit
) {
    val due = ScnsCalculators.totalDue(learner.subjects)
    val paid = ScnsCalculators.paidTotal(learner)
    val bal = (due - paid).coerceAtLeast(0L)

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${learner.names} ${learner.surname}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${learner.regNo}  •  Grade ${learner.grade}  •  ${learner.school.ifBlank { "School N/A" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (learner.status) {
                        "Accepted" -> ScnsEmeraldDeep.copy(alpha = 0.14f)
                        "Rejected" -> ScnsCrimson.copy(alpha = 0.14f)
                        else -> ScnsAmber.copy(alpha = 0.18f)
                    }
                ) {
                    Text(
                        text = learner.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (learner.status) {
                            "Accepted" -> ScnsEmeraldDeep
                            "Rejected" -> ScnsCrimson
                            else -> Color(0xFF9E6000)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Subjects: ${learner.subjects.ifBlank { "None selected" }}",
                style = MaterialTheme.typography.bodySmall,
                color = ScnsNavy
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Dep R700: ${if (learner.depPaid) "Yes" else "No"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (learner.depPaid) ScnsEmeraldDeep else ScnsCrimson
                )
                Text(
                    text = "Paid ${ScnsCalculators.formatZar(paid)} / Due ${ScnsCalculators.formatZar(due)} (Bal: ${ScnsCalculators.formatZar(bal)})",
                    style = ScnsNumericStyle.copy(
                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
                        color = if (bal > 0) ScnsCrimson else ScnsEmeraldDeep
                    )
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onExportPdf) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDF")
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }
                IconButton(onClick = onDeleteRequest) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete ${learner.names}",
                        tint = ScnsCrimson
                    )
                }
            }
        }
    }
}
