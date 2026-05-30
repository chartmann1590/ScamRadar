package com.charles.scamradar.app.ui.senior

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.charles.scamradar.app.data.db.AppDatabase
import com.charles.scamradar.app.data.db.ScanHistoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeniorHistoryScreen(
    onBack: () -> Unit,
    onOpenResult: (ScanHistoryEntity) -> Unit,
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val all by db.scanHistoryDao().getAll().collectAsState(initial = emptyList())
    val entries = all
        .filter { it.verdict == "LIKELY_SCAM" || it.verdict == "SUSPICIOUS" }
        .sortedByDescending { it.timestamp }
        .take(40)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Past scans", fontSize = 26.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { inner ->
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(inner).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("No flagged scans yet.", fontSize = 24.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(entries) { entry ->
                    HistoryRow(entry, onClick = { onOpenResult(entry) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryRow(entry: ScanHistoryEntity, onClick: () -> Unit) {
    val color = when (entry.verdict) {
        "LIKELY_SCAM" -> Color(0xFFB3261E)
        "SUSPICIOUS" -> Color(0xFFE07B00)
        else -> Color(0xFF1E7A3A)
    }
    val label = when (entry.verdict) {
        "LIKELY_SCAM" -> "SCAM"
        "SUSPICIOUS" -> "WARNING"
        else -> "SAFE"
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = label, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                text = entry.originalMessage.take(140),
                fontSize = 20.sp,
                color = Color.White,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Tap to read more",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}
