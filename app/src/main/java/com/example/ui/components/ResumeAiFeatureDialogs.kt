package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ai.GeminiResumeService
import com.example.data.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 1. AUTOMATED JOB DESCRIPTION MATCHER & KEYWORD GAP HIGHLIGHTER
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JobKeywordMatcherDialog(
    resume: ResumeFull,
    initialJobDescription: String,
    onDismiss: () -> Unit,
    onInsertSuggestedBullet: (experienceIndex: Int, bullet: String) -> Unit,
    onAddMissingSkill: (skill: String) -> Unit,
    onSaveJobDescription: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val geminiService = remember { GeminiResumeService() }

    var jobDescriptionInput by remember { mutableStateOf(initialJobDescription) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<JobKeywordGapResult?>(null) }
    var selectedExpIndex by remember { mutableIntStateOf(0) }

    // Auto-analyze on launch if job description already present
    LaunchedEffect(Unit) {
        if (jobDescriptionInput.isNotBlank()) {
            isAnalyzing = true
            coroutineScope.launch {
                val res = geminiService.matchJobDescriptionKeywords(resume, jobDescriptionInput)
                result = res.getOrNull()
                isAnalyzing = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("job_keyword_matcher_dialog"),
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(AiSparkleGradientStart, AiSparkleGradientEnd))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Job Keyword Gap Matcher", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Real-time ATS alignment & 1-tap insertion", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = jobDescriptionInput,
                            onValueChange = { jobDescriptionInput = it },
                            label = { Text("Paste Target Job Posting / Requirements") },
                            placeholder = { Text("Paste qualifications, requirements, tech stack (e.g., '5+ years Kotlin, Kubernetes, microservices architecture, system design...')...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 110.dp, max = 150.dp)
                                .testTag("jd_input_field"),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryIndigo
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                if (jobDescriptionInput.isNotBlank()) {
                                    isAnalyzing = true
                                    onSaveJobDescription(jobDescriptionInput)
                                    coroutineScope.launch {
                                        val res = geminiService.matchJobDescriptionKeywords(resume, jobDescriptionInput)
                                        result = res.getOrNull()
                                        isAnalyzing = false
                                    }
                                }
                            },
                            enabled = jobDescriptionInput.isNotBlank() && !isAnalyzing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("analyze_keywords_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryIndigo
                            )
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing Keyword Alignment with Gemini...")
                            } else {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan & Match Job Keywords", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (result != null) {
                        val gap = result!!
                        item {
                            // Match Score Banner
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (gap.matchPercentage >= 75) SuccessGreen.copy(alpha = 0.08f) else WarningAmber.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (gap.matchPercentage >= 80) "Strong ATS Match" else "Keyword Gap Identified",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (gap.matchPercentage >= 75) SuccessGreen else WarningAmber
                                        )
                                        Text(
                                            text = gap.recruiterAdvice,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(58.dp)
                                            .clip(CircleShape)
                                            .background(if (gap.matchPercentage >= 75) SuccessGreen else WarningAmber),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${gap.matchPercentage}%",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Matched Keywords Chips
                        if (gap.matchedKeywords.isNotEmpty()) {
                            item {
                                Text("✅ Matched Keywords in Resume (${gap.matchedKeywords.size})", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    gap.matchedKeywords.forEach { kw ->
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(kw, fontSize = 11.5.sp, color = SuccessGreen, fontWeight = FontWeight.SemiBold) },
                                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(13.dp)) },
                                            colors = AssistChipDefaults.assistChipColors(containerColor = SuccessGreen.copy(alpha = 0.1f)),
                                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
                                        )
                                    }
                                }
                            }
                        }

                        // Missing Keywords with 1-Tap Insertion
                        if (gap.missingKeywords.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "🎯 Missing High-Impact Keywords (${gap.missingKeywords.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = ErrorRed
                                    )
                                    if (resume.experiences.isNotEmpty()) {
                                        Text("Targeting: ${resume.experiences.getOrNull(selectedExpIndex)?.company ?: "Experience"}", fontSize = 11.sp, color = PrimaryIndigo, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Text("Tap below to instantly insert optimized STAR bullet points or skills into your resume draft:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            items(gap.missingKeywords) { item ->
                                var wasAdded by remember { mutableStateOf(false) }

                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(PrimaryIndigo.copy(alpha = 0.3f), PrimaryIndigo.copy(alpha = 0.1f)))),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(item.keyword, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryIndigo)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text(item.category, fontSize = 10.sp) },
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.height(24.dp)
                                                )
                                            }

                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text(item.contextImportance, fontSize = 10.sp, color = if (item.contextImportance == "High") ErrorRed else WarningAmber, fontWeight = FontWeight.Bold) },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = (if (item.contextImportance == "High") ErrorRed else WarningAmber).copy(alpha = 0.12f)),
                                                modifier = Modifier.height(24.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "\"${item.suggestedBullet}\"",
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    onAddMissingSkill(item.keyword)
                                                    wasAdded = true
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("+ Add to Skills", fontSize = 11.sp)
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Button(
                                                onClick = {
                                                    onInsertSuggestedBullet(selectedExpIndex, item.suggestedBullet)
                                                    wasAdded = true
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = if (wasAdded) SuccessGreen else PrimaryIndigo),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(if (wasAdded) Icons.Default.Check else Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (wasAdded) "Inserted!" else "Insert STAR Bullet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 2. MULTI-FORMAT RESUME IMPORTER (PARSES RAW TEXT / PDF TEXT INTO RESUMEFULL)
 */
@Composable
fun ImportResumeDialog(
    onDismiss: () -> Unit,
    onImportSuccess: (ResumeFull) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val geminiService = remember { GeminiResumeService() }

    var rawResumeText by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }

    val sampleResumeText = """
        Satoshi Nakamoto
        satoshi@example.com | (555) 234-5678 | San Francisco, CA
        linkedin.com/in/satoshinakamoto | github.com/satoshi

        SUMMARY
        Innovative Principal Software Engineer with 8+ years of experience architecting high-throughput distributed systems, cryptographic protocols, and cloud applications.

        EXPERIENCE
        Principal Distributed Systems Architect | Nexus Cloud Labs (2021 - Present)
        • Spearheaded the re-architecture of core distributed messaging queues, reducing latency by 45% across 200M+ daily events.
        • Orchestrated multi-region Kubernetes deployments resulting in 99.999% system availability and $320K annual cloud savings.
        • Mentored 14 senior and staff engineers across 3 time zones in microservices best practices.

        Lead Software Engineer | Apex FinTech Solutions (2018 - 2021)
        • Engineered real-time cryptographic transaction engine processing 15,000 TPS with zero security incidents.
        • Migrated legacy monolithic Java backend to modern Kotlin coroutines microservices, accelerating feature velocity by 3x.

        EDUCATION
        B.S. in Computer Science | Stanford University (2014 - 2018)
        • GPA: 3.92 • Honors in Distributed Computing

        SKILLS
        • Technical Skills: Kotlin, Java, Go, Python, Distributed Systems, Cryptography, REST APIs, GraphQL
        • Tools & Cloud: AWS, Kubernetes, Docker, PostgreSQL, Redis, Kafka, Terraform, CI/CD
    """.trimIndent()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("import_resume_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(AiSparkleGradientStart, PrimaryIndigo))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Import Existing Resume", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Gemini extracts all fields into structured format", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Paste resume content below:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    TextButton(
                        onClick = { rawResumeText = sampleResumeText },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Fill Sample Resume", fontSize = 11.sp, color = PrimaryIndigo)
                    }
                }

                OutlinedTextField(
                    value = rawResumeText,
                    onValueChange = { rawResumeText = it },
                    placeholder = { Text("Paste any text from your PDF, Word doc, or LinkedIn profile here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("import_raw_text_field"),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (rawResumeText.isNotBlank()) {
                            isImporting = true
                            coroutineScope.launch {
                                val result = geminiService.parseResumeFromRawText(rawResumeText)
                                val parsed = result.getOrNull()
                                isImporting = false
                                if (parsed != null) {
                                    onImportSuccess(parsed)
                                    onDismiss()
                                }
                            }
                        }
                    },
                    enabled = rawResumeText.isNotBlank() && !isImporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_import_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Parsing & Structuring with Gemini...")
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Extract & Build Resume", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 3. BULLET POINT METRIC & GOOGLE XYZ SCORER
 */
@Composable
fun BulletMetricScorerDialog(
    initialBullet: String,
    jobTitle: String,
    onDismiss: () -> Unit,
    onApplyImprovedBullet: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val geminiService = remember { GeminiResumeService() }

    var bulletText by remember { mutableStateOf(initialBullet) }
    var isScoring by remember { mutableStateOf(false) }
    var scoreResult by remember { mutableStateOf<BulletMetricScore?>(null) }

    LaunchedEffect(Unit) {
        if (bulletText.isNotBlank()) {
            isScoring = true
            coroutineScope.launch {
                val res = geminiService.evaluateBulletPointMetrics(bulletText, jobTitle)
                scoreResult = res.getOrNull()
                isScoring = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("bullet_metric_scorer_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(PrimaryIndigo, SecondaryPurple))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Insights, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Google XYZ Bullet Scorer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Accomplished [X] measured by [Y] by doing [Z]", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = bulletText,
                            onValueChange = { bulletText = it },
                            label = { Text("Active Bullet Point") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 90.dp)
                                .testTag("active_bullet_field"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (bulletText.isNotBlank()) {
                                    isScoring = true
                                    coroutineScope.launch {
                                        val res = geminiService.evaluateBulletPointMetrics(bulletText, jobTitle)
                                        scoreResult = res.getOrNull()
                                        isScoring = false
                                    }
                                }
                            },
                            enabled = bulletText.isNotBlank() && !isScoring,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isScoring) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Evaluating XYZ Metrics...")
                            } else {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Re-score & Generate Metrics", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (scoreResult != null) {
                        val score = scoreResult!!
                        item {
                            // XYZ Breakdown Card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Score: ${score.score}/10", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = PrimaryIndigo)
                                        AssistChip(
                                            onClick = {},
                                            label = { Text(score.ratingBadge, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = when (score.ratingBadge) {
                                                    "Strong XYZ" -> SuccessGreen.copy(alpha = 0.15f)
                                                    "Moderate" -> WarningAmber.copy(alpha = 0.15f)
                                                    else -> ErrorRed.copy(alpha = 0.15f)
                                                }
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 3 Checklist Pillars
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        PillarBadge("Action Verb", score.hasActionVerb, Modifier.weight(1f))
                                        PillarBadge("Metric / Numbers", score.hasQuantifiableMetric, Modifier.weight(1f))
                                        PillarBadge("Business Result", score.hasBusinessResult, Modifier.weight(1f))
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(score.feedbackExplanation, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        if (score.improvedVariations.isNotEmpty()) {
                            item {
                                Text("✨ High-Impact Metric Rewrites:", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                Text("Tap any variation to immediately apply it:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            items(score.improvedVariations) { opt ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onApplyImprovedBullet(opt)
                                            onDismiss()
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = PrimaryIndigo.copy(alpha = 0.05f)),
                                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(PrimaryIndigo.copy(alpha = 0.4f), PrimaryIndigo.copy(alpha = 0.2f))))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(opt, fontSize = 12.5.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Select", tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun PillarBadge(label: String, passed: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (passed) SuccessGreen.copy(alpha = 0.12f) else ErrorRed.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                if (passed) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (passed) SuccessGreen else ErrorRed,
                modifier = Modifier.size(14.dp)
            )
            Text(label, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = if (passed) SuccessGreen else ErrorRed, textAlign = TextAlign.Center)
        }
    }
}

/**
 * 4. ONE-CLICK MULTI-LANGUAGE TRANSLATOR
 */
@Composable
fun TranslateResumeDialog(
    resume: ResumeFull,
    onDismiss: () -> Unit,
    onTranslationComplete: (ResumeFull) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val geminiService = remember { GeminiResumeService() }

    var selectedLang by remember { mutableStateOf(TargetLanguage.SPANISH) }
    var isTranslating by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .testTag("translate_resume_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(SecondaryPurple, PrimaryIndigo))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Translate, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Translate Resume", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Multi-language localization with Gemini", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Select target language for international applications:", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(TargetLanguage.values()) { lang ->
                        val isSelected = (selectedLang == lang)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLang = lang },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryIndigo.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(PrimaryIndigo, SecondaryPurple))) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(lang.flag, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(lang.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("ISO Code: ${lang.code}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedLang = lang }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        isTranslating = true
                        coroutineScope.launch {
                            val res = geminiService.translateEntireResume(resume, selectedLang)
                            val translated = res.getOrNull()
                            isTranslating = false
                            if (translated != null) {
                                onTranslationComplete(translated)
                                onDismiss()
                            }
                        }
                    },
                    enabled = !isTranslating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_translation_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    if (isTranslating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Translating to ${selectedLang.displayName}...")
                    } else {
                        Icon(Icons.Outlined.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Translate to ${selectedLang.displayName}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
