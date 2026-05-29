package com.charles.scamradar.app.ui.screens.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.charles.scamradar.app.data.model.UrlScanMetadata
import java.net.URI

private val Danger = Color(0xFFEF4444)
private val Warning = Color(0xFFF59E0B)
private val Safe = Color(0xFF10B981)
private val Neutral = Color(0xFF6B7280)

@Composable
fun LinkMicroscopeCard(metadata: UrlScanMetadata) {
    val parts = remember(metadata.finalUrl) { parseUrl(metadata.finalUrl) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Biotech,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Link microscope",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            UrlAnatomyRow(parts, metadata)

            if (metadata.redirectCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Redirect chain",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = metadata.originalUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = "↓ ${metadata.redirectCount} hop${if (metadata.redirectCount == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Warning,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = metadata.finalUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UrlAnatomyRow(parts: UrlParts, metadata: UrlScanMetadata) {
    val schemeColor = if (parts.scheme == "https") Safe else Danger
    val hostColor = when {
        metadata.findings.any { it.contains("look-alike") } -> Danger
        metadata.findings.any { it.contains("punycode") || it.contains("IP") } -> Danger
        else -> Neutral
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "URL anatomy",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            AnatomyChip(label = "Scheme", value = parts.scheme.ifEmpty { "—" }, color = schemeColor)
            AnatomyChip(label = "Host", value = parts.host.ifEmpty { "—" }, color = hostColor)
            if (parts.path.isNotEmpty()) {
                AnatomyChip(label = "Path", value = parts.path, color = Neutral)
            }
            if (parts.query.isNotEmpty()) {
                AnatomyChip(label = "Query", value = parts.query.take(80), color = Neutral)
            }
        }
    }
}

@Composable
private fun AnatomyChip(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(60.dp)
                .padding(end = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

private data class UrlParts(
    val scheme: String,
    val host: String,
    val path: String,
    val query: String
)

private fun parseUrl(url: String): UrlParts {
    return runCatching {
        val u = URI(url)
        UrlParts(
            scheme = u.scheme?.lowercase().orEmpty(),
            host = u.host?.lowercase().orEmpty(),
            path = u.path.orEmpty(),
            query = u.query.orEmpty()
        )
    }.getOrDefault(UrlParts("", url, "", ""))
}
