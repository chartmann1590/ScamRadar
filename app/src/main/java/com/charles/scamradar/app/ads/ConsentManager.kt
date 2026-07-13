package com.charles.scamradar.app.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Gathers UMP (Google-certified EEA/UK/CA) ad consent before the Mobile Ads SDK is
 * initialized or any ad unit requests an ad, per AdMob's consent requirements.
 */
object ConsentManager {
    private const val TAG = "ConsentManager"

    private val _adsReady = MutableStateFlow(false)
    val adsReady: StateFlow<Boolean> = _adsReady

    fun gatherConsentAndInitialize(activity: Activity) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.errorCode} ${formError.message}")
                    }
                    initializeIfReady(activity, consentInformation.canRequestAds())
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info update failed: ${requestError.errorCode} ${requestError.message}")
                initializeIfReady(activity, consentInformation.canRequestAds())
            }
        )

        if (consentInformation.canRequestAds()) {
            initializeIfReady(activity, true)
        }
    }

    private fun initializeIfReady(activity: Activity, canRequestAds: Boolean) {
        if (!canRequestAds || _adsReady.value) return
        MobileAds.initialize(activity.applicationContext) {}
        _adsReady.value = true
    }
}
