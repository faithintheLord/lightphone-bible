package com.lightphone.bible.ui.reader

import com.lightphone.bible.data.db.VerseEntity
import com.lightphone.bible.data.db.WordTokenEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReaderTextTest {
    @Test
    fun `word study preserves Scripture punctuation spacing and text missing from tokens`() {
        val display = display("God said, “Let there be light!”", listOf("God" to "H430", "said" to "H559", "light" to "H216"))
        val annotated = buildTokenAnnotatedString(display)
        assertEquals("3 God said, “Let there be light!”", annotated.text)
        assertEquals(listOf("God", "said", "light"), annotated.getStringAnnotations("strongs", 0, annotated.length)
            .map { annotated.text.substring(it.start, it.end) })
    }

    @Test
    fun `spacing tokens from current interlinear feed do not add duplicate spaces`() {
        val display = display("In the beginning, God created.", listOf(
            "In" to "H7225", " " to null, "the" to null, " " to null,
            "beginning" to null, ", " to null, "God" to "H430", " " to null,
            "created" to "H1254", "." to null,
        ))
        assertEquals("3 In the beginning, God created.", buildTokenAnnotatedString(display).text)
    }

    @Test
    fun `repeated words receive their own Strong's references`() {
        val display = display("Love love", listOf("Love" to "G25", "love" to "G26"))
        val annotated = buildTokenAnnotatedString(display)
        val annotations = annotated.getStringAnnotations("strongs", 0, annotated.length)
        assertEquals(listOf("G25", "G26"), annotations.map { it.item })
        assertEquals(listOf(2, 7), annotations.map { it.start })
    }

    @Test
    fun `unmatched interlinear text never replaces the downloaded verse`() {
        val display = display("The actual verse.", listOf("Different" to "G1", "source" to "G2"))
        val annotated = buildTokenAnnotatedString(display)
        assertEquals("3 The actual verse.", annotated.text)
        assertTrue(annotated.getStringAnnotations("strongs", 0, annotated.length).isEmpty())
    }

    private fun display(text: String, tokens: List<Pair<String, String?>>): VerseDisplay = VerseDisplay(
        verse = VerseEntity(translationId = "BSB", bookId = "GEN", chapter = 1, verse = 3, text = text),
        tokens = tokens.mapIndexed { index, (word, strongs) ->
            WordTokenEntity(
                translationId = "BSB", bookId = "GEN", chapter = 1, verse = 3,
                tokenIndex = index, text = word, strongsNumber = strongs,
            )
        },
    )
}
