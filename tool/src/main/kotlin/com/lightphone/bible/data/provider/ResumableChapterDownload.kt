package com.lightphone.bible.data.provider

import com.lightphone.bible.data.model.HelloAoBook
import com.lightphone.bible.data.model.ParsedVerse

/** Each successful save is a checkpoint; a failed request never replaces stored content. */
internal suspend fun downloadMissingChapters(
    books: List<HelloAoBook>,
    hasChapter: suspend (String, Int) -> Boolean,
    fetchChapter: suspend (String, Int) -> List<ParsedVerse>,
    saveChapter: suspend (String, Int, List<ParsedVerse>) -> Unit,
) {
    require(books.isNotEmpty()) { "The translation contains no books" }
    require(books.map { it.id }.distinct().size == books.size) { "Duplicate Bible books" }
    for (book in books) {
        require(book.numberOfChapters > 0) { "${book.id} contains no chapters" }
        for (chapter in 1..book.numberOfChapters) {
            if (hasChapter(book.id, chapter)) continue
            val verses = fetchChapter(book.id, chapter)
            require(verses.isNotEmpty()) { "${book.id} $chapter contains no verses" }
            require(verses.all { it.number > 0 && it.text.isNotBlank() }) {
                "Invalid verse in ${book.id} $chapter"
            }
            require(verses.map { it.number }.distinct().size == verses.size) {
                "Duplicate verse in ${book.id} $chapter"
            }
            saveChapter(book.id, chapter, verses)
        }
    }
}
