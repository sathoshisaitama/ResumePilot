package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.PersonalInfo
import com.example.data.model.ResumeFull
import com.example.data.model.ResumeTemplate
import com.example.data.model.SkillCategory
import com.example.data.repository.ResumeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ResumeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ResumeRepository

    val allResumes: StateFlow<List<ResumeFull>>
    private val _currentResume = MutableStateFlow<ResumeFull?>(null)
    val currentResume: StateFlow<ResumeFull?> = _currentResume.asStateFlow()

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
        }
    }

    fun deleteResume(resume: ResumeFull) {
        viewModelScope.launch {
            repository.deleteResume(resume.id)
            if (_currentResume.value?.id == resume.id) {
                _currentResume.value = null
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
}
