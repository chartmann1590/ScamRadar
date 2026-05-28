package com.scamradar.app.classifier

import com.scamradar.app.data.model.ClassifierTier
import com.scamradar.app.data.model.RedFlag
import com.scamradar.app.data.model.ScamType
import com.scamradar.app.data.model.ScanResult
import com.scamradar.app.data.model.Verdict
import kotlinx.coroutines.delay

class StubClassifier : ScamClassifier {
    override val name: String = "StubClassifier"
    override val isAvailable: Boolean = true

    override suspend fun classify(message: String): ScanResult {
        delay(1000)
        return ScanResult(
            verdict = Verdict.LIKELY_SCAM,
            confidence = 0.92f,
            scamType = ScamType.PACKAGE_DELIVERY,
            redFlags = listOf(
                RedFlag(
                    "USPS package waiting for delivery schedule",
                    "Fake package delivery notices are a common phishing tactic to steal personal information"
                ),
                RedFlag(
                    "click the link below to schedule",
                    "Legitimate delivery services do not require you to click unfamiliar links to reschedule"
                ),
                RedFlag(
                    "package will be returned to sender",
                    "Artificial urgency threatening return of a package is designed to pressure victims into acting quickly"
                ),
                RedFlag(
                    "tracking number USPS-2024-",
                    "Fake tracking numbers with unusual formatting are a hallmark of delivery scams"
                ),
                RedFlag(
                    "http://usps.schedule-delivery-now.com",
                    "This URL impersonates USPS but uses a non-official domain \u2014 usps.com is the only legitimate domain"
                )
            ),
            aiGeneratedIndicators = listOf(
                "Generic greeting without personalization",
                "Template-like structure matching known scam patterns"
            ),
            recommendedAction = "Do not click any links. If you are expecting a package, track it directly on the official USPS website (usps.com) using the tracking number from your receipt.",
            originalMessage = message,
            timestamp = System.currentTimeMillis(),
            classifierTier = ClassifierTier.STUB
        )
    }
}
