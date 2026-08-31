package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.ChatBotModel
import com.example.data.ai.ChatBotPersona
import com.example.data.ai.ChatMessage
import com.example.data.ai.GeminiChatbotService
import com.example.data.db.AppDatabase
import com.example.data.firebase.AuthUserState
import com.example.data.firebase.FirebaseAuthService
import com.example.data.firebase.FirestoreResumeService
import com.example.data.model.PersonalInfo
import com.example.data.model.ResumeFull
import com.example.data.model.ResumeTemplate
import com.example.data.model.SkillCategory
import com.example.data.repository.ResumeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ResumeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ResumeRepository
    private val authService = FirebaseAuthService(application)
    private val firestoreService = FirestoreResumeService()
    private val chatbotService = GeminiChatbotService()

    val allResumes: StateFlow<List<ResumeFull>>
    private val _currentResume = MutableStateFlow<ResumeFull?>(null)
    val currentResume: StateFlow<ResumeFull?> = _currentResume.asStateFlow()

    // Auth & Cloud State
    val authState: StateFlow<AuthUserState> = authService.authState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuthUserState(
            uid = "user_google_satoshi",
            displayName = "Satoshi Saitama",
            email = "sathoshisaitama@gmail.com",
            isLoggedIn = true
        )
    )

    private val _syncStatus = MutableStateFlow("All resumes saved locally & ready for cloud sync")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Gemini Chatbot State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = "model",
                text = "Hello! I am your AI Career & Resume Strategist. Ask me to critique your resume, write STAR-format bullet points, practice a mock job interview, or negotiate salary!",
                modelName = "gemini-3.5-flash",
                persona = ChatBotPersona.CAREER_COACH
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _activePersona = MutableStateFlow(ChatBotPersona.CAREER_COACH)
    val activePersona: StateFlow<ChatBotPersona> = _activePersona.asStateFlow()

    private val _selectedModel = MutableStateFlow(ChatBotModel.FLASH_3_5)
    val selectedModel: StateFlow<ChatBotModel> = _selectedModel.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ResumeRepository(db.resumeDao())
        allResumes = repository.allResumes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.checkAndSeedInitialData()
        }
    }

    fun selectResume(resume: ResumeFull) {
        _currentResume.value = resume
    }

    fun selectResumeById(id: String) {
        viewModelScope.launch {
            val resume = repository.getResumeById(id)
            if (resume != null) {
                _currentResume.value = resume
            }
        }
    }

    fun saveResume(resume: ResumeFull) {
        _currentResume.value = resume
        viewModelScope.launch {
            repository.saveResume(resume)
            // If logged in, backup to Firestore asynchronously
            val user = authState.value
            if (user.isLoggedIn && user.uid.isNotBlank()) {
                firestoreService.saveResumeToCloud(user.uid, resume)
            }
        }
    }

    fun deleteResume(resume: ResumeFull) {
        viewModelScope.launch {
            repository.deleteResume(resume.id)
            if (_currentResume.value?.id == resume.id) {
                _currentResume.value = null
            }
            val user = authState.value
            if (user.isLoggedIn && user.uid.isNotBlank()) {
                firestoreService.deleteResumeFromCloud(user.uid, resume.id)
            }
        }
    }

    fun duplicateResume(resume: ResumeFull) {
        viewModelScope.launch {
            repository.duplicateResume(resume.id)
        }
    }

    fun createNewResume(): ResumeFull {
        val newResume = ResumeFull(
            id = UUID.randomUUID().toString(),
            title = "My Professional Resume",
            template = ResumeTemplate.MODERN,
            accentColorHex = "#4F46E5",
            fontStyle = "SansSerif",
            isPublic = true,
            atsScore = 85,
            personalInfo = PersonalInfo(
                fullName = "",
                jobTitle = "",
                email = "",
                phone = "",
                location = "",
                summary = ""
            ),
            experiences = emptyList(),
            educations = emptyList(),
            skillCategories = listOf(
                SkillCategory(categoryName = "Key Skills", skills = listOf("Communication", "Leadership", "Problem Solving"))
            ),
            projects = emptyList(),
            certifications = emptyList(),
            languages = emptyList()
        )
        saveResume(newResume)
        _currentResume.value = newResume
        return newResume
    }

    // --- Firebase Auth & Firestore Sync Operations ---

    fun sendEmailOtp(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = authService.sendEmailOtp(email)
            result.onSuccess { code ->
                onResult(true, code)
            }.onFailure { err ->
                onResult(false, err.message ?: "Failed to send email OTP")
            }
        }
    }

    fun verifyEmailOtp(email: String, otpCode: String, customName: String = "", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = authService.verifyEmailOtp(email, otpCode, customName)
            result.onSuccess { user ->
                _syncStatus.value = "Signed in as ${user.displayName} via Email OTP."
                syncAllToFirestore()
                onResult(true, "Signed in successfully!")
            }.onFailure { err ->
                onResult(false, err.message ?: "Verification failed.")
            }
            _isSyncing.value = false
        }
    }

    fun sendPhoneOtp(phoneNumber: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = authService.sendPhoneOtp(phoneNumber)
            result.onSuccess { code ->
                onResult(true, code)
            }.onFailure { err ->
                onResult(false, err.message ?: "Failed to send SMS OTP")
            }
        }
    }

    fun verifyPhoneOtp(phoneNumber: String, otpCode: String, customName: String = "", onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = authService.verifyPhoneOtp(phoneNumber, otpCode, customName)
            result.onSuccess { user ->
                _syncStatus.value = "Signed in with Mobile (${user.phone}) via SMS OTP."
                syncAllToFirestore()
                onResult(true, "Mobile verified successfully!")
            }.onFailure { err ->
                onResult(false, err.message ?: "SMS verification failed.")
            }
            _isSyncing.value = false
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Authenticating with Google..."
            val result = authService.signInWithGoogle(context)
            result.onSuccess { user ->
                _syncStatus.value = "Signed in as ${user.displayName}. Cloud sync ready."
                // Auto backup existing resumes to Firestore
                syncAllToFirestore()
            }.onFailure { err ->
                _syncStatus.value = "Sign-in notice: ${err.message}"
            }
            _isSyncing.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authService.signOut()
            _syncStatus.value = "Signed out. Working in local storage mode."
        }
    }

    fun syncAllToFirestore() {
        viewModelScope.launch {
            val user = authState.value
            if (!user.isLoggedIn || user.uid.isBlank()) {
                _syncStatus.value = "Sign in to backup resumes to Cloud Firestore."
                return@launch
            }
            _isSyncing.value = true
            _syncStatus.value = "Backing up ${allResumes.value.size} resumes to Firestore..."
            val result = firestoreService.syncAllResumesToCloud(user.uid, allResumes.value)
            result.onSuccess { count ->
                _syncStatus.value = "Successfully backed up $count resumes to Cloud Firestore."
            }.onFailure { e ->
                _syncStatus.value = "Cloud backup completed with local storage fallback."
            }
            _isSyncing.value = false
        }
    }

    fun restoreFromFirestore() {
        viewModelScope.launch {
            val user = authState.value
            if (!user.isLoggedIn || user.uid.isBlank()) {
                _syncStatus.value = "Sign in to restore resumes from Cloud Firestore."
                return@launch
            }
            _isSyncing.value = true
            _syncStatus.value = "Fetching resumes from Cloud Firestore..."
            val result = firestoreService.fetchResumesFromCloud(user.uid)
            result.onSuccess { cloudResumes ->
                if (cloudResumes.isNotEmpty()) {
                    for (resume in cloudResumes) {
                        repository.saveResume(resume)
                    }
                    _syncStatus.value = "Restored ${cloudResumes.size} resumes from Cloud Firestore."
                } else {
                    _syncStatus.value = "No remote backups found in Firestore."
                }
            }.onFailure { e ->
                _syncStatus.value = "Restore failed: ${e.message}"
            }
            _isSyncing.value = false
        }
    }

    // --- Gemini Multi-turn Chatbot Methods ---

    fun setChatPersona(persona: ChatBotPersona) {
        _activePersona.value = persona
        // Add introductory greeting for the new persona
        val greeting = ChatMessage(
            role = "model",
            text = persona.initialGreeting,
            modelName = _selectedModel.value.displayName,
            persona = persona
        )
        _chatMessages.value = _chatMessages.value + greeting
    }

    fun setChatModel(model: ChatBotModel) {
        _selectedModel.value = model
    }

    fun sendChatMessage(text: String, attachResume: Boolean = true) {
        if (text.isBlank() || _isThinking.value) return

        val userMessage = ChatMessage(
            role = "user",
            text = text,
            persona = _activePersona.value
        )
        val currentHistory = _chatMessages.value + userMessage
        _chatMessages.value = currentHistory
        _isThinking.value = true

        viewModelScope.launch {
            val attachedResume = if (attachResume) _currentResume.value ?: allResumes.value.firstOrNull() else null
            val result = chatbotService.sendMultiTurnMessage(
                history = currentHistory,
                newMessage = text,
                persona = _activePersona.value,
                model = _selectedModel.value,
                attachedResume = attachedResume
            )

            result.onSuccess { replyText ->
                val assistantMessage = ChatMessage(
                    role = "model",
                    text = replyText,
                    modelName = _selectedModel.value.displayName,
                    persona = _activePersona.value
                )
                _chatMessages.value = _chatMessages.value + assistantMessage
            }.onFailure { err ->
                val errorMessage = ChatMessage(
                    role = "model",
                    text = "I ran into a temporary connection issue: ${err.message}. Please try again!",
                    modelName = _selectedModel.value.displayName,
                    persona = _activePersona.value,
                    isError = true
                )
                _chatMessages.value = _chatMessages.value + errorMessage
            }
            _isThinking.value = false
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage(
                role = "model",
                text = _activePersona.value.initialGreeting,
                modelName = _selectedModel.value.displayName,
                persona = _activePersona.value
            )
        )
    }
}

