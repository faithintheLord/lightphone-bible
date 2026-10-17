package com.lightphone.bible.ui.saved

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
import com.thelightphone.sdk.ui.LightBottomBar
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

private const val ROW_HEIGHT = 3f

class SavedScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, SavedViewModel>(sealedActivity) {

    override val viewModelClass: Class<SavedViewModel>
        get() = SavedViewModel::class.java

    override fun createViewModel(): SavedViewModel = SavedViewModel(lightContext)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.uiState.collectAsState()
        val items = when (state.selectedTab) {
            SavedTab.BOOKMARKS -> state.bookmarks
            SavedTab.HIGHLIGHTS -> state.highlights
            SavedTab.NOTES -> state.notes
        }

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
                    center = LightTopBarCenter.Text(
                        when (state.selectedTab) {
                            SavedTab.BOOKMARKS -> "Bookmarks"
                            SavedTab.HIGHLIGHTS -> "Highlights"
                            SavedTab.NOTES -> "Notes"
                        },
                    ),
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
                    } else if (items.isEmpty()) {
                        item {
                            LightText(
                                text = "Nothing saved yet",
                                variant = LightTextVariant.Copy,
                                modifier = Modifier.height(ROW_HEIGHT.gridUnitsAsDp()),
                            )
                        }
                    } else {
                        items(items, key = {
                            "${state.selectedTab}.${it.reference.translationId}.${it.id}"
                        }) { item ->
                            Column(
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
                                                translationId = item.reference.translationId,
                                            )
                                        })
                                    }
                                    .padding(vertical = 0.35f.gridUnitsAsDp()),
                            ) {
                                LightText(text = item.label, variant = LightTextVariant.Copy, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                item.detail?.let { detail ->
                                    LightText(
                                        text = detail,
                                        variant = LightTextVariant.Detail,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        lighten = true,
                                    )
                                }
                            }
                        }
                    }
                }

                LightBottomBar(
                    items = listOf(
                        LightBarButton.LightIcon(
                            icon = LightIcons.STAR,
                            contentDescription = "Bookmarks",
                            onClick = { viewModel.selectTab(SavedTab.BOOKMARKS) },
                        ),
                        LightBarButton.LightIcon(
                            icon = LightIcons.PENCIL,
                            contentDescription = "Highlights",
                            onClick = { viewModel.selectTab(SavedTab.HIGHLIGHTS) },
                        ),
                        LightBarButton.LightIcon(
                            icon = LightIcons.COMPOSE_MESSAGE,
                            contentDescription = "Notes",
                            onClick = { viewModel.selectTab(SavedTab.NOTES) },
                        ),
                    ),
                )
            }
        }
    }
}
