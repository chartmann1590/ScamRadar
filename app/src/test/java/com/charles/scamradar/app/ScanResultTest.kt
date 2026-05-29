package com.charles.scamradar.app

import com.charles.scamradar.app.data.model.ClassifierTier
import com.charles.scamradar.app.data.model.RedFlag
import com.charles.scamradar.app.data.model.ScamType
import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.data.model.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanResultTest {

    @Test
    fun scanResult_holdsAllFields() {
        val flags = listOf(RedFlag("bad link", "phishing"))
        val result = ScanResult(
            verdict = Verdict.LIKELY_SCAM,
            confidence = 0.95f,
            scamType = ScamType.PHISHING,
            redFlags = flags,
            aiGeneratedIndicators = listOf("template"),
            recommendedAction = "Delete",
            originalMessage = "Click here",
            timestamp = 1000L,
            classifierTier = ClassifierTier.STUB
        )
        assertEquals(Verdict.LIKELY_SCAM, result.verdict)
        assertEquals(0.95f, result.confidence, 0.001f)
        assertEquals(ScamType.PHISHING, result.scamType)
        assertEquals(1, result.redFlags.size)
        assertEquals("bad link", result.redFlags[0].phrase)
        assertEquals("phishing", result.redFlags[0].reason)
        assertEquals(listOf("template"), result.aiGeneratedIndicators)
        assertEquals("Delete", result.recommendedAction)
        assertEquals("Click here", result.originalMessage)
        assertEquals(1000L, result.timestamp)
        assertEquals(ClassifierTier.STUB, result.classifierTier)
    }

    @Test
    fun scamType_valuesExist() {
        val expected = setOf(
            "PHISHING", "ROMANCE", "IRS_IMPERSONATION", "CRYPTO",
            "FAMILY_EMERGENCY", "PACKAGE_DELIVERY", "JOB_OFFER",
            "TECH_SUPPORT", "LOTTERY", "INVESTMENT", "OTHER", "NONE"
        )
        val actual = ScamType.values().map { it.name }.toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun verdict_valuesExist() {
        val expected = setOf("SAFE", "SUSPICIOUS", "LIKELY_SCAM")
        val actual = Verdict.values().map { it.name }.toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun redFlag_dataClassEquality() {
        val a = RedFlag("phrase", "reason")
        val b = RedFlag("phrase", "reason")
        assertEquals(a, b)
    }

    @Test
    fun scanResult_defaultValues() {
        val result = ScanResult(
            verdict = Verdict.SAFE,
            confidence = 0.5f,
            scamType = ScamType.NONE,
            redFlags = emptyList(),
            aiGeneratedIndicators = emptyList(),
            recommendedAction = "",
            originalMessage = "hello"
        )
        assertEquals(ClassifierTier.STUB, result.classifierTier)
        assertTrue(result.timestamp > 0)
    }
}
