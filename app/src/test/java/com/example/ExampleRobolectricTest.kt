package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.GeminiResumeService
import com.example.data.model.EducationItem
import com.example.data.model.ExperienceItem
import com.example.data.model.ResumeFull
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("AI Resume Builder", appName)
    }

    @Test
    fun `test fallback experience bullets generation`() = runBlocking {
        val service = GeminiResumeService()
        val result = service.generateExperienceBullets(
            position = "Senior Android Engineer",
            company = "Tech Corp",
            userNotes = "Worked on Compose and Room",
            targetJobDescription = "Looking for Senior Kotlin/Compose developer",
            focusArea = "Impact & Metrics"
        )
        assertTrue(result.isSuccess)
        val bullets = result.getOrNull()
        assertNotNull(bullets)
        assertTrue(bullets!!.isNotEmpty())
        assertTrue(bullets.any { it.contains("Kotlin", ignoreCase = true) || it.contains("%") || it.contains("latency", ignoreCase = true) })
    }

    @Test
    fun `test fallback education suggestions`() = runBlocking {
        val service = GeminiResumeService()
        val result = service.suggestEducation(
            degree = "B.S. in Computer Science",
            fieldOfStudy = "Computer Science",
            institution = "Stanford University",
            targetRole = "Software Engineer",
            targetJobDescription = "Distributed Systems Engineer"
        )
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()
        assertNotNull(suggestions)
        assertTrue(suggestions!!.relevantCoursework.isNotEmpty())
        assertTrue(suggestions.academicBulletPoints.isNotEmpty())
    }

    @Test
    fun `test resume model with target job description and education highlights`() {
        val resume = ResumeFull(
            title = "Targeted SWE Resume",
            targetJobDescription = "Senior Backend Engineer specializing in scalable APIs",
            educations = listOf(
                EducationItem(
                    degree = "B.S. in Computer Science",
                    institution = "MIT",
                    highlights = listOf("Coursework: Distributed Systems, Advanced Operating Systems")
                )
            ),
            experiences = listOf(
                ExperienceItem(
                    position = "Backend Engineer",
                    company = "CloudScale",
                    bullets = listOf("Architected microservices handling 10k req/sec with 99.99% uptime.")
                )
            )
        )
        assertEquals("Targeted SWE Resume", resume.title)
        assertEquals("Senior Backend Engineer specializing in scalable APIs", resume.targetJobDescription)
        assertEquals(1, resume.educations.first().highlights.size)
        assertEquals(1, resume.experiences.first().bullets.size)
    }

    @Test
    fun `test qr code generator creates valid bitmap in robolectric`() {
        val qrBitmap = com.example.util.QrCodeGenerator.generateQrBitmap("https://linkedin.com/in/testuser", size = 200)
        assertNotNull(qrBitmap)
        assertEquals(200, qrBitmap?.width)
        assertEquals(200, qrBitmap?.height)
    }
}
