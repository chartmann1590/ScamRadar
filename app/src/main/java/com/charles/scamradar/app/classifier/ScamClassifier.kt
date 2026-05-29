package com.charles.scamradar.app.classifier

import com.charles.scamradar.app.data.model.ScanResult

interface ScamClassifier {
    suspend fun classify(message: String): ScanResult
    val name: String
    val isAvailable: Boolean
}
