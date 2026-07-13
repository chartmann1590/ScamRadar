package com.charles.scamradar.app.community

import android.content.Context
import java.util.UUID

/**
 * Stable per-device identifier used in place of a Firebase Auth uid for Family
 * pod / Community Reports. Firebase Authentication requires GCP billing to be
 * enabled on this project, which is intentionally not set up — this generates
 * and persists a random id locally instead, matching the "shared secret" model
 * already used for family codes.
 */
object DeviceIdentity {
    private const val PREFS = "device_identity"
    private const val KEY_ID = "device_id"

    fun getId(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_ID, null)?.let { return it }
        val fresh = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_ID, fresh).apply()
        return fresh
    }
}
