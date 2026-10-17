package com.lightphone.bible.ui.plans

import androidx.lifecycle.viewModelScope
import com.lightphone.bible.BibleApp
import com.lightphone.bible.repository.ReadingPlan
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

data class PlanListItem(
    val plan: ReadingPlan,
    val completedDays: Int,
    val nextDayIndex: Int,
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

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        viewModelScope.launch(Dispatchers.IO) {
            val plans = app.plansRepository.getPlans().map { plan ->
                val progress = app.plansRepository.getProgress(plan.id)
                val completed = progress.count { it.completed }
                val completedDays = progress.filter { it.completed }.map { it.dayIndex }.toSet()
                val nextDay = (0 until plan.dayCount).firstOrNull { it !in completedDays } ?: 0
                PlanListItem(plan = plan, completedDays = completed, nextDayIndex = nextDay)
            }
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isLoading = false, plans = plans) }
            }
        }
    }
}
