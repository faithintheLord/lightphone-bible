package com.lightphone.bible.ui.search

import androidx.lifecycle.viewModelScope

import com.lightphone.bible.BibleApp
import com.lightphone.bible.data.db.VerseEntity
import com.lightphone.bible.ui.formatReference
import com.lightphone.bible.data.model.BibleReference
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SearchResultItem(
    val reference: BibleReference,
    val text: String,
    val label: String,
)

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<SearchResultItem> = emptyList(),
    val editorSession: Int = 0,
    val showEditor: Boolean = false,
)

class SearchViewModel(
    lightContext: SealedLightContext,
) : LightViewModel<Unit>() {
    private val app = BibleApp.ensureInitialized(lightContext)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun openEditor() {
        _uiState.update { it.copy(showEditor = true, editorSession = it.editorSession + 1) }
    }

    fun cancelEditor() {
        _uiState.update { it.copy(showEditor = false) }
    }

    fun submitQuery(raw: CharSequence) {
        val query = raw.toString().trim()
        _uiState.update { it.copy(query = query, showEditor = false, isSearching = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val translationId = app.preferences.getPrimaryTranslationId() ?: "BSB"
            val verses = app.searchRepository.search(translationId, query)
            val results = verses.map { it.toResultItem(translationId) }
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isSearching = false, results = results) }
            }
        }
    }

    private fun VerseEntity.toResultItem(translationId: String): SearchResultItem {
        val ref = BibleReference(translationId, bookId, chapter, verse)
        val snippet = text.take(100).let { if (text.length > 100) "$it…" else it }
        return SearchResultItem(
            reference = ref,
            text = text,
            label = "${formatReference(ref)} — $snippet",
        )
    }
}
