package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ResumeFull
import com.example.data.model.ResumeTemplate
import com.example.ui.components.ColorPickerRow
import com.example.ui.components.ResumeSheetPreview
import com.example.ui.components.TranslateResumeDialog
import com.example.ui.theme.PrimaryIndigo
import com.example.util.ResumePdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewExportScreen(
    initialResume: ResumeFull,
    onSaveResume: (ResumeFull) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateAtsScanner: (ResumeFull) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var resumeState by remember { mutableStateOf(initialResume) }
    var showShareModal by remember { mutableStateOf(false) }
    var showColorPaletteSheet by remember { mutableStateOf(false) }
    var showTranslateDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag("preview_export_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = resumeState.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${resumeState.template.displayName} • Live PDF Preview",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        onSaveResume(resumeState)
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showTranslateDialog = true },
                        modifier = Modifier.testTag("preview_translate_button")
                    ) {
                        Icon(Icons.Outlined.Translate, contentDescription = "Translate Resume")
                    }

                    IconButton(
                        onClick = { onNavigateAtsScanner(resumeState) },
                        modifier = Modifier.testTag("ats_scanner_icon_button")
                    ) {
                        Icon(Icons.Outlined.Analytics, contentDescription = "ATS Score", tint = PrimaryIndigo)
                    }

                    IconButton(
                        onClick = { showColorPaletteSheet = !showColorPaletteSheet }
                    ) {
                        Icon(Icons.Outlined.ColorLens, contentDescription = "Color Theme")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Print / Save as PDF Button
                    Button(
                        onClick = {
                            if (activity != null) {
                                ResumePdfGenerator.printResume(activity, resumeState)
                            } else {
                                ResumePdfGenerator.sharePdf(context, resumeState)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("download_pdf_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print / PDF", fontWeight = FontWeight.Bold)
                    }

                    // Share Button
                    OutlinedButton(
                        onClick = { showShareModal = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_resume_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Options", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Template Switcher Bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ResumeTemplate.values()) { tpl ->
                    val isSelected = resumeState.template == tpl
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            resumeState = resumeState.copy(template = tpl)
                            onSaveResume(resumeState)
                        },
                        label = { Text(tpl.displayName, fontSize = 12.sp) }
                    )
                }
            }

            if (showColorPaletteSheet) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Pick Accent Theme Color:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        ColorPickerRow(
                            selectedColorHex = resumeState.accentColorHex,
                            onColorSelected = {
                                resumeState = resumeState.copy(accentColorHex = it)
                                onSaveResume(resumeState)
                            }
                        )
                    }
                }
            }

            // Scrollable Document Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFE2E8F0))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                ResumeSheetPreview(
                    resume = resumeState,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Share Options Bottom Sheet / Dialog
    if (showShareModal) {
        AlertDialog(
            onDismissRequest = { showShareModal = false },
            title = { Text("Export & Share Resume", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // PDF Share
                    ListItem(
                        headlineContent = { Text("Send PDF Document", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Send PDF via Email, Messaging, or Cloud Drive") },
                        leadingContent = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = PrimaryIndigo) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = {
                            Button(
                                onClick = {
                                    showShareModal = false
                                    ResumePdfGenerator.sharePdf(context, resumeState)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Share")
                            }
                        }
                    )

                    Divider()

                    // Plain Text Copy
                    ListItem(
                        headlineContent = { Text("Copy Plain Text", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Copy structured text for online forms and job applications") },
                        leadingContent = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF0D9488)) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = {
                            OutlinedButton(
                                onClick = {
                                    showShareModal = false
                                    ResumePdfGenerator.shareAsPlainText(context, resumeState)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Send Text")
                            }
                        }
                    )

                    Divider()

                    // Public Link
                    val publicLink = "https://resumebuilder.ai/r/${resumeState.shareSlug}"
                    ListItem(
                        headlineContent = { Text("Public Web Link", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(publicLink, color = PrimaryIndigo, fontSize = 12.sp) },
                        leadingContent = { Icon(Icons.Default.Public, contentDescription = null, tint = Color(0xFF2563EB)) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = {
                            FilledTonalButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Resume Public Link", publicLink)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Public Link copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    showShareModal = false
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Copy Link")
                            }
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareModal = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showTranslateDialog) {
        TranslateResumeDialog(
            resume = resumeState,
            onDismiss = { showTranslateDialog = false },
            onTranslationComplete = { translated ->
                resumeState = translated
                onSaveResume(translated)
                Toast.makeText(context, "Resume translated to ${translated.title}!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
