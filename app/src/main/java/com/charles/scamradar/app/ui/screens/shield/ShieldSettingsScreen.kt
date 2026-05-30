package com.charles.scamradar.app.ui.screens.shield

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.charles.scamradar.app.data.datastore.UserPrefs
import com.charles.scamradar.app.engagement.AchievementEngine
import com.charles.scamradar.app.shield.NotificationPermissionGate
import com.charles.scamradar.app.shield.ShieldFilter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { UserPrefs(context) }

    val enabled by prefs.shieldEnabled.collectAsState(initial = false)
    val sensitivity by prefs.shieldSensitivity.collectAsState(initial = UserPrefs.SHIELD_SENSITIVITY_MEDIUM)
    val pausedUntil by prefs.shieldPausedUntil.collectAsState(initial = 0L)
    val disabled by prefs.shieldPerAppDisabled.collectAsState(initial = emptySet())
    val clipboardEnabled by prefs.clipboardChipEnabled.collectAsState(initial = false)
    var requestPostNotifications by remember { mutableStateOf(false) }

    val notifAccessGranted = remember(enabled) {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
        flat.contains("com.charles.scamradar.app/.shield.NotificationShieldService")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Shield") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.padding(end = 8.dp)) {
                            Text("Live Shield", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Reads incoming message notifications on-device. Nothing is uploaded.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(0.dp))
                        Switch(
                            checked = enabled,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    prefs.setShieldEnabled(checked)
                                    AchievementEngine.onShieldToggled(context, checked)
                                }
                                if (checked) requestPostNotifications = true
                            },
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            NotificationPermissionGate(
                triggerRequest = requestPostNotifications,
                onConsumeTrigger = { requestPostNotifications = false },
            )

            if (!notifAccessGranted) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Grant notification access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Required for ScamRadar to read incoming messages on-device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Open Android settings") }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sensitivity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    listOf(
                        UserPrefs.SHIELD_SENSITIVITY_LOW to "Low — only the strongest signals",
                        UserPrefs.SHIELD_SENSITIVITY_MEDIUM to "Medium — recommended balance",
                        UserPrefs.SHIELD_SENSITIVITY_HIGH to "High — also flag suspicious",
                    ).forEach { (value, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = sensitivity == value,
                                onClick = { scope.launch { prefs.setShieldSensitivity(value) } },
                            )
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Apps watched", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    ShieldFilter.defaultAllowed().forEach { pkg ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = friendlyName(pkg),
                                modifier = Modifier.padding(end = 8.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                            Switch(
                                checked = pkg !in disabled,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        val next = if (checked) disabled - pkg else disabled + pkg
                                        prefs.setShieldPerAppDisabled(next)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pause", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    if (pausedUntil > System.currentTimeMillis()) {
                        Text("Paused until ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(pausedUntil))}")
                        OutlinedButton(
                            onClick = { scope.launch { prefs.setShieldPausedUntil(0L) } },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Resume now") }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        prefs.setShieldPausedUntil(System.currentTimeMillis() + 3_600_000L)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("Pause 1h") }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        prefs.setShieldPausedUntil(System.currentTimeMillis() + 24L * 3_600_000L)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("Pause 24h") }
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.padding(end = 8.dp)) {
                            Text("Clipboard chip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Offer a one-tap check when you copy links or money phrases. Android shows a clipboard message each time we read it.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(0.dp))
                        Switch(
                            checked = clipboardEnabled,
                            onCheckedChange = { checked ->
                                scope.launch { prefs.setClipboardChipEnabled(checked) }
                            },
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            if (com.charles.scamradar.app.BuildConfig.DEBUG) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Developer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Debug builds only — visible because you're running app-debug.apk.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val nonce = System.currentTimeMillis()
                                    com.charles.scamradar.app.shield.ShieldRouter.classifyAndAlert(
                                        context.applicationContext,
                                        "com.charles.scamradar.test",
                                        "URGENT — Bank Alert ($nonce)",
                                        "Your account will be suspended! Verify your identity at http://scam-link.cn immediately or face legal action. Act now — final notice. ref=$nonce",
                                        null,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Fire test alert (LIKELY_SCAM)") }
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val nonce = System.currentTimeMillis()
                                    com.charles.scamradar.app.shield.ShieldRouter.classifyAndAlert(
                                        context.applicationContext,
                                        "com.charles.scamradar.test",
                                        "Hello ($nonce)",
                                        "Hi, how are you doing today? Let's grab coffee tomorrow if you're free. ref=$nonce",
                                        null,
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Fire test alert (SAFE — should NOT post)") }
                    }
                }
            }
        }
    }
}

private fun friendlyName(pkg: String): String {
    return when (pkg) {
        "com.google.android.apps.messaging" -> "Google Messages"
        "com.samsung.android.messaging" -> "Samsung Messages"
        "com.whatsapp" -> "WhatsApp"
        "org.telegram.messenger" -> "Telegram"
        "org.thoughtcrime.securesms" -> "Signal"
        "com.google.android.gm" -> "Gmail"
        "com.microsoft.office.outlook" -> "Outlook"
        "com.instagram.android" -> "Instagram"
        "com.facebook.orca" -> "Messenger"
        "com.discord" -> "Discord"
        "us.zoom.videomeetings" -> "Zoom"
        else -> pkg
    }
}
