package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.GoogleAuthManager
import com.example.data.firebase.GoogleSignInOutcome
import com.example.data.firebase.ScnsRepository
import com.example.data.model.AttendanceEntry
import com.example.data.model.ChairpersonConfig
import com.example.data.model.ExamEntry
import com.example.data.model.ExecutiveMember
import com.example.data.model.LearnerRecord
import com.example.data.model.LocalPhotoImportRecord
import com.example.data.model.ManualSaveSnapshot
import com.example.data.model.PaymentEntry
import com.example.data.model.ScnsCalculators
import com.example.data.model.ScnsConstants
import com.example.data.model.ScnsSubjectCatalog
import com.example.util.CsvExportScope
import com.example.util.ExportedFileResult
import com.example.util.FileExportHelper
import com.example.util.PdfExportScope
import com.example.util.ScnsCsvExporter
import com.example.util.ScnsPdfGenerator
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

sealed interface AuthUiState {
    data object Checking : AuthUiState
    data class SignedOut(val message: String? = null) : AuthUiState
    data class SignedIn(
        val uid: String,
        val displayName: String,
        val email: String
    ) : AuthUiState
}

enum class LearnersHubTab(val label: String) {
    LEARNERS("Learner List"),
    ATTENDANCE("Attendance"),
    PAYMENTS("Payments")
}

data class ScnsDashboardMetrics(
    val totalLearners: Int = 0,
    val acceptedCount: Int = 0,
    val rejectedCount: Int = 0,
    val waitingCount: Int = 0,
    val depositUnpaidCount: Int = 0,
    val owingCount: Int = 0,
    val totalCollected: Long = 0L,
    val totalOutstanding: Long = 0L
)

data class ScnsUiState(
    val authState: AuthUiState = AuthUiState.Checking,
    val isSigningIn: Boolean = false,
    val executiveUnlocked: Boolean = false,
    val showExecutiveModal: Boolean = false,
    val activeExecutive: ExecutiveMember? = null,
    val isTrialActive: Boolean = false,
    val trialDaysLeft: Int = 0,
    val isLoadingData: Boolean = false,
    val learners: List<LearnerRecord> = emptyList(),
    val executives: List<ExecutiveMember> = emptyList(),
    val chairConfig: ChairpersonConfig = ChairpersonConfig(),
    val manualSaves: List<ManualSaveSnapshot> = emptyList(),
    val metrics: ScnsDashboardMetrics = ScnsDashboardMetrics(),
    val hubTab: LearnersHubTab = LearnersHubTab.LEARNERS,
    val searchQuery: String = "",
    val statusFilter: String = "All",
    val gradeFilter: String = "",
    val subjectFilter: String = "",
    val dayFilter: String = "",
    val individualLearnerId: String = "",
    val photoHistory: List<LocalPhotoImportRecord> = emptyList(),
    val statusMessage: String? = null,
    val lastExportedFile: ExportedFileResult? = null
)

class ScnsViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = GoogleAuthManager(application.applicationContext)
    private val repository = ScnsRepository(application.applicationContext)
    private val prefs by lazy {
        application.getSharedPreferences("scns_local_prefs", Context.MODE_PRIVATE)
    }

    private val _uiState = MutableStateFlow(ScnsUiState())
    val uiState: StateFlow<ScnsUiState> = _uiState.asStateFlow()

    private var dataJobs: List<Job> = emptyList()
    private var observedUid: String? = null
    private var startupInitialized = false

    fun onUiFirstFrameReady(activityContext: Context) {
        if (startupInitialized) return
        startupInitialized = true

        viewModelScope.launch(Dispatchers.IO) {
            loadPhotoHistory()
        }
        viewModelScope.launch {
            val existingUser = withContext(Dispatchers.IO) { authManager.currentUser }
            if (existingUser != null) {
                applySignedInUser(existingUser)
            } else {
                when (val outcome = authManager.attemptSilentSignIn(activityContext)) {
                    is GoogleSignInOutcome.Success -> applySignedInUser(outcome.user)
                    is GoogleSignInOutcome.NoAccount -> _uiState.update {
                        it.copy(authState = AuthUiState.SignedOut(null))
                    }
                    is GoogleSignInOutcome.Cancelled -> _uiState.update {
                        it.copy(authState = AuthUiState.SignedOut(null))
                    }
                    is GoogleSignInOutcome.Error -> _uiState.update {
                        it.copy(authState = AuthUiState.SignedOut(outcome.message))
                    }
                }
            }
            observeAuthLifecycle()
        }
    }

    private fun observeAuthLifecycle() {
        viewModelScope.launch {
            authManager.observeAuthUser().collect { user ->
                if (user == null) {
                    stopDataStreams()
                    observedUid = null
                    _uiState.update {
                        it.copy(
                            authState = AuthUiState.SignedOut(
                                if (it.authState is AuthUiState.SignedOut) it.authState.message else null
                            ),
                            executiveUnlocked = false,
                            activeExecutive = null,
                            learners = emptyList(),
                            executives = emptyList(),
                            manualSaves = emptyList(),
                            metrics = ScnsDashboardMetrics()
                        )
                    }
                } else {
                    applySignedInUser(user)
                }
            }
        }
    }

    private fun applySignedInUser(user: FirebaseUser) {
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@")
            ?: "SCNS Executive"
        _uiState.update {
            it.copy(
                authState = AuthUiState.SignedIn(
                    uid = user.uid,
                    displayName = displayName,
                    email = user.email.orEmpty()
                )
            )
        }
        if (observedUid != user.uid) {
            observedUid = user.uid
            startDataStreams(user.uid)
        }
    }

    private fun startDataStreams(uid: String) {
        stopDataStreams()
        _uiState.update { it.copy(isLoadingData = true) }

        val bootstrap = viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.ensureInitialized() }

            val j1 = launch {
                repository.observeLearners(uid)
                    .catch { e ->
                        _uiState.update {
                            it.copy(isLoadingData = false, statusMessage = e.message ?: "Failed loading learners.")
                        }
                    }
                    .collect { learners ->
                        _uiState.update { state ->
                            state.copy(
                                learners = learners,
                                metrics = computeMetrics(learners),
                                isLoadingData = false
                            )
                        }
                    }
            }

            launch {
                repository.observeExecutives(uid)
                    .catch { e ->
                        _uiState.update { it.copy(statusMessage = e.message) }
                    }
                    .collect { execs ->
                        _uiState.update { state ->
                            val refreshedActive = state.activeExecutive?.let { active ->
                                execs.firstOrNull { it.id == active.id }
                            }
                            state.copy(
                                executives = execs,
                                activeExecutive = refreshedActive ?: state.activeExecutive
                            )
                        }
                    }
            }

            launch {
                repository.observeChairpersonConfig(uid)
                    .catch { e ->
                        _uiState.update { it.copy(statusMessage = e.message) }
                    }
                    .collect { config ->
                        val daysLeft = ScnsCalculators.trialDaysLeft(config)
                        _uiState.update { state ->
                            val unlockFromTrial = daysLeft > 0 && !state.executiveUnlocked
                            state.copy(
                                chairConfig = config.copy(ownerId = uid),
                                isTrialActive = daysLeft > 0,
                                trialDaysLeft = daysLeft,
                                executiveUnlocked = state.executiveUnlocked || unlockFromTrial
                            )
                        }
                    }
            }

            launch {
                repository.observeManualSaves(uid)
                    .catch { e ->
                        _uiState.update { it.copy(statusMessage = e.message) }
                    }
                    .collect { saves ->
                        _uiState.update { it.copy(manualSaves = saves) }
                    }
            }
        }

        dataJobs = listOf(bootstrap)
    }

    private fun stopDataStreams() {
        dataJobs.forEach { it.cancel() }
        dataJobs = emptyList()
    }

    private fun computeMetrics(learners: List<LearnerRecord>): ScnsDashboardMetrics {
        var accepted = 0
        var rejected = 0
        var waiting = 0
        var depUnpaid = 0
        var owing = 0
        var collected = 0L
        var outstanding = 0L
        for (l in learners) {
            when (l.status) {
                "Accepted" -> accepted++
                "Rejected" -> rejected++
                else -> waiting++
            }
            if (!l.depPaid) depUnpaid++
            val due = ScnsCalculators.totalDue(l.subjects)
            val paid = ScnsCalculators.paidTotal(l)
            val bal = (due - paid).coerceAtLeast(0L)
            if (bal > 0L) owing++
            collected += paid
            outstanding += bal
        }
        return ScnsDashboardMetrics(
            totalLearners = learners.size,
            acceptedCount = accepted,
            rejectedCount = rejected,
            waitingCount = waiting,
            depositUnpaidCount = depUnpaid,
            owingCount = owing,
            totalCollected = collected,
            totalOutstanding = outstanding
        )
    }

    fun signInWithGoogle(activityContext: Context) {
        if (_uiState.value.isSigningIn) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningIn = true) }
            when (val outcome = authManager.signInInteractive(activityContext)) {
                is GoogleSignInOutcome.Success -> {
                    applySignedInUser(outcome.user)
                    _uiState.update { it.copy(isSigningIn = false) }
                }
                is GoogleSignInOutcome.Cancelled -> {
                    _uiState.update {
                        it.copy(
                            isSigningIn = false,
                            authState = AuthUiState.SignedOut("Sign-in cancelled.")
                        )
                    }
                }
                is GoogleSignInOutcome.NoAccount -> {
                    _uiState.update {
                        it.copy(
                            isSigningIn = false,
                            authState = AuthUiState.SignedOut(outcome.message)
                        )
                    }
                }
                is GoogleSignInOutcome.Error -> {
                    _uiState.update {
                        it.copy(
                            isSigningIn = false,
                            authState = AuthUiState.SignedOut(outcome.message)
                        )
                    }
                }
            }
        }
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            stopDataStreams()
            authManager.signOut()
            _uiState.update {
                it.copy(
                    authState = AuthUiState.SignedOut(null),
                    executiveUnlocked = false,
                    showExecutiveModal = false,
                    activeExecutive = null,
                    statusMessage = "Signed out of Google account."
                )
            }
        }
    }

    private fun currentUid(): String? = (_uiState.value.authState as? AuthUiState.SignedIn)?.uid

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun setShowExecutiveModal(show: Boolean) {
        _uiState.update { it.copy(showExecutiveModal = show) }
    }

    fun lockExecutiveGate() {
        _uiState.update { it.copy(executiveUnlocked = false, activeExecutive = null) }
    }

    fun startFreeTrial() {
        val uid = currentUid() ?: return
        viewModelScope.launch {
            val existing = _uiState.value.chairConfig
            val updated = existing.copy(
                ownerId = uid,
                trialStartMs = if (existing.trialStartMs > 0L) existing.trialStartMs else System.currentTimeMillis()
            )
            repository.saveChairpersonConfig(uid, updated)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            executiveUnlocked = true,
                            showExecutiveModal = false,
                            isTrialActive = true,
                            trialDaysLeft = 7,
                            statusMessage = "7-Day Free Trial active! All SCNS tabs unlocked."
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(statusMessage = e.message) }
                }
        }
    }

    fun registerExecutive(
        name: String,
        surname: String,
        idNumber: String,
        cell: String,
        pin: String,
        role: String,
        termLabel: String = "Term 1"
    ) {
        val uid = currentUid() ?: return
        val cleanName = name.trim()
        val cleanSurname = surname.trim()
        val cleanCell = ScnsCalculators.normalizeCell(cell)
        val cleanPin = pin.trim()
        val cleanId = idNumber.trim()

        if (cleanName.isBlank() || cleanSurname.isBlank() || cleanCell.isBlank()) {
            _uiState.update { it.copy(statusMessage = "Please enter Name, Surname, and Cellphone.") }
            return
        }
        if (cleanPin.length != 4 || cleanPin.any { !it.isDigit() }) {
            _uiState.update { it.copy(statusMessage = "PIN must be exactly 4 digits.") }
            return
        }
        if (cleanId.isNotEmpty() && cleanId.length != 13) {
            _uiState.update { it.copy(statusMessage = "South African ID number must be 13 digits (or leave blank).") }
            return
        }

        val currentExecs = _uiState.value.executives
        val existingSameCell = currentExecs.firstOrNull {
            ScnsCalculators.normalizeCell(it.cell) == cleanCell
        }
        if (existingSameCell == null && currentExecs.size >= ScnsConstants.MAX_EXECUTIVES) {
            _uiState.update {
                it.copy(statusMessage = "Maximum 4 Executive users reached. Replace or remove a member first.")
            }
            return
        }

        val isChair = ScnsCalculators.isChairpersonMatch(
            cleanName, cleanSurname, cleanCell, _uiState.value.chairConfig
        )
        val newMember = ExecutiveMember(
            id = existingSameCell?.id.orEmpty(),
            ownerId = uid,
            name = cleanName,
            surname = cleanSurname,
            idNumber = cleanId,
            cell = cleanCell,
            pin = cleanPin,
            role = if (isChair) "Chairperson" else role,
            approved = isChair,
            otp = "",
            otpPending = !isChair,
            termLabel = termLabel
        )

        viewModelScope.launch {
            repository.upsertExecutive(uid, newMember)
                .onSuccess { docId ->
                    val saved = newMember.copy(id = docId)
                    _uiState.update {
                        it.copy(
                            activeExecutive = if (isChair) saved else it.activeExecutive,
                            executiveUnlocked = if (isChair) true else it.executiveUnlocked,
                            showExecutiveModal = !isChair,
                            statusMessage = if (isChair) {
                                "Welcome Chairperson $cleanName $cleanSurname — immediate access granted."
                            } else {
                                "Access requested for $cleanName $cleanSurname ($role). Chairperson can generate OTP below."
                            }
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    fun loginExecutive(cell: String, pin: String) {
        val cleanCell = ScnsCalculators.normalizeCell(cell)
        val cleanPin = pin.trim()
        val match = _uiState.value.executives.firstOrNull {
            ScnsCalculators.normalizeCell(it.cell) == cleanCell && it.pin == cleanPin
        }
        if (match == null) {
            _uiState.update { it.copy(statusMessage = "Cellphone or 4-digit PIN did not match any registered executive.") }
            return
        }
        if (!match.approved) {
            _uiState.update {
                it.copy(statusMessage = "${match.name} is awaiting Chairperson approval / OTP verification.")
            }
            return
        }
        _uiState.update {
            it.copy(
                activeExecutive = match,
                executiveUnlocked = true,
                showExecutiveModal = false,
                statusMessage = "Logged in as ${match.role}: ${match.name} ${match.surname}."
            )
        }
    }

    fun requestOtpForCell(cell: String) {
        val uid = currentUid() ?: return
        val cleanCell = ScnsCalculators.normalizeCell(cell)
        val match = _uiState.value.executives.firstOrNull {
            ScnsCalculators.normalizeCell(it.cell) == cleanCell
        }
        if (match == null) {
            _uiState.update { it.copy(statusMessage = "No executive found with cell $cell.") }
            return
        }
        viewModelScope.launch {
            repository.upsertExecutive(uid, match.copy(otpPending = true))
                .onSuccess {
                    _uiState.update {
                        it.copy(statusMessage = "OTP requested from Chairperson (${it.chairConfig.chairCell}).")
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    fun approveOrGenerateOtp(exec: ExecutiveMember, directApprove: Boolean) {
        val uid = currentUid() ?: return
        val otp = if (directApprove) "" else Random.nextInt(100000, 999999).toString()
        val updated = exec.copy(
            approved = directApprove,
            otp = otp,
            otpPending = !directApprove
        )
        viewModelScope.launch {
            repository.upsertExecutive(uid, updated)
                .onSuccess {
                    val msg = if (directApprove) {
                        "Approved ${exec.name} ${exec.surname} (${exec.role})."
                    } else {
                        "Generated OTP $otp for ${exec.name} (${exec.cell}). Share manually via WhatsApp or SMS."
                    }
                    _uiState.update { it.copy(statusMessage = msg) }
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    fun verifyExecutiveOtp(cell: String, otpInput: String) {
        val uid = currentUid() ?: return
        val cleanCell = ScnsCalculators.normalizeCell(cell)
        val cleanOtp = otpInput.trim()
        val match = _uiState.value.executives.firstOrNull {
            ScnsCalculators.normalizeCell(it.cell) == cleanCell && it.otp.isNotBlank() && it.otp == cleanOtp
        }
        if (match == null) {
            _uiState.update { it.copy(statusMessage = "Invalid OTP or cellphone.") }
            return
        }
        val approvedMember = match.copy(approved = true, otp = "", otpPending = false)
        viewModelScope.launch {
            repository.upsertExecutive(uid, approvedMember)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            activeExecutive = approvedMember,
                            executiveUnlocked = true,
                            showExecutiveModal = false,
                            statusMessage = "OTP verified! Welcome ${approvedMember.name} (${approvedMember.role})."
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    fun removeExecutive(execId: String) {
        val uid = currentUid() ?: return
        viewModelScope.launch {
            repository.deleteExecutive(uid, execId)
                .onSuccess {
                    _uiState.update { it.copy(statusMessage = "Executive slot cleared.") }
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    fun saveChairpersonSettings(newName: String, newCell: String, otpSender: String) {
        val uid = currentUid() ?: return
        val cleanName = newName.trim().ifBlank { ScnsConstants.DEFAULT_CHAIR_NAME }
        val cleanCell = ScnsCalculators.normalizeCell(newCell).ifBlank { ScnsConstants.DEFAULT_CHAIR_CELL }
        val current = _uiState.value.chairConfig
        val cellChanged = ScnsCalculators.normalizeCell(current.chairCell) != cleanCell
        val updated = current.copy(
            ownerId = uid,
            chairName = cleanName,
            chairCell = cleanCell,
            prevChairCell = if (cellChanged) current.chairCell else current.prevChairCell,
            prevCellUntilMs = if (cellChanged) System.currentTimeMillis() + ScnsConstants.SEVEN_DAYS_MS else current.prevCellUntilMs,
            otpSender = otpSender
        )
        viewModelScope.launch {
            repository.saveChairpersonConfig(uid, updated)
                .onSuccess {
                    _uiState.update {
                        it.copy(statusMessage = "Chairperson updated ($cleanName • $cleanCell). Previous cell kept 7 days if changed.")
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    // --- Filters & Hub Tab ---
    fun setHubTab(tab: LearnersHubTab) {
        _uiState.update { it.copy(hubTab = tab) }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateFilters(
        status: String = _uiState.value.statusFilter,
        grade: String = _uiState.value.gradeFilter,
        subject: String = _uiState.value.subjectFilter,
        day: String = _uiState.value.dayFilter,
        individualId: String = _uiState.value.individualLearnerId
    ) {
        _uiState.update {
            it.copy(
                statusFilter = status,
                gradeFilter = grade,
                subjectFilter = subject,
                dayFilter = day,
                individualLearnerId = individualId
            )
        }
    }

    fun clearAllFilters() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                statusFilter = "All",
                gradeFilter = "",
                subjectFilter = "",
                dayFilter = "",
                individualLearnerId = ""
            )
        }
    }

    // --- Learners CRUD ---
    fun saveLearner(learner: LearnerRecord, onDone: () -> Unit = {}) {
        val uid = currentUid() ?: return
        if (learner.names.isBlank() || learner.surname.isBlank()) {
            _uiState.update { it.copy(statusMessage = "Names and Surname are required.") }
            return
        }
        val finalRegNo = learner.regNo.ifBlank { ScnsCalculators.nextRegNo(_uiState.value.learners) }
        val toSave = learner.copy(
            ownerId = uid,
            regNo = finalRegNo,
            grade = learner.grade.ifBlank { "10" },
            status = learner.status.ifBlank { "Accepted" }
        )
        viewModelScope.launch {
            repository.upsertLearner(uid, toSave)
                .onSuccess {
                    _uiState.update {
                        it.copy(statusMessage = "Saved ${toSave.names} ${toSave.surname} (${toSave.regNo}).")
                    }
                    onDone()
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    fun deleteLearner(learner: LearnerRecord) {
        val uid = currentUid() ?: return
        viewModelScope.launch {
            repository.deleteLearner(uid, learner.id)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            individualLearnerId = if (state.individualLearnerId == learner.id) "" else state.individualLearnerId,
                            statusMessage = "Deleted ${learner.names} ${learner.surname} (${learner.regNo})."
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    // --- Attendance ---
    fun addSingleAttendance(
        learner: LearnerRecord,
        date: String,
        day: String,
        status: String,
        subject: String
    ) {
        val uid = currentUid() ?: return
        val normSubj = ScnsSubjectCatalog.normalizeSubject(subject.ifBlank { ScnsCalculators.primarySubjectOf(learner) })
        val updatedAtt = learner.att + AttendanceEntry(d = date, day = day, s = status, subject = normSubj)
        viewModelScope.launch {
            repository.upsertLearner(uid, learner.copy(att = updatedAtt))
                .onSuccess {
                    _uiState.update { it.copy(statusMessage = "${learner.names}: $status on $date ($normSubj)") }
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    fun bulkAddAttendance(
        byGrade: Boolean,
        grade: String,
        subject: String,
        date: String,
        day: String,
        status: String
    ) {
        val uid = currentUid() ?: return
        val targets = _uiState.value.learners.filter { l ->
            if (byGrade) {
                grade.isBlank() || l.grade == grade
            } else {
                subject.isBlank() || ScnsCalculators.learnerMatchesSubject(l, subject)
            }
        }
        if (targets.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "No learners match that ${if (byGrade) "Grade" else "Subject"} filter.") }
            return
        }
        val updatedList = targets.map { l ->
            val subj = if (!byGrade && subject.isNotBlank()) {
                ScnsSubjectCatalog.normalizeSubject(subject)
            } else {
                ScnsSubjectCatalog.normalizeSubject(subject.ifBlank { ScnsCalculators.primarySubjectOf(l) })
            }
            l.copy(att = l.att + AttendanceEntry(d = date, day = day, s = status, subject = subj))
        }
        viewModelScope.launch {
            repository.batchUpsertLearners(uid, updatedList)
                .onSuccess { count ->
                    _uiState.update {
                        it.copy(statusMessage = "Added $status attendance on $date ($day) for $count learners.")
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    fun clearAttendanceForScope(
        targetLearnerId: String,
        grade: String,
        subject: String,
        dateFilter: String
    ) {
        val uid = currentUid() ?: return
        val targets = _uiState.value.learners.filter { l ->
            (targetLearnerId.isBlank() || l.id == targetLearnerId) &&
                (grade.isBlank() || l.grade == grade) &&
                (subject.isBlank() || ScnsCalculators.learnerMatchesSubject(l, subject))
        }
        if (targets.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "No matching learners to clear attendance.") }
            return
        }
        var clearedEntries = 0
        val updated = targets.mapNotNull { l ->
            val fallback = ScnsCalculators.primarySubjectOf(l)
            val remaining = l.att.filterNot { a ->
                val dateMatches = dateFilter.isBlank() || a.d == dateFilter
                val subjMatches = subject.isBlank() || ScnsSubjectCatalog.normalizeSubject(
                    a.subject.ifBlank { fallback }
                ).equals(ScnsSubjectCatalog.normalizeSubject(subject), ignoreCase = true)
                dateMatches && subjMatches
            }
            val diff = l.att.size - remaining.size
            if (diff > 0) {
                clearedEntries += diff
                l.copy(att = remaining)
            } else {
                null
            }
        }
        if (updated.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "No attendance records matched that filter.") }
            return
        }
        viewModelScope.launch {
            repository.batchUpsertLearners(uid, updated)
                .onSuccess {
                    _uiState.update { it.copy(statusMessage = "Cleared $clearedEntries attendance entries.") }
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    fun clearRepeatedAttendance() {
        val uid = currentUid() ?: return
        var totalRemoved = 0
        val changedLearners = _uiState.value.learners.mapNotNull { l ->
            val (deduped, removed) = ScnsCalculators.deduplicateAttendance(
                l.att,
                ScnsCalculators.primarySubjectOf(l)
            )
            if (removed > 0) {
                totalRemoved += removed
                l.copy(att = deduped)
            } else {
                null
            }
        }
        if (changedLearners.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "No duplicate attendance entries found.") }
            return
        }
        viewModelScope.launch {
            repository.batchUpsertLearners(uid, changedLearners)
                .onSuccess {
                    _uiState.update {
                        it.copy(statusMessage = "Removed $totalRemoved duplicate attendance entries.")
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    // --- Payments ---
    fun addPayment(
        learner: LearnerRecord,
        date: String,
        type: String,
        month: String,
        term: String,
        amount: Long,
        note: String
    ) {
        val uid = currentUid() ?: return
        if (amount <= 0L) {
            _uiState.update { it.copy(statusMessage = "Enter a valid payment amount greater than R0.") }
            return
        }
        val entry = PaymentEntry(
            d = date,
            t = type,
            month = if (type.equals("Monthly", ignoreCase = true)) month else "",
            term = if (type.equals("Termly", ignoreCase = true)) term else "",
            amt = amount,
            note = note.trim()
        )
        val depNowPaid = learner.depPaid || amount >= ScnsConstants.DEPOSIT_AMOUNT_ZAR ||
            note.contains("dep", ignoreCase = true)
        val depDate = if (learner.depDate.isBlank() && depNowPaid) date else learner.depDate
        val updated = learner.copy(
            pay = learner.pay + entry,
            depPaid = depNowPaid,
            depDate = depDate
        )
        viewModelScope.launch {
            repository.upsertLearner(uid, updated)
                .onSuccess {
                    _uiState.update {
                        it.copy(statusMessage = "Recorded R$amount ($type) for ${learner.names} ${learner.surname}.")
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    // --- Exams ---
    fun saveExamForLearner(learner: LearnerRecord, entry: ExamEntry) {
        val uid = currentUid() ?: return
        val normSubject = ScnsSubjectCatalog.normalizeSubject(entry.subject)
        val key = ScnsCalculators.examKey(normSubject, entry.term)
        val cleanEntry = entry.copy(
            subject = normSubject,
            t1 = entry.t1.coerceIn(0, 100),
            t2 = entry.t2.coerceIn(0, 100),
            t3 = entry.t3.coerceIn(0, 100),
            t4 = entry.t4.coerceIn(0, 100)
        )
        val updatedExams = learner.exams.toMutableMap().apply { put(key, cleanEntry) }
        viewModelScope.launch {
            repository.upsertLearner(uid, learner.copy(exams = updatedExams))
                .onSuccess {
                    val stats = ScnsCalculators.computeExamStats(cleanEntry)
                    _uiState.update {
                        it.copy(
                            statusMessage = "Saved ${learner.names}: ${stats.total}/400 (${stats.percentage}% • ${stats.ratingLabel})"
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    fun saveAllExamsBatch(updates: List<Pair<LearnerRecord, ExamEntry>>) {
        val uid = currentUid() ?: return
        if (updates.isEmpty()) return
        val updatedLearners = updates.map { (learner, entry) ->
            val normSubject = ScnsSubjectCatalog.normalizeSubject(entry.subject)
            val key = ScnsCalculators.examKey(normSubject, entry.term)
            val cleanEntry = entry.copy(
                subject = normSubject,
                t1 = entry.t1.coerceIn(0, 100),
                t2 = entry.t2.coerceIn(0, 100),
                t3 = entry.t3.coerceIn(0, 100),
                t4 = entry.t4.coerceIn(0, 100)
            )
            val map = learner.exams.toMutableMap().apply { put(key, cleanEntry) }
            learner.copy(exams = map)
        }
        viewModelScope.launch {
            repository.batchUpsertLearners(uid, updatedLearners)
                .onSuccess { count ->
                    _uiState.update { it.copy(statusMessage = "Saved exam marks for $count learners.") }
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    fun quickAddLearnerFromExams(
        names: String,
        surname: String,
        grade: String,
        subject: String,
        school: String,
        sex: String,
        age: String,
        parent: String,
        admDate: String
    ) {
        val uid = currentUid() ?: return
        if (names.isBlank() || surname.isBlank()) {
            _uiState.update { it.copy(statusMessage = "Names and Surname are required.") }
            return
        }
        val cleanGrade = grade.ifBlank { ScnsSubjectCatalog.defaultGradeForSubject(subject) }
        val cleanSubject = ScnsSubjectCatalog.normalizeSubject(subject.ifBlank { "Mathematics" })
        val regNo = ScnsCalculators.nextRegNo(_uiState.value.learners)
        val learner = LearnerRecord(
            ownerId = uid,
            regNo = regNo,
            names = names.trim(),
            surname = surname.trim(),
            grade = cleanGrade,
            school = school.trim(),
            sex = sex.trim(),
            age = age.trim(),
            admDate = admDate,
            subjects = cleanSubject,
            status = "Accepted",
            depPaid = false,
            parent = parent.trim()
        )
        viewModelScope.launch {
            repository.upsertLearner(uid, learner)
                .onSuccess {
                    _uiState.update {
                        it.copy(statusMessage = "Added ${learner.names} ${learner.surname} ($regNo) to Grade $cleanGrade $cleanSubject.")
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    // --- Photo Picker History & Batch List Import ---
    fun addPickedPhoto(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>().applicationContext
            val photosDir = File(ctx.filesDir, "picked_photos").apply { mkdirs() }
            val destFile = File(photosDir, "register_${System.currentTimeMillis()}.jpg")
            val copied = runCatching {
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
            }.isSuccess

            val storedUri = if (copied && destFile.exists()) Uri.fromFile(destFile).toString() else uri.toString()
            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val record = LocalPhotoImportRecord(
                id = UUID.randomUUID().toString(),
                uriString = storedUri,
                label = "Register Photo $stamp",
                createdAtDisplay = stamp
            )
            val updated = (listOf(record) + _uiState.value.photoHistory).take(10)
            savePhotoHistory(updated)
            _uiState.update {
                it.copy(
                    photoHistory = updated,
                    statusMessage = "Photo saved locally to register gallery. Enter or paste names below to batch-add."
                )
            }
        }
    }

    fun batchImportLearners(
        rawList: String,
        grade: String,
        subject: String,
        school: String,
        date: String,
        day: String,
        alsoAddAttendance: Boolean,
        alsoAddPayment: Boolean
    ) {
        val uid = currentUid() ?: return
        val parsed = ScnsCalculators.parseBatchLines(rawList)
        if (parsed.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Paste or type at least one learner name line first.") }
            return
        }
        val cleanSubject = ScnsSubjectCatalog.normalizeSubject(subject.ifBlank { "Mathematics" })
        val cleanGrade = grade.ifBlank { "9" }

        var maxReg = _uiState.value.learners.maxOfOrNull { l ->
            Regex("""(\d+)$""").find(l.regNo)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        } ?: 0

        val newLearners = parsed.map { (surname, names) ->
            maxReg++
            val reg = "SCNS-2027-" + maxReg.toString().padStart(3, '0')
            LearnerRecord(
                ownerId = uid,
                regNo = reg,
                names = names,
                surname = surname,
                grade = cleanGrade,
                school = school.trim(),
                admDate = date,
                subjects = cleanSubject,
                status = "Accepted",
                depPaid = false,
                att = if (alsoAddAttendance) {
                    listOf(AttendanceEntry(d = date, day = day, s = "Present", subject = cleanSubject))
                } else emptyList(),
                pay = if (alsoAddPayment) {
                    listOf(PaymentEntry(d = date, t = "Termly", term = "Term 1", amt = 0L, note = "Batch Import"))
                } else emptyList()
            )
        }

        viewModelScope.launch {
            repository.batchUpsertLearners(uid, newLearners)
                .onSuccess { count ->
                    _uiState.update {
                        it.copy(
                            statusMessage = "Batch imported $count learners into Grade $cleanGrade ($cleanSubject)."
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    // --- Manual Save Snapshots & Exports ---
    fun createManualSaveNow() {
        val uid = currentUid() ?: return
        val learners = _uiState.value.learners
        val collected = _uiState.value.metrics.totalCollected
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val json = ScnsCsvExporter.serializeLearnersToJson(learners)
        val snapshot = ManualSaveSnapshot(
            ownerId = uid,
            label = "Manual Backup (${learners.size} learners)",
            savedAtDisplay = stamp,
            learnerCount = learners.size.toLong(),
            totalCollected = collected,
            snapshotJson = json
        )
        viewModelScope.launch {
            repository.createManualSave(uid, snapshot)
                .onSuccess {
                    _uiState.update {
                        it.copy(statusMessage = "Saved backup snapshot ($stamp) with ${learners.size} learners.")
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    fun deleteManualSave(saveId: String) {
        val uid = currentUid() ?: return
        viewModelScope.launch {
            repository.deleteManualSave(uid, saveId)
                .onSuccess { _uiState.update { it.copy(statusMessage = "Deleted snapshot.") } }
                .onFailure { e -> _uiState.update { it.copy(statusMessage = e.message) } }
        }
    }

    fun exportCsv(scope: CsvExportScope, shareImmediately: Boolean) {
        val ctx = getApplication<Application>().applicationContext
        val learners = _uiState.value.learners
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val csv = ScnsCsvExporter.buildCsv(scope, learners)
                val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                FileExportHelper.saveAndPrepareShare(
                    context = ctx,
                    fileName = "${scope.fileBase}_$stamp.csv",
                    mimeType = "text/csv",
                    bytes = csv.toByteArray(Charsets.UTF_8)
                )
            }
            _uiState.update {
                it.copy(
                    lastExportedFile = result,
                    statusMessage = "Saved ${result.displayLocation}"
                )
            }
            if (shareImmediately) {
                FileExportHelper.launchShareIntent(ctx, result.shareUri, "text/csv", scope.label)
            }
        }
    }

    fun exportPdf(
        scope: PdfExportScope,
        shareImmediately: Boolean,
        subjectFilter: String = "",
        gradeFilter: String = "",
        termFilter: String = "Term 1"
    ) {
        val ctx = getApplication<Application>().applicationContext
        val learners = _uiState.value.learners
        val config = _uiState.value.chairConfig
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val bytes = ScnsPdfGenerator.generateScopePdf(
                    context = ctx,
                    scope = scope,
                    learners = learners,
                    config = config,
                    subjectFilter = subjectFilter,
                    gradeFilter = gradeFilter,
                    termFilter = termFilter
                )
                val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                FileExportHelper.saveAndPrepareShare(
                    context = ctx,
                    fileName = "${scope.fileBase}_$stamp.pdf",
                    mimeType = "application/pdf",
                    bytes = bytes
                )
            }
            _uiState.update {
                it.copy(
                    lastExportedFile = result,
                    statusMessage = "PDF saved: ${result.displayLocation}"
                )
            }
            if (shareImmediately) {
                FileExportHelper.launchShareIntent(ctx, result.shareUri, "application/pdf", scope.title)
            }
        }
    }

    fun exportFilteredAttendancePdf(
        title: String,
        subtitle: String,
        entries: List<Pair<LearnerRecord, AttendanceEntry>>,
        shareImmediately: Boolean
    ) {
        val ctx = getApplication<Application>().applicationContext
        val config = _uiState.value.chairConfig
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val bytes = ScnsPdfGenerator.generateFilteredAttendancePdf(ctx, title, subtitle, entries, config)
                val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                FileExportHelper.saveAndPrepareShare(
                    ctx,
                    "SCNS_Attendance_View_$stamp.pdf",
                    "application/pdf",
                    bytes
                )
            }
            _uiState.update {
                it.copy(lastExportedFile = result, statusMessage = "PDF saved: ${result.displayLocation}")
            }
            if (shareImmediately) {
                FileExportHelper.launchShareIntent(ctx, result.shareUri, "application/pdf", title)
            }
        }
    }

    fun exportIndividualLearnerPdf(learner: LearnerRecord, paymentOnly: Boolean, shareImmediately: Boolean) {
        val ctx = getApplication<Application>().applicationContext
        val config = _uiState.value.chairConfig
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val bytes = if (paymentOnly) {
                    ScnsPdfGenerator.generateIndividualPaymentPdf(ctx, learner, config)
                } else {
                    ScnsPdfGenerator.generateIndividualLearnerPdf(ctx, learner, config)
                }
                val prefix = if (paymentOnly) "SCNS_Payments" else "SCNS_Learner"
                val safeReg = learner.regNo.replace(Regex("""[^A-Za-z0-9_-]"""), "_")
                FileExportHelper.saveAndPrepareShare(
                    ctx,
                    "${prefix}_$safeReg.pdf",
                    "application/pdf",
                    bytes
                )
            }
            _uiState.update {
                it.copy(lastExportedFile = result, statusMessage = "PDF saved: ${result.displayLocation}")
            }
            if (shareImmediately) {
                FileExportHelper.launchShareIntent(
                    ctx,
                    result.shareUri,
                    "application/pdf",
                    "${learner.names} ${learner.surname}"
                )
            }
        }
    }

    fun exportSchoolProgressReportPdf(
        learner: LearnerRecord,
        subject: String,
        term: String,
        shareImmediately: Boolean
    ) {
        val ctx = getApplication<Application>().applicationContext
        val config = _uiState.value.chairConfig
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val bytes = ScnsPdfGenerator.generateSchoolProgressReportPdf(
                    context = ctx,
                    learner = learner,
                    subject = subject,
                    term = term,
                    config = config
                )
                val safeReg = learner.regNo.replace(Regex("""[^A-Za-z0-9_-]"""), "_")
                val safeTerm = term.replace(" ", "")
                FileExportHelper.saveAndPrepareShare(
                    ctx,
                    "SCNS_ProgressReport_${safeReg}_$safeTerm.pdf",
                    "application/pdf",
                    bytes
                )
            }
            _uiState.update {
                it.copy(lastExportedFile = result, statusMessage = "One-Page Progress Report saved: ${result.displayLocation}")
            }
            if (shareImmediately) {
                FileExportHelper.launchShareIntent(
                    ctx,
                    result.shareUri,
                    "application/pdf",
                    "Progress Report: ${learner.names} ${learner.surname} ($term)"
                )
            }
        }
    }

    fun shareExportedFile(file: ExportedFileResult) {
        val ctx = getApplication<Application>().applicationContext
        val mime = if (file.fileName.endsWith(".csv")) "text/csv" else "application/pdf"
        FileExportHelper.launchShareIntent(ctx, file.shareUri, mime, file.fileName)
    }

    fun openExportedFile(file: ExportedFileResult) {
        val ctx = getApplication<Application>().applicationContext
        val mime = if (file.fileName.endsWith(".csv")) "text/csv" else "application/pdf"
        FileExportHelper.openFileIntent(ctx, file.shareUri, mime)
    }

    private fun loadPhotoHistory() {
        val raw = prefs.getString("photo_history_json", "[]") ?: "[]"
        val list = runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        LocalPhotoImportRecord(
                            id = o.optString("id"),
                            uriString = o.optString("uriString"),
                            label = o.optString("label"),
                            createdAtDisplay = o.optString("createdAtDisplay")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
        _uiState.update { it.copy(photoHistory = list) }
    }

    private fun savePhotoHistory(list: List<LocalPhotoImportRecord>) {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("uriString", item.uriString)
                    put("label", item.label)
                    put("createdAtDisplay", item.createdAtDisplay)
                }
            )
        }
        prefs.edit().putString("photo_history_json", arr.toString()).apply()
    }
}
