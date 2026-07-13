package com.charles.scamradar.app.data.feedback

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.feedbackBugReportsStore: DataStore<Preferences> by preferencesDataStore(
    name = "feedback_bug_reports"
)

class BugReportRepo(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val bugReports: Flow<List<BugReport>> = context.feedbackBugReportsStore.data.map { preferences ->
        decodeReports(preferences[KEY_REPORTS].orEmpty())
    }

    suspend fun saveBugReport(report: BugReport) {
        updateBugReports(getBugReportsList().filterNot { it.number == report.number } + report)
    }

    suspend fun updateBugReports(reports: List<BugReport>) {
        val sorted = reports.sortedWith(compareByDescending<BugReport> { it.createdAt }.thenByDescending { it.number })
        context.feedbackBugReportsStore.edit { preferences ->
            preferences[KEY_REPORTS] = json.encodeToString(ListSerializer(BugReport.serializer()), sorted)
        }
    }

    suspend fun getBugReportsList(): List<BugReport> = bugReports.first()

    private fun decodeReports(value: String): List<BugReport> {
        if (value.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(BugReport.serializer()), value)
        }.getOrDefault(emptyList())
    }

    private companion object {
        val KEY_REPORTS = stringPreferencesKey("bug_reports_list")
    }
}
