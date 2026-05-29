package com.charles.scamradar.app.ui.screens.scanning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.charles.scamradar.app.analytics.Analytics
import com.charles.scamradar.app.classifier.ClassifierRouter
import com.charles.scamradar.app.data.model.ScanMode
import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.ui.components.PulsingShieldRings
import kotlinx.coroutines.CancellationException

@Composable
fun ScanningScreen(
    message: String,
    scanMode: ScanMode,
    classifierRouter: ClassifierRouter,
    onResult: (ScanResult) -> Unit,
    onError: () -> Unit
) {
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnError by rememberUpdatedState(onError)

    LaunchedEffect(message) {
        val tier = classifierRouter.currentTier()
        Analytics.scanStarted(scanMode, tier)
        val trace = Analytics.startClassifyTrace(tier)
        val startedAt = System.currentTimeMillis()
        try {
            val result = classifierRouter.selectClassifier().classify(message)
            trace.stop()
            Analytics.scanCompleted(
                verdict = result.verdict,
                scamType = result.scamType,
                tier = result.classifierTier,
                durationMs = System.currentTimeMillis() - startedAt
            )
            currentOnResult(result)
        } catch (e: CancellationException) {
            trace.stop()
            throw e
        } catch (_: Exception) {
            trace.stop()
            currentOnError()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PulsingShieldRings(diameter = 280.dp)

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Analyzing on your device…",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your message never leaves your phone",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            LinearProgressIndicator(
                modifier = Modifier.width(160.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round
            )
        }
    }
}
