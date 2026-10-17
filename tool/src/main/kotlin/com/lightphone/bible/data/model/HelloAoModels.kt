package com.lightphone.bible.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

const val VERSE_TEXT_NORMALIZATION_VERSION = 1

@Serializable
data class HelloAoBooksResponse(
    val translation: HelloAoTranslationMeta,
    val books: List<HelloAoBook> = emptyList(),
)

@Serializable
data class HelloAoTranslationMeta(
    val id: String,
    val name: String,
    val shortName: String,
    val language: String,
    val numberOfBooks: Int = 0,
    val totalNumberOfChapters: Int = 0,
    val totalNumberOfVerses: Int = 0,
)

@Serializable
data class HelloAoBook(
    val id: String,
    val translationId: String,
    val name: String,
    val commonName: String,
    val title: String,
    val order: Int,
    val numberOfChapters: Int,
    val totalNumberOfVerses: Int,
)

@Serializable
data class HelloAoChapterResponse(
    val translation: HelloAoTranslationMeta,
    val book: HelloAoBook,
    val chapter: HelloAoChapter,
    val numberOfVerses: Int = 0,
)

@Serializable
data class HelloAoChapter(
    val number: Int,
    val content: List<HelloAoChapterContent> = emptyList(),
)

@Serializable
data class HelloAoChapterContent(
    val type: String,
    val number: Int? = null,
    val content: List<JsonElement>? = null,
)

@Serializable
data class HelloAoCompleteResponse(
    val translation: HelloAoTranslationMeta,
    val books: List<HelloAoCompleteBook> = emptyList(),
)

@Serializable
data class HelloAoCompleteBook(
    val id: String,
    val name: String,
    val commonName: String,
    val title: String,
    val order: Int,
    val numberOfChapters: Int,
    val totalNumberOfVerses: Int,
    val chapters: List<HelloAoCompleteChapterEntry> = emptyList(),
)

@Serializable
data class HelloAoCompleteChapterEntry(
    val numberOfVerses: Int = 0,
    val chapter: HelloAoChapter,
)

data class ParsedVerse(
    val number: Int,
    val text: String,
)

fun HelloAoChapter.parseVerses(): List<ParsedVerse> =
    content.mapNotNull { block ->
        if (block.type != "verse" || block.number == null) return@mapNotNull null
        // Some translations retain only an editorial footnote at an omitted verse number.
        // Keep the surrounding verse numbers unchanged and do not invent scripture text.
        val text = block.content.orEmpty().toVersePlainText()
        if (text.isBlank()) return@mapNotNull null
        ParsedVerse(
            number = block.number,
            text = text,
        )
    }

/**
 * Matches HelloAO's simplified verse semantics: preserve text fragments, separate omitted
 * markers when needed, and retain poetry/line breaks. Do not insert spaces into formatted
 * text boundaries or reconstruct punctuation.
 * https://github.com/HelloAOLab/bible-api/blob/main/packages/helloao-tools/generation/simple.ts
 */
fun List<JsonElement>.toVersePlainText(): String {
    val parts = flatMap { if (it is JsonArray) it.toList() else listOf(it) }
    return buildString {
        fun newLine() {
            if (isEmpty() || last() == '\n') return
            while (isNotEmpty() && last() != '\n' && last().isVerseWhitespace()) {
                setLength(length - 1)
            }
            append('\n')
        }

        parts.forEachIndexed { index, part ->
            when (part) {
                is JsonPrimitive -> if (part.isString) append(part.content)
                is JsonObject -> {
                    val text = part.verseText()
                    when {
                        text != null -> {
                            if (part.isPoem()) newLine()
                            append(text)
                        }
                        "lineBreak" in part -> newLine()
                        "noteId" in part || "heading" in part -> {
                            val next = parts.getOrNull(index + 1)
                            val nextText = when (next) {
                                is JsonPrimitive -> next.takeIf { it.isString }?.content
                                is JsonObject -> next.takeUnless { it.isPoem() }?.verseText()
                                else -> null
                            }
                            if (isNotEmpty() && !last().isVerseWhitespace() &&
                                !nextText.isNullOrEmpty() && !nextText.first().isVerseWhitespace()
                            ) append(' ')
                        }
                    }
                }
                else -> Unit
            }
        }
    }.trim { it.isVerseWhitespace() }
}

private fun JsonObject.verseText(): String? =
    (this["text"] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

private fun JsonObject.isPoem(): Boolean =
    (this["poem"] as? JsonPrimitive)?.let { !it.isString && it.doubleOrNull != null } == true

private fun Char.isVerseWhitespace(): Boolean = isWhitespace() || this == '\uFEFF'

fun JsonElement.toVersePlainText(): String =
    (if (this is JsonArray) toList() else listOf(this)).toVersePlainText()

@Serializable
data class HelloAoReference(
    val translationId: String,
    val book: String,
    val chapter: Int,
    val verse: Int? = null,
) {
    fun toBibleReference(): BibleReference = BibleReference(
        translationId = translationId,
        bookId = book,
        chapter = chapter,
        verse = verse ?: 1,
    )
}

fun HelloAoReference.format(): String =
    if (verse != null) {
        "$translationId:$book.$chapter.$verse"
    } else {
        "$translationId:$book.$chapter"
    }
