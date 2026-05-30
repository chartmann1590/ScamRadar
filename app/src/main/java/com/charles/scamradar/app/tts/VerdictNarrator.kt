package com.charles.scamradar.app.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import com.charles.scamradar.app.data.model.ScamType
import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.data.model.Verdict
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class VerdictNarrator(context: Context) {

    private val ready = AtomicBoolean(false)
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                runCatching { tts.language = Locale.getDefault() }
                ready.set(true)
            }
        }
    }

    fun speak(scanResult: ScanResult) {
        if (!ready.get()) return
        val text = lineFor(scanResult)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun speakText(text: String) {
        if (!ready.get()) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun stop() {
        runCatching { tts.stop() }
    }

    fun shutdown() {
        runCatching { tts.stop() }
        runCatching { tts.shutdown() }
    }

    private fun lineFor(scanResult: ScanResult): String {
        val verdictLine = when (scanResult.verdict) {
            Verdict.LIKELY_SCAM -> "Stop. This looks like a scam. Do not reply. Do not call back."
            Verdict.SUSPICIOUS -> "Be careful. There are warning signs in this message. Verify the sender before doing anything."
            Verdict.SAFE -> "This looks safe. We did not find scam signs in this message."
        }
        val typeLine = when (scanResult.scamType) {
            ScamType.PHISHING -> " It looks like a phishing attempt that tries to steal your login."
            ScamType.ROMANCE -> " It looks like a romance scam — never send money to someone you have not met."
            ScamType.IRS_IMPERSONATION -> " It looks like a tax-agency impersonator. The real IRS does not call or text."
            ScamType.CRYPTO -> " It looks like a cryptocurrency scam."
            ScamType.FAMILY_EMERGENCY -> " It looks like the family-emergency scam. Verify with your family directly."
            ScamType.PACKAGE_DELIVERY -> " It looks like a fake delivery message."
            ScamType.JOB_OFFER -> " It looks like a fake job-offer scam."
            ScamType.TECH_SUPPORT -> " It looks like a fake tech-support scam."
            ScamType.LOTTERY -> " It looks like a prize or lottery scam."
            ScamType.INVESTMENT -> " It looks like an investment scam."
            ScamType.OTHER, ScamType.NONE -> ""
        }
        return verdictLine + typeLine
    }
}
