package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ResumeFull
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
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class ChatBotModel(val modelId: String, val displayName: String, val badge: String, val idealFor: String) {
    FLASH_3_5("gemini-3.5-flash", "Gemini 3.5 Flash", "Standard", "General Career Q&A, Resume Advice & STAR Bullets"),
    PRO_3_1("gemini-3.1-pro-preview", "Gemini 3.1 Pro", "Advanced", "Deep Resume Audit, Complex Mock Interviews & System Design"),
    FLASH_LITE_3_1("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash-Lite", "Fast", "Instant Rewrites, Quick Grammar & Fast Feedback")
}

enum class ChatBotPersona(
    val title: String,
    val subtitle: String,
    val initialGreeting: String,
    val systemPrompt: String
) {
    CAREER_COACH(
        title = "Career & Resume Coach",
        subtitle = "Resume optimization, positioning & job strategy",
        initialGreeting = "Hello! I am your AI Career & Executive Resume Coach. How can I assist you with your resume, portfolio, or career strategy today?",
        systemPrompt = """
            You are an elite Executive Career Coach and Fortune 500 Senior Recruiter.
            Your role is to guide the user to craft world-class resumes, elevate their personal brand, align their qualifications with target job descriptions, and formulate career advancement strategies.
            Provide actionable, specific advice. When rewriting bullets or text, follow Google's XYZ formula ("Accomplished [X], as measured by [Y], by doing [Z]").
            Keep your responses structured, clear, and encouraging. Use bullet points and bold highlights for readability.
        """.trimIndent()
    ),
    MOCK_INTERVIEWER(
        title = "Mock Job Interviewer",
        subtitle = "Roleplay behavioral & technical interviews with live feedback",
        initialGreeting = "Welcome to your Mock Interview session! What target position and company are we preparing for? Once you let me know, I'll ask you 1 realistic question at a time and critique your response using the STAR method.",
        systemPrompt = """
            You are an experienced Hiring Manager and Technical/Behavioral Interviewer at leading tech and Fortune 100 companies.
            Your goal is to conduct realistic, engaging mock interviews:
            1. Ask ONE question at a time.
            2. After the user responds, provide a brief critique highlighting strengths, rating their answer (e.g. 8/10), suggest improvements using the STAR method (Situation, Task, Action, Result), and then ask the next follow-up or next behavioral/technical question.
            3. Keep the tone professional, realistic, and constructive.
        """.trimIndent()
    ),
    SALARY_NEGOTIATOR(
        title = "Salary & Offer Negotiator",
        subtitle = "Negotiation tactics, counteroffers & total compensation",
        initialGreeting = "Hi there! Let's maximize your total compensation. Have you received a job offer, or are you preparing for salary discussions during your upcoming review?",
        systemPrompt = """
            You are a seasoned Executive Compensation Consultant and Talent Negotiation Specialist.
            Help the candidate negotiate base salary, performance bonuses, signing bonuses, stock grants (RSUs/options), remote work flexibility, and benefits.
            Provide tactical scripts, counteroffer email templates, market benchmarking advice, and psychological leverage strategies.
            Keep guidance ethical, confident, and professional.
        """.trimIndent()
    ),
    TECH_SPECIALIST(
        title = "Tech & Architecture Prep",
        subtitle = "System design, coding questions & technical depth",
        initialGreeting = "Ready to dive into technical prep! Whether you want to discuss Distributed Systems architecture, LeetCode algorithms, cloud design, or API patterns, ask away!",
        systemPrompt = """
            You are a Principal Software Architect and Staff Engineer.
            Help the candidate master system design interviews (scalability, caching, consistency, database sharding, latency trade-offs) and technical questions.
            Provide crisp architectural diagrams in ASCII/Markdown, explain trade-offs clearly, and review engineering design choices with deep rigor.
        """.trimIndent()
    )
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelName: String = "",
    val persona: ChatBotPersona = ChatBotPersona.CAREER_COACH,
    val isError: Boolean = false
)

interface DynamicGeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

class GeminiChatbotService {

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

    private val apiService = retrofit.create(DynamicGeminiApiService::class.java)

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun sendMultiTurnMessage(
        history: List<ChatMessage>,
        newMessage: String,
        persona: ChatBotPersona,
        model: ChatBotModel,
        attachedResume: ResumeFull? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(getFallbackChatbotResponse(persona, newMessage, attachedResume))
        }

        // Build resume context if attached
        val resumeContext = if (attachedResume != null) {
            """
                
                [ACTIVE CANDIDATE RESUME CONTEXT]:
                - Name: ${attachedResume.personalInfo.fullName}
                - Current / Target Title: ${attachedResume.personalInfo.jobTitle.ifBlank { attachedResume.title }}
                - Summary: ${attachedResume.personalInfo.summary}
                - Key Skills: ${attachedResume.skillCategories.flatMap { it.skills }.joinToString(", ")}
                - Target Job Description / Goals: ${attachedResume.targetJobDescription}
                - Experience Count: ${attachedResume.experiences.size} roles
                ${attachedResume.experiences.take(2).joinToString("\n") { "- ${it.position} at ${it.company} (${it.startDate} - ${it.endDate}): ${it.bullets.joinToString("; ")}" }}
            """.trimIndent()
        } else ""

        val systemPrompt = "${persona.systemPrompt}$resumeContext"

        val contents = mutableListOf<Content>()

        // Add history
        history.filter { !it.isError }.takeLast(10).forEach { msg ->
            contents.add(
                Content(
                    role = if (msg.role == "user") "user" else "model",
                    parts = listOf(Part(text = msg.text))
                )
            )
        }

        // Add current new user message
        contents.add(
            Content(
                role = "user",
                parts = listOf(Part(text = newMessage))
            )
        )

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(
                temperature = if (persona == ChatBotPersona.MOCK_INTERVIEWER) 0.8f else 0.65f,
                topP = 0.95f,
                maxOutputTokens = 2048
            )
        )

        try {
            val response = apiService.generateContent(
                model = model.modelId,
                apiKey = apiKey,
                request = request
            )

            val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!replyText.isNullOrBlank()) {
                Result.success(replyText.trim())
            } else {
                Result.success(getFallbackChatbotResponse(persona, newMessage, attachedResume))
            }
        } catch (e: Exception) {
            Log.e("GeminiChatbotService", "Chat error on model ${model.modelId}: ${e.message}", e)
            Result.success(getFallbackChatbotResponse(persona, newMessage, attachedResume))
        }
    }

    private fun getFallbackChatbotResponse(
        persona: ChatBotPersona,
        userMsg: String,
        resume: ResumeFull?
    ): String {
        val lower = userMsg.lowercase()
        val candidateName = resume?.personalInfo?.fullName?.ifBlank { "there" } ?: "there"
        val role = resume?.personalInfo?.jobTitle?.ifBlank { "Professional" } ?: "Professional"

        return when (persona) {
            ChatBotPersona.CAREER_COACH -> {
                when {
                    "bullet" in lower || "experience" in lower -> """
                        Here is a recommended STAR-format bullet point framework for your $role background:
                        
                        • **Spearheaded development of core features**, resulting in a **32% improvement in throughput** and reducing API response latency by **180ms**.
                        • **Collaborated across cross-functional engineering and product teams**, accelerating sprint delivery cadence by **25%**.
                        
                        **Coach's Tip**: Always highlight the specific action verb, the quantifiable metric, and the organizational impact!
                    """.trimIndent()

                    "skills" in lower || "keywords" in lower -> """
                        For a modern **$role** role, top ATS algorithms look for high-intent keywords:
                        
                        1. **Architecture & Design**: System Scalability, Cloud Infrastructure (AWS/GCP), CI/CD Automation
                        2. **Core Technologies**: Modern Frameworks, Clean Architecture, Reactive State Management
                        3. **Leadership**: Technical Mentorship, Agile/Scrum, Stakeholder Communication
                    """.trimIndent()

                    else -> """
                        Great question, $candidateName! For your **$role** profile:
                        
                        1. **Elevate Your Summary**: Ensure your opening 3-sentence summary highlights your years of experience, core technical mastery, and primary business impact.
                        2. **Quantify Results**: Replace passive descriptions with active metrics (e.g., *“Reduced cloud infrastructure costs by 22%”* instead of *“Managed servers”*).
                        3. **Tailor for ATS**: Match exact skill terms from target job postings in your Technical Skills section.
                        
                        Would you like me to rewrite a specific section of your resume or review your work experience bullets?
                    """.trimIndent()
                }
            }

            ChatBotPersona.MOCK_INTERVIEWER -> {
                """
                    **Interview Question**:
                    
                    *"Tell me about a time when you encountered a high-severity technical or project blocker under tight deadlines. How did you diagnose the issue, align your team, and what was the outcome?"*
                    
                    **Tip for your answer**: Structure your response using the **STAR method**:
                    - **Situation**: Context & stakes
                    - **Task**: Your specific responsibility
                    - **Action**: The direct steps and technical choices you made
                    - **Result**: The quantified outcome and key learnings
                    
                    Take your time and reply with your answer!
                """.trimIndent()
            }

            ChatBotPersona.SALARY_NEGOTIATOR -> {
                """
                    **Offer Negotiation Strategy**:
                    
                    1. **Express Gratitude & Excitement First**: *"I am thrilled about the opportunity to join the team and contribute to [Company's Key Goal]!"*
                    2. **Anchor with Market Data**: State your benchmarked compensation range based on your $role experience level and industry metrics.
                    3. **Negotiate Total Compensation**: If base salary has rigid band limits, request a signing bonus, additional equity grants (RSUs), or accelerated performance reviews at 6 months.
                    
                    Would you like an email script to counteroffer an initial package?
                """.trimIndent()
            }

            ChatBotPersona.TECH_SPECIALIST -> {
                """
                    **Technical Architecture & System Design Blueprint**:
                    
                    When designing a high-throughput distributed system for your $role interview:
                    
                    ```
                    [Client] -> [API Gateway / CDN] -> [Load Balancer]
                                                          |
                                      +-------------------+-------------------+
                                      |                                       |
                             [Microservice Pod A]                   [Microservice Pod B]
                                      |                                       |
                             [Redis Cache Cluster]                  [Kafka Event Bus]
                                      |                                       |
                             [Primary PostgreSQL]                   [Worker Nodes / S3]
                    ```
                    
                    **Key Discussion Points**:
                    • **Caching Strategy**: Cache-Aside with TTL & Redis cluster
                    • **Resilience**: Circuit breakers, rate limiting, and exponential backoff
                    • **Data Consistency**: Eventual consistency vs strict ACID guarantees
                """.trimIndent()
            }
        }
    }
}
