package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ResumeFull
import com.example.data.model.ResumeTemplate
import com.example.util.QrCodeGenerator

@Composable
fun ResumeSheetPreview(
    resume: ResumeFull,
    modifier: Modifier = Modifier
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(resume.accentColorHex))
    } catch (e: Exception) {
        Color(0xFF4F46E5)
    }

    val fontFamily = when (resume.fontStyle) {
        "Serif" -> FontFamily.Serif
        "Mono" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(12.dp))
            .testTag("resume_sheet_preview"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        when (resume.template) {
            ResumeTemplate.MODERN -> ModernTemplateLayout(resume, accentColor, fontFamily)
            ResumeTemplate.CLASSIC_ATS -> ClassicAtsTemplateLayout(resume, accentColor, fontFamily)
            ResumeTemplate.MINIMALIST -> MinimalistTemplateLayout(resume, accentColor, fontFamily)
            ResumeTemplate.EXECUTIVE -> ExecutiveTemplateLayout(resume, accentColor, fontFamily)
            ResumeTemplate.TECH_SLATE -> TechSlateTemplateLayout(resume, accentColor, fontFamily)
        }
    }
}

// ---------------- MODERN TEMPLATE ----------------
@Composable
private fun ModernTemplateLayout(
    resume: ResumeFull,
    accentColor: Color,
    fontFamily: FontFamily
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resume.personalInfo.fullName.ifBlank { "Your Full Name" },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    fontFamily = fontFamily
                )
                Text(
                    text = resume.personalInfo.jobTitle.ifBlank { "Your Professional Title" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155),
                    fontFamily = fontFamily
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                ResumeQrThumbnail(resume, sizeDp = 42)
                Spacer(modifier = Modifier.width(8.dp))
                // Initials Avatar Badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = resume.personalInfo.fullName.split(" ")
                            .mapNotNull { it.firstOrNull()?.toString() }
                            .take(2).joinToString("").ifBlank { "ME" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Contact info bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (resume.personalInfo.email.isNotBlank()) {
                ContactMiniItem(icon = Icons.Outlined.Email, text = resume.personalInfo.email)
            }
            if (resume.personalInfo.phone.isNotBlank()) {
                ContactMiniItem(icon = Icons.Outlined.Phone, text = resume.personalInfo.phone)
            }
            if (resume.personalInfo.location.isNotBlank()) {
                ContactMiniItem(icon = Icons.Outlined.LocationOn, text = resume.personalInfo.location)
            }
            if (resume.personalInfo.linkedinUrl.isNotBlank()) {
                ContactMiniItem(icon = Icons.Outlined.Link, text = resume.personalInfo.linkedinUrl)
            }
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp), color = accentColor.copy(alpha = 0.3f), thickness = 1.5.dp)

        // Summary
        if (resume.personalInfo.summary.isNotBlank()) {
            ModernSectionHeader("SUMMARY", accentColor)
            Text(
                text = resume.personalInfo.summary,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = Color(0xFF334155),
                fontFamily = fontFamily,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Experience
        if (resume.experiences.isNotEmpty()) {
            ModernSectionHeader("EXPERIENCE", accentColor)
            resume.experiences.forEach { exp ->
                Column(modifier = Modifier.padding(bottom = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${exp.position} • ${exp.company}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            fontFamily = fontFamily
                        )
                        Text(
                            text = "${exp.startDate} - ${if (exp.isCurrent) "Present" else exp.endDate}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                    if (exp.location.isNotBlank()) {
                        Text(
                            text = exp.location,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    exp.bullets.forEach { bullet ->
                        Row(modifier = Modifier.padding(vertical = 1.dp)) {
                            Text("• ", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(bullet, fontSize = 10.5.sp, lineHeight = 15.sp, color = Color(0xFF334155), fontFamily = fontFamily)
                        }
                    }
                }
            }
        }

        // Education
        if (resume.educations.isNotEmpty()) {
            ModernSectionHeader("EDUCATION", accentColor)
            resume.educations.forEach { edu ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${edu.degree}${if (edu.fieldOfStudy.isNotBlank()) ", ${edu.fieldOfStudy}" else ""}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = edu.institution,
                            fontSize = 10.5.sp,
                            color = Color(0xFF475569)
                        )
                        if (edu.gpa.isNotBlank() || edu.honors.isNotBlank()) {
                            Text(
                                text = listOfNotNull(edu.gpa.takeIf { it.isNotBlank() }?.let { "GPA: $it" }, edu.honors.takeIf { it.isNotBlank() }).joinToString(" • "),
                                fontSize = 9.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        edu.highlights.forEach { hl ->
                            Row(modifier = Modifier.padding(top = 1.dp)) {
                                Text("• ", color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(hl, fontSize = 9.5.sp, lineHeight = 13.sp, color = Color(0xFF475569), fontFamily = fontFamily)
                            }
                        }
                    }
                    Text(
                        text = "${edu.startDate} - ${edu.endDate}",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        // Skills
        if (resume.skillCategories.isNotEmpty()) {
            ModernSectionHeader("SKILLS & TOOLS", accentColor)
            resume.skillCategories.forEach { cat ->
                Row(modifier = Modifier.padding(bottom = 4.dp)) {
                    Text(
                        text = "${cat.categoryName}: ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = cat.skills.joinToString(", "),
                        fontSize = 10.5.sp,
                        color = Color(0xFF334155),
                        fontFamily = fontFamily
                    )
                }
            }
        }

        // Projects
        if (resume.projects.isNotEmpty()) {
            ModernSectionHeader("FEATURED PROJECTS", accentColor)
            resume.projects.forEach { proj ->
                Column(modifier = Modifier.padding(bottom = 6.dp)) {
                    Text(
                        text = "${proj.title} [${proj.techStack}]",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    if (proj.description.isNotBlank()) {
                        Text(
                            text = proj.description,
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}

// ---------------- CLASSIC ATS TEMPLATE ----------------
@Composable
private fun ClassicAtsTemplateLayout(
    resume: ResumeFull,
    accentColor: Color,
    fontFamily: FontFamily
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = resume.personalInfo.fullName.ifBlank { "Your Name" }.uppercase(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            fontFamily = fontFamily,
            letterSpacing = 1.sp
        )
        Text(
            text = resume.personalInfo.jobTitle.ifBlank { "Professional Title" },
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF334155),
            fontFamily = fontFamily
        )

        val contacts = listOfNotNull(
            resume.personalInfo.location.takeIf { it.isNotBlank() },
            resume.personalInfo.phone.takeIf { it.isNotBlank() },
            resume.personalInfo.email.takeIf { it.isNotBlank() },
            resume.personalInfo.linkedinUrl.takeIf { it.isNotBlank() }
        ).joinToString("  |  ")

        if (contacts.isNotBlank()) {
            Text(
                text = contacts,
                fontSize = 9.5.sp,
                color = Color(0xFF475569),
                modifier = Modifier.padding(top = 4.dp),
                textAlign = TextAlign.Center
            )
        }

        Divider(modifier = Modifier.padding(top = 10.dp, bottom = 12.dp), color = Color.Black, thickness = 1.dp)

        // Summary
        if (resume.personalInfo.summary.isNotBlank()) {
            AtsSectionHeader("PROFESSIONAL SUMMARY")
            Text(
                text = resume.personalInfo.summary,
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = Color.Black,
                fontFamily = fontFamily,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            )
        }

        // Experience
        if (resume.experiences.isNotEmpty()) {
            AtsSectionHeader("WORK EXPERIENCE")
            resume.experiences.forEach { exp ->
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${exp.position}, ${exp.company}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "${exp.startDate} - ${if (exp.isCurrent) "Present" else exp.endDate}",
                            fontSize = 10.sp,
                            color = Color.Black
                        )
                    }
                    exp.bullets.forEach { bullet ->
                        Row(modifier = Modifier.padding(vertical = 1.dp)) {
                            Text("• ", color = Color.Black, fontSize = 10.5.sp)
                            Text(bullet, fontSize = 10.sp, lineHeight = 14.sp, color = Color.Black)
                        }
                    }
                }
            }
        }

        // Education
        if (resume.educations.isNotEmpty()) {
            AtsSectionHeader("EDUCATION")
            resume.educations.forEach { edu ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${edu.degree}${if (edu.fieldOfStudy.isNotBlank()) ", ${edu.fieldOfStudy}" else ""} — ${edu.institution}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "${edu.startDate} - ${edu.endDate}",
                        fontSize = 10.sp,
                        color = Color.Black
                    )
                }
            }
        }

        // Skills
        if (resume.skillCategories.isNotEmpty()) {
            AtsSectionHeader("TECHNICAL & PROFESSIONAL SKILLS")
            resume.skillCategories.forEach { cat ->
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)) {
                    Text("${cat.categoryName}: ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(cat.skills.joinToString(", "), fontSize = 10.sp, color = Color.Black)
                }
            }
        }
    }
}

// ---------------- EXECUTIVE TEMPLATE ----------------
@Composable
private fun ExecutiveTemplateLayout(
    resume: ResumeFull,
    accentColor: Color,
    fontFamily: FontFamily
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Colored Executive Top Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(accentColor)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = resume.personalInfo.fullName.ifBlank { "Executive Candidate" },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = fontFamily
            )
            Text(
                text = resume.personalInfo.jobTitle.ifBlank { "Principal Director" },
                fontSize = 13.sp,
                color = Color(0xFFF1F5F9),
                fontFamily = fontFamily
            )
            Spacer(modifier = Modifier.height(6.dp))
            val contacts = listOfNotNull(
                resume.personalInfo.email.takeIf { it.isNotBlank() },
                resume.personalInfo.phone.takeIf { it.isNotBlank() },
                resume.personalInfo.location.takeIf { it.isNotBlank() }
            ).joinToString("  •  ")
            Text(text = contacts, fontSize = 9.5.sp, color = Color(0xFFE2E8F0))
        }

        Column(modifier = Modifier.padding(18.dp)) {
            if (resume.personalInfo.summary.isNotBlank()) {
                ExecutiveSectionHeader("EXECUTIVE PROFILE", accentColor)
                Text(
                    text = resume.personalInfo.summary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF1E293B),
                    fontFamily = fontFamily,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            if (resume.experiences.isNotEmpty()) {
                ExecutiveSectionHeader("LEADERSHIP & EXPERIENCE", accentColor)
                resume.experiences.forEach { exp ->
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${exp.position} | ${exp.company}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "${exp.startDate} - ${if (exp.isCurrent) "Present" else exp.endDate}",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        exp.bullets.forEach { bullet ->
                            Row(modifier = Modifier.padding(vertical = 1.dp)) {
                                Text("▪ ", color = accentColor, fontSize = 10.sp)
                                Text(bullet, fontSize = 10.5.sp, lineHeight = 15.sp, color = Color(0xFF334155))
                            }
                        }
                    }
                }
            }

            if (resume.educations.isNotEmpty()) {
                ExecutiveSectionHeader("EDUCATION & CREDENTIALS", accentColor)
                resume.educations.forEach { edu ->
                    Text(
                        text = "${edu.degree}, ${edu.institution} (${edu.startDate} - ${edu.endDate})",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            if (resume.skillCategories.isNotEmpty()) {
                ExecutiveSectionHeader("CORE COMPETENCIES", accentColor)
                resume.skillCategories.forEach { cat ->
                    Text(
                        text = "${cat.categoryName}: ${cat.skills.joinToString(", ")}",
                        fontSize = 10.5.sp,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

// ---------------- MINIMALIST TEMPLATE ----------------
@Composable
private fun MinimalistTemplateLayout(
    resume: ResumeFull,
    accentColor: Color,
    fontFamily: FontFamily
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp)
    ) {
        Text(
            text = resume.personalInfo.fullName.ifBlank { "Full Name" },
            fontSize = 24.sp,
            fontWeight = FontWeight.Light,
            color = Color(0xFF0F172A),
            fontFamily = fontFamily,
            letterSpacing = 1.5.sp
        )
        Text(
            text = resume.personalInfo.jobTitle.ifBlank { "Role" },
            fontSize = 12.sp,
            color = accentColor,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = listOfNotNull(
                resume.personalInfo.email.takeIf { it.isNotBlank() },
                resume.personalInfo.phone.takeIf { it.isNotBlank() },
                resume.personalInfo.location.takeIf { it.isNotBlank() }
            ).joinToString(" / "),
            fontSize = 9.sp,
            color = Color(0xFF64748B)
        )

        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE2E8F0))

        if (resume.personalInfo.summary.isNotBlank()) {
            Text("ABOUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 1.5.sp)
            Text(resume.personalInfo.summary, fontSize = 11.sp, lineHeight = 16.sp, color = Color(0xFF334155), modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
        }

        if (resume.experiences.isNotEmpty()) {
            Text("EXPERIENCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 1.5.sp)
            resume.experiences.forEach { exp ->
                Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                    Text("${exp.position} — ${exp.company}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("${exp.startDate} – ${if (exp.isCurrent) "Present" else exp.endDate}", fontSize = 9.5.sp, color = Color(0xFF64748B))
                    exp.bullets.forEach { b ->
                        Text("– $b", fontSize = 10.5.sp, color = Color(0xFF334155), modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }

        if (resume.skillCategories.isNotEmpty()) {
            Text("SKILLS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 1.5.sp)
            resume.skillCategories.forEach { cat ->
                Text("${cat.categoryName}: ${cat.skills.joinToString(", ")}", fontSize = 10.5.sp, color = Color(0xFF334155), modifier = Modifier.padding(vertical = 1.dp))
            }
        }
    }
}

// ---------------- TECH SLATE TEMPLATE ----------------
@Composable
private fun TechSlateTemplateLayout(
    resume: ResumeFull,
    accentColor: Color,
    fontFamily: FontFamily
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resume.personalInfo.fullName.ifBlank { "Tech Candidate" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = resume.personalInfo.jobTitle.ifBlank { "Software Engineer" },
                    fontSize = 12.sp,
                    color = Color(0xFF38BDF8)
                )
                Text(
                    text = listOfNotNull(resume.personalInfo.email, resume.personalInfo.githubUrl, resume.personalInfo.location).filter { it.isNotBlank() }.joinToString(" • "),
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (resume.personalInfo.summary.isNotBlank()) {
            ModernSectionHeader("OVERVIEW", accentColor)
            Text(resume.personalInfo.summary, fontSize = 10.5.sp, lineHeight = 15.sp, color = Color(0xFF334155))
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (resume.experiences.isNotEmpty()) {
            ModernSectionHeader("EXPERIENCE & IMPACT", accentColor)
            resume.experiences.forEach { exp ->
                Column(modifier = Modifier.padding(bottom = 6.dp)) {
                    Text("${exp.position} @ ${exp.company}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("${exp.startDate} - ${if (exp.isCurrent) "Present" else exp.endDate}", fontSize = 9.5.sp, color = Color(0xFF64748B))
                    exp.bullets.forEach { b ->
                        Text("⚡ $b", fontSize = 10.sp, color = Color(0xFF334155), modifier = Modifier.padding(vertical = 1.dp))
                    }
                }
            }
        }

        if (resume.skillCategories.isNotEmpty()) {
            ModernSectionHeader("STACK & TOOLS", accentColor)
            resume.skillCategories.forEach { cat ->
                Text("${cat.categoryName}: ${cat.skills.joinToString("  •  ")}", fontSize = 10.5.sp, color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 1.dp))
            }
        }
    }
}

// Subcomponents
@Composable
private fun ModernSectionHeader(title: String, accentColor: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp)) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 1.sp
        )
        Divider(color = accentColor.copy(alpha = 0.4f), thickness = 1.dp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun AtsSectionHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            text = title,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            letterSpacing = 0.8.sp
        )
        Divider(color = Color.Black, thickness = 1.dp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun ExecutiveSectionHeader(title: String, accentColor: Color) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 1.sp
        )
        Divider(color = Color(0xFFCBD5E1), thickness = 1.dp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun ContactMiniItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(11.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(text, fontSize = 9.sp, color = Color(0xFF475569))
    }
}

@Composable
private fun ResumeQrThumbnail(resume: ResumeFull, sizeDp: Int = 42) {
    if (!resume.personalInfo.includeQrCode) return
    val url = when (resume.personalInfo.qrCodeTarget) {
        "LinkedIn" -> resume.personalInfo.linkedinUrl.ifBlank { resume.personalInfo.websiteUrl }
        "GitHub" -> resume.personalInfo.githubUrl.ifBlank { resume.personalInfo.linkedinUrl }
        else -> resume.personalInfo.websiteUrl.ifBlank { resume.personalInfo.linkedinUrl }
    }
    if (url.isBlank()) return

    val qrBitmap = remember(url) {
        QrCodeGenerator.generateQrBitmap(url, 100)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Image(
            bitmap = qrBitmap.asImageBitmap(),
            contentDescription = "QR Code to ${resume.personalInfo.qrCodeTarget}",
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
        )
        Text(
            text = "Scan ${resume.personalInfo.qrCodeTarget}",
            fontSize = 7.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
