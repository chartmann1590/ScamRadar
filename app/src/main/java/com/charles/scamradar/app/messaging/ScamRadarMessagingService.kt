package com.charles.scamradar.app.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.charles.scamradar.app.MainActivity
import com.charles.scamradar.app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ScamRadarMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmRegistrar.onTokenChanged(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val type = message.data["type"] ?: "general"
        when (type) {
            "trending" -> TrendingNotificationHandler.handle(applicationContext, message.data)
            "digest" -> postDigestNotification(message.data)
            "family_alert" -> postFamilyAlertNotification(message.data)
            else -> postGenericNotification(message.notification?.title, message.notification?.body)
        }
    }

    private fun postDigestNotification(data: Map<String, String>) {
        val title = data["title"] ?: getString(R.string.family_digest_title)
        val body = data["body"] ?: getString(R.string.family_digest_subtitle)
        val deepLink = "scamradar://digest"
        showNotification(CHANNEL_DIGEST, title, body, deepLink)
    }

    private fun postFamilyAlertNotification(data: Map<String, String>) {
        val title = data["title"] ?: "Family alert"
        val body = data["body"] ?: "A pod member flagged a scam"
        val deepLink = "scamradar://family"
        showNotification(CHANNEL_FAMILY, title, body, deepLink)
    }

    private fun postGenericNotification(title: String?, body: String?) {
        showNotification(CHANNEL_GENERAL, title ?: getString(R.string.app_name), body ?: "")
    }

    private fun showNotification(
        channelId: String,
        title: String,
        body: String,
        deepLink: String? = null,
    ) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannels(manager)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (deepLink != null) data = Uri.parse(deepLink)
        }
        val pending = PendingIntent.getActivity(
            this,
            channelId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify((title + body).hashCode(), notif)
    }

    private fun ensureChannels(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        listOf(
            Triple(CHANNEL_DIGEST, "Family digest", NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CHANNEL_FAMILY, "Family alerts", NotificationManager.IMPORTANCE_HIGH),
            Triple(CHANNEL_GENERAL, "General", NotificationManager.IMPORTANCE_LOW),
            Triple(CHANNEL_TRENDING, "Trending scams", NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CHANNEL_SHIELD, "Live Shield alerts", NotificationManager.IMPORTANCE_HIGH),
        ).forEach { (id, name, importance) ->
            if (manager.getNotificationChannel(id) == null) {
                manager.createNotificationChannel(NotificationChannel(id, name, importance))
            }
        }
    }

    companion object {
        const val CHANNEL_DIGEST = "channel_digest"
        const val CHANNEL_FAMILY = "channel_family"
        const val CHANNEL_GENERAL = "channel_general"
        const val CHANNEL_TRENDING = "channel_trending"
        const val CHANNEL_SHIELD = "channel_shield"
    }
}
