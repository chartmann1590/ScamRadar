package com.charles.scamradar.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * After an app update, home-screen widgets still show RemoteViews from the previous install
 * whose click PendingIntents point at Glance's internal action registry, which is not preserved
 * across updates. Tapping one of those stale widgets crashes ActionTrampolineActivity with
 * IllegalArgumentException ("List adapter activity trampoline invoked without specifying target
 * intent."). Forcing an update here replaces the stale RemoteViews before the user can tap them.
 */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { ScamRadarWidget.refreshAll(appContext) }
            runCatching { TodayWidget.refreshAll(appContext) }
            pendingResult.finish()
        }
    }
}
