package com.charles.scamradar.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.charles.scamradar.app.ads.ConsentManager
import com.charles.scamradar.app.ads.InterstitialController
import com.charles.scamradar.app.premium.BillingClientWrapper
import com.charles.scamradar.app.premium.EntitlementRepository
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.charles.scamradar.app.data.datastore.UserPrefs
import com.charles.scamradar.app.ui.navigation.PendingDeepLink
import com.charles.scamradar.app.ui.navigation.ScamRadarNavHost
import com.charles.scamradar.app.ui.theme.ScamRadarTheme
import com.charles.scamradar.app.ui.theme.SeniorTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userPrefs = UserPrefs(applicationContext)
        val filesDir = applicationContext.filesDir

        ConsentManager.gatherConsentAndInitialize(this)
        val entitlementRepository = EntitlementRepository(applicationContext)

        // Re-verify entitlement with Play Billing on every launch so a
        // subscription canceled/expired outside the app (or in the Play
        // Store) is detected promptly instead of only when the user next
        // opens the Premium screen.
        val startupBilling = BillingClientWrapper(applicationContext, entitlementRepository)
        startupBilling.startConnection()

        CoroutineScope(Dispatchers.Main).launch {
            ConsentManager.adsReady.filter { it }.first()
            if (!entitlementRepository.entitlement.first().unlocksPremium()) {
                InterstitialController.preload(applicationContext)
            }
        }

        val initialDeepLink = extractDeepLink(intent)

        setContent {
            val careMode by userPrefs.careMode.collectAsState(initial = false)

            val content: @androidx.compose.runtime.Composable () -> Unit = {
                var pending by remember { mutableStateOf(initialDeepLink) }
                ScamRadarNavHost(
                    userPrefs = userPrefs,
                    filesDir = filesDir,
                    pendingDeepLink = pending,
                    onDeepLinkConsumed = { pending = null },
                    seniorMode = careMode,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (careMode) {
                SeniorTheme { content() }
            } else {
                ScamRadarTheme(careMode = false) { content() }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val link = extractDeepLink(intent)
        if (link != null) recreate()
    }

    private fun extractDeepLink(intent: Intent?): PendingDeepLink? {
        intent ?: return null
        if (intent.action != Intent.ACTION_VIEW) return null
        val data: Uri = intent.data ?: return null

        if (data.scheme == "scamradar") {
            return when (data.host) {
                "result" -> {
                    val payload = data.getQueryParameter("payload")?.takeIf { it.isNotBlank() }
                    payload?.let { PendingDeepLink.OpenResult(it) }
                }
                "family" -> {
                    val code = data.lastPathSegment.orEmpty().takeIf { it.isNotBlank() }
                    code?.let { PendingDeepLink.JoinFamily(it) }
                }
                "remotesetup" -> {
                    val payload = data.lastPathSegment.orEmpty().takeIf { it.isNotBlank() }
                    payload?.let { PendingDeepLink.ApplyRemoteSetup(it) }
                }
                "shield" -> {
                    val pkg = data.getQueryParameter("pkg") ?: data.lastPathSegment.orEmpty()
                    if (pkg.isBlank()) null else PendingDeepLink.OpenShieldAlert(pkg)
                }
                "library" -> {
                    val scamType = data.lastPathSegment.orEmpty().takeIf { it.isNotBlank() }
                    scamType?.let { PendingDeepLink.OpenLibraryPattern(it) }
                }
                "digest" -> PendingDeepLink.OpenDigest
                else -> null
            }
        }

        if (data.scheme == "https" && data.host == "verify.scamradar.app") {
            val payload = data.lastPathSegment.orEmpty().takeIf { it.isNotBlank() }
            return payload?.let { PendingDeepLink.OpenVerifyResult(it) }
        }

        return null
    }
}
