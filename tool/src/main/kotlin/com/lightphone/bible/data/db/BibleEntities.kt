package com.lightphone.bible.data.db

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "translations")
data class TranslationEntity(
    @PrimaryKey val id: String,
    val providerTranslationId: String,
    val displayName: String,
    val language: String,
    val provider: String,
    val supportsWordStudy: Boolean,
    val interlinearProvider: String?,
    val isDownloaded: Boolean = false,
    val downloadedAtEpochMs: Long? = null,
)

@Entity(
    tableName = "books",
    primaryKeys = ["translationId", "bookId"],
    indices = [Index(value = ["translationId", "sortOrder"])],
)
data class BookEntity(
    val translationId: String,
    val bookId: String,
    val name: String,
    val commonName: String,
    val sortOrder: Int,
    val chapterCount: Int,
    val verseCount: Int,
)

@Entity(
    tableName = "verses",
    indices = [
        Index(value = ["translationId", "bookId", "chapter", "verse"], unique = true),
        Index(value = ["translationId", "bookId", "chapter"]),
    ],
)
data class VerseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
)

@Fts4(contentEntity = VerseEntity::class)
@Entity(tableName = "verses_fts")
data class VerseFtsEntity(
    val text: String,
)

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["translationId", "bookId", "chapter", "verse"], unique = true)],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    val createdAtEpochMs: Long,
)

@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    val verseStart: Int,
    val verseEnd: Int,
    val colorArgb: Int,
    val createdAtEpochMs: Long,
)

@Entity(
    tableName = "notes",
    indices = [Index(value = ["translationId", "bookId", "chapter", "verse"], unique = true)],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    val note: String,
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "word_tokens",
    indices = [Index(value = ["translationId", "bookId", "chapter", "verse", "tokenIndex"], unique = true)],
)
data class WordTokenEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    val tokenIndex: Int,
    val text: String,
    val strongsNumber: String?,
)

@Entity(tableName = "strongs_entries")
data class StrongsEntryEntity(
    @PrimaryKey val number: String,
    val language: String,
    val lemma: String?,
    val translit: String?,
    val pronunciation: String?,
    val definition: String?,
    val derivation: String?,
    val kjvDef: String?,
)

@Entity(
    tableName = "reading_progress",
    primaryKeys = ["translationId"],
)
data class ReadingProgressEntity(
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    val verse: Int,
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "plan_progress",
    primaryKeys = ["planId", "dayIndex"],
)
data class PlanProgressEntity(
    val planId: String,
    val dayIndex: Int,
    val completed: Boolean,
    val updatedAtEpochMs: Long,
)
