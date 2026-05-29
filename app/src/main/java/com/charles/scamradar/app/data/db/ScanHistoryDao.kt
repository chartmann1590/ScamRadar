package com.charles.scamradar.app.data.db

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class ScanHistoryDao(context: Context) {

    private val gson = Gson()
    private val historyFile = File(context.filesDir, "scan_history.json")
    private val items = MutableStateFlow(loadItems())

    fun getAll(): Flow<List<ScanHistoryEntity>> = items

    fun getRecent(limit: Int = 50): Flow<List<ScanHistoryEntity>> {
        return items.map { it.sortedByDescending(ScanHistoryEntity::timestamp).take(limit) }
    }

    suspend fun insert(entity: ScanHistoryEntity) {
        val current = items.value
        val nextId = ((current.maxOfOrNull { it.id } ?: 0L) + 1L)
        val updated = listOf(entity.copy(id = nextId)) + current
        items.value = updated.take(50)
        saveItems(items.value)
    }

    suspend fun deleteAll() {
        items.value = emptyList()
        saveItems(items.value)
    }

    suspend fun deleteById(id: Long) {
        items.value = items.value.filterNot { it.id == id }
        saveItems(items.value)
    }

    fun getCount(): Flow<Int> = items.map { it.size }

    private fun loadItems(): List<ScanHistoryEntity> {
        return runCatching {
            if (!historyFile.exists()) return emptyList()
            val type = object : TypeToken<List<ScanHistoryEntity>>() {}.type
            gson.fromJson<List<ScanHistoryEntity>>(historyFile.readText(), type).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun saveItems(value: List<ScanHistoryEntity>) {
        runCatching {
            historyFile.writeText(gson.toJson(value))
        }
    }
}
