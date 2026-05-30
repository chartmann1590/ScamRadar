package com.charles.scamradar.app.shield

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.charles.scamradar.app.R
import com.charles.scamradar.app.ui.quickverdict.QuickVerdictActivity
import com.charles.scamradar.app.messaging.ScamRadarMessagingService

object ClipboardHeadsUp {

    private const val CHANNEL = "channel_clipboard"

    fun post(context: Context, copiedText: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val intent = Intent(context, QuickVerdictActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, copiedText.take(2000))
        }
        val pending = PendingIntent.getActivity(
            context,
            copiedText.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(context.getString(R.string.clipboard_chip_action))
            .setContentText("Tap to check what you just copied")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        manager.notify(CHANNEL.hashCode(), notif)
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (manager.getNotificationChannel(CHANNEL) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Clipboard chip", NotificationManager.IMPORTANCE_LOW)
        )
    }
}
