package com.charles.scamradar.app.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PremiumCard(
    entitlement: EntitlementState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1E2F8B), Color(0xFF6A1B9A))
                    )
                )
                .clickable(onClick = onClick)
                .padding(16.dp),
            color = Color.Transparent,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = when (entitlement) {
                    EntitlementState.FAMILY -> Icons.Default.Group
                    EntitlementState.FAMILY_MEMBER -> Icons.Default.Group
                    EntitlementState.PREMIUM -> Icons.Default.WorkspacePremium
                    EntitlementState.FREE -> Icons.Default.Diamond
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Text(
                        text = when (entitlement) {
                            EntitlementState.FREE -> "Go Premium"
                            EntitlementState.PREMIUM -> "ScamRadar Premium"
                            EntitlementState.FAMILY -> "ScamRadar Family"
                            EntitlementState.FAMILY_MEMBER -> "Family pod member"
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when (entitlement) {
                            EntitlementState.FREE ->
                                "Recovery PDFs, evidence export, unlimited Shield apps, no ads"
                            EntitlementState.PREMIUM ->
                                "Active — recovery PDFs, evidence locker, full Shield unlocked"
                            EntitlementState.FAMILY ->
                                "Active — your pod is covered, weekly digest delivered Mondays"
                            EntitlementState.FAMILY_MEMBER ->
                                "Active — premium unlocked by your pod organizer"
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumLockBadge(
    text: String = "Premium",
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color(0xFF6A1B9A).copy(alpha = 0.15f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = Color(0xFF6A1B9A),
                modifier = Modifier.height(14.dp).width(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6A1B9A),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
