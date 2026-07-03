package com.lightphone.bible.ui.wordstudy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
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

class WordStudyScreen(
    sealedActivity: SealedLightActivity,
    private val strongsNumber: String,
) : LightScreen<Unit, WordStudyViewModel>(sealedActivity) {

    override val viewModelClass: Class<WordStudyViewModel>
        get() = WordStudyViewModel::class.java

    override fun createViewModel(): WordStudyViewModel =
        WordStudyViewModel(lightContext, strongsNumber)

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
                        onClick = { goBack() },
                    ),
                    center = LightTopBarCenter.Text("Strong's $strongsNumber"),
                    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                )

                LightScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 1f.gridUnitsAsDp()),
                ) {
                    if (state.isLoading) {
                        LightText(text = "Loading…", variant = LightTextVariant.Copy)
                    } else if (state.entry == null) {
                        LightText(
                            text = "No entry found for $strongsNumber",
                            variant = LightTextVariant.Copy,
                        )
                    } else {
                        val entry = state.entry!!
                        entry.lemma?.let {
                            DetailBlock("Lemma", it)
                        }
                        entry.translit?.let {
                            DetailBlock("Transliteration", it)
                        }
                        entry.pronunciation?.let {
                            DetailBlock("Pronunciation", it)
                        }
                        entry.definition?.let {
                            DetailBlock("Definition", it)
                        }
                        entry.derivation?.let {
                            DetailBlock("Derivation", it)
                        }
                        entry.kjvDef?.let {
                            DetailBlock("KJV Usage", it)
                        }
                    }
                }

                LightBottomBar(
                    items = listOf(
                        LightBarButton.Text(
                            text = "CONCORDANCE",
                            onClick = {
                                navigateTo(screenFactory = {
                                    StrongsConcordanceScreen(it, strongsNumber)
                                })
                            },
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun DetailBlock(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp())) {
        LightText(text = label.uppercase(), variant = LightTextVariant.Detail, lighten = true)
        LightText(
            text = value,
            variant = LightTextVariant.Paragraph,
            modifier = Modifier.padding(top = 0.25f.gridUnitsAsDp()),
        )
    }
}
