package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.ScnsConstants
import com.example.ui.components.ScnsBrandCrest
import com.example.ui.screens.ExamsScreen
import com.example.ui.screens.ExecutiveGateScreen
import com.example.ui.screens.ExportSaveScreen
import com.example.ui.screens.GoogleAuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LearnersHubScreen
import com.example.ui.screens.UploadPhotoScreen
import com.example.ui.theme.ScnsGoldBright
import com.example.ui.theme.ScnsNavy
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.ScnsViewModel
import com.example.util.PdfExportScope

private data class NavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val SCNS_DESTINATIONS = listOf(
    NavDestination("home", "Home", Icons.Default.Home),
    NavDestination("learners", "Learners", Icons.Default.Groups),
    NavDestination("upload", "Upload", Icons.Default.PhotoCamera),
    NavDestination("exams", "Exams", Icons.Default.Assessment),
    NavDestination("export", "Save/PDF", Icons.Default.SaveAlt)
)

@Composable
fun ScnsAppRoot(viewModel: ScnsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        withFrameNanos { }
        viewModel.onUiFirstFrameReady(context)
    }

    when (val auth = uiState.authState) {
        is AuthUiState.Checking -> {
            GoogleAuthScreen(
                isChecking = true,
                isSigningIn = false,
                errorMessage = null,
                onSignInWithGoogle = {}
            )
        }

        is AuthUiState.SignedOut -> {
            GoogleAuthScreen(
                isChecking = false,
                isSigningIn = uiState.isSigningIn,
                errorMessage = auth.message,
                onSignInWithGoogle = { viewModel.signInWithGoogle(context) }
            )
        }

        is AuthUiState.SignedIn -> {
            key(auth.uid) {
                if (!uiState.executiveUnlocked || uiState.showExecutiveModal) {
                    BackHandler(enabled = uiState.executiveUnlocked && uiState.showExecutiveModal) {
                        viewModel.setShowExecutiveModal(false)
                    }
                    ExecutiveGateScreen(
                        executives = uiState.executives,
                        chairConfig = uiState.chairConfig,
                        trialDaysLeft = uiState.trialDaysLeft,
                        statusMessage = uiState.statusMessage,
                        canDismissModal = uiState.executiveUnlocked,
                        onDismissModal = { viewModel.setShowExecutiveModal(false) },
                        onStartTrial = { viewModel.startFreeTrial() },
                        onRegisterExecutive = { n, s, id, c, p, r, t ->
                            viewModel.registerExecutive(n, s, id, c, p, r, t)
                        },
                        onLoginExecutive = { c, p -> viewModel.loginExecutive(c, p) },
                        onRequestOtp = { c -> viewModel.requestOtpForCell(c) },
                        onVerifyOtp = { c, otp -> viewModel.verifyExecutiveOtp(c, otp) },
                        onApproveOrOtp = { exec, direct -> viewModel.approveOrGenerateOtp(exec, direct) },
                        onRemoveExecutive = { id -> viewModel.removeExecutive(id) },
                        onSaveChairConfig = { n, c, sender -> viewModel.saveChairpersonSettings(n, c, sender) },
                        onSignOutGoogle = { viewModel.signOutGoogle() }
                    )
                } else {
                    SignedInWorkspaceScaffold(
                        auth = auth,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignedInWorkspaceScaffold(
    auth: AuthUiState.SignedIn,
    viewModel: ScnsViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ScnsNavy,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ScnsBrandCrest(size = 34.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = ScnsConstants.ORG_NAME,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ScnsGoldBright,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = uiState.activeExecutive?.let { "${it.role}: ${it.name}" }
                                        ?: auth.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.setShowExecutiveModal(true) },
                            modifier = Modifier.testTag("topbar_users_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ManageAccounts,
                                contentDescription = "Executive Users and Chairperson Settings"
                            )
                        }
                        IconButton(
                            onClick = { viewModel.signOutGoogle() },
                            modifier = Modifier.testTag("topbar_signout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign out"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        SCNS_DESTINATIONS.forEach { dest ->
                            NavigationBarItem(
                                selected = currentRoute == dest.route,
                                onClick = { navigateTo(dest.route) },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        SCNS_DESTINATIONS.forEach { dest ->
                            NavigationRailItem(
                                selected = currentRoute == dest.route,
                                onClick = { navigateTo(dest.route) },
                                icon = { Icon(dest.icon, contentDescription = dest.label) },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                uiState = uiState,
                                onNavigateRoute = { navigateTo(it) },
                                onSelectHubTab = { viewModel.setHubTab(it) },
                                onExportPdf = { scope, share -> viewModel.exportPdf(scope, share) },
                                onOpenExecutiveModal = { viewModel.setShowExecutiveModal(true) }
                            )
                        }
                        composable("learners") {
                            LearnersHubScreen(
                                uiState = uiState,
                                onSetHubTab = { viewModel.setHubTab(it) },
                                onUpdateSearch = { viewModel.updateSearch(it) },
                                onUpdateFilters = { s, g, sub, d, ind -> viewModel.updateFilters(s, g, sub, d, ind) },
                                onClearFilters = { viewModel.clearAllFilters() },
                                onSaveLearner = { learner, done -> viewModel.saveLearner(learner, done) },
                                onDeleteLearner = { viewModel.deleteLearner(it) },
                                onAddSingleAttendance = { l, d, day, st, sub ->
                                    viewModel.addSingleAttendance(l, d, day, st, sub)
                                },
                                onBulkAttendance = { byGrade, g, sub, d, day, st ->
                                    viewModel.bulkAddAttendance(byGrade, g, sub, d, day, st)
                                },
                                onClearAttendanceScope = { ind, g, sub, dt ->
                                    viewModel.clearAttendanceForScope(ind, g, sub, dt)
                                },
                                onClearRepeatedAttendance = { viewModel.clearRepeatedAttendance() },
                                onExportFilteredAttendancePdf = { title, sub, entries, share ->
                                    viewModel.exportFilteredAttendancePdf(title, sub, entries, share)
                                },
                                onAddPayment = { l, d, t, m, term, amt, note ->
                                    viewModel.addPayment(l, d, t, m, term, amt, note)
                                },
                                onExportIndividualPdf = { l, payOnly, share ->
                                    viewModel.exportIndividualLearnerPdf(l, payOnly, share)
                                },
                                onExportAllPaymentsPdf = { share ->
                                    viewModel.exportPdf(PdfExportScope.PAYMENTS, share)
                                }
                            )
                        }
                        composable("upload") {
                            UploadPhotoScreen(
                                uiState = uiState,
                                onPhotoPicked = { viewModel.addPickedPhoto(it) },
                                onBatchImport = { raw, g, sub, sch, d, day, att, pay ->
                                    viewModel.batchImportLearners(raw, g, sub, sch, d, day, att, pay)
                                }
                            )
                        }
                        composable("exams") {
                            ExamsScreen(
                                uiState = uiState,
                                onSaveExamRow = { l, entry -> viewModel.saveExamForLearner(l, entry) },
                                onSaveAllExams = { batch -> viewModel.saveAllExamsBatch(batch) },
                                onQuickAddLearner = { n, s, g, sub, sch, sex, age, par, dt ->
                                    viewModel.quickAddLearnerFromExams(n, s, g, sub, sch, sex, age, par, dt)
                                },
                                onDownloadProgressReport = { l, sub, term, share ->
                                    viewModel.exportSchoolProgressReportPdf(l, sub, term, share)
                                },
                                onDownloadExamsPdf = { scope, sub, g, term, share ->
                                    viewModel.exportPdf(scope, share, sub, g, term)
                                }
                            )
                        }
                        composable("export") {
                            ExportSaveScreen(
                                uiState = uiState,
                                onExportPdf = { scope, share -> viewModel.exportPdf(scope, share) },
                                onExportCsv = { scope, share -> viewModel.exportCsv(scope, share) },
                                onSaveManualNow = { viewModel.createManualSaveNow() },
                                onDeleteManualSave = { viewModel.deleteManualSave(it) },
                                onOpenExportedFile = { viewModel.openExportedFile(it) },
                                onShareExportedFile = { viewModel.shareExportedFile(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
