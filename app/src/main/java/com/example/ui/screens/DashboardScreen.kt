package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PersonalInfo
import com.example.data.model.ResumeFull
import com.example.data.model.ResumeTemplate
import com.example.ui.components.ResumeCardItem
import com.example.ui.theme.AiSparkleGradientEnd
import com.example.ui.theme.AiSparkleGradientStart
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SuccessGreen
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    resumes: List<ResumeFull>,
    onCreateNewResume: () -> Unit,
    onEditResume: (ResumeFull) -> Unit,
    onPreviewResume: (ResumeFull) -> Unit,
    onDuplicateResume: (ResumeFull) -> Unit,
    onDeleteResume: (ResumeFull) -> Unit,
    onAtsScanResume: (ResumeFull) -> Unit,
    onShareResume: (ResumeFull) -> Unit,
    onOpenCoverLetterGenerator: (ResumeFull?) -> Unit,
    onOpenAiCoach: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTemplate by remember { mutableStateOf<ResumeTemplate?>(null) }
    var resumeToDelete by remember { mutableStateOf<ResumeFull?>(null) }

    val filteredResumes = remember(resumes, searchQuery, selectedFilterTemplate) {
        resumes.filter { resume ->
            val matchesQuery = searchQuery.isBlank() ||
                    resume.title.contains(searchQuery, ignoreCase = true) ||
                    resume.personalInfo.fullName.contains(searchQuery, ignoreCase = true) ||
                    resume.personalInfo.jobTitle.contains(searchQuery, ignoreCase = true)
            val matchesTemplate = selectedFilterTemplate == null || resume.template == selectedFilterTemplate
            matchesQuery && matchesTemplate
        }
    }

    val avgAtsScore = remember(resumes) {
        if (resumes.isEmpty()) 0
        else (resumes.map { if (it.atsScore > 0) it.atsScore else 85 }.average()).toInt()
    }

    Scaffold(
        modifier = modifier.testTag("dashboard_screen"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateNewResume,
                icon = { Icon(Icons.Default.Add, contentDescription = "Create") },
                text = { Text("Create Resume", fontWeight = FontWeight.Bold) },
                containerColor = PrimaryIndigo,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("create_resume_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp)
        ) {
            // Hero AI Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4338CA))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFFFDE047),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Gemini 3.5 AI Enabled",
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onOpenCoverLetterGenerator(resumes.firstOrNull()) }
                                ) {
                                    Icon(
                                        Icons.Outlined.Email,
                                        contentDescription = "Cover Letter Generator",
                                        tint = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Build ATS-Winning Resumes in Minutes",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                lineHeight = 26.sp
                            )

                            Text(
                                text = "Generate STAR-formula bullets, professional summaries, and instant ATS score diagnostics.",
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onCreateNewResume,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Resume", color = PrimaryIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = onOpenAiCoach,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(Color.White, Color.White))),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AI Coach", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = { onOpenCoverLetterGenerator(resumes.firstOrNull()) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.5f)))),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cover Letter", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Total Resumes",
                        value = "${resumes.size}",
                        icon = Icons.Outlined.Description,
                        accent = PrimaryIndigo,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Avg ATS Score",
                        value = "$avgAtsScore%",
                        icon = Icons.Outlined.Speed,
                        accent = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Public Links",
                        value = "${resumes.count { it.isPublic }}",
                        icon = Icons.Outlined.Public,
                        accent = Color(0xFF0D9488),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search Bar & Filter Chips
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by title, name, or role...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = null) } }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("resume_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Template Filter Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilterTemplate == null,
                            onClick = { selectedFilterTemplate = null },
                            label = { Text("All Templates (${resumes.size})") }
                        )
                    }
                    items(ResumeTemplate.values()) { tpl ->
                        val count = resumes.count { it.template == tpl }
                        FilterChip(
                            selected = selectedFilterTemplate == tpl,
                            onClick = { selectedFilterTemplate = if (selectedFilterTemplate == tpl) null else tpl },
                            label = { Text("${tpl.displayName} ($count)") }
                        )
                    }
                }
            }

            // Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Resumes (${filteredResumes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Resume List
            if (filteredResumes.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "No resumes found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Create a new resume or try a different search filter",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onCreateNewResume,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Create First Resume")
                            }
                        }
                    }
                }
            } else {
                items(filteredResumes, key = { it.id }) { resume ->
                    ResumeCardItem(
                        resume = resume,
                        onEdit = { onEditResume(resume) },
                        onPreview = { onPreviewResume(resume) },
                        onDuplicate = { onDuplicateResume(resume) },
                        onDelete = { resumeToDelete = resume },
                        onAtsScan = { onAtsScanResume(resume) },
                        onShare = { onShareResume(resume) }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    resumeToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { resumeToDelete = null },
            title = { Text("Delete Resume?") },
            text = { Text("Are you sure you want to delete \"${target.title}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteResume(target)
                        resumeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { resumeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
