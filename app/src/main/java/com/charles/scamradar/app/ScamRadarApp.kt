package com.charles.scamradar.app

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
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
        // Mobile Ads SDK is initialized from MainActivity via ConsentManager, after UMP consent is gathered.
        installStaleGlanceTrampolineGuard()

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            runCatching { ModelManager.verifyModelOnStartup(applicationContext) }
        }
        scope.launch {
            runCatching { FcmRegistrar.bootstrap(applicationContext) }
        }
        scope.launch {
            runCatching { AchievementEngine.bootstrap(applicationContext) }
        }
        // Deferred to the next main-thread loop iteration so it doesn't add to the
        // synchronous onCreate work measured by the cold-start ANR watchdog.
        Handler(Looper.getMainLooper()).post {
            runCatching { ClipboardWatcher.register(applicationContext) }
        }
    }

    /**
     * Home-screen widgets left over from before an app update can still hold click
     * PendingIntents referencing Glance's internal action registry, which isn't preserved
     * across updates. Tapping one crashes system code (ActionTrampolineActivity /
     * InvisibleActionTrampolineActivity) with IllegalArgumentException before app code ever
     * runs. WidgetRefreshReceiver proactively refreshes widgets on update to prevent this; this
     * is a safety net for any stale click that slips through regardless.
     */
    private fun installStaleGlanceTrampolineGuard() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isStaleGlanceTrampolineCrash(throwable)) {
                runCatching { FirebaseCrashlytics.getInstance().recordException(throwable) }
                return@setDefaultUncaughtExceptionHandler
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun isStaleGlanceTrampolineCrash(throwable: Throwable): Boolean {
        var cause: Throwable? = throwable
        while (cause != null) {
            val frame = cause.stackTrace.firstOrNull()
            if (cause is IllegalArgumentException &&
                frame?.className == "androidx.glance.appwidget.action.ActionTrampolineKt"
            ) {
                return true
            }
            cause = cause.cause
        }
        return false
    }
}
