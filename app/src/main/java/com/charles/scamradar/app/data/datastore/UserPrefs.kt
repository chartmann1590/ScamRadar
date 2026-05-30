package com.charles.scamradar.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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

    val tier: Flow<String> = dataStore.data.map { it[KEY_TIER] ?: TIER_FREE }
    val shieldEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_SHIELD_ENABLED] ?: false }
    val shieldPerAppDisabled: Flow<Set<String>> = dataStore.data.map { it[KEY_SHIELD_PER_APP_DISABLED] ?: emptySet() }
    val shieldSensitivity: Flow<String> = dataStore.data.map { it[KEY_SHIELD_SENSITIVITY] ?: SHIELD_SENSITIVITY_MEDIUM }
    val shieldPausedUntil: Flow<Long> = dataStore.data.map { it[KEY_SHIELD_PAUSED_UNTIL] ?: 0L }
    val clipboardChipEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_CLIPBOARD_CHIP_ENABLED] ?: false }
    val seniorMode: Flow<Boolean> = dataStore.data.map { it[KEY_SENIOR_MODE] ?: false }
    val seniorEmergencyContact: Flow<String> = dataStore.data.map { it[KEY_SENIOR_EMERGENCY_CONTACT] ?: "" }
    val trendingAlertsEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_TRENDING_ALERTS_ENABLED] ?: true }
    val regionOverride: Flow<String> = dataStore.data.map { it[KEY_REGION_OVERRIDE] ?: "" }
    val fcmTokenRegistered: Flow<Boolean> = dataStore.data.map { it[KEY_FCM_TOKEN_REGISTERED] ?: false }
    val shieldOnboardingSeen: Flow<Boolean> = dataStore.data.map { it[KEY_SHIELD_ONBOARDING_SEEN] ?: false }

    suspend fun setOnboardingComplete(value: Boolean) { dataStore.edit { it[KEY_ONBOARDING_COMPLETE] = value } }
    suspend fun setDarkMode(value: String) { dataStore.edit { it[KEY_DARK_MODE] = value } }
    suspend fun setScanCountToday(value: Int) { dataStore.edit { it[KEY_SCAN_COUNT_TODAY] = value } }
    suspend fun setScanCountDate(value: String) { dataStore.edit { it[KEY_SCAN_COUNT_DATE] = value } }
    suspend fun setModelDownloaded(value: Boolean) { dataStore.edit { it[KEY_MODEL_DOWNLOADED] = value } }
    suspend fun setWifiOnlyDownload(value: Boolean) { dataStore.edit { it[KEY_WIFI_ONLY_DOWNLOAD] = value } }
    suspend fun setInterstitialCount(value: Int) { dataStore.edit { it[KEY_INTERSTITIAL_COUNT] = value } }
    suspend fun setCurrentStreak(value: Int) { dataStore.edit { it[KEY_CURRENT_STREAK] = value } }
    suspend fun setLongestStreak(value: Int) { dataStore.edit { it[KEY_LONGEST_STREAK] = value } }
    suspend fun setLastQuizDate(value: String) { dataStore.edit { it[KEY_LAST_QUIZ_DATE] = value } }
    suspend fun setQuizCorrectTotal(value: Int) { dataStore.edit { it[KEY_QUIZ_CORRECT_TOTAL] = value } }
    suspend fun setQuizAnsweredTotal(value: Int) { dataStore.edit { it[KEY_QUIZ_ANSWERED_TOTAL] = value } }
    suspend fun setUnlockedAchievements(value: Set<String>) { dataStore.edit { it[KEY_UNLOCKED_ACHIEVEMENTS] = value } }
    suspend fun setFamilyCode(value: String) { dataStore.edit { it[KEY_FAMILY_CODE] = value } }
    suspend fun setFamilyMemberLabel(value: String) { dataStore.edit { it[KEY_FAMILY_MEMBER_LABEL] = value } }
    suspend fun setCareMode(value: Boolean) { dataStore.edit { it[KEY_CARE_MODE] = value } }
    suspend fun setCareModeAutoShare(value: Boolean) { dataStore.edit { it[KEY_CARE_MODE_AUTO_SHARE] = value } }
    suspend fun setCareModeAutoShareThreshold(value: String) { dataStore.edit { it[KEY_CARE_MODE_AUTO_SHARE_THRESHOLD] = value } }

    suspend fun setTier(value: String) { dataStore.edit { it[KEY_TIER] = value } }
    suspend fun setShieldEnabled(value: Boolean) { dataStore.edit { it[KEY_SHIELD_ENABLED] = value } }
    suspend fun setShieldPerAppDisabled(value: Set<String>) { dataStore.edit { it[KEY_SHIELD_PER_APP_DISABLED] = value } }
    suspend fun setShieldSensitivity(value: String) { dataStore.edit { it[KEY_SHIELD_SENSITIVITY] = value } }
    suspend fun setShieldPausedUntil(value: Long) { dataStore.edit { it[KEY_SHIELD_PAUSED_UNTIL] = value } }
    suspend fun setClipboardChipEnabled(value: Boolean) { dataStore.edit { it[KEY_CLIPBOARD_CHIP_ENABLED] = value } }
    suspend fun setSeniorMode(value: Boolean) { dataStore.edit { it[KEY_SENIOR_MODE] = value } }
    suspend fun setSeniorEmergencyContact(value: String) { dataStore.edit { it[KEY_SENIOR_EMERGENCY_CONTACT] = value } }
    suspend fun setTrendingAlertsEnabled(value: Boolean) { dataStore.edit { it[KEY_TRENDING_ALERTS_ENABLED] = value } }
    suspend fun setRegionOverride(value: String) { dataStore.edit { it[KEY_REGION_OVERRIDE] = value } }
    suspend fun setFcmTokenRegistered(value: Boolean) { dataStore.edit { it[KEY_FCM_TOKEN_REGISTERED] = value } }
    suspend fun setShieldOnboardingSeen(value: Boolean) { dataStore.edit { it[KEY_SHIELD_ONBOARDING_SEEN] = value } }

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
        private val KEY_UNLOCKED_ACHIEVEMENTS = stringSetPreferencesKey("unlocked_achievements")
        private val KEY_FAMILY_CODE = stringPreferencesKey("family_code")
        private val KEY_FAMILY_MEMBER_LABEL = stringPreferencesKey("family_member_label")
        private val KEY_CARE_MODE = booleanPreferencesKey("care_mode")
        private val KEY_CARE_MODE_AUTO_SHARE = booleanPreferencesKey("care_mode_auto_share")
        private val KEY_CARE_MODE_AUTO_SHARE_THRESHOLD = stringPreferencesKey("care_mode_auto_share_threshold")

        private val KEY_TIER = stringPreferencesKey("entitlement_tier")
        private val KEY_SHIELD_ENABLED = booleanPreferencesKey("shield_enabled")
        private val KEY_SHIELD_PER_APP_DISABLED = stringSetPreferencesKey("shield_per_app_disabled")
        private val KEY_SHIELD_SENSITIVITY = stringPreferencesKey("shield_sensitivity")
        private val KEY_SHIELD_PAUSED_UNTIL = longPreferencesKey("shield_paused_until")
        private val KEY_CLIPBOARD_CHIP_ENABLED = booleanPreferencesKey("clipboard_chip_enabled")
        private val KEY_SENIOR_MODE = booleanPreferencesKey("senior_mode")
        private val KEY_SENIOR_EMERGENCY_CONTACT = stringPreferencesKey("senior_emergency_contact")
        private val KEY_TRENDING_ALERTS_ENABLED = booleanPreferencesKey("trending_alerts_enabled")
        private val KEY_REGION_OVERRIDE = stringPreferencesKey("region_override")
        private val KEY_FCM_TOKEN_REGISTERED = booleanPreferencesKey("fcm_token_registered")
        private val KEY_SHIELD_ONBOARDING_SEEN = booleanPreferencesKey("shield_onboarding_seen")

        const val AUTO_SHARE_LIKELY_ONLY = "LIKELY_ONLY"
        const val AUTO_SHARE_LIKELY_AND_SUSPICIOUS = "LIKELY_AND_SUSPICIOUS"

        const val TIER_FREE = "FREE"
        const val TIER_PREMIUM = "PREMIUM"
        const val TIER_FAMILY = "FAMILY"
        const val TIER_FAMILY_MEMBER = "FAMILY_MEMBER"

        const val SHIELD_SENSITIVITY_LOW = "LOW"
        const val SHIELD_SENSITIVITY_MEDIUM = "MEDIUM"
        const val SHIELD_SENSITIVITY_HIGH = "HIGH"
    }
}
