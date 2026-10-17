package com.lightphone.bible.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.lightphone.bible.data.model.HelloAoBook
import com.lightphone.bible.data.model.ParsedVerse
import com.lightphone.bible.data.model.VERSE_TEXT_NORMALIZATION_VERSION
import com.lightphone.bible.data.provider.downloadMissingChapters
import com.lightphone.bible.repository.BiblePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextNormalizationMigrationTest {
    private class MemoryPreferences : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            return transform(data.value).also { data.value = it }
        }
    }

    @Test
    fun legacyTextIsReplacedByChapterAndInterruptedMigrationResumes() = runBlocking {
        val storage = MemoryPreferences()
        val book = HelloAoBook("GEN", "BSB", "Genesis", "Genesis", "Genesis", 1, 2, 2)
        val verses = mutableMapOf(1 to "wordnext", 2 to "oldreadabletext")
        var fail = true
        val requests = mutableListOf<Int>()
        suspend fun migrate() {
            // New repository/preferences instances must reuse persisted progress after a restart.
            val preferences = BiblePreferences(storage)
            val progress = preferences.getTextNormalizationProgress("BSB")
            downloadMissingChapters(listOf(book),
                hasChapter = { id, chapter -> progress.contains(id, chapter) && chapter in verses },
                fetchChapter = { _, chapter ->
                    requests += chapter
                    if (fail && chapter == 2) error("Connection lost")
                    listOf(ParsedVerse(1, "normalized chapter $chapter"))
                },
                saveChapter = { id, chapter, content ->
                    verses[chapter] = content.single().text
                    preferences.markTextChapterNormalized("BSB", id, chapter)
                },
            )
            preferences.markTranslationTextNormalized("BSB")
        }
        assertFailsWith<IllegalStateException> { migrate() }
        val preferences = BiblePreferences(storage)
        assertEquals(0, preferences.getTextNormalizationVersion("BSB"))
        assertEquals("normalized chapter 1", verses[1])
        assertEquals("oldreadabletext", verses[2])
        assertTrue(preferences.getTextNormalizationProgress("BSB").contains("GEN", 1))
        assertFalse(preferences.getTextNormalizationProgress("BSB").contains("GEN", 2))
        fail = false
        migrate()
        assertEquals(listOf(1, 2, 2), requests)
        assertEquals(VERSE_TEXT_NORMALIZATION_VERSION, preferences.getTextNormalizationVersion("BSB"))
        assertTrue(preferences.getNormalizedTextChapters("BSB").isEmpty())
        assertFalse(preferences.getTextNormalizationProgress("WEB").contains("GEN", 1))
    }

    @Test
    fun freshPartialDownloadsResumeAndPreferencesArePreserved() = runBlocking {
        val storage = MemoryPreferences()
        val preferences = BiblePreferences(storage)
        preferences.setPrimaryTranslationId("WEB")
        preferences.markTextChapterNormalized("BSB", "GEN", 1)
        val resumed = BiblePreferences(storage)
        assertTrue(resumed.getTextNormalizationProgress("BSB").contains("GEN", 1))
        assertFalse(resumed.getTextNormalizationProgress("BSB").contains("GEN", 2))
        assertEquals("WEB", resumed.getPrimaryTranslationId())
        assertEquals(0, resumed.getTextNormalizationVersion("BSB"))
    }
}
