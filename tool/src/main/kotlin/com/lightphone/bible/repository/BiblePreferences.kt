package com.lightphone.bible.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.lightphone.bible.data.model.BibleReference
import com.lightphone.bible.data.model.VERSE_TEXT_NORMALIZATION_VERSION
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
    val completedInterlinearBooks = stringSetPreferencesKey("completed_interlinear_books_v2")
}

internal data class TextNormalizationProgress(
    val version: Int,
    val chapters: Set<String>,
) {
    fun contains(bookId: String, chapter: Int): Boolean =
        version == VERSE_TEXT_NORMALIZATION_VERSION || "$bookId.$chapter" in chapters
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
        require(scale.isFinite() && scale in 0.85f..1.3f)
        dataStore.edit { it[BiblePreferenceKeys.fontScale] = scale }
    }

    suspend fun getCompletedInterlinearBooks(): Set<String> =
        dataStore.data.first()[BiblePreferenceKeys.completedInterlinearBooks].orEmpty()

    suspend fun markInterlinearBookComplete(translationId: String, bookId: String) {
        dataStore.edit { prefs ->
            prefs[BiblePreferenceKeys.completedInterlinearBooks] =
                prefs[BiblePreferenceKeys.completedInterlinearBooks].orEmpty() + "$translationId:$bookId"
        }
    }

    suspend fun getTextNormalizationVersion(translationId: String): Int =
        dataStore.data.first()[intPreferencesKey("text_normalization_version_$translationId")] ?: 0

    suspend fun getNormalizedTextChapters(translationId: String): Set<String> =
        dataStore.data.first()[normalizedTextChaptersKey(translationId)].orEmpty()

    internal suspend fun getTextNormalizationProgress(translationId: String): TextNormalizationProgress {
        val prefs = dataStore.data.first()
        return TextNormalizationProgress(
            version = prefs[intPreferencesKey("text_normalization_version_$translationId")] ?: 0,
            chapters = prefs[normalizedTextChaptersKey(translationId)].orEmpty(),
        )
    }

    suspend fun markTextChapterNormalized(translationId: String, bookId: String, chapter: Int) {
        dataStore.edit { prefs ->
            val key = normalizedTextChaptersKey(translationId)
            prefs[key] = prefs[key].orEmpty() + "$bookId.$chapter"
        }
    }

    suspend fun markTranslationTextNormalized(translationId: String) {
        dataStore.edit { prefs ->
            prefs[intPreferencesKey("text_normalization_version_$translationId")] = VERSE_TEXT_NORMALIZATION_VERSION
            prefs.remove(normalizedTextChaptersKey(translationId))
        }
    }

    private fun normalizedTextChaptersKey(translationId: String) =
        stringSetPreferencesKey("normalized_text_chapters_v${VERSE_TEXT_NORMALIZATION_VERSION}_$translationId")

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
