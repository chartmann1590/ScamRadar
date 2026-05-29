package com.charles.scamradar.app.community

import com.charles.scamradar.app.data.model.ScamType
import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.data.model.Verdict
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class TrendingItem(
    val scamType: String,
    val count7d: Long,
    val updatedAt: Long
)

class CommunityReportsRepository {

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    sealed interface ReportOutcome {
        data object Submitted : ReportOutcome
        data object NotEligible : ReportOutcome
        data class Failed(val message: String) : ReportOutcome
    }

    suspend fun submitReport(result: ScanResult): ReportOutcome {
        if (result.verdict != Verdict.LIKELY_SCAM) return ReportOutcome.NotEligible
        val sanitized = ReportSanitizer.sanitize(result.originalMessage)
        if (!sanitized.isUsable) return ReportOutcome.NotEligible

        val uid = AnonymousAuthBootstrapper.ensureSignedIn()
            ?: return ReportOutcome.Failed("Couldn't sign in anonymously.")

        return runCatching {
            firestore.collection("reports")
                .add(
                    mapOf(
                        "scamType" to result.scamType.name,
                        "verdict" to result.verdict.name,
                        "confidence" to result.confidence,
                        "excerpt" to sanitized.excerpt,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "authorUid" to uid
                    )
                )
                .await()
            ReportOutcome.Submitted
        }.getOrElse {
            ReportOutcome.Failed(it.message ?: "Report failed.")
        }
    }

    suspend fun loadTrending(): List<TrendingItem> {
        return runCatching {
            firestore.collection("trending")
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val type = doc.id
                    val count = doc.getLong("count7d") ?: return@mapNotNull null
                    val updated = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                    TrendingItem(type, count, updated)
                }
                .sortedByDescending { it.count7d }
        }.getOrDefault(emptyList())
    }
}
