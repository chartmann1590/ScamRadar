package com.charles.scamradar.app.shield

import android.content.Context
import com.charles.scamradar.app.data.datastore.UserPrefs
import kotlinx.coroutines.flow.first

object ShieldFilter {

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
        val prefs = UserPrefs(context)
        if (!prefs.shieldEnabled.first()) return false
        val pausedUntil = prefs.shieldPausedUntil.first()
        if (pausedUntil > System.currentTimeMillis()) return false
        val disabled = prefs.shieldPerAppDisabled.first()
        if (packageName in disabled) return false
        return packageName in DEFAULT_ALLOWED
    }

    fun defaultAllowed(): Set<String> = DEFAULT_ALLOWED
}
