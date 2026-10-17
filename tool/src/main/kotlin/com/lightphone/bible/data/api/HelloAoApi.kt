package com.lightphone.bible.data.api

import com.lightphone.bible.data.model.HelloAoBook
import com.lightphone.bible.data.model.HelloAoBooksResponse
import com.lightphone.bible.data.model.HelloAoChapter
import com.lightphone.bible.data.model.HelloAoChapterResponse
import com.lightphone.bible.data.model.HelloAoCompleteResponse
import com.lightphone.bible.data.model.ParsedVerse
import com.lightphone.bible.data.model.parseVerses
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

data class HelloAoChapterPayload(
    val bookId: String,
    val chapterNumber: Int,
    val verses: List<ParsedVerse>,
)

class HelloAoApi(
    private val client: HttpClient = defaultClient(),
) {
    private val baseUrl = "https://bible.helloao.org/api"

    suspend fun fetchBooks(providerTranslationId: String): Result<HelloAoBooksResponse> = runCatching {
        val response = client.get("$baseUrl/$providerTranslationId/books.json")
        if (!response.status.isSuccess()) {
            val body = response.bodyAsText().take(500)
            throw IllegalStateException("HelloAO books HTTP ${response.status.value}: $body")
        }
        response.body()
    }

    suspend fun fetchChapter(
        providerTranslationId: String,
        bookId: String,
        chapter: Int,
    ): Result<HelloAoChapterResponse> = runCatching {
        val response = client.get("$baseUrl/$providerTranslationId/$bookId/$chapter.json")
        if (!response.status.isSuccess()) {
            val body = response.bodyAsText().take(500)
            throw IllegalStateException("HelloAO chapter HTTP ${response.status.value}: $body")
        }
        response.body()
    }

    fun fetchCompleteTranslation(providerTranslationId: String): Flow<HelloAoChapterPayload> = flow {
        val response = client.get("$baseUrl/$providerTranslationId/complete.json")
        if (!response.status.isSuccess()) {
            val body = response.bodyAsText().take(500)
            throw IllegalStateException("HelloAO complete HTTP ${response.status.value}: $body")
        }
        val complete: HelloAoCompleteResponse = response.body()
        for (book in complete.books) {
            for (entry in book.chapters) {
                emit(
                    HelloAoChapterPayload(
                        bookId = book.id,
                        chapterNumber = entry.chapter.number,
                        verses = entry.chapter.parseVerses(),
                    ),
                )
            }
        }
    }

    suspend fun fetchBooksList(providerTranslationId: String): List<HelloAoBook> =
        fetchBooks(providerTranslationId).getOrThrow().books

    suspend fun fetchChapterVerses(
        providerTranslationId: String,
        bookId: String,
        chapter: Int,
    ): List<ParsedVerse> =
        fetchChapter(providerTranslationId, bookId, chapter).getOrThrow().chapter.parseVerses()

    fun close() {
        client.close()
    }

    companion object {
        private fun defaultClient(): HttpClient {
            val json = Json { ignoreUnknownKeys = true }
            return HttpClient(OkHttp) {
                install(HttpTimeout) {
                    connectTimeoutMillis = 20_000
                    requestTimeoutMillis = 60_000
                }
                install(ContentNegotiation) {
                    json(json)
                }
            }
        }
    }
}
