package com.lightphone.bible.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.lightphone.bible.data.model.BibleReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class BibleSyncState {
    IDLE,
    SYNCING,
    FAILED,
    COMPLETE,
}

object BiblePreferenceKeys {
    val primaryTranslationId = stringPreferencesKey("primary_translation_id")
    val compareTranslationIds = stringSetPreferencesKey("compare_translation_ids")
    val fontScale = floatPreferencesKey("font_scale")
    val lastReadReference = stringPreferencesKey("last_read_reference")
    val syncState = stringPreferencesKey("sync_state")
    val lastSyncError = stringPreferencesKey("last_sync_error")
}

class BiblePreferences(
    private val dataStore: DataStore<Preferences>,
) {
    val primaryTranslationId: Flow<String?> =
        dataStore.data.map { it[BiblePreferenceKeys.primaryTranslationId] }

    val compareTranslationIds: Flow<Set<String>> =
        dataStore.data.map { it[BiblePreferenceKeys.compareTranslationIds].orEmpty() }

    val fontScale: Flow<Float> =
        dataStore.data.map { it[BiblePreferenceKeys.fontScale] ?: DEFAULT_FONT_SCALE }

    val lastReadReference: Flow<BibleReference?> =
        dataStore.data.map { prefs ->
            prefs[BiblePreferenceKeys.lastReadReference]?.let(BibleReference::parse)
        }

    val syncState: Flow<BibleSyncState> =
        dataStore.data.map { prefs ->
            prefs[BiblePreferenceKeys.syncState]
                ?.let { runCatching { BibleSyncState.valueOf(it) }.getOrNull() }
                ?: BibleSyncState.IDLE
        }

    suspend fun getPrimaryTranslationId(): String? =
        dataStore.data.first()[BiblePreferenceKeys.primaryTranslationId]

    suspend fun setPrimaryTranslationId(id: String) {
        dataStore.edit { it[BiblePreferenceKeys.primaryTranslationId] = id }
    }

    suspend fun setCompareTranslationIds(ids: Set<String>) {
        dataStore.edit { it[BiblePreferenceKeys.compareTranslationIds] = ids }
    }

    suspend fun setFontScale(scale: Float) {
        dataStore.edit { it[BiblePreferenceKeys.fontScale] = scale }
    }

    suspend fun setLastRead(reference: BibleReference) {
        dataStore.edit { it[BiblePreferenceKeys.lastReadReference] = reference.format() }
    }

    suspend fun setSyncState(state: BibleSyncState, errorMessage: String? = null) {
        dataStore.edit { prefs ->
            prefs[BiblePreferenceKeys.syncState] = state.name
            if (errorMessage == null) {
                prefs.remove(BiblePreferenceKeys.lastSyncError)
            } else {
                prefs[BiblePreferenceKeys.lastSyncError] = errorMessage
            }
        }
    }

    companion object {
        const val DEFAULT_FONT_SCALE = 1.0f
    }
}
