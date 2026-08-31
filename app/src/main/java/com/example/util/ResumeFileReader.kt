package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.model.ResumeFull
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern

data class FileImportResult(
    val fileName: String,
    val fileSizeKb: Long,
    val fileExtension: String,
    val rawText: String,
    val parsedResume: ResumeFull? = null,
    val errorMessage: String? = null
)

object ResumeFileReader {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun readFileFromUri(context: Context, uri: Uri): FileImportResult {
        var fileName = "imported_resume"
        var fileSizeKb = 0L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    if (sizeIndex != -1) {
                        val bytes = cursor.getLong(sizeIndex)
                        fileSizeKb = if (bytes > 0) bytes / 1024 else 0
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("ResumeFileReader", "Failed to query file metadata: ${e.message}")
        }

        val extension = fileName.substringAfterLast(".", "").lowercase()

        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return FileImportResult(
                    fileName = fileName,
                    fileSizeKb = fileSizeKb,
                    fileExtension = extension,
                    rawText = "",
                    errorMessage = "Cannot open input stream for selected file."
                )

            val bytes = inputStream.readBytes()
            inputStream.close()

            if (fileSizeKb == 0L) {
                fileSizeKb = (bytes.size / 1024).toLong()
            }

            // Case 1: JSON Direct Resume Backup
            if (extension == "json") {
                val jsonString = String(bytes, Charsets.UTF_8)
                try {
                    val adapter = moshi.adapter(ResumeFull::class.java)
                    val directResume = adapter.fromJson(jsonString)
                    if (directResume != null && (directResume.personalInfo.fullName.isNotBlank() || directResume.title.isNotBlank())) {
                        return FileImportResult(
                            fileName = fileName,
                            fileSizeKb = fileSizeKb,
                            fileExtension = extension,
                            rawText = jsonString,
                            parsedResume = directResume
                        )
                    }
                } catch (e: Exception) {
                    Log.d("ResumeFileReader", "Not a direct ResumeFull JSON, treating as raw text: ${e.message}")
                }
                return FileImportResult(
                    fileName = fileName,
                    fileSizeKb = fileSizeKb,
                    fileExtension = extension,
                    rawText = jsonString
                )
            }

            // Case 2: PDF Document
            if (extension == "pdf") {
                val extractedText = extractTextFromPdfBytes(bytes)
                return FileImportResult(
                    fileName = fileName,
                    fileSizeKb = fileSizeKb,
                    fileExtension = extension,
                    rawText = extractedText
                )
            }

            // Case 3: Plain text, Markdown, RTF, CSV, DOCX
            val rawString = extractReadableText(bytes)
            FileImportResult(
                fileName = fileName,
                fileSizeKb = fileSizeKb,
                fileExtension = extension,
                rawText = rawString
            )
        } catch (e: Exception) {
            Log.e("ResumeFileReader", "Error reading file: ${e.message}", e)
            FileImportResult(
                fileName = fileName,
                fileSizeKb = fileSizeKb,
                fileExtension = extension,
                rawText = "",
                errorMessage = "Failed to process file: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Extracts text from PDF bytes by parsing standard PDF text objects (BT ... ET blocks, Tj and TJ operators)
     * and readable text segments without external heavy binary dependencies.
     */
    private fun extractTextFromPdfBytes(bytes: ByteArray): String {
        val fullString = String(bytes, Charsets.ISO_8859_1)
        val extractedLines = mutableListOf<String>()

        // Look for PDF Text Blocks: (Text) Tj or [(T)(e)(x)(t)] TJ
        val tjPattern = Pattern.compile("\\(([^\\)]*)\\)\\s*Tj", Pattern.MULTILINE)
        val matcher = tjPattern.matcher(fullString)
        while (matcher.find()) {
            val text = matcher.group(1)?.trim()
            if (!text.isNullOrBlank()) {
                extractedLines.add(cleanPdfString(text))
            }
        }

        // Look for TJ Array patterns: [(...) ...] TJ
        val tjArrayPattern = Pattern.compile("\\[([^\\]]*)\\]\\s*TJ", Pattern.MULTILINE)
        val arrayMatcher = tjArrayPattern.matcher(fullString)
        while (arrayMatcher.find()) {
            val arrayContent = arrayMatcher.group(1) ?: ""
            val innerMatcher = Pattern.compile("\\(([^\\)]*)\\)").matcher(arrayContent)
            val combined = StringBuilder()
            while (innerMatcher.find()) {
                innerMatcher.group(1)?.let { combined.append(it) }
            }
            if (combined.isNotBlank()) {
                extractedLines.add(cleanPdfString(combined.toString()))
            }
        }

        // Fallback: If minimal tokens were extracted (e.g. compressed streams), extract readable ASCII chunks
        if (extractedLines.size < 4) {
            val cleanAscii = extractReadableText(bytes)
            if (cleanAscii.length > 50) {
                return cleanAscii
            }
        }

        val result = extractedLines.joinToString("\n").trim()
        return if (result.isNotBlank()) result else extractReadableText(bytes)
    }

    private fun cleanPdfString(raw: String): String {
        return raw.replace("\\n", "\n")
            .replace("\\r", "")
            .replace("\\t", " ")
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")
            .trim()
    }

    /**
     * Extracts sequences of readable characters from binary or text files (DOCX, RTF, raw files)
     */
    private fun extractReadableText(bytes: ByteArray): String {
        val sb = StringBuilder()
        val currentWord = StringBuilder()

        for (b in bytes) {
            val c = b.toInt().toChar()
            if (c in ' '..'~' || c == '\n' || c == '\t') {
                currentWord.append(c)
            } else {
                if (currentWord.length >= 2) {
                    sb.append(currentWord).append(" ")
                }
                currentWord.clear()
            }
        }
        if (currentWord.length >= 2) {
            sb.append(currentWord)
        }

        // Clean redundant spaces and control characters
        return sb.toString()
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }
}
