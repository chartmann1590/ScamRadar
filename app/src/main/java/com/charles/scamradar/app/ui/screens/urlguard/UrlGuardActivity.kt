package com.charles.scamradar.app.ui.screens.urlguard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.charles.scamradar.app.classifier.ClassifierRouter
import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.data.model.Verdict
import com.charles.scamradar.app.ui.theme.ScamRadarTheme
import kotlinx.coroutines.launch

class UrlGuardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val incoming = intent?.data?.toString().orEmpty()
        setContent {
            ScamRadarTheme {
                UrlGuardBody(
                    url = incoming,
                    onOpen = {
                        val outIntent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        outIntent.selector = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
                        startActivity(outIntent)
                        finish()
                    },
                    onCancel = { finish() },
                )
            }
        }
    }
}

@Composable
private fun UrlGuardBody(
    url: String,
    onOpen: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<ScanResult?>(null) }

    LaunchedEffect(url) {
        scope.launch {
            val router = ClassifierRouter(context)
            result = runCatching { router.liteOnly().classify(url) }.getOrNull()
        }
    }

    Scaffold { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Hold on — checking that link",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val r = result
            if (r == null) {
                CircularProgressIndicator()
                Text("Running on-device check…")
            } else {
                val bg = when (r.verdict) {
                    Verdict.LIKELY_SCAM -> Color(0xFFB3261E)
                    Verdict.SUSPICIOUS -> Color(0xFFE07B00)
                    Verdict.SAFE -> Color(0xFF1E7A3A)
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = bg),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (r.verdict == Verdict.SAFE) Icons.Default.CheckCircle
                                else Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                            )
                            Spacer(Modifier.height(0.dp))
                            Text(
                                text = "  " + when (r.verdict) {
                                    Verdict.SAFE -> "Looks safe"
                                    Verdict.SUSPICIOUS -> "Suspicious"
                                    Verdict.LIKELY_SCAM -> "Likely scam"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${(r.confidence * 100).toInt()}% confidence",
                            color = Color.White.copy(alpha = 0.9f),
                        )
                        if (r.recommendedAction.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(r.recommendedAction, color = Color.White)
                        }
                    }
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = { onCancel() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Don't open") }
            Button(
                onClick = { onOpen(url) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (result?.verdict) {
                        Verdict.SAFE -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("  Open anyway in browser")
            }
        }
    }
}
