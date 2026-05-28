package com.scamradar.app.analytics

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.scamradar.app.data.model.ClassifierTier
import com.scamradar.app.data.model.ScamType
import com.scamradar.app.data.model.ScanMode
import com.scamradar.app.data.model.Verdict

object Analytics {

    fun scanStarted(scanMode: ScanMode, tier: ClassifierTier) {
    }

    fun startClassifyTrace(tier: ClassifierTier): Trace {
        return FirebasePerformance.getInstance().newTrace("classify_${tier.name}")
    }

    fun scanCompleted(verdict: Verdict, scamType: ScamType, tier: ClassifierTier, durationMs: Long) {
    }

    fun shareCardExported(verdict: Verdict, format: String) {
    }

    fun libraryViewed() {
    }

    fun libraryDetailViewed(category: String) {
    }
}
