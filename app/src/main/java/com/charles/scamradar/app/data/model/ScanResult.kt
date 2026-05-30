package com.charles.scamradar.app.data.model

enum class Verdict { SAFE, SUSPICIOUS, LIKELY_SCAM }
enum class ScamType {
    PHISHING, ROMANCE, IRS_IMPERSONATION, CRYPTO,
    FAMILY_EMERGENCY, PACKAGE_DELIVERY, JOB_OFFER,
    TECH_SUPPORT, LOTTERY, INVESTMENT, OTHER, NONE
}
data class RedFlag(val phrase: String, val reason: String)

/** Optional URL-scan metadata. Null for text/OCR/voice scans. */
data class UrlScanMetadata(
    val originalUrl: String,
    val finalUrl: String,
    val screenshotPath: String,
    val redirectCount: Int,
    val findings: List<String>
)

data class ScanResult(
    val verdict: Verdict,
    val confidence: Float,
    val scamType: ScamType,
    val redFlags: List<RedFlag>,
    val aiGeneratedIndicators: List<String>,
    val recommendedAction: String,
    val originalMessage: String,
    val timestamp: Long = System.currentTimeMillis(),
    val classifierTier: ClassifierTier = ClassifierTier.STUB,
    val urlMetadata: UrlScanMetadata? = null
)
enum class ClassifierTier { STUB, LITE, GEMMA }
enum class ScanMode { TEXT, OCR, VOICE, URL, SHIELD, CLIPBOARD }
