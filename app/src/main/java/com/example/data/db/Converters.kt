package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.ResumeTemplate
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @TypeConverter
    fun fromResumeTemplate(template: ResumeTemplate): String {
        return template.name
    }

    @TypeConverter
    fun toResumeTemplate(name: String): ResumeTemplate {
        return try {
            ResumeTemplate.valueOf(name)
        } catch (e: Exception) {
            ResumeTemplate.MODERN
        }
    }
}
