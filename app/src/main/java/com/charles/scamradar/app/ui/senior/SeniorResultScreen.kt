package com.charles.scamradar.app.ui.senior

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charles.scamradar.app.data.datastore.UserPrefs
import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.data.model.Verdict
import com.charles.scamradar.app.tts.VerdictNarrator

@Composable
fun SeniorResultScreen(
    scanResult: ScanResult,
    onScanAgain: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { UserPrefs(context) }
    val emergencyContact by prefs.seniorEmergencyContact.collectAsState(initial = "")
    val narrator = remember { VerdictNarrator(context) }

    LaunchedEffect(scanResult) { narrator.speak(scanResult) }
    DisposableEffect(Unit) { onDispose { narrator.shutdown() } }

    val bg = when (scanResult.verdict) {
        Verdict.LIKELY_SCAM -> Color(0xFFB3261E)
        Verdict.SUSPICIOUS -> Color(0xFFE07B00)
        Verdict.SAFE -> Color(0xFF1E7A3A)
    }
    val title = when (scanResult.verdict) {
        Verdict.LIKELY_SCAM -> "STOP"
        Verdict.SUSPICIOUS -> "BE CAREFUL"
        Verdict.SAFE -> "LOOKS SAFE"
    }
    val subtitle = when (scanResult.verdict) {
        Verdict.LIKELY_SCAM -> "This looks like a scam. Do not reply."
        Verdict.SUSPICIOUS -> "There are warning signs. Verify before acting."
        Verdict.SAFE -> "No scam signs found."
    }

    Box(modifier = Modifier.fillMaxSize().background(bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(20.dp))

            Icon(
                imageVector = if (scanResult.verdict == Verdict.SAFE) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(96.dp),
            )

            Text(
                text = title,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                fontSize = 24.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Button(
                onClick = { narrator.speak(scanResult) },
                modifier = Modifier.fillMaxWidth().height(72.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = bg,
                ),
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.size(12.dp))
                Text("Read this to me again", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            if (scanResult.recommendedAction.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.18f)),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "What to do",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = scanResult.recommendedAction,
                            fontSize = 22.sp,
                            color = Color.White,
                        )
                    }
                }
            }

            if (emergencyContact.isNotBlank()) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$emergencyContact"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = bg,
                    ),
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.size(16.dp))
                    Text("Call my family", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = onScanAgain,
                modifier = Modifier.fillMaxWidth().height(72.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.25f),
                    contentColor = Color.White,
                ),
            ) {
                Text("Check another message", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
