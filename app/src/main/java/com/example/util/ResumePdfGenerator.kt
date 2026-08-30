package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.ResumeFull
import com.example.data.model.ResumeTemplate
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.IOException

object ResumePdfGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 pt width
    private const val PAGE_HEIGHT = 842 // Standard A4 pt height
    private const val MARGIN_X = 36f
    private const val MARGIN_Y = 36f

    fun generatePdfFile(context: Context, resume: ResumeFull): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

        // Accent Color
        val accentColor = try {
            Color.parseColor(resume.accentColorHex)
        } catch (e: Exception) {
            Color.parseColor("#4F46E5")
        }

        var currentY = MARGIN_Y

        // Header Top Bar or Banner
        if (resume.template == ResumeTemplate.EXECUTIVE || resume.template == ResumeTemplate.TECH_SLATE) {
            val headerPaint = Paint().apply {
                color = accentColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 90f, headerPaint)
            
            // Name in white
            val namePaint = Paint().apply {
                color = Color.WHITE
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(resume.personalInfo.fullName.ifBlank { "Your Name" }, MARGIN_X, 42f, namePaint)

            // Title in subtle white
            val titlePaint = Paint().apply {
                color = Color.parseColor("#E0E7FF")
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }
            canvas.drawText(resume.personalInfo.jobTitle.ifBlank { "Professional Title" }, MARGIN_X, 62f, titlePaint)

            // Contacts line
            val contactLine = listOfNotNull(
                resume.personalInfo.email.takeIf { it.isNotBlank() },
                resume.personalInfo.phone.takeIf { it.isNotBlank() },
                resume.personalInfo.location.takeIf { it.isNotBlank() },
                resume.personalInfo.linkedinUrl.takeIf { it.isNotBlank() }
            ).joinToString("  •  ")

            val contactPaint = Paint().apply {
                color = Color.parseColor("#C7D2FE")
                textSize = 9f
                isAntiAlias = true
            }
            canvas.drawText(contactLine, MARGIN_X, 78f, contactPaint)

            currentY = 106f
        } else {
            // Modern / Minimal / ATS Classic Header
            val namePaint = Paint().apply {
                color = if (resume.template == ResumeTemplate.CLASSIC_ATS) Color.BLACK else accentColor
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(resume.personalInfo.fullName.ifBlank { "Your Name" }, MARGIN_X, currentY + 18f, namePaint)
            currentY += 24f

            val titlePaint = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText(resume.personalInfo.jobTitle.ifBlank { "Professional Title" }, MARGIN_X, currentY + 12f, titlePaint)
            currentY += 18f

            val contactLine = listOfNotNull(
                resume.personalInfo.email.takeIf { it.isNotBlank() },
                resume.personalInfo.phone.takeIf { it.isNotBlank() },
                resume.personalInfo.location.takeIf { it.isNotBlank() },
                resume.personalInfo.linkedinUrl.takeIf { it.isNotBlank() },
                resume.personalInfo.githubUrl.takeIf { it.isNotBlank() }
            ).joinToString("   |   ")

            val contactPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 9.5f
                isAntiAlias = true
            }
            canvas.drawText(contactLine, MARGIN_X, currentY + 10f, contactPaint)
            currentY += 16f

            // Divider line
            val dividerPaint = Paint().apply {
                color = if (resume.template == ResumeTemplate.MODERN) accentColor else Color.parseColor("#CBD5E1")
                strokeWidth = 1.5f
            }
            canvas.drawLine(MARGIN_X, currentY + 4f, PAGE_WIDTH - MARGIN_X, currentY + 4f, dividerPaint)
            currentY += 14f
        }

        // Summary Section
        if (resume.personalInfo.summary.isNotBlank()) {
            currentY = drawSectionHeader(canvas, "PROFESSIONAL SUMMARY", currentY, accentColor, resume.template)
            currentY = drawWrappedText(canvas, resume.personalInfo.summary, MARGIN_X, currentY, PAGE_WIDTH - (MARGIN_X * 2), 9.5f, Color.parseColor("#334155"))
            currentY += 10f
        }

        // Experience Section
        if (resume.experiences.isNotEmpty()) {
            currentY = drawSectionHeader(canvas, "EXPERIENCE", currentY, accentColor, resume.template)
            for (exp in resume.experiences) {
                if (currentY > PAGE_HEIGHT - 60) break // avoid page overflow
                
                // Position & Company
                val rolePaint = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 10.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                canvas.drawText("${exp.position} — ${exp.company}", MARGIN_X, currentY + 10f, rolePaint)

                // Dates
                val dateText = "${exp.startDate} - ${if (exp.isCurrent) "Present" else exp.endDate} ${if (exp.location.isNotBlank()) " | ${exp.location}" else ""}"
                val datePaint = Paint().apply {
                    color = Color.parseColor("#64748B")
                    textSize = 9f
                    textAlign = Paint.Align.RIGHT
                    isAntiAlias = true
                }
                canvas.drawText(dateText, PAGE_WIDTH - MARGIN_X, currentY + 10f, datePaint)
                currentY += 16f

                // Bullets
                val allBullets = if (exp.bullets.isNotEmpty()) exp.bullets else if (exp.description.isNotBlank()) listOf(exp.description) else emptyList()
                for (bullet in allBullets) {
                    if (currentY > PAGE_HEIGHT - 50) break
                    val bulletText = "• $bullet"
                    currentY = drawWrappedText(canvas, bulletText, MARGIN_X + 6f, currentY, PAGE_WIDTH - (MARGIN_X * 2) - 6f, 9f, Color.parseColor("#334155"))
                    currentY += 2f
                }
                currentY += 6f
            }
            currentY += 6f
        }

        // Education Section
        if (resume.educations.isNotEmpty()) {
            currentY = drawSectionHeader(canvas, "EDUCATION", currentY, accentColor, resume.template)
            for (edu in resume.educations) {
                if (currentY > PAGE_HEIGHT - 60) break
                val eduTitlePaint = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 10.5f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val degreeText = "${edu.degree}${if (edu.fieldOfStudy.isNotBlank()) ", ${edu.fieldOfStudy}" else ""} — ${edu.institution}"
                canvas.drawText(degreeText, MARGIN_X, currentY + 10f, eduTitlePaint)

                val dateText = "${edu.startDate} - ${edu.endDate}"
                val datePaint = Paint().apply {
                    color = Color.parseColor("#64748B")
                    textSize = 9f
                    textAlign = Paint.Align.RIGHT
                    isAntiAlias = true
                }
                canvas.drawText(dateText, PAGE_WIDTH - MARGIN_X, currentY + 10f, datePaint)
                currentY += 14f

                if (edu.gpa.isNotBlank() || edu.honors.isNotBlank()) {
                    val subText = listOfNotNull(
                        edu.gpa.takeIf { it.isNotBlank() }?.let { "GPA: $it" },
                        edu.honors.takeIf { it.isNotBlank() }
                    ).joinToString("  •  ")
                    currentY = drawWrappedText(canvas, subText, MARGIN_X + 6f, currentY, PAGE_WIDTH - (MARGIN_X * 2), 8.5f, Color.parseColor("#64748B"))
                }
                for (hl in edu.highlights) {
                    if (currentY > PAGE_HEIGHT - 50) break
                    val bulletText = "• $hl"
                    currentY = drawWrappedText(canvas, bulletText, MARGIN_X + 6f, currentY, PAGE_WIDTH - (MARGIN_X * 2) - 6f, 8.5f, Color.parseColor("#475569"))
                    currentY += 2f
                }
                currentY += 4f
            }
            currentY += 6f
        }

        // Skills Section
        if (resume.skillCategories.isNotEmpty()) {
            currentY = drawSectionHeader(canvas, "SKILLS & COMPETENCIES", currentY, accentColor, resume.template)
            for (cat in resume.skillCategories) {
                if (currentY > PAGE_HEIGHT - 50) break
                val skillLine = "${cat.categoryName}: ${cat.skills.joinToString(", ")}"
                currentY = drawWrappedText(canvas, skillLine, MARGIN_X, currentY, PAGE_WIDTH - (MARGIN_X * 2), 9f, Color.parseColor("#1E293B"))
                currentY += 3f
            }
            currentY += 6f
        }

        // Projects Section
        if (resume.projects.isNotEmpty() && currentY < PAGE_HEIGHT - 90) {
            currentY = drawSectionHeader(canvas, "KEY PROJECTS", currentY, accentColor, resume.template)
            for (proj in resume.projects) {
                if (currentY > PAGE_HEIGHT - 60) break
                val projPaint = Paint().apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val projLine = "${proj.title} [${proj.techStack}]"
                canvas.drawText(projLine, MARGIN_X, currentY + 10f, projPaint)
                currentY += 14f

                if (proj.description.isNotBlank()) {
                    currentY = drawWrappedText(canvas, proj.description, MARGIN_X + 6f, currentY, PAGE_WIDTH - (MARGIN_X * 2) - 6f, 8.5f, Color.parseColor("#475569"))
                }
                currentY += 4f
            }
        }

        // Bottom Footer
        val footerPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 7.5f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Generated with AI Resume Builder  •  ATS-Friendly Verified  •  resumebuilder.ai/${resume.shareSlug}", PAGE_WIDTH / 2f, PAGE_HEIGHT - 16f, footerPaint)

        pdfDocument.finishPage(page)

        // Write to cache file
        val outputDir = File(context.cacheDir, "resumes").apply { mkdirs() }
        val sanitizedTitle = resume.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(outputDir, "${sanitizedTitle}_resume.pdf")

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return file
    }

    private fun drawSectionHeader(
        canvas: android.graphics.Canvas,
        title: String,
        y: Float,
        accentColor: Int,
        template: ResumeTemplate
    ): Float {
        var nextY = y
        val paint = Paint().apply {
            color = if (template == ResumeTemplate.CLASSIC_ATS) Color.BLACK else accentColor
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
            isAntiAlias = true
        }
        canvas.drawText(title, MARGIN_X, nextY + 10f, paint)
        nextY += 14f

        // Section underline
        val linePaint = Paint().apply {
            color = if (template == ResumeTemplate.CLASSIC_ATS) Color.BLACK else Color.parseColor("#E2E8F0")
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN_X, nextY, PAGE_WIDTH - MARGIN_X, nextY, linePaint)
        nextY += 8f
        return nextY
    }

    private fun drawWrappedText(
        canvas: android.graphics.Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        textSize: Float,
        textColor: Int
    ): Float {
        val textPaint = TextPaint().apply {
            color = textColor
            this.textSize = textSize
            isAntiAlias = true
        }

        val staticLayout = StaticLayout.Builder.obtain(
            text,
            0,
            text.length,
            textPaint,
            width.toInt()
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1f)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(x, y)
        staticLayout.draw(canvas)
        canvas.restore()

        return y + staticLayout.height
    }

    fun printResume(activity: Activity, resume: ResumeFull) {
        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(activity, "Print service not available", Toast.LENGTH_SHORT).show()
            return
        }

        val printAdapter = object : PrintDocumentAdapter() {
            private var pdfFile: File? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                pdfFile = generatePdfFile(activity, resume)

                val info = PrintDocumentInfo.Builder("${resume.title}.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()

                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    val input = FileInputStream(pdfFile)
                    val output = FileOutputStream(destination?.fileDescriptor)

                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } >= 0) {
                        output.write(buffer, 0, bytesRead)
                    }

                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    input.close()
                    output.close()
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        val jobName = "${activity.packageName}: ${resume.title}"
        printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
    }

    fun sharePdf(context: Context, resume: ResumeFull) {
        try {
            val file = generatePdfFile(context, resume)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "${resume.personalInfo.fullName} - Resume")
                putExtra(Intent.EXTRA_TEXT, "Here is the resume for ${resume.personalInfo.fullName} (${resume.personalInfo.jobTitle}).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Resume PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareAsPlainText(context: Context, resume: ResumeFull) {
        val text = buildString {
            appendLine(resume.personalInfo.fullName.uppercase())
            appendLine(resume.personalInfo.jobTitle)
            appendLine("${resume.personalInfo.email} | ${resume.personalInfo.phone} | ${resume.personalInfo.location}")
            if (resume.personalInfo.linkedinUrl.isNotBlank()) appendLine("LinkedIn: ${resume.personalInfo.linkedinUrl}")
            if (resume.personalInfo.githubUrl.isNotBlank()) appendLine("GitHub: ${resume.personalInfo.githubUrl}")
            appendLine()
            if (resume.personalInfo.summary.isNotBlank()) {
                appendLine("SUMMARY")
                appendLine("----------------------------------------")
                appendLine(resume.personalInfo.summary)
                appendLine()
            }
            if (resume.experiences.isNotEmpty()) {
                appendLine("EXPERIENCE")
                appendLine("----------------------------------------")
                resume.experiences.forEach { exp ->
                    appendLine("${exp.position} — ${exp.company} (${exp.startDate} - ${if (exp.isCurrent) "Present" else exp.endDate})")
                    exp.bullets.forEach { appendLine("• $it") }
                    appendLine()
                }
            }
            if (resume.educations.isNotEmpty()) {
                appendLine("EDUCATION")
                appendLine("----------------------------------------")
                resume.educations.forEach { edu ->
                    appendLine("${edu.degree}, ${edu.fieldOfStudy} — ${edu.institution} (${edu.startDate} - ${edu.endDate})")
                }
                appendLine()
            }
            if (resume.skillCategories.isNotEmpty()) {
                appendLine("SKILLS")
                appendLine("----------------------------------------")
                resume.skillCategories.forEach { cat ->
                    appendLine("${cat.categoryName}: ${cat.skills.joinToString(", ")}")
                }
                appendLine()
            }
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "${resume.personalInfo.fullName} - Plain Text Resume")
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Resume Text"))
    }
}
