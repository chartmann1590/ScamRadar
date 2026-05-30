package com.charles.scamradar.app.ui.screens.recovery

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.charles.scamradar.app.data.model.ScamType
import com.charles.scamradar.app.recovery.Authority
import com.charles.scamradar.app.recovery.AuthorityAction
import com.charles.scamradar.app.recovery.AuthorityDirectory
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorityReportScreen(
    scamTypeName: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { UserPrefs(context) }
    val regionOverride by prefs.regionOverride.collectAsState(initial = "")
    val country = if (regionOverride.length == 2) regionOverride else Locale.getDefault().country
    val scamType = runCatching { ScamType.valueOf(scamTypeName) }.getOrDefault(ScamType.OTHER)
    val list = remember(scamType, country) {
        AuthorityDirectory.authoritiesFor(country, scamType)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report to authorities") },
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
                        Text(
                            text = "ScamRadar prepared these for you — review before sending.",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Region: $country. Change in Settings → Region.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            items(list) { auth ->
                AuthorityRow(auth) {
                    handleAction(context, auth.action)
                }
            }

            if (list.isEmpty()) {
                item { Text("No prefilled authorities for this region yet.") }
            }
        }
    }
}

@Composable
private fun AuthorityRow(authority: Authority, onTap: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                authority.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                authority.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onTap,
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Default.Launch, contentDescription = null)
                Spacer(Modifier.height(6.dp))
                Text(
                    when (authority.action) {
                        is AuthorityAction.Url -> "Open form"
                        is AuthorityAction.SmsCompose -> "Compose SMS"
                        is AuthorityAction.Dial -> "Dial"
                    }
                )
            }
        }
    }
}

private fun handleAction(context: android.content.Context, action: AuthorityAction) {
    val intent = when (action) {
        is AuthorityAction.Url -> Intent(Intent.ACTION_VIEW, Uri.parse(action.url))
        is AuthorityAction.SmsCompose -> Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:${action.number}")
            putExtra("sms_body", action.body)
        }
        is AuthorityAction.Dial -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:${action.number}"))
    }
    runCatching { context.startActivity(intent) }
}
