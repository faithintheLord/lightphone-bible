package com.lightphone.bible.repository

import com.lightphone.bible.data.catalog.TranslationRegistry
import com.lightphone.bible.data.db.BibleDatabase
import com.lightphone.bible.data.db.BookEntity
import com.lightphone.bible.data.db.BookmarkEntity
import com.lightphone.bible.data.db.HighlightEntity
import com.lightphone.bible.data.db.NoteEntity
import com.lightphone.bible.data.db.PlanProgressEntity
import com.lightphone.bible.data.db.ReadingProgressEntity
import com.lightphone.bible.data.db.StrongsEntryEntity
import com.lightphone.bible.data.db.TranslationEntity
import com.lightphone.bible.data.db.VerseEntity
import com.lightphone.bible.data.db.WordTokenEntity
import com.lightphone.bible.data.model.BibleReference
import com.lightphone.bible.data.model.TranslationEntry
import com.lightphone.bible.data.model.VERSE_TEXT_NORMALIZATION_VERSION
import com.lightphone.bible.data.provider.BibleContentProvider
import com.lightphone.bible.data.provider.InterlinearProvider
import androidx.room.withTransaction
import com.thelightphone.sdk.SealedLightContext
import com.lightphone.bible.data.provider.downloadMissingChapters
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class ReadingPlan(
    val id: String,
    val name: String,
    val description: String,
    val dayCount: Int,
    val referencesByDay: List<List<BibleReference>>,
)

class BibleContentRepository(
    private val database: BibleDatabase,
    private val registry: TranslationRegistry,
    private val preferences: BiblePreferences,
) {
    private val translationDao = database.translationDao()
    private val bookDao = database.bookDao()
    private val verseDao = database.verseDao()

    suspend fun ensureCatalogInDatabase() = database.withTransaction {
        val catalog = registry.getCatalog()
        translationDao.upsertAll(
            catalog.translations.map { entry ->
                val existing = translationDao.getById(entry.id)
                TranslationEntity(
                    id = entry.id,
                    providerTranslationId = entry.providerTranslationId,
                    displayName = entry.displayName,
                    language = entry.language,
                    provider = entry.provider,
                    supportsWordStudy = entry.supportsWordStudy,
                    interlinearProvider = entry.interlinearProvider,
                    isDownloaded = existing?.isDownloaded ?: false,
                    downloadedAtEpochMs = existing?.downloadedAtEpochMs,
                )
            },
        )
    }

    suspend fun getTranslations(): List<TranslationEntity> = translationDao.getAll()

    fun observeTranslations() = translationDao.observeAll().distinctUntilChanged()

    suspend fun getTranslation(id: String): TranslationEntity? = translationDao.getById(id)

    suspend fun getBooks(translationId: String): List<BookEntity> =
        bookDao.getBooksForTranslation(translationId)

    suspend fun getChapter(
        translationId: String,
        bookId: String,
        chapter: Int,
    ): List<VerseEntity> = verseDao.getChapter(translationId, bookId, chapter)

    suspend fun getVerse(reference: BibleReference): VerseEntity? =
        verseDao.getVerse(
            translationId = reference.translationId,
            bookId = reference.bookId,
            chapter = reference.chapter,
            verse = reference.verse,
        )

    suspend fun syncTranslation(entry: TranslationEntry, provider: BibleContentProvider) {
        val books = provider.fetchBooks(entry.providerTranslationId)
        val normalizedText = preferences.getTextNormalizationProgress(entry.id)
        bookDao.upsertAll(
            books.map { book ->
                BookEntity(
                    translationId = entry.id,
                    bookId = book.id,
                    name = book.name,
                    commonName = book.commonName,
                    sortOrder = book.order,
                    chapterCount = book.numberOfChapters,
                    verseCount = book.totalNumberOfVerses,
                )
            },
        )

        downloadMissingChapters(
            books = books,
            hasChapter = { bookId, chapter ->
                normalizedText.contains(bookId, chapter) &&
                    verseDao.getChapter(entry.id, bookId, chapter).isNotEmpty()
            },
            fetchChapter = { bookId, chapter ->
                provider.fetchChapterVerses(entry.providerTranslationId, bookId, chapter)
            },
            saveChapter = { bookId, chapter, verses ->
                database.withTransaction {
                    verseDao.deleteChapter(entry.id, bookId, chapter)
                    verseDao.insertAll(verses.map { parsed ->
                        VerseEntity(
                            translationId = entry.id,
                            bookId = bookId,
                            chapter = chapter,
                            verse = parsed.number,
                            text = parsed.text,
                        )
                    })
                }
                // Commit first, then checkpoint. A crash between them only repeats this chapter.
                preferences.markTextChapterNormalized(entry.id, bookId, chapter)
            },
        )

        translationDao.markDownloaded(
            id = entry.id,
            downloaded = true,
            downloadedAt = System.currentTimeMillis(),
        )
        preferences.markTranslationTextNormalized(entry.id)
    }

    suspend fun isTranslationComplete(translationId: String): Boolean {
        val translation = translationDao.getById(translationId) ?: return false
        if (!translation.isDownloaded) return false
        if (preferences.getTextNormalizationVersion(translationId) != VERSE_TEXT_NORMALIZATION_VERSION) return false
        val books = bookDao.getBooksForTranslation(translationId)
        return books.isNotEmpty() && books.all { book ->
            verseDao.countChapters(translationId, book.bookId, book.chapterCount) == book.chapterCount
        }
    }

    fun contentProviderFor(entry: TranslationEntry): BibleContentProvider? =
        registry.contentProviderFor(entry)
}

class UserDataRepository(
    private val database: BibleDatabase,
) {
    private val bookmarkDao = database.bookmarkDao()
    private val highlightDao = database.highlightDao()
    private val noteDao = database.noteDao()
    private val readingProgressDao = database.readingProgressDao()

    suspend fun addBookmark(reference: BibleReference): Long =
        bookmarkDao.insert(
            BookmarkEntity(
                translationId = reference.translationId,
                bookId = reference.bookId,
                chapter = reference.chapter,
                verse = reference.verse,
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )

    suspend fun removeBookmark(id: Long) {
        bookmarkDao.deleteById(id)
    }

    suspend fun getBookmarks(): List<BookmarkEntity> = bookmarkDao.getAll()

    suspend fun addHighlight(
        reference: BibleReference,
        verseEnd: Int,
        colorArgb: Int,
    ): Long = database.withTransaction {
        require(verseEnd >= reference.verse)
        highlightDao.deleteOverlapping(
            reference.translationId, reference.bookId, reference.chapter, reference.verse, verseEnd,
        )
        highlightDao.insert(
            HighlightEntity(
                translationId = reference.translationId,
                bookId = reference.bookId,
                chapter = reference.chapter,
                verseStart = reference.verse,
                verseEnd = verseEnd,
                colorArgb = colorArgb,
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun removeHighlight(id: Long) = highlightDao.deleteById(id)

    suspend fun removeNote(reference: BibleReference) {
        getNote(reference)?.let { noteDao.deleteById(it.id) }
    }

    suspend fun getHighlightsForChapter(
        translationId: String,
        bookId: String,
        chapter: Int,
    ): List<HighlightEntity> = highlightDao.getForChapter(translationId, bookId, chapter)

    suspend fun getHighlights(): List<HighlightEntity> = highlightDao.getAll()

    suspend fun upsertNote(reference: BibleReference, note: String): Long =
        noteDao.upsert(
            NoteEntity(
                translationId = reference.translationId,
                bookId = reference.bookId,
                chapter = reference.chapter,
                verse = reference.verse,
                note = note,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )

    suspend fun getNote(reference: BibleReference): NoteEntity? =
        noteDao.getForVerse(
            translationId = reference.translationId,
            bookId = reference.bookId,
            chapter = reference.chapter,
            verse = reference.verse,
        )

    suspend fun getNotes(): List<NoteEntity> = noteDao.getAll()

    suspend fun saveReadingProgress(reference: BibleReference) {
        readingProgressDao.upsert(
            ReadingProgressEntity(
                translationId = reference.translationId,
                bookId = reference.bookId,
                chapter = reference.chapter,
                verse = reference.verse,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun getReadingProgress(translationId: String): ReadingProgressEntity? =
        readingProgressDao.get(translationId)
}

class SearchRepository(
    private val database: BibleDatabase,
) {
    private val verseDao = database.verseDao()

    suspend fun search(
        translationId: String,
        query: String,
        limit: Int = 100,
    ): List<VerseEntity> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        return verseDao.searchByText(
            translationId = translationId,
            pattern = escapeLikePattern(trimmed),
            limit = limit,
        )
    }

    private fun escapeLikePattern(raw: String): String =
        raw.replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
}

class StrongsRepository(
    private val context: SealedLightContext,
    private val database: BibleDatabase,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val strongsDao = database.strongsDao()

    suspend fun ensureLoaded() {
        if (strongsDao.count() > 0) return
        val greek = loadDictionary("strongs/greek.json")
        val hebrew = loadDictionary("strongs/hebrew.json")
        val entries = buildList {
            addAll(greek.map { (number, obj) -> obj.toEntity(number, "greek") })
            addAll(hebrew.map { (number, obj) -> obj.toEntity(number, "hebrew") })
        }
        strongsDao.insertAll(entries)
    }

    suspend fun lookup(number: String): StrongsEntryEntity? {
        ensureLoaded()
        return strongsDao.getByNumber(number)
    }

    suspend fun findVerses(
        strongsNumber: String,
        limit: Int = 200,
    ): List<BibleReference> {
        ensureLoaded()
        return database.wordTokenDao().findVersesByStrongs(strongsNumber, limit).map { ref ->
            BibleReference(
                translationId = ref.translationId,
                bookId = ref.bookId,
                chapter = ref.chapter,
                verse = ref.verse,
            )
        }
    }

    private fun loadDictionary(assetPath: String): Map<String, JsonObject> {
        val text = context.readAsset(assetPath).decodeToString()
        val root = json.decodeFromString<Map<String, JsonObject>>(text)
        return root
    }

    private fun JsonObject.toEntity(number: String, language: String): StrongsEntryEntity =
        StrongsEntryEntity(
            number = number,
            language = language,
            lemma = stringField("lemma"),
            translit = stringField("translit"),
            pronunciation = stringField("pron") ?: stringField("xlit"),
            definition = stringField("strongs_def"),
            derivation = stringField("derivation"),
            kjvDef = stringField("kjv_def"),
        )

    private fun JsonObject.stringField(name: String): String? =
        this[name]?.jsonPrimitive?.contentOrNull
}

class InterlinearRepository(
    private val database: BibleDatabase,
    private val registry: TranslationRegistry,
    private val preferences: BiblePreferences,
) {
    private val wordTokenDao = database.wordTokenDao()

    suspend fun syncBook(
        translationId: String,
        bookId: String,
        provider: InterlinearProvider,
    ) {
        if (isBookComplete(translationId, bookId)) return
        val bookTokens = mutableListOf<WordTokenEntity>()
        provider.fetchBookVerses(bookId).collect { verse ->
            require(verse.b == bookId && verse.c > 0 && verse.v > 0) { "Invalid word-study reference" }
            val tokens = provider.parseWordTokens(verse)
            require(tokens.isNotEmpty()) { "No word-study text for $bookId ${verse.c}:${verse.v}" }
            bookTokens += tokens.mapIndexed { index, token ->
                WordTokenEntity(
                    translationId = translationId,
                    bookId = verse.b,
                    chapter = verse.c,
                    verse = verse.v,
                    tokenIndex = index,
                    text = token.text,
                    strongsNumber = token.strongsNumber,
                )
            }
        }
        require(bookTokens.isNotEmpty()) { "No word-study data for $bookId" }
        database.withTransaction {
            wordTokenDao.deleteForBook(translationId, bookId)
            wordTokenDao.insertAll(bookTokens)
        }
        // Set only after the complete book has committed. Interrupted books remain retryable.
        preferences.markInterlinearBookComplete(translationId, bookId)
    }

    private suspend fun isBookComplete(translationId: String, bookId: String): Boolean =
        "$translationId:$bookId" in preferences.getCompletedInterlinearBooks()

    suspend fun getWordTokens(
        translationId: String,
        bookId: String,
        chapter: Int,
        verse: Int,
    ): List<WordTokenEntity> =
        wordTokenDao.getForVerse(translationId, bookId, chapter, verse)

    suspend fun hasInterlinear(translationId: String): Boolean {
        val books = database.bookDao().getBooksForTranslation(translationId)
        val completed = preferences.getCompletedInterlinearBooks()
        return books.isNotEmpty() && books.all { "$translationId:${it.bookId}" in completed }
    }

    fun interlinearProviderFor(entry: TranslationEntry): InterlinearProvider? =
        registry.interlinearProviderFor(entry)
}

class PlansRepository(
    private val context: SealedLightContext,
    private val database: BibleDatabase,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val planProgressDao = database.planProgressDao()

    private val builtInPlans: List<ReadingPlan> by lazy { loadPlansFromAssets() }

    fun getPlans(): List<ReadingPlan> = builtInPlans

    fun getPlan(planId: String): ReadingPlan? =
        builtInPlans.find { it.id == planId }

    suspend fun getProgress(planId: String): List<PlanProgressEntity> =
        planProgressDao.getForPlan(planId)

    suspend fun markDayComplete(planId: String, dayIndex: Int, completed: Boolean) {
        planProgressDao.upsert(
            PlanProgressEntity(
                planId = planId,
                dayIndex = dayIndex,
                completed = completed,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun getDayReferences(planId: String, dayIndex: Int): List<BibleReference> {
        val plan = getPlan(planId) ?: return emptyList()
        return plan.referencesByDay.getOrNull(dayIndex).orEmpty()
    }

    private fun loadPlansFromAssets(): List<ReadingPlan> {
        val files = listOf("bible-in-a-year.json", "psalms-in-30-days.json", "nt-in-90-days.json")
        return files.map { fileName ->
            parsePlanJson(context.readAsset("plans/$fileName").decodeToString())
        }.sortedBy { it.name }
    }

    private fun parsePlanJson(text: String): ReadingPlan {
        val root = json.decodeFromString<JsonObject>(text)
        val id = root.stringField("id") ?: error("Plan missing id")
        val name = root.stringField("name") ?: id
        val description = root.stringField("description").orEmpty()
        val daysElement = root["days"]
        val referencesByDay = when {
            daysElement == null -> emptyList()
            daysElement is kotlinx.serialization.json.JsonArray -> {
                daysElement.map { dayElement ->
                    when (dayElement) {
                        is kotlinx.serialization.json.JsonArray ->
                            dayElement.mapNotNull { ref ->
                                ref.jsonPrimitive.contentOrNull?.let(BibleReference::parseCanonical)
                            }
                        is JsonObject -> {
                            val readings = dayElement["readings"] as? kotlinx.serialization.json.JsonArray
                                ?: dayElement["references"] as? kotlinx.serialization.json.JsonArray
                            readings?.mapNotNull { ref ->
                                ref.jsonPrimitive.contentOrNull?.let(BibleReference::parseCanonical)
                            }.orEmpty()
                        }
                        else -> emptyList()
                    }
                }
            }
            else -> emptyList()
        }
        return ReadingPlan(
            id = id,
            name = name,
            description = description,
            dayCount = referencesByDay.size,
            referencesByDay = referencesByDay,
        )
    }

    private fun JsonObject.stringField(name: String): String? =
        this[name]?.jsonPrimitive?.contentOrNull
}
