package com.charles.scamradar.app.classifier

import com.charles.scamradar.app.data.model.ClassifierTier
import com.charles.scamradar.app.data.model.ScamType
import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.data.model.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackLearningEngineTest {

    private fun scanResult(message: String, verdict: Verdict, confidence: Float = 0.8f) = ScanResult(
        verdict = verdict,
        confidence = confidence,
        scamType = ScamType.OTHER,
        redFlags = emptyList(),
        aiGeneratedIndicators = emptyList(),
        recommendedAction = "Verify the sender before acting.",
        originalMessage = message,
        classifierTier = ClassifierTier.LITE
    )

    @Test
    fun `identical message previously marked false positive is downgraded`() {
        val message = "Your package delivery is delayed, please confirm your address to reschedule"
        val original = scanResult(message, Verdict.SUSPICIOUS)
        val knownFalsePositives = listOf(FeedbackLearningEngine.signature(message))

        val adjusted = FeedbackLearningEngine.adjustForFeedback(original, knownFalsePositives)

        assertEquals(Verdict.SAFE, adjusted.verdict)
    }

    @Test
    fun `likely scam downgrades only one step to suspicious`() {
        val message = "Your package delivery is delayed, please confirm your address to reschedule"
        val original = scanResult(message, Verdict.LIKELY_SCAM)
        val knownFalsePositives = listOf(FeedbackLearningEngine.signature(message))

        val adjusted = FeedbackLearningEngine.adjustForFeedback(original, knownFalsePositives)

        assertEquals(Verdict.SUSPICIOUS, adjusted.verdict)
    }

    @Test
    fun `unrelated message is not affected by unrelated feedback`() {
        val original = scanResult(
            "Congratulations, you have won a lottery prize, send your bank details now",
            Verdict.LIKELY_SCAM
        )
        val knownFalsePositives = listOf(
            FeedbackLearningEngine.signature("Your package delivery is delayed, please confirm your address")
        )

        val adjusted = FeedbackLearningEngine.adjustForFeedback(original, knownFalsePositives)

        assertEquals(Verdict.LIKELY_SCAM, adjusted.verdict)
        assertEquals(original.confidence, adjusted.confidence, 0.001f)
    }

    @Test
    fun `safe verdict is never touched`() {
        val message = "Hey, are we still on for lunch tomorrow?"
        val original = scanResult(message, Verdict.SAFE)
        val knownFalsePositives = listOf(FeedbackLearningEngine.signature(message))

        val adjusted = FeedbackLearningEngine.adjustForFeedback(original, knownFalsePositives)

        assertEquals(original, adjusted)
    }

    @Test
    fun `no known feedback leaves result untouched`() {
        val original = scanResult("Some suspicious looking message", Verdict.SUSPICIOUS)

        val adjusted = FeedbackLearningEngine.adjustForFeedback(original, emptyList())

        assertEquals(original, adjusted)
    }

    @Test
    fun `similarity is symmetric and bounded`() {
        val a = FeedbackLearningEngine.signature("urgent account verification required immediately")
        val b = FeedbackLearningEngine.signature("urgent account verification required now")

        val sim = FeedbackLearningEngine.similarity(a, b)

        assertTrue(sim in 0f..1f)
        assertEquals(sim, FeedbackLearningEngine.similarity(b, a), 0.0001f)
    }
}
