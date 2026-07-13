package com.charles.scamradar.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.feedbackDataStore: DataStore<Preferences> by preferencesDataStore(name = "scan_feedback")

/**
 * Stores token signatures of messages the user has told ScamRadar were incorrectly
 * flagged, entirely on-device. Read by [com.charles.scamradar.app.classifier.FeedbackLearningEngine]
 * to soften future verdicts on similar messages.
 */
class FeedbackStore(private val context: Context) {

    private val dataStore = context.feedbackDataStore

    val falsePositiveSignatures: Flow<List<Set<String>>> = dataStore.data.map { prefs ->
        (prefs[KEY_FALSE_POSITIVES] ?: emptySet())
            .map { entry -> entry.split(DELIMITER).toSet() }
    }

    suspend fun recordFalsePositive(tokens: Set<String>) {
        if (tokens.isEmpty()) return
        dataStore.edit { prefs ->
            val existing = (prefs[KEY_FALSE_POSITIVES] ?: emptySet()).toMutableList()
            existing.add(tokens.joinToString(DELIMITER))
            while (existing.size > MAX_ENTRIES) existing.removeAt(0)
            prefs[KEY_FALSE_POSITIVES] = existing.toSet()
        }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(KEY_FALSE_POSITIVES) }
    }

    companion object {
        private const val DELIMITER = ""
        private const val MAX_ENTRIES = 200
        private val KEY_FALSE_POSITIVES = stringSetPreferencesKey("false_positive_signatures")
    }
}
