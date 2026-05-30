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
import com.charles.scamradar.app.family.FamilyRepository
import com.charles.scamradar.app.family.RemoteSetupPayload
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteSetupApplyScreen(
    encodedPayload: String,
    onBack: () -> Unit,
    onApplied: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val payload = remember(encodedPayload) { RemoteSetupPayload.decode(encodedPayload) }
    var status by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apply settings?") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (payload == null) {
                Text("This setup link is invalid or expired.", style = MaterialTheme.typography.bodyLarge)
                return@Column
            }

            Text(
                text = "Someone in your family wants to set up your phone.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "They will appear in your pod as \"${payload.memberLabel}\".",
                style = MaterialTheme.typography.bodyLarge,
            )
            SettingRow("Family pod code", payload.podCode)
            SettingRow("Senior Mode (big text + voice)", if (payload.seniorMode) "On" else "Off")
            SettingRow("Care Mode", if (payload.careMode) "On" else "Off")
            if (payload.emergencyContact.isNotBlank()) {
                SettingRow("Emergency contact", payload.emergencyContact)
            }

            status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        status = "Joining pod…"
                        val prefs = UserPrefs(context)
                        val repo = FamilyRepository(context)
                        when (val outcome = repo.joinPodWithPresets(payload)) {
                            is FamilyRepository.JoinOutcome.Joined -> {
                                prefs.setFamilyCode(outcome.code)
                                prefs.setFamilyMemberLabel(outcome.memberLabel ?: payload.memberLabel)
                                prefs.setSeniorMode(payload.seniorMode)
                                prefs.setCareMode(payload.careMode)
                                if (payload.emergencyContact.isNotBlank()) {
                                    prefs.setSeniorEmergencyContact(payload.emergencyContact)
                                }
                                prefs.setOnboardingComplete(true)
                                onApplied()
                            }
                            is FamilyRepository.JoinOutcome.PodFull -> status = "That family pod is full."
                            is FamilyRepository.JoinOutcome.NotFound -> status = "Family pod not found."
                            is FamilyRepository.JoinOutcome.Failed -> status = "Couldn’t apply: ${outcome.reason}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(72.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text("Apply these settings", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text("Not now")
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
