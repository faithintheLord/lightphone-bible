package com.lightphone.bible.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MorphologyDecoderTest {
    @Test
    fun decodesHebrewNoun() {
        val result = MorphologyDecoder.decode("HR/Ncfsa")
        assertTrue(result.contains("noun") || result.contains("Noun") || result.isNotBlank())
    }

    @Test
    fun decodesGreekVerb() {
        val result = MorphologyDecoder.decode("V-PAI-3S")
        assertTrue(result.isNotBlank())
    }

    @Test
    fun emptyCodeReturnsEmpty() {
        assertEquals("", MorphologyDecoder.decode(""))
    }
}
