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
                location = "San Francisco, CA",
                linkedinUrl = "https://linkedin.com/in/janedev",
                jobTitle = "Senior Android Engineer",
                summary = "Accomplished Senior Android Engineer with 6+ years building scalable mobile apps using Kotlin, Jetpack Compose, Coroutines, and MVVM architecture. Proven track record optimizing performance and delivering clean code across multiple production applications."
            ),
            experiences = listOf(
                ExperienceItem(
                    position = "Senior Android Engineer",
                    company = "Tech Innovations",
                    bullets = listOf(
                        "Architected modular Jetpack Compose app, reducing startup latency by 35%.",
                        "Led team of 6 engineers to deliver modern Kotlin offline-first Room persistence with 99.9% crash-free sessions.",
                        "Optimized network caching layer saving $50K annual cloud infrastructure costs."
                    )
                )
            ),
            educations = listOf(
                EducationItem(degree = "B.S.", fieldOfStudy = "Computer Science", institution = "Tech University")
            ),
            skillCategories = listOf(
                SkillCategory(categoryName = "Languages & Tools", skills = listOf("Kotlin", "Java", "Jetpack Compose", "Coroutines", "Room", "REST API", "Git", "CI/CD", "Unit Testing", "MVVM", "Clean Architecture", "Firebase"))
            )
        )

        val analysis = AtsAnalyzer.analyze(resume)
        assertTrue(analysis.overallScore >= 70)
        assertTrue(analysis.actionVerbsCount >= 2)
        assertTrue(analysis.metricsCount >= 2)
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

    @Test
    fun testAuthUserState_otpSupport() {
        val emailUser = com.example.data.firebase.AuthUserState(
            uid = "email_user_123",
            displayName = "Alex Morgan",
            email = "alex@example.com",
            isLoggedIn = true,
            authProvider = "EMAIL_OTP"
        )
        assertTrue(emailUser.isLoggedIn)
        assertEquals("EMAIL_OTP", emailUser.authProvider)
        assertEquals("alex@example.com", emailUser.email)

        val phoneUser = com.example.data.firebase.AuthUserState(
            uid = "phone_user_456",
            displayName = "Jordan Lee",
            phone = "+1 555 234 5678",
            isLoggedIn = true,
            authProvider = "PHONE_OTP"
        )
        assertTrue(phoneUser.isLoggedIn)
        assertEquals("PHONE_OTP", phoneUser.authProvider)
        assertEquals("+1 555 234 5678", phoneUser.phone)
    }

    @Test
    fun testFileImportResult_structure() {
        val result = com.example.util.FileImportResult(
            fileName = "my_resume.pdf",
            fileSizeKb = 45,
            fileExtension = "pdf",
            rawText = "John Doe\nSoftware Engineer\nExperience at Tech Corp"
        )
        assertEquals("my_resume.pdf", result.fileName)
        assertEquals("pdf", result.fileExtension)
        assertEquals(45L, result.fileSizeKb)
        assertTrue(result.rawText.contains("John Doe"))
        assertNull(result.errorMessage)
    }
}

