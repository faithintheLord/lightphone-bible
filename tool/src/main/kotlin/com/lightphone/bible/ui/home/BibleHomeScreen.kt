package com.lightphone.bible.ui.home

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
import com.lightphone.bible.ui.books.BookPickerScreen
import com.lightphone.bible.ui.plans.PlansScreen
import com.lightphone.bible.ui.reader.ReaderScreen
import com.lightphone.bible.ui.saved.SavedScreen
import com.lightphone.bible.ui.search.SearchScreen
import com.lightphone.bible.ui.settings.SettingsScreen
import com.lightphone.bible.ui.translations.TranslationsScreen
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightJobState
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp

@InitialScreen
class BibleHomeScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, BibleHomeViewModel>(sealedActivity) {

    override val viewModelClass: Class<BibleHomeViewModel>
        get() = BibleHomeViewModel::class.java

    override fun createViewModel(): BibleHomeViewModel =
        BibleHomeViewModel(lightContext)

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
                    center = LightTopBarCenter.Text("Bible"),
                    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                )

                if (!state.hasDownloadedData) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                    ) {
                        LightText(
                            text = state.syncMessage ?: "Downloading Bible content…",
                            variant = LightTextVariant.Paragraph,
                            modifier = Modifier.padding(vertical = 1f.gridUnitsAsDp()),
                        )
                        if (!state.isLoading && state.syncJobState !is LightJobState.Running) {
                            LightText(
                                text = "RETRY DOWNLOAD",
                                variant = LightTextVariant.Copy,
                                modifier = Modifier
                                    .clickable { viewModel.retrySync() }
                                    .padding(vertical = 0.75f.gridUnitsAsDp()),
                            )
                        }
                    }
                } else {
                    LightScrollView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 1f.gridUnitsAsDp()),
                    ) {
                        state.verseOfDayReference?.let { ref ->
                            val bookName =
                                com.lightphone.bible.data.model.BibleBooks.find(ref.bookId)?.name
                                    ?: ref.bookId
                            Column(modifier = Modifier.padding(bottom = 1.5f.gridUnitsAsDp())) {
                                LightText(
                                    text = "VERSE OF THE DAY",
                                    variant = LightTextVariant.Detail,
                                    lighten = true,
                                )
                                state.verseOfDayText?.let { text ->
                                    LightText(
                                        text = text,
                                        variant = LightTextVariant.Paragraph,
                                        modifier = Modifier.padding(top = 0.5f.gridUnitsAsDp()),
                                    )
                                }
                                LightText(
                                    text = "$bookName ${ref.chapter}:${ref.verse}",
                                    variant = LightTextVariant.Detail,
                                    modifier = Modifier
                                        .padding(top = 0.5f.gridUnitsAsDp())
                                        .clickable {
                                            navigateTo(screenFactory = {
                                                ReaderScreen(
                                                    it,
                                                    ref.bookId,
                                                    ref.chapter,
                                                    ref.verse,
                                                    translationId = ref.translationId,
                                                )
                                            })
                                        },
                                )
                            }
                        }

                        state.continueReadingReference?.let { ref ->
                            Column(modifier = Modifier.padding(bottom = 1.5f.gridUnitsAsDp())) {
                                LightText(
                                    text = "CONTINUE READING",
                                    variant = LightTextVariant.Detail,
                                    lighten = true,
                                )
                                LightText(
                                    text = state.continueReadingLabel ?: "Resume",
                                    variant = LightTextVariant.Heading,
                                    modifier = Modifier
                                        .padding(top = 0.5f.gridUnitsAsDp())
                                        .clickable {
                                            navigateTo(screenFactory = {
                                                ReaderScreen(
                                                    it,
                                                    ref.bookId,
                                                    ref.chapter,
                                                    ref.verse,
                                                    translationId = ref.translationId,
                                                )
                                            })
                                        },
                                )
                            }
                        }

                        HomeNavItem("BOOKS") { navigateTo(::BookPickerScreen) }
                        HomeNavItem("SEARCH") { navigateTo(::SearchScreen) }
                        HomeNavItem("SAVED") { navigateTo(::SavedScreen) }
                        HomeNavItem("PLANS") { navigateTo(::PlansScreen) }
                        HomeNavItem("SETTINGS") { navigateTo(::SettingsScreen) }
                        HomeNavItem("TRANSLATIONS") { navigateTo(::TranslationsScreen) }
                    }
                }

                LightBottomBar(
                    items = listOf(
                        LightBarButton.LightIcon(
                            icon = LightIcons.SEARCH,
                            contentDescription = "Search",
                            onClick = { navigateTo(::SearchScreen) },
                        ),
                        LightBarButton.Text(
                            text = "BOOKS",
                            onClick = { navigateTo(::BookPickerScreen) },
                        ),
                        LightBarButton.LightIcon(
                            icon = LightIcons.SETTINGS,
                            contentDescription = "Settings",
                            onClick = { navigateTo(::SettingsScreen) },
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun HomeNavItem(label: String, onClick: () -> Unit) {
    LightText(
        text = label,
        variant = LightTextVariant.Copy,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 0.75f.gridUnitsAsDp()),
    )
}
