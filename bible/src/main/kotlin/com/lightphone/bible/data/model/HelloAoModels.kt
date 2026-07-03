package com.lightphone.bible.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

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
        ParsedVerse(
            number = block.number,
            text = block.content.orEmpty().toVersePlainText(),
        )
    }

fun List<JsonElement>.toVersePlainText(): String = buildString {
    for (element in this@toVersePlainText) {
        when (element) {
            is JsonPrimitive -> {
                if (element.isString) {
                    append(element.content)
                }
            }
            is JsonObject -> {
                element["text"]?.jsonPrimitive?.contentOrNull?.let(::append)
            }
            is JsonArray -> {
                append((element as List<JsonElement>).toVersePlainText())
            }
        }
    }
}

fun JsonElement.toVersePlainText(): String = when (this) {
    is JsonPrimitive -> if (isString) content else ""
    is JsonObject -> this["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
    is JsonArray -> joinToString(separator = "") { it.toVersePlainText() }
}

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
