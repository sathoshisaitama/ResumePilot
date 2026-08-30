package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
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
import com.example.data.ai.GeminiResumeService
import com.example.data.model.ResumeFull
import com.example.ui.theme.AiSparkleGradientEnd
import com.example.ui.theme.AiSparkleGradientStart
import com.example.ui.theme.PrimaryIndigo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverLetterScreen(
    resumes: List<ResumeFull>,
    selectedResumeId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val geminiService = remember { GeminiResumeService() }

    var currentResume by remember {
        mutableStateOf(resumes.find { it.id == selectedResumeId } ?: resumes.firstOrNull())
    }

    var companyName by remember { mutableStateOf("Google") }
    var jobTitle by remember { mutableStateOf(currentResume?.personalInfo?.jobTitle ?: "Senior Software Engineer") }
    var jobDescription by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf("Confident & Tailored") }
    var isGenerating by remember { mutableStateOf(false) }
    var coverLetterText by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.testTag("cover_letter_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Cover Letter Writer", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Powered by Google Gemini",
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 40.dp, top = 8.dp)
        ) {
            // Input Details Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                Text("Generate Targeted Cover Letter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Matches your exact resume credentials to the target company", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = companyName,
                                onValueChange = { companyName = it },
                                label = { Text("Company Name *") },
                                placeholder = { Text("e.g. Stripe") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = jobTitle,
                                onValueChange = { jobTitle = it },
                                label = { Text("Role Title *") },
                                placeholder = { Text("e.g. Staff Engineer") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = jobDescription,
                            onValueChange = { jobDescription = it },
                            label = { Text("Job Description / Keywords") },
                            placeholder = { Text("Paste key requirements or company mission highlights...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )

                        // Tone Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Confident & Tailored", "Enthusiastic", "Executive").forEach { tone ->
                                FilterChip(
                                    selected = selectedTone == tone,
                                    onClick = { selectedTone = tone },
                                    label = { Text(tone, fontSize = 12.sp) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val res = currentResume
                                if (res != null) {
                                    coroutineScope.launch {
                                        isGenerating = true
                                        val result = geminiService.generateCoverLetter(
                                            resume = res,
                                            companyName = companyName,
                                            jobTitle = jobTitle,
                                            jobRequirements = jobDescription
                                        )
                                        result.onSuccess { text ->
                                            coverLetterText = text
                                        }
                                        isGenerating = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("generate_cover_letter_button"),
                            enabled = !isGenerating && companyName.isNotBlank() && jobTitle.isNotBlank() && currentResume != null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Writing Cover Letter...")
                            } else {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate Cover Letter with AI", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Cover Letter Result Section
            if (coverLetterText.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Generated Cover Letter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Cover Letter", coverLetterText)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Cover letter copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy")
                                    }

                                    IconButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, coverLetterText)
                                                putExtra(Intent.EXTRA_SUBJECT, "Cover Letter - $jobTitle at $companyName")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Cover Letter"))
                                        }
                                    ) {
                                        Icon(Icons.Outlined.Share, contentDescription = "Share")
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = coverLetterText,
                                onValueChange = { coverLetterText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("cover_letter_editor"),
                                minLines = 12,
                                maxLines = 24
                            )
                        }
                    }
                }
            }
        }
    }
}
