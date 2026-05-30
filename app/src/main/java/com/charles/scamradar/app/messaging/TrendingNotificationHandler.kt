package com.charles.scamradar.app.messaging

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.charles.scamradar.app.MainActivity
import com.charles.scamradar.app.R
import android.app.NotificationChannel
import android.os.Build
import com.charles.scamradar.app.data.datastore.UserPrefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object TrendingNotificationHandler {

    fun handle(context: Context, data: Map<String, String>) {
        val enabled = runBlocking { UserPrefs(context).trendingAlertsEnabled.first() }
        if (!enabled) return

        val scamType = data["scamType"] ?: "trending"
        val title = data["title"] ?: context.getString(R.string.trending_alert_title)
        val body = data["body"] ?: "A scam is trending in your area. Tap to learn more."

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.getNotificationChannel(ScamRadarMessagingService.CHANNEL_TRENDING) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    ScamRadarMessagingService.CHANNEL_TRENDING,
                    "Trending scams",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            this.data = Uri.parse("scamradar://library/$scamType")
        }
        val pending = PendingIntent.getActivity(
            context,
            scamType.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, ScamRadarMessagingService.CHANNEL_TRENDING)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(scamType.hashCode(), notif)
    }
}
