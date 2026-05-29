package com.charles.scamradar.app.ads

import com.charles.scamradar.app.BuildConfig

object AdUnits {
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"

    val banner: String
        get() = if (BuildConfig.USE_TEST_ADS) TEST_BANNER else BuildConfig.ADMOB_BANNER_ID

    val interstitial: String
        get() = if (BuildConfig.USE_TEST_ADS) TEST_INTERSTITIAL else BuildConfig.ADMOB_INTERSTITIAL_ID
}
