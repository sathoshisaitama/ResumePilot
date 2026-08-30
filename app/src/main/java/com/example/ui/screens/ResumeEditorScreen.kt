package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import java.util.UUID

enum class EditorTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PERSONAL("Personal", Icons.Outlined.Person),
    SUMMARY("Summary", Icons.Outlined.Article),
    EXPERIENCE("Experience", Icons.Outlined.Work),
    EDUCATION("Education", Icons.Outlined.School),
    SKILLS("Skills", Icons.Outlined.Code),
    PROJECTS("Projects", Icons.Outlined.Folder),
    DESIGN("Design & Theme", Icons.Outlined.Palette)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeEditorScreen(
    initialResume: ResumeFull,
    onSaveResume: (ResumeFull) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigatePreview: (ResumeFull) -> Unit,
    modifier: Modifier = Modifier
) {
    var resumeState by remember { mutableStateOf(initialResume) }
    var selectedTab by remember { mutableStateOf(EditorTab.PERSONAL) }

    // Dialog States
    var showAiSummaryDialog by remember { mutableStateOf(false) }
    var bulletToOptimize by remember { mutableStateOf<Pair<Int, Int>?>(null) } // expIndex, bulletIndex
    var bulletToScoreXyz by remember { mutableStateOf<Triple<Int, Int, String>?>(null) } // expIndex, bulletIndex, text
    var experienceRoleToGenerate by remember { mutableStateOf<Int?>(null) } // expIndex (-1 for new role)
    var educationToSuggest by remember { mutableStateOf<Int?>(null) } // eduIndex (-1 for new edu)
    var showAiSkillsDialog by remember { mutableStateOf(false) }
    var showJobDescriptionDialog by remember { mutableStateOf(false) }
    var showJobKeywordMatcherDialog by remember { mutableStateOf(false) }
    var showTranslateDialog by remember { mutableStateOf(false) }
    var showSaveToast by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag("resume_editor_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        OutlinedTextField(
                            value = resumeState.title,
                            onValueChange = { resumeState = resumeState.copy(title = it) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("resume_title_field")
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
                    // Job Keyword Matcher Chip
                    AssistChip(
                        onClick = { showJobKeywordMatcherDialog = true },
                        label = {
                            Text(
                                "Keywords",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = PrimaryIndigo
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = PrimaryIndigo.copy(alpha = 0.08f)),
                        modifier = Modifier.padding(end = 2.dp)
                    )

                    // Target Job Description quick button
                    AssistChip(
                        onClick = { showJobDescriptionDialog = true },
                        label = {
                            Text(
                                if (resumeState.targetJobDescription.isNotBlank()) "JD: Set" else "JD",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (resumeState.targetJobDescription.isNotBlank()) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.padding(end = 2.dp)
                    )

                    // Translate Button
                    IconButton(
                        onClick = { showTranslateDialog = true },
                        modifier = Modifier.testTag("editor_translate_button")
                    ) {
                        Icon(Icons.Outlined.Translate, contentDescription = "Translate Resume", tint = SecondaryPurple)
                    }

                    // Preview Icon
                    IconButton(
                        onClick = {
                            onSaveResume(resumeState)
                            onNavigatePreview(resumeState)
                        },
                        modifier = Modifier.testTag("editor_preview_button")
                    ) {
                        Icon(Icons.Outlined.Visibility, contentDescription = "Preview", tint = PrimaryIndigo)
                    }

                    // Save Button
                    FilledTonalButton(
                        onClick = {
                            onSaveResume(resumeState)
                            showSaveToast = true
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("editor_save_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        bottomBar = {
            // Tab Selector Scrollable Row
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    edgePadding = 12.dp,
                    divider = {}
                ) {
                    EditorTab.values().forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = { Text(tab.label, fontSize = 12.sp, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                EditorTab.PERSONAL -> PersonalInfoSection(
                    personalInfo = resumeState.personalInfo,
                    onUpdate = { resumeState = resumeState.copy(personalInfo = it) }
                )
                EditorTab.SUMMARY -> SummarySection(
                    summary = resumeState.personalInfo.summary,
                    targetRole = resumeState.personalInfo.jobTitle,
                    skills = resumeState.skillCategories.flatMap { it.skills }.joinToString(", "),
                    onUpdateSummary = { resumeState = resumeState.copy(personalInfo = resumeState.personalInfo.copy(summary = it)) },
                    onOpenAiDialog = { showAiSummaryDialog = true }
                )
                EditorTab.EXPERIENCE -> ExperienceSection(
                    experiences = resumeState.experiences,
                    targetRole = resumeState.personalInfo.jobTitle,
                    onUpdateExperiences = { resumeState = resumeState.copy(experiences = it) },
                    onOptimizeBullet = { expIdx, bulletIdx ->
                        bulletToOptimize = expIdx to bulletIdx
                    },
                    onScoreBulletXyz = { expIdx, bulletIdx, text ->
                        bulletToScoreXyz = Triple(expIdx, bulletIdx, text)
                    },
                    onSuggestRoleBullets = { expIdx ->
                        experienceRoleToGenerate = expIdx
                    }
                )
                EditorTab.EDUCATION -> EducationSection(
                    educations = resumeState.educations,
                    onUpdateEducations = { resumeState = resumeState.copy(educations = it) },
                    onSuggestEducation = { eduIdx ->
                        educationToSuggest = eduIdx
                    }
                )
                EditorTab.SKILLS -> SkillsSection(
                    skillCategories = resumeState.skillCategories,
                    jobTitle = resumeState.personalInfo.jobTitle,
                    onUpdateSkills = { resumeState = resumeState.copy(skillCategories = it) },
                    onOpenAiSuggester = { showAiSkillsDialog = true }
                )
                EditorTab.PROJECTS -> ProjectsSection(
                    projects = resumeState.projects,
                    onUpdateProjects = { resumeState = resumeState.copy(projects = it) }
                )
                EditorTab.DESIGN -> DesignThemeSection(
                    resume = resumeState,
                    onUpdateResume = {
                        resumeState = it
                        onSaveResume(it)
                    }
                )
            }

            // Save Toast Snackbar
            if (showSaveToast) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { showSaveToast = false }) {
                            Text("OK", color = Color.White)
                        }
                    }
                ) {
                    Text("Resume saved successfully!")
                }
                LaunchedEffect(showSaveToast) {
                    kotlinx.coroutines.delay(2000)
                    showSaveToast = false
                }
            }
        }
    }

    // 1. AI Summary Dialog
    if (showAiSummaryDialog) {
        AiSummaryDialog(
            initialRole = resumeState.personalInfo.jobTitle,
            initialSkills = resumeState.skillCategories.flatMap { it.skills }.joinToString(", "),
            initialJobDescription = resumeState.targetJobDescription,
            onDismiss = { showAiSummaryDialog = false },
            onApplySummary = { generatedSummary ->
                resumeState = resumeState.copy(
                    personalInfo = resumeState.personalInfo.copy(summary = generatedSummary)
                )
            }
        )
    }

    // 2. AI Bullet Optimizer Dialog
    bulletToOptimize?.let { (expIdx, bulletIdx) ->
        val currentBullet = resumeState.experiences.getOrNull(expIdx)?.bullets?.getOrNull(bulletIdx) ?: ""
        val currentRole = resumeState.experiences.getOrNull(expIdx)?.position ?: resumeState.personalInfo.jobTitle
        AiBulletOptimizerDialog(
            jobTitle = currentRole,
            currentBullet = currentBullet,
            targetJobDescription = resumeState.targetJobDescription,
            onDismiss = { bulletToOptimize = null },
            onApplyBullet = { optimized ->
                val exps = resumeState.experiences.toMutableList()
                if (expIdx in exps.indices) {
                    val exp = exps[expIdx]
                    val bullets = exp.bullets.toMutableList()
                    if (bulletIdx in bullets.indices) {
                        bullets[bulletIdx] = optimized
                    } else {
                        bullets.add(optimized)
                    }
                    exps[expIdx] = exp.copy(bullets = bullets)
                    resumeState = resumeState.copy(experiences = exps)
                }
                bulletToOptimize = null
            }
        )
    }

    // 3. AI Experience Role Bullets Dialog
    experienceRoleToGenerate?.let { expIdx ->
        val targetExp = if (expIdx >= 0) resumeState.experiences.getOrNull(expIdx) else null
        val initialPos = targetExp?.position ?: resumeState.personalInfo.jobTitle.ifBlank { "Software Engineer" }
        val initialComp = targetExp?.company ?: ""

        AiExperienceBulletsDialog(
            initialPosition = initialPos,
            initialCompany = initialComp,
            initialJobDescription = resumeState.targetJobDescription,
            onDismiss = { experienceRoleToGenerate = null },
            onInsertBullets = { newBullets ->
                val exps = resumeState.experiences.toMutableList()
                if (expIdx >= 0 && expIdx in exps.indices) {
                    val exp = exps[expIdx]
                    exps[expIdx] = exp.copy(bullets = exp.bullets + newBullets)
                } else {
                    // Create a new experience entry with these bullets
                    val newEntry = ExperienceItem(
                        position = initialPos,
                        company = initialComp.ifBlank { "Company Name" },
                        startDate = "Jan 2022",
                        endDate = "Present",
                        isCurrent = true,
                        bullets = newBullets
                    )
                    exps.add(0, newEntry)
                }
                resumeState = resumeState.copy(experiences = exps)
                experienceRoleToGenerate = null
            }
        )
    }

    // 4. AI Education Suggester Dialog
    educationToSuggest?.let { eduIdx ->
        val targetEdu = if (eduIdx >= 0) resumeState.educations.getOrNull(eduIdx) else null
        val initDegree = targetEdu?.degree ?: "B.S. in Computer Science"
        val initInst = targetEdu?.institution ?: "University Name"

        AiEducationSuggesterDialog(
            initialDegree = initDegree,
            initialFieldOfStudy = "Computer Science",
            initialInstitution = initInst,
            targetRole = resumeState.personalInfo.jobTitle,
            initialJobDescription = resumeState.targetJobDescription,
            onDismiss = { educationToSuggest = null },
            onApplyEducationContent = { bullets, coursework, honors, capstone ->
                val edus = resumeState.educations.toMutableList()
                val allHighlights = (bullets + capstone).distinct()
                val courseworkStr = if (coursework.isNotEmpty()) "Relevant Coursework: ${coursework.joinToString(", ")}" else ""
                val honorsStr = if (honors.isNotEmpty()) "Honors: ${honors.joinToString(", ")}" else ""
                val combinedHighlights = (listOfNotNull(
                    courseworkStr.takeIf { it.isNotBlank() },
                    honorsStr.takeIf { it.isNotBlank() }
                ) + allHighlights).distinct()

                if (eduIdx >= 0 && eduIdx in edus.indices) {
                    val current = edus[eduIdx]
                    edus[eduIdx] = current.copy(
                        highlights = (current.highlights + combinedHighlights).distinct()
                    )
                } else {
                    val newEdu = EducationItem(
                        degree = initDegree,
                        institution = initInst,
                        startDate = "2018",
                        endDate = "2022",
                        gpa = "3.8 / 4.0",
                        highlights = combinedHighlights
                    )
                    edus.add(newEdu)
                }
                resumeState = resumeState.copy(educations = edus)
                educationToSuggest = null
            }
        )
    }

    // 5. AI Skills Suggester Dialog
    if (showAiSkillsDialog) {
        AiSkillsSuggesterDialog(
            jobTitle = resumeState.personalInfo.jobTitle,
            initialJobDescription = resumeState.targetJobDescription,
            onDismiss = { showAiSkillsDialog = false },
            onAddSkills = { categoryName, newSkills ->
                val categories = resumeState.skillCategories.toMutableList()
                val existingIdx = categories.indexOfFirst { it.categoryName.equals(categoryName, ignoreCase = true) }
                if (existingIdx >= 0) {
                    val existing = categories[existingIdx]
                    val combined = (existing.skills + newSkills).distinct()
                    categories[existingIdx] = existing.copy(skills = combined)
                } else {
                    categories.add(SkillCategory(categoryName = categoryName, skills = newSkills))
                }
                resumeState = resumeState.copy(skillCategories = categories)
            }
        )
    }

    // 6. Target Job Description Dialog
    if (showJobDescriptionDialog) {
        var tempJd by remember { mutableStateOf(resumeState.targetJobDescription) }
        Dialog(
            onDismissRequest = { showJobDescriptionDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = PrimaryIndigo)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Target Job Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { showJobDescriptionDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Text(
                        text = "Paste the job description or requirements here. All AI generators (Experience bullets, Skills keywords, Education coursework, and Summaries) will analyze this to match keywords & ATS algorithms.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = tempJd,
                        onValueChange = { tempJd = it },
                        placeholder = { Text("Paste job posting requirements, required qualifications, and key skills...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp, max = 300.dp),
                        minLines = 6
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (tempJd.isNotBlank()) {
                            TextButton(onClick = {
                                tempJd = ""
                                resumeState = resumeState.copy(targetJobDescription = "")
                                showJobDescriptionDialog = false
                            }) {
                                Text("Clear JD", color = MaterialTheme.colorScheme.error)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Button(
                            onClick = {
                                resumeState = resumeState.copy(targetJobDescription = tempJd.trim())
                                showJobDescriptionDialog = false
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Job Target", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // 7. Job Keyword Matcher & Gap Highlighter Dialog
    if (showJobKeywordMatcherDialog) {
        JobKeywordMatcherDialog(
            resume = resumeState,
            initialJobDescription = resumeState.targetJobDescription,
            onDismiss = { showJobKeywordMatcherDialog = false },
            onInsertSuggestedBullet = { expIdx, bullet ->
                val exps = resumeState.experiences.toMutableList()
                if (exps.isNotEmpty()) {
                    val targetIdx = expIdx.coerceIn(0, exps.lastIndex)
                    val exp = exps[targetIdx]
                    exps[targetIdx] = exp.copy(bullets = exp.bullets + bullet)
                } else {
                    exps.add(
                        ExperienceItem(
                            position = resumeState.personalInfo.jobTitle.ifBlank { "Software Engineer" },
                            company = "Professional Experience",
                            startDate = "Jan 2022",
                            endDate = "Present",
                            isCurrent = true,
                            bullets = listOf(bullet)
                        )
                    )
                }
                resumeState = resumeState.copy(experiences = exps)
            },
            onAddMissingSkill = { skill ->
                val categories = resumeState.skillCategories.toMutableList()
                if (categories.isNotEmpty()) {
                    val first = categories[0]
                    categories[0] = first.copy(skills = (first.skills + skill).distinct())
                } else {
                    categories.add(SkillCategory(categoryName = "Technical Skills", skills = listOf(skill)))
                }
                resumeState = resumeState.copy(skillCategories = categories)
            },
            onSaveJobDescription = { jd ->
                resumeState = resumeState.copy(targetJobDescription = jd)
            }
        )
    }

    // 8. Google XYZ Bullet Metric Scorer Dialog
    bulletToScoreXyz?.let { (expIdx, bulletIdx, bulletText) ->
        val currentRole = resumeState.experiences.getOrNull(expIdx)?.position ?: resumeState.personalInfo.jobTitle
        BulletMetricScorerDialog(
            initialBullet = bulletText,
            jobTitle = currentRole,
            onDismiss = { bulletToScoreXyz = null },
            onApplyImprovedBullet = { improved ->
                val exps = resumeState.experiences.toMutableList()
                if (expIdx in exps.indices) {
                    val exp = exps[expIdx]
                    val bullets = exp.bullets.toMutableList()
                    if (bulletIdx in bullets.indices) {
                        bullets[bulletIdx] = improved
                    }
                    exps[expIdx] = exp.copy(bullets = bullets)
                    resumeState = resumeState.copy(experiences = exps)
                }
                bulletToScoreXyz = null
            }
        )
    }

    // 9. One-Click Multi-Language Translation Dialog
    if (showTranslateDialog) {
        TranslateResumeDialog(
            resume = resumeState,
            onDismiss = { showTranslateDialog = false },
            onTranslationComplete = { translatedResume ->
                resumeState = translatedResume
                onSaveResume(translatedResume)
            }
        )
    }
}

// ---------------- SECTION 1: PERSONAL INFO ----------------
@Composable
private fun PersonalInfoSection(
    personalInfo: PersonalInfo,
    onUpdate: (PersonalInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Contact & Profile Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Enter your contact details recruiters will use to reach you", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            OutlinedTextField(
                value = personalInfo.fullName,
                onValueChange = { onUpdate(personalInfo.copy(fullName = it)) },
                label = { Text("Full Name *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("full_name_input"),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = personalInfo.jobTitle,
                onValueChange = { onUpdate(personalInfo.copy(jobTitle = it)) },
                label = { Text("Professional Title *") },
                placeholder = { Text("e.g. Senior Full-Stack Engineer") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("job_title_input"),
                singleLine = true
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = personalInfo.email,
                    onValueChange = { onUpdate(personalInfo.copy(email = it)) },
                    label = { Text("Email Address *") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = personalInfo.phone,
                    onValueChange = { onUpdate(personalInfo.copy(phone = it)) },
                    label = { Text("Phone Number") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        item {
            OutlinedTextField(
                value = personalInfo.location,
                onValueChange = { onUpdate(personalInfo.copy(location = it)) },
                label = { Text("Location (City, State / Remote)") },
                placeholder = { Text("San Francisco, CA (or Remote)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = personalInfo.linkedinUrl,
                    onValueChange = { onUpdate(personalInfo.copy(linkedinUrl = it)) },
                    label = { Text("LinkedIn URL") },
                    placeholder = { Text("linkedin.com/in/username") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = personalInfo.githubUrl,
                    onValueChange = { onUpdate(personalInfo.copy(githubUrl = it)) },
                    label = { Text("GitHub / Portfolio") },
                    placeholder = { Text("github.com/username") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        item {
            OutlinedTextField(
                value = personalInfo.websiteUrl,
                onValueChange = { onUpdate(personalInfo.copy(websiteUrl = it)) },
                label = { Text("Personal Website / Blog") },
                placeholder = { Text("https://yourname.dev") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // LinkedIn/Portfolio QR Code Toggle
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Display Header QR Code", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                            Text("Embed a scannable QR code on your PDF & preview", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = personalInfo.includeQrCode,
                            onCheckedChange = { onUpdate(personalInfo.copy(includeQrCode = it)) }
                        )
                    }

                    if (personalInfo.includeQrCode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("QR Code links to:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("LinkedIn", "GitHub", "Website").forEach { target ->
                                FilterChip(
                                    selected = personalInfo.qrCodeTarget == target,
                                    onClick = { onUpdate(personalInfo.copy(qrCodeTarget = target)) },
                                    label = { Text(target, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- SECTION 2: SUMMARY ----------------
@Composable
private fun SummarySection(
    summary: String,
    targetRole: String,
    skills: String,
    onUpdateSummary: (String) -> Unit,
    onOpenAiDialog: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // AI Action Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(AiSparkleGradientStart, AiSparkleGradientEnd))),
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
                            Text("AI Professional Summary Generator", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Auto-generate 3 tailored variations with ATS keywords", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Button(
                        onClick = onOpenAiDialog,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("ai_summary_trigger_button")
                    ) {
                        Text("AI Generate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("Professional Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("A compelling 2-4 sentence overview of your career trajectory and technical strengths.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = summary,
                onValueChange = onUpdateSummary,
                placeholder = { Text("Write or AI-generate your professional summary...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("summary_text_input"),
                minLines = 6,
                maxLines = 10
            )
        }
    }
}

// ---------------- SECTION 3: EXPERIENCE ----------------
@Composable
private fun ExperienceSection(
    experiences: List<ExperienceItem>,
    targetRole: String,
    onUpdateExperiences: (List<ExperienceItem>) -> Unit,
    onOptimizeBullet: (expIndex: Int, bulletIndex: Int) -> Unit,
    onScoreBulletXyz: (expIndex: Int, bulletIndex: Int, bulletText: String) -> Unit,
    onSuggestRoleBullets: (expIndex: Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        // AI Generator Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(AiSparkleGradientStart, AiSparkleGradientEnd))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("AI Experience Suggester", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Generate XYZ-formula bullet points matched to job description", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Button(
                        onClick = { onSuggestRoleBullets(-1) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("ai_suggest_experience_button")
                    ) {
                        Text("AI Bullets", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Work Experience (${experiences.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Highlight impact using action verbs & metrics", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(
                    onClick = {
                        val newItem = ExperienceItem(position = "Software Engineer", company = "Company Name", startDate = "Jan 2022", endDate = "Present", isCurrent = true, bullets = listOf("Architected and delivered core platform features with 99.9% uptime."))
                        onUpdateExperiences(listOf(newItem) + experiences)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Role")
                }
            }
        }

        itemsIndexed(experiences) { expIdx, exp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Position #${expIdx + 1}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // AI Suggest for this specific role
                            TextButton(onClick = { onSuggestRoleBullets(expIdx) }) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryIndigo)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Bullets", fontSize = 12.sp, color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = {
                                    val updated = experiences.toMutableList().also { it.removeAt(expIdx) }
                                    onUpdateExperiences(updated)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Role", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = exp.position,
                            onValueChange = { newPos ->
                                val list = experiences.toMutableList()
                                list[expIdx] = exp.copy(position = newPos)
                                onUpdateExperiences(list)
                            },
                            label = { Text("Job Position *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = exp.company,
                            onValueChange = { newComp ->
                                val list = experiences.toMutableList()
                                list[expIdx] = exp.copy(company = newComp)
                                onUpdateExperiences(list)
                            },
                            label = { Text("Company *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = exp.startDate,
                            onValueChange = { newDate ->
                                val list = experiences.toMutableList()
                                list[expIdx] = exp.copy(startDate = newDate)
                                onUpdateExperiences(list)
                            },
                            label = { Text("Start Date") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = exp.endDate,
                            onValueChange = { newDate ->
                                val list = experiences.toMutableList()
                                list[expIdx] = exp.copy(endDate = newDate)
                                onUpdateExperiences(list)
                            },
                            label = { Text("End Date") },
                            modifier = Modifier.weight(1f),
                            enabled = !exp.isCurrent,
                            singleLine = true
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = exp.isCurrent,
                            onCheckedChange = { isCurr ->
                                val list = experiences.toMutableList()
                                list[expIdx] = exp.copy(isCurrent = isCurr, endDate = if (isCurr) "Present" else "")
                                onUpdateExperiences(list)
                            }
                        )
                        Text("Currently working in this role", fontSize = 13.sp)
                    }

                    // Bullets Section
                    Text("Achievement Bullets (Accomplished [X], measured by [Y], by doing [Z])", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    exp.bullets.forEachIndexed { bulletIdx, bulletText ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = bulletText,
                                onValueChange = { newBullet ->
                                    val list = experiences.toMutableList()
                                    val bullets = exp.bullets.toMutableList()
                                    bullets[bulletIdx] = newBullet
                                    list[expIdx] = exp.copy(bullets = bullets)
                                    onUpdateExperiences(list)
                                },
                                modifier = Modifier.weight(1f),
                                maxLines = 4,
                                placeholder = { Text("e.g. Spearheaded system rewrite...") }
                            )

                            // XYZ Metric Scorer Button
                            IconButton(
                                onClick = { onScoreBulletXyz(expIdx, bulletIdx, bulletText) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Insights, contentDescription = "Score XYZ Formula", tint = MaterialTheme.colorScheme.tertiary)
                            }

                            // AI Optimize Button for this bullet
                            IconButton(
                                onClick = { onOptimizeBullet(expIdx, bulletIdx) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = "AI Optimize Bullet", tint = PrimaryIndigo)
                            }

                            IconButton(
                                onClick = {
                                    val list = experiences.toMutableList()
                                    val bullets = exp.bullets.toMutableList().also { it.removeAt(bulletIdx) }
                                    list[expIdx] = exp.copy(bullets = bullets)
                                    onUpdateExperiences(list)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Delete Bullet", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val list = experiences.toMutableList()
                                val bullets = exp.bullets + "Delivered key architectural milestones reducing processing latency by 30%."
                                list[expIdx] = exp.copy(bullets = bullets)
                                onUpdateExperiences(list)
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Bullet Point")
                        }

                        TextButton(onClick = { onSuggestRoleBullets(expIdx) }) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryIndigo)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Generate Bullets", fontSize = 12.sp, color = PrimaryIndigo)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- SECTION 4: EDUCATION ----------------
@Composable
private fun EducationSection(
    educations: List<EducationItem>,
    onUpdateEducations: (List<EducationItem>) -> Unit,
    onSuggestEducation: (eduIndex: Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        // AI Education Suggester Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(AiSparkleGradientStart, AiSparkleGradientEnd))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("AI Education Suggester", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("Suggest relevant coursework, capstone & honors for job target", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Button(
                        onClick = { onSuggestEducation(-1) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("ai_suggest_education_button")
                    ) {
                        Text("AI Suggest", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Education & Degrees", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FilledTonalButton(
                    onClick = {
                        val newItem = EducationItem(degree = "B.S. in Computer Science", institution = "University Name", startDate = "2018", endDate = "2022")
                        onUpdateEducations(educations + newItem)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Education")
                }
            }
        }

        itemsIndexed(educations) { idx, edu ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Degree #${idx + 1}", fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onSuggestEducation(idx) }) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryIndigo)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AI Highlights", fontSize = 12.sp, color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = {
                                    val list = educations.toMutableList().also { it.removeAt(idx) }
                                    onUpdateEducations(list)
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = edu.degree,
                        onValueChange = { newDeg ->
                            val list = educations.toMutableList()
                            list[idx] = edu.copy(degree = newDeg)
                            onUpdateEducations(list)
                        },
                        label = { Text("Degree / Certificate *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = edu.institution,
                        onValueChange = { newInst ->
                            val list = educations.toMutableList()
                            list[idx] = edu.copy(institution = newInst)
                            onUpdateEducations(list)
                        },
                        label = { Text("Institution / University *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = edu.startDate,
                            onValueChange = { d ->
                                val list = educations.toMutableList()
                                list[idx] = edu.copy(startDate = d)
                                onUpdateEducations(list)
                            },
                            label = { Text("Start Year") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = edu.endDate,
                            onValueChange = { d ->
                                val list = educations.toMutableList()
                                list[idx] = edu.copy(endDate = d)
                                onUpdateEducations(list)
                            },
                            label = { Text("End Year") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = edu.gpa,
                            onValueChange = { g ->
                                val list = educations.toMutableList()
                                list[idx] = edu.copy(gpa = g)
                                onUpdateEducations(list)
                            },
                            label = { Text("GPA") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Academic Highlights, Coursework & Distinctions
                    Text("Coursework, Honors & Capstone Highlights:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    edu.highlights.forEachIndexed { hIdx, highlight ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = highlight,
                                onValueChange = { newH ->
                                    val list = educations.toMutableList()
                                    val hList = edu.highlights.toMutableList()
                                    hList[hIdx] = newH
                                    list[idx] = edu.copy(highlights = hList)
                                    onUpdateEducations(list)
                                },
                                modifier = Modifier.weight(1f),
                                maxLines = 3,
                                placeholder = { Text("e.g. Coursework: Distributed Systems, Algorithms...") }
                            )
                            IconButton(
                                onClick = {
                                    val list = educations.toMutableList()
                                    val hList = edu.highlights.toMutableList().also { it.removeAt(hIdx) }
                                    list[idx] = edu.copy(highlights = hList)
                                    onUpdateEducations(list)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Highlight", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val list = educations.toMutableList()
                                val hList = edu.highlights + "Relevant Coursework: Algorithms, Data Structures, Distributed Computing"
                                list[idx] = edu.copy(highlights = hList)
                                onUpdateEducations(list)
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Highlight / Coursework")
                        }

                        TextButton(onClick = { onSuggestEducation(idx) }) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryIndigo)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Suggest", fontSize = 12.sp, color = PrimaryIndigo)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- SECTION 5: SKILLS ----------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillsSection(
    skillCategories: List<SkillCategory>,
    jobTitle: String,
    onUpdateSkills: (List<SkillCategory>) -> Unit,
    onOpenAiSuggester: () -> Unit
) {
    var newSkillInput by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(AiSparkleGradientStart, AiSparkleGradientEnd))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("AI Skill & Keyword Suggester", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("ATS keyword suggestions for $jobTitle", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Button(
                        onClick = onOpenAiSuggester,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("ai_skills_trigger_button")
                    ) {
                        Text("Suggest Skills", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Categorized Skills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = {
                        val newCat = SkillCategory(categoryName = "New Category", skills = listOf("Skill 1", "Skill 2"))
                        onUpdateSkills(skillCategories + newCat)
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Category")
                }
            }
        }

        itemsIndexed(skillCategories) { catIdx, category ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = category.categoryName,
                            onValueChange = { newName ->
                                val list = skillCategories.toMutableList()
                                list[catIdx] = category.copy(categoryName = newName)
                                onUpdateSkills(list)
                            },
                            label = { Text("Category Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                val list = skillCategories.toMutableList().also { it.removeAt(catIdx) }
                                onUpdateSkills(list)
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    // Skills Chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        category.skills.forEachIndexed { skillIdx, skill ->
                            InputChip(
                                selected = true,
                                onClick = { },
                                label = { Text(skill) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove skill",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                val list = skillCategories.toMutableList()
                                                val skills = category.skills.toMutableList().also { it.removeAt(skillIdx) }
                                                list[catIdx] = category.copy(skills = skills)
                                                onUpdateSkills(list)
                                            }
                                    )
                                }
                            )
                        }
                    }

                    // Add skill quick input
                    var skillDraft by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = skillDraft,
                            onValueChange = { skillDraft = it },
                            placeholder = { Text("Type skill & press Add...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (skillDraft.isNotBlank()) {
                                    val list = skillCategories.toMutableList()
                                    val skills = category.skills + skillDraft.trim()
                                    list[catIdx] = category.copy(skills = skills)
                                    onUpdateSkills(list)
                                    skillDraft = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}

// ---------------- SECTION 6: PROJECTS ----------------
@Composable
private fun ProjectsSection(
    projects: List<ProjectItem>,
    onUpdateProjects: (List<ProjectItem>) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Key Projects & Portfolio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                FilledTonalButton(
                    onClick = {
                        val newProj = ProjectItem(title = "New Project", techStack = "React, Node.js", description = "Engineered real-time features scaling to 10k users.")
                        onUpdateProjects(projects + newProj)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Project")
                }
            }
        }

        itemsIndexed(projects) { idx, proj ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Project #${idx + 1}", fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                        IconButton(
                            onClick = {
                                val list = projects.toMutableList().also { it.removeAt(idx) }
                                onUpdateProjects(list)
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    OutlinedTextField(
                        value = proj.title,
                        onValueChange = { t ->
                            val list = projects.toMutableList()
                            list[idx] = proj.copy(title = t)
                            onUpdateProjects(list)
                        },
                        label = { Text("Project Title *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = proj.techStack,
                        onValueChange = { ts ->
                            val list = projects.toMutableList()
                            list[idx] = proj.copy(techStack = ts)
                            onUpdateProjects(list)
                        },
                        label = { Text("Tech Stack") },
                        placeholder = { Text("e.g. Kotlin, Compose, Room, Retrofit") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = proj.description,
                        onValueChange = { d ->
                            val list = projects.toMutableList()
                            list[idx] = proj.copy(description = d)
                            onUpdateProjects(list)
                        },
                        label = { Text("Description & Highlights") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }
        }
    }
}

// ---------------- SECTION 7: DESIGN & THEME ----------------
@Composable
private fun DesignThemeSection(
    resume: ResumeFull,
    onUpdateResume: (ResumeFull) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 60.dp)
    ) {
        item {
            Text("Resume Template", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Select an ATS-optimized layout format", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(ResumeTemplate.values()) { tpl ->
            val isSelected = resume.template == tpl
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onUpdateResume(resume.copy(template = tpl)) }
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
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
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onUpdateResume(resume.copy(template = tpl)) }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tpl.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text(tpl.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Accent Color Palette", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Custom theme accents applied to headings and visual badges", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            ColorPickerRow(
                selectedColorHex = resume.accentColorHex,
                onColorSelected = { onUpdateResume(resume.copy(accentColorHex = it)) }
            )
        }

        item {
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Typography Style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SansSerif" to "Modern Sans", "Serif" to "Classic Serif", "Mono" to "Tech Monospace").forEach { (fontKey, label) ->
                    val isFontSelected = resume.fontStyle == fontKey
                    FilterChip(
                        selected = isFontSelected,
                        onClick = { onUpdateResume(resume.copy(fontStyle = fontKey)) },
                        label = { Text(label) }
                    )
                }
            }
        }

        item {
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Header QR Code", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Embed scannable LinkedIn/Portfolio QR code in resume header", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = resume.personalInfo.includeQrCode,
                    onCheckedChange = { onUpdateResume(resume.copy(personalInfo = resume.personalInfo.copy(includeQrCode = it))) }
                )
            }
        }

        item {
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Public Live Web Link", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Allow prospective recruiters to view this resume online at resumebuilder.ai/r/${resume.shareSlug}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = resume.isPublic,
                    onCheckedChange = { onUpdateResume(resume.copy(isPublic = it)) }
                )
            }
        }
    }
}
