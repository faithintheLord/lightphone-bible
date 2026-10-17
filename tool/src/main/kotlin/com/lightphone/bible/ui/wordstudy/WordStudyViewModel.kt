package com.lightphone.bible.ui.wordstudy

import androidx.lifecycle.viewModelScope

import com.lightphone.bible.BibleApp
import com.lightphone.bible.data.db.StrongsEntryEntity
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WordStudyUiState(
    val isLoading: Boolean = true,
    val strongsNumber: String = "",
    val entry: StrongsEntryEntity? = null,
)

class WordStudyViewModel(
    lightContext: SealedLightContext,
    strongsNumber: String,
) : LightViewModel<Unit>() {
    private val app = BibleApp.ensureInitialized(lightContext)

    private val _uiState = MutableStateFlow(WordStudyUiState(strongsNumber = strongsNumber))
    val uiState: StateFlow<WordStudyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val entry = app.strongsRepository.lookup(strongsNumber)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isLoading = false, entry = entry) }
            }
        }
    }
}
