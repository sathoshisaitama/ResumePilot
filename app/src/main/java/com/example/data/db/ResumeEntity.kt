package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ResumeFull
import com.example.data.model.ResumeTemplate

@Entity(tableName = "resumes")
data class ResumeEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val template: ResumeTemplate,
    val accentColorHex: String,
    val fontStyle: String,
    val isPublic: Boolean,
    val shareSlug: String,
    val resumeDataJson: String, // Full serialized JSON for fidelity and forward compatibility
    val atsScore: Int,
    val lastUpdated: Long,
    val createdAt: Long
)
