package com.charles.scamradar.app.ui.screens.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.charles.scamradar.app.recovery.RecoveryFlowRepository
import com.charles.scamradar.app.recovery.RecoveryProgressStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryHubScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { RecoveryProgressStore(context) }
    val repo = remember { RecoveryFlowRepository(context) }
    val incidents by store.all.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recovery hub") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        if (incidents.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(inner).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "No active recovery flows.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(incidents.sortedByDescending { it.updatedAt }) { incident ->
                    val flow = repo.flowFor(
                        runCatching { com.charles.scamradar.app.data.model.ScamType.valueOf(incident.scamType) }
                            .getOrDefault(com.charles.scamradar.app.data.model.ScamType.PHISHING)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = flow?.title ?: incident.scamType,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(8.dp))
                            val total = flow?.steps?.size ?: 0
                            val pct = if (total == 0) 0f
                            else incident.completedSteps.size.toFloat() / total
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${incident.completedSteps.size} / $total steps · updated ${formatRelative(incident.updatedAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatRelative(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    val minutes = diff / 60_000L
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 60 * 24 -> "${minutes / 60} h ago"
        else -> "${minutes / 60 / 24} d ago"
    }
}
