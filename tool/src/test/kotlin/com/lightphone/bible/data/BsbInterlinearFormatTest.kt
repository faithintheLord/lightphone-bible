package com.lightphone.bible.data

import com.lightphone.bible.data.api.parseDisplayChapter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BsbInterlinearFormatTest {
    @Test
    fun readsCurrentDisplayFormatIncludingSpacingPunctuationAndMetadata() {
        val verses = parseDisplayChapter("JHN", 1, """
            {"eng":{"2":[["He","G3778"],[".",null]],
            "1":[["In","G1722"],[" ",null],["the beginning","G746",{"supplied":true}],
            [", ",null],["","G3588",{"elided":true}],["God","G2316"],[".",null]]},
            "unused":{"metadata":true}}
        """.trimIndent())
        assertEquals(listOf(1, 2), verses.map { it.v })
        assertEquals("JHN", verses.first().b)
        assertEquals("In the beginning, God.", verses.first().w.joinToString("") { it[0].orEmpty() })
        assertEquals("G746", verses.first().w[2][1])
        assertEquals("G3588", verses.first().w[4][1])
    }

    @Test
    fun missingOrEmptyEnglishDataFailsInsteadOfMarkingBookDownloaded() {
        assertFailsWith<IllegalStateException> { parseDisplayChapter("JHN", 1, "{}") }
        assertFailsWith<IllegalArgumentException> { parseDisplayChapter("JHN", 1, """{"eng":{}}""") }
    }
}
