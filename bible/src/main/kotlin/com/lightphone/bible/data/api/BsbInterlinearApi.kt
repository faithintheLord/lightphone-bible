package com.lightphone.bible.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BsbInterlinearVerse(
    val b: String,
    val c: Int,
    val v: Int,
    val w: List<List<String?>> = emptyList(),
    val citations: List<String>? = null,
)

data class BsbWordToken(
    val text: String,
    val strongsNumber: String?,
)

class BsbInterlinearApi(
    private val client: HttpClient = HttpClient(OkHttp),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val baseUrl =
        "https://raw.githubusercontent.com/BSB-publishing/bsb-data-output/main/base"

    suspend fun fetchBookJsonl(bookId: String): Result<String> = runCatching {
        val primaryUrl = "$baseUrl/strongs/$bookId.jsonl"
        var response = client.get(primaryUrl)
        if (!response.status.isSuccess()) {
            response = client.get("$baseUrl/display/$bookId.jsonl")
        }
        if (!response.status.isSuccess()) {
            val body = response.bodyAsText().take(500)
            throw IllegalStateException("BSB interlinear HTTP ${response.status.value}: $body")
        }
        response.bodyAsText()
    }

    fun fetchBookVerses(bookId: String): Flow<BsbInterlinearVerse> = flow {
        val text = fetchBookJsonl(bookId).getOrThrow()
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            emit(json.decodeFromString<BsbInterlinearVerse>(trimmed))
        }
    }

    fun parseWordTokens(verse: BsbInterlinearVerse): List<BsbWordToken> =
        verse.w.mapNotNull { pair ->
            val text = pair.getOrNull(0)?.takeIf { !it.isNullOrBlank() } ?: return@mapNotNull null
            BsbWordToken(text = text, strongsNumber = pair.getOrNull(1))
        }

    fun close() {
        client.close()
    }
}
