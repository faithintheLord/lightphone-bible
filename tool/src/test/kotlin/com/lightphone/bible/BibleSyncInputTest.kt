package com.lightphone.bible

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BibleSyncInputTest {
    @Test
    fun acceptsPlainAndSdkEntryEncodedTranslationIds() {
        assertEquals("WEB", decodeTranslationInput("WEB"))
        assertEquals("WEB", decodeTranslationInput("translationId=WEB"))
        assertNull(decodeTranslationInput(null))
        assertNull(decodeTranslationInput(""))
    }
}
