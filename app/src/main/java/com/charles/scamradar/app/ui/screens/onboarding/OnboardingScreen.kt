package com.charles.scamradar.app.ui.screens.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

enum class DeviceClass {
    FULL,
    LITE_RECOMMENDED,
    LITE_ONLY
}

enum class DownloadState {
    IDLE,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED
}

private val Emerald = Color(0xFF10B981)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onStartDownload: () -> Unit,
    onPauseDownload: () -> Unit,
    deviceClass: DeviceClass,
    downloadProgress: Float?,
    downloadState: DownloadState,
    bytesDownloaded: Long,
    totalBytes: Long
) {
    val coroutineScope = rememberCoroutineScope()
    var choseFullModel by remember { mutableStateOf(false) }

    val pageCount = if (choseFullModel) 6 else 5
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) { page ->
                val resolvedPage = if (!choseFullModel && page >= 4) page + 1 else page

                when (resolvedPage) {
                    0 -> WelcomePage(
                        onGetStarted = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    )
                    1 -> PrivacyPage(
                        onContinue = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        }
                    )
                    2 -> HowItWorksPage(
                        onContinue = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(3)
                            }
                        }
                    )
                    3 -> DeviceCheckPage(
                        deviceClass = deviceClass,
                        onDownloadFull = {
                            if (!choseFullModel) {
                                choseFullModel = true
                                onStartDownload()
                            }
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(4)
                            }
                        },
                        onStartLite = {
                            choseFullModel = false
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(4)
                            }
                        }
                    )
                    4 -> {
                        if (choseFullModel) {
                            DownloadingPage(
                                progress = downloadProgress,
                                state = downloadState,
                                bytesDownloaded = bytesDownloaded,
                                totalBytes = totalBytes,
                                onPause = onPauseDownload,
                                onContinueInBackground = onComplete
                            )
                        } else {
                            ReadyPage(
                                isFullModel = false,
                                onStartScanning = onComplete
                            )
                        }
                    }
                    5 -> ReadyPage(
                        isFullModel = true,
                        onStartScanning = onComplete
                    )
                }
            }

            PageIndicators(
                pageCount = pageCount,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun PageIndicators(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 8.dp,
                animationSpec = tween(300),
                label = "indicator_width"
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                animationSpec = tween(300),
                label = "indicator_color"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(width)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun WelcomePage(onGetStarted: () -> Unit) {
    OnboardingPageContent {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Welcome to ScamRadar",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "The free, private, on-device AI that tells you in 3 seconds whether that text, email, or voicemail is a scam \u2014 and exactly why.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Get started",
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PrivacyPage(onContinue: () -> Unit) {
    OnboardingPageContent {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = Emerald,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Your messages never leave your phone",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))
        BulletPoint(text = "No account required")
        Spacer(modifier = Modifier.height(12.dp))
        BulletPoint(text = "No cloud processing")
        Spacer(modifier = Modifier.height(12.dp))
        BulletPoint(text = "No SMS auto-scanning")
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Continue",
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(0.8f)
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Emerald,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HowItWorksPage(onContinue: () -> Unit) {
    OnboardingPageContent {
        Text(
            text = "How It Works",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(36.dp))
        StepRow(number = 1, text = "Paste any suspicious message", icon = Icons.Filled.ContentPaste)
        Spacer(modifier = Modifier.height(20.dp))
        StepRow(number = 2, text = "AI checks it on your phone", icon = Icons.Filled.Shield)
        Spacer(modifier = Modifier.height(20.dp))
        StepRow(number = 3, text = "Get a verdict \u2014 and why", icon = Icons.Filled.CheckCircle)
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Continue",
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun StepRow(number: Int, text: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(0.85f)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DeviceCheckPage(
    deviceClass: DeviceClass,
    onDownloadFull: () -> Unit,
    onStartLite: () -> Unit
) {
    val deviceLabel = when (deviceClass) {
        DeviceClass.FULL -> "Full"
        DeviceClass.LITE_RECOMMENDED -> "Lite-recommended"
        DeviceClass.LITE_ONLY -> "Lite-only"
    }

    OnboardingPageContent {
        Text(
            text = "Device Check",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Text(
                text = "Your device: $deviceLabel",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onDownloadFull,
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(12.dp),
            enabled = deviceClass != DeviceClass.LITE_ONLY
        ) {
            Text(
                text = "Download AI now (~3.1 GB, Wi-Fi recommended)",
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onStartLite,
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Start with Lite mode",
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Full AI mode downloads the Gemma 4 model for on-device analysis. Lite mode uses fast pattern matching \u2014 no download required.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(0.85f)
        )
    }
}

@Composable
private fun DownloadingPage(
    progress: Float?,
    state: DownloadState,
    bytesDownloaded: Long,
    totalBytes: Long,
    onPause: () -> Unit,
    onContinueInBackground: () -> Unit
) {
    val currentProgress = progress ?: 0f
    val totalMb = if (totalBytes > 0L) (totalBytes / (1024 * 1024)).toInt() else 3100
    val downloadedMb = if (bytesDownloaded > 0L) {
        (bytesDownloaded / (1024 * 1024)).toInt()
    } else {
        (currentProgress * totalMb).toInt()
    }
    val estimatedSeconds = if (currentProgress in 0.001f..0.999f) {
        ((1f - currentProgress) * 300).toInt()
    } else 0
    val minutes = estimatedSeconds / 60
    val seconds = estimatedSeconds % 60

    OnboardingPageContent {
        Text(
            text = "Downloading AI Model",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(32.dp))
        LinearProgressIndicator(
            progress = { currentProgress },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp)),
            strokeCap = StrokeCap.Round,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "${(currentProgress * 100).toInt()}%",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$downloadedMb MB / $totalMb MB",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when (state) {
                DownloadState.IDLE -> "Preparing download..."
                DownloadState.DOWNLOADING -> if (estimatedSeconds > 0) {
                    "Estimated time remaining: ${minutes}m ${seconds}s"
                } else {
                    "Calculating..."
                }
                DownloadState.PAUSED -> "Paused"
                DownloadState.COMPLETED -> "Download complete"
                DownloadState.FAILED -> "Download failed. Check your connection and try again."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            OutlinedButton(
                onClick = onPause,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                enabled = state == DownloadState.DOWNLOADING || state == DownloadState.PAUSED
            ) {
                Text(text = if (state == DownloadState.PAUSED) "Resume" else "Pause")
            }
            Button(
                onClick = onContinueInBackground,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (state == DownloadState.COMPLETED) "Continue"
                    else "Continue in background"
                )
            }
        }
    }
}

@Composable
private fun ReadyPage(
    isFullModel: Boolean,
    onStartScanning: () -> Unit
) {
    OnboardingPageContent {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Emerald.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Emerald,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "You\u2019re all set!",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isFullModel) "AI detection active. Welcome to ScamRadar."
            else "Lite mode active. You can enable full AI anytime from Settings.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onStartScanning,
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Start scanning",
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        content = content
    )
}
