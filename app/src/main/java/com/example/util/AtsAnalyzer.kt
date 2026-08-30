package com.example.util

import com.example.data.model.AtsAnalysisResult
import com.example.data.model.AtsRecommendation
import com.example.data.model.ResumeFull
import java.util.Locale

object AtsAnalyzer {

    private val ACTION_VERBS = setOf(
        "led", "built", "scaled", "architected", "spearheaded", "optimized", "delivered",
        "managed", "implemented", "developed", "engineered", "designed", "orchestrated",
        "formulated", "automated", "overhauled", "accelerated", "reduced", "increased",
        "launched", "created", "directed", "established", "streamlined", "revamped",
        "generated", "resolved", "executed", "collaborated", "mentored", "achieved"
    )

    private val COMMON_TECH_KEYWORDS = listOf(
        "Kotlin", "Java", "Python", "TypeScript", "JavaScript", "React", "Jetpack Compose",
        "REST API", "GraphQL", "PostgreSQL", "MySQL", "MongoDB", "Redis", "Docker",
        "Kubernetes", "AWS", "GCP", "Azure", "CI/CD", "Git", "Coroutines", "Room",
        "MVVM", "Clean Architecture", "Microservices", "Unit Testing", "Agile", "Scrum",
        "System Design", "Performance Optimization", "Security", "Firebase", "SQL"
    )

    fun analyze(resume: ResumeFull, jobDescription: String = resume.targetJobDescription): AtsAnalysisResult {
        var contactScore = 0
        var summaryScore = 0
        var experienceScore = 0
        var skillsScore = 0
        var formattingScore = 0
        var keywordsScore = 0

        val recommendations = mutableListOf<AtsRecommendation>()
        val strengths = mutableListOf<String>()
        val potentialIssues = mutableListOf<String>()

        // 1. CONTACT INFO (Max 10)
        val p = resume.personalInfo
        if (p.fullName.isNotBlank()) contactScore += 3
        if (p.email.contains("@") && p.email.contains(".")) contactScore += 3 else {
            potentialIssues.add("Missing or invalid email address")
            recommendations.add(
                AtsRecommendation(
                    title = "Add Professional Email",
                    description = "Ensure a clear professional email (e.g. name@domain.com) is in your header.",
                    targetSection = "Contact",
                    priority = "High"
                )
            )
        }
        if (p.phone.isNotBlank()) contactScore += 2 else {
            recommendations.add(
                AtsRecommendation(
                    title = "Add Phone Number",
                    description = "Include your phone number with country code for recruiter callbacks.",
                    targetSection = "Contact",
                    priority = "Medium"
                )
            )
        }
        if (p.location.isNotBlank() || p.linkedinUrl.isNotBlank()) contactScore += 2

        if (contactScore >= 8) {
            strengths.add("Complete recruiter contact header")
        }

        // 2. PROFESSIONAL SUMMARY (Max 10)
        val summaryWords = p.summary.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        when {
            summaryWords >= 40 -> {
                summaryScore = 10
                strengths.add("Compelling, well-developed career summary (${summaryWords} words)")
            }
            summaryWords >= 20 -> {
                summaryScore = 7
                recommendations.add(
                    AtsRecommendation(
                        title = "Expand Professional Summary",
                        description = "Enrich your summary to 40+ words highlighting your core domain achievements.",
                        targetSection = "Summary",
                        priority = "Medium"
                    )
                )
            }
            summaryWords > 0 -> {
                summaryScore = 4
                recommendations.add(
                    AtsRecommendation(
                        title = "Strengthen Summary Section",
                        description = "Your professional summary is brief. Expand with years of experience and top specialties.",
                        targetSection = "Summary",
                        priority = "High"
                    )
                )
            }
            else -> {
                summaryScore = 0
                potentialIssues.add("No professional summary found")
                recommendations.add(
                    AtsRecommendation(
                        title = "Add Professional Summary",
                        description = "Add a 3-4 sentence elevator pitch targeting your desired job role.",
                        targetSection = "Summary",
                        priority = "High"
                    )
                )
            }
        }

        // 3. EXPERIENCE QUALITY (Max 20)
        var actionVerbsFound = 0
        var metricsFound = 0
        var totalBullets = 0

        val allResumeText = buildString {
            append(p.fullName).append(" ")
            append(p.jobTitle).append(" ")
            append(p.summary).append(" ")
            resume.experiences.forEach { exp ->
                append(exp.position).append(" ").append(exp.company).append(" ").append(exp.description).append(" ")
                exp.bullets.forEach { b ->
                    append(b).append(" ")
                    totalBullets++
                    val words = b.lowercase(Locale.ROOT).split("[\\s,;:.()]+".toRegex())
                    if (words.any { it in ACTION_VERBS }) actionVerbsFound++
                    if (b.contains(Regex("""\d+%|\$\d+|\b\d{1,4}\b|\b\d+x\b|increased|reduced|saved|accelerated""", RegexOption.IGNORE_CASE))) {
                        metricsFound++
                    }
                }
            }
            resume.educations.forEach { edu ->
                append(edu.degree).append(" ").append(edu.fieldOfStudy).append(" ").append(edu.institution).append(" ")
            }
            resume.skillCategories.forEach { cat ->
                cat.skills.forEach { s -> append(s).append(" ") }
            }
            resume.projects.forEach { proj ->
                append(proj.title).append(" ").append(proj.techStack).append(" ").append(proj.description).append(" ")
            }
        }.lowercase(Locale.ROOT)

        if (resume.experiences.isNotEmpty()) {
            experienceScore += 6
            if (totalBullets >= 3) experienceScore += 4
            if (actionVerbsFound >= 2) {
                experienceScore += 5
                strengths.add("Strong action verbs in work history ($actionVerbsFound found)")
            } else {
                recommendations.add(
                    AtsRecommendation(
                        title = "Use Strong Action Verbs",
                        description = "Start bullet points with assertive verbs like 'Spearheaded', 'Architected', 'Optimized'.",
                        targetSection = "Experience",
                        priority = "High"
                    )
                )
            }

            if (metricsFound >= 2) {
                experienceScore += 5
                strengths.add("Quantifiable business metrics detected ($metricsFound bullets with data)")
            } else {
                recommendations.add(
                    AtsRecommendation(
                        title = "Add Quantifiable Metrics",
                        description = "Include metrics, percentages, dollar values, or scale (e.g. 'boosted performance by 35%').",
                        targetSection = "Experience",
                        priority = "High"
                    )
                )
            }
        } else {
            potentialIssues.add("No work experience entries added")
            recommendations.add(
                AtsRecommendation(
                    title = "Add Experience Entries",
                    description = "Document your previous roles or major projects with 3-4 achievement-driven bullets each.",
                    targetSection = "Experience",
                    priority = "High"
                )
            )
        }

        // 4. SKILLS COVERAGE (Max 10)
        val allSkills = resume.skillCategories.flatMap { it.skills }.filter { it.isNotBlank() }
        when {
            allSkills.size >= 10 -> {
                skillsScore = 10
                strengths.add("Diverse skill profile with ${allSkills.size} indexed skills")
            }
            allSkills.size >= 5 -> {
                skillsScore = 7
                recommendations.add(
                    AtsRecommendation(
                        title = "Expand Technical Skills",
                        description = "Group your technical capabilities into distinct categories (e.g. Languages, Frameworks, Cloud).",
                        targetSection = "Skills",
                        priority = "Medium"
                    )
                )
            }
            allSkills.isNotEmpty() -> {
                skillsScore = 4
                recommendations.add(
                    AtsRecommendation(
                        title = "Add Core Competencies",
                        description = "List at least 8-10 relevant technical and hard skills for keyword scanning.",
                        targetSection = "Skills",
                        priority = "High"
                    )
                )
            }
            else -> {
                skillsScore = 0
                potentialIssues.add("No skills listed on resume")
                recommendations.add(
                    AtsRecommendation(
                        title = "Add Skills Section",
                        description = "Applicant tracking systems heavily index skills. Add your core toolset.",
                        targetSection = "Skills",
                        priority = "High"
                    )
                )
            }
        }

        // 5. FORMATTING & ATS READABILITY (Max 10)
        formattingScore = 10
        if (resume.educations.isEmpty()) {
            formattingScore -= 2
            recommendations.add(
                AtsRecommendation(
                    title = "Add Education Details",
                    description = "Include your degree, field of study, and university.",
                    targetSection = "Education",
                    priority = "Medium"
                )
            )
        }

        // 6. KEYWORDS & JOB MATCHING (Max 20)
        val matchedKeywords = mutableListOf<String>()
        val missingKeywords = mutableListOf<String>()
        var jobMatchPercentage = 0

        val targetKeywords = if (jobDescription.isNotBlank()) {
            extractKeywordsFromJobDescription(jobDescription)
        } else {
            getKeywordsForRole(resume.personalInfo.jobTitle)
        }

        targetKeywords.forEach { kw ->
            val kwLower = kw.lowercase(Locale.ROOT)
            if (allResumeText.contains(kwLower)) {
                matchedKeywords.add(kw)
            } else {
                missingKeywords.add(kw)
            }
        }

        if (targetKeywords.isNotEmpty()) {
            val matchRatio = matchedKeywords.size.toFloat() / targetKeywords.size.toFloat()
            jobMatchPercentage = (matchRatio * 100).toInt().coerceIn(15, 98)
            keywordsScore = (matchRatio * 20).toInt().coerceIn(5, 20)
        } else {
            jobMatchPercentage = 75
            keywordsScore = 15
        }

        if (missingKeywords.isNotEmpty()) {
            val topMissing = missingKeywords.take(3).joinToString(", ")
            val targetExp = resume.experiences.firstOrNull()?.position ?: "Experience"
            recommendations.add(
                AtsRecommendation(
                    title = "Add Missing Keywords ($topMissing)",
                    description = "Add \"${missingKeywords.first()}\" to your $targetExp section to boost ATS ranking.",
                    targetSection = "Keywords",
                    priority = "High"
                )
            )
        }

        val totalScore = (contactScore + summaryScore + experienceScore + skillsScore + formattingScore + keywordsScore)
            .coerceIn(25, 99)

        return AtsAnalysisResult(
            overallScore = totalScore,
            jobMatchPercentage = jobMatchPercentage,
            keywordsScore = keywordsScore,
            maxKeywordsScore = 20,
            skillsScore = skillsScore,
            maxSkillsScore = 10,
            formattingScore = formattingScore,
            maxFormattingScore = 10,
            experienceScore = experienceScore,
            maxExperienceScore = 20,
            summaryScore = summaryScore,
            maxSummaryScore = 10,
            contactScore = contactScore,
            maxContactScore = 10,
            matchedKeywords = matchedKeywords.distinct(),
            missingKeywords = missingKeywords.distinct(),
            recommendations = recommendations,
            actionVerbsCount = actionVerbsFound,
            metricsCount = metricsFound,
            strengths = strengths,
            potentialIssues = potentialIssues
        )
    }

    private fun extractKeywordsFromJobDescription(jd: String): List<String> {
        val jdLower = jd.lowercase(Locale.ROOT)
        val extracted = mutableListOf<String>()

        COMMON_TECH_KEYWORDS.forEach { kw ->
            if (jdLower.contains(kw.lowercase(Locale.ROOT))) {
                extracted.add(kw)
            }
        }

        // Additional common industry words
        val additionalWords = listOf(
            "Leadership", "Communication", "Problem Solving", "Collaboration", "DevOps",
            "Continuous Integration", "Automated Testing", "Data Analysis", "Optimization",
            "Scalability", "Mentorship", "Project Management", "Product Strategy", "API Integration"
        )
        additionalWords.forEach { kw ->
            if (jdLower.contains(kw.lowercase(Locale.ROOT))) {
                extracted.add(kw)
            }
        }

        return if (extracted.isNotEmpty()) extracted else COMMON_TECH_KEYWORDS.take(8)
    }

    private fun getKeywordsForRole(role: String): List<String> {
        val r = role.lowercase(Locale.ROOT)
        return when {
            r.contains("android") || r.contains("mobile") -> listOf(
                "Kotlin", "Jetpack Compose", "Coroutines", "Room", "MVVM", "Clean Architecture", "REST API", "CI/CD", "Git", "Unit Testing"
            )
            r.contains("backend") || r.contains("server") -> listOf(
                "Java", "Python", "REST API", "Microservices", "PostgreSQL", "Redis", "Docker", "Kubernetes", "AWS", "CI/CD"
            )
            r.contains("frontend") || r.contains("web") -> listOf(
                "TypeScript", "JavaScript", "React", "GraphQL", "REST API", "Performance Optimization", "Git", "CI/CD", "State Management"
            )
            r.contains("data") || r.contains("ai") || r.contains("machine") -> listOf(
                "Python", "SQL", "Data Analysis", "Machine Learning", "PyTorch", "TensorFlow", "PostgreSQL", "Cloud", "Git"
            )
            else -> listOf(
                "Kotlin", "REST API", "PostgreSQL", "Docker", "Git", "CI/CD", "Agile", "Clean Architecture", "Unit Testing"
            )
        }
    }
}
