package com.charles.scamradar.app.recovery

import android.content.Context
import com.charles.scamradar.app.data.model.ScamType
import com.google.gson.Gson

data class RecoveryStep(
    val id: String,
    val title: String,
    val description: String,
)

data class RecoveryFlow(
    val scamType: String,
    val title: String,
    val lead: String,
    val steps: List<RecoveryStep>,
)

data class RecoveryFlowFile(val flows: List<RecoveryFlow>)

class RecoveryFlowRepository(private val context: Context) {

    private val gson = Gson()

    private val cached: List<RecoveryFlow> by lazy {
        runCatching {
            val json = context.assets.open("recovery_flows.json").bufferedReader().use { it.readText() }
            gson.fromJson(json, RecoveryFlowFile::class.java).flows
        }.getOrDefault(emptyList())
    }

    fun flowFor(scamType: ScamType): RecoveryFlow? {
        val target = scamType.name
        cached.firstOrNull { it.scamType == target }?.let { return it }
        return cached.firstOrNull { it.scamType == "PHISHING" }
    }

    fun all(): List<RecoveryFlow> = cached
}
