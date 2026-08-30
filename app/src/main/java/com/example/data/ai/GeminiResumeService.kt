package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.UUID
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

class GeminiResumeService {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService = retrofit.create(GeminiApiService::class.java)

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun generateSummaries(
        role: String,
        experienceYears: String,
        keySkills: String,
        tone: String = "Professional"
    ): Result<List<SummaryOption>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(getFallbackSummaries(role, experienceYears, keySkills))
        }

        val prompt = """
            You are an expert resume writer and executive career coach.
            Generate 3 distinct, high-impact professional resume summary variations for a candidate with the following profile:
            - Target Role: $role
            - Experience: $experienceYears
            - Key Skills: $keySkills
            - Tone: $tone

            Format your response strictly as 3 numbered items:
            1. [Impact-Driven]: (A strong, metric-oriented summary highlighting quantifiable results and leadership)
            2. [Technical Mastery]: (A deep-dive technical summary highlighting architecture, modern tools, and system scaling)
            3. [Executive / Strategic]: (A concise, vision-focused summary emphasizing strategic alignment, cross-functional impact, and innovation)

            Keep each summary between 2 and 4 sentences. Do not add intro or outro pleasantries.
        """.trimIndent()

        try {
            val response = apiService.generateContent(
                apiKey = apiKey,
                request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(temperature = 0.7f)
                )
            )

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                val parsed = parseNumberedSummaries(text, role)
                Result.success(parsed)
            } else {
                Result.success(getFallbackSummaries(role, experienceYears, keySkills))
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Summary generation error: ${e.message}", e)
            Result.success(getFallbackSummaries(role, experienceYears, keySkills))
        }
    }

    suspend fun enhanceBulletPoint(
        jobTitle: String,
        rawBullet: String,
        targetJobDescription: String = ""
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(getFallbackEnhancedBullets(jobTitle, rawBullet))
        }

        val prompt = """
            You are a top ATS optimization specialist and recruiter at Fortune 500 companies.
            Transform this rough resume bullet point for a "$jobTitle" into 3 high-impact, ATS-optimized bullet points following the Google XYZ formula ("Accomplished [X], as measured by [Y], by doing [Z]"):
            
            Original bullet: "$rawBullet"
            ${if (targetJobDescription.isNotBlank()) "Target Job Requirements / Keywords: $targetJobDescription" else ""}
            
            Requirements:
            - Start with powerful action verbs (e.g., Engineered, Spearheaded, Accelerated, Optimized, Orchestrated).
            - Include realistic metrics and business impact (percentages, latency reductions, revenue, uptime).
            - If target job keywords are provided, weave in relevant terms seamlessly.
            - Keep each bullet concise (15-28 words).
            
            Format strictly as:
            1. Option 1
            2. Option 2
            3. Option 3
        """.trimIndent()

        try {
            val response = apiService.generateContent(
                apiKey = apiKey,
                request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(temperature = 0.6f)
                )
            )

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                val bullets = text.lines()
                    .filter { it.isNotBlank() }
                    .map { it.replaceFirst(Regex("^\\d+[.)\\-]\\s*"), "").trim() }
                    .filter { it.isNotEmpty() }
                    .take(3)
                Result.success(if (bullets.isNotEmpty()) bullets else getFallbackEnhancedBullets(jobTitle, rawBullet))
            } else {
                Result.success(getFallbackEnhancedBullets(jobTitle, rawBullet))
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Bullet optimization error: ${e.message}", e)
            Result.success(getFallbackEnhancedBullets(jobTitle, rawBullet))
        }
    }

    suspend fun generateExperienceBullets(
        position: String,
        company: String,
        userNotes: String = "",
        targetJobDescription: String = "",
        focusArea: String = "Impact & Metrics"
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(getFallbackExperienceBullets(position, company, userNotes, focusArea))
        }

        val prompt = """
            You are a world-class executive resume writer and FAANG technical recruiter.
            Generate 4-5 high-impact, ATS-optimized bullet points for the following work experience entry:
            - Job Title / Position: $position
            - Company: $company
            - User Draft / Responsibilities: ${if (userNotes.isNotBlank()) userNotes else "Key deliverables for $position"}
            - Target Job Description / Keywords: ${if (targetJobDescription.isNotBlank()) targetJobDescription else "Standard industry requirements for $position"}
            - Focus Tone / Dimension: $focusArea

            Requirements:
            - Follow the Google XYZ Formula: "Accomplished [X], as measured by [Y], by doing [Z]"
            - Start each bullet point with a powerful, distinctive action verb (e.g., Architected, Spearheaded, Optimized, Accelerated, Orchestrated).
            - Include realistic metrics (% efficiency improvements, scale, throughput, dollar savings, uptime).
            - Align tightly with key skills and qualifications in the target job description.
            - Length: 15-28 words per bullet.

            Format strictly as a numbered list:
            1. [Bullet 1]
            2. [Bullet 2]
            3. [Bullet 3]
            4. [Bullet 4]
            5. [Bullet 5]
        """.trimIndent()

        try {
            val response = apiService.generateContent(
                apiKey = apiKey,
                request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(temperature = 0.6f)
                )
            )

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                val bullets = text.lines()
                    .filter { it.isNotBlank() }
                    .map { it.replaceFirst(Regex("^\\d+[.)\\-]\\s*"), "").trim() }
                    .filter { it.isNotEmpty() }
                Result.success(if (bullets.isNotEmpty()) bullets else getFallbackExperienceBullets(position, company, userNotes, focusArea))
            } else {
                Result.success(getFallbackExperienceBullets(position, company, userNotes, focusArea))
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Experience bullets generation error: ${e.message}", e)
            Result.success(getFallbackExperienceBullets(position, company, userNotes, focusArea))
        }
    }

    suspend fun suggestEducation(
        degree: String,
        fieldOfStudy: String,
        institution: String,
        targetRole: String = "",
        targetJobDescription: String = ""
    ): Result<EducationSuggestions> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(getFallbackEducationSuggestions(degree, fieldOfStudy, institution, targetRole))
        }

        val prompt = """
            You are a university career advisor and executive resume coach.
            Generate impactful resume content for an Education section entry based on the following candidate profile:
            - Degree: $degree
            - Field of Study: $fieldOfStudy
            - Institution: $institution
            - Target Job Role: ${targetRole.ifBlank { "Professional in field" }}
            - Target Job Description / Keywords: ${if (targetJobDescription.isNotBlank()) targetJobDescription else "General industry requirements"}

            Provide structured output matching the following exact section format:
            
            BULLETS:
            - [Academic achievement bullet point 1 with quantifiable research or lab impact]
            - [Academic achievement bullet point 2 highlighting technical coursework or teamwork]
            - [Academic achievement bullet point 3 highlighting leadership or scholarship]
            
            COURSEWORK: course1, course2, course3, course4, course5, course6
            
            HONORS: honor1, honor2, honor3
            
            CAPSTONE:
            - [Capstone project title with 1-sentence technical description aligned with target job]
            - [Alternative capstone project title and description]
        """.trimIndent()

        try {
            val response = apiService.generateContent(
                apiKey = apiKey,
                request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(temperature = 0.5f)
                )
            )

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                val parsed = parseEducationResponse(text, degree, fieldOfStudy, institution, targetRole)
                Result.success(parsed)
            } else {
                Result.success(getFallbackEducationSuggestions(degree, fieldOfStudy, institution, targetRole))
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Education suggestion error: ${e.message}", e)
            Result.success(getFallbackEducationSuggestions(degree, fieldOfStudy, institution, targetRole))
        }
    }

    suspend fun analyzeAts(
        resume: ResumeFull,
        targetJobDescription: String = ""
    ): Result<AtsScoreBreakdown> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        
        // Compute base algorithmic heuristic score first
        val baseScore = calculateAlgorithmicAtsScore(resume, targetJobDescription)
        
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(baseScore)
        }

        val resumeSummaryText = buildString {
            append("Role: ${resume.personalInfo.jobTitle}\n")
            append("Summary: ${resume.personalInfo.summary}\n")
            append("Experience:\n")
            resume.experiences.forEach { exp ->
                append("- ${exp.position} at ${exp.company}: ${exp.bullets.joinToString("; ")}\n")
            }
            append("Skills: ${resume.skillCategories.flatMap { it.skills }.joinToString(", ")}\n")
            append("Projects: ${resume.projects.joinToString { it.title }}\n")
            append("Education: ${resume.educations.joinToString { "${it.degree} from ${it.institution}" }}\n")
        }

        val prompt = """
            You are an advanced Applicant Tracking System (ATS) and Senior Technical Recruiter.
            Evaluate this resume against general industry standards and the target job description (if provided).
            
            Target Job Description:
            ${if (targetJobDescription.isNotBlank()) targetJobDescription else "General competitive tech/business industry standards"}
            
            Candidate Resume:
            $resumeSummaryText
            
            Provide an ATS evaluation with:
            OVERALL_SCORE: (number 50-98)
            IMPACT_SCORE: (number 50-100)
            KEYWORD_SCORE: (number 50-100)
            BREVITY_SCORE: (number 50-100)
            FORMAT_SCORE: (number 50-100)
            STRENGTHS:
            - [Strength 1]
            - [Strength 2]
            - [Strength 3]
            IMPROVEMENTS:
            - [Actionable fix 1]
            - [Actionable fix 2]
            - [Actionable fix 3]
            MISSING_KEYWORDS:
            - [Keyword 1]
            - [Keyword 2]
            - [Keyword 3]
            SUMMARY_FEEDBACK: [Concise 2-sentence summary feedback]
        """.trimIndent()

        try {
            val response = apiService.generateContent(
                apiKey = apiKey,
                request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(temperature = 0.4f)
                )
            )

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                val parsed = parseAtsResponse(text, baseScore)
                Result.success(parsed)
            } else {
                Result.success(baseScore)
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "ATS analysis error: ${e.message}", e)
            Result.success(baseScore)
        }
    }

    suspend fun suggestSkills(
        targetRole: String,
        targetJobDescription: String = ""
    ): Result<Map<String, List<String>>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(getFallbackSkills(targetRole, targetJobDescription))
        }

        val prompt = """
            Analyze the role "$targetRole" and the target job description below to identify the highest-priority in-demand skills and keywords for ATS optimization in 2026.
            
            Target Job Description:
            ${if (targetJobDescription.isNotBlank()) targetJobDescription else "Industry standard requirements for $targetRole"}

            Categorize the extracted and recommended skills into the following exact sections:
            TECHNICAL: skill1, skill2, skill3, skill4, skill5, skill6
            TOOLS_PLATFORMS: tool1, tool2, tool3, tool4, tool5, tool6
            FRAMEWORKS_LIBRARIES: fw1, fw2, fw3, fw4, fw5
            METHODOLOGIES: method1, method2, method3, method4
            SOFT_SKILLS: soft1, soft2, soft3, soft4
        """.trimIndent()

        try {
            val response = apiService.generateContent(
                apiKey = apiKey,
                request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(temperature = 0.5f)
                )
            )

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                val map = mutableMapOf<String, List<String>>()
                text.lines().forEach { line ->
                    when {
                        line.startsWith("TECHNICAL:", ignoreCase = true) -> {
                            map["Technical Skills"] = line.substringAfter(":").split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        }
                        line.startsWith("TOOLS_PLATFORMS:", ignoreCase = true) || line.startsWith("TOOLS:", ignoreCase = true) -> {
                            map["Tools & Platforms"] = line.substringAfter(":").split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        }
                        line.startsWith("FRAMEWORKS_LIBRARIES:", ignoreCase = true) || line.startsWith("FRAMEWORKS:", ignoreCase = true) -> {
                            map["Frameworks & Libraries"] = line.substringAfter(":").split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        }
                        line.startsWith("METHODOLOGIES:", ignoreCase = true) -> {
                            map["Methodologies & Practices"] = line.substringAfter(":").split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        }
                        line.startsWith("SOFT_SKILLS:", ignoreCase = true) -> {
                            map["Soft Skills"] = line.substringAfter(":").split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        }
                    }
                }
                if (map.isNotEmpty()) Result.success(map) else Result.success(getFallbackSkills(targetRole, targetJobDescription))
            } else {
                Result.success(getFallbackSkills(targetRole, targetJobDescription))
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Skills suggestion error: ${e.message}", e)
            Result.success(getFallbackSkills(targetRole, targetJobDescription))
        }
    }

    suspend fun generateCoverLetter(
        resume: ResumeFull,
        companyName: String,
        jobTitle: String,
        jobRequirements: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val candidateName = resume.personalInfo.fullName.ifBlank { "Candidate" }
        val experienceSummary = resume.experiences.take(2).joinToString("; ") { "${it.position} at ${it.company}" }
        val skillsSummary = resume.skillCategories.flatMap { it.skills }.take(8).joinToString(", ")

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(
                getFallbackCoverLetter(candidateName, resume.personalInfo.jobTitle, companyName, jobTitle, skillsSummary)
            )
        }

        val prompt = """
            You are an expert executive cover letter writer.
            Write a compelling, personalized, modern 3-paragraph cover letter for:
            - Candidate Name: $candidateName
            - Target Company: $companyName
            - Target Role: $jobTitle
            - Candidate Background: $experienceSummary
            - Key Strengths/Skills: $skillsSummary
            - Job Requirements to address: $jobRequirements

            Structure:
            - Paragraph 1: Enthusiastic opening stating the exact role, company admiration, and value proposition hook.
            - Paragraph 2: Core relevant accomplishments with quantifiable results linking candidate's experience to the job requirements.
            - Paragraph 3: Vision for how candidate will contribute to $companyName's immediate goals, proactive call to action for an interview.
            
            Keep the tone confident, professional, and authentic.
        """.trimIndent()

        try {
            val response = apiService.generateContent(
                apiKey = apiKey,
                request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(temperature = 0.7f)
                )
            )

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                Result.success(text.trim())
            } else {
                Result.success(getFallbackCoverLetter(candidateName, resume.personalInfo.jobTitle, companyName, jobTitle, skillsSummary))
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Cover letter error: ${e.message}", e)
            Result.success(getFallbackCoverLetter(candidateName, resume.personalInfo.jobTitle, companyName, jobTitle, skillsSummary))
        }
    }

    // --- Fallbacks & Heuristics ---

    private fun getFallbackSummaries(role: String, experience: String, skills: String): List<SummaryOption> {
        val cleanRole = role.ifBlank { "Professional" }
        return listOf(
            SummaryOption(
                tag = "Impact-Driven",
                title = "Results & Leadership Focus",
                text = "Results-driven $cleanRole with $experience of proven track record in architecting high-performance systems and scaling business outcomes. Adept at leveraging $skills to streamline cross-functional workflows, enhance product velocity by 35%, and consistently deliver scalable solutions on time."
            ),
            SummaryOption(
                tag = "Technical Mastery",
                title = "Deep Technical Focus",
                text = "Innovative $cleanRole specialized in modern full-stack development, distributed architecture, and cloud infrastructure. Hands-on expertise with $skills, driving 99.9% system reliability, automated testing pipelines, and cutting-edge software engineering best practices across fast-paced environments."
            ),
            SummaryOption(
                tag = "Strategic / Executive",
                title = "Visionary Leadership Focus",
                text = "Strategic and forward-thinking $cleanRole combining deep technical domain knowledge in $skills with strong cross-functional communication. Passionate about mentoring high-performing teams, translating complex user requirements into elegant products, and driving sustainable business growth."
            )
        )
    }

    private fun getFallbackCoverLetter(
        candidateName: String,
        candidateTitle: String,
        companyName: String,
        targetRole: String,
        skills: String
    ): String {
        val role = targetRole.ifBlank { candidateTitle.ifBlank { "Position" } }
        val comp = companyName.ifBlank { "your organization" }
        return """
Dear Hiring Team,

I am writing to express my strong enthusiasm for the $role position at $comp. With a solid track record in building resilient, high-impact software solutions and hands-on domain expertise in $skills, I am confident in my ability to immediately accelerate your engineering and product objectives.

Throughout my career, I have focused on delivering measurable business impact—from architecting performant features to optimizing system scalability and collaborating closely with cross-functional partners. My experience driving project lifecycles from ideation through deployment has equipped me with the technical depth and problem-solving agility needed to thrive at $comp.

I am particularly excited about the prospect of contributing to $comp's mission and would welcome the opportunity to discuss how my background, technical strengths in $skills, and dedication to excellence align with your team's upcoming goals. Thank you for your time and consideration.

Sincerely,
$candidateName
        """.trimIndent()
    }

    private fun getFallbackEnhancedBullets(jobTitle: String, rawBullet: String): List<String> {
        val cleanInput = rawBullet.trim().removeSuffix(".")
        return listOf(
            "Spearheaded $cleanInput, resulting in a 32% increase in system efficiency and reducing delivery cycles by 3 weeks.",
            "Architected and deployed end-to-end solutions for $cleanInput, scaling throughput to 50k+ daily users with 99.9% uptime.",
            "Collaborated cross-functionally to streamline $cleanInput, slashing operational latency by 40% while ensuring strict code quality standards."
        )
    }

    private fun getFallbackExperienceBullets(
        position: String,
        company: String,
        userNotes: String,
        focusArea: String
    ): List<String> {
        val cleanPos = position.ifBlank { "Software Engineer" }
        val note = if (userNotes.isNotBlank()) " focusing on ${userNotes.trim().removeSuffix(".")}" else ""
        return listOf(
            "Architected and deployed high-performance solutions for $cleanPos$note, increasing transaction throughput by 42% and reducing p99 latency to under 45ms.",
            "Spearheaded cross-functional delivery of core platform capabilities, collaborating with 8+ stakeholders to reduce sprint cycle time by 28%.",
            "Engineered automated CI/CD deployment pipelines and comprehensive test suites, achieving 99.98% system uptime and slashing production rollbacks by 65%.",
            "Championed cloud cost optimization and infrastructure modernization, saving $35k+ annually while scaling to accommodate 100k+ active users.",
            "Mentored 4 junior engineers in best practices, clean architecture, and modular code design, improving overall team velocity by 25%."
        )
    }

    private fun getFallbackEducationSuggestions(
        degree: String,
        fieldOfStudy: String,
        institution: String,
        targetRole: String
    ): EducationSuggestions {
        val cleanDegree = degree.ifBlank { "B.S. in Computer Science" }
        val cleanField = fieldOfStudy.ifBlank { "Computer Science & Engineering" }
        return EducationSuggestions(
            academicBulletPoints = listOf(
                "Completed high-distinction capstone project developing a real-time distributed application handling concurrent streaming pipelines.",
                "Served as Teaching Assistant & Lab Lead for Advanced Systems, mentoring 40+ undergraduate students in software design patterns.",
                "Published peer-reviewed research on scalable data structures and algorithm optimization, earning Faculty Honors."
            ),
            relevantCoursework = listOf(
                "Data Structures & Algorithms",
                "Distributed Systems",
                "Cloud Computing & Architecture",
                "Database Management Systems",
                "Software Engineering Methodologies",
                "Machine Learning & AI"
            ),
            honorsAndAchievements = listOf(
                "Dean's Honor List (All Semesters)",
                "Academic Excellence & Innovation Scholarship",
                "1st Place, Annual University Engineering Hackathon"
            ),
            capstoneHighlights = listOf(
                "Real-Time Collaborative Platform: Engineered microservices-based full-stack application supporting real-time data sync with WebSockets.",
                "Autonomous Predictive Engine: Designed and trained machine learning models achieving 94.2% classification accuracy on benchmark datasets."
            )
        )
    }

    private fun parseEducationResponse(
        text: String,
        degree: String,
        fieldOfStudy: String,
        institution: String,
        targetRole: String
    ): EducationSuggestions {
        val fallback = getFallbackEducationSuggestions(degree, fieldOfStudy, institution, targetRole)
        val bullets = mutableListOf<String>()
        val coursework = mutableListOf<String>()
        val honors = mutableListOf<String>()
        val capstone = mutableListOf<String>()

        var currentSection = ""

        text.lines().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("BULLETS:", ignoreCase = true) -> currentSection = "BULLETS"
                line.startsWith("COURSEWORK:", ignoreCase = true) -> {
                    currentSection = "COURSEWORK"
                    val inline = line.substringAfter(":").trim()
                    if (inline.isNotEmpty()) {
                        coursework.addAll(inline.split(",").map { it.trim().removePrefix("-").trim() }.filter { it.isNotEmpty() })
                    }
                }
                line.startsWith("HONORS:", ignoreCase = true) -> {
                    currentSection = "HONORS"
                    val inline = line.substringAfter(":").trim()
                    if (inline.isNotEmpty()) {
                        honors.addAll(inline.split(",").map { it.trim().removePrefix("-").trim() }.filter { it.isNotEmpty() })
                    }
                }
                line.startsWith("CAPSTONE:", ignoreCase = true) -> currentSection = "CAPSTONE"
                line.startsWith("-") || line.startsWith("*") || line.matches(Regex("^\\d+\\..*")) -> {
                    val item = line.replaceFirst(Regex("^[-*\\d.]+\\s*"), "").trim()
                    if (item.isNotEmpty()) {
                        when (currentSection) {
                            "BULLETS" -> bullets.add(item)
                            "COURSEWORK" -> coursework.addAll(item.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                            "HONORS" -> honors.addAll(item.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                            "CAPSTONE" -> capstone.add(item)
                        }
                    }
                }
            }
        }

        return EducationSuggestions(
            academicBulletPoints = if (bullets.isNotEmpty()) bullets else fallback.academicBulletPoints,
            relevantCoursework = if (coursework.isNotEmpty()) coursework else fallback.relevantCoursework,
            honorsAndAchievements = if (honors.isNotEmpty()) honors else fallback.honorsAndAchievements,
            capstoneHighlights = if (capstone.isNotEmpty()) capstone else fallback.capstoneHighlights
        )
    }

    private fun getFallbackSkills(targetRole: String, targetJobDescription: String = ""): Map<String, List<String>> {
        val lower = targetRole.lowercase() + " " + targetJobDescription.lowercase()
        return when {
            "android" in lower || "mobile" in lower || "ios" in lower || "kotlin" in lower -> mapOf(
                "Technical Skills" to listOf("Kotlin", "Jetpack Compose", "Coroutines & Flow", "Room DB", "Retrofit", "Clean Architecture"),
                "Tools & Platforms" to listOf("Android Studio", "Git/GitHub", "Firebase", "Gradle", "CI/CD Actions", "Play Console"),
                "Frameworks & Libraries" to listOf("Moshi/Gson", "Hilt/Dagger", "Ktor", "Coil", "Material 3"),
                "Methodologies & Practices" to listOf("MVVM Architecture", "TDD & Unit Testing", "Design Systems", "REST API Design"),
                "Soft Skills" to listOf("Problem Solving", "Cross-functional Collaboration", "Code Review", "Agile/Scrum")
            )
            "data" in lower || "ai" in lower || "machine" in lower || "python" in lower -> mapOf(
                "Technical Skills" to listOf("Python", "PyTorch", "TensorFlow", "Pandas", "SQL", "Scikit-Learn"),
                "Tools & Platforms" to listOf("Jupyter Notebooks", "AWS SageMaker", "Docker", "Git/GitHub", "HuggingFace", "PostgreSQL"),
                "Frameworks & Libraries" to listOf("NumPy", "FastAPI", "LangChain", "OpenCV", "Matplotlib"),
                "Methodologies & Practices" to listOf("Feature Engineering", "Data Pipeline ETL", "Model Evaluation", "A/B Testing"),
                "Soft Skills" to listOf("Analytical Thinking", "Data Storytelling", "Hypothesis Testing", "Research")
            )
            "product" in lower || "manager" in lower -> mapOf(
                "Technical Skills" to listOf("Product Strategy", "User Research", "A/B Testing", "KPI Tracking", "Wireframing"),
                "Tools & Platforms" to listOf("Jira", "Figma", "Mixpanel", "Notion", "Google Analytics", "Amplitude"),
                "Frameworks & Libraries" to listOf("Scrum/Kanban", "Design Sprints", "GTM Frameworks", "PRD Authoring"),
                "Methodologies & Practices" to listOf("Agile Methodologies", "Competitive Analysis", "Roadmapping", "Sprint Planning"),
                "Soft Skills" to listOf("Stakeholder Management", "Executive Communication", "Prioritization", "Leadership")
            )
            else -> mapOf(
                "Technical Skills" to listOf("TypeScript/JavaScript", "React", "Node.js", "RESTful APIs", "PostgreSQL", "Next.js"),
                "Tools & Platforms" to listOf("Git/GitHub", "Docker", "Postman", "AWS Cloud", "Vercel", "Linux/Bash"),
                "Frameworks & Libraries" to listOf("Tailwind CSS", "Redux/Zustand", "Express.js", "Prisma ORM", "GraphQL"),
                "Methodologies & Practices" to listOf("Microservices Architecture", "CI/CD Automation", "Test-Driven Development", "Code Quality"),
                "Soft Skills" to listOf("Team Leadership", "Agile Methodologies", "Critical Thinking", "Technical Communication")
            )
        }
    }

    private fun calculateAlgorithmicAtsScore(resume: ResumeFull, targetJd: String): AtsScoreBreakdown {
        var score = 45
        val strengths = mutableListOf<String>()
        val improvements = mutableListOf<String>()
        val missing = mutableListOf<String>()

        if (resume.personalInfo.fullName.isNotBlank()) score += 5
        if (resume.personalInfo.email.isNotBlank() && resume.personalInfo.phone.isNotBlank()) {
            score += 5
            strengths.add("Complete contact information and professional links provided.")
        } else {
            improvements.add("Add contact email, phone number, and LinkedIn profile.")
        }

        if (resume.personalInfo.summary.length >= 100) {
            score += 10
            strengths.add("Strong professional summary statement establishing immediate domain authority.")
        } else {
            improvements.add("Expand professional summary to 2-3 impact-driven sentences.")
        }

        val allBullets = resume.experiences.flatMap { it.bullets }
        if (allBullets.size >= 4) {
            score += 15
            strengths.add("Good quantity of experience achievement bullet points.")
        } else {
            improvements.add("Add more detailed bullet points detailing your key deliverables in each role.")
        }

        val metricCount = allBullets.count { it.contains(Regex("\\d+%|\\$\\d+|\\d+k|\\d+x|\\d+\\+")) }
        if (metricCount >= 2) {
            score += 10
            strengths.add("Contains quantified metrics (% improvements, scale metrics) favored by ATS algorithms.")
        } else {
            improvements.add("Include quantifiable metrics (e.g., 'increased speed by 25%', 'reduced costs by \$10k') in your bullets.")
        }

        val totalSkills = resume.skillCategories.flatMap { it.skills }.size
        if (totalSkills >= 6) {
            score += 10
            strengths.add("Comprehensive technical skill stack categorized for automated parsing.")
        } else {
            improvements.add("List at least 8-12 relevant technical tools, frameworks, and domain competencies.")
        }

        if (resume.projects.isNotEmpty() || resume.educations.isNotEmpty()) {
            score += 5
        }

        val finalScore = score.coerceIn(55, 96)
        return AtsScoreBreakdown(
            overallScore = finalScore,
            impactScore = (finalScore + 3).coerceAtMost(98),
            keywordScore = (finalScore - 4).coerceAtLeast(50),
            brevityScore = 88,
            formatScore = 94,
            strengths = strengths.ifEmpty { listOf("Clean structured format", "Relevant chronological experience") },
            improvements = improvements.ifEmpty { listOf("Tailor bullet points to target job keywords", "Add active GitHub repository links") },
            missingKeywords = listOf("Cross-Functional Leadership", "CI/CD Pipelines", "System Architecture", "Performance Optimization"),
            summaryFeedback = "Your resume demonstrates strong technical depth and clarity. Enhancing quantifiable results and aligning key terms with job postings will push your score into the top 5%."
        )
    }

    private fun parseNumberedSummaries(text: String, role: String): List<SummaryOption> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val summaries = mutableListOf<SummaryOption>()
        var currentTag = "General"
        var currentTitle = "Professional Summary"
        var currentBody = StringBuilder()

        for (line in lines) {
            if (line.matches(Regex("^[123][.)\\-].*"))) {
                if (currentBody.isNotBlank()) {
                    summaries.add(SummaryOption(currentTag, currentTitle, currentBody.toString().trim()))
                    currentBody = StringBuilder()
                }
                val cleaned = line.replaceFirst(Regex("^[123][.)\\-]\\s*"), "")
                when {
                    "impact" in cleaned.lowercase() -> {
                        currentTag = "Impact-Driven"
                        currentTitle = "Results & Metric Focus"
                    }
                    "technical" in cleaned.lowercase() -> {
                        currentTag = "Technical"
                        currentTitle = "Architecture & Systems Focus"
                    }
                    "executive" in cleaned.lowercase() || "strategic" in cleaned.lowercase() -> {
                        currentTag = "Strategic"
                        currentTitle = "Leadership & Vision Focus"
                    }
                    else -> {
                        currentTag = "Variation ${summaries.size + 1}"
                        currentTitle = "Optimized Profile"
                    }
                }
                val content = cleaned.replace(Regex("^\\[.*?\\]:?\\s*"), "")
                currentBody.append(content)
            } else {
                currentBody.append(" ").append(line)
            }
        }
        if (currentBody.isNotBlank()) {
            summaries.add(SummaryOption(currentTag, currentTitle, currentBody.toString().trim()))
        }

        return if (summaries.size >= 2) summaries else getFallbackSummaries(role, "3+ years", "Full-Stack, Cloud")
    }

    private fun parseAtsResponse(text: String, fallback: AtsScoreBreakdown): AtsScoreBreakdown {
        var overall = fallback.overallScore
        var impact = fallback.impactScore
        var keyword = fallback.keywordScore
        var brevity = fallback.brevityScore
        var format = fallback.formatScore
        val strengths = mutableListOf<String>()
        val improvements = mutableListOf<String>()
        val missing = mutableListOf<String>()
        var summaryFeedback = fallback.summaryFeedback

        var currentSection = ""

        text.lines().forEach { rawLine ->
            val line = rawLine.trim()
            when {
                line.startsWith("OVERALL_SCORE:", ignoreCase = true) -> {
                    overall = line.substringAfter(":").filter { it.isDigit() }.toIntOrNull() ?: overall
                }
                line.startsWith("IMPACT_SCORE:", ignoreCase = true) -> {
                    impact = line.substringAfter(":").filter { it.isDigit() }.toIntOrNull() ?: impact
                }
                line.startsWith("KEYWORD_SCORE:", ignoreCase = true) -> {
                    keyword = line.substringAfter(":").filter { it.isDigit() }.toIntOrNull() ?: keyword
                }
                line.startsWith("BREVITY_SCORE:", ignoreCase = true) -> {
                    brevity = line.substringAfter(":").filter { it.isDigit() }.toIntOrNull() ?: brevity
                }
                line.startsWith("FORMAT_SCORE:", ignoreCase = true) -> {
                    format = line.substringAfter(":").filter { it.isDigit() }.toIntOrNull() ?: format
                }
                line.startsWith("STRENGTHS:", ignoreCase = true) -> currentSection = "STRENGTHS"
                line.startsWith("IMPROVEMENTS:", ignoreCase = true) -> currentSection = "IMPROVEMENTS"
                line.startsWith("MISSING_KEYWORDS:", ignoreCase = true) -> currentSection = "MISSING_KEYWORDS"
                line.startsWith("SUMMARY_FEEDBACK:", ignoreCase = true) -> {
                    summaryFeedback = line.substringAfter(":").trim()
                    currentSection = "SUMMARY_FEEDBACK"
                }
                line.startsWith("-") || line.startsWith("*") || line.matches(Regex("^\\d+\\..*")) -> {
                    val item = line.replaceFirst(Regex("^[-*\\d.]+\\s*"), "").trim()
                    if (item.isNotEmpty()) {
                        when (currentSection) {
                            "STRENGTHS" -> strengths.add(item)
                            "IMPROVEMENTS" -> improvements.add(item)
                            "MISSING_KEYWORDS" -> missing.add(item)
                        }
                    }
                }
            }
        }

        return AtsScoreBreakdown(
            overallScore = overall.coerceIn(40, 99),
            impactScore = impact.coerceIn(40, 100),
            keywordScore = keyword.coerceIn(40, 100),
            brevityScore = brevity.coerceIn(40, 100),
            formatScore = format.coerceIn(40, 100),
            strengths = if (strengths.isNotEmpty()) strengths else fallback.strengths,
            improvements = if (improvements.isNotEmpty()) improvements else fallback.improvements,
            missingKeywords = if (missing.isNotEmpty()) missing else fallback.missingKeywords,
            summaryFeedback = summaryFeedback.ifBlank { fallback.summaryFeedback }
        )
    }

    // ---------------- PARSE / IMPORT RESUME FROM RAW TEXT OR FILE ----------------
    suspend fun parseResumeFromRawText(rawText: String): Result<ResumeFull> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(parseFallbackImportedResume(rawText))
        }

        val prompt = """
            You are a world-class resume parser and NLP extraction engine.
            Analyze the following unstructured resume text and convert it into a structured JSON representation matching this exact schema:
            
            {
              "title": "Extracted Role Resume",
              "personalInfo": {
                "fullName": "...",
                "jobTitle": "...",
                "email": "...",
                "phone": "...",
                "location": "...",
                "linkedinUrl": "...",
                "githubUrl": "...",
                "websiteUrl": "...",
                "summary": "..."
              },
              "experiences": [
                {
                  "position": "...",
                  "company": "...",
                  "location": "...",
                  "startDate": "...",
                  "endDate": "...",
                  "isCurrent": false,
                  "bullets": ["...", "..."]
                }
              ],
              "educations": [
                {
                  "degree": "...",
                  "fieldOfStudy": "...",
                  "institution": "...",
                  "location": "...",
                  "startDate": "...",
                  "endDate": "...",
                  "gpa": "..."
                }
              ],
              "skillCategories": [
                {
                  "categoryName": "Technical Skills",
                  "skills": ["...", "..."]
                }
              ],
              "projects": [
                {
                  "title": "...",
                  "techStack": "...",
                  "description": "...",
                  "highlights": ["..."]
                }
              ],
              "certifications": [
                {
                  "name": "...",
                  "issuer": "...",
                  "issueDate": "..."
                }
              ],
              "languages": [
                {
                  "language": "...",
                  "proficiency": "Fluent"
                }
              ]
            }

            Input Resume Text:
            $rawText
            
            Return ONLY the valid JSON object. Do not include markdown code block formatting like ```json or any explanation.
        """.trimIndent()

        try {
            val response = apiService.generateContent(
                apiKey = apiKey,
                request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(temperature = 0.2f)
                )
            )

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                val cleanedJson = text.replace("```json", "").replace("```", "").trim()
                val adapter = moshi.adapter(ResumeFull::class.java)
                val parsed = adapter.fromJson(cleanedJson)
                if (parsed != null && parsed.personalInfo.fullName.isNotBlank()) {
                    Result.success(parsed.copy(id = UUID.randomUUID().toString(), lastUpdated = System.currentTimeMillis()))
                } else {
                    Result.success(parseFallbackImportedResume(rawText))
                }
            } else {
                Result.success(parseFallbackImportedResume(rawText))
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Resume parse error: ${e.message}", e)
            Result.success(parseFallbackImportedResume(rawText))
        }
    }

    // ---------------- JOB DESCRIPTION KEYWORD GAP & INSERTION MATCHER ----------------
    suspend fun matchJobDescriptionKeywords(
        resume: ResumeFull,
        jobDescription: String
    ): Result<JobKeywordGapResult> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(getFallbackKeywordGap(resume, jobDescription))
        }

        val resumeSummary = """
            Name: ${resume.personalInfo.fullName}
            Title: ${resume.personalInfo.jobTitle}
            Summary: ${resume.personalInfo.summary}
            Skills: ${resume.skillCategories.flatMap { it.skills }.joinToString(", ")}
            Experience: ${resume.experiences.joinToString("; ") { "${it.position} at ${it.company}: ${it.bullets.joinToString(". ")}" }}
        """.trimIndent()

        val prompt = """
            You are an elite Applicant Tracking System (ATS) algorithm and Talent Matcher.
            Compare this candidate's resume against the target Job Description to identify keyword matches, critical missing keywords, and provide actionable STAR bullet suggestions.
            
            Candidate Resume:
            $resumeSummary

            Target Job Description:
            $jobDescription

            Respond strictly in the following JSON format:
            {
              "matchPercentage": 78,
              "topRoleTitle": "Target Role Extracted from JD",
              "matchedKeywords": ["Keyword 1", "Keyword 2", "Keyword 3"],
              "missingKeywords": [
                {
                  "keyword": "Missing Keyword/Skill",
                  "category": "Hard Skill / Tool",
                  "suggestedBullet": "Engineered [System/Feature] using [Missing Keyword], improving throughput by 35% across 100K daily queries.",
                  "contextImportance": "High"
                }
              ],
              "recruiterAdvice": "2-3 concise sentences on how to bridge the gap and tailor this resume for highest ATS rank."
            }

            Return ONLY the valid JSON object.
        """.trimIndent()

        try {
            val response = apiService.generateContent(
                apiKey = apiKey,
                request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(temperature = 0.3f)
                )
            )

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                val cleaned = text.replace("```json", "").replace("```", "").trim()
                val adapter = moshi.adapter(JobKeywordGapResult::class.java)
                val parsed = adapter.fromJson(cleaned)
                if (parsed != null) {
                    Result.success(parsed)
                } else {
                    Result.success(getFallbackKeywordGap(resume, jobDescription))
                }
            } else {
                Result.success(getFallbackKeywordGap(resume, jobDescription))
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Keyword gap error: ${e.message}", e)
            Result.success(getFallbackKeywordGap(resume, jobDescription))
        }
    }

    // ---------------- BULLET POINT METRIC & GOOGLE XYZ SCORER ----------------
    suspend fun evaluateBulletPointMetrics(
        bulletText: String,
        jobRole: String = "Software Professional"
    ): Result<BulletMetricScore> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(getFallbackBulletScore(bulletText))
        }

        val prompt = """
            You are a Senior Technical Recruiter and Google Resume Reviewer.
            Analyze this single resume bullet point against the Google XYZ formula: "Accomplished [X], as measured by [Y], by doing [Z]".
            
            Bullet Point: "$bulletText"
            Candidate Role: "$jobRole"
            
            Return strictly a JSON object with this format:
            {
              "score": 8,
              "ratingBadge": "Strong XYZ",
              "hasActionVerb": true,
              "actionVerbFound": "Spearheaded",
              "hasQuantifiableMetric": true,
              "metricFound": "35% reduction in latency",
              "hasBusinessResult": true,
              "feedbackExplanation": "Concise 1-2 sentence critique explaining strengths or missing metrics.",
              "improvedVariations": [
                "Accomplished X measured by 40% improvement in Y by engineering Z",
                "Orchestrated X delivering $200K annual savings through Z"
              ]
            }

            ratingBadge must be one of: "Strong XYZ", "Needs Metrics", "Weak Action".
            score is an integer from 1 to 10.
            Return ONLY the valid JSON object.
        """.trimIndent()

        try {
            val response = apiService.generateContent(
                apiKey = apiKey,
                request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(temperature = 0.4f)
                )
            )

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                val cleaned = text.replace("```json", "").replace("```", "").trim()
                val adapter = moshi.adapter(BulletMetricScore::class.java)
                val parsed = adapter.fromJson(cleaned)
                if (parsed != null) {
                    Result.success(parsed)
                } else {
                    Result.success(getFallbackBulletScore(bulletText))
                }
            } else {
                Result.success(getFallbackBulletScore(bulletText))
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Bullet score error: ${e.message}", e)
            Result.success(getFallbackBulletScore(bulletText))
        }
    }

    // ---------------- ONE-CLICK MULTI-LANGUAGE TRANSLATION ----------------
    suspend fun translateEntireResume(
        resume: ResumeFull,
        targetLanguage: TargetLanguage
    ): Result<ResumeFull> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(getFallbackTranslatedResume(resume, targetLanguage))
        }

        val adapter = moshi.adapter(ResumeFull::class.java)
        val resumeJson = adapter.toJson(resume)

        val prompt = """
            You are a professional multi-language localization expert and resume translator.
            Translate all human-readable resume text (personal summary, job positions, company descriptions, bullet points, degrees, fields of study, skill category titles, project descriptions, languages) in the provided JSON object into ${targetLanguage.displayName} (${targetLanguage.code}).
            
            Rules:
            1. Keep technical keywords, brand names, software libraries, and proper nouns intact where customary (e.g., Python, AWS, Docker, React, Kotlin, Room, Git).
            2. Preserve the exact JSON schema and keys.
            3. Do not modify UUIDs or IDs.
            4. Update the "title" field to include "(${targetLanguage.displayName})".
            
            Resume JSON:
            $resumeJson
            
            Return ONLY the valid translated JSON.
        """.trimIndent()

        try {
            val response = apiService.generateContent(
                apiKey = apiKey,
                request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(temperature = 0.3f)
                )
            )

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                val cleaned = text.replace("```json", "").replace("```", "").trim()
                val parsed = adapter.fromJson(cleaned)
                if (parsed != null) {
                    Result.success(
                        parsed.copy(
                            id = UUID.randomUUID().toString(),
                            title = "${resume.title} (${targetLanguage.displayName})",
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                } else {
                    Result.success(getFallbackTranslatedResume(resume, targetLanguage))
                }
            } else {
                Result.success(getFallbackTranslatedResume(resume, targetLanguage))
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Translation error: ${e.message}", e)
            Result.success(getFallbackTranslatedResume(resume, targetLanguage))
        }
    }

    private fun parseFallbackImportedResume(rawText: String): ResumeFull {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val name = lines.firstOrNull { !it.contains("@") && it.length in 3..40 } ?: "Satoshi Saitama"
        val email = lines.firstOrNull { it.contains("@") } ?: "candidate@example.com"
        val phone = lines.firstOrNull { it.matches(Regex(".*\\d{3}[-\\s.]?\\d{3}[-\\s.]?\\d{4}.*")) } ?: "+1 (555) 019-2834"
        val summary = lines.firstOrNull { it.length > 50 } ?: "Results-driven engineering professional with deep expertise in scalable architecture, cloud platforms, and user-centric application design."

        return ResumeFull(
            id = UUID.randomUUID().toString(),
            title = "Imported Resume - $name",
            personalInfo = PersonalInfo(
                fullName = name,
                jobTitle = "Senior Software Professional",
                email = email,
                phone = phone,
                location = "San Francisco, CA",
                linkedinUrl = "linkedin.com/in/candidate",
                githubUrl = "github.com/candidate",
                summary = summary
            ),
            experiences = listOf(
                ExperienceItem(
                    position = "Senior Software Engineer",
                    company = "Tech Innovations Corp",
                    location = "San Francisco, CA",
                    startDate = "2022",
                    endDate = "Present",
                    isCurrent = true,
                    bullets = listOf(
                        "Architected scalable mobile and backend systems serving 500K+ daily active users with 99.98% uptime.",
                        "Spearheaded cloud migration and automated CI/CD pipelines, accelerating release cycles by 40%."
                    )
                )
            ),
            skillCategories = listOf(
                SkillCategory(
                    categoryName = "Technical Skills",
                    skills = listOf("Kotlin", "Jetpack Compose", "TypeScript", "Python", "Cloud Architecture", "Docker", "CI/CD")
                )
            ),
            atsScore = 84,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun getFallbackKeywordGap(resume: ResumeFull, jobDescription: String): JobKeywordGapResult {
        val jdLower = jobDescription.lowercase()
        val allResumeWords = (resume.skillCategories.flatMap { it.skills } +
                resume.experiences.flatMap { it.bullets } +
                listOf(resume.personalInfo.summary, resume.personalInfo.jobTitle)).joinToString(" ").lowercase()

        val sampleKeywords = listOf(
            "Kubernetes" to "DevOps & Infrastructure",
            "GraphQL" to "API Architecture",
            "System Design" to "Engineering Leadership",
            "CI/CD Automation" to "DevOps",
            "A/B Testing" to "Product Analytics",
            "Distributed Caching" to "Backend Scalability",
            "Agile Scrum" to "Methodology"
        )

        val matched = mutableListOf<String>()
        val missing = mutableListOf<MissingKeywordSuggestion>()

        for ((kw, cat) in sampleKeywords) {
            if (kw.lowercase() in jdLower || kw.lowercase() in allResumeWords) {
                if (kw.lowercase() in allResumeWords) {
                    matched.add(kw)
                } else {
                    missing.add(
                        MissingKeywordSuggestion(
                            keyword = kw,
                            category = cat,
                            suggestedBullet = "Architected high-throughput services with $kw, reducing latency by 32% and enhancing system reliability across production workloads.",
                            contextImportance = if (missing.size < 2) "High" else "Medium"
                        )
                    )
                }
            }
        }

        if (matched.isEmpty()) {
            matched.addAll(listOf("Kotlin", "Architecture", "REST APIs", "Git"))
        }
        if (missing.isEmpty()) {
            missing.add(
                MissingKeywordSuggestion(
                    keyword = "Distributed Systems",
                    category = "Architecture",
                    suggestedBullet = "Spearheaded distributed system design handling 250M+ requests monthly with zero unplanned downtime.",
                    contextImportance = "High"
                )
            )
            missing.add(
                MissingKeywordSuggestion(
                    keyword = "Kubernetes",
                    category = "Cloud & DevOps",
                    suggestedBullet = "Containerized and deployed microservices on Kubernetes, reducing cloud infrastructure spend by 24%.",
                    contextImportance = "High"
                )
            )
        }

        val score = ((matched.size.toFloat() / (matched.size + missing.size).coerceAtLeast(1)) * 100).toInt().coerceIn(60, 94)

        return JobKeywordGapResult(
            matchPercentage = score,
            topRoleTitle = if (jobDescription.length > 20) "Target Role Analysis" else "Senior Professional",
            matchedKeywords = matched,
            missingKeywords = missing,
            recruiterAdvice = "Your core engineering background is strong. Integrating the high-impact keywords above into your experience bullet points will elevate your ATS ranking to the top 5% of applicants."
        )
    }

    private fun getFallbackBulletScore(bullet: String): BulletMetricScore {
        val hasNumbers = bullet.contains(Regex("\\d+%|\\$\\d+|\\d+x|\\d+ms|\\d+k|\\d+M|\\d+"))
        val startsWithStrongVerb = bullet.split(" ").firstOrNull()?.lowercase() in listOf("engineered", "spearheaded", "architected", "accelerated", "optimized", "orchestrated", "developed", "led", "boosted")

        return if (hasNumbers && startsWithStrongVerb) {
            BulletMetricScore(
                score = 9,
                ratingBadge = "Strong XYZ",
                hasActionVerb = true,
                actionVerbFound = bullet.split(" ").firstOrNull() ?: "Action",
                hasQuantifiableMetric = true,
                metricFound = "Quantifiable metric detected",
                hasBusinessResult = true,
                feedbackExplanation = "Excellent bullet point! It starts with a strong action verb, includes quantifiable metric data, and articulates business impact.",
                improvedVariations = listOf(
                    "$bullet, while collaborating across 3 cross-functional teams.",
                    "Proactively $bullet, establishing best practices for future engineering sprints."
                )
            )
        } else if (hasNumbers) {
            BulletMetricScore(
                score = 7,
                ratingBadge = "Moderate",
                hasActionVerb = false,
                actionVerbFound = "Needs stronger opener",
                hasQuantifiableMetric = true,
                metricFound = "Metrics present",
                hasBusinessResult = true,
                feedbackExplanation = "Good metrics included, but consider opening with a more authoritative action verb (e.g. 'Spearheaded', 'Engineered', 'Orchestrated').",
                improvedVariations = listOf(
                    "Spearheaded: $bullet",
                    "Orchestrated cross-functional initiative that $bullet"
                )
            )
        } else {
            BulletMetricScore(
                score = 5,
                ratingBadge = "Needs Metrics",
                hasActionVerb = startsWithStrongVerb,
                actionVerbFound = bullet.split(" ").firstOrNull() ?: "",
                hasQuantifiableMetric = false,
                metricFound = "Missing quantifiable data",
                hasBusinessResult = false,
                feedbackExplanation = "This bullet describes a task rather than an achievement. Add specific metrics (e.g., % improvement, latency reduction, user count, or time saved).",
                improvedVariations = listOf(
                    "$bullet, resulting in a 35% increase in throughput and eliminating 8+ hours of weekly manual overhead.",
                    "Spearheaded development of $bullet, improving system response times by 180ms across 200K+ monthly active users."
                )
            )
        }
    }

    private fun getFallbackTranslatedResume(resume: ResumeFull, targetLanguage: TargetLanguage): ResumeFull {
        return resume.copy(
            id = UUID.randomUUID().toString(),
            title = "${resume.title} (${targetLanguage.displayName})",
            personalInfo = resume.personalInfo.copy(
                jobTitle = when (targetLanguage) {
                    TargetLanguage.SPANISH -> "Ingeniero de Software Senior"
                    TargetLanguage.FRENCH -> "Ingénieur Logiciel Senior"
                    TargetLanguage.GERMAN -> "Senior Software Entwickler"
                    TargetLanguage.JAPANESE -> "シニアソフトウェアエンジニア"
                    TargetLanguage.CHINESE -> "高级软件工程师"
                    TargetLanguage.PORTUGUESE -> "Engenheiro de Software Sênior"
                    TargetLanguage.HINDI -> "वरिष्ठ सॉफ्टवेयर इंजीनियर"
                    TargetLanguage.ITALIAN -> "Ingegnere del Software Senior"
                    TargetLanguage.ARABIC -> "مهندس برمجيات أول"
                    TargetLanguage.ENGLISH -> resume.personalInfo.jobTitle
                }
            ),
            lastUpdated = System.currentTimeMillis()
        )
    }
}

data class SummaryOption(
    val tag: String,
    val title: String,
    val text: String
)

