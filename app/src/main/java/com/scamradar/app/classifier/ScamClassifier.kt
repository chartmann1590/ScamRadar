package com.scamradar.app.classifier

import com.scamradar.app.data.model.ScanResult

interface ScamClassifier {
    suspend fun classify(message: String): ScanResult
    val name: String
    val isAvailable: Boolean
}
