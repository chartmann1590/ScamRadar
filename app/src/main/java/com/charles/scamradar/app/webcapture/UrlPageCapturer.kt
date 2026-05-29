package com.charles.scamradar.app.webcapture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.webkit.WebView
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Helpers for full-page screenshot capture from an already-attached [WebView].
 *
 * The WebView itself is owned by the Composable layer ([androidx.compose.ui.viewinterop.AndroidView])
 * so that it has a real Window context — detached WebViews don't render reliably.
 */
object UrlPageCapturer {

    private const val TARGET_WIDTH = 720
    private const val MIN_CAPTURE_HEIGHT = 1200
    private const val MAX_CAPTURE_HEIGHT = 12_000

    suspend fun captureToFile(context: Context, webView: WebView): String = withContext(Dispatchers.Main) {
        check(Looper.myLooper() == Looper.getMainLooper())
        val bitmap = renderFullPage(webView)
        try {
            persistScreenshot(context, bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun renderFullPage(webView: WebView): Bitmap = suspendCancellableCoroutine { cont ->
        try {
            val rawHeight = try {
                (View::class.java
                    .getDeclaredMethod("computeVerticalScrollRange")
                    .invoke(webView) as Int)
            } catch (_: Throwable) {
                webView.contentHeight.coerceAtLeast(0)
            }
            val totalHeight = rawHeight
                .coerceAtLeast(MIN_CAPTURE_HEIGHT)
                .coerceAtMost(MAX_CAPTURE_HEIGHT)

            webView.measure(
                View.MeasureSpec.makeMeasureSpec(TARGET_WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(totalHeight, View.MeasureSpec.AT_MOST)
            )
            webView.layout(0, 0, TARGET_WIDTH, totalHeight)
            val bitmap = Bitmap.createBitmap(TARGET_WIDTH, totalHeight, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            webView.draw(canvas)
            cont.resume(bitmap)
        } catch (t: Throwable) {
            cont.resumeWith(Result.failure(t))
        }
    }

    private fun persistScreenshot(context: Context, bitmap: Bitmap): String {
        val dir = File(context.filesDir, "url_captures").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.webp")
        FileOutputStream(file).use { out ->
            @Suppress("DEPRECATION")
            bitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
        }
        return file.absolutePath
    }
}
