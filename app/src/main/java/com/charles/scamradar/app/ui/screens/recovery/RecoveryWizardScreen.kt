package com.charles.scamradar.app.ui.screens.recovery

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.engagement.AchievementEngine
import com.charles.scamradar.app.premium.EntitlementRepository
import com.charles.scamradar.app.premium.EntitlementState
import com.charles.scamradar.app.premium.PremiumLockBadge
import com.charles.scamradar.app.recovery.IncidentReportBuilder
import com.charles.scamradar.app.recovery.RecoveryFlowRepository
import com.charles.scamradar.app.recovery.RecoveryProgress
import com.charles.scamradar.app.recovery.RecoveryProgressStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryWizardScreen(
    scanResult: ScanResult,
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    onOpenAuthorities: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { RecoveryFlowRepository(context) }
    val progressStore = remember { RecoveryProgressStore(context) }
    val entitlementRepo = remember { EntitlementRepository(context) }
    val flow = remember(scanResult.scamType) { repo.flowFor(scanResult.scamType) }
    val scanId = remember(scanResult) { "${scanResult.timestamp}_${scanResult.originalMessage.hashCode()}" }
    val saved = remember { progressStore.forScan(scanId) }
    var completed by remember { mutableStateOf(saved?.completedSteps ?: emptySet()) }
    var notes by remember { mutableStateOf(saved?.notes ?: "") }
    val entitlement by entitlementRepo.entitlement.collectAsState(initial = EntitlementState.FREE)

    if (flow == null) {
        Scaffold(topBar = { TopAppBar(title = { Text("Recovery") }) }) { inner ->
            Box(modifier = Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Text("No recovery checklist available for this type.")
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(flow.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(flow.lead, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(12.dp))
                        val pct = if (flow.steps.isEmpty()) 0f
                        else completed.size.toFloat() / flow.steps.size
                        LinearProgressIndicator(
                            progress = { pct },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${completed.size} / ${flow.steps.size} steps complete",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(flow.steps) { step ->
                val isDone = step.id in completed
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Checkbox(
                            checked = isDone,
                            onCheckedChange = { checked ->
                                val next = if (checked) completed + step.id else completed - step.id
                                completed = next
                                progressStore.upsert(
                                    RecoveryProgress(
                                        scanId = scanId,
                                        scamType = scanResult.scamType.name,
                                        completedSteps = next,
                                        notes = notes,
                                        updatedAt = System.currentTimeMillis(),
                                    )
                                )
                                if (next.size == flow.steps.size) {
                                    AchievementEngine.onRecoveryFlowCompleted(context)
                                }
                            },
                        )
                        Spacer(Modifier.size(8.dp))
                        Column {
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = step.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = {
                                notes = it
                                progressStore.upsert(
                                    RecoveryProgress(
                                        scanId = scanId,
                                        scamType = scanResult.scamType.name,
                                        completedSteps = completed,
                                        notes = it,
                                        updatedAt = System.currentTimeMillis(),
                                    )
                                )
                            },
                            placeholder = { Text("What happened, who you contacted, any reference numbers…") },
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                        )
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onOpenAuthorities,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Open Authority Reporting Hub")
                }
            }

            item {
                if (entitlement.unlocksPremium()) {
                    Button(
                        onClick = {
                            val builder = IncidentReportBuilder(context)
                            val file = builder.buildPdf(
                                IncidentReportBuilder.BuildInput(
                                    scanResult = scanResult,
                                    flow = flow,
                                    completedStepIds = completed,
                                    notes = notes,
                                )
                            )
                            context.startActivity(
                                Intent.createChooser(builder.shareIntent(file), "Share incident PDF")
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Export incident PDF")
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        onClick = onUpgrade,
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.padding(end = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Export incident PDF",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    PremiumLockBadge()
                                }
                                Text(
                                    "Bring this to your bank or police report. Unlocks with Premium.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
