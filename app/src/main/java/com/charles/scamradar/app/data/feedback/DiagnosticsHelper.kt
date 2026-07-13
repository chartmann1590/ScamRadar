package com.charles.scamradar.app.data.feedback

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.charles.scamradar.app.BuildConfig
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

object DiagnosticsHelper {
    fun collectMarkdown(context: Context): String {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val stat = StatFs(Environment.getDataDirectory().path)
        val storageTotal = stat.blockCountLong * stat.blockSizeLong
        val storageFree = stat.availableBlocksLong * stat.blockSizeLong
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)

        return """
            ## Diagnostics

            - App: ScamRadar
            - Package: ${context.packageName}
            - Version: ${packageInfo.versionName ?: BuildConfig.VERSION_NAME} (${packageInfo.longVersionCodeCompat()})
            - Device: ${Build.BRAND} ${Build.MODEL}
            - Manufacturer: ${Build.MANUFACTURER}
            - Android: ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}
            - Locale: ${Locale.getDefault()}
            - Time Zone: ${TimeZone.getDefault().id}
            - Storage Free/Total: ${formatBytes(storageFree)} / ${formatBytes(storageTotal)}
            - Memory Free/Total: ${formatBytes(memoryInfo.availMem)} / ${formatBytes(memoryInfo.totalMem)}
            - Timestamp: ${Instant.now()}
        """.trimIndent()
    }

    @Suppress("DEPRECATION")
    private fun android.content.pm.PackageInfo.longVersionCodeCompat(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode else versionCode.toLong()

    private fun formatBytes(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) {
            String.format(Locale.US, "%.1f GB", gb)
        } else {
            String.format(Locale.US, "%.0f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
