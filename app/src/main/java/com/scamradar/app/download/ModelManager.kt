package com.scamradar.app.download

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import com.scamradar.app.BuildConfig
import java.io.File
import java.security.MessageDigest

enum class DeviceClass { FULL, LITE_RECOMMENDED, LITE_ONLY }

object ModelManager {

    private const val MODEL_DIR = "models"
    private const val MODEL_FILENAME = "gemma-4-E2B-it.litertlm"

    fun getModelFile(context: Context): File {
        return File(File(context.filesDir, MODEL_DIR), MODEL_FILENAME)
    }

    fun isModelDownloaded(context: Context): Boolean {
        return getModelFile(context).exists() && getModelFile(context).length() > 0
    }

    fun deleteModel(context: Context): Boolean {
        val file = getModelFile(context)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    fun getModelFileSize(context: Context): Long {
        val file = getModelFile(context)
        return if (file.exists()) file.length() else 0L
    }

    fun verifyModelHash(context: Context): Boolean {
        val expectedHash = BuildConfig.MODEL_SHA256
        if (expectedHash.isEmpty()) return true
        val file = getModelFile(context)
        if (!file.exists()) return false

        val digest = MessageDigest.getInstance("SHA-256")
        val inputStream = java.io.FileInputStream(file)
        val buffer = ByteArray(8 * 1024)
        var bytesRead: Int
        while (true) {
            bytesRead = inputStream.read(buffer)
            if (bytesRead == -1) break
            digest.update(buffer, 0, bytesRead)
        }
        inputStream.close()
        val hashBytes = digest.digest()
        val hashString = hashBytes.joinToString("") { "%02x".format(it) }
        return hashString.equals(expectedHash, ignoreCase = true)
    }

    fun getDeviceClass(context: Context): DeviceClass {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRamMb = memoryInfo.totalMem / (1024 * 1024)

        val statFs = StatFs(context.filesDir.absolutePath)
        val freeStorageBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            statFs.availableBlocksLong * statFs.blockSizeLong
        } else {
            statFs.availableBlocks.toLong() * statFs.blockSize.toLong()
        }
        val freeStorageMb = freeStorageBytes / (1024 * 1024)

        return when {
            totalRamMb >= 4096 && freeStorageMb >= 4096 -> DeviceClass.FULL
            totalRamMb >= 3072 -> DeviceClass.LITE_RECOMMENDED
            else -> DeviceClass.LITE_ONLY
        }
    }
}
