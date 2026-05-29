package com.charles.scamradar.app.ui.screens.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.charles.scamradar.app.analytics.Analytics
import com.charles.scamradar.app.data.model.ClassifierTier
import com.charles.scamradar.app.data.model.RedFlag
import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.data.model.Verdict
import com.charles.scamradar.app.share.buildShareText
import com.charles.scamradar.app.share.shareCardImage
import com.charles.scamradar.app.share.shareText
import kotlinx.coroutines.launch

private val Danger = Color(0xFFEF4444)
private val Warning = Color(0xFFF59E0B)
private val Safe = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    scanResult: ScanResult,
    onScanAgain: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var sharePickerOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val shareCardGraphicsLayer = rememberGraphicsLayer()
    val shareCardWidthDp = 360.dp

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Result") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { sharePickerOpen = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share result")
                    }
                    Button(
                        onClick = onScanAgain,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Scan another")
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                VerdictHero(scanResult)

                HighlightedMessageCard(scanResult)

                when (scanResult.verdict) {
                    Verdict.SAFE -> {
                        SafeReassuranceCard(scanResult)
                    }
                    Verdict.SUSPICIOUS, Verdict.LIKELY_SCAM -> {
                        if (scanResult.redFlags.isNotEmpty()) {
                            RedFlagsCard(scanResult.redFlags)
                        }
                        NumberedActionCard(scanResult)
                        if (scanResult.aiGeneratedIndicators.isNotEmpty()) {
                            SimpleListCard(
                                title = "AI-generated indicators",
                                items = scanResult.aiGeneratedIndicators
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Offscreen share card: kept in composition so the GraphicsLayer always
            // has a fresh snapshot to capture. Sized to a fixed width, lays out at its
            // natural height, then is positioned far off-screen so it never paints
            // into the visible area.
            Box(
                modifier = Modifier
                    .size(0.dp)
                    .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                    .offset(x = (-5000).dp, y = (-5000).dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(shareCardWidthDp)
                        .background(Color(0xFF0B1220))
                        .drawWithContent {
                            shareCardGraphicsLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(shareCardGraphicsLayer)
                        }
                ) {
                    ShareCard(result = scanResult)
                }
            }
        }
    }

    if (sharePickerOpen) {
        ModalBottomSheet(
            onDismissRequest = { sharePickerOpen = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Share scan result",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                ShareOptionRow(
                    icon = Icons.Default.TextFields,
                    title = "Share as text",
                    subtitle = "Plain-text summary for SMS, chat, or email"
                ) {
                    sharePickerOpen = false
                    Analytics.shareCardExported(scanResult.verdict, "text")
                    shareText(context, buildShareText(scanResult))
                }
                ShareOptionRow(
                    icon = Icons.Default.Image,
                    title = "Share as image card",
                    subtitle = "Screenshot-style card for group chats"
                ) {
                    sharePickerOpen = false
                    coroutineScope.launch {
                        val bitmap = shareCardGraphicsLayer.toImageBitmap().asAndroidBitmap()
                        Analytics.shareCardExported(scanResult.verdict, "image")
                        shareCardImage(context, bitmap)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ShareOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VerdictHero(scanResult: ScanResult) {
    val color = verdictColor(scanResult.verdict)
    val title = when (scanResult.verdict) {
        Verdict.LIKELY_SCAM -> "Likely Scam"
        Verdict.SUSPICIOUS -> "Suspicious"
        Verdict.SAFE -> "Looks Safe"
    }
    val subtitle = when (scanResult.verdict) {
        Verdict.LIKELY_SCAM -> "Strong scam indicators detected"
        Verdict.SUSPICIOUS -> "Some warning signs found"
        Verdict.SAFE -> "No scam indicators found"
    }
    val classifierLabel = when (scanResult.classifierTier) {
        ClassifierTier.GEMMA -> "Gemma AI analysis"
        ClassifierTier.LITE -> "Lite pattern analysis"
        ClassifierTier.STUB -> "Test analysis"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(140f, 100f),
                        radius = 520f
                    )
                )
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (scanResult.verdict == Verdict.SAFE) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${(scanResult.confidence * 100).toInt()}% confidence",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val typeLabel = scanResult.scamType.name.replace("_", " ")
                        val tail = if (scanResult.scamType.name in setOf("NONE", "OTHER")) "" else " · $typeLabel"
                        Text(
                            text = "$classifierLabel$tail",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SafeReassuranceCard(scanResult: ScanResult) {
    SectionCard(title = "What this means") {
        val message = scanResult.recommendedAction.ifBlank {
            "No strong scam indicators were found in this message. Stay alert — verify any unexpected requests for money, credentials, or personal info through official channels you trust."
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HighlightedMessageCard(scanResult: ScanResult) {
    if (scanResult.originalMessage.isBlank()) return
    SectionCard(title = "Scanned text") {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val highlighted = remember(scanResult.originalMessage, scanResult.redFlags) {
                buildHighlightedMessage(scanResult.originalMessage, scanResult.redFlags)
            }
            Text(
                text = highlighted,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

private fun buildHighlightedMessage(message: String, flags: List<RedFlag>): AnnotatedString {
    return buildAnnotatedString {
        append(message)
        val phrases = flags
            .map { it.phrase }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        val applied = mutableListOf<IntRange>()
        phrases.forEach { phrase ->
            val pattern = try {
                Regex(Regex.escape(phrase), RegexOption.IGNORE_CASE)
            } catch (_: Exception) {
                return@forEach
            }
            pattern.findAll(message).forEach { match ->
                val range = match.range
                val overlaps = applied.any { it.first <= range.last && it.last >= range.first }
                if (!overlaps) {
                    addStyle(
                        SpanStyle(
                            background = Danger.copy(alpha = 0.22f),
                            color = Danger,
                            fontWeight = FontWeight.SemiBold
                        ),
                        range.first,
                        range.last + 1
                    )
                    applied += range
                }
            }
        }
    }
}

@Composable
private fun NumberedActionCard(scanResult: ScanResult) {
    val rawAction = scanResult.recommendedAction.ifBlank {
        when (scanResult.verdict) {
            Verdict.SUSPICIOUS -> "Verify the sender using an official website, app, or phone number before acting. Do not click any links until you confirm. Watch for follow-up messages."
            Verdict.LIKELY_SCAM -> "Do not reply or click any links. Do not share personal or financial information. Report the message to 7726 (SPAM) and delete it."
            else -> ""
        }
    }
    val steps = rawAction
        .split(Regex("(?<=[.!?])\\s+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    SectionCard(title = "What to do") {
        if (steps.isEmpty()) return@SectionCard
        steps.forEachIndexed { index, step ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            if (index < steps.lastIndex) {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun RedFlagsCard(redFlags: List<RedFlag>) {
    SectionCard(title = "Why this was flagged") {
        val unique = redFlags.distinctBy { it.phrase.lowercase() }.take(6)
        unique.forEachIndexed { index, flag ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Danger)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = flag.phrase,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Danger
                    )
                    Text(
                        text = flag.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (index < unique.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SimpleListCard(title: String, items: List<String>) {
    SectionCard(title = title) {
        items.forEachIndexed { index, item ->
            Text(
                text = "• $item",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (index < items.lastIndex) Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

private fun verdictColor(verdict: Verdict): Color {
    return when (verdict) {
        Verdict.LIKELY_SCAM -> Danger
        Verdict.SUSPICIOUS -> Warning
        Verdict.SAFE -> Safe
    }
}
