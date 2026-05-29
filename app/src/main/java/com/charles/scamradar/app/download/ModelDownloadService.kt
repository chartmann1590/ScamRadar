package com.charles.scamradar.app.download

import android.app.DownloadManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.charles.scamradar.app.BuildConfig
import com.charles.scamradar.app.R
import com.charles.scamradar.app.data.datastore.UserPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

enum class DownloadState {
    IDLE, DOWNLOADING, PAUSED, COMPLETED, FAILED
}

data class DownloadProgress(
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val state: DownloadState = DownloadState.IDLE
)

class ModelDownloadService : Service() {

    companion object {
        private const val CHANNEL_ID = "model_download"
        private const val NOTIFICATION_ID = 1001

        private val _downloadProgress = MutableStateFlow(DownloadProgress())
        val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress

        @Volatile private var currentDownloadId: Long = -1L

        fun startDownload(context: Context) {
            val intent = Intent(context, ModelDownloadService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun pauseDownload(context: Context) {
            val intent = Intent(context, ModelDownloadService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeDownload(context: Context) {
            val intent = Intent(context, ModelDownloadService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        const val ACTION_START = "com.charles.scamradar.app.ACTION_START_DOWNLOAD"
        const val ACTION_PAUSE = "com.charles.scamradar.app.ACTION_PAUSE_DOWNLOAD"
        const val ACTION_RESUME = "com.charles.scamradar.app.ACTION_RESUME_DOWNLOAD"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var pollJob: Job? = null
    private lateinit var notificationManager: NotificationManager
    private lateinit var downloadManager: DownloadManager
    private var completionReceiver: BroadcastReceiver? = null

    @Volatile private var finalizing: Boolean = false

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        createNotificationChannel()
        registerCompletionReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundAndDownload()
            ACTION_PAUSE -> pauseInternal()
            ACTION_RESUME -> resumeInternal()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        completionReceiver?.let { runCatching { unregisterReceiver(it) } }
        completionReceiver = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model Download",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows model download progress"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(progress: Int, text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading Gemma 4 Model")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
    }

    private fun startForegroundAndDownload() {
        val notification = buildNotification(0, "Preparing download…")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        serviceScope.launch {
            if (tryRecoverCompletedPart()) return@launch
            enqueueAndTrack()
        }
    }

    private suspend fun tryRecoverCompletedPart(): Boolean {
        val externalModelsDir = File(getExternalFilesDir(null), "models")
        val partFile = File(externalModelsDir, "gemma-4-E2B-it.litertlm.part")
        if (!partFile.exists()) return false
        val expected = BuildConfig.MODEL_SIZE_BYTES
        if (expected <= 0L || partFile.length() < expected) return false

        if (BuildConfig.MODEL_SHA256.isNotEmpty()) {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(100, "Verifying download…"))
            val ok = withContext(Dispatchers.IO) { verifyFileHash(partFile) }
            if (!ok) {
                partFile.delete()
                return false
            }
        }

        val internalModelsDir = File(filesDir, "models").apply { mkdirs() }
        val finalFile = File(internalModelsDir, "gemma-4-E2B-it.litertlm")
        withContext(Dispatchers.IO) {
            if (finalFile.exists()) finalFile.delete()
            partFile.inputStream().use { input ->
                finalFile.outputStream().use { output -> input.copyTo(output) }
            }
            partFile.delete()
        }

        _downloadProgress.value = DownloadProgress(
            bytesDownloaded = expected,
            totalBytes = expected,
            state = DownloadState.COMPLETED
        )
        val done = NotificationCompat.Builder(this@ModelDownloadService, CHANNEL_ID)
            .setContentTitle("Model Download Complete")
            .setContentText("Gemma 4 model is ready to use")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(false)
            .build()
        notificationManager.notify(NOTIFICATION_ID, done)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return true
    }

    private suspend fun enqueueAndTrack() {
        val prefs = UserPrefs(applicationContext)
        val wifiOnly = prefs.wifiOnlyDownload.first()

        finalizing = false
        val externalModelsDir = File(getExternalFilesDir(null), "models").apply { mkdirs() }
        val partFile = File(externalModelsDir, "gemma-4-E2B-it.litertlm.part")
        if (partFile.exists()) partFile.delete()

        val request = DownloadManager.Request(Uri.parse(BuildConfig.MODEL_DOWNLOAD_URL)).apply {
            setTitle("Gemma 4 model")
            setDescription("Downloading on-device scam detection model")
            setDestinationUri(Uri.fromFile(partFile))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            val networks = if (wifiOnly) DownloadManager.Request.NETWORK_WIFI
            else DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
            setAllowedNetworkTypes(networks)
            setAllowedOverMetered(!wifiOnly)
            setAllowedOverRoaming(false)
        }

        val id = downloadManager.enqueue(request)
        currentDownloadId = id
        _downloadProgress.value = DownloadProgress(state = DownloadState.DOWNLOADING)

        pollJob?.cancel()
        pollJob = serviceScope.launch { pollProgress(id) }
    }

    private suspend fun pollProgress(id: Long) {
        val query = DownloadManager.Query().setFilterById(id)
        while (serviceScope.isActive) {
            val cursor = downloadManager.query(query)
            if (cursor == null) {
                delay(500)
                continue
            }
            if (!cursor.moveToFirst()) {
                cursor.close()
                delay(500)
                continue
            }
            val downloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val downloaded = if (downloadedIdx >= 0) cursor.getLong(downloadedIdx) else 0L
            val total = if (totalIdx >= 0) cursor.getLong(totalIdx) else BuildConfig.MODEL_SIZE_BYTES
            val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else DownloadManager.STATUS_PENDING
            cursor.close()

            val state = when (status) {
                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> DownloadState.DOWNLOADING
                DownloadManager.STATUS_PAUSED -> DownloadState.PAUSED
                DownloadManager.STATUS_SUCCESSFUL -> DownloadState.COMPLETED
                DownloadManager.STATUS_FAILED -> DownloadState.FAILED
                else -> DownloadState.DOWNLOADING
            }
            _downloadProgress.value = DownloadProgress(
                bytesDownloaded = downloaded,
                totalBytes = if (total > 0) total else BuildConfig.MODEL_SIZE_BYTES,
                state = state
            )

            val percent = if (total > 0L) ((downloaded * 100) / total).toInt() else 0
            val mbDownloaded = downloaded / (1024.0 * 1024.0)
            val mbTotal = (if (total > 0) total else BuildConfig.MODEL_SIZE_BYTES) / (1024.0 * 1024.0)
            notificationManager.notify(
                NOTIFICATION_ID,
                buildNotification(percent, String.format("%.1f / %.1f MB", mbDownloaded, mbTotal))
            )

            if (state == DownloadState.COMPLETED) {
                serviceScope.launch { finalizeDownload(id) }
                break
            }
            if (state == DownloadState.FAILED) break
            delay(500)
        }
    }

    private fun registerCompletionReceiver() {
        if (completionReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id != currentDownloadId) return
                serviceScope.launch { finalizeDownload(id) }
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, filter)
        }
        completionReceiver = receiver
    }

    private suspend fun finalizeDownload(id: Long) {
        if (finalizing) return
        finalizing = true
        val query = DownloadManager.Query().setFilterById(id)
        val cursor = downloadManager.query(query) ?: return failDownload()
        try {
            if (!cursor.moveToFirst()) return failDownload()
            val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else -1
            if (status != DownloadManager.STATUS_SUCCESSFUL) return failDownload()
        } finally {
            cursor.close()
        }

        val externalModelsDir = File(getExternalFilesDir(null), "models")
        val partFile = File(externalModelsDir, "gemma-4-E2B-it.litertlm.part")
        val internalModelsDir = File(filesDir, "models").apply { mkdirs() }
        val finalFile = File(internalModelsDir, "gemma-4-E2B-it.litertlm")

        if (BuildConfig.MODEL_SHA256.isNotEmpty()) {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(100, "Verifying download…"))
            val ok = withContext(Dispatchers.IO) { verifyFileHash(partFile) }
            if (!ok) {
                partFile.delete()
                return failDownload()
            }
        }

        withContext(Dispatchers.IO) {
            if (finalFile.exists()) finalFile.delete()
            partFile.inputStream().use { input ->
                finalFile.outputStream().use { output -> input.copyTo(output) }
            }
            partFile.delete()
        }

        val total = _downloadProgress.value.totalBytes
        _downloadProgress.value = DownloadProgress(
            bytesDownloaded = total,
            totalBytes = total,
            state = DownloadState.COMPLETED
        )
        val done = NotificationCompat.Builder(this@ModelDownloadService, CHANNEL_ID)
            .setContentTitle("Model Download Complete")
            .setContentText("Gemma 4 model is ready to use")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(false)
            .build()
        notificationManager.notify(NOTIFICATION_ID, done)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun failDownload() {
        finalizing = false
        _downloadProgress.value = _downloadProgress.value.copy(state = DownloadState.FAILED)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun pauseInternal() {
        // DownloadManager doesn't expose programmatic pause for user-initiated downloads;
        // cancelling is the only consistent way. Treat pause as cancel + remember to re-enqueue.
        if (currentDownloadId > 0) downloadManager.remove(currentDownloadId)
        _downloadProgress.value = _downloadProgress.value.copy(state = DownloadState.PAUSED)
    }

    private fun resumeInternal() {
        startForegroundAndDownload()
    }

    private fun verifyFileHash(file: File): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n == -1) break
                digest.update(buffer, 0, n)
            }
        }
        val hashBytes = digest.digest()
        val hashString = hashBytes.joinToString("") { "%02x".format(it) }
        return hashString.equals(BuildConfig.MODEL_SHA256, ignoreCase = true)
    }
}
