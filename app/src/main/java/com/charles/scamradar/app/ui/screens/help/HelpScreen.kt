package com.charles.scamradar.app.ui.screens.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Diversity1
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onReplayTutorial: () -> Unit,
    onOpenFamily: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HelpIntroCard(
                onReplayTutorial = onReplayTutorial,
                onOpenFamily = onOpenFamily
            )

            HelpTopicCard(
                icon = Icons.Default.Security,
                title = "Scanning messages",
                body = "Paste suspicious text, scan a screenshot, check a URL, or share text/images into ScamRadar from Android's share sheet. ScamRadar saves the result to local history and shows a verdict, confidence, warning signs, and what to do next."
            )

            HelpTopicCard(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy",
                body = "Text scanning runs on your device. Community reports and family shares only upload short sanitized excerpts after you choose to report or share. The app does not read your SMS inbox automatically."
            )

            HelpTopicCard(
                icon = Icons.Default.Groups,
                title = "Family pods",
                body = "Create a family pod from Settings, then share the family code, QR code, or scamradar://family link with relatives. Up to 8 people can join. A shared family scan appears in the private family activity feed."
            )

            HelpTopicCard(
                icon = Icons.Default.Share,
                title = "Sharing with family",
                body = "When you are in a family pod, scam results show Share with family. The app strips obvious private data and writes a short alert to the family's Firestore feed. Care Mode can auto-share likely scams, or likely plus suspicious results if you enable that setting."
            )

            HelpTopicCard(
                icon = Icons.Default.Report,
                title = "Community reports",
                body = "Report anonymously sends likely scam results to the community report collection. The app removes links, emails, phone numbers, and long numbers first. On the free Firebase Spark plan, reports are stored but scheduled trending aggregation is disabled."
            )

            HelpTopicCard(
                icon = Icons.Default.Diversity1,
                title = "Care Mode",
                body = "Care Mode makes text larger, simplifies scam result actions, hides ads, and can auto-share scam alerts with family. It is meant for relatives who need fewer steps and clearer warnings."
            )

            HelpTopicCard(
                icon = Icons.Default.Settings,
                title = "Model and settings",
                body = "Lite mode works without a large download. Full AI mode downloads the on-device model when available. Settings also lets you manage family, Care Mode, download behavior, stats, privacy links, and this help section."
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HelpIntroCard(
    onReplayTutorial: () -> Unit,
    onOpenFamily: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "How ScamRadar works",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            Text(
                text = "Use this guide anytime to understand scanning, family protection, community reporting, privacy, Care Mode, and model setup.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onReplayTutorial,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.School, contentDescription = null)
                Text("Replay onboarding tutorial", modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedButton(
                onClick = onOpenFamily,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Groups, contentDescription = null)
                Text("Open family setup", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun HelpTopicCard(
    icon: ImageVector,
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
            Column(
                modifier = Modifier.padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
