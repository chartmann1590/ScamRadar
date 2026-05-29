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
import com.charles.scamradar.app.ads.InterstitialController
import com.charles.scamradar.app.data.datastore.UserPrefs
import com.charles.scamradar.app.ui.navigation.PendingDeepLink
import com.charles.scamradar.app.ui.navigation.ScamRadarNavHost
import com.charles.scamradar.app.ui.theme.ScamRadarTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userPrefs = UserPrefs(applicationContext)
        val filesDir = applicationContext.filesDir
        InterstitialController.preload(applicationContext)

        val initialDeepLink = extractDeepLink(intent)

        setContent {
            val careMode by userPrefs.careMode.collectAsState(initial = false)
            ScamRadarTheme(careMode = careMode) {
                var pending by remember { mutableStateOf(initialDeepLink) }
                ScamRadarNavHost(
                    userPrefs = userPrefs,
                    filesDir = filesDir,
                    pendingDeepLink = pending,
                    onDeepLinkConsumed = { pending = null },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // The next time NavHost recomposes from a setContent-level state change it will
        // pick up the new pending deep link via state. For simplicity we restart by
        // re-setting content via recreate when a deep link arrives via NEW_INTENT.
        val link = extractDeepLink(intent)
        if (link != null) {
            recreate()
        }
    }

    private fun extractDeepLink(intent: Intent?): PendingDeepLink? {
        intent ?: return null
        if (intent.action != Intent.ACTION_VIEW) return null
        val data: Uri = intent.data ?: return null
        if (data.scheme != "scamradar") return null

        return when (data.host) {
            "result" -> {
                val payload = data.getQueryParameter("payload")?.takeIf { it.isNotBlank() }
                payload?.let { PendingDeepLink.OpenResult(it) }
            }
            "family" -> {
                val code = data.lastPathSegment.orEmpty().takeIf { it.isNotBlank() }
                code?.let { PendingDeepLink.JoinFamily(it) }
            }
            else -> null
        }
    }
}
