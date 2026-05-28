package com.scamradar.app

import android.app.Application
import com.google.android.gms.ads.MobileAds

class ScamRadarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
    }
}
