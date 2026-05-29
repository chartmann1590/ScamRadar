package com.charles.scamradar.app.community

/**
 * Strips obvious PII before a scan excerpt leaves the device.
 */
object ReportSanitizer {

    private val URL_REGEX = Regex("https?://\\S+|\\bwww\\.\\S+", RegexOption.IGNORE_CASE)
    private val EMAIL_REGEX = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val PHONE_REGEX = Regex("(?:\\+?\\d{1,3}[\\s-]?)?(?:\\(\\d{2,4}\\)[\\s-]?)?\\d{3,4}[\\s-]?\\d{3,4}(?:[\\s-]?\\d{0,4})?")
    private val LONG_NUMBER_REGEX = Regex("\\b\\d{4,}\\b")

    data class Sanitized(val excerpt: String, val isUsable: Boolean)

    fun sanitize(raw: String): Sanitized {
        var text = raw.trim()
        text = URL_REGEX.replace(text, "[link]")
        text = EMAIL_REGEX.replace(text, "[email]")
        text = PHONE_REGEX.replace(text, "[phone]")
        text = LONG_NUMBER_REGEX.replace(text, "[number]")
        text = text.replace(Regex("\\s+"), " ").trim()
        if (text.length > 280) text = text.substring(0, 280) + "..."
        return Sanitized(excerpt = text, isUsable = text.length >= 20)
    }
}
