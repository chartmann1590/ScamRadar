package com.charles.scamradar.app.ui.screens.family

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.charles.scamradar.app.data.datastore.UserPrefs
import com.charles.scamradar.app.family.QrCodeRenderer
import com.charles.scamradar.app.family.RemoteSetupPayload
import com.charles.scamradar.app.share.shareText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteSetupCreateScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { UserPrefs(context) }
    val familyCode by prefs.familyCode.collectAsState(initial = "")
    val ownLabel by prefs.familyMemberLabel.collectAsState(initial = "")

    var seniorMode by remember { mutableStateOf(true) }
    var careMode by remember { mutableStateOf(true) }
    var emergencyContact by remember { mutableStateOf("") }
    var memberLabel by remember { mutableStateOf("Mom") }

    val payload = remember(familyCode, seniorMode, careMode, emergencyContact, memberLabel) {
        if (familyCode.isBlank()) null else RemoteSetupPayload(
            podCode = familyCode,
            memberLabel = memberLabel.ifBlank { "Member" },
            seniorMode = seniorMode,
            careMode = careMode,
            emergencyContact = emergencyContact,
        )
    }

    val qrBitmap = remember(payload) {
        payload?.let { runCatching { QrCodeRenderer.encode(RemoteSetupPayload.shareUrl(it), 512) }.getOrNull() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set up someone’s phone") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (familyCode.isBlank()) {
                Text("Create or join a family pod first to share these settings.")
                return@Column
            }

            Text(
                text = "Hand this QR code to your relative. Scanning it joins them to your pod, turns on Senior Mode + Care Mode, and saves you as their emergency contact.",
                style = MaterialTheme.typography.bodyMedium,
            )

            qrBitmap?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Setup QR code",
                        modifier = Modifier.size(220.dp),
                    )
                }
            }

            OutlinedTextField(
                value = memberLabel,
                onValueChange = { memberLabel = it },
                label = { Text("Their name in your pod") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = emergencyContact,
                onValueChange = { emergencyContact = it },
                label = { Text("Your phone number for their \"Call my family\" button") },
                placeholder = { Text("+15551234567") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            ToggleRow("Senior Mode (big text + voice)", seniorMode) { seniorMode = it }
            ToggleRow("Care Mode (auto-share alerts to pod)", careMode) { careMode = it }

            Button(
                onClick = {
                    payload?.let {
                        shareText(
                            context,
                            "Set up ScamRadar for me — tap: ${RemoteSetupPayload.shareUrl(it)}"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.height(8.dp))
                Text("Send setup link instead")
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
