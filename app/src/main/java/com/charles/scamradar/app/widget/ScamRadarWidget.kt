package com.charles.scamradar.app.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.runtime.Composable
import com.charles.scamradar.app.MainActivity
import com.charles.scamradar.app.ui.quickverdict.QuickVerdictActivity

class ScamRadarWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadWidgetSnapshot(context)
        provideContent {
            GlanceTheme {
                WidgetBody(snapshot)
            }
        }
    }

    companion object {
        suspend fun refreshAll(context: Context) {
            ScamRadarWidget().updateAll(context)
        }
    }
}

@Composable
private fun WidgetBody(snapshot: WidgetSnapshot) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .cornerRadius(28.dp)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            HeaderRow(snapshot)
            Spacer(modifier = GlanceModifier.height(10.dp))
            if (snapshot.recent.isEmpty()) {
                EmptyState()
            } else {
                RecentList(snapshot.recent)
            }
        }
    }
}

@Composable
private fun HeaderRow(snapshot: WidgetSnapshot) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShieldDot()
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = "ScamRadar",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = monthSummary(snapshot),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@Composable
private fun ShieldDot() {
    Box(
        modifier = GlanceModifier
            .size(28.dp)
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🛡",
            style = TextStyle(
                color = GlanceTheme.colors.onPrimaryContainer,
                fontSize = 14.sp
            )
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(18.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<QuickVerdictActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Tap to check a suspicious message",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun RecentList(items: List<RecentVerdict>) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            VerdictRow(item)
            if (index < items.lastIndex) {
                Spacer(modifier = GlanceModifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun VerdictRow(item: RecentVerdict) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(14.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .size(10.dp)
                .background(ColorProvider(verdictColor(item.verdict)))
                .cornerRadius(5.dp)
        ) {}
        Spacer(modifier = GlanceModifier.width(10.dp))
        Text(
            text = item.excerpt,
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 12.sp
            )
        )
    }
}

private fun monthSummary(snapshot: WidgetSnapshot): String {
    return when {
        snapshot.totalThisMonth == 0 -> "No scans yet"
        snapshot.flaggedThisMonth == 0 -> "${snapshot.totalThisMonth} safe scans · 30d"
        else -> "${snapshot.flaggedThisMonth} flagged · 30d"
    }
}

private fun verdictColor(verdict: String): Color {
    return when (verdict) {
        "LIKELY_SCAM" -> Color(0xFFEF4444)
        "SUSPICIOUS" -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }
}
