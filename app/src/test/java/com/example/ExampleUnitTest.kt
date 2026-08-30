package com.example

import com.example.data.model.*
import com.example.util.AtsAnalyzer
import com.example.util.QrCodeGenerator
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testQrCodeGenerator_createsValidBitmap() {
        val qrBitmap = QrCodeGenerator.generateQrBitmap("https://linkedin.com/in/testuser", size = 200)
        assertNotNull(qrBitmap)
        assertEquals(200, qrBitmap?.width)
        assertEquals(200, qrBitmap?.height)
    }

    @Test
    fun testResumeFull_qrCodeDefaults() {
        val resume = ResumeFull()
        assertTrue(resume.personalInfo.includeQrCode)
        assertEquals("LinkedIn", resume.personalInfo.qrCodeTarget)
    }

    @Test
    fun testAtsAnalyzer_computesScoreAndRecommendations() {
        val resume = ResumeFull(
            personalInfo = PersonalInfo(
                fullName = "Jane Developer",
                email = "jane@example.com",
                phone = "+1-555-0199",
                jobTitle = "Senior Android Engineer",
                summary = "Accomplished Senior Android Engineer with 6+ years building scalable mobile apps using Kotlin, Jetpack Compose, Coroutines, and MVVM architecture. Proven track record optimizing performance and delivering clean code."
            ),
            experiences = listOf(
                ExperienceItem(
                    position = "Lead Mobile Engineer",
                    company = "Tech Innovations",
                    bullets = listOf(
                        "Architected modular Jetpack Compose app, reducing startup latency by 35%.",
                        "Led team of 6 engineers to deliver modern Kotlin offline-first Room persistence."
                    )
                )
            ),
            educations = listOf(
                EducationItem(degree = "B.S.", fieldOfStudy = "Computer Science", institution = "Tech University")
            ),
            skillCategories = listOf(
                SkillCategory(categoryName = "Languages & Tools", skills = listOf("Kotlin", "Jetpack Compose", "Coroutines", "Room", "REST API", "Git", "CI/CD", "Unit Testing", "MVVM", "Clean Architecture"))
            )
        )

        val analysis = AtsAnalyzer.analyze(resume)
        assertTrue(analysis.overallScore >= 80)
        assertTrue(analysis.actionVerbsCount >= 2)
        assertTrue(analysis.metricsCount >= 1)
        assertTrue(analysis.matchedKeywords.contains("Kotlin"))
    }

    @Test
    fun testAtsAnalyzer_jobDescriptionKeywordGap() {
        val resume = ResumeFull(
            personalInfo = PersonalInfo(
                fullName = "Jane Dev",
                email = "jane@example.com",
                jobTitle = "Android Developer"
            ),
            skillCategories = listOf(
                SkillCategory(categoryName = "Tech", skills = listOf("Kotlin", "Jetpack Compose"))
            )
        )

        val jobDescription = "We are seeking a Senior Android Engineer with strong skills in Kotlin, Jetpack Compose, GraphQL, and Redis."
        val analysis = AtsAnalyzer.analyze(resume, jobDescription)

        assertTrue(analysis.matchedKeywords.contains("Kotlin"))
        assertTrue(analysis.missingKeywords.contains("GraphQL") || analysis.missingKeywords.contains("Redis"))
        assertTrue(analysis.recommendations.any { it.targetSection == "Keywords" || it.title.contains("Missing Keywords") })
    }

    @Test
    fun testAllSixTemplatesAvailable() {
        val templates = ResumeTemplate.values()
        assertEquals(6, templates.size)
        val expected = setOf(
            ResumeTemplate.CLASSIC_ATS,
            ResumeTemplate.MODERN,
            ResumeTemplate.MINIMALIST,
            ResumeTemplate.EXECUTIVE,
            ResumeTemplate.TECH_SLATE,
            ResumeTemplate.PROFESSIONAL
        )
        assertEquals(expected, templates.toSet())
    }
}

