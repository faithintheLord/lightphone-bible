package com.lightphone.bible.ui.plans

import androidx.lifecycle.viewModelScope

import com.lightphone.bible.BibleApp
import com.lightphone.bible.data.model.BibleBooks
import com.lightphone.bible.ui.formatCanonical
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlanDayReading(
    val label: String,
    val bookId: String,
    val chapter: Int,
    val verse: Int,
)

data class PlanDayUiState(
    val isLoading: Boolean = true,
    val planName: String = "",
    val dayIndex: Int = 0,
    val dayCount: Int = 0,
    val readings: List<PlanDayReading> = emptyList(),
    val completed: Boolean = false,
)

class PlanDayViewModel(
    lightContext: SealedLightContext,
    planId: String,
    dayIndex: Int,
) : LightViewModel<Unit>() {
    private val app = BibleApp.ensureInitialized(lightContext)

    private val _uiState = MutableStateFlow(PlanDayUiState(dayIndex = dayIndex))
    val uiState: StateFlow<PlanDayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val plan = app.plansRepository.getPlan(planId)
            val refs = app.plansRepository.getDayReferences(planId, dayIndex)
            val progress = app.plansRepository.getProgress(planId)
            val completed = progress.find { it.dayIndex == dayIndex }?.completed == true
            val readings = refs.map { ref ->
                val bookName = BibleBooks.find(ref.bookId)?.name ?: ref.bookId
                PlanDayReading(
                    label = "$bookName ${ref.chapter} (${formatCanonical(ref)})",
                    bookId = ref.bookId,
                    chapter = ref.chapter,
                    verse = ref.verse,
                )
            }
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        planName = plan?.name ?: planId,
                        dayIndex = dayIndex,
                        dayCount = plan?.dayCount ?: 0,
                        readings = readings,
                        completed = completed,
                    )
                }
            }
        }
    }

    fun toggleComplete(planId: String) {
        val state = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            app.plansRepository.markDayComplete(planId, state.dayIndex, !state.completed)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(completed = !state.completed) }
            }
        }
    }
}
