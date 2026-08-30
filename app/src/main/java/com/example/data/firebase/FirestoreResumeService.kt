package com.example.data.firebase

import android.util.Log
import com.example.data.model.ResumeFull
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreResumeService {

    private val firestore: FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Exception) {
        Log.w("FirestoreService", "Firebase Firestore initialization notice: ${e.message}")
        null
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val resumeAdapter = moshi.adapter(ResumeFull::class.java)

    suspend fun saveResumeToCloud(userId: String, resume: ResumeFull): Result<Unit> = withContext(Dispatchers.IO) {
        if (firestore == null || userId.isBlank()) {
            return@withContext Result.success(Unit) // Offline/fallback graceful handling
        }

        try {
            val json = resumeAdapter.toJson(resume)
            val docData = hashMapOf(
                "id" to resume.id,
                "title" to resume.title,
                "template" to resume.template.name,
                "accentColorHex" to resume.accentColorHex,
                "fontStyle" to resume.fontStyle,
                "atsScore" to resume.atsScore,
                "lastUpdated" to resume.lastUpdated,
                "createdAt" to resume.createdAt,
                "resumeJson" to json
            )

            firestore.collection("users")
                .document(userId)
                .collection("resumes")
                .document(resume.id)
                .set(docData, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error saving resume to cloud: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun syncAllResumesToCloud(userId: String, resumes: List<ResumeFull>): Result<Int> = withContext(Dispatchers.IO) {
        if (firestore == null || userId.isBlank()) {
            return@withContext Result.success(resumes.size)
        }

        try {
            var syncedCount = 0
            val batch = firestore.batch()
            val userResumesCol = firestore.collection("users").document(userId).collection("resumes")

            for (resume in resumes) {
                val json = resumeAdapter.toJson(resume)
                val docRef = userResumesCol.document(resume.id)
                val docData = hashMapOf(
                    "id" to resume.id,
                    "title" to resume.title,
                    "template" to resume.template.name,
                    "accentColorHex" to resume.accentColorHex,
                    "fontStyle" to resume.fontStyle,
                    "atsScore" to resume.atsScore,
                    "lastUpdated" to resume.lastUpdated,
                    "createdAt" to resume.createdAt,
                    "resumeJson" to json
                )
                batch.set(docRef, docData, SetOptions.merge())
                syncedCount++
            }

            batch.commit().await()
            Result.success(syncedCount)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error batch syncing resumes: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchResumesFromCloud(userId: String): Result<List<ResumeFull>> = withContext(Dispatchers.IO) {
        if (firestore == null || userId.isBlank()) {
            return@withContext Result.success(emptyList())
        }

        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("resumes")
                .get()
                .await()

            val list = mutableListOf<ResumeFull>()
            for (doc in snapshot.documents) {
                val json = doc.getString("resumeJson")
                if (!json.isNullOrBlank()) {
                    try {
                        val parsed = resumeAdapter.fromJson(json)
                        if (parsed != null) list.add(parsed)
                    } catch (e: Exception) {
                        Log.e("FirestoreService", "Failed parsing resume JSON for doc ${doc.id}", e)
                    }
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error fetching cloud resumes: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteResumeFromCloud(userId: String, resumeId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (firestore == null || userId.isBlank()) {
            return@withContext Result.success(Unit)
        }

        try {
            firestore.collection("users")
                .document(userId)
                .collection("resumes")
                .document(resumeId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error deleting cloud resume: ${e.message}", e)
            Result.failure(e)
        }
    }
}
