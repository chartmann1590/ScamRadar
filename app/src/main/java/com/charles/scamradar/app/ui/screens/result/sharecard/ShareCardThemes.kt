package com.charles.scamradar.app.ui.screens.result.sharecard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.data.model.Verdict

enum class ShareCardTheme { Minimal, BoldAlert, Educational }

@Composable
fun ShareCardSelector(theme: ShareCardTheme, result: ScanResult) {
    when (theme) {
        ShareCardTheme.Minimal -> MinimalShareCard(result)
        ShareCardTheme.BoldAlert -> BoldAlertShareCard(result)
        ShareCardTheme.Educational -> EducationalShareCard(result)
    }
}

@Composable
fun MinimalShareCard(result: ScanResult) {
    val verdictColor = verdictColor(result.verdict)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0B1220))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ScamRadar",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = verdictLabel(result.verdict),
            color = verdictColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${(result.confidence * 100).toInt()}% confidence",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
        if (result.redFlags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            result.redFlags.take(3).forEach { flag ->
                Text(
                    text = "• ${flag.phrase}",
                    color = Color(0xFFFCA5A5),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun BoldAlertShareCard(result: ScanResult) {
    val verdictColor = verdictColor(result.verdict)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(verdictColor, verdictColor.copy(alpha = 0.7f))
                )
            )
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "SCAM ALERT",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Icon(
            imageVector = if (result.verdict == Verdict.SAFE) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = verdictLabel(result.verdict).uppercase(),
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${(result.confidence * 100).toInt()}% confidence",
            color = Color.White,
            fontSize = 14.sp
        )
        if (result.redFlags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Watch for:",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            result.redFlags.take(2).forEach { flag ->
                Text(
                    text = "→ ${flag.phrase}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Caught by ScamRadar — on-device AI",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp
        )
    }
}

@Composable
fun EducationalShareCard(result: ScanResult) {
    val verdictColor = verdictColor(result.verdict)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFFBEB))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = Color(0xFFD97706),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Spot the scam — a teachable moment",
                color = Color(0xFF7C2D12),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = verdictLabel(result.verdict),
            color = verdictColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        if (result.redFlags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "What to look for next time:",
                color = Color(0xFF1F2937),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            result.redFlags.take(4).forEachIndexed { index, flag ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(verdictColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = flag.phrase,
                            color = Color(0xFF111827),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = flag.reason,
                            color = Color(0xFF4B5563),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Generated by ScamRadar — free, private, on-device.",
            color = Color(0xFF6B7280),
            fontSize = 11.sp
        )
    }
}

private fun verdictLabel(verdict: Verdict): String = when (verdict) {
    Verdict.SAFE -> "Looks safe"
    Verdict.SUSPICIOUS -> "Suspicious"
    Verdict.LIKELY_SCAM -> "Likely scam"
}

private fun verdictColor(verdict: Verdict): Color = when (verdict) {
    Verdict.SAFE -> Color(0xFF10B981)
    Verdict.SUSPICIOUS -> Color(0xFFF59E0B)
    Verdict.LIKELY_SCAM -> Color(0xFFEF4444)
}
