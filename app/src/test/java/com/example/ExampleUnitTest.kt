package com.example

import com.example.data.model.ResumeFull
import com.example.util.QrCodeGenerator
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testQrCodeGenerator_createsValidBitmap() {
        val qrBitmap = QrCodeGenerator.generateQrBitmap("https://linkedin.com/in/testuser", size = 200)
        assertNotNull(qrBitmap)
        assertEquals(200, qrBitmap?.width)
        assertEquals(200, qrBitmap?.height)
    }

    @Test
    fun testResumeFull_qrCodeDefaults() {
        val resume = ResumeFull()
        assertTrue(resume.personalInfo.includeQrCode)
        assertEquals("LinkedIn", resume.personalInfo.qrCodeTarget)
    }
}
