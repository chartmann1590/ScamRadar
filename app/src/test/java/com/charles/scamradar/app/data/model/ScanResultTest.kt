package com.charles.scamradar.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanResultTest {

    @Test
    fun `verdict enum has expected values`() {
        val values = Verdict.values()
        assertEquals(3, values.size)
        assertEquals(Verdict.SAFE, values[0])
        assertEquals(Verdict.SUSPICIOUS, values[1])
        assertEquals(Verdict.LIKELY_SCAM, values[2])
    }

    @Test
    fun `scanResult holds all fields`() {
        val flag = RedFlag("urgent", "Creates false urgency")
        val result = ScanResult(
            verdict = Verdict.LIKELY_SCAM,
            confidence = 0.95f,
            scamType = ScamType.PHISHING,
            redFlags = listOf(flag),
            aiGeneratedIndicators = listOf("generic greeting"),
            recommendedAction = "Delete the message",
            originalMessage = "URGENT: your account is compromised",
            classifierTier = ClassifierTier.STUB
        )
        assertEquals(Verdict.LIKELY_SCAM, result.verdict)
        assertEquals(0.95f, result.confidence, 0.001f)
        assertEquals(ScamType.PHISHING, result.scamType)
        assertEquals(1, result.redFlags.size)
        assertEquals("urgent", result.redFlags[0].phrase)
        assertEquals("Delete the message", result.recommendedAction)
    }

    @Test
    fun `scanResult copy works`() {
        val original = ScanResult(
            verdict = Verdict.SAFE,
            confidence = 0.1f,
            scamType = ScamType.NONE,
            redFlags = emptyList(),
            aiGeneratedIndicators = emptyList(),
            recommendedAction = "No action needed",
            originalMessage = "Hello"
        )
        val modified = original.copy(verdict = Verdict.SUSPICIOUS, confidence = 0.5f)
        assertEquals(Verdict.SUSPICIOUS, modified.verdict)
        assertEquals(0.5f, modified.confidence, 0.001f)
        assertEquals(original.originalMessage, modified.originalMessage)
    }

    @Test
    fun `scamType enum covers expected categories`() {
        val expected = setOf(
            "PHISHING", "ROMANCE", "IRS_IMPERSONATION", "CRYPTO",
            "FAMILY_EMERGENCY", "PACKAGE_DELIVERY", "JOB_OFFER",
            "TECH_SUPPORT", "LOTTERY", "INVESTMENT", "OTHER", "NONE"
        )
        val actual = ScamType.values().map { it.name }.toSet()
        assertEquals(expected, actual)
    }
}
