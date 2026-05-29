package com.charles.scamradar.app.webcapture

data class CaptureResult(
    val originalUrl: String,
    val finalUrl: String,
    val screenshotPath: String,
    val redirectCount: Int,
    val loadDurationMs: Long
)
