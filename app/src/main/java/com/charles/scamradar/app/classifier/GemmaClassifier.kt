package com.charles.scamradar.app.classifier

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.charles.scamradar.app.data.model.ClassifierTier
import com.charles.scamradar.app.data.model.RedFlag
import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.data.model.ScamType
import com.charles.scamradar.app.data.model.Verdict
import com.charles.scamradar.app.download.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GemmaClassifier private constructor(
    private val context: Context,
    private val gson: Gson = Gson()
) : ScamClassifier {

    override val name: String = "Gemma 4 (LiteRT-LM)"

    override val isAvailable: Boolean
        get() = ModelManager.isModelDownloaded(context)

    override suspend fun classify(message: String): ScanResult = withContext(Dispatchers.IO) {
        val modelFile = ModelManager.getModelFile(context)
        if (!modelFile.exists()) {
            return@withContext LiteClassifier().classify(message)
        }

        runCatching {
            val engineConfig = EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = Backend.CPU(),
                cacheDir = context.cacheDir.absolutePath
            )
            Engine(engineConfig).use { engine ->
                engine.initialize()
                engine.createConversation().use { conversation ->
                    val response = conversation.sendMessage(buildPrompt(message))
                        .contents
                        .contents
                        .filterIsInstance<Content.Text>()
                        .joinToString("") { it.text }
                    parseResponse(response, message)
                }
            }
        }.getOrElse {
            LiteClassifier().classify(message).copy(classifierTier = ClassifierTier.LITE)
        }
    }

    private fun buildPrompt(message: String): String {
        return """
            You are ScamRadar, an on-device scam detection specialist.
            Analyze the message and return ONLY compact JSON with this exact schema:
            {
              "verdict": "SAFE" | "SUSPICIOUS" | "LIKELY_SCAM",
              "confidence": 0.0,
              "scam_type": "PHISHING" | "ROMANCE" | "IRS_IMPERSONATION" | "CRYPTO" | "FAMILY_EMERGENCY" | "PACKAGE_DELIVERY" | "JOB_OFFER" | "TECH_SUPPORT" | "OTHER" | "NONE",
              "red_flags": [{"phrase":"exact text from message","reason":"short reason"}],
              "ai_generated_indicators": ["short indicator"],
              "recommended_action": "short practical advice"
            }
            Message:
            ${message.take(6000)}
        """.trimIndent()
    }

    private fun parseResponse(response: String, originalMessage: String): ScanResult {
        val jsonText = response.substringAfter('{', "").substringBeforeLast('}', "")
        val json = JsonParser.parseString("{$jsonText}").asJsonObject
        val verdict = enumValue(json, "verdict", Verdict.SAFE)
        val scamType = enumValue(json, "scam_type", ScamType.NONE)
        val redFlags = json.getAsJsonArray("red_flags")?.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val phrase = obj.get("phrase")?.asString.orEmpty()
            val reason = obj.get("reason")?.asString.orEmpty()
            if (phrase.isBlank() || reason.isBlank()) null else RedFlag(phrase, reason)
        }.orEmpty()
        val indicators = json.getAsJsonArray("ai_generated_indicators")?.mapNotNull {
            runCatching { it.asString }.getOrNull()
        }.orEmpty()

        return ScanResult(
            verdict = verdict,
            confidence = json.get("confidence")?.asFloat?.coerceIn(0f, 1f) ?: 0.5f,
            scamType = scamType,
            redFlags = redFlags,
            aiGeneratedIndicators = indicators,
            recommendedAction = json.get("recommended_action")?.asString.orEmpty()
                .ifBlank { defaultAction(verdict) },
            originalMessage = originalMessage,
            classifierTier = ClassifierTier.GEMMA
        )
    }

    private inline fun <reified T : Enum<T>> enumValue(json: JsonObject, key: String, fallback: T): T {
        val raw = json.get(key)?.asString ?: return fallback
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
    }

    private fun defaultAction(verdict: Verdict): String {
        return when (verdict) {
            Verdict.SAFE -> "No strong scam indicators were found. Still verify unexpected requests."
            Verdict.SUSPICIOUS -> "Verify the sender through an official channel before taking action."
            Verdict.LIKELY_SCAM -> "Do not reply, click links, or share personal information. Report and delete it."
        }
    }

    companion object {
        fun create(context: Context): GemmaClassifier {
            return GemmaClassifier(context.applicationContext)
        }
    }
}
