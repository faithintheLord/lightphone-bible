package com.lightphone.bible.ui.reader

import com.lightphone.bible.data.model.BibleBooks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReaderNavigationTest {
    @Test
    fun `moves within a book`() {
        assertEquals(ReaderChapter("JHN", 4), adjacentChapter("JHN", 3, forward = true))
        assertEquals(ReaderChapter("JHN", 2), adjacentChapter("JHN", 3, forward = false))
    }

    @Test
    fun `crosses book boundaries in both directions`() {
        assertEquals(ReaderChapter("EXO", 1), adjacentChapter("GEN", 50, forward = true))
        assertEquals(ReaderChapter("GEN", 50), adjacentChapter("EXO", 1, forward = false))
    }

    @Test
    fun `crosses testament boundary and single chapter books`() {
        assertEquals(ReaderChapter("MAT", 1), adjacentChapter("MAL", 4, forward = true))
        assertEquals(ReaderChapter("MAL", 4), adjacentChapter("MAT", 1, forward = false))
        assertEquals(ReaderChapter("JUD", 1), adjacentChapter("3JN", 1, forward = true))
        assertEquals(ReaderChapter("3JN", 1), adjacentChapter("JUD", 1, forward = false))
    }

    @Test
    fun `stops at the first and last chapters`() {
        assertNull(adjacentChapter("GEN", 1, forward = false))
        assertNull(adjacentChapter("REV", 22, forward = true))
    }

    @Test
    fun `rejects invalid starting chapters and unknown books`() {
        for (forward in listOf(false, true)) {
            assertNull(adjacentChapter("UNKNOWN", 1, forward))
            assertNull(adjacentChapter("GEN", 0, forward))
            assertNull(adjacentChapter("GEN", 51, forward))
        }
    }

    @Test
    fun `every canonical chapter has the correct reversible neighbors`() {
        val chapters = BibleBooks.all.flatMap { book ->
            (1..BibleBooks.chapterCount(book.id)).map { ReaderChapter(book.id, it) }
        }
        assertEquals(1189, chapters.size)
        chapters.forEachIndexed { index, current ->
            assertEquals(chapters.getOrNull(index - 1), adjacentChapter(current.bookId, current.chapter, false))
            assertEquals(chapters.getOrNull(index + 1), adjacentChapter(current.bookId, current.chapter, true))
        }
    }
}
