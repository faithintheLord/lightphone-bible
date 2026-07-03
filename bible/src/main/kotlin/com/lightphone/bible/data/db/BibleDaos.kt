package com.lightphone.bible.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface TranslationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(translations: List<TranslationEntity>)

    @Update
    suspend fun update(translation: TranslationEntity)

    @Query("SELECT * FROM translations ORDER BY displayName COLLATE NOCASE")
    suspend fun getAll(): List<TranslationEntity>

    @Query("SELECT * FROM translations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TranslationEntity?

    @Query(
        "UPDATE translations SET isDownloaded = :downloaded, downloadedAtEpochMs = :downloadedAt " +
            "WHERE id = :id",
    )
    suspend fun markDownloaded(id: String, downloaded: Boolean, downloadedAt: Long?)
}

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(books: List<BookEntity>)

    @Query(
        "SELECT * FROM books WHERE translationId = :translationId ORDER BY sortOrder",
    )
    suspend fun getBooksForTranslation(translationId: String): List<BookEntity>

    @Query("DELETE FROM books WHERE translationId = :translationId")
    suspend fun deleteForTranslation(translationId: String)
}

@Dao
interface VerseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(verses: List<VerseEntity>)

    @Query(
        "SELECT * FROM verses WHERE translationId = :translationId AND bookId = :bookId " +
            "AND chapter = :chapter ORDER BY verse",
    )
    suspend fun getChapter(
        translationId: String,
        bookId: String,
        chapter: Int,
    ): List<VerseEntity>

    @Query(
        "SELECT * FROM verses WHERE translationId = :translationId AND bookId = :bookId " +
            "AND chapter = :chapter AND verse = :verse LIMIT 1",
    )
    suspend fun getVerse(
        translationId: String,
        bookId: String,
        chapter: Int,
        verse: Int,
    ): VerseEntity?

    @Query("DELETE FROM verses WHERE translationId = :translationId")
    suspend fun deleteForTranslation(translationId: String)
}

@Dao
interface VerseSearchDao {
    @Query(
        """
        SELECT verses.*
        FROM verses
        JOIN verses_fts ON verses.rowid = verses_fts.rowid
        WHERE verses_fts MATCH :ftsQuery AND verses.translationId = :translationId
        ORDER BY verses.bookId, verses.chapter, verses.verse
        LIMIT :limit
        """,
    )
    suspend fun search(
        translationId: String,
        ftsQuery: String,
        limit: Int,
    ): List<VerseEntity>
}

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM bookmarks ORDER BY createdAtEpochMs DESC")
    suspend fun getAll(): List<BookmarkEntity>

    @Query(
        "SELECT * FROM bookmarks WHERE translationId = :translationId AND bookId = :bookId " +
            "AND chapter = :chapter AND verse = :verse LIMIT 1",
    )
    suspend fun find(
        translationId: String,
        bookId: String,
        chapter: Int,
        verse: Int,
    ): BookmarkEntity?
}

@Dao
interface HighlightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(highlight: HighlightEntity): Long

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        "SELECT * FROM highlights WHERE translationId = :translationId AND bookId = :bookId " +
            "AND chapter = :chapter ORDER BY verseStart",
    )
    suspend fun getForChapter(
        translationId: String,
        bookId: String,
        chapter: Int,
    ): List<HighlightEntity>

    @Query("SELECT * FROM highlights ORDER BY createdAtEpochMs DESC")
    suspend fun getAll(): List<HighlightEntity>
}

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        "SELECT * FROM notes WHERE translationId = :translationId AND bookId = :bookId " +
            "AND chapter = :chapter AND verse = :verse LIMIT 1",
    )
    suspend fun getForVerse(
        translationId: String,
        bookId: String,
        chapter: Int,
        verse: Int,
    ): NoteEntity?

    @Query("SELECT * FROM notes ORDER BY updatedAtEpochMs DESC")
    suspend fun getAll(): List<NoteEntity>
}

@Dao
interface WordTokenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tokens: List<WordTokenEntity>)

    @Query(
        "SELECT * FROM word_tokens WHERE translationId = :translationId AND bookId = :bookId " +
            "AND chapter = :chapter AND verse = :verse ORDER BY tokenIndex",
    )
    suspend fun getForVerse(
        translationId: String,
        bookId: String,
        chapter: Int,
        verse: Int,
    ): List<WordTokenEntity>

    @Query("DELETE FROM word_tokens WHERE translationId = :translationId AND bookId = :bookId")
    suspend fun deleteForBook(translationId: String, bookId: String)

    @Query("SELECT COUNT(*) FROM word_tokens WHERE translationId = :translationId LIMIT 1")
    suspend fun countForTranslation(translationId: String): Int

    @Query(
        """
        SELECT DISTINCT translationId, bookId, chapter, verse
        FROM word_tokens
        WHERE strongsNumber = :strongsNumber
        ORDER BY bookId, chapter, verse
        LIMIT :limit
        """,
    )
    suspend fun findVersesByStrongs(
        strongsNumber: String,
        limit: Int,
    ): List<WordTokenVerseRef>
}

data class WordTokenVerseRef(
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    val verse: Int,
)

@Dao
interface StrongsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<StrongsEntryEntity>)

    @Query("SELECT * FROM strongs_entries WHERE number = :number LIMIT 1")
    suspend fun getByNumber(number: String): StrongsEntryEntity?

    @Query("SELECT COUNT(*) FROM strongs_entries")
    suspend fun count(): Int
}

@Dao
interface ReadingProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ReadingProgressEntity)

    @Query("SELECT * FROM reading_progress WHERE translationId = :translationId LIMIT 1")
    suspend fun get(translationId: String): ReadingProgressEntity?
}

@Dao
interface PlanProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: PlanProgressEntity)

    @Query("SELECT * FROM plan_progress WHERE planId = :planId ORDER BY dayIndex")
    suspend fun getForPlan(planId: String): List<PlanProgressEntity>

    @Transaction
    suspend fun replacePlanProgress(planId: String, entries: List<PlanProgressEntity>) {
        deleteForPlan(planId)
        entries.forEach { upsert(it) }
    }

    @Query("DELETE FROM plan_progress WHERE planId = :planId")
    suspend fun deleteForPlan(planId: String)
}
