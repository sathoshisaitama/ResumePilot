package com.example.data.repository

import com.example.data.db.ResumeDao
import com.example.data.db.ResumeEntity
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class ResumeRepository(private val resumeDao: ResumeDao) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val resumeAdapter = moshi.adapter(ResumeFull::class.java)

    val allResumes: Flow<List<ResumeFull>> = resumeDao.getAllResumes().map { entities ->
        entities.mapNotNull { entity ->
            try {
                resumeAdapter.fromJson(entity.resumeDataJson)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getResumeById(id: String): ResumeFull? = withContext(Dispatchers.IO) {
        val entity = resumeDao.getResumeById(id) ?: return@withContext null
        try {
            resumeAdapter.fromJson(entity.resumeDataJson)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveResume(resume: ResumeFull) = withContext(Dispatchers.IO) {
        val json = resumeAdapter.toJson(resume)
        val entity = ResumeEntity(
            id = resume.id,
            title = resume.title.ifBlank { "Untitled Resume" },
            template = resume.template,
            accentColorHex = resume.accentColorHex,
            fontStyle = resume.fontStyle,
            isPublic = resume.isPublic,
            shareSlug = resume.shareSlug,
            resumeDataJson = json,
            atsScore = resume.atsScore,
            lastUpdated = System.currentTimeMillis(),
            createdAt = resume.createdAt
        )
        resumeDao.insertResume(entity)
    }

    suspend fun deleteResume(id: String) = withContext(Dispatchers.IO) {
        resumeDao.deleteResumeById(id)
    }

    suspend fun duplicateResume(id: String): ResumeFull? = withContext(Dispatchers.IO) {
        val original = getResumeById(id) ?: return@withContext null
        val newId = UUID.randomUUID().toString()
        val duplicated = original.copy(
            id = newId,
            title = "${original.title} (Copy)",
            shareSlug = UUID.randomUUID().toString().take(8),
            lastUpdated = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
        saveResume(duplicated)
        duplicated
    }

    suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
        val count = resumeDao.getResumeCount()
        if (count == 0) {
            seedSampleResumes()
        }
    }

    private suspend fun seedSampleResumes() {
        val devResume = ResumeFull(
            id = "sample_fullstack_dev",
            title = "Full Stack Engineer",
            template = ResumeTemplate.MODERN,
            accentColorHex = "#4F46E5",
            fontStyle = "SansSerif",
            isPublic = true,
            atsScore = 88,
            personalInfo = PersonalInfo(
                fullName = "Alex Chen",
                jobTitle = "Senior Full-Stack Developer",
                email = "alex.chen.dev@gmail.com",
                phone = "+1 (555) 234-5678",
                location = "San Francisco, CA",
                websiteUrl = "alexchen.dev",
                linkedinUrl = "linkedin.com/in/alexchendev",
                githubUrl = "github.com/alexchen",
                summary = "Results-driven Senior Full-Stack Engineer with 6+ years of experience architecting high-throughput distributed systems and responsive web applications. Proven track record in scaling microservices to 10M+ monthly users, reducing API latencies by 42%, and mentoring engineering teams."
            ),
            experiences = listOf(
                ExperienceItem(
                    id = "exp_1",
                    position = "Lead Full-Stack Engineer",
                    company = "CloudScale Technologies",
                    location = "San Francisco, CA",
                    startDate = "Jan 2022",
                    endDate = "Present",
                    isCurrent = true,
                    bullets = listOf(
                        "Architected scalable microservices using Kotlin, Node.js, and Redis, serving 12M+ monthly active users with 99.99% uptime.",
                        "Spearheaded the migration of monolithic frontend to modular React/TypeScript, slashing build times by 55% and page load by 1.8s.",
                        "Designed real-time collaborative dashboard features utilized by 40k+ enterprise teams worldwide."
                    )
                ),
                ExperienceItem(
                    id = "exp_2",
                    position = "Full-Stack Software Engineer",
                    company = "Nexus Software Labs",
                    location = "Austin, TX",
                    startDate = "Aug 2019",
                    endDate = "Dec 2021",
                    isCurrent = false,
                    bullets = listOf(
                        "Developed automated CI/CD deployment pipelines using Docker and Kubernetes, reducing deployment cycle times by 60%.",
                        "Implemented secure OAuth2 authentication and role-based access control (RBAC) across 8 integrated client applications.",
                        "Optimized PostgreSQL database queries and indexes, decreasing database read latency by 38% under peak load."
                    )
                )
            ),
            educations = listOf(
                EducationItem(
                    id = "edu_1",
                    degree = "B.S. in Computer Science",
                    fieldOfStudy = "Software Engineering & Distributed Systems",
                    institution = "University of California, Berkeley",
                    location = "Berkeley, CA",
                    startDate = "2015",
                    endDate = "2019",
                    gpa = "3.85 / 4.0",
                    honors = "Dean's Honor List (4 semesters)"
                )
            ),
            skillCategories = listOf(
                SkillCategory(
                    id = "cat_tech",
                    categoryName = "Languages & Frameworks",
                    skills = listOf("TypeScript", "React", "Node.js", "Kotlin", "Python", "GraphQL", "Next.js", "Tailwind CSS")
                ),
                SkillCategory(
                    id = "cat_tools",
                    categoryName = "Cloud, Databases & DevOps",
                    skills = listOf("AWS", "PostgreSQL", "MongoDB", "Docker", "Kubernetes", "Redis", "Kafka", "Git")
                ),
                SkillCategory(
                    id = "cat_soft",
                    categoryName = "Core Competencies",
                    skills = listOf("System Architecture", "Agile Leadership", "Code Review", "Cross-Functional Collaboration")
                )
            ),
            projects = listOf(
                ProjectItem(
                    id = "proj_1",
                    title = "AI Resume & Portfolio Engine",
                    techStack = "React, Node.js, Gemini AI, MongoDB",
                    description = "Full-stack web application generating ATS-optimized resumes and portfolio sites with real-time preview and export.",
                    liveUrl = "resumepulse.app",
                    githubUrl = "github.com/alexchen/ai-resume-engine",
                    highlights = listOf(
                        "Integrated Google Gemini models for automated ATS keyword scoring and STAR-bullet rewriting.",
                        "Processed over 25,000 PDF generations with customizable themes and layouts."
                    )
                ),
                ProjectItem(
                    id = "proj_2",
                    title = "Distributed Task Queue (FlowQueue)",
                    techStack = "Kotlin, Redis, gRPC, Docker",
                    description = "High-throughput asynchronous job processing queue handling 100k+ events/sec with fault tolerance.",
                    liveUrl = "",
                    githubUrl = "github.com/alexchen/flowqueue",
                    highlights = listOf(
                        "Achieved sub-5ms processing latency with concurrent worker pools and exponential backoff retry policies."
                    )
                )
            ),
            certifications = listOf(
                CertificationItem(
                    id = "cert_1",
                    name = "AWS Certified Solutions Architect – Associate",
                    issuer = "Amazon Web Services",
                    issueDate = "2023",
                    credentialUrl = "aws.amazon.com/verify"
                )
            ),
            languages = listOf(
                LanguageItem(id = "lang_1", language = "English", proficiency = "Native"),
                LanguageItem(id = "lang_2", language = "Mandarin", proficiency = "Professional")
            )
        )

        val pmResume = ResumeFull(
            id = "sample_product_manager",
            title = "Lead Product Manager",
            template = ResumeTemplate.EXECUTIVE,
            accentColorHex = "#0F766E", // Emerald Teal
            fontStyle = "Serif",
            isPublic = true,
            atsScore = 92,
            personalInfo = PersonalInfo(
                fullName = "Elena Rostova",
                jobTitle = "Principal Product Manager",
                email = "elena.rostova.pm@gmail.com",
                phone = "+1 (555) 890-1234",
                location = "New York, NY",
                websiteUrl = "elenarostova.com",
                linkedinUrl = "linkedin.com/in/elenarostova",
                githubUrl = "",
                summary = "Strategic and customer-obsessed Principal Product Manager with 8+ years scaling B2B SaaS and AI-driven platforms from 0 to \$40M ARR. Expert in user-centric discovery, data-backed prioritization, and cross-functional leadership across engineering, design, and GTM."
            ),
            experiences = listOf(
                ExperienceItem(
                    id = "pm_exp_1",
                    position = "Principal Product Manager",
                    company = "Aura Enterprise Analytics",
                    location = "New York, NY",
                    startDate = "Mar 2021",
                    endDate = "Present",
                    isCurrent = true,
                    bullets = listOf(
                        "Led cross-functional product pod of 18 engineers and designers to launch Aura Insights AI, generating \$14M in new ARR within 12 months.",
                        "Conducted 80+ customer discovery interviews with Fortune 500 executives to refine enterprise roadmap and core UX workflows.",
                        "Improved user onboarding conversion rate by 28% through continuous A/B testing and friction reduction."
                    )
                ),
                ExperienceItem(
                    id = "pm_exp_2",
                    position = "Senior Product Manager",
                    company = "FinVerve Platform",
                    location = "Boston, MA",
                    startDate = "Jun 2018",
                    endDate = "Feb 2021",
                    isCurrent = false,
                    bullets = listOf(
                        "Spearheaded mobile payments checkout revamp, increasing quarterly transaction volume by \$35M (+22% YoY).",
                        "Collaborated with compliance and data science teams to implement automated AML/fraud detection models."
                    )
                )
            ),
            educations = listOf(
                EducationItem(
                    id = "pm_edu_1",
                    degree = "M.B.A., Technology & Strategy",
                    fieldOfStudy = "Product Management",
                    institution = "Columbia Business School",
                    location = "New York, NY",
                    startDate = "2016",
                    endDate = "2018",
                    gpa = "3.9 / 4.0",
                    honors = "Beta Gamma Sigma"
                )
            ),
            skillCategories = listOf(
                SkillCategory(
                    id = "pm_skills_1",
                    categoryName = "Product Strategy & Analytics",
                    skills = listOf("Product Discovery", "Roadmapping", "A/B Testing", "Mixpanel", "SQL", "User Journey Mapping", "KPI Tracking")
                ),
                SkillCategory(
                    id = "pm_skills_2",
                    categoryName = "Leadership & Operations",
                    skills = listOf("Stakeholder Management", "Executive Communication", "OKRs", "Agile/Scrum", "Go-to-Market (GTM)")
                )
            ),
            projects = emptyList(),
            certifications = listOf(
                CertificationItem(
                    id = "pm_cert_1",
                    name = "Pragmatic Institute Certified (PMC-III)",
                    issuer = "Pragmatic Institute",
                    issueDate = "2020",
                    credentialUrl = ""
                )
            ),
            languages = listOf(
                LanguageItem(id = "pm_lang_1", language = "English", proficiency = "Native"),
                LanguageItem(id = "pm_lang_2", language = "French", proficiency = "Conversational")
            )
        )

        val atsResume = ResumeFull(
            id = "sample_ats_classic",
            title = "Data Scientist & AI Researcher",
            template = ResumeTemplate.CLASSIC_ATS,
            accentColorHex = "#1E293B", // Dark Slate
            fontStyle = "SansSerif",
            isPublic = true,
            atsScore = 95,
            personalInfo = PersonalInfo(
                fullName = "Dr. Marcus Vance",
                jobTitle = "Senior AI / Machine Learning Scientist",
                email = "marcus.vance.ai@gmail.com",
                phone = "+1 (555) 456-7890",
                location = "Seattle, WA",
                websiteUrl = "marcusvance.ai",
                linkedinUrl = "linkedin.com/in/marcusvance",
                githubUrl = "github.com/mvance-ai",
                summary = "AI Researcher & Senior Data Scientist with 5+ years specializing in Large Language Models (LLMs), reinforcement learning, and production inference optimization. Published author in NeurIPS & ICML with proven ability to deploy state-of-the-art neural networks into mission-critical production pipelines."
            ),
            experiences = listOf(
                ExperienceItem(
                    id = "ds_exp_1",
                    position = "Senior ML Scientist",
                    company = "Synthetix AI",
                    location = "Seattle, WA",
                    startDate = "Feb 2022",
                    endDate = "Present",
                    isCurrent = true,
                    bullets = listOf(
                        "Fine-tuned 70B+ parameter generative models using LoRA and FlashAttention-2, boosting domain-specific benchmark accuracy by 24%.",
                        "Reduced inference latency by 45% using TensorRT-LLM and vLLM quantization (FP8/INT4), slashing server infrastructure costs by \$180k/yr.",
                        "Published 2 peer-reviewed research papers on efficient chain-of-thought distillation for edge compute devices."
                    )
                )
            ),
            educations = listOf(
                EducationItem(
                    id = "ds_edu_1",
                    degree = "Ph.D. in Computer Science",
                    fieldOfStudy = "Machine Learning & NLP",
                    institution = "University of Washington",
                    location = "Seattle, WA",
                    startDate = "2017",
                    endDate = "2021",
                    gpa = "3.95 / 4.0",
                    honors = "Outstanding Graduate Research Award"
                )
            ),
            skillCategories = listOf(
                SkillCategory(
                    id = "ds_skills_1",
                    categoryName = "AI / ML Core",
                    skills = listOf("PyTorch", "Hugging Face", "vLLM", "CUDA", "TensorFlow", "Pandas", "NumPy", "Scikit-Learn")
                ),
                SkillCategory(
                    id = "ds_skills_2",
                    categoryName = "Languages & Cloud",
                    skills = listOf("Python", "C++", "SQL", "AWS SageMaker", "Kubernetes", "Docker", "MLflow")
                )
            ),
            projects = listOf(
                ProjectItem(
                    id = "ds_proj_1",
                    title = "Open-Source FastLLM Distiller",
                    techStack = "PyTorch, CUDA, Python",
                    description = "Popular open-source library for automated parameter pruning and knowledge distillation with 4.5k+ GitHub stars.",
                    liveUrl = "fastllm.org",
                    githubUrl = "github.com/mvance-ai/fastllm",
                    highlights = listOf(
                        "Adopted by 50+ research labs and enterprise AI teams for accelerating edge deployment."
                    )
                )
            ),
            certifications = emptyList(),
            languages = listOf(
                LanguageItem(id = "ds_lang_1", language = "English", proficiency = "Native")
            )
        )

        saveResume(devResume)
        saveResume(pmResume)
        saveResume(atsResume)
    }
}
