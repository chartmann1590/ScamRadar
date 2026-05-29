package com.charles.scamradar.app.ui.screens.urlscan

import android.annotation.SuppressLint
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.charles.scamradar.app.analytics.Analytics
import com.charles.scamradar.app.classifier.ClassifierRouter
import com.charles.scamradar.app.data.model.ScanMode
import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.ocr.OcrProcessor
import com.charles.scamradar.app.ui.components.PulsingShieldRings
import com.charles.scamradar.app.webcapture.CaptureResult
import com.charles.scamradar.app.webcapture.PageReadyDetector
import com.charles.scamradar.app.webcapture.SafeBrowsingGuard
import com.charles.scamradar.app.webcapture.UrlFeatureExtractor
import com.charles.scamradar.app.webcapture.UrlPageCapturer
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

enum class UrlScanPhase(
    val label: String,
    val icon: ImageVector,
    val color: Color
) {
    LOADING("Loading the website…", Icons.Default.Language, Color(0xFF06B6D4)),
    CAPTURING("Capturing screenshot…", Icons.Default.CameraAlt, Color(0xFFF59E0B)),
    READING("Reading visible text…", Icons.Default.Visibility, Color(0xFF8B5CF6)),
    ANALYZING("Analyzing for scams…", Icons.Default.Psychology, Color(0xFFEF4444))
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun UrlScanningScreen(
    url: String,
    classifierRouter: ClassifierRouter,
    onResult: (ScanResult, CaptureResult) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val ocrProcessor = remember { OcrProcessor() }
    val detector = remember(url) { PageReadyDetector() }

    var phase by remember(url) { mutableStateOf(UrlScanPhase.LOADING) }
    var errorMessage by remember(url) { mutableStateOf<String?>(null) }
    var capturedFinalUrl by remember(url) { mutableStateOf(url) }
    var redirectCount by remember(url) { mutableStateOf(0) }
    var webViewRef by remember(url) { mutableStateOf<WebView?>(null) }
    val startedAt = remember(url) { System.currentTimeMillis() }
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnError by rememberUpdatedState(onError)

    LaunchedEffect(url, webViewRef) {
        val wv = webViewRef ?: return@LaunchedEffect
        try {
            phase = UrlScanPhase.LOADING
            val ok = detector.await(wv)
            if (!ok) {
                // Page didn't reach fully-loaded by the cap — proceed anyway.
            }

            phase = UrlScanPhase.CAPTURING
            delay(250)
            val screenshotPath = UrlPageCapturer.captureToFile(context, wv)

            phase = UrlScanPhase.READING
            val screenshotUri = Uri.fromFile(File(screenshotPath))
            val ocrText = ocrProcessor.recognizeText(screenshotUri, context)

            phase = UrlScanPhase.ANALYZING
            val features = UrlFeatureExtractor.extract(
                originalUrl = url,
                finalUrl = capturedFinalUrl,
                redirectCount = redirectCount
            )
            val prelude = UrlFeatureExtractor.classifierPrelude(features, ocrText)
            val tier = classifierRouter.currentTier()
            Analytics.scanStarted(ScanMode.URL, tier)
            val classifyStart = System.currentTimeMillis()
            val result = classifierRouter.selectClassifier().classify(prelude)
            Analytics.scanCompleted(
                verdict = result.verdict,
                scamType = result.scamType,
                tier = result.classifierTier,
                durationMs = System.currentTimeMillis() - classifyStart
            )

            val capture = CaptureResult(
                originalUrl = url,
                finalUrl = capturedFinalUrl,
                screenshotPath = screenshotPath,
                redirectCount = redirectCount,
                loadDurationMs = System.currentTimeMillis() - startedAt
            )
            currentOnResult(result, capture)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            errorMessage = t.message?.takeIf { it.isNotBlank() }
                ?: "Couldn't analyze that website. The site may be unreachable or blocked."
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Invisible but attached WebView — alpha 0 keeps it rendered by the
            // view system so onPageFinished + draw() work reliably.
            key(url) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0f),
                    factory = { ctx ->
                        SafeBrowsingGuard.initialize(ctx)
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            SafeBrowsingGuard.harden(this)
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    detector.onProgress(newProgress)
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val nextUrl = request?.url?.toString() ?: return false
                                    if (nextUrl.startsWith("https://") || nextUrl.startsWith("http://")) {
                                        if (nextUrl != url) {
                                            redirectCount += 1
                                            capturedFinalUrl = nextUrl
                                        }
                                        return false
                                    }
                                    return true
                                }
                                override fun onPageFinished(view: WebView?, finalUrl: String?) {
                                    if (finalUrl != null) capturedFinalUrl = finalUrl
                                    detector.onPageFinished()
                                }
                            }
                            webViewRef = this
                            loadUrl(url)
                        }
                    }
                )
            }

            if (errorMessage != null) {
                ErrorState(
                    message = errorMessage!!,
                    url = url,
                    onDismiss = { currentOnError(errorMessage!!) }
                )
            } else {
                ScanningStateColumn(phase = phase, url = url)
            }
        }
    }
}

@Composable
private fun ScanningStateColumn(phase: UrlScanPhase, url: String) {
    val ringColor by animateColorAsState(targetValue = phase.color, label = "ringColor")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            PulsingShieldRings(diameter = 280.dp, color = ringColor) {
                AnimatedContent(
                    targetState = phase,
                    transitionSpec = {
                        (fadeIn() + slideInVertically { it / 4 }) togetherWith
                            (fadeOut() + slideOutVertically { -it / 4 })
                    },
                    label = "phaseIcon"
                ) { current ->
                    Icon(
                        imageVector = current.icon,
                        contentDescription = null,
                        tint = ringColor,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(36.dp))
        AnimatedContent(
            targetState = phase,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "phaseLabel"
        ) { current ->
            Text(
                text = current.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = url,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.55f)
                .height(4.dp),
            color = ringColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun ErrorState(message: String, url: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFF59E0B),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Couldn't analyze that website",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Try a different URL")
        }
    }
}
