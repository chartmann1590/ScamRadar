package com.charles.scamradar.app.ui.screens.home

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.charles.scamradar.app.data.model.ScanMode
import com.charles.scamradar.app.ocr.OcrProcessor
import com.charles.scamradar.app.speech.SpeechProcessor
import com.charles.scamradar.app.speech.TranscriptionResult
import com.charles.scamradar.app.ui.navigation.NavArgCodec
import com.charles.scamradar.app.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    scanCountToday: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var noticeMessage by remember { mutableStateOf<String?>(null) }
    var isExtractingScreenshot by remember { mutableStateOf(false) }
    var isTranscribingFile by remember { mutableStateOf(false) }
    var showVoicemailChooser by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val speechProcessor = remember { SpeechProcessor() }
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            isTranscribingFile = true
            val result = speechProcessor.transcribeAudioFile(uri, context)
            isTranscribingFile = false
            when (result) {
                is TranscriptionResult.Success -> {
                    if (result.text.isBlank()) {
                        noticeMessage = "We couldn't make out any speech in that file."
                    } else {
                        val voicePrompt = """
                            Source: voicemail audio file transcript.
                            Analyze the following voicemail transcript for scams, including family-emergency scams, voice-cloning scripts, impersonation, urgency, payment requests, and credential theft.

                            Voicemail transcript:
                            ${result.text}
                        """.trimIndent()
                        val encoded = NavArgCodec.encode(voicePrompt)
                        navController.navigate("scanning/$encoded/${ScanMode.VOICE.name}")
                    }
                }
                is TranscriptionResult.Error -> noticeMessage = result.message
                is TranscriptionResult.Unsupported -> noticeMessage = result.message
            }
        }
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            isExtractingScreenshot = true
            val extractedText = OcrProcessor().recognizeText(uri, context).trim()
            isExtractingScreenshot = false
            if (extractedText.isBlank()) {
                noticeMessage = "No readable text was found in that image."
            } else {
                val screenshotPrompt = """
                    Source: screenshot OCR.
                    Analyze the following text extracted from a screenshot. Treat OCR artifacts as possible noise, but evaluate the message for scams, phishing, impersonation, urgency, payment requests, and credential theft.

                    Extracted screenshot text:
                    $extractedText
                """.trimIndent()
                val encoded = NavArgCodec.encode(screenshotPrompt)
                navController.navigate("scanning/$encoded/${ScanMode.OCR.name}")
            }
        }
    }
    val speechRecognizer = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val transcript = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.joinToString(separator = "\n")
            ?.trim()
            .orEmpty()
        if (transcript.isBlank()) {
            noticeMessage = "No speech was captured. Try playing the voicemail near the microphone and scan again."
        } else {
            val voicePrompt = """
                Source: voicemail speech transcript.
                Analyze the following voicemail transcript for scams, including family-emergency scams, voice-cloning scripts, impersonation, urgency, payment requests, and credential theft.

                Voicemail transcript:
                $transcript
            """.trimIndent()
            val encoded = NavArgCodec.encode(voicePrompt)
            navController.navigate("scanning/$encoded/${ScanMode.VOICE.name}")
        }
    }

    if (showDialog) {
        TextInputDialog(
            onDismiss = { showDialog = false },
            onAnalyze = { message ->
                showDialog = false
                val encoded = NavArgCodec.encode(message)
                navController.navigate("scanning/$encoded/${ScanMode.TEXT.name}")
            }
        )
    }

    noticeMessage?.let { message ->
        NoticeDialog(
            message = message,
            onDismiss = { noticeMessage = null }
        )
    }

    if (showVoicemailChooser) {
        VoicemailChooserDialog(
            fileImportSupported = speechProcessor.isFileImportSupported(),
            onDismiss = { showVoicemailChooser = false },
            onRecordLive = {
                showVoicemailChooser = false
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Play the voicemail near your phone")
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                runCatching {
                    speechRecognizer.launch(intent)
                }.onFailure {
                    noticeMessage = "Speech recognition is not available on this device."
                }
            },
            onImportFile = {
                showVoicemailChooser = false
                audioPicker.launch("audio/*")
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "ScamRadar",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            RadarPulseBackground()

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF4CAF50), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Today's scan count: $scanCountToday",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { showDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF4D6FFF), Color(0xFF2D4FE0)),
                                    center = Offset(size.width / 2, size.height / 2),
                                    radius = size.maxDimension
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentPaste,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Paste & Check",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    icon = Icons.Filled.AddPhotoAlternate,
                    label = if (isExtractingScreenshot) "Reading image" else "Scan screenshot",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        imagePicker.launch("image/*")
                    }
                )
                ActionCard(
                    icon = Icons.Filled.Voicemail,
                    label = if (isTranscribingFile) "Transcribing…" else "Check voicemail",
                    modifier = Modifier.weight(1f),
                    onClick = { showVoicemailChooser = true }
                )
                ActionCard(
                    icon = Icons.Filled.Keyboard,
                    label = "Type manually",
                    modifier = Modifier.weight(1f),
                    onClick = { showDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trending scams",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = {
                        navController.navigate("library") {
                            popUpTo("scan") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                ) {
                    Text(text = "View all")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val trendingScams = listOf(
                TrendingScam(
                    scamPatternId = 2,
                    title = "Fake USPS delivery",
                    description = "Fake package tracking links sent via SMS claiming failed delivery.",
                    badgeLabel = "HIGH RISK",
                    badgeColor = Color(0xFFE53935),
                    placeholderColor = Color(0xFFFFCDD2)
                ),
                TrendingScam(
                    scamPatternId = 4,
                    title = "AI grandpa voice scam",
                    description = "AI-generated voice impersonating family members requesting money.",
                    badgeLabel = "AI VOICE",
                    badgeColor = Color(0xFFFFA000),
                    placeholderColor = Color(0xFFFFECB3)
                ),
                TrendingScam(
                    scamPatternId = 10,
                    title = "Netflix payment phishing",
                    description = "Phishing emails claiming Netflix subscription payment failure.",
                    badgeLabel = "PHISHING",
                    badgeColor = Color(0xFFE53935),
                    placeholderColor = Color(0xFFFFCDD2)
                )
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trendingScams) { scam ->
                    TrendingScamCard(
                        scam = scam,
                        onClick = {
                            navController.navigate(Screen.Library.createDetailRoute(scam.scamPatternId)) {
                                popUpTo("scan") { saveState = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun NoticeDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        },
        title = { Text("ScamRadar") },
        text = { Text(message) }
    )
}

@Composable
private fun RadarPulseBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    val scale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale1"
    )
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha1"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale2"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha2"
    )

    val scale3 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale3"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha3"
    )

    Box(
        modifier = Modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .graphicsLayer {
                    scaleX = scale1
                    scaleY = scale1
                    alpha = alpha1
                }
                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    scaleX = scale2
                    scaleY = scale2
                    alpha = alpha2
                }
                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    scaleX = scale3
                    scaleY = scale3
                    alpha = alpha3
                }
                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
        )
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class TrendingScam(
    val scamPatternId: Int,
    val title: String,
    val description: String,
    val badgeLabel: String,
    val badgeColor: Color,
    val placeholderColor: Color
)

@Composable
private fun TrendingScamCard(
    scam: TrendingScam,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .widthIn(min = 200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.width(220.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(scam.placeholderColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = scam.badgeColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
            }
            Column(modifier = Modifier.padding(14.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = scam.badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = scam.badgeLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = scam.badgeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = scam.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = scam.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TextInputDialog(
    onDismiss: () -> Unit,
    onAnalyze: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Paste or type the message",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    placeholder = { Text("Paste or type the suspicious message here...") },
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (text.isNotBlank()) onAnalyze(text.trim()) },
                        enabled = text.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Analyze")
                    }
                }
            }
        }
    }
}

@Composable
private fun VoicemailChooserDialog(
    fileImportSupported: Boolean,
    onDismiss: () -> Unit,
    onRecordLive: () -> Unit,
    onImportFile: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Check a voicemail",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pick how you want to feed the voicemail in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                VoicemailChoiceRow(
                    icon = Icons.Filled.Voicemail,
                    title = "Play near phone",
                    subtitle = "We'll listen through the mic and transcribe it on-device.",
                    onClick = onRecordLive
                )
                Spacer(modifier = Modifier.height(10.dp))
                VoicemailChoiceRow(
                    icon = Icons.Filled.AddPhotoAlternate,
                    title = if (fileImportSupported) "Import audio file" else "Import (requires Android 13+)",
                    subtitle = if (fileImportSupported)
                        "Pick an .m4a, .mp3, or .wav file from your phone."
                    else
                        "Audio file import needs Android 13 or newer.",
                    enabled = fileImportSupported,
                    onClick = onImportFile
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun VoicemailChoiceRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
