package com.lightphone.bible.data.provider

import com.lightphone.bible.data.api.HelloAoApi
import com.lightphone.bible.data.api.HelloAoChapterPayload
import com.lightphone.bible.data.model.HelloAoBook
import com.lightphone.bible.data.model.ParsedVerse
import kotlinx.coroutines.flow.Flow

interface BibleContentProvider {
    val providerId: String

    suspend fun fetchBooks(providerTranslationId: String): List<HelloAoBook>

    suspend fun fetchChapterVerses(
        providerTranslationId: String,
        bookId: String,
        chapter: Int,
    ): List<ParsedVerse>

    fun streamCompleteTranslation(providerTranslationId: String): Flow<HelloAoChapterPayload>
}

class HelloAoContentProvider(
    private val api: HelloAoApi = HelloAoApi(),
) : BibleContentProvider {
    override val providerId: String = "helloao"

    override suspend fun fetchBooks(providerTranslationId: String): List<HelloAoBook> =
        api.fetchBooksList(providerTranslationId)

    override suspend fun fetchChapterVerses(
        providerTranslationId: String,
        bookId: String,
        chapter: Int,
    ): List<ParsedVerse> = api.fetchChapterVerses(providerTranslationId, bookId, chapter)

    override fun streamCompleteTranslation(providerTranslationId: String): Flow<HelloAoChapterPayload> =
        api.fetchCompleteTranslation(providerTranslationId)
}
