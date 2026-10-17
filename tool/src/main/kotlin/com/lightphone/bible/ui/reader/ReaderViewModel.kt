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
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class ReaderChapter(val bookId: String, val chapter: Int)

/** Adjacent chapters follow canonical book order, without wrapping at either end. */
internal fun adjacentChapter(bookId: String, chapter: Int, forward: Boolean): ReaderChapter? {
    val bookIndex = BibleBooks.all.indexOfFirst { it.id == bookId }
    if (bookIndex < 0 || chapter !in 1..BibleBooks.chapterCount(bookId)) return null
    val nextChapter = chapter + if (forward) 1 else -1
    if (nextChapter in 1..BibleBooks.chapterCount(bookId)) {
        return ReaderChapter(bookId, nextChapter)
    }
    val nextBook = BibleBooks.all.getOrNull(bookIndex + if (forward) 1 else -1) ?: return null
    return ReaderChapter(nextBook.id, if (forward) 1 else BibleBooks.chapterCount(nextBook.id))
}

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
    val isSaving: Boolean = false,
    val bookId: String = "",
    val bookName: String = "",
    val chapter: Int = 1,
    val translationId: String = "BSB",
    val compareTranslationId: String? = null,
    val compareEnabled: Boolean = false,
    val fontScale: Float = 1f,
    val verses: List<VerseDisplay> = emptyList(),
    val selectedVerse: Int? = null,
    val scrollToVerse: Int? = null,
    val scrollRequest: Int = 0,
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
    private val requestedTranslationId: String? = null,
) : LightViewModel<Unit>() {
    private val app = BibleApp.ensureInitialized(lightContext)
    private var loadJob: Job? = null
    private var progressJob: Job? = null
    private val _uiState = MutableStateFlow(
        ReaderUiState(bookId = bookId, chapter = chapter, selectedVerse = initialVerse, translationId = requestedTranslationId ?: "BSB"),
    )
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        loadChapter(bookId, chapter, initialVerse)
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        val state = _uiState.value
        if (!state.isLoading) loadChapter(state.bookId, state.chapter, state.selectedVerse)
    }

    override fun onBackPressed(): Boolean = when {
        _uiState.value.noteEditorVerse != null -> { cancelNoteEditor(); true }
        _uiState.value.highlightPickerVisible -> { dismissHighlightPicker(); true }
        _uiState.value.errorMessage != null -> { dismissError(); true }
        else -> false
    }

    fun loadChapter(bookId: String, chapter: Int, scrollToVerse: Int? = null) {
        loadJob?.cancel()
        progressJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val translationId = requestedTranslationId
                    ?: app.preferences.getPrimaryTranslationId() ?: "BSB"
                val compareId = app.preferences.compareTranslationIds.first()
                    .sorted().firstOrNull { it != translationId && app.contentRepository.getTranslation(it)?.isDownloaded == true }
                val fontScale = app.preferences.fontScale.first().let {
                    if (it.isFinite()) it.coerceIn(0.85f, 1.3f) else 1f
                }
                val translation = app.contentRepository.getTranslation(translationId)
                val verses = app.contentRepository.getChapter(translationId, bookId, chapter)
                val highlights = app.userDataRepository.getHighlightsForChapter(translationId, bookId, chapter)
                val bookmarks = app.userDataRepository.getBookmarks()
                val compareVerses = compareId?.let {
                    app.contentRepository.getChapter(it, bookId, chapter).associateBy { verse -> verse.verse }
                }.orEmpty()
                val displays = verses.map { verse ->
                    val ref = BibleReference(translationId, bookId, chapter, verse.verse)
                    VerseDisplay(
                        verse = verse,
                        compareText = compareVerses[verse.verse]?.text,
                        tokens = if (translation?.supportsWordStudy == true) {
                            app.interlinearRepository.getWordTokens(translationId, bookId, chapter, verse.verse)
                        } else emptyList(),
                        highlight = highlights.firstOrNull { verse.verse in it.verseStart..it.verseEnd },
                        hasNote = app.userDataRepository.getNote(ref) != null,
                        isBookmarked = bookmarks.any {
                            it.translationId == translationId && it.bookId == bookId &&
                                it.chapter == chapter && it.verse == verse.verse
                        },
                    )
                }
                currentCoroutineContext().ensureActive()
                val selected = verses.firstOrNull { it.verse == scrollToVerse }?.verse
                    ?: verses.firstOrNull()?.verse
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        bookId = bookId,
                        bookName = BibleBooks.find(bookId)?.name ?: bookId,
                        chapter = chapter,
                        translationId = translationId,
                        compareTranslationId = compareId,
                        compareEnabled = compareId != null &&
                            (it.compareTranslationId != compareId || it.compareEnabled),
                        fontScale = fontScale,
                        verses = displays,
                        selectedVerse = selected,
                        scrollToVerse = selected,
                        scrollRequest = it.scrollRequest + 1,
                        hasPreviousChapter = adjacentChapter(bookId, chapter, forward = false) != null,
                        hasNextChapter = adjacentChapter(bookId, chapter, forward = true) != null,
                        supportsWordStudy = displays.any { verse -> verse.tokens.any { !it.strongsNumber.isNullOrBlank() } },
                        errorMessage = if (verses.isEmpty()) "This chapter is not downloaded. Open Translations to download it." else null,
                    )
                }
                selected?.let { saveProgress(BibleReference(translationId, bookId, chapter, it)) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Unable to load this chapter. Please try again.")
                }
            }
        }
    }

    fun toggleCompare() {
        _uiState.update { it.copy(compareEnabled = it.compareTranslationId != null && !it.compareEnabled) }
    }

    fun selectVerse(verse: Int) {
        val state = _uiState.value
        if (state.isLoading || state.verses.none { it.verse.verse == verse }) return
        _uiState.update { it.copy(selectedVerse = verse) }
        progressJob?.cancel()
        progressJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                saveProgress(BibleReference(state.translationId, state.bookId, state.chapter, verse))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Unable to save your reading position. Please try again.") }
            }
        }
    }

    private suspend fun saveProgress(reference: BibleReference) {
        app.userDataRepository.saveReadingProgress(reference)
        app.preferences.setLastRead(reference)
    }

    fun previousChapter() {
        val state = _uiState.value
        if (state.isLoading || state.isSaving || !state.hasPreviousChapter) return
        val previous = adjacentChapter(state.bookId, state.chapter, forward = false) ?: return
        loadChapter(previous.bookId, previous.chapter, 1)
    }

    fun nextChapter() {
        val state = _uiState.value
        if (state.isLoading || state.isSaving || !state.hasNextChapter) return
        val next = adjacentChapter(state.bookId, state.chapter, forward = true) ?: return
        loadChapter(next.bookId, next.chapter, 1)
    }

    private fun changeSelectedVerse(action: suspend (BibleReference) -> Unit) {
        val state = _uiState.value
        val verse = state.selectedVerse ?: return
        if (state.isLoading || state.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                action(BibleReference(state.translationId, state.bookId, state.chapter, verse))
                _uiState.update { it.copy(highlightPickerVisible = false, noteEditorVerse = null) }
                loadChapter(state.bookId, state.chapter, verse)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Unable to save your change. Please try again.") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun toggleBookmark() = changeSelectedVerse { ref ->
        val existing = app.userDataRepository.getBookmarks().find {
            it.translationId == ref.translationId && it.bookId == ref.bookId &&
                it.chapter == ref.chapter && it.verse == ref.verse
        }
        if (existing == null) app.userDataRepository.addBookmark(ref)
        else app.userDataRepository.removeBookmark(existing.id)
    }

    fun showHighlightPicker() {
        if (!_uiState.value.isLoading && _uiState.value.selectedVerse != null) {
            _uiState.update { it.copy(highlightPickerVisible = true) }
        }
    }

    fun dismissHighlightPicker() {
        _uiState.update { it.copy(highlightPickerVisible = false) }
    }

    fun applyHighlight(color: HighlightColor) = changeSelectedVerse { ref ->
        app.userDataRepository.addHighlight(ref, verseEnd = ref.verse, colorArgb = color.argb)
    }

    fun removeHighlight() = changeSelectedVerse { ref ->
        app.userDataRepository.getHighlightsForChapter(ref.translationId, ref.bookId, ref.chapter)
            .filter { ref.verse in it.verseStart..it.verseEnd }
            .forEach { app.userDataRepository.removeHighlight(it.id) }
    }

    fun openNoteEditor() {
        val state = _uiState.value
        val verse = state.selectedVerse ?: return
        if (state.isLoading || state.isSaving) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ref = BibleReference(state.translationId, state.bookId, state.chapter, verse)
                val existing = app.userDataRepository.getNote(ref)?.note.orEmpty()
                _uiState.update {
                    if (it.bookId != state.bookId || it.chapter != state.chapter || it.isLoading) it
                    else it.copy(noteEditorVerse = verse, noteEditorInitialText = existing, noteEditorSession = it.noteEditorSession + 1)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Unable to open this note. Please try again.") }
            }
        }
    }

    fun cancelNoteEditor() {
        _uiState.update { it.copy(noteEditorVerse = null) }
    }

    fun submitNote(text: CharSequence) {
        val verse = _uiState.value.noteEditorVerse ?: return
        _uiState.update { it.copy(selectedVerse = verse) }
        changeSelectedVerse { ref ->
            val trimmed = text.toString().trim()
            if (trimmed.isEmpty()) app.userDataRepository.removeNote(ref)
            else app.userDataRepository.upsertNote(ref, trimmed)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
