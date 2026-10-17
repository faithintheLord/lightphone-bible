package com.lightphone.bible.ui.search

import androidx.lifecycle.viewModelScope
import com.lightphone.bible.BibleApp
import com.lightphone.bible.data.db.VerseEntity
import com.lightphone.bible.data.model.BibleReference
import com.lightphone.bible.ui.formatReference
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    val errorMessage: String? = null,
)

class SearchViewModel(
    lightContext: SealedLightContext,
) : LightViewModel<Unit>() {
    private val app = BibleApp.ensureInitialized(lightContext)
    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    override fun onBackPressed(): Boolean {
        if (!_uiState.value.showEditor) return false
        cancelEditor()
        return true
    }

    fun openEditor() {
        _uiState.update { it.copy(showEditor = true, editorSession = it.editorSession + 1) }
    }

    fun cancelEditor() {
        _uiState.update { it.copy(showEditor = false) }
    }

    fun submitQuery(raw: CharSequence) {
        val query = raw.toString().trim()
        searchJob?.cancel()
        _uiState.update {
            it.copy(query = query, showEditor = false, isSearching = query.isNotEmpty(), results = emptyList(), errorMessage = null)
        }
        if (query.isEmpty()) return
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val translationId = app.preferences.getPrimaryTranslationId() ?: "BSB"
                if (app.contentRepository.getTranslation(translationId)?.isDownloaded != true) {
                    _uiState.update {
                        it.copy(isSearching = false, errorMessage = "Download a translation before searching.")
                    }
                    return@launch
                }
                val verses = app.searchRepository.search(translationId, query)
                val results = verses.map { it.toResultItem(translationId) }
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isSearching = false, results = results) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isSearching = false, results = emptyList(), errorMessage = "Unable to search. Check that this translation is downloaded, then try again.") }
                }
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
