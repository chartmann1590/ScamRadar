package com.scamradar.app.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd

object NativeAdLoader {
    private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"
    private const val TAG = "NativeAdLoader"
    val slotCache = mutableMapOf<Int, NativeAd>()

    fun preload(context: Context, slots: List<Int>) {
        slots.forEach { slot ->
            if (slotCache.containsKey(slot)) return@forEach
            val loader = AdLoader.Builder(context, AD_UNIT_ID)
                .forNativeAd { ad: NativeAd ->
                    slotCache[slot] = ad
                }
                .build()
            loader.loadAd(AdRequest.Builder().build())
        }
    }
}
