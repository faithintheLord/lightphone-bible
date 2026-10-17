package com.lightphone.bible.data.api

import com.lightphone.bible.data.model.BibleBooks
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class BsbInterlinearVerse(
    val b: String,
    val c: Int,
    val v: Int,
    val w: List<List<String?>> = emptyList(),
)

data class BsbWordToken(val text: String, val strongsNumber: String?)

class BsbInterlinearApi(
    private val client: HttpClient = HttpClient(OkHttp) {
        install(HttpTimeout) {
            connectTimeoutMillis = 20_000
            requestTimeoutMillis = 60_000
        }
    },
) {
    private val baseUrl =
        "https://raw.githubusercontent.com/BSB-publishing/bsb-data-output/main/base/display"

    fun fetchBookVerses(bookId: String): Flow<BsbInterlinearVerse> = flow {
        require(BibleBooks.find(bookId) != null) { "Unknown Bible book: $bookId" }
        // Published display data has one JSON file per chapter, not book-level JSONL.
        for (chapters in (1..BibleBooks.chapterCount(bookId)).chunked(4)) {
            val batches = coroutineScope {
                chapters.map { chapter ->
                    async {
                        val response = client.get("$baseUrl/$bookId/$bookId$chapter.json")
                        check(response.status.isSuccess()) {
                            "Word-study download failed for $bookId $chapter (HTTP ${response.status.value})"
                        }
                        parseDisplayChapter(bookId, chapter, response.bodyAsText())
                    }
                }.awaitAll()
            }
            batches.flatten().forEach { emit(it) }
        }
    }

    fun parseWordTokens(verse: BsbInterlinearVerse): List<BsbWordToken> =
        verse.w.mapNotNull { pair ->
            val text = pair.getOrNull(0) ?: return@mapNotNull null
            BsbWordToken(text, pair.getOrNull(1)?.takeIf { it.isNotBlank() })
        }

    fun close() = client.close()
}

internal fun parseDisplayChapter(bookId: String, chapter: Int, text: String): List<BsbInterlinearVerse> {
    val root = Json.parseToJsonElement(text) as? JsonObject ?: error("Invalid word-study chapter")
    val english = root["eng"] as? JsonObject ?: error("Word-study chapter has no English text")
    require(english.isNotEmpty()) { "Word-study chapter is empty" }
    return english.map { (number, element) ->
        val verse = number.toIntOrNull()?.takeIf { it > 0 } ?: error("Invalid word-study verse: $number")
        val words = element as? JsonArray ?: error("Invalid word-study tokens")
        BsbInterlinearVerse(bookId, chapter, verse, words.map { word ->
            val pair = word as? JsonArray ?: error("Invalid word-study token")
            val content = (pair.getOrNull(0) as? JsonPrimitive)?.contentOrNull
                ?: error("Missing word-study text")
            val strongs = (pair.getOrNull(1) as? JsonPrimitive)?.contentOrNull
            listOf(content, strongs)
        })
    }.sortedBy { it.v }
}
