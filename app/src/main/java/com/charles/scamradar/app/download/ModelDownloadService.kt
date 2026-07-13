package com.charles.scamradar.app.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.charles.scamradar.app.BuildConfig
import com.charles.scamradar.app.R
import com.charles.scamradar.app.data.datastore.UserPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

enum class DownloadState {
    IDLE, DOWNLOADING, PAUSED, COMPLETED, FAILED
}

data class DownloadProgress(
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val state: DownloadState = DownloadState.IDLE
)

/**
 * Downloads the Gemma model directly (instead of delegating to the system
 * DownloadManager) because on this OS the foreground-service grant for a
 * dataSync service gets force-stopped by AMS ("Stop FGS timeout") after
 * roughly 2.5 minutes regardless of declared type, and DownloadManager's own
 * transfer + destination file were observed to die along with it — losing
 * a multi-GB download outright rather than just losing progress tracking.
 *
 * This service instead: (1) writes bytes itself via a resumable Range
 * request so a restart can always continue from the last flushed offset, and
 * (2) proactively re-issues startForegroundService() every ~100s — well
 * inside the observed ~150s kill window — so a fresh "generation" is always
 * running before AMS's timer would otherwise fire.
 */
class ModelDownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "model_download"
        const val NOTIFICATION_ID = 1001
        private const val RENEW_INTERVAL_MS = 100_000L

        private val _downloadProgress = MutableStateFlow(DownloadProgress())
        val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress

        @Volatile private var downloadJob: Job? = null
        @Volatile private var cancelRequested: Boolean = false

        fun startDownload(context: Context) {
            cancelRequested = false
            val intent = Intent(context, ModelDownloadService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun pauseDownload(context: Context) {
            cancelRequested = true
            _downloadProgress.value = _downloadProgress.value.copy(state = DownloadState.PAUSED)
        }

        fun resumeDownload(context: Context) = startDownload(context)

        const val ACTION_START = "com.charles.scamradar.app.ACTION_START_DOWNLOAD"
        const val ACTION_PAUSE = "com.charles.scamradar.app.ACTION_PAUSE_DOWNLOAD"
        const val ACTION_RESUME = "com.charles.scamradar.app.ACTION_RESUME_DOWNLOAD"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var renewJob: Job? = null
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundAndDownload()
            ACTION_PAUSE -> pauseDownload(applicationContext)
            ACTION_RESUME -> startForegroundAndDownload()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        renewJob?.cancel()
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
        cancelRequested = false
        val current = _downloadProgress.value
        val startPercent = if (current.totalBytes > 0) ((current.bytesDownloaded * 100) / current.totalBytes).toInt() else 0
        val notification = buildNotification(startPercent, "Preparing download…")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        scheduleRenewal()
        if (downloadJob?.isActive != true) {
            downloadJob = serviceScope.launch { runDownload() }
        }
    }

    private fun scheduleRenewal() {
        renewJob?.cancel()
        renewJob = serviceScope.launch {
            delay(RENEW_INTERVAL_MS)
            if (!isActive) return@launch
            val state = _downloadProgress.value.state
            if (state == DownloadState.DOWNLOADING) {
                startDownload(applicationContext)
            }
        }
    }

    private fun hasUsableNetwork(wifiOnly: Boolean): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false
        if (wifiOnly) return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        return true
    }

    private suspend fun runDownload() = withContext(Dispatchers.IO) {
        val prefs = UserPrefs(applicationContext)
        val wifiOnly = prefs.wifiOnlyDownload.first()

        val externalModelsDir = File(getExternalFilesDir(null), "models").apply { mkdirs() }
        val partFile = File(externalModelsDir, "gemma-4-E2B-it.litertlm.part")
        val expectedSize = BuildConfig.MODEL_SIZE_BYTES

        if (partFile.exists() && expectedSize > 0L && partFile.length() >= expectedSize) {
            finalize(partFile)
            return@withContext
        }

        if (!hasUsableNetwork(wifiOnly)) {
            _downloadProgress.value = _downloadProgress.value.copy(state = DownloadState.PAUSED)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return@withContext
        }

        var existingBytes = if (partFile.exists()) partFile.length() else 0L
        _downloadProgress.value = DownloadProgress(existingBytes, if (expectedSize > 0) expectedSize else existingBytes, DownloadState.DOWNLOADING)

        val result = runCatching {
            val connection = URL(BuildConfig.MODEL_DOWNLOAD_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            if (existingBytes > 0) connection.setRequestProperty("Range", "bytes=$existingBytes-")
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                connection.disconnect()
                error("HTTP $responseCode")
            }
            if (responseCode == HttpURLConnection.HTTP_OK && existingBytes > 0) {
                // Server doesn't honor Range; restart from scratch.
                partFile.delete()
                existingBytes = 0L
            }

            val totalSize = if (expectedSize > 0) expectedSize
                else existingBytes + connection.contentLengthLong.coerceAtLeast(0)

            RandomAccessFile(partFile, "rw").use { raf ->
                raf.seek(existingBytes)
                connection.inputStream.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = existingBytes
                    var lastNotify = 0L
                    while (isActive && !cancelRequested) {
                        val n = input.read(buffer)
                        if (n == -1) break
                        raf.write(buffer, 0, n)
                        downloaded += n
                        _downloadProgress.value = DownloadProgress(downloaded, totalSize, DownloadState.DOWNLOADING)
                        val now = System.currentTimeMillis()
                        if (now - lastNotify > 800) {
                            lastNotify = now
                            val percent = if (totalSize > 0) ((downloaded * 100) / totalSize).toInt() else 0
                            val mbDownloaded = downloaded / (1024.0 * 1024.0)
                            val mbTotal = totalSize / (1024.0 * 1024.0)
                            notificationManager.notify(
                                NOTIFICATION_ID,
                                buildNotification(percent, String.format("%.1f / %.1f MB", mbDownloaded, mbTotal))
                            )
                        }
                    }
                    if (cancelRequested) return@use
                }
            }
            connection.disconnect()
        }

        if (cancelRequested) {
            renewJob?.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return@withContext
        }

        if (result.isFailure) {
            // Network hiccup or process death mid-transfer: leave the partial
            // file in place. A fresh startDownload() call will resume from
            // partFile.length() via the Range header above.
            _downloadProgress.value = _downloadProgress.value.copy(state = DownloadState.PAUSED)
            renewJob?.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return@withContext
        }

        finalize(partFile)
    }

    private suspend fun finalize(partFile: File) = withContext(Dispatchers.IO) {
        val expectedSize = BuildConfig.MODEL_SIZE_BYTES
        if (!partFile.exists() || (expectedSize > 0L && partFile.length() < expectedSize)) {
            _downloadProgress.value = _downloadProgress.value.copy(state = DownloadState.PAUSED)
            renewJob?.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return@withContext
        }

        if (BuildConfig.MODEL_SHA256.isNotEmpty()) {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(100, "Verifying download…"))
            if (!verifyFileHash(partFile)) {
                partFile.delete()
                _downloadProgress.value = DownloadProgress(state = DownloadState.FAILED)
                renewJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@withContext
            }
        }

        val internalModelsDir = File(filesDir, "models").apply { mkdirs() }
        val finalFile = File(internalModelsDir, "gemma-4-E2B-it.litertlm")
        if (finalFile.exists()) finalFile.delete()
        partFile.inputStream().use { input ->
            finalFile.outputStream().use { output -> input.copyTo(output) }
        }
        partFile.delete()

        val total = if (expectedSize > 0) expectedSize else finalFile.length()
        _downloadProgress.value = DownloadProgress(total, total, DownloadState.COMPLETED)
        val done = NotificationCompat.Builder(this@ModelDownloadService, CHANNEL_ID)
            .setContentTitle("Model Download Complete")
            .setContentText("Gemma 4 model is ready to use")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(false)
            .build()
        notificationManager.notify(NOTIFICATION_ID, done)
        renewJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        val hashString = digest.digest().joinToString("") { "%02x".format(it) }
        return hashString.equals(BuildConfig.MODEL_SHA256, ignoreCase = true)
    }
}
