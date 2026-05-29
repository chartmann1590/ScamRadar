package com.charles.scamradar.app.widget

import android.content.Context
import com.charles.scamradar.app.data.db.AppDatabase
import com.charles.scamradar.app.data.db.ScanHistoryEntity
import kotlinx.coroutines.flow.first

data class WidgetSnapshot(
    val totalThisMonth: Int,
    val flaggedThisMonth: Int,
    val recent: List<RecentVerdict>
) {
    companion object {
        val Empty = WidgetSnapshot(
            totalThisMonth = 0,
            flaggedThisMonth = 0,
            recent = emptyList()
        )
    }
}

data class RecentVerdict(
    val id: Long,
    val verdict: String,
    val excerpt: String,
    val timestamp: Long
)

suspend fun loadWidgetSnapshot(context: Context): WidgetSnapshot {
    val dao = AppDatabase.getInstance(context).scanHistoryDao()
    val all = dao.getAll().first()
    val cutoff = thirtyDaysAgoMillis()
    val recent30 = all.filter { it.timestamp >= cutoff }

    val recent3 = all
        .sortedByDescending { it.timestamp }
        .take(3)
        .map { it.toRecent() }

    return WidgetSnapshot(
        totalThisMonth = recent30.size,
        flaggedThisMonth = recent30.count { it.verdict != "SAFE" },
        recent = recent3
    )
}

private fun ScanHistoryEntity.toRecent(): RecentVerdict {
    val text = originalMessage.lineSequence().firstOrNull().orEmpty().trim()
    val excerpt = if (text.length > 48) text.substring(0, 48) + "…" else text
    return RecentVerdict(
        id = id,
        verdict = verdict,
        excerpt = excerpt.ifEmpty { "Tap to scan" },
        timestamp = timestamp
    )
}

private fun thirtyDaysAgoMillis(): Long =
    System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L
