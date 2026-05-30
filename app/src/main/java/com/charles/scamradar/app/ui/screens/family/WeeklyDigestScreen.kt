package com.charles.scamradar.app.ui.screens.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.charles.scamradar.app.data.datastore.UserPrefs
import com.charles.scamradar.app.family.DigestRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyDigestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { UserPrefs(context) }
    val podCode by prefs.familyCode.collectAsState(initial = "")
    val repo = remember { DigestRepository(context) }
    val digest by repo.observeLatest(podCode).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Family Weekly Digest") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (podCode.isBlank()) {
                Text("Join a family pod first to see weekly digests.")
                return@Column
            }
            val d = digest
            if (d == null) {
                Text("Your first digest will land Monday at 9am local time.")
                return@Column
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Week of ${d.weekStarting}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatTile("Shares", d.totalShares.toString(), Modifier.weight(1f))
                        StatTile("Likely scams", d.likelyScams.toString(), Modifier.weight(1f))
                        StatTile("Suspicious", d.suspicious.toString(), Modifier.weight(1f))
                    }
                    if (d.topScamTypes.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Top scam types: ${d.topScamTypes.joinToString(", ")}")
                    }
                }
            }
            if (d.perMember.isNotEmpty()) {
                Text("By pod member", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                d.perMember.forEach { m ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Text(m.memberLabel, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Text("${m.scansThisWeek} scans · ${m.scamsCaught} caught")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
