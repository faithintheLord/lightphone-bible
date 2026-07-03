package com.lightphone.bible.ui.wordstudy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.lightphone.bible.ui.reader.ReaderScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightLazyScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp

private const val ROW_HEIGHT = 3.5f

class StrongsConcordanceScreen(
    sealedActivity: SealedLightActivity,
    private val strongsNumber: String,
) : LightScreen<Unit, StrongsConcordanceViewModel>(sealedActivity) {

    override val viewModelClass: Class<StrongsConcordanceViewModel>
        get() = StrongsConcordanceViewModel::class.java

    override fun createViewModel(): StrongsConcordanceViewModel =
        StrongsConcordanceViewModel(lightContext, strongsNumber)

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
                    center = LightTopBarCenter.Text("Concordance $strongsNumber"),
                    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                )

                LightLazyScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 1f.gridUnitsAsDp()),
                    uniformItemHeightGridUnits = ROW_HEIGHT,
                ) {
                    if (state.isLoading) {
                        item {
                            LightText(
                                text = "Loading…",
                                variant = LightTextVariant.Copy,
                                modifier = Modifier.height(ROW_HEIGHT.gridUnitsAsDp()),
                            )
                        }
                    } else if (state.verses.isEmpty()) {
                        item {
                            LightText(
                                text = "No verses found",
                                variant = LightTextVariant.Copy,
                                modifier = Modifier.height(ROW_HEIGHT.gridUnitsAsDp()),
                            )
                        }
                    } else {
                        items(state.verses, key = {
                            "${it.reference.bookId}.${it.reference.chapter}.${it.reference.verse}"
                        }) { item ->
                            LightText(
                                text = item.label,
                                variant = LightTextVariant.Detail,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(ROW_HEIGHT.gridUnitsAsDp())
                                    .clickable {
                                        navigateTo(screenFactory = {
                                            ReaderScreen(
                                                it,
                                                item.reference.bookId,
                                                item.reference.chapter,
                                                item.reference.verse,
                                            )
                                        })
                                    }
                                    .padding(vertical = 0.5f.gridUnitsAsDp()),
                            )
                        }
                    }
                }
            }
        }
    }
}
