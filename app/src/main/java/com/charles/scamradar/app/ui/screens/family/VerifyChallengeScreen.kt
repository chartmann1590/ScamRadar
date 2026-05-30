package com.charles.scamradar.app.ui.screens.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.charles.scamradar.app.data.datastore.UserPrefs
import com.charles.scamradar.app.family.VerifyChallengeService
import com.charles.scamradar.app.share.shareText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyChallengeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { UserPrefs(context) }
    val podCode by prefs.familyCode.collectAsState(initial = "")
    val memberLabel by prefs.familyMemberLabel.collectAsState(initial = "")
    var lastUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(podCode, memberLabel) {
        if (podCode.isNotBlank()) {
            VerifyChallengeService.ensureRegistered(podCode, memberLabel.ifBlank { "Member" })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("It's really me") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (podCode.isBlank()) {
                Text("Join a family pod first to send a verified message.")
                return@Column
            }

            Text(
                text = "Send a signed link",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "If someone is worried a call or message claiming to be you is fake (voice clones are now common), send them this link. When they tap it, ScamRadar verifies you cryptographically with your pod.",
                style = MaterialTheme.typography.bodyLarge,
            )

            Button(
                onClick = {
                    scope.launch {
                        val signed = VerifyChallengeService.sign(podCode, memberLabel.ifBlank { "Member" })
                            ?: return@launch
                        val url = VerifyChallengeService.shareUrl(signed)
                        lastUrl = url
                        shareText(context, "It's really me — tap to verify: $url")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(72.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("  Send a verified link", style = MaterialTheme.typography.titleMedium)
            }

            lastUrl?.let { url ->
                Text(
                    text = "Last signed link (valid 72h):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(url, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(
                    onClick = { shareText(context, "It's really me — tap to verify: $url") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) { Text("Share again") }
            }
        }
    }
}
