package com.charles.scamradar.app.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.charles.scamradar.app.data.model.ScanResult
import java.io.File
import java.io.FileOutputStream

fun buildShareText(scanResult: ScanResult): String {
    return buildString {
        appendLine("ScamRadar Scan Result")
        appendLine("Verdict: ${scanResult.verdict.name}")
        appendLine("Confidence: ${(scanResult.confidence * 100).toInt()}%")
        if (scanResult.scamType.name != "NONE") {
            appendLine("Scam Type: ${scanResult.scamType.name.replace("_", " ")}")
        }
        if (scanResult.redFlags.isNotEmpty()) {
            appendLine("\nRed Flags:")
            scanResult.redFlags.forEach { appendLine("- ${it.phrase}: ${it.reason}") }
        }
        appendLine("\nRecommended Action: ${scanResult.recommendedAction}")
    }
}

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}

fun shareCardImage(context: Context, bitmap: Bitmap) {
    val file = File(context.cacheDir, "share_card_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share via"))
}
