package com.charles.scamradar.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.charles.scamradar.app.classifier.LiteClassifier
import com.charles.scamradar.app.data.model.Verdict
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ScamClassifierInstrumentedTest {

    data class Fixture(
        val id: Int,
        val message: String,
        val expected_verdict: String,
        val expected_scam_type: String
    )

    private lateinit var classifier: LiteClassifier
    private lateinit var fixtures: List<Fixture>

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        classifier = LiteClassifier(context)

        val inputStream = InstrumentationRegistry.getInstrumentation()
            .context.assets.open("scam_fixtures.json")
        val json = inputStream.bufferedReader().use { it.readText() }
        val type = object : TypeToken<Map<String, List<Fixture>>>() {}.type
        val map: Map<String, List<Fixture>> = Gson().fromJson(json, type)
        fixtures = map["scam_fixtures"] ?: emptyList()
    }

    @Test
    fun test1_fixturesLoad() {
        assertTrue("Fixtures should load from assets", fixtures.isNotEmpty())
        assertEquals("Should have 20 fixtures", 20, fixtures.size)
    }

    @Test
    fun test2_scamDetectionAccuracy() {
        var correctScam = 0
        var totalScam = 0

        fixtures.filter { it.expected_verdict == "LIKELY_SCAM" }.forEach { fixture ->
            totalScam++
            val result = classifier.classify(fixture.message)
            val isScamOrSuspicious = result.verdict == Verdict.LIKELY_SCAM ||
                result.verdict == Verdict.SUSPICIOUS
            if (isScamOrSuspicious) correctScam++
        }

        val accuracy = correctScam.toFloat() / totalScam.toFloat()
        assertTrue(
            "Scam detection accuracy should be >= 90% (was ${(accuracy * 100).toInt()}%). " +
                "Correct: $correctScam/$totalScam",
            accuracy >= 0.9f
        )
    }

    @Test
    fun test3_safeDetectionAccuracy() {
        var correctSafe = 0
        var totalSafe = 0

        fixtures.filter { it.expected_verdict == "SAFE" }.forEach { fixture ->
            totalSafe++
            val result = classifier.classify(fixture.message)
            if (result.verdict == Verdict.SAFE) correctSafe++
        }

        val accuracy = correctSafe.toFloat() / totalSafe.toFloat()
        assertTrue(
            "Safe detection accuracy should be >= 90% (was ${(accuracy * 100).toInt()}%). " +
                "Correct: $correctSafe/$totalSafe",
            accuracy >= 0.9f
        )
    }

    @Test
    fun test4_noFalsePositivesOnObviousSafe() {
        val safeMessages = fixtures.filter { it.expected_verdict == "SAFE" }
        for (fixture in safeMessages) {
            val result = classifier.classify(fixture.message)
            assertTrue(
                "Message '${fixture.message.take(40)}...' should not be LIKELY_SCAM (got ${result.verdict})",
                result.verdict != Verdict.LIKELY_SCAM
            )
        }
    }

    @Test
    fun test5_classifierReturnsStructuredResult() {
        val result = classifier.classify("URGENT: Your account has been suspended. Click here now.")
        assertTrue("Should have a non-empty verdict", result.verdict.name.isNotEmpty())
        assertTrue("Confidence should be between 0 and 1", result.confidence in 0f..1f)
        assertTrue("Should have red flags for scam-like text",
            result.verdict == Verdict.LIKELY_SCAM || result.verdict == Verdict.SUSPICIOUS)
    }
}
