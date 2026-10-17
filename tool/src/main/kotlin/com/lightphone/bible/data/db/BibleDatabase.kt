package com.lightphone.bible.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TranslationEntity::class,
        BookEntity::class,
        VerseEntity::class,
        VerseFtsEntity::class,
        BookmarkEntity::class,
        HighlightEntity::class,
        NoteEntity::class,
        WordTokenEntity::class,
        StrongsEntryEntity::class,
        ReadingProgressEntity::class,
        PlanProgressEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class BibleDatabase : RoomDatabase() {
    abstract fun translationDao(): TranslationDao
    abstract fun bookDao(): BookDao
    abstract fun verseDao(): VerseDao
    abstract fun verseSearchDao(): VerseSearchDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun highlightDao(): HighlightDao
    abstract fun noteDao(): NoteDao
    abstract fun wordTokenDao(): WordTokenDao
    abstract fun strongsDao(): StrongsDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun planProgressDao(): PlanProgressDao
}
