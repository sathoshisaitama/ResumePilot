package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ResumeFull
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AtsScoreBadge(
    score: Int,
    modifier: Modifier = Modifier,
    size: Int = 42
) {
    val animatedScore by animateFloatAsState(targetValue = score / 100f, label = "score_anim")

    val scoreColor = when {
        score >= 85 -> SuccessGreen
        score >= 70 -> PrimaryIndigo
        score >= 50 -> WarningAmber
        else -> ErrorRed
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(scoreColor.copy(alpha = 0.12f))
            .border(1.5.dp, scoreColor.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$score",
                fontSize = (size * 0.35).sp,
                fontWeight = FontWeight.ExtraBold,
                color = scoreColor
            )
            if (size >= 48) {
                Text(
                    text = "ATS",
                    fontSize = (size * 0.16).sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
            }
        }
    }
}

@Composable
fun ColorPickerRow(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val presetColors = listOf(
        "#4F46E5" to "Indigo",
        "#0D9488" to "Teal",
        "#881337" to "Bordeaux",
        "#2563EB" to "Royal Blue",
        "#7C3AED" to "Violet",
        "#059669" to "Emerald",
        "#334155" to "Slate",
        "#B45309" to "Amber",
        "#0284C7" to "Sky Blue",
        "#475569" to "Charcoal"
    )

    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(presetColors.size) { index ->
            val (hex, name) = presetColors[index]
            val color = try {
                Color(android.graphics.Color.parseColor(hex))
            } catch (e: Exception) {
                Color(0xFF4F46E5)
            }
            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onColorSelected(hex) }
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected $name",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeCardItem(
    resume: ResumeFull,
    onEdit: () -> Unit,
    onPreview: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onAtsScan: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(resume.lastUpdated))
    val accentColor = try {
        Color(android.graphics.Color.parseColor(resume.accentColorHex))
    } catch (e: Exception) {
        PrimaryIndigo
    }

    Card(
        onClick = onEdit,
        modifier = modifier
            .fillMaxWidth()
            .testTag("resume_card_${resume.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row: Title, Template pill, ATS Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = resume.title.ifBlank { "Untitled Resume" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${resume.personalInfo.fullName.ifBlank { "Candidate" }} • ${resume.personalInfo.jobTitle.ifBlank { "Professional" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                AtsScoreBadge(score = if (resume.atsScore > 0) resume.atsScore else 84, size = 42)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = resume.template.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (resume.isPublic) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (resume.isPublic) Icons.Default.Public else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (resume.isPublic) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (resume.isPublic) "Public Web Link" else "Private",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (resume.isPublic) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Updated $dateStr",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onPreview,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Preview", fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = onAtsScan,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Outlined.Analytics, contentDescription = "ATS Score", tint = PrimaryIndigo)
                    }
                }

                Row {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicate", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/**
 * High-Priority Dashboard Widget: Live Resume Health & Quick Actions
 */
@Composable
fun ResumeHealthDashboardWidget(
    activeResume: ResumeFull,
    onImproveResume: () -> Unit,
    onAnalyzeJob: () -> Unit,
    onImportProfile: () -> Unit,
    onChangeTemplate: () -> Unit,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val analysis = remember(activeResume) {
        com.example.util.AtsAnalyzer.analyze(activeResume, activeResume.targetJobDescription)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("resume_health_widget"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (analysis.overallScore >= 80) SuccessGreen else if (analysis.overallScore >= 60) WarningAmber else ErrorRed
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Resume Health",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryIndigo.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = activeResume.title.ifBlank { "Active Resume" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryIndigo,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ATS Score Metric
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ATS Score",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${analysis.overallScore}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (analysis.overallScore >= 80) SuccessGreen else if (analysis.overallScore >= 60) WarningAmber else ErrorRed
                            )
                            Text(
                                text = "/100",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 2.dp, start = 1.dp)
                            )
                        }
                    }
                }

                // Job Match Metric
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Job Match",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (activeResume.targetJobDescription.isNotBlank()) "${analysis.jobMatchPercentage}%" else "${analysis.overallScore}%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryIndigo
                        )
                    }
                }

                // Missing Keywords Metric
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Keywords",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${analysis.missingKeywords.size} missing",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (analysis.missingKeywords.isEmpty()) SuccessGreen else WarningAmber
                        )
                    }
                }

                // Suggestions Metric
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Suggestions",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${analysis.recommendations.size} tips",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryIndigo
                        )
                    }
                }
            }

            // Keyword preview tag list if any missing
            if (analysis.missingKeywords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Keywords: ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val missingList = analysis.missingKeywords.take(4)
                        items(missingList) { kw ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = WarningAmber.copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, WarningAmber.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "+ $kw",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WarningAmber,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Quick Actions Title
            Text(
                text = "Quick Actions",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 5 Action Buttons Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Improve Resume
                FilledTonalButton(
                    onClick = onImproveResume,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Improve", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                // Analyze Job
                FilledTonalButton(
                    onClick = onAnalyzeJob,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Analyze", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                // Import Profile
                FilledTonalButton(
                    onClick = onImportProfile,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Import", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                // Change Template
                FilledTonalButton(
                    onClick = onChangeTemplate,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Template", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                // Export PDF
                Button(
                    onClick = onExportPdf,
                    modifier = Modifier.weight(1.1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                    }
                }
            }
        }
    }
}

