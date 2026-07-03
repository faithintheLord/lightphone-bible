package com.lightphone.bible.ui.wordstudy

import androidx.lifecycle.viewModelScope

import com.lightphone.bible.BibleApp
import com.lightphone.bible.data.model.BibleReference
import com.lightphone.bible.ui.formatReference
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ConcordanceVerse(
    val reference: BibleReference,
    val label: String,
)

data class StrongsConcordanceUiState(
    val isLoading: Boolean = true,
    val strongsNumber: String = "",
    val verses: List<ConcordanceVerse> = emptyList(),
)

class StrongsConcordanceViewModel(
    lightContext: SealedLightContext,
    strongsNumber: String,
) : LightViewModel<Unit>() {
    private val app = BibleApp.ensureInitialized(lightContext)

    private val _uiState = MutableStateFlow(StrongsConcordanceUiState(strongsNumber = strongsNumber))
    val uiState: StateFlow<StrongsConcordanceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val refs = app.strongsRepository.findVerses(strongsNumber)
            val verses = refs.map { ref ->
                val text = app.contentRepository.getVerse(ref)?.text.orEmpty()
                val snippet = text.take(80).let { if (text.length > 80) "$it…" else it }
                ConcordanceVerse(
                    reference = ref,
                    label = "${formatReference(ref)} — $snippet",
                )
            }
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isLoading = false, verses = verses) }
            }
        }
    }
}
