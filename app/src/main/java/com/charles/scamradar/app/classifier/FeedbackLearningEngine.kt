package com.charles.scamradar.app.classifier

import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.data.model.Verdict

/**
 * Learns from user feedback ("this wasn't a scam") without any server-side training:
 * each dismissed alert is reduced to a token signature and stored on-device. Future
 * scans that closely resemble a previously-dismissed message have their verdict
 * softened, so the app stops repeating the same false alert once a user has corrected it.
 */
object FeedbackLearningEngine {

    const val SIMILARITY_THRESHOLD = 0.5f

    private val STOPWORDS = setOf(
        "this", "that", "with", "from", "your", "have", "will", "would", "could",
        "there", "their", "about", "which", "when", "what", "please", "just", "were",
        "been", "into", "than", "then", "they", "them", "some", "such", "only",
        "also", "here", "very", "more", "most", "does", "should"
    )

    /** Normalizes a message into a stable set of salient tokens for similarity comparison. */
    fun signature(message: String): Set<String> {
        return Regex("[a-zA-Z]{4,}")
            .findAll(message.lowercase())
            .map { it.value }
            .filterNot { it in STOPWORDS }
            .toSet()
    }

    /** Jaccard similarity between two token sets, in [0, 1]. */
    fun similarity(a: Set<String>, b: Set<String>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val intersection = a.intersect(b).size
        val union = a.union(b).size
        return if (union == 0) 0f else intersection.toFloat() / union.toFloat()
    }

    /**
     * Softens [result]'s verdict by one step (LIKELY_SCAM -> SUSPICIOUS -> SAFE) if its
     * message is similar enough to a message the user previously marked as not a scam.
     * Never touches already-SAFE results and never escalates a verdict.
     */
    fun adjustForFeedback(
        result: ScanResult,
        knownFalsePositiveSignatures: List<Set<String>>,
        threshold: Float = SIMILARITY_THRESHOLD
    ): ScanResult {
        if (result.verdict == Verdict.SAFE || knownFalsePositiveSignatures.isEmpty()) return result

        val sig = signature(result.originalMessage)
        if (sig.isEmpty()) return result

        val bestMatch = knownFalsePositiveSignatures.maxOf { similarity(sig, it) }
        if (bestMatch < threshold) return result

        val downgradedVerdict = when (result.verdict) {
            Verdict.LIKELY_SCAM -> Verdict.SUSPICIOUS
            Verdict.SUSPICIOUS -> Verdict.SAFE
            Verdict.SAFE -> Verdict.SAFE
        }
        val newConfidence = (result.confidence * (1f - bestMatch)).coerceIn(0f, 1f)

        return result.copy(
            verdict = downgradedVerdict,
            confidence = newConfidence,
            recommendedAction = if (downgradedVerdict == Verdict.SAFE) {
                "This message appears to be safe, but always remain cautious."
            } else {
                "You've told ScamRadar similar messages weren't scams before, so this alert was softened. " +
                    result.recommendedAction
            }
        )
    }
}
