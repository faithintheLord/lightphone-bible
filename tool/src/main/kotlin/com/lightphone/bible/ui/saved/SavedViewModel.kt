package com.lightphone.bible.ui.saved

import androidx.lifecycle.viewModelScope
import com.lightphone.bible.BibleApp
import com.lightphone.bible.data.model.BibleReference
import com.lightphone.bible.ui.formatReference
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SavedTab {
    BOOKMARKS,
    HIGHLIGHTS,
    NOTES,
}

data class SavedItem(
    val reference: BibleReference,
    val id: Long,
    val label: String,
    val detail: String? = null,
)

data class SavedUiState(
    val selectedTab: SavedTab = SavedTab.BOOKMARKS,
    val bookmarks: List<SavedItem> = emptyList(),
    val highlights: List<SavedItem> = emptyList(),
    val notes: List<SavedItem> = emptyList(),
    val isLoading: Boolean = true,
)

class SavedViewModel(
    lightContext: SealedLightContext,
) : LightViewModel<Unit>() {
    private val app = BibleApp.ensureInitialized(lightContext)

    private val _uiState = MutableStateFlow(SavedUiState())
    val uiState: StateFlow<SavedUiState> = _uiState.asStateFlow()

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        reload()
    }

    fun selectTab(tab: SavedTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            val bookmarks = app.userDataRepository.getBookmarks().map { bm ->
                val ref = BibleReference(bm.translationId, bm.bookId, bm.chapter, bm.verse)
                SavedItem(reference = ref, id = bm.id, label = "${formatReference(ref)} · ${ref.translationId}")
            }
            val highlights = app.userDataRepository.getHighlights().map { hl ->
                val ref = BibleReference(hl.translationId, hl.bookId, hl.chapter, hl.verseStart)
                SavedItem(
                    reference = ref,
                    id = hl.id,
                    label = "${formatReference(ref)} · ${ref.translationId}",
                    detail = "Verses ${hl.verseStart}–${hl.verseEnd}",
                )
            }
            val notes = app.userDataRepository.getNotes().map { note ->
                val ref = BibleReference(note.translationId, note.bookId, note.chapter, note.verse)
                SavedItem(
                    reference = ref,
                    id = note.id,
                    label = "${formatReference(ref)} · ${ref.translationId}",
                    detail = note.note.take(60).let { if (note.note.length > 60) "$it…" else it },
                )
            }
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        bookmarks = bookmarks,
                        highlights = highlights,
                        notes = notes,
                    )
                }
            }
        }
    }
}
