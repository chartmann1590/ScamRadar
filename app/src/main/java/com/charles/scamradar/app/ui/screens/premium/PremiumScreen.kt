package com.charles.scamradar.app.ui.screens.premium

import android.app.Activity
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.ProductDetails
import com.charles.scamradar.app.premium.BillingClientWrapper
import com.charles.scamradar.app.premium.EntitlementRepository
import com.charles.scamradar.app.premium.EntitlementSku
import com.charles.scamradar.app.premium.EntitlementState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val entitlementRepository = remember { EntitlementRepository(context) }
    val billing = remember { BillingClientWrapper(context, entitlementRepository) }
    val entitlement by entitlementRepository.entitlement.collectAsState(initial = EntitlementState.FREE)
    val products by billing.products.collectAsState()

    DisposableEffect(Unit) {
        billing.startConnection()
        onDispose { billing.close() }
    }
    LaunchedEffect(Unit) { billing.refreshEntitlement() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ScamRadar Premium") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            if (entitlement.unlocksPremium()) {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(0.dp))
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                "Premium active",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                when (entitlement) {
                                    EntitlementState.FAMILY -> "Family pod tier — organizer subscription"
                                    EntitlementState.FAMILY_MEMBER -> "Premium via your family pod"
                                    EntitlementState.PREMIUM -> "Individual subscription"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "What's included",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))

            BenefitRow("Incident Report PDF after every recovery flow")
            BenefitRow("Evidence Locker — exportable ZIP for bank disputes")
            BenefitRow("Live Shield — unlimited per-app coverage")
            BenefitRow("Ad-free across all screens")
            BenefitRow("Family pod tier covers up to 8 family members")

            Spacer(Modifier.height(24.dp))

            Text("Choose a plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            products.forEach { product ->
                PlanCard(
                    product = product,
                    onSubscribe = {
                        val activity = context as? Activity ?: return@PlanCard
                        billing.launchPurchase(activity, product)
                    },
                )
                Spacer(Modifier.height(8.dp))
            }

            if (products.isEmpty()) {
                Text(
                    text = "Plans loading… If this persists, check Play Store sign-in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Subscriptions auto-renew until cancelled in Google Play. Manage anytime in Play Store > Subscriptions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(0.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun PlanCard(product: ProductDetails, onSubscribe: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            val isFamily = product.productId == EntitlementSku.FAMILY_MONTHLY ||
                product.productId == EntitlementSku.FAMILY_YEARLY
            Text(
                text = if (isFamily) "Family" else "Premium",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            val price = product.subscriptionOfferDetails
                ?.firstOrNull()
                ?.pricingPhases
                ?.pricingPhaseList
                ?.firstOrNull()
                ?.formattedPrice
                ?: "—"
            Text(
                text = price,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onSubscribe, modifier = Modifier.fillMaxWidth()) {
                Text("Subscribe")
            }
        }
    }
}
