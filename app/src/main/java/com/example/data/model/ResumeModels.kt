package com.example.data.model

import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class PersonalInfo(
    val fullName: String = "",
    val jobTitle: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val websiteUrl: String = "",
    val linkedinUrl: String = "",
    val githubUrl: String = "",
    val summary: String = "",
    val photoUri: String = "",
    val avatarColorHex: String = "#4F46E5",
    val includeQrCode: Boolean = true,
    val qrCodeTarget: String = "LinkedIn" // "LinkedIn", "GitHub", "Website"
)

@JsonClass(generateAdapter = true)
data class ExperienceItem(
    val id: String = UUID.randomUUID().toString(),
    val position: String = "",
    val company: String = "",
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val isCurrent: Boolean = false,
    val description: String = "",
    val bullets: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class EducationItem(
    val id: String = UUID.randomUUID().toString(),
    val degree: String = "",
    val fieldOfStudy: String = "",
    val institution: String = "",
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val gpa: String = "",
    val honors: String = "",
    val highlights: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class EducationSuggestions(
    val relevantCoursework: List<String> = emptyList(),
    val honorsAndAchievements: List<String> = emptyList(),
    val academicBulletPoints: List<String> = emptyList(),
    val capstoneHighlights: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SkillCategory(
    val id: String = UUID.randomUUID().toString(),
    val categoryName: String = "Technical Skills",
    val skills: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ProjectItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val techStack: String = "",
    val description: String = "",
    val liveUrl: String = "",
    val githubUrl: String = "",
    val highlights: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CertificationItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val issuer: String = "",
    val issueDate: String = "",
    val credentialUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class LanguageItem(
    val id: String = UUID.randomUUID().toString(),
    val language: String = "",
    val proficiency: String = "Fluent" // Native, Fluent, Professional, Intermediate
)

enum class ResumeTemplate(val displayName: String, val description: String) {
    MODERN("Modern Dynamic", "Clean two-column layout with bold accents, ideal for tech & design"),
    CLASSIC_ATS("Classic ATS", "Single column, high-readability layout optimized for ATS parsers"),
    MINIMALIST("Clean Minimal", "Spacious typography-focused design with elegant hierarchy"),
    EXECUTIVE("Executive Bordeaux", "Refined header with dignified section dividers for leadership"),
    TECH_SLATE("Tech Indigo", "Modern header with tech badges, metrics highlights, and dark accents")
}

@JsonClass(generateAdapter = true)
data class ResumeFull(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled Resume",
    val template: ResumeTemplate = ResumeTemplate.MODERN,
    val accentColorHex: String = "#4F46E5",
    val fontStyle: String = "SansSerif",
    val isPublic: Boolean = true,
    val shareSlug: String = UUID.randomUUID().toString().take(8),
    val targetJobDescription: String = "",
    val personalInfo: PersonalInfo = PersonalInfo(),
    val experiences: List<ExperienceItem> = emptyList(),
    val educations: List<EducationItem> = emptyList(),
    val skillCategories: List<SkillCategory> = emptyList(),
    val projects: List<ProjectItem> = emptyList(),
    val certifications: List<CertificationItem> = emptyList(),
    val languages: List<LanguageItem> = emptyList(),
    val atsScore: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class AtsScoreBreakdown(
    val overallScore: Int = 0,
    val impactScore: Int = 0,
    val keywordScore: Int = 0,
    val brevityScore: Int = 0,
    val formatScore: Int = 0,
    val strengths: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    val missingKeywords: List<String> = emptyList(),
    val summaryFeedback: String = ""
)

@JsonClass(generateAdapter = true)
data class UserProfile(
    val id: String = "user_default",
    val name: String = "Alex Morgan",
    val email: String = "alex.morgan@example.com",
    val role: String = "Senior Software Engineer",
    val avatarUrl: String = "",
    val isLoggedIn: Boolean = true,
    val planType: String = "Pro Plan (Gemini AI Included)"
)

@JsonClass(generateAdapter = true)
data class MissingKeywordSuggestion(
    val keyword: String = "",
    val category: String = "Skill", // "Hard Skill", "Soft Skill", "Tool / Framework", "Certification"
    val suggestedBullet: String = "",
    val contextImportance: String = "High" // "High", "Medium", "Bonus"
)

@JsonClass(generateAdapter = true)
data class JobKeywordGapResult(
    val matchPercentage: Int = 0,
    val matchedKeywords: List<String> = emptyList(),
    val missingKeywords: List<MissingKeywordSuggestion> = emptyList(),
    val topRoleTitle: String = "",
    val recruiterAdvice: String = ""
)

@JsonClass(generateAdapter = true)
data class BulletMetricScore(
    val score: Int = 8, // 1 to 10
    val ratingBadge: String = "Strong XYZ", // "Strong XYZ", "Needs Metrics", "Weak Action"
    val hasActionVerb: Boolean = true,
    val actionVerbFound: String = "",
    val hasQuantifiableMetric: Boolean = true,
    val metricFound: String = "",
    val hasBusinessResult: Boolean = true,
    val feedbackExplanation: String = "",
    val improvedVariations: List<String> = emptyList()
)

enum class TargetLanguage(val code: String, val displayName: String, val flag: String) {
    SPANISH("es", "Spanish", "🇪🇸"),
    FRENCH("fr", "French", "🇫🇷"),
    GERMAN("de", "German", "🇩🇪"),
    JAPANESE("ja", "Japanese", "🇯🇵"),
    CHINESE("zh", "Chinese (Simplified)", "🇨🇳"),
    PORTUGUESE("pt", "Portuguese", "🇵🇹"),
    HINDI("hi", "Hindi", "🇮🇳"),
    ITALIAN("it", "Italian", "🇮🇹"),
    ARABIC("ar", "Arabic", "🇦🇪"),
    ENGLISH("en", "English", "🇺🇸")
}

