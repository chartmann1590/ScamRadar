package com.charles.scamradar.app.data.db

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.charles.scamradar.app.data.model.ClassifierTier
import com.charles.scamradar.app.data.model.RedFlag
import com.charles.scamradar.app.data.model.ScanMode
import com.charles.scamradar.app.data.model.ScanResult

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val verdict: String,
    val scamType: String,
    val confidence: Float,
    val originalMessage: String,
    val redFlagsJson: String,
    val recommendedAction: String,
    val classifierTier: String,
    val scanMode: String,
    val timestamp: Long
) {
    @Ignore
    constructor(scanResult: ScanResult, scanMode: ScanMode) : this(
        id = 0,
        verdict = scanResult.verdict.name,
        scamType = scanResult.scamType.name,
        confidence = scanResult.confidence,
        originalMessage = scanResult.originalMessage,
        redFlagsJson = Gson().toJson(scanResult.redFlags),
        recommendedAction = scanResult.recommendedAction,
        classifierTier = scanResult.classifierTier.name,
        scanMode = scanMode.name,
        timestamp = System.currentTimeMillis()
    )
}
