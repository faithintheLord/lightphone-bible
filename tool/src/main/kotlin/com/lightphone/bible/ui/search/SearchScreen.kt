package com.lightphone.bible.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.lightphone.bible.ui.reader.ReaderScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.rememberKeyboardOptions
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightLazyScrollView
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextInputEditor
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp

private const val RESULT_ROW_HEIGHT = 3.5f

class SearchScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, SearchViewModel>(sealedActivity) {

    override val viewModelClass: Class<SearchViewModel>
        get() = SearchViewModel::class.java

    override fun createViewModel(): SearchViewModel = SearchViewModel(lightContext)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.uiState.collectAsState()
        val keyboardOptionsFlow = rememberKeyboardOptions()

        LightTheme(colors = themeColors) {
            if (state.showEditor) {
                key(state.editorSession) {
                    val textState = rememberTextFieldState(state.query)
                    LightTextInputEditor(
                        title = "Search Bible",
                        editorKey = state.editorSession,
                        keyboardOptionsFlow = keyboardOptionsFlow,
                        state = textState,
                        onSubmit = viewModel::submitQuery,
                        onBack = { goBack() },
                        submitIcon = LightIcons.SEARCH,
                        showBackButton = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LightThemeTokens.colors.background),
                    )
                }
            } else {
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
                        center = LightTopBarCenter.Text("Search"),
                        modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                    )

                    LightText(
                        text = state.query,
                        variant = LightTextVariant.Heading,
                        modifier = Modifier
                            .padding(horizontal = 1f.gridUnitsAsDp())
                            .clickable { viewModel.openEditor() }
                            .padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    if (state.isSearching) {
                        LightScrollView(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(start = 1f.gridUnitsAsDp()),
                        ) {
                            LightText(text = "Searching…", variant = LightTextVariant.Copy)
                        }
                    } else if (state.errorMessage != null || (state.results.isEmpty() && state.query.isNotBlank())) {
                        LightScrollView(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(start = 1f.gridUnitsAsDp()),
                        ) {
                            LightText(text = state.errorMessage ?: "No results", variant = LightTextVariant.Copy)
                        }
                    } else {
                        LightLazyScrollView(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(start = 1f.gridUnitsAsDp()),
                            uniformItemHeightGridUnits = RESULT_ROW_HEIGHT,
                        ) {
                            items(state.results, key = {
                                "${it.reference.translationId}.${it.reference.bookId}.${it.reference.chapter}.${it.reference.verse}"
                            }) { result ->
                                LightText(
                                    text = result.label,
                                    variant = LightTextVariant.Detail,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(RESULT_ROW_HEIGHT.gridUnitsAsDp())
                                        .clickable {
                                            navigateTo(screenFactory = {
                                                ReaderScreen(
                                                    it,
                                                    result.reference.bookId,
                                                    result.reference.chapter,
                                                    result.reference.verse,
                                                    translationId = result.reference.translationId,
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
}
