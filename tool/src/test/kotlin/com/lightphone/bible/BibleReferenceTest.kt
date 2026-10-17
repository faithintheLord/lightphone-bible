package com.lightphone.bible

import com.lightphone.bible.data.model.BibleReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BibleReferenceTest {
    @Test
    fun parseFullReference() {
        val ref = BibleReference.parse("BSB:GEN.1.1")
        assertNotNull(ref)
        assertEquals("BSB", ref.translationId)
        assertEquals("GEN", ref.bookId)
        assertEquals(1, ref.chapter)
        assertEquals(1, ref.verse)
    }

    @Test
    fun parseCanonicalWithoutVerse() {
        val ref = BibleReference.parseCanonical("MAT.5", translationId = "WEB")
        assertNotNull(ref)
        assertEquals("WEB", ref.translationId)
        assertEquals("MAT", ref.bookId)
        assertEquals(5, ref.chapter)
        assertEquals(1, ref.verse)
    }

    @Test
    fun parseCanonicalWithVerse() {
        val ref = BibleReference.parseCanonical("JHN.3.16")
        assertNotNull(ref)
        assertEquals("JHN", ref.bookId)
        assertEquals(16, ref.verse)
    }

    @Test
    fun invalidReferenceReturnsNull() {
        assertNull(BibleReference.parse("not-a-ref"))
    }

    @Test
    fun invalidNumbersAndUnknownBooksReturnNullWithoutThrowing() {
        listOf("BSB:GEN.0.1", "BSB:GEN.1.0", "BSB:GEN.9999999999999.1",
            "BSB:GEN.51.1", "BSB:XYZ.1.1").forEach { assertNull(BibleReference.parse(it)) }
        listOf("GEN.0", "GEN.1.0", "GEN.9999999999999", "XYZ.1").forEach {
            assertNull(BibleReference.parseCanonical(it))
        }
    }

    @Test
    fun formatReference() {
        val ref = BibleReference("BSB", "PSA", 23, 1)
        assertEquals("BSB:PSA.23.1", ref.format())
    }
}
