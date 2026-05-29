package com.charles.scamradar.app.webcapture

import android.os.SystemClock
import android.webkit.WebView
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Three-signal AND gate for "page is actually done loading":
 *
 *  1. [onPageFinished] has fired at least once
 *  2. [WebChromeClient.onProgressChanged] reached 100 and stayed there for [stableMs]
 *  3. document.readyState === "complete"
 *
 * Hard-capped at [hardCapMs]. Whatever the page looks like at the cap, capture and continue.
 */
class PageReadyDetector(
    private val stableMs: Long = 1500L,
    private val hardCapMs: Long = 8000L
) {
    @Volatile private var pageFinished = false
    @Volatile private var lastProgress100At: Long = 0L
    @Volatile private var progressFull = false

    fun onPageFinished() {
        pageFinished = true
    }

    fun onProgress(percent: Int) {
        if (percent >= 100) {
            if (!progressFull) {
                progressFull = true
                lastProgress100At = SystemClock.uptimeMillis()
            }
        } else {
            progressFull = false
            lastProgress100At = 0L
        }
    }

    suspend fun await(webView: WebView): Boolean {
        val deadline = SystemClock.uptimeMillis() + hardCapMs
        val result = withTimeoutOrNull(hardCapMs) {
            while (SystemClock.uptimeMillis() < deadline) {
                if (pageFinished && progressFull && stableEnough() && documentReady(webView)) {
                    return@withTimeoutOrNull true
                }
                delay(150)
            }
            false
        }
        return result == true
    }

    private fun stableEnough(): Boolean {
        val ts = lastProgress100At
        return ts > 0L && SystemClock.uptimeMillis() - ts >= stableMs
    }

    private suspend fun documentReady(webView: WebView): Boolean {
        return suspendCancellableCoroutine { cont ->
            try {
                webView.evaluateJavascript("document.readyState") { value ->
                    val ready = value?.contains("complete") == true
                    if (cont.isActive) cont.resume(ready)
                }
            } catch (_: Throwable) {
                if (cont.isActive) cont.resume(false)
            }
        }
    }
}
