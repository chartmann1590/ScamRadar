package com.scamradar.app.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.scamradar.app.BuildConfig
import com.scamradar.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
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

class ModelDownloadService : Service() {

    companion object {
        private const val CHANNEL_ID = "model_download"
        private const val NOTIFICATION_ID = 1001
        private const val BUFFER_SIZE = 8 * 1024

        private val _downloadProgress = MutableStateFlow(DownloadProgress())
        val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress

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

        const val ACTION_START = "com.scamradar.app.ACTION_START_DOWNLOAD"
        const val ACTION_PAUSE = "com.scamradar.app.ACTION_PAUSE_DOWNLOAD"
        const val ACTION_RESUME = "com.scamradar.app.ACTION_RESUME_DOWNLOAD"
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var downloadJob: Job? = null
    private var pausedBytesDownloaded: Long = 0L
    private var currentConnection: HttpURLConnection? = null
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundAndDownload()
            ACTION_PAUSE -> pauseDownloadInternal()
            ACTION_RESUME -> resumeDownloadInternal()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
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
        val notification = buildNotification(0, "Preparing download...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        val tempFile = File(File(filesDir, "models"), "gemma-4-E2B-it.litertlm.tmp")
        pausedBytesDownloaded = if (tempFile.exists()) tempFile.length() else 0L
        beginDownload(pausedBytesDownloaded)
    }

    private fun beginDownload(fromByte: Long) {
        downloadJob?.cancel()
        downloadJob = serviceScope.launch {
            try {
                _downloadProgress.value = _downloadProgress.value.copy(state = DownloadState.DOWNLOADING)

                val modelsDir = File(filesDir, "models")
                if (!modelsDir.exists()) {
                    withContext(Dispatchers.IO) {
                        modelsDir.mkdirs()
                    }
                }
                val modelFile = File(modelsDir, "gemma-4-E2B-it.litertlm")
                val tempFile = File(modelsDir, "gemma-4-E2B-it.litertlm.tmp")

                val url = URL(BuildConfig.MODEL_DOWNLOAD_URL)
                val connection = withContext(Dispatchers.IO) { url.openConnection() as HttpURLConnection }
                currentConnection = connection

                connection.connectTimeout = 30_000
                connection.readTimeout = 30_000

                if (fromByte > 0) {
                    connection.setRequestProperty("Range", "bytes=$fromByte-")
                }

                withContext(Dispatchers.IO) { connection.connect() }

                val responseCode = connection.responseCode
                val responseLength = connection.contentLengthLong
                val isResuming = responseCode == HttpURLConnection.HTTP_PARTIAL && fromByte > 0
                val effectiveFromByte = if (isResuming) fromByte else 0L
                val totalBytes = when {
                    isResuming && responseLength > 0L -> fromByte + responseLength
                    responseLength > 0L -> responseLength
                    else -> BuildConfig.MODEL_SIZE_BYTES
                }
                val inputStream = withContext(Dispatchers.IO) {
                    BufferedInputStream(connection.inputStream, BUFFER_SIZE)
                }

                _downloadProgress.value = DownloadProgress(
                    bytesDownloaded = effectiveFromByte,
                    totalBytes = totalBytes,
                    state = DownloadState.DOWNLOADING
                )

                val fileOutputStream = withContext(Dispatchers.IO) {
                    if (effectiveFromByte > 0 && tempFile.exists()) {
                        FileOutputStream(tempFile, true)
                    } else {
                        FileOutputStream(tempFile)
                    }
                }

                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                var totalRead = effectiveFromByte

                withContext(Dispatchers.IO) {
                    while (isActive) {
                        bytesRead = inputStream.read(buffer)
                        if (bytesRead == -1) break
                        fileOutputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        _downloadProgress.value = DownloadProgress(
                            bytesDownloaded = totalRead,
                            totalBytes = totalBytes,
                            state = DownloadState.DOWNLOADING
                        )

                        val percent = if (totalBytes > 0) {
                            ((totalRead * 100) / totalBytes).toInt()
                        } else 0

                        val mbDownloaded = totalRead / (1024.0 * 1024.0)
                        val mbTotal = totalBytes / (1024.0 * 1024.0)
                        val notification = buildNotification(
                            percent,
                            String.format("%.1f / %.1f MB", mbDownloaded, mbTotal)
                        )
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    }

                    fileOutputStream.flush()
                    fileOutputStream.close()
                    inputStream.close()
                }

                connection.disconnect()
                currentConnection = null

                if (_downloadProgress.value.state == DownloadState.PAUSED) {
                    pausedBytesDownloaded = totalRead
                    return@launch
                }

                if (BuildConfig.MODEL_SHA256.isNotEmpty()) {
                    val notification = buildNotification(100, "Verifying download...")
                    notificationManager.notify(NOTIFICATION_ID, notification)

                    val hashVerified = withContext(Dispatchers.IO) { verifyFileHash(tempFile) }
                    if (!hashVerified) {
                        tempFile.delete()
                        _downloadProgress.value = DownloadProgress(
                            bytesDownloaded = 0L,
                            totalBytes = totalBytes,
                            state = DownloadState.FAILED
                        )
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@launch
                    }
                }

                withContext(Dispatchers.IO) {
                    if (modelFile.exists()) modelFile.delete()
                    tempFile.renameTo(modelFile)
                }

                _downloadProgress.value = DownloadProgress(
                    bytesDownloaded = totalBytes,
                    totalBytes = totalBytes,
                    state = DownloadState.COMPLETED
                )

                val doneNotification = NotificationCompat.Builder(this@ModelDownloadService, CHANNEL_ID)
                    .setContentTitle("Model Download Complete")
                    .setContentText("Gemma 4 model is ready to use")
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setOngoing(false)
                    .build()
                notificationManager.notify(NOTIFICATION_ID, doneNotification)

                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()

            } catch (e: Exception) {
                if (_downloadProgress.value.state != DownloadState.PAUSED) {
                    _downloadProgress.value = DownloadProgress(
                        bytesDownloaded = _downloadProgress.value.bytesDownloaded,
                        totalBytes = _downloadProgress.value.totalBytes,
                        state = DownloadState.FAILED
                    )
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun pauseDownloadInternal() {
        _downloadProgress.value = _downloadProgress.value.copy(state = DownloadState.PAUSED)
        pausedBytesDownloaded = _downloadProgress.value.bytesDownloaded
        currentConnection?.disconnect()
        currentConnection = null
        downloadJob?.cancel()
    }

    private fun resumeDownloadInternal() {
        beginDownload(pausedBytesDownloaded)
    }

    private fun verifyFileHash(file: File): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        val inputStream = java.io.FileInputStream(file)
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        while (true) {
            bytesRead = inputStream.read(buffer)
            if (bytesRead == -1) break
            digest.update(buffer, 0, bytesRead)
        }
        inputStream.close()
        val hashBytes = digest.digest()
        val hashString = hashBytes.joinToString("") { "%02x".format(it) }
        return hashString.equals(BuildConfig.MODEL_SHA256, ignoreCase = true)
    }
}
