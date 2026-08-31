package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.*
import com.example.ui.viewmodel.ResumeViewModel

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Resumes", Icons.Outlined.Description)
    object Chat : Screen("ai_coach_chat", "AI Coach", Icons.Outlined.SmartToy)
    object CoverLetter : Screen("cover_letter", "Cover Letter", Icons.Outlined.Email)
    object Profile : Screen("profile", "Account", Icons.Outlined.Person)
    object Editor : Screen("editor", "Editor")
    object Preview : Screen("preview", "Preview")
    object AtsScanner : Screen("ats_scanner", "ATS Scanner")
}

@Composable
fun ResumeApp(
    viewModel: ResumeViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val allResumes by viewModel.allResumes.collectAsStateWithLifecycle()
    val currentResume by viewModel.currentResume.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val activePersona by viewModel.activePersona.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()

    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.Chat.route,
        Screen.CoverLetter.route,
        Screen.Profile.route
    )

    Scaffold(
        modifier = modifier.testTag("resume_app_root"),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(tonalElevation = 8.dp) {
                    listOf(Screen.Dashboard, Screen.Chat, Screen.CoverLetter, Screen.Profile).forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Dashboard.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { screen.icon?.let { Icon(it, contentDescription = screen.title) } },
                            label = { Text(screen.title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Dashboard
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    resumes = allResumes,
                    onCreateNewResume = {
                        val newResume = viewModel.createNewResume()
                        viewModel.selectResume(newResume)
                        navController.navigate(Screen.Editor.route)
                    },
                    onImportResume = { imported ->
                        viewModel.saveResume(imported)
                        viewModel.selectResume(imported)
                        navController.navigate(Screen.Editor.route)
                    },
                    onEditResume = { resume ->
                        viewModel.selectResume(resume)
                        navController.navigate(Screen.Editor.route)
                    },
                    onPreviewResume = { resume ->
                        viewModel.selectResume(resume)
                        navController.navigate(Screen.Preview.route)
                    },
                    onDuplicateResume = { resume ->
                        viewModel.duplicateResume(resume)
                    },
                    onDeleteResume = { resume ->
                        viewModel.deleteResume(resume)
                    },
                    onAtsScanResume = { resume ->
                        viewModel.selectResume(resume)
                        navController.navigate(Screen.AtsScanner.route)
                    },
                    onShareResume = { resume ->
                        viewModel.selectResume(resume)
                        navController.navigate(Screen.Preview.route)
                    },
                    onOpenCoverLetterGenerator = { resume ->
                        if (resume != null) viewModel.selectResume(resume)
                        navController.navigate(Screen.CoverLetter.route)
                    },
                    onOpenAiCoach = {
                        navController.navigate(Screen.Chat.route)
                    }
                )
            }

            // AI Coach Chat
            composable(Screen.Chat.route) {
                AiCoachChatScreen(
                    messages = chatMessages,
                    activePersona = activePersona,
                    selectedModel = selectedModel,
                    isThinking = isThinking,
                    activeResume = currentResume ?: allResumes.firstOrNull(),
                    onSendMessage = { text ->
                        viewModel.sendChatMessage(text)
                    },
                    onSelectPersona = { persona ->
                        viewModel.setChatPersona(persona)
                    },
                    onSelectModel = { model ->
                        viewModel.setChatModel(model)
                    },
                    onClearChat = {
                        viewModel.clearChat()
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Editor
            composable(Screen.Editor.route) {
                val resumeToEdit = currentResume ?: allResumes.firstOrNull() ?: viewModel.createNewResume()
                ResumeEditorScreen(
                    initialResume = resumeToEdit,
                    onSaveResume = { updated ->
                        viewModel.saveResume(updated)
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigatePreview = { updated ->
                        viewModel.saveResume(updated)
                        navController.navigate(Screen.Preview.route)
                    }
                )
            }

            // Preview & Export
            composable(Screen.Preview.route) {
                val resumeToPreview = currentResume ?: allResumes.firstOrNull() ?: viewModel.createNewResume()
                PreviewExportScreen(
                    initialResume = resumeToPreview,
                    onSaveResume = { updated ->
                        viewModel.saveResume(updated)
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateAtsScanner = { updated ->
                        viewModel.saveResume(updated)
                        navController.navigate(Screen.AtsScanner.route)
                    }
                )
            }

            // ATS Scanner
            composable(Screen.AtsScanner.route) {
                val resumeToScan = currentResume ?: allResumes.firstOrNull() ?: viewModel.createNewResume()
                AtsScannerScreen(
                    resume = resumeToScan,
                    onSaveResume = { updated ->
                        viewModel.saveResume(updated)
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // AI Cover Letter
            composable(Screen.CoverLetter.route) {
                CoverLetterScreen(
                    resumes = allResumes,
                    selectedResumeId = currentResume?.id,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Profile
            composable(Screen.Profile.route) {
                ProfileScreen(
                    resumeCount = allResumes.size,
                    authState = authState,
                    syncStatus = syncStatus,
                    isSyncing = isSyncing,
                    onSignInWithGoogle = {
                        viewModel.signInWithGoogle(context)
                    },
                    onSendEmailOtp = { email, callback ->
                        viewModel.sendEmailOtp(email, callback)
                    },
                    onVerifyEmailOtp = { email, otp, name, callback ->
                        viewModel.verifyEmailOtp(email, otp, name, callback)
                    },
                    onSendPhoneOtp = { phone, callback ->
                        viewModel.sendPhoneOtp(phone, callback)
                    },
                    onVerifyPhoneOtp = { phone, otp, name, callback ->
                        viewModel.verifyPhoneOtp(phone, otp, name, callback)
                    },
                    onSignOut = {
                        viewModel.signOut()
                    },
                    onSyncAllToFirestore = {
                        viewModel.syncAllToFirestore()
                    },
                    onRestoreFromFirestore = {
                        viewModel.restoreFromFirestore()
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
