package com.charles.scamradar.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialController {

    private const val TAG = "InterstitialController"
    private const val COOLDOWN_MS = 30_000L

    private var loadedAd: InterstitialAd? = null
    private var isLoading: Boolean = false
    private var lastShownMs: Long = 0L

    fun preload(context: Context) {
        if (isLoading || loadedAd != null) return
        isLoading = true
        InterstitialAd.load(
            context.applicationContext,
            AdUnits.interstitial,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadedAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed to load: ${error.code} ${error.message}")
                    loadedAd = null
                    isLoading = false
                }
            }
        )
    }

    fun maybeShow(activity: Activity, onDismiss: () -> Unit) {
        val now = System.currentTimeMillis()
        val ad = loadedAd

        if (ad == null) {
            preload(activity.applicationContext)
            onDismiss()
            return
        }

        if (now - lastShownMs < COOLDOWN_MS) {
            onDismiss()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadedAd = null
                lastShownMs = System.currentTimeMillis()
                preload(activity.applicationContext)
                onDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Interstitial failed to show: ${error.code} ${error.message}")
                loadedAd = null
                preload(activity.applicationContext)
                onDismiss()
            }
        }

        ad.show(activity)
    }
}
