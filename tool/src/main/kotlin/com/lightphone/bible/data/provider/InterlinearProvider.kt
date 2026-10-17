package com.lightphone.bible.data.provider

import com.lightphone.bible.data.api.BsbInterlinearApi
import com.lightphone.bible.data.api.BsbInterlinearVerse
import com.lightphone.bible.data.api.BsbWordToken
import kotlinx.coroutines.flow.Flow

interface InterlinearProvider {
    val providerId: String

    fun fetchBookVerses(bookId: String): Flow<BsbInterlinearVerse>

    fun parseWordTokens(verse: BsbInterlinearVerse): List<BsbWordToken>
}

class BsbDataOutputInterlinearProvider(
    private val api: BsbInterlinearApi = BsbInterlinearApi(),
) : InterlinearProvider {
    override val providerId: String = "bsb-data-output"

    override fun fetchBookVerses(bookId: String): Flow<BsbInterlinearVerse> =
        api.fetchBookVerses(bookId)

    override fun parseWordTokens(verse: BsbInterlinearVerse): List<BsbWordToken> =
        api.parseWordTokens(verse)
}
