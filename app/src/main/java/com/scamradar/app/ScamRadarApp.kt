package com.scamradar.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.scamradar.app.download.ModelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ScamRadarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = true
        MobileAds.initialize(this) {}

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { ModelManager.verifyModelOnStartup(applicationContext) }
        }
    }
}
