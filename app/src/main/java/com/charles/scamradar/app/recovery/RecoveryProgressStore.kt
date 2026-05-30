package com.charles.scamradar.app.recovery

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class RecoveryProgress(
    val scanId: String,
    val scamType: String,
    val completedSteps: Set<String>,
    val notes: String,
    val updatedAt: Long,
)

class RecoveryProgressStore(context: Context) {

    private val file = File(context.filesDir, "recovery_progress.json")
    private val gson = Gson()
    private val _all = MutableStateFlow(load())
    val all: StateFlow<List<RecoveryProgress>> = _all

    fun upsert(progress: RecoveryProgress) {
        val updated = _all.value.filterNot { it.scanId == progress.scanId } + progress
        _all.value = updated
        save(updated)
    }

    fun remove(scanId: String) {
        val updated = _all.value.filterNot { it.scanId == scanId }
        _all.value = updated
        save(updated)
    }

    fun forScan(scanId: String): RecoveryProgress? = _all.value.firstOrNull { it.scanId == scanId }

    private fun load(): List<RecoveryProgress> {
        return runCatching {
            if (!file.exists()) return emptyList()
            val type = object : TypeToken<List<RecoveryProgress>>() {}.type
            gson.fromJson<List<RecoveryProgress>>(file.readText(), type).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun save(items: List<RecoveryProgress>) {
        runCatching { file.writeText(gson.toJson(items)) }
    }
}
