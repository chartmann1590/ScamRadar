package com.charles.scamradar.app.shield

import android.content.Context
import com.charles.scamradar.app.classifier.ClassifierRouter
import com.charles.scamradar.app.data.datastore.UserPrefs
import com.charles.scamradar.app.data.db.AppDatabase
import com.charles.scamradar.app.data.db.ScanHistoryEntity
import com.charles.scamradar.app.data.model.ScanMode
import com.charles.scamradar.app.data.model.Verdict
import com.charles.scamradar.app.download.ModelManager
import com.charles.scamradar.app.widget.ScamRadarWidget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ShieldRouter {

    private val recentlySeen = LinkedHashMap<Int, Long>()
    private val lock = Mutex()
    private const val DEDUP_WINDOW_MS = 5L * 60 * 1000

    suspend fun classifyAndAlert(
        context: Context,
        sourcePackage: String,
        title: CharSequence?,
        text: CharSequence?,
        bigText: CharSequence?,
    ) {
        val combined = listOfNotNull(title, text, bigText)
            .joinToString("\n") { it.toString() }
            .trim()
        if (combined.length < 20) return

        val hash = combined.hashCode()
        val now = System.currentTimeMillis()
        lock.withLock {
            recentlySeen.entries.removeAll { now - it.value > DEDUP_WINDOW_MS }
            if (recentlySeen.contains(hash)) return
            recentlySeen[hash] = now
        }

        val sensitivity = UserPrefs(context).shieldSensitivity.first()
        val router = ClassifierRouter(context)

        // Stage 1 — fast Lite pass. Filters out the ~95% of safe notifications
        // before we spend Gemma cycles + RAM on them.
        val liteResult = runCatching { router.liteOnly().classify(combined) }.getOrNull() ?: return
        val needsAiConfirmation = when (liteResult.verdict) {
            Verdict.LIKELY_SCAM -> true
            Verdict.SUSPICIOUS -> true
            Verdict.SAFE -> liteResult.confidence < 0.85f
        }

        // Stage 2 — Gemma confirmation when available and the Lite verdict
        // is anything but a confident SAFE.
        val gemmaAvailable = ModelManager.isModelDownloaded(context)
        val finalResult = if (needsAiConfirmation && gemmaAvailable) {
            runCatching { router.selectClassifier().classify(combined) }
                .getOrNull()
                ?: liteResult
        } else {
            liteResult
        }

        val crossesThreshold = when (sensitivity) {
            UserPrefs.SHIELD_SENSITIVITY_LOW -> finalResult.verdict == Verdict.LIKELY_SCAM
            UserPrefs.SHIELD_SENSITIVITY_HIGH ->
                finalResult.verdict == Verdict.LIKELY_SCAM || finalResult.verdict == Verdict.SUSPICIOUS
            else -> finalResult.verdict == Verdict.LIKELY_SCAM ||
                (finalResult.verdict == Verdict.SUSPICIOUS && finalResult.confidence > 0.6f)
        }
        if (!crossesThreshold) return

        val db = AppDatabase.getInstance(context)
        runCatching { db.scanHistoryDao().insert(ScanHistoryEntity(finalResult, ScanMode.SHIELD)) }
        runCatching { ScamRadarWidget.refreshAll(context) }
        ShieldAlertNotifier.postAlert(context, sourcePackage, finalResult)
    }
}
