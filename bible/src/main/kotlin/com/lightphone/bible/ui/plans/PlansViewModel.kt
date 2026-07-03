package com.lightphone.bible.ui.plans

import androidx.lifecycle.viewModelScope

import com.lightphone.bible.BibleApp
import com.lightphone.bible.repository.ReadingPlan
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PlanListItem(
    val plan: ReadingPlan,
    val completedDays: Int,
)

data class PlansUiState(
    val isLoading: Boolean = true,
    val plans: List<PlanListItem> = emptyList(),
)

class PlansViewModel(
    lightContext: SealedLightContext,
) : LightViewModel<Unit>() {
    private val app = BibleApp.ensureInitialized(lightContext)

    private val _uiState = MutableStateFlow(PlansUiState())
    val uiState: StateFlow<PlansUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val plans = app.plansRepository.getPlans().map { plan ->
                val progress = app.plansRepository.getProgress(plan.id)
                val completed = progress.count { it.completed }
                PlanListItem(plan = plan, completedDays = completed)
            }
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isLoading = false, plans = plans) }
            }
        }
    }
}
