package com.charles.scamradar.app.shield

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.charles.scamradar.app.MainActivity
import com.charles.scamradar.app.data.model.ScanResult
import com.charles.scamradar.app.data.model.Verdict
import com.charles.scamradar.app.messaging.ScamRadarMessagingService

object ShieldAlertNotifier {

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(ScamRadarMessagingService.CHANNEL_SHIELD) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    ScamRadarMessagingService.CHANNEL_SHIELD,
                    "Live Shield alerts",
                    NotificationManager.IMPORTANCE_HIGH,
                )
            )
        }
    }

    fun postAlert(context: Context, sourcePackage: String, result: ScanResult) {
        if (result.verdict == Verdict.SAFE) return
        ensureChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val deepLink = "scamradar://shield/alert?pkg=$sourcePackage"
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.parse(deepLink)
        }
        val pending = PendingIntent.getActivity(
            context,
            sourcePackage.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (result.verdict == Verdict.LIKELY_SCAM)
            "ScamRadar caught a likely scam"
        else
            "ScamRadar flagged a suspicious message"

        val notif = NotificationCompat.Builder(context, ScamRadarMessagingService.CHANNEL_SHIELD)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText("Tap to review — your message stays on this phone.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(result.recommendedAction))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(sourcePackage.hashCode(), notif)
    }
}
