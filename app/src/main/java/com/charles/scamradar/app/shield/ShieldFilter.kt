package com.charles.scamradar.app.shield

import android.content.Context
import com.charles.scamradar.app.data.datastore.UserPrefs
import com.charles.scamradar.app.premium.EntitlementRepository
import kotlinx.coroutines.flow.first

object ShieldFilter {

    const val FREE_TIER_APP_LIMIT = 3

    private val DEFAULT_ALLOWED = setOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.whatsapp",
        "org.telegram.messenger",
        "org.thoughtcrime.securesms", // Signal
        "com.google.android.gm",
        "com.microsoft.office.outlook",
        "com.instagram.android",
        "com.facebook.orca",
        "com.discord",
        "us.zoom.videomeetings",
    )

    suspend fun shouldScan(context: Context, packageName: String): Boolean {
        if (packageName !in DEFAULT_ALLOWED) return false
        val prefs = UserPrefs(context)
        if (!prefs.shieldEnabled.first()) return false
        val pausedUntil = prefs.shieldPausedUntil.first()
        if (pausedUntil > System.currentTimeMillis()) return false
        val disabled = prefs.shieldPerAppDisabled.first()
        if (packageName in disabled) return false

        val entitlement = EntitlementRepository(context).entitlement.first()
        if (entitlement.unlocksPremium()) return true

        // Free tier: only the first FREE_TIER_APP_LIMIT enabled apps (in default order) are watched.
        val enabledInOrder = DEFAULT_ALLOWED.filterNot { it in disabled }
        return enabledInOrder.take(FREE_TIER_APP_LIMIT).contains(packageName)
    }

    fun defaultAllowed(): Set<String> = DEFAULT_ALLOWED
}
