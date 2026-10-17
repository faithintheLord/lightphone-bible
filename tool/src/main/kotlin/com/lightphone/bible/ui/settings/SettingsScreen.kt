package com.lightphone.bible.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.lightphone.bible.ui.translations.TranslationsScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp

class SettingsScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, SettingsViewModel>(sealedActivity) {

    override val viewModelClass: Class<SettingsViewModel>
        get() = SettingsViewModel::class.java

    override fun createViewModel(): SettingsViewModel = SettingsViewModel(lightContext)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.uiState.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = LightBarButton.LightIcon(
                        icon = LightIcons.BACK,
                        onClick = { if (state.showLicenses) viewModel.hideLicenses() else goBack() },
                    ),
                    center = LightTopBarCenter.Text(if (state.showLicenses) "Licenses" else "Settings"),
                    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                )

                key(state.showLicenses) {
                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 1f.gridUnitsAsDp()),
                    ) {
                        if (state.showLicenses) {
                            LightText(
                                text = "These notices are included with the app and available offline.",
                                variant = LightTextVariant.Detail,
                                modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                            )
                            state.licenses.forEach { notice -> LicenseBlock(notice.title, notice.text) }
                            if (state.strongsNotice.isNotBlank()) {
                                LicenseBlock("Full Strong's notices", state.strongsNotice)
                            }
                        } else {
                            SettingRow(
                                label = "Font Size",
                                value = state.fontScaleLabel,
                                onClick = viewModel::cycleFontScale,
                            )
                            SettingRow(
                                label = "Translation",
                                value = state.primaryTranslationName,
                                onClick = { navigateTo(::TranslationsScreen) },
                            )
                            SettingRow(
                                label = "Compare Translation",
                                value = if (state.hasCompareOptions) state.compareTranslationName else "Download another translation",
                                onClick = {
                                    if (state.hasCompareOptions) viewModel.cycleCompareTranslation()
                                    else navigateTo(::TranslationsScreen)
                                },
                            )
                            SettingRow(
                                label = "All Translations",
                                value = "Manage downloads",
                                onClick = { navigateTo(::TranslationsScreen) },
                            )
                            SettingRow(
                                label = "Credits & Licenses",
                                value = "Read offline",
                                onClick = viewModel::showLicenses,
                            )

                            if (state.attribution.isNotBlank()) {
                                Column(modifier = Modifier.padding(vertical = 1f.gridUnitsAsDp())) {
                                    LightText(
                                        text = "ABOUT",
                                        variant = LightTextVariant.Detail,
                                        lighten = true,
                                    )
                                    LightText(
                                        text = state.attribution,
                                        variant = LightTextVariant.Paragraph,
                                        modifier = Modifier.padding(top = 0.5f.gridUnitsAsDp()),
                                    )
                                }
                            }

                            Column(modifier = Modifier.padding(vertical = 1f.gridUnitsAsDp())) {
                                LightText(
                                    text = "Light Phone Bible",
                                    variant = LightTextVariant.Heading,
                                )
                                LightText(
                                    text = "BSB and WEB scripture. Word study with Open Scriptures Strong's dictionaries and BSB word mappings.",
                                    variant = LightTextVariant.Detail,
                                    lighten = true,
                                    modifier = Modifier.padding(top = 0.5f.gridUnitsAsDp()),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LicenseBlock(title: String, text: String) {
    Column(modifier = Modifier.padding(bottom = 1.5f.gridUnitsAsDp())) {
        LightText(text = title, variant = LightTextVariant.Heading)
        LightText(
            text = text,
            variant = LightTextVariant.Detail,
            modifier = Modifier.padding(top = 0.5f.gridUnitsAsDp()),
        )
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 0.75f.gridUnitsAsDp()),
    ) {
        LightText(text = label, variant = LightTextVariant.Detail, lighten = true)
        LightText(text = value, variant = LightTextVariant.Heading)
    }
}
