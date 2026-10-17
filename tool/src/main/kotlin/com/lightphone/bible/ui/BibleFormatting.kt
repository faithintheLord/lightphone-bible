package com.lightphone.bible.ui

import com.lightphone.bible.data.model.BibleBooks
import com.lightphone.bible.data.model.BibleReference

fun formatReference(reference: BibleReference): String {
    val book = BibleBooks.find(reference.bookId)?.name ?: reference.bookId
    return "$book ${reference.chapter}:${reference.verse}"
}

fun formatChapterReference(bookId: String, chapter: Int): String {
    val book = BibleBooks.find(bookId)?.name ?: bookId
    return "$book $chapter"
}

fun formatCanonical(reference: BibleReference): String =
    "${reference.bookId}.${reference.chapter}.${reference.verse}"
