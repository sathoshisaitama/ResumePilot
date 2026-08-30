package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ai.GeminiResumeService
import com.example.data.ai.SummaryOption
import com.example.data.model.EducationSuggestions
import com.example.ui.theme.AiSparkleGradientEnd
import com.example.ui.theme.AiSparkleGradientStart
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

// ---------------- 1. AI SUMMARY GENERATOR DIALOG ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSummaryDialog(
    initialRole: String,
    initialSkills: String,
    initialJobDescription: String = "",
    onDismiss: () -> Unit,
    onApplySummary: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val geminiService = remember { GeminiResumeService() }

    var role by remember { mutableStateOf(initialRole) }
    var experienceYears by remember { mutableStateOf("4+ years") }
    var keySkills by remember { mutableStateOf(initialSkills) }
    var targetJobDescription by remember { mutableStateOf(initialJobDescription) }
    var showJdInput by remember { mutableStateOf(initialJobDescription.isNotBlank()) }
    var selectedTone by remember { mutableStateOf("Impact-Driven") }
    var isLoading by remember { mutableStateOf(false) }
    var summaryOptions by remember { mutableStateOf<List<SummaryOption>>(emptyList()) }
    var selectedOptionIndex by remember { mutableStateOf(0) }

    val tones = listOf("Impact-Driven", "Technical", "Executive", "Creative")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("ai_summary_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with AI Sparkle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(AiSparkleGradientStart, AiSparkleGradientEnd)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "AI Sparkle",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "AI Summary Generator",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Optimized for Target Job Description",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Form & Results
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text("Target Role Title *") },
                        placeholder = { Text("e.g. Senior Full-Stack Engineer") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_role_input"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = experienceYears,
                            onValueChange = { experienceYears = it },
                            label = { Text("Experience Level") },
                            placeholder = { Text("e.g. 5+ years") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = keySkills,
                            onValueChange = { keySkills = it },
                            label = { Text("Key Skills Focus") },
                            placeholder = { Text("React, Kotlin, AWS") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )
                    }

                    // Target Job Description Accordion
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showJdInput = !showJdInput }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (targetJobDescription.isNotBlank()) "Target Job Description (Active)" else "Paste Target Job Description (Optional)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (targetJobDescription.isNotBlank()) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            if (showJdInput) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = showJdInput) {
                        OutlinedTextField(
                            value = targetJobDescription,
                            onValueChange = { targetJobDescription = it },
                            label = { Text("Target Job Description / Key Requirements") },
                            placeholder = { Text("Paste requirements or key job responsibilities here for tighter ATS keyword alignment...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_summary_jd_input"),
                            minLines = 3,
                            maxLines = 6
                        )
                    }

                    // Tone selector chips
                    Column {
                        Text(
                            text = "Summary Tone / Focus:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tones.forEach { tone ->
                                FilterChip(
                                    selected = selectedTone == tone,
                                    onClick = { selectedTone = tone },
                                    label = { Text(tone, fontSize = 12.sp) },
                                    leadingIcon = if (selectedTone == tone) {
                                        {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }

                    // Generate Button
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                val effectiveSkills = if (targetJobDescription.isNotBlank()) {
                                    "$keySkills, Keywords: ${targetJobDescription.take(120)}"
                                } else keySkills

                                val result = geminiService.generateSummaries(
                                    role = role,
                                    experienceYears = experienceYears,
                                    keySkills = effectiveSkills,
                                    tone = selectedTone
                                )
                                result.onSuccess { options ->
                                    summaryOptions = options
                                    selectedOptionIndex = 0
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_summaries_button"),
                        enabled = !isLoading && role.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating tailored summaries...")
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate 3 ATS-Tailored Summaries", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Options List
                    if (summaryOptions.isNotEmpty()) {
                        Text(
                            text = "Select a Summary Variation:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        summaryOptions.forEachIndexed { index, option ->
                            val isSelected = selectedOptionIndex == index
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedOptionIndex = index }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = option.tag,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedOptionIndex = index }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = option.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Footer Apply Button
                if (summaryOptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val selectedText = summaryOptions.getOrNull(selectedOptionIndex)?.text
                            if (!selectedText.isNullOrBlank()) {
                                onApplySummary(selectedText)
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("apply_summary_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply Selected Summary to Resume", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- 2. EXPERIENCE AI: SINGLE BULLET OPTIMIZER ----------------
@Composable
fun AiBulletOptimizerDialog(
    jobTitle: String,
    currentBullet: String,
    targetJobDescription: String = "",
    onDismiss: () -> Unit,
    onApplyBullet: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val geminiService = remember { GeminiResumeService() }

    var rawBullet by remember { mutableStateOf(currentBullet) }
    var targetJd by remember { mutableStateOf(targetJobDescription) }
    var showJdInput by remember { mutableStateOf(targetJobDescription.isNotBlank()) }
    var isLoading by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedIndex by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("ai_bullet_dialog"),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(AiSparkleGradientStart, AiSparkleGradientEnd)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "STAR Bullet Optimizer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Action Verb + Context + Metric Formula",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = rawBullet,
                        onValueChange = { rawBullet = it },
                        label = { Text("Draft Achievement / Task") },
                        placeholder = { Text("e.g. built api endpoints and made app faster") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("raw_bullet_input"),
                        minLines = 2,
                        maxLines = 4
                    )

                    // Job Description Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showJdInput = !showJdInput }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (targetJd.isNotBlank()) "Target Job Description (Active)" else "+ Align with Job Description Keywords",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (targetJd.isNotBlank()) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(if (showJdInput) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                    }

                    AnimatedVisibility(visible = showJdInput) {
                        OutlinedTextField(
                            value = targetJd,
                            onValueChange = { targetJd = it },
                            label = { Text("Target Job Description / Keywords") },
                            placeholder = { Text("Paste requirements to tailor bullet phrasing...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                val result = geminiService.enhanceBulletPoint(jobTitle, rawBullet, targetJd)
                                result.onSuccess { res ->
                                    options = res
                                    selectedIndex = 0
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("optimize_bullet_button"),
                        enabled = !isLoading && rawBullet.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Optimizing...")
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Optimize with Action Verbs & Metrics", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (options.isNotEmpty()) {
                        Text(
                            text = "ATS-Enhanced Variations (Google XYZ Formula):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        options.forEachIndexed { idx, opt ->
                            val isSelected = selectedIndex == idx
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedIndex = idx }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedIndex = idx }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = opt,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (options.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val picked = options.getOrNull(selectedIndex)
                            if (!picked.isNullOrBlank()) {
                                onApplyBullet(picked)
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("apply_bullet_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply to Resume", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- 3. EXPERIENCE AI: FULL ROLE BULLET GENERATOR ----------------
@Composable
fun AiExperienceBulletsDialog(
    initialPosition: String,
    initialCompany: String,
    initialJobDescription: String = "",
    onDismiss: () -> Unit,
    onInsertBullets: (List<String>) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val geminiService = remember { GeminiResumeService() }

    var position by remember { mutableStateOf(initialPosition.ifBlank { "Software Engineer" }) }
    var company by remember { mutableStateOf(initialCompany.ifBlank { "Company Name" }) }
    var userNotes by remember { mutableStateOf("") }
    var targetJobDescription by remember { mutableStateOf(initialJobDescription) }
    var showJdInput by remember { mutableStateOf(initialJobDescription.isNotBlank()) }
    var selectedFocus by remember { mutableStateOf("Impact & Metrics") }
    var isLoading by remember { mutableStateOf(false) }
    var generatedBullets by remember { mutableStateOf<List<String>>(emptyList()) }
    val selectedIndices = remember { mutableStateListOf<Int>() }

    val focusAreas = listOf("Impact & Metrics", "System Architecture & Scale", "Leadership & Delivery", "Cost & Efficiency")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("ai_experience_bullets_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(AiSparkleGradientStart, AiSparkleGradientEnd))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("AI Experience Bullets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Tailored to Role & Job Description", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = position,
                            onValueChange = { position = it },
                            label = { Text("Job Position *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = company,
                            onValueChange = { company = it },
                            label = { Text("Company") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = userNotes,
                        onValueChange = { userNotes = it },
                        label = { Text("Your Responsibilities / Draft Notes (Optional)") },
                        placeholder = { Text("e.g. Led migration to microservices, improved database queries, mentored juniors...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    // Job Description Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showJdInput = !showJdInput }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (targetJobDescription.isNotBlank()) "Target Job Description (Active)" else "+ Align with Job Description",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (targetJobDescription.isNotBlank()) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(if (showJdInput) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                    }

                    AnimatedVisibility(visible = showJdInput) {
                        OutlinedTextField(
                            value = targetJobDescription,
                            onValueChange = { targetJobDescription = it },
                            label = { Text("Target Job Description / Requirements") },
                            placeholder = { Text("Paste job posting text here to mirror key requirements and keywords...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6
                        )
                    }

                    // Focus chips
                    Column {
                        Text("Bullet Focus:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            focusAreas.take(2).forEach { f ->
                                FilterChip(
                                    selected = selectedFocus == f,
                                    onClick = { selectedFocus = f },
                                    label = { Text(f, fontSize = 11.sp) }
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            focusAreas.drop(2).forEach { f ->
                                FilterChip(
                                    selected = selectedFocus == f,
                                    onClick = { selectedFocus = f },
                                    label = { Text(f, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                val result = geminiService.generateExperienceBullets(
                                    position = position,
                                    company = company,
                                    userNotes = userNotes,
                                    targetJobDescription = targetJobDescription,
                                    focusArea = selectedFocus
                                )
                                result.onSuccess { bullets ->
                                    generatedBullets = bullets
                                    selectedIndices.clear()
                                    selectedIndices.addAll(bullets.indices)
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_experience_bullets_button"),
                        enabled = !isLoading && position.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing job description & generating...")
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate 4-5 Impact Bullets (XYZ Formula)", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (generatedBullets.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Bullets to Insert (${selectedIndices.size}/${generatedBullets.size}):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = {
                                    if (selectedIndices.size == generatedBullets.size) selectedIndices.clear()
                                    else {
                                        selectedIndices.clear()
                                        selectedIndices.addAll(generatedBullets.indices)
                                    }
                                }
                            ) {
                                Text(if (selectedIndices.size == generatedBullets.size) "Deselect All" else "Select All", fontSize = 12.sp)
                            }
                        }

                        generatedBullets.forEachIndexed { idx, bullet ->
                            val isChecked = selectedIndices.contains(idx)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedIndices.remove(idx)
                                        else selectedIndices.add(idx)
                                    }
                                    .border(
                                        width = if (isChecked) 1.5.dp else 1.dp,
                                        color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { chk ->
                                            if (chk) selectedIndices.add(idx) else selectedIndices.remove(idx)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = bullet,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (generatedBullets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val chosen = selectedIndices.sorted().mapNotNull { generatedBullets.getOrNull(it) }
                            if (chosen.isNotEmpty()) {
                                onInsertBullets(chosen)
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("apply_experience_bullets_button"),
                        enabled = selectedIndices.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Insert ${selectedIndices.size} Selected Bullets into Experience", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- 4. EDUCATION AI SUGGESTER DIALOG ----------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiEducationSuggesterDialog(
    initialDegree: String,
    initialFieldOfStudy: String,
    initialInstitution: String,
    targetRole: String = "",
    initialJobDescription: String = "",
    onDismiss: () -> Unit,
    onApplyEducationContent: (
        bullets: List<String>,
        coursework: List<String>,
        honors: List<String>,
        capstone: List<String>
    ) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val geminiService = remember { GeminiResumeService() }

    var degree by remember { mutableStateOf(initialDegree.ifBlank { "B.S. in Computer Science" }) }
    var fieldOfStudy by remember { mutableStateOf(initialFieldOfStudy.ifBlank { "Computer Science" }) }
    var institution by remember { mutableStateOf(initialInstitution.ifBlank { "University" }) }
    var role by remember { mutableStateOf(targetRole) }
    var targetJobDescription by remember { mutableStateOf(initialJobDescription) }
    var showJdInput by remember { mutableStateOf(initialJobDescription.isNotBlank()) }
    var isLoading by remember { mutableStateOf(false) }

    var suggestions by remember { mutableStateOf<EducationSuggestions?>(null) }
    val selectedBullets = remember { mutableStateListOf<String>() }
    val selectedCoursework = remember { mutableStateListOf<String>() }
    val selectedHonors = remember { mutableStateListOf<String>() }
    val selectedCapstone = remember { mutableStateListOf<String>() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("ai_education_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(AiSparkleGradientStart, AiSparkleGradientEnd))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("AI Education Suggester", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Coursework, Capstone & Honors for Job Target", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = degree,
                        onValueChange = { degree = it },
                        label = { Text("Degree / Program *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fieldOfStudy,
                            onValueChange = { fieldOfStudy = it },
                            label = { Text("Major / Field") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = institution,
                            onValueChange = { institution = it },
                            label = { Text("Institution") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Job Description Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showJdInput = !showJdInput }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (targetJobDescription.isNotBlank()) "Target Job Description (Active)" else "+ Align with Job Description",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (targetJobDescription.isNotBlank()) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(if (showJdInput) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                    }

                    AnimatedVisibility(visible = showJdInput) {
                        OutlinedTextField(
                            value = targetJobDescription,
                            onValueChange = { targetJobDescription = it },
                            label = { Text("Target Job Description / Keywords") },
                            placeholder = { Text("Paste requirements to tailor academic projects and coursework...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6
                        )
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                val result = geminiService.suggestEducation(
                                    degree = degree,
                                    fieldOfStudy = fieldOfStudy,
                                    institution = institution,
                                    targetRole = role,
                                    targetJobDescription = targetJobDescription
                                )
                                result.onSuccess { sug ->
                                    suggestions = sug
                                    selectedBullets.clear()
                                    selectedBullets.addAll(sug.academicBulletPoints)
                                    selectedCoursework.clear()
                                    selectedCoursework.addAll(sug.relevantCoursework)
                                    selectedHonors.clear()
                                    selectedHonors.addAll(sug.honorsAndAchievements)
                                    selectedCapstone.clear()
                                    selectedCapstone.addAll(sug.capstoneHighlights.take(1))
                                }
                                isLoading = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_education_suggestions_button"),
                        enabled = !isLoading && degree.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing curriculum & job requirements...")
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Education Content & Coursework", fontWeight = FontWeight.Bold)
                        }
                    }

                    suggestions?.let { sug ->
                        // 1. Relevant Coursework
                        if (sug.relevantCoursework.isNotEmpty()) {
                            Text("Relevant Coursework (Tap to toggle):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                sug.relevantCoursework.forEach { course ->
                                    val isSelected = selectedCoursework.contains(course)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected) selectedCoursework.remove(course)
                                            else selectedCoursework.add(course)
                                        },
                                        label = { Text(course, fontSize = 11.sp) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }

                        // 2. Academic & Research Bullets
                        if (sug.academicBulletPoints.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Academic & Research Bullets:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                            sug.academicBulletPoints.forEach { bullet ->
                                val isSelected = selectedBullets.contains(bullet)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelected) selectedBullets.remove(bullet)
                                            else selectedBullets.add(bullet)
                                        }
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                        Checkbox(checked = isSelected, onCheckedChange = { chk ->
                                            if (chk) selectedBullets.add(bullet) else selectedBullets.remove(bullet)
                                        })
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(bullet, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        // 3. Honors & Distinctions
                        if (sug.honorsAndAchievements.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Honors & Distinctions:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                sug.honorsAndAchievements.forEach { honor ->
                                    val isSelected = selectedHonors.contains(honor)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected) selectedHonors.remove(honor)
                                            else selectedHonors.add(honor)
                                        },
                                        label = { Text(honor, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        // 4. Capstone Highlights
                        if (sug.capstoneHighlights.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Capstone / Thesis Highlights:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                            sug.capstoneHighlights.forEach { cap ->
                                val isSelected = selectedCapstone.contains(cap)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelected) selectedCapstone.remove(cap)
                                            else selectedCapstone.add(cap)
                                        }
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                        Checkbox(checked = isSelected, onCheckedChange = { chk ->
                                            if (chk) selectedCapstone.add(cap) else selectedCapstone.remove(cap)
                                        })
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(cap, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                if (suggestions != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onApplyEducationContent(
                                selectedBullets.toList(),
                                selectedCoursework.toList(),
                                selectedHonors.toList(),
                                selectedCapstone.toList()
                            )
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("apply_education_content_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply Selected Education Highlights", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- 5. SKILLS AI SUGGESTER DIALOG ----------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSkillsSuggesterDialog(
    jobTitle: String,
    initialJobDescription: String = "",
    onDismiss: () -> Unit,
    onAddSkills: (category: String, skills: List<String>) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val geminiService = remember { GeminiResumeService() }

    var targetRole by remember { mutableStateOf(jobTitle.ifBlank { "Software Engineer" }) }
    var targetJobDescription by remember { mutableStateOf(initialJobDescription) }
    var showJdInput by remember { mutableStateOf(initialJobDescription.isNotBlank()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var suggestedMap by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    val selectedSkills = remember { mutableStateMapOf<String, MutableSet<String>>() }

    // Load initial
    LaunchedEffect(Unit) {
        isLoading = true
        val result = geminiService.suggestSkills(targetRole, targetJobDescription)
        result.onSuccess { map ->
            suggestedMap = map
            map.forEach { (cat, list) ->
                selectedSkills[cat] = list.toMutableSet()
            }
        }
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("ai_skills_dialog"),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(AiSparkleGradientStart, AiSparkleGradientEnd))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("In-Demand Skills & Keywords", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Analyzed for $targetRole & Target JD", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = targetRole,
                            onValueChange = { targetRole = it },
                            label = { Text("Role Title") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    val res = geminiService.suggestSkills(targetRole, targetJobDescription)
                                    res.onSuccess { map ->
                                        suggestedMap = map
                                        selectedSkills.clear()
                                        map.forEach { (cat, list) ->
                                            selectedSkills[cat] = list.toMutableSet()
                                        }
                                    }
                                    isLoading = false
                                }
                            },
                            enabled = !isLoading && targetRole.isNotBlank(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Analyze")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Analyze")
                        }
                    }

                    // Job Description Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showJdInput = !showJdInput }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (targetJobDescription.isNotBlank()) "Target Job Description (Active Analysis)" else "+ Analyze Job Description for ATS Keywords",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (targetJobDescription.isNotBlank()) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(if (showJdInput) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                    }

                    AnimatedVisibility(visible = showJdInput) {
                        OutlinedTextField(
                            value = targetJobDescription,
                            onValueChange = { targetJobDescription = it },
                            label = { Text("Paste Job Description Requirements") },
                            placeholder = { Text("Paste requirements text here to extract exact required skills and libraries...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6
                        )
                    }

                    if (suggestedMap.isNotEmpty()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search skills...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        suggestedMap.forEach { (category, skillsList) ->
                            val filtered = if (searchQuery.isBlank()) skillsList else skillsList.filter { it.contains(searchQuery, ignoreCase = true) }
                            if (filtered.isNotEmpty()) {
                                val currentSelected = selectedSkills.getOrPut(category) { mutableSetOf() }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(
                                        onClick = {
                                            if (currentSelected.containsAll(filtered)) {
                                                currentSelected.removeAll(filtered.toSet())
                                            } else {
                                                currentSelected.addAll(filtered)
                                            }
                                        }
                                    ) {
                                        Text(if (currentSelected.containsAll(filtered)) "Clear" else "Select All", fontSize = 11.sp)
                                    }
                                }

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    filtered.forEach { skill ->
                                        val isSelected = currentSelected.contains(skill)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                if (isSelected) currentSelected.remove(skill)
                                                else currentSelected.add(skill)
                                            },
                                            label = { Text(skill, fontSize = 12.sp) },
                                            leadingIcon = if (isSelected) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                            } else null
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }

                val totalSelected = selectedSkills.values.sumOf { it.size }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        selectedSkills.forEach { (cat, skillSet) ->
                            if (skillSet.isNotEmpty()) {
                                onAddSkills(cat, skillSet.toList())
                            }
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("apply_skills_button"),
                    enabled = totalSelected > 0,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add $totalSelected Selected Skills to Resume", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
