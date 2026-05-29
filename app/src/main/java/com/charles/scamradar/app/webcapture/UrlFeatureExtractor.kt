package com.charles.scamradar.app.webcapture

import java.net.IDN
import java.net.URI

data class UrlFeatures(
    val originalUrl: String,
    val finalUrl: String,
    val scheme: String,
    val host: String,
    val isIdn: Boolean,
    val isIpHost: Boolean,
    val hasPunycode: Boolean,
    val homoglyphScore: Int,
    val suspiciousTld: Boolean,
    val redirectCount: Int,
    val isHttps: Boolean,
    val findings: List<String>
)

object UrlFeatureExtractor {

    private val suspiciousTlds = setOf(
        "zip", "mov", "xyz", "top", "click", "country", "stream",
        "gq", "tk", "ml", "cf", "rest", "rip", "monster", "icu"
    )

    fun extract(originalUrl: String, finalUrl: String, redirectCount: Int): UrlFeatures {
        val findings = mutableListOf<String>()
        val original = runCatching { URI(normalize(originalUrl)) }.getOrNull()
        val final = runCatching { URI(normalize(finalUrl)) }.getOrNull() ?: original

        val scheme = final?.scheme?.lowercase().orEmpty()
        val host = final?.host?.lowercase().orEmpty()

        val isHttps = scheme == "https"
        if (!isHttps && scheme.isNotEmpty()) {
            findings += "Connection is not HTTPS — data sent here is unencrypted."
        }

        val hasPunycode = host.contains("xn--")
        if (hasPunycode) {
            findings += "Hostname uses punycode (xn--), often used to spoof real brand names."
        }

        val decoded = runCatching { IDN.toUnicode(host) }.getOrDefault(host)
        val isIdn = decoded != host
        val homoglyphScore = countHomoglyphs(decoded)
        if (homoglyphScore >= 1) {
            findings += "Hostname contains look-alike characters that mimic Latin letters."
        }

        val isIpHost = IP_REGEX.matches(host)
        if (isIpHost) {
            findings += "Host is a raw IP address. Legitimate brands use domain names."
        }

        val tld = host.substringAfterLast('.', "")
        val suspiciousTld = tld in suspiciousTlds
        if (suspiciousTld) {
            findings += "TLD .$tld is associated with high-abuse rates."
        }

        if (redirectCount >= 3) {
            findings += "URL bounced through $redirectCount redirects before resolving."
        }

        val originalHost = original?.host?.lowercase().orEmpty()
        if (originalHost.isNotEmpty() && originalHost != host) {
            findings += "Original link points to $originalHost but resolved to $host."
        }

        return UrlFeatures(
            originalUrl = originalUrl,
            finalUrl = finalUrl,
            scheme = scheme,
            host = host,
            isIdn = isIdn,
            isIpHost = isIpHost,
            hasPunycode = hasPunycode,
            homoglyphScore = homoglyphScore,
            suspiciousTld = suspiciousTld,
            redirectCount = redirectCount,
            isHttps = isHttps,
            findings = findings
        )
    }

    fun classifierPrelude(features: UrlFeatures, ocrText: String): String {
        val sb = StringBuilder()
        sb.appendLine("URL: ${features.finalUrl}")
        if (features.originalUrl != features.finalUrl) {
            sb.appendLine("Original URL: ${features.originalUrl}")
        }
        sb.appendLine("Host: ${features.host}")
        sb.appendLine("Scheme: ${features.scheme}")
        if (features.findings.isNotEmpty()) {
            sb.appendLine("URL signals:")
            features.findings.forEach { sb.appendLine("- $it") }
        }
        sb.appendLine()
        sb.appendLine("Visible page text (OCR):")
        sb.append(ocrText.take(4000))
        return sb.toString()
    }

    private fun normalize(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.isEmpty() -> trimmed
            else -> "https://$trimmed"
        }
    }

    private fun countHomoglyphs(host: String): Int {
        var n = 0
        host.forEach { c ->
            if (c.code in 0x0400..0x04FF) n++  // Cyrillic range
            if (c.code in 0x0370..0x03FF) n++  // Greek range
            if (c == 'а' || c == 'е' || c == 'о' || c == 'р' || c == 'с' || c == 'х') n++
        }
        return n
    }

    private val IP_REGEX = Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
}
