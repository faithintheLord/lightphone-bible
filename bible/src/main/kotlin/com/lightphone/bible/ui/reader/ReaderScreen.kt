package com.lightphone.bible.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.lightphone.bible.ui.HighlightColor
import com.lightphone.bible.ui.wordstudy.WordStudyScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.rememberKeyboardOptions
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightFullscreenModal
import com.thelightphone.sdk.ui.LightIcons
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

class ReaderScreen(
    sealedActivity: SealedLightActivity,
    private val bookId: String,
    private val chapter: Int,
    private val initialVerse: Int = 1,
) : LightScreen<Unit, ReaderViewModel>(sealedActivity) {

    override val viewModelClass: Class<ReaderViewModel>
        get() = ReaderViewModel::class.java

    override fun createViewModel(): ReaderViewModel =
        ReaderViewModel(lightContext, bookId, chapter, initialVerse)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.uiState.collectAsState()
        val keyboardOptionsFlow = rememberKeyboardOptions()

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                if (state.noteEditorVerse != null) {
                    val textState = rememberTextFieldState(state.noteEditorInitialText)
                    key(state.noteEditorSession) {
                        LightTextInputEditor(
                            title = "Note — ${state.bookName} ${state.chapter}:${state.noteEditorVerse}",
                            editorKey = state.noteEditorSession,
                            keyboardOptionsFlow = keyboardOptionsFlow,
                            state = textState,
                            onSubmit = viewModel::submitNote,
                            onBack = viewModel::cancelNoteEditor,
                            submitIcon = LightIcons.ACCEPT,
                            showBackButton = true,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LightTopBar(
                            leftButton = if (state.hasPreviousChapter) {
                                LightBarButton.LightIcon(
                                    icon = LightIcons.BACK,
                                    onClick = viewModel::previousChapter,
                                )
                            } else {
                                LightBarButton.LightIcon(
                                    icon = LightIcons.BACK,
                                    onClick = { goBack() },
                                )
                            },
                            center = LightTopBarCenter.Text(
                                "${state.bookName} ${state.chapter}",
                            ),
                            rightButton = if (state.hasNextChapter) {
                                LightBarButton.LightIcon(
                                    icon = LightIcons.ARROW_RIGHT,
                                    onClick = viewModel::nextChapter,
                                )
                            } else {
                                null
                            },
                            modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
                        )

                        if (state.compareTranslationId != null) {
                            LightText(
                                text = if (state.compareEnabled) "COMPARE ON" else "COMPARE OFF",
                                variant = LightTextVariant.Detail,
                                modifier = Modifier
                                    .padding(horizontal = 1f.gridUnitsAsDp())
                                    .clickable { viewModel.toggleCompare() }
                                    .padding(bottom = 0.5f.gridUnitsAsDp()),
                            )
                        }

                        LightScrollView(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(start = 1f.gridUnitsAsDp()),
                        ) {
                            if (state.isLoading) {
                                LightText(
                                    text = "Loading…",
                                    variant = LightTextVariant.Copy,
                                )
                            } else {
                                state.verses.forEach { display ->
                                    VerseBlock(
                                        display = display,
                                        compareEnabled = state.compareEnabled,
                                        supportsWordStudy = state.supportsWordStudy,
                                        onSelectVerse = { viewModel.selectVerse(display.verse.verse) },
                                        onWordTap = { strongs ->
                                            navigateTo(screenFactory = {
                                                WordStudyScreen(it, strongs)
                                            })
                                        },
                                    )
                                }
                            }
                        }

                        LightBottomBar(
                            items = listOf(
                                LightBarButton.LightIcon(
                                    icon = LightIcons.STAR,
                                    onClick = viewModel::toggleBookmark,
                                    contentDescription = "Bookmark",
                                ),
                                LightBarButton.LightIcon(
                                    icon = LightIcons.PENCIL,
                                    onClick = viewModel::showHighlightPicker,
                                    contentDescription = "Highlight",
                                ),
                                LightBarButton.LightIcon(
                                    icon = LightIcons.COMPOSE_MESSAGE,
                                    onClick = viewModel::openNoteEditor,
                                    contentDescription = "Note",
                                ),
                                if (state.supportsWordStudy) {
                                    LightBarButton.LightIcon(
                                        icon = LightIcons.SEARCH,
                                        onClick = {
                                            val verse = state.selectedVerse
                                                ?: state.verses.firstOrNull()?.verse?.verse
                                            val token = state.verses
                                                .find { it.verse.verse == verse }
                                                ?.tokens
                                                ?.firstOrNull { !it.strongsNumber.isNullOrBlank() }
                                            token?.strongsNumber?.let { strongs ->
                                                navigateTo(screenFactory = {
                                                    WordStudyScreen(it, strongs)
                                                })
                                            }
                                        },
                                        contentDescription = "Word study",
                                    )
                                } else {
                                    null
                                },
                            ),
                        )
                    }
                }

                if (state.highlightPickerVisible) {
                    HighlightPickerModal(
                        onSelect = viewModel::applyHighlight,
                        onDismiss = viewModel::dismissHighlightPicker,
                    )
                }

                state.errorMessage?.let { message ->
                    LightFullscreenModal(
                        message = message,
                        onClose = viewModel::dismissError,
                    )
                }
            }
        }
    }
}

@Composable
private fun VerseBlock(
    display: VerseDisplay,
    compareEnabled: Boolean,
    supportsWordStudy: Boolean,
    onSelectVerse: () -> Unit,
    onWordTap: (String) -> Unit,
) {
    val highlightColor = display.highlight?.colorArgb?.let { Color(it) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectVerse)
            .background(highlightColor ?: Color.Transparent)
            .padding(vertical = 0.5f.gridUnitsAsDp()),
    ) {
        if (supportsWordStudy && display.tokens.isNotEmpty()) {
            val annotated = buildTokenAnnotatedString(display)
            ClickableText(
                text = annotated,
                style = LightThemeTokens.typography.paragraph,
                onClick = { offset ->
                    annotated.getStringAnnotations("strongs", offset, offset)
                        .firstOrNull()
                        ?.item
                        ?.let(onWordTap)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LightText(
                text = "${display.verse.verse} ${display.verse.text}",
                variant = LightTextVariant.Paragraph,
            )
        }

        if (compareEnabled && display.compareText != null) {
            LightText(
                text = display.compareText,
                variant = LightTextVariant.Detail,
                lighten = true,
                modifier = Modifier.padding(top = 0.25f.gridUnitsAsDp(), start = 1f.gridUnitsAsDp()),
            )
        }

        if (display.hasNote || display.isBookmarked) {
            val markers = buildList {
                if (display.isBookmarked) add("★")
                if (display.hasNote) add("✎")
            }.joinToString(" ")
            LightText(
                text = markers,
                variant = LightTextVariant.Micro,
                modifier = Modifier.padding(top = 0.15f.gridUnitsAsDp()),
            )
        }
    }
}

private fun buildTokenAnnotatedString(display: VerseDisplay): AnnotatedString =
    buildAnnotatedString {
        append("${display.verse.verse} ")
        display.tokens.forEachIndexed { index, token ->
            val strongs = token.strongsNumber
            if (!strongs.isNullOrBlank()) {
                pushStringAnnotation(tag = "strongs", annotation = strongs)
                append(token.text)
                pop()
            } else {
                append(token.text)
            }
            if (index < display.tokens.lastIndex) append(" ")
        }
    }

@Composable
private fun HighlightPickerModal(
    onSelect: (HighlightColor) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(androidx.compose.ui.Alignment.Center)
                .padding(horizontal = 1f.gridUnitsAsDp()),
        ) {
            LightText(
                text = "HIGHLIGHT COLOR",
                variant = LightTextVariant.Heading,
                modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
            )
            HighlightColor.entries.forEach { color ->
                LightText(
                    text = color.name,
                    variant = LightTextVariant.Copy,
                    modifier = Modifier
                        .clickable { onSelect(color) }
                        .padding(vertical = 0.75f.gridUnitsAsDp()),
                )
            }
        }
    }
}
