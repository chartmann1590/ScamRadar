package com.charles.scamradar.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.charles.scamradar.app.community.AnonymousAuthBootstrapper
import com.charles.scamradar.app.download.ModelManager
import com.charles.scamradar.app.engagement.AchievementEngine
import com.charles.scamradar.app.messaging.FcmRegistrar
import com.charles.scamradar.app.shield.ClipboardWatcher
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

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            runCatching { ModelManager.verifyModelOnStartup(applicationContext) }
        }
        scope.launch {
            runCatching { AnonymousAuthBootstrapper.ensureSignedIn() }
        }
        scope.launch {
            runCatching { FcmRegistrar.bootstrap(applicationContext) }
        }
        scope.launch {
            runCatching { AchievementEngine.bootstrap(applicationContext) }
        }
        runCatching { ClipboardWatcher.register(applicationContext) }
    }
}
