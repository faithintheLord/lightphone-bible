package com.lightphone.bible.data

import com.lightphone.bible.data.model.HelloAoBook
import com.lightphone.bible.data.model.ParsedVerse
import com.lightphone.bible.data.provider.downloadMissingChapters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class ResumableChapterDownloadTest {
    private val book = HelloAoBook("GEN", "BSB", "Genesis", "Genesis", "Genesis", 1, 3, 3)

    @Test
    fun interruptedDownloadKeepsCompletedChaptersAndResumesAtMissingChapter() = runBlocking {
        val stored = mutableMapOf<Int, List<ParsedVerse>>()
        val requests = mutableListOf<Int>()
        var fail = true
        suspend fun download() = downloadMissingChapters(
            listOf(book),
            hasChapter = { _, chapter -> chapter in stored },
            fetchChapter = { _, chapter ->
                requests += chapter
                if (fail && chapter == 2) throw IllegalStateException("Offline")
                listOf(ParsedVerse(1, "Chapter $chapter"))
            },
            saveChapter = { _, chapter, verses -> stored[chapter] = verses },
        )
        assertFailsWith<IllegalStateException> { download() }
        assertEquals(setOf(1), stored.keys)
        fail = false
        download()
        assertEquals(listOf(1, 2, 2, 3), requests)
        assertEquals(setOf(1, 2, 3), stored.keys)
    }

    @Test
    fun existingContentIsPreservedWhenMissingChapterFails() = runBlocking {
        val stored = mutableMapOf(1 to listOf(ParsedVerse(1, "Saved offline text")))
        assertFailsWith<IllegalStateException> {
            downloadMissingChapters(listOf(book), { _, chapter -> chapter in stored },
                { _, _ -> error("Offline") }, { _, chapter, verses -> stored[chapter] = verses })
        }
        assertEquals("Saved offline text", stored[1]?.single()?.text)
        assertEquals(1, stored.size)
    }

    @Test
    fun emptyOrDuplicateChapterCannotBeSavedAsComplete() = runBlocking {
        for (invalid in listOf(emptyList(), listOf(ParsedVerse(1, "  ")), listOf(ParsedVerse(1, "a"), ParsedVerse(1, "b")))) {
            var saved = false
            assertFailsWith<IllegalArgumentException> {
                downloadMissingChapters(listOf(book), { _, _ -> false }, { _, _ -> invalid },
                    { _, _, _ -> saved = true })
            }
            assertFalse(saved)
        }
    }

    @Test
    fun cancellationIsPropagatedWithoutSavingIncompleteChapter() = runBlocking {
        var saved = false
        assertFailsWith<CancellationException> {
            downloadMissingChapters(listOf(book), { _, _ -> false },
                { _, _ -> throw CancellationException() }, { _, _, _ -> saved = true })
        }
        assertFalse(saved)
    }
}
