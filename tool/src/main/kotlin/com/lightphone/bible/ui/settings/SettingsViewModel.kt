package com.lightphone.bible.ui.settings

import androidx.lifecycle.viewModelScope
import com.lightphone.bible.BibleApp
import com.lightphone.bible.repository.BiblePreferences
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightContext
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val FONT_SCALES = listOf(0.85f, 1.0f, 1.15f, 1.3f)

data class LicenseNotice(val title: String, val text: String)

private val CONTENT_NOTICES = listOf(
    LicenseNotice(
        "Berean Standard Bible",
        "The Holy Bible, Berean Standard Bible (BSB) is dedicated to the public domain. " +
            "Produced in cooperation with Bible Hub, Discovery Bible, OpenBible.com, and the Berean Bible Translation Committee.\n\n" +
            "Publisher and public-domain notice:\nhttps://berean.bible/",
    ),
    LicenseNotice(
        "World English Bible",
        "The World English Bible (WEB) is public domain. World English Bible is a trademark of eBible.org; " +
            "the name must not be used for altered translation text. This app displays the supplied verse text.\n\n" +
            "Publisher's notice:\nhttps://ebible.org/engwebp/copyright.htm",
    ),
    LicenseNotice(
        "Scripture downloads",
        "BSB and WEB are provided through the Free Use Bible API.\n\nhttps://bible.helloao.org/docs/",
    ),
    LicenseNotice(
        "BSB word mappings",
        "English word mappings from BSB Publishing's base/display data are CC0 / public domain.\n\n" +
            "Source and attribution:\nhttps://github.com/BSB-publishing/bsb-data-output\n\n" +
            "https://github.com/BSB-publishing/bsb-data-output/blob/main/ATTRIBUTION.md",
    ),
    LicenseNotice(
        "Strong's dictionaries",
        "Greek JSON: copyright 2009 Open Scriptures; Hebrew JSON: copyright 2010 Open Scriptures. CC-BY-SA. " +
            "The upstream notices do not specify a license version. " +
            "Greek source prepared by Ulrik Petersen; Hebrew source by David Instone-Brewer, with editing, " +
            "corrections, and Unicode transliterations by David Troidl.\n\n" +
            "JavaScript wrappers were removed and JSON formatting compacted; entries are unchanged. " +
            "Dictionary data retains its upstream CC-BY-SA terms, separate from the app's MIT license.\n\n" +
            "Source:\nhttps://github.com/openscriptures/strongs\n\n" +
            "Creative Commons license information:\nhttps://creativecommons.org/licenses/",
    ),
    LicenseNotice(
        "Light SDK",
        "The Light SDK is MIT licensed.\n\n" +
            "https://github.com/lightphone/light-sdk\n\n" +
            "https://github.com/lightphone/light-sdk/blob/main/LICENSE",
    ),
)

data class SettingsUiState(
    val fontScale: Float = BiblePreferences.DEFAULT_FONT_SCALE,
    val fontScaleLabel: String = "Normal",
    val primaryTranslationName: String = "",
    val attribution: String = "",
    val compareTranslationName: String = "Off",
    val hasCompareOptions: Boolean = false,
    val showLicenses: Boolean = false,
    val licenses: List<LicenseNotice> = CONTENT_NOTICES,
    val strongsNotice: String = "",
)

class SettingsViewModel(
    lightContext: SealedLightContext,
) : LightViewModel<Unit>() {
    private val app = BibleApp.ensureInitialized(lightContext)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val notice = lightContext.readAsset("strongs/NOTICE.txt").decodeToString()
            _uiState.update { it.copy(strongsNotice = notice) }
        }
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                app.preferences.fontScale,
                app.preferences.primaryTranslationId,
                app.preferences.compareTranslationIds,
                app.preferences.syncState,
            ) { _, _, _, _ -> Unit }.collect { refresh() }
        }
    }

    fun showLicenses() {
        _uiState.update { it.copy(showLicenses = true) }
    }

    fun hideLicenses() {
        _uiState.update { it.copy(showLicenses = false) }
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        viewModelScope.launch(Dispatchers.IO) { refresh() }
    }

    private suspend fun refresh() {
        val fontScale = app.preferences.fontScale.first()
        val translationId = app.preferences.getPrimaryTranslationId() ?: "BSB"
        val translation = app.contentRepository.getTranslation(translationId)
        val catalogEntry = app.registry.getTranslation(translationId)
        val candidates = app.contentRepository.getTranslations().filter {
            it.isDownloaded && it.id != translationId
        }
        val compareIds = app.preferences.compareTranslationIds.first()
        val comparison = candidates.firstOrNull { it.id in compareIds }
        _uiState.update {
            it.copy(
                fontScale = fontScale,
                fontScaleLabel = labelForScale(fontScale),
                primaryTranslationName = translation?.displayName ?: translationId,
                attribution = catalogEntry?.attribution.orEmpty(),
                compareTranslationName = comparison?.displayName ?: "Off",
                hasCompareOptions = candidates.isNotEmpty(),
            )
        }
    }

    fun cycleCompareTranslation() {
        viewModelScope.launch(Dispatchers.IO) {
            val primary = app.preferences.getPrimaryTranslationId() ?: "BSB"
            val candidates = app.contentRepository.getTranslations()
                .filter { it.isDownloaded && it.id != primary }.sortedBy { it.id }
            val current = app.preferences.compareTranslationIds.first()
            val next = candidates.indexOfFirst { it.id in current } + 1
            app.preferences.setCompareTranslationIds(
                candidates.getOrNull(next)?.id?.let { setOf(it) }.orEmpty(),
            )
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
