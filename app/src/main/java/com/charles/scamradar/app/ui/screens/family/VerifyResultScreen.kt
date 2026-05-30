package com.charles.scamradar.app.ui.screens.family

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import com.charles.scamradar.app.family.VerifyChallengeService

@Composable
fun VerifyResultScreen(
    encoded: String,
    onContinue: () -> Unit,
) {
    var result by remember { mutableStateOf<VerifyChallengeService.Result?>(null) }

    LaunchedEffect(encoded) {
        result = VerifyChallengeService.verify(encoded)
    }

    val verdict = result
    Scaffold { inner ->
        if (verdict == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(inner),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Verifying…")
            }
            return@Scaffold
        }

        val (bg, title, subtitle, icon) = when (verdict) {
            is VerifyChallengeService.Result.Verified -> Quadruple(
                Color(0xFF1E7A3A),
                "Verified ✓",
                "This is really ${verdict.memberLabel} from your pod (${verdict.podCode}).",
                Icons.Default.CheckCircle,
            )
            VerifyChallengeService.Result.SignatureMismatch -> Quadruple(
                Color(0xFFB3261E),
                "Not from your pod",
                "This signature does not match any of your family pod members.",
                Icons.Default.Block,
            )
            VerifyChallengeService.Result.UnknownPod -> Quadruple(
                Color(0xFFB3261E),
                "Pod not recognized",
                "We don't have the pod key on this device.",
                Icons.Default.Block,
            )
            VerifyChallengeService.Result.Expired -> Quadruple(
                Color(0xFFE07B00),
                "Expired",
                "This verified link is older than 72 hours.",
                Icons.Default.Block,
            )
            VerifyChallengeService.Result.MalformedLink -> Quadruple(
                Color(0xFFB3261E),
                "Invalid link",
                "This is not a valid ScamRadar verification link.",
                Icons.Default.Block,
            )
        }

        Box(modifier = Modifier.fillMaxSize().padding(inner).background(bg)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(96.dp))
                Spacer(Modifier.height(24.dp))
                Text(
                    text = title,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = subtitle,
                    fontSize = 22.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(40.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text("Continue", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
