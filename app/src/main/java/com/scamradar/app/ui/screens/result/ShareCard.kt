package com.scamradar.app.ui.screens.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scamradar.app.data.model.ScanResult

@Composable
fun ShareCard(result: ScanResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0B1220))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "ScamRadar",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Verdict: ${result.verdict.name}",
            color = when (result.verdict) {
                com.scamradar.app.data.model.Verdict.SAFE -> Color(0xFF10B981)
                com.scamradar.app.data.model.Verdict.SUSPICIOUS -> Color(0xFFF59E0B)
                com.scamradar.app.data.model.Verdict.LIKELY_SCAM -> Color(0xFFEF4444)
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Confidence: ${(result.confidence * 100).toInt()}%",
            color = Color.White,
            fontSize = 14.sp
        )
        if (result.redFlags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            result.redFlags.forEach { flag ->
                Text(
                    text = "- ${flag.phrase}",
                    color = Color(0xFFFCA5A5),
                    fontSize = 12.sp
                )
            }
        }
    }
}
