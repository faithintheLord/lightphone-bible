package com.lightphone.bible.ui.plans

import androidx.lifecycle.viewModelScope
import com.lightphone.bible.BibleApp
import com.lightphone.bible.ui.formatChapterReference
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class PlanDayViewModel(
    lightContext: SealedLightContext,
    private val planId: String,
    dayIndex: Int,
) : LightViewModel<Unit>() {
    private val app = BibleApp.ensureInitialized(lightContext)

    private val _uiState = MutableStateFlow(PlanDayUiState(dayIndex = dayIndex))
    val uiState: StateFlow<PlanDayUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        showDay(_uiState.value.dayIndex)
    }

    fun showDay(dayIndex: Int) {
        if (_uiState.value.isSaving) return
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val plan = app.plansRepository.getPlan(planId)
                require(plan != null && dayIndex in 0 until plan.dayCount)
                val refs = app.plansRepository.getDayReferences(planId, dayIndex)
                val progress = app.plansRepository.getProgress(planId)
                val completed = progress.find { it.dayIndex == dayIndex }?.completed == true
                val readings = refs.map { ref ->
                    PlanDayReading(
                        label = formatChapterReference(ref.bookId, ref.chapter),
                        bookId = ref.bookId,
                        chapter = ref.chapter,
                        verse = ref.verse,
                    )
                }
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            planName = plan.name,
                            dayIndex = dayIndex,
                            dayCount = plan.dayCount,
                            readings = readings,
                            completed = completed,
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Unable to load this reading plan.") }
            }
        }
    }

    fun toggleComplete() {
        val state = _uiState.value
        if (state.isLoading || state.isSaving || state.dayCount == 0) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                app.plansRepository.markDayComplete(planId, state.dayIndex, !state.completed)
                _uiState.update { it.copy(completed = !state.completed) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Unable to save plan progress. Please try again.") }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
