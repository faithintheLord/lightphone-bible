package com.lightphone.bible.ui.reader

import androidx.lifecycle.viewModelScope

import com.lightphone.bible.BibleApp
import com.lightphone.bible.data.db.HighlightEntity
import com.lightphone.bible.data.db.VerseEntity
import com.lightphone.bible.data.db.WordTokenEntity
import com.lightphone.bible.data.model.BibleBooks
import com.lightphone.bible.data.model.BibleReference
import com.lightphone.bible.ui.HighlightColor
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class VerseDisplay(
    val verse: VerseEntity,
    val compareText: String? = null,
    val tokens: List<WordTokenEntity> = emptyList(),
    val highlight: HighlightEntity? = null,
    val hasNote: Boolean = false,
    val isBookmarked: Boolean = false,
)

data class ReaderUiState(
    val isLoading: Boolean = true,
    val bookId: String = "",
    val bookName: String = "",
    val chapter: Int = 1,
    val translationId: String = "BSB",
    val compareTranslationId: String? = null,
    val compareEnabled: Boolean = false,
    val verses: List<VerseDisplay> = emptyList(),
    val selectedVerse: Int? = null,
    val hasPreviousChapter: Boolean = false,
    val hasNextChapter: Boolean = false,
    val supportsWordStudy: Boolean = false,
    val noteEditorSession: Int = 0,
    val noteEditorVerse: Int? = null,
    val noteEditorInitialText: String = "",
    val highlightPickerVisible: Boolean = false,
    val errorMessage: String? = null,
)

class ReaderViewModel(
    lightContext: SealedLightContext,
    bookId: String,
    chapter: Int,
    initialVerse: Int,
) : LightViewModel<Unit>() {
    private val app = BibleApp.ensureInitialized(lightContext)

    private val _uiState = MutableStateFlow(
        ReaderUiState(bookId = bookId, chapter = chapter, selectedVerse = initialVerse),
    )
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        loadChapter(bookId, chapter, initialVerse)
    }

    fun loadChapter(bookId: String, chapter: Int, scrollToVerse: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val translationId = app.preferences.getPrimaryTranslationId() ?: "BSB"
            val compareIds = app.preferences.compareTranslationIds.first()
            val compareId = compareIds.firstOrNull()
            val translation = app.contentRepository.getTranslation(translationId)
            val verses = app.contentRepository.getChapter(translationId, bookId, chapter)
            val highlights = app.userDataRepository.getHighlightsForChapter(
                translationId, bookId, chapter,
            )
            val bookmarks = app.userDataRepository.getBookmarks()
            val bookName = BibleBooks.find(bookId)?.name ?: bookId
            val chapterCount = BibleBooks.chapterCount(bookId)

            val displays = verses.map { verse ->
                val ref = BibleReference(translationId, bookId, chapter, verse.verse)
                val compareText = compareId?.let { cmpId ->
                    app.contentRepository.getVerse(
                        BibleReference(cmpId, bookId, chapter, verse.verse),
                    )?.text
                }
                val tokens = if (translation?.supportsWordStudy == true) {
                    app.interlinearRepository.getWordTokens(
                        translationId, bookId, chapter, verse.verse,
                    )
                } else {
                    emptyList()
                }
                val highlight = highlights.find { h ->
                    verse.verse in h.verseStart..h.verseEnd
                }
                val note = app.userDataRepository.getNote(ref)
                val bookmarked = bookmarks.any {
                    it.translationId == translationId &&
                        it.bookId == bookId &&
                        it.chapter == chapter &&
                        it.verse == verse.verse
                }
                VerseDisplay(
                    verse = verse,
                    compareText = compareText,
                    tokens = tokens,
                    highlight = highlight,
                    hasNote = note != null,
                    isBookmarked = bookmarked,
                )
            }

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        bookId = bookId,
                        bookName = bookName,
                        chapter = chapter,
                        translationId = translationId,
                        compareTranslationId = compareId,
                        compareEnabled = compareId != null,
                        verses = displays,
                        selectedVerse = scrollToVerse ?: it.selectedVerse,
                        hasPreviousChapter = chapter > 1,
                        hasNextChapter = chapter < chapterCount,
                        supportsWordStudy = translation?.supportsWordStudy == true,
                    )
                }
            }

            app.userDataRepository.saveReadingProgress(
                BibleReference(translationId, bookId, chapter, scrollToVerse ?: 1),
            )
        }
    }

    fun toggleCompare() {
        _uiState.update { it.copy(compareEnabled = !it.compareEnabled) }
    }

    fun selectVerse(verse: Int) {
        _uiState.update { it.copy(selectedVerse = verse) }
    }

    fun previousChapter() {
        val state = _uiState.value
        if (state.chapter <= 1) return
        loadChapter(state.bookId, state.chapter - 1, 1)
    }

    fun nextChapter() {
        val state = _uiState.value
        if (!state.hasNextChapter) return
        loadChapter(state.bookId, state.chapter + 1, 1)
    }

    fun toggleBookmark() {
        val state = _uiState.value
        val verse = state.selectedVerse
            ?: state.verses.firstOrNull()?.verse?.verse
            ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val ref = BibleReference(state.translationId, state.bookId, state.chapter, verse)
            val existing = app.userDataRepository.getBookmarks().find {
                it.translationId == ref.translationId &&
                    it.bookId == ref.bookId &&
                    it.chapter == ref.chapter &&
                    it.verse == ref.verse
            }
            if (existing != null) {
                app.userDataRepository.removeBookmark(existing.id)
            } else {
                app.userDataRepository.addBookmark(ref)
            }
            loadChapter(state.bookId, state.chapter, verse)
        }
    }

    fun showHighlightPicker() {
        _uiState.update { it.copy(highlightPickerVisible = true) }
    }

    fun dismissHighlightPicker() {
        _uiState.update { it.copy(highlightPickerVisible = false) }
    }

    fun applyHighlight(color: HighlightColor) {
        val state = _uiState.value
        val verse = state.selectedVerse ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val ref = BibleReference(state.translationId, state.bookId, state.chapter, verse)
            app.userDataRepository.addHighlight(ref, verseEnd = verse, colorArgb = color.argb)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(highlightPickerVisible = false) }
            }
            loadChapter(state.bookId, state.chapter, verse)
        }
    }

    fun openNoteEditor() {
        val state = _uiState.value
        val verse = state.selectedVerse ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val ref = BibleReference(state.translationId, state.bookId, state.chapter, verse)
            val existing = app.userDataRepository.getNote(ref)?.note.orEmpty()
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        noteEditorVerse = verse,
                        noteEditorInitialText = existing,
                        noteEditorSession = it.noteEditorSession + 1,
                    )
                }
            }
        }
    }

    fun cancelNoteEditor() {
        _uiState.update { it.copy(noteEditorVerse = null) }
    }

    fun submitNote(text: CharSequence) {
        val state = _uiState.value
        val verse = state.noteEditorVerse ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val ref = BibleReference(state.translationId, state.bookId, state.chapter, verse)
            val trimmed = text.toString().trim()
            if (trimmed.isNotEmpty()) {
                app.userDataRepository.upsertNote(ref, trimmed)
            }
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(noteEditorVerse = null) }
            }
            loadChapter(state.bookId, state.chapter, verse)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
