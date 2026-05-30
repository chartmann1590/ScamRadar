package com.charles.scamradar.app.messaging

import android.content.Context
import com.charles.scamradar.app.data.datastore.UserPrefs
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

object FcmRegistrar {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun bootstrap(context: Context) {
        scope.launch {
            val prefs = UserPrefs(context.applicationContext)
            val alreadyRegistered = prefs.fcmTokenRegistered.first()
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onTokenChanged(context, task.result)
                }
            }
            if (alreadyRegistered) return@launch
            val country = resolveCountry(context, prefs)
            subscribeTrendingTopic(country)
            prefs.setFcmTokenRegistered(true)
        }
    }

    fun onTokenChanged(context: Context, token: String) {
        scope.launch {
            val prefs = UserPrefs(context.applicationContext)
            val country = resolveCountry(context, prefs)
            subscribeTrendingTopic(country)
        }
    }

    fun subscribeFamilyDigest(podCode: String) {
        if (podCode.isBlank()) return
        FirebaseMessaging.getInstance().subscribeToTopic("pod_${podCode.lowercase()}_digest")
    }

    fun unsubscribeFamilyDigest(podCode: String) {
        if (podCode.isBlank()) return
        FirebaseMessaging.getInstance().unsubscribeFromTopic("pod_${podCode.lowercase()}_digest")
    }

    private fun subscribeTrendingTopic(country: String) {
        val safe = country.takeIf { it.length == 2 }?.uppercase() ?: "US"
        FirebaseMessaging.getInstance().subscribeToTopic("trending_$safe")
    }

    private suspend fun resolveCountry(context: Context, prefs: UserPrefs): String {
        val override = prefs.regionOverride.first()
        if (override.length == 2) return override.uppercase()
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE)
            as? android.telephony.TelephonyManager
        val sim = telephony?.simCountryIso?.uppercase()
        if (!sim.isNullOrBlank() && sim.length == 2) return sim
        return Locale.getDefault().country.takeIf { it.length == 2 } ?: "US"
    }
}
