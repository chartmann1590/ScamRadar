package com.scamradar.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPrefs(private val context: Context) {

    private val dataStore = context.dataStore

    val onboardingComplete: Flow<Boolean> = dataStore.data.map { it[KEY_ONBOARDING_COMPLETE] ?: false }

    val darkMode: Flow<String> = dataStore.data.map { it[KEY_DARK_MODE] ?: "system" }

    val scanCountToday: Flow<Int> = dataStore.data.map { it[KEY_SCAN_COUNT_TODAY] ?: 0 }

    val scanCountDate: Flow<String> = dataStore.data.map { it[KEY_SCAN_COUNT_DATE] ?: "" }

    val modelDownloaded: Flow<Boolean> = dataStore.data.map { it[KEY_MODEL_DOWNLOADED] ?: false }

    val wifiOnlyDownload: Flow<Boolean> = dataStore.data.map { it[KEY_WIFI_ONLY_DOWNLOAD] ?: true }

    val interstitialCount: Flow<Int> = dataStore.data.map { it[KEY_INTERSTITIAL_COUNT] ?: 0 }

    suspend fun setOnboardingComplete(value: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETE] = value }
    }

    suspend fun setDarkMode(value: String) {
        dataStore.edit { it[KEY_DARK_MODE] = value }
    }

    suspend fun setScanCountToday(value: Int) {
        dataStore.edit { it[KEY_SCAN_COUNT_TODAY] = value }
    }

    suspend fun setScanCountDate(value: String) {
        dataStore.edit { it[KEY_SCAN_COUNT_DATE] = value }
    }

    suspend fun setModelDownloaded(value: Boolean) {
        dataStore.edit { it[KEY_MODEL_DOWNLOADED] = value }
    }

    suspend fun setWifiOnlyDownload(value: Boolean) {
        dataStore.edit { it[KEY_WIFI_ONLY_DOWNLOAD] = value }
    }

    suspend fun setInterstitialCount(value: Int) {
        dataStore.edit { it[KEY_INTERSTITIAL_COUNT] = value }
    }

    companion object {
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
        private val KEY_SCAN_COUNT_TODAY = intPreferencesKey("scan_count_today")
        private val KEY_SCAN_COUNT_DATE = stringPreferencesKey("scan_count_date")
        private val KEY_MODEL_DOWNLOADED = booleanPreferencesKey("model_downloaded")
        private val KEY_WIFI_ONLY_DOWNLOAD = booleanPreferencesKey("wifi_only_download")
        private val KEY_INTERSTITIAL_COUNT = intPreferencesKey("interstitial_count")
    }
}
