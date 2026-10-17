package com.lightphone.bible.data

import com.lightphone.bible.data.model.toVersePlainText
import com.lightphone.bible.data.model.HelloAoChapter
import com.lightphone.bible.data.model.parseVerses
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HelloAoPlainTextTest {
    private fun parse(content: String) = Json.parseToJsonElement(content).toVersePlainText()

    @Test
    fun webLukeNoteOnlyVerseIsOmittedWithoutRenumberingScripture() {
        // Actual ENGWEBP Luke 17:36 contains only noteId 37.
        val chapter = Json.decodeFromString<HelloAoChapter>("""
            {"number":17,"content":[
              {"type":"verse","number":35,"content":[{"text":"There will be two grinding grain together. One will be taken and the other will be left.”","wordsOfJesus":true}]},
              {"type":"verse","number":36,"content":[{"noteId":37}]},
              {"type":"verse","number":37,"content":["They, answering, asked him, “Where, Lord?” He said to them,",{"text":"“Where the body is, there the vultures will also be gathered together.”","wordsOfJesus":true}]}
            ]}
        """.trimIndent())
        val verses = chapter.parseVerses()
        assertEquals(listOf(35, 37), verses.map { it.number })
        assertTrue(verses.all { it.text.isNotBlank() })
        assertTrue(verses.last().text.contains("Where the body is"))
    }

    @Test
    fun footnoteBoundariesKeepGenesisWordsAndSentencesApart() {
        assertEquals(
            "And God said, “Let there be light,” and there was light.",
            parse("""["And God said, “Let there be light,”",{"noteId":0},"and there was light."]"""),
        )
        assertEquals("an expanse between the waters", parse("""["an expanse",{"noteId":2},"between the waters"]"""))
    }

    @Test
    fun markersUseOnlyOneSeparatorAndPreserveExistingWhitespace() {
        assertEquals("word next", parse("""["word",{"noteId":1},{"noteId":2},"next"]"""))
        assertEquals("word next", parse("""["word ",{"noteId":1},"next"]"""))
        assertEquals("word next", parse("""["word",{"heading":"Not scripture"}," next"]"""))
        assertEquals("word", parse("""[{"noteId":0},"word",{"noteId":1}]"""))
    }

    @Test
    fun inlineFormattingPreservesSourceSpacingAndPunctuation() {
        assertEquals("Jesus said, “Follow me!”", parse("""
            ["Jesus said, ",{"text":"“Follow me","wordsOfJesus":true},"!”"]
        """.trimIndent()))
        assertEquals("The wind blows", parse("""
            [{"text":"The wind","wordsOfJesus":true},{"noteId":11},{"text":"blows","wordsOfJesus":true}]
        """.trimIndent()))
    }

    @Test
    fun poetryAndLineBreaksRetainLinesWithoutDuplicateBlankLines() {
        assertEquals("The LORD is my shepherd;\nI shall not want.", parse("""
            [{"text":"The LORD is my shepherd;","poem":1},{"noteId":50},{"text":"I shall not want.","poem":2}]
        """.trimIndent()))
        assertEquals("First\nSecond", parse("""
            [{"lineBreak":true},"First  ",{"lineBreak":true},{"lineBreak":true},{"text":"Second","poem":1},{"lineBreak":true}]
        """.trimIndent()))
    }

    @Test
    fun realBsbAndWebVersesMatchPublishedSimplifiedText() {
        val text = checkNotNull(javaClass.getResourceAsStream("/helloao-verse-fixtures.json"))
            .bufferedReader().use { it.readText() }
        val fixtures = Json.parseToJsonElement(text).jsonArray
        assertTrue(fixtures.size > 500)
        fixtures.forEach { fixture ->
            val record = fixture.jsonObject
            assertEquals(
                record.getValue("expected").jsonPrimitive.content,
                record.getValue("content").toVersePlainText(),
                record.getValue("reference").jsonPrimitive.content,
            )
        }
    }
}
