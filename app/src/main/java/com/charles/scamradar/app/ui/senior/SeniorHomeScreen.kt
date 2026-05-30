package com.charles.scamradar.app.ui.senior

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charles.scamradar.app.data.datastore.UserPrefs
import com.charles.scamradar.app.ocr.OcrProcessor
import kotlinx.coroutines.launch

@Composable
fun SeniorHomeScreen(
    onStartScan: (String) -> Unit,
    onScanScreenshot: (String) -> Unit,
    onOpenUrlScan: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { UserPrefs(context) }
    val emergencyContact by prefs.seniorEmergencyContact.collectAsState(initial = "")
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    var showPasteDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    var isExtractingScreenshot by remember { mutableStateOf(false) }
    var ocrError by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isExtractingScreenshot = true
            val extracted = OcrProcessor().recognizeText(uri, context).trim()
            isExtractingScreenshot = false
            if (extracted.isBlank()) {
                ocrError = "We couldn't find any text in that picture. Try a clearer screenshot."
            } else {
                onScanScreenshot(extracted)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = "ScamRadar",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Text(
            text = "Got a strange message?",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 4.dp),
        )

        BigPrimary(
            text = "Paste & check",
            icon = Icons.Default.ContentPaste,
            onClick = {
                val text = clipboardManager.getText()?.text.orEmpty().trim()
                if (text.isNotBlank()) {
                    onStartScan(text)
                } else {
                    pasteText = ""
                    showPasteDialog = true
                }
            },
        )

        BigPrimary(
            text = "Check a website",
            icon = Icons.Default.Language,
            onClick = onOpenUrlScan,
        )

        BigPrimary(
            text = "Check a screenshot",
            icon = Icons.Default.Image,
            onClick = { imagePicker.launch("image/*") },
            loading = isExtractingScreenshot,
        )

        if (emergencyContact.isNotBlank()) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$emergencyContact"))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(34.dp))
                Spacer(Modifier.size(14.dp))
                Text("Call my family", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(4.dp))

        OutlinedButton(
            onClick = onOpenHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(26.dp))
            Spacer(Modifier.size(10.dp))
            Text("Past scans", fontSize = 20.sp)
        }

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = { showExitDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Exit Care Mode",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text("Check a message", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Nothing was copied yet. Type or paste the message you want to check below.",
                        fontSize = 18.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        placeholder = { Text("Paste the message here", fontSize = 18.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 20.sp),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = pasteText.trim()
                        if (trimmed.isNotBlank()) {
                            showPasteDialog = false
                            onStartScan(trimmed)
                        }
                    },
                    enabled = pasteText.trim().isNotBlank(),
                ) { Text("Check it", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) {
                    Text("Cancel", fontSize = 20.sp)
                }
            },
        )
    }

    ocrError?.let { msg ->
        AlertDialog(
            onDismissRequest = { ocrError = null },
            title = { Text("Couldn't read that picture", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
            text = { Text(msg, fontSize = 18.sp) },
            confirmButton = {
                Button(onClick = { ocrError = null }) {
                    Text("OK", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit Care Mode?", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Going back to the normal app. Larger text and spoken verdicts will turn off.",
                    fontSize = 18.sp,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            prefs.setCareMode(false)
                            prefs.setSeniorMode(false)
                        }
                        showExitDialog = false
                    },
                ) { Text("Exit", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Stay in Care Mode", fontSize = 20.sp)
                }
            },
        )
    }
}

@Composable
private fun BigPrimary(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.size(14.dp))
            Text("Reading…", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        } else {
            Icon(icon, contentDescription = null, modifier = Modifier.size(34.dp))
            Spacer(Modifier.size(14.dp))
            Text(text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}
