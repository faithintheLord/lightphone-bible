package com.lightphone.bible.ui.books

import androidx.lifecycle.viewModelScope
import com.lightphone.bible.BibleApp
import com.lightphone.bible.data.db.BookEntity
import com.lightphone.bible.data.model.BibleBooks
import com.lightphone.bible.data.model.Testament
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

data class BookPickerUiState(
    val isLoading: Boolean = true,
    val oldTestament: List<BookEntity> = emptyList(),
    val newTestament: List<BookEntity> = emptyList(),
)

class BookPickerViewModel(
    lightContext: SealedLightContext,
) : LightViewModel<Unit>() {
    private val app = BibleApp.ensureInitialized(lightContext)

    private val _uiState = MutableStateFlow(BookPickerUiState())
    val uiState: StateFlow<BookPickerUiState> = _uiState.asStateFlow()

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        viewModelScope.launch(Dispatchers.IO) {
            val translationId = app.preferences.getPrimaryTranslationId() ?: "BSB"
            val books = app.contentRepository.getBooks(translationId)
            val ot = books.filter { book ->
                BibleBooks.find(book.bookId)?.testament == Testament.OLD
            }
            val nt = books.filter { book ->
                BibleBooks.find(book.bookId)?.testament == Testament.NEW
            }
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(isLoading = false, oldTestament = ot, newTestament = nt)
                }
            }
        }
    }
}
