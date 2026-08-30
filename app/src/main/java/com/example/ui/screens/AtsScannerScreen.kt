package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.ai.GeminiResumeService
import com.example.data.model.AtsScoreBreakdown
import com.example.data.model.ResumeFull
import com.example.data.model.SkillCategory
import com.example.ui.components.AtsScoreBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AtsScannerScreen(
    resume: ResumeFull,
    onSaveResume: (ResumeFull) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val geminiService = remember { GeminiResumeService() }

    var jobDescription by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<AtsScoreBreakdown?>(null) }
    var currentResume by remember { mutableStateOf(resume) }

    // Run initial quick analysis
    LaunchedEffect(Unit) {
        isAnalyzing = true
        val result = geminiService.analyzeAts(currentResume, jobDescription)
        result.onSuccess { res ->
            analysisResult = res
            if (currentResume.atsScore != res.overallScore) {
                currentResume = currentResume.copy(atsScore = res.overallScore)
                onSaveResume(currentResume)
            }
        }
        isAnalyzing = false
    }

    Scaffold(
        modifier = modifier.testTag("ats_scanner_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI ATS Resume Scanner", fontWeight = FontWeight.Bold)
                        Text(
                            text = currentResume.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
            // Overall Score Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Applicant Tracking System (ATS) Compatibility",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        val score = analysisResult?.overallScore ?: currentResume.atsScore.coerceAtLeast(82)
                        AtsScoreBadge(score = score, size = 96)

                        Spacer(modifier = Modifier.height(12.dp))

                        val statusText = when {
                            score >= 85 -> "Excellent • High Interview Probability"
                            score >= 70 -> "Good • Minor Keyword Gaps"
                            else -> "Needs Optimization • Low ATS Pass Rate"
                        }
                        Text(
                            text = statusText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (score >= 80) SuccessGreen else WarningAmber
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Category Breakdown Bars
                        analysisResult?.let { res ->
                            ScoreBar(title = "Impact & Action Verbs", score = res.impactScore)
                            Spacer(modifier = Modifier.height(8.dp))
                            ScoreBar(title = "Keyword Matching & Relevance", score = res.keywordScore)
                            Spacer(modifier = Modifier.height(8.dp))
                            ScoreBar(title = "ATS Layout & Parsability", score = res.formatScore)
                            Spacer(modifier = Modifier.height(8.dp))
                            ScoreBar(title = "Brevity & Quantifiable Metrics", score = res.brevityScore)
                        }
                    }
                }
            }

            // Job Description Targeting Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.WorkOutline, contentDescription = null, tint = PrimaryIndigo)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Target Job Description (Optional)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Paste a job description from LinkedIn or Indeed to calculate exact keyword match rate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = jobDescription,
                            onValueChange = { jobDescription = it },
                            placeholder = { Text("Paste Job Description here (e.g. We are looking for a Senior Developer with experience in Kotlin, GraphQL, and AWS...)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("job_description_input"),
                            minLines = 3,
                            maxLines = 6
                        )

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isAnalyzing = true
                                    val res = geminiService.analyzeAts(currentResume, jobDescription)
                                    res.onSuccess { r ->
                                        analysisResult = r
                                        currentResume = currentResume.copy(atsScore = r.overallScore)
                                        onSaveResume(currentResume)
                                    }
                                    isAnalyzing = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("recalculate_ats_button"),
                            enabled = !isAnalyzing,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing with Gemini...")
                            } else {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan Against Job Description", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Missing Keywords
            analysisResult?.missingKeywords?.takeIf { it.isNotEmpty() }?.let { missingList ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Key, contentDescription = null, tint = WarningAmber)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Recommended Missing Keywords", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                text = "Tap any keyword to immediately append it to your skills section.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                missingList.forEach { keyword ->
                                    AssistChip(
                                        onClick = {
                                            val categories = currentResume.skillCategories.toMutableList()
                                            if (categories.isEmpty()) {
                                                categories.add(SkillCategory(categoryName = "Technical Skills", skills = listOf(keyword)))
                                            } else {
                                                val firstCat = categories[0]
                                                if (!firstCat.skills.contains(keyword)) {
                                                    categories[0] = firstCat.copy(skills = firstCat.skills + keyword)
                                                }
                                            }
                                            currentResume = currentResume.copy(skillCategories = categories)
                                            onSaveResume(currentResume)
                                        },
                                        label = { Text("+ $keyword") },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Strengths Checklist
            analysisResult?.strengths?.takeIf { it.isNotEmpty() }?.let { strengths ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Resume Strengths", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                            strengths.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(item, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }

            // Actionable Improvements
            analysisResult?.improvements?.takeIf { it.isNotEmpty() }?.let { improvements ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.TipsAndUpdates, contentDescription = null, tint = TertiaryAmber)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Actionable Improvements", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                            improvements.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = TertiaryAmber, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(item, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreBar(title: String, score: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("$score%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (score >= 80) SuccessGreen else if (score >= 60) PrimaryIndigo else WarningAmber,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
