package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.AuthUserState
import com.example.ui.theme.AiSparkleGradientEnd
import com.example.ui.theme.AiSparkleGradientStart
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SuccessGreen

import com.example.ui.components.SignInOtpDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    resumeCount: Int,
    authState: AuthUserState,
    syncStatus: String,
    isSyncing: Boolean,
    onSignInWithGoogle: () -> Unit,
    onSendEmailOtp: (String, (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    onVerifyEmailOtp: (String, String, String, (Boolean, String) -> Unit) -> Unit = { _, _, _, _ -> },
    onSendPhoneOtp: (String, (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    onVerifyPhoneOtp: (String, String, String, (Boolean, String) -> Unit) -> Unit = { _, _, _, _ -> },
    onSignOut: () -> Unit,
    onSyncAllToFirestore: () -> Unit,
    onRestoreFromFirestore: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSignInModal by remember { mutableStateOf(false) }

    if (showSignInModal) {
        SignInOtpDialog(
            onDismiss = { showSignInModal = false },
            onSendEmailOtp = onSendEmailOtp,
            onVerifyEmailOtp = onVerifyEmailOtp,
            onSendPhoneOtp = onSendPhoneOtp,
            onVerifyPhoneOtp = onVerifyPhoneOtp,
            onSignInWithGoogle = onSignInWithGoogle
        )
    }

    Scaffold(
        modifier = modifier.testTag("profile_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Account & Cloud Sync", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("profile_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 40.dp, top = 8.dp)
        ) {
            // User & Auth Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(PrimaryIndigo, Color(0xFF9333EA)))),
                            contentAlignment = Alignment.Center
                        ) {
                            val initials = if (authState.isLoggedIn && authState.displayName.isNotBlank()) {
                                authState.displayName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").uppercase()
                            } else "SS"

                            Text(
                                text = initials,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (authState.isLoggedIn) authState.displayName else "Guest User",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (authState.isLoggedIn) authState.email else "Sign in with Google to enable cloud backup",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (authState.isLoggedIn) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SuccessGreen.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val authProviderLabel = when (authState.authProvider) {
                                        "EMAIL_OTP" -> "Email OTP Verified"
                                        "PHONE_OTP" -> "Mobile SMS Verified"
                                        "GOOGLE" -> "Google Authenticated"
                                        else -> "Cloud Sync Active"
                                    }
                                    Text(authProviderLabel, color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            if (authState.phone.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Mobile: ${authState.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showSignInModal = true },
                                    modifier = Modifier.weight(1f).testTag("switch_account_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Outlined.SwitchAccount, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Switch / OTP", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = onSignOut,
                                    modifier = Modifier.weight(1f).testTag("sign_out_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sign Out", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showSignInModal = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("open_otp_sign_in_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                                ) {
                                    Icon(Icons.Outlined.Password, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sign In with Email / Mobile OTP", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = onSignInWithGoogle,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("google_sign_in_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sign In with Google", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Cloud Firestore Persistence & Sync Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryIndigo.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.CloudSync, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Cloud Firestore Persistence", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Real-time cloud database backup & restore", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Sync Status Banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryIndigo)
                                } else {
                                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = syncStatus,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Action Buttons: Backup and Restore
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    onSyncAllToFirestore()
                                    Toast.makeText(context, "Syncing all resumes to Cloud Firestore...", Toast.LENGTH_SHORT).show()
                                },
                                enabled = !isSyncing,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sync_firestore_backup_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup Now", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    onRestoreFromFirestore()
                                    Toast.makeText(context, "Fetching backups from Firestore...", Toast.LENGTH_SHORT).show()
                                },
                                enabled = !isSyncing,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sync_firestore_restore_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore Cloud", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // AI & Database Engine Details Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(AiSparkleGradientStart, AiSparkleGradientEnd))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Gemini AI & Database Stack", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }

                        ListItem(
                            headlineContent = { Text("Multi-Turn Gemini Chatbot", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("4 Career Personas • gemini-3.5-flash, gemini-3.1-pro, gemini-3.1-flash-lite") },
                            leadingContent = { Icon(Icons.Outlined.Forum, contentDescription = null) }
                        )
                        Divider()
                        ListItem(
                            headlineContent = { Text("Cloud Firestore Database", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Cloud Document Store under users/{userId}/resumes") },
                            leadingContent = { Icon(Icons.Outlined.Cloud, contentDescription = null) }
                        )
                        Divider()
                        ListItem(
                            headlineContent = { Text("Local Room Database (Offline First)", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Room SQLite Persistence ($resumeCount stored resumes on device)") },
                            leadingContent = { Icon(Icons.Outlined.Storage, contentDescription = null) }
                        )
                        Divider()
                        ListItem(
                            headlineContent = { Text("PDF Print & Export Engine", fontWeight = FontWeight.Medium) },
                            supportingContent = { Text("Android PrintManager & Vector Canvas A4/Letter") },
                            leadingContent = { Icon(Icons.Outlined.Print, contentDescription = null) }
                        )
                    }
                }
            }

            // App Version Info
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AI Resume Builder v2.1 • Firebase Firestore & Gemini Multi-Turn AI",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
