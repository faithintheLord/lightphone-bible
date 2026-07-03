package com.lightphone.bible.ui.settings

import androidx.lifecycle.viewModelScope

import com.lightphone.bible.BibleApp
import com.lightphone.bible.repository.BiblePreferences
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val FONT_SCALES = listOf(0.85f, 1.0f, 1.15f, 1.3f)

data class SettingsUiState(
    val fontScale: Float = BiblePreferences.DEFAULT_FONT_SCALE,
    val fontScaleLabel: String = "Normal",
    val primaryTranslationName: String = "",
    val attribution: String = "",
)

class SettingsViewModel(
    lightContext: SealedLightContext,
) : LightViewModel<Unit>() {
    private val app = BibleApp.ensureInitialized(lightContext)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val fontScale = app.preferences.fontScale.first()
            val translationId = app.preferences.getPrimaryTranslationId() ?: "BSB"
            val translation = app.contentRepository.getTranslation(translationId)
            val catalogEntry = app.registry.getTranslation(translationId)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        fontScale = fontScale,
                        fontScaleLabel = labelForScale(fontScale),
                        primaryTranslationName = translation?.displayName ?: translationId,
                        attribution = catalogEntry?.attribution.orEmpty(),
                    )
                }
            }
        }
    }

    fun cycleFontScale() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _uiState.value.fontScale
            val nextIndex = (FONT_SCALES.indexOfFirst { it == current } + 1) % FONT_SCALES.size
            val next = FONT_SCALES[nextIndex]
            app.preferences.setFontScale(next)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(fontScale = next, fontScaleLabel = labelForScale(next))
                }
            }
        }
    }

    private fun labelForScale(scale: Float): String = when (scale) {
        0.85f -> "Small"
        1.15f -> "Large"
        1.3f -> "Extra Large"
        else -> "Normal"
    }
}
