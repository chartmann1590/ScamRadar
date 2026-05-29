package com.charles.scamradar.app.data.datastore

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

    val currentStreak: Flow<Int> = dataStore.data.map { it[KEY_CURRENT_STREAK] ?: 0 }

    val longestStreak: Flow<Int> = dataStore.data.map { it[KEY_LONGEST_STREAK] ?: 0 }

    val lastQuizDate: Flow<String> = dataStore.data.map { it[KEY_LAST_QUIZ_DATE] ?: "" }

    val quizCorrectTotal: Flow<Int> = dataStore.data.map { it[KEY_QUIZ_CORRECT_TOTAL] ?: 0 }

    val quizAnsweredTotal: Flow<Int> = dataStore.data.map { it[KEY_QUIZ_ANSWERED_TOTAL] ?: 0 }

    val unlockedAchievements: Flow<Set<String>> = dataStore.data.map {
        it[KEY_UNLOCKED_ACHIEVEMENTS] ?: emptySet()
    }

    val familyCode: Flow<String> = dataStore.data.map { it[KEY_FAMILY_CODE] ?: "" }

    val familyMemberLabel: Flow<String> = dataStore.data.map { it[KEY_FAMILY_MEMBER_LABEL] ?: "" }

    val careMode: Flow<Boolean> = dataStore.data.map { it[KEY_CARE_MODE] ?: false }

    val careModeAutoShare: Flow<Boolean> = dataStore.data.map { it[KEY_CARE_MODE_AUTO_SHARE] ?: false }

    val careModeAutoShareThreshold: Flow<String> = dataStore.data.map {
        it[KEY_CARE_MODE_AUTO_SHARE_THRESHOLD] ?: AUTO_SHARE_LIKELY_ONLY
    }

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

    suspend fun setCurrentStreak(value: Int) {
        dataStore.edit { it[KEY_CURRENT_STREAK] = value }
    }

    suspend fun setLongestStreak(value: Int) {
        dataStore.edit { it[KEY_LONGEST_STREAK] = value }
    }

    suspend fun setLastQuizDate(value: String) {
        dataStore.edit { it[KEY_LAST_QUIZ_DATE] = value }
    }

    suspend fun setQuizCorrectTotal(value: Int) {
        dataStore.edit { it[KEY_QUIZ_CORRECT_TOTAL] = value }
    }

    suspend fun setQuizAnsweredTotal(value: Int) {
        dataStore.edit { it[KEY_QUIZ_ANSWERED_TOTAL] = value }
    }

    suspend fun setUnlockedAchievements(value: Set<String>) {
        dataStore.edit { it[KEY_UNLOCKED_ACHIEVEMENTS] = value }
    }

    suspend fun setFamilyCode(value: String) {
        dataStore.edit { it[KEY_FAMILY_CODE] = value }
    }

    suspend fun setFamilyMemberLabel(value: String) {
        dataStore.edit { it[KEY_FAMILY_MEMBER_LABEL] = value }
    }

    suspend fun setCareMode(value: Boolean) {
        dataStore.edit { it[KEY_CARE_MODE] = value }
    }

    suspend fun setCareModeAutoShare(value: Boolean) {
        dataStore.edit { it[KEY_CARE_MODE_AUTO_SHARE] = value }
    }

    suspend fun setCareModeAutoShareThreshold(value: String) {
        dataStore.edit { it[KEY_CARE_MODE_AUTO_SHARE_THRESHOLD] = value }
    }

    companion object {
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
        private val KEY_SCAN_COUNT_TODAY = intPreferencesKey("scan_count_today")
        private val KEY_SCAN_COUNT_DATE = stringPreferencesKey("scan_count_date")
        private val KEY_MODEL_DOWNLOADED = booleanPreferencesKey("model_downloaded")
        private val KEY_WIFI_ONLY_DOWNLOAD = booleanPreferencesKey("wifi_only_download")
        private val KEY_INTERSTITIAL_COUNT = intPreferencesKey("interstitial_count")
        private val KEY_CURRENT_STREAK = intPreferencesKey("current_streak")
        private val KEY_LONGEST_STREAK = intPreferencesKey("longest_streak")
        private val KEY_LAST_QUIZ_DATE = stringPreferencesKey("last_quiz_date")
        private val KEY_QUIZ_CORRECT_TOTAL = intPreferencesKey("quiz_correct_total")
        private val KEY_QUIZ_ANSWERED_TOTAL = intPreferencesKey("quiz_answered_total")
        private val KEY_UNLOCKED_ACHIEVEMENTS =
            androidx.datastore.preferences.core.stringSetPreferencesKey("unlocked_achievements")
        private val KEY_FAMILY_CODE = stringPreferencesKey("family_code")
        private val KEY_FAMILY_MEMBER_LABEL = stringPreferencesKey("family_member_label")
        private val KEY_CARE_MODE = booleanPreferencesKey("care_mode")
        private val KEY_CARE_MODE_AUTO_SHARE = booleanPreferencesKey("care_mode_auto_share")
        private val KEY_CARE_MODE_AUTO_SHARE_THRESHOLD = stringPreferencesKey("care_mode_auto_share_threshold")

        const val AUTO_SHARE_LIKELY_ONLY = "LIKELY_ONLY"
        const val AUTO_SHARE_LIKELY_AND_SUSPICIOUS = "LIKELY_AND_SUSPICIOUS"
    }
}
