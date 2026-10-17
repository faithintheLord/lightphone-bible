package com.lightphone.bible.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
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
import com.thelightphone.sdk.ui.designVerticalPxToSp
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.rememberLightTypography
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

class ReaderScreen(
    sealedActivity: SealedLightActivity,
    private val bookId: String,
    private val chapter: Int,
    private val initialVerse: Int = 1,
    private val translationId: String? = null,
) : LightScreen<Unit, ReaderViewModel>(sealedActivity) {

    override val viewModelClass: Class<ReaderViewModel>
        get() = ReaderViewModel::class.java

    override fun createViewModel(): ReaderViewModel =
        ReaderViewModel(lightContext, bookId, chapter, initialVerse, translationId)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.uiState.collectAsState()
        val keyboardOptionsFlow = rememberKeyboardOptions()

        val scrollState = rememberScrollState()
        val verseOffsets = remember(state.bookId, state.chapter, state.translationId, state.scrollRequest) {
            mutableStateMapOf<Int, Int>()
        }
        LaunchedEffect(state.isLoading, state.scrollRequest, state.noteEditorVerse) {
            if (!state.isLoading && state.noteEditorVerse == null) {
                state.scrollToVerse?.let { verse ->
                    val offset = snapshotFlow { verseOffsets[verse] }.filterNotNull().first()
                    withFrameNanos { }
                    scrollState.scrollTo(offset)
                }
            }
        }
        val typography = rememberLightTypography().let {
            it.copy(
                paragraph = it.paragraph.copy(
                    fontSize = it.paragraph.fontSize * state.fontScale,
                    lineHeight = it.paragraph.lineHeight * state.fontScale,
                ),
                detail = it.detail.copy(
                    fontSize = it.detail.fontSize * state.fontScale,
                    lineHeight = it.detail.lineHeight * state.fontScale,
                ),
            )
        }

        LightTheme(colors = themeColors, typography = typography) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                if (state.noteEditorVerse != null) {
                    key(state.noteEditorSession) {
                        val textState = rememberTextFieldState(state.noteEditorInitialText)
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
                            leftButton = LightBarButton.LightIcon(
                                icon = LightIcons.BACK,
                                onClick = { goBack() },
                                contentDescription = "Back",
                            ),
                            center = LightTopBarCenter.Text(
                                "${state.bookName} ${state.chapter}",
                            ),
                            rightButton = if (state.hasNextChapter) {
                                LightBarButton.LightIcon(
                                    icon = LightIcons.ARROW_RIGHT,
                                    onClick = if (state.isLoading || state.isSaving) null else viewModel::nextChapter,
                                    contentDescription = "Next chapter",
                                )
                            } else {
                                null
                            },
                            modifier = Modifier.padding(bottom = 0.25f.gridUnitsAsDp()),
                        )

                        LightText(
                            text = "${state.translationId} · Verse ${state.selectedVerse ?: initialVerse}",
                            variant = LightTextVariant.Detail,
                            modifier = Modifier.padding(horizontal = 1f.gridUnitsAsDp()),
                        )
                        if (state.hasPreviousChapter) {
                            LightText(
                                text = "PREVIOUS CHAPTER",
                                variant = LightTextVariant.Detail,
                                modifier = Modifier
                                    .padding(horizontal = 1f.gridUnitsAsDp())
                                    .clickable(enabled = !state.isLoading && !state.isSaving, onClick = viewModel::previousChapter)
                                    .padding(vertical = 0.25f.gridUnitsAsDp()),
                            )
                        }
                        if (state.compareTranslationId != null) {
                            LightText(
                                text = "${state.compareTranslationId}: " + if (state.compareEnabled) "COMPARE ON" else "COMPARE OFF",
                                variant = LightTextVariant.Detail,
                                modifier = Modifier
                                    .padding(horizontal = 1f.gridUnitsAsDp())
                                    .clickable { viewModel.toggleCompare() }
                                    .padding(bottom = 0.5f.gridUnitsAsDp()),
                            )
                        }

                        LightScrollView(
                            scrollState = scrollState,
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
                            } else if (state.verses.isEmpty()) {
                                LightText(
                                    text = "Chapter unavailable. Tap to retry.",
                                    variant = LightTextVariant.Copy,
                                    modifier = Modifier.clickable {
                                        viewModel.loadChapter(state.bookId, state.chapter, state.selectedVerse)
                                    },
                                )
                            } else {
                                state.verses.forEach { display ->
                                    VerseBlock(
                                        display = display,
                                        selected = state.selectedVerse == display.verse.verse,
                                        modifier = Modifier.onGloballyPositioned {
                                            verseOffsets[display.verse.verse] = it.positionInParent().y.toInt()
                                        },
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
                                    onClick = if (state.isLoading || state.isSaving || state.verses.isEmpty()) null else viewModel::toggleBookmark,
                                    contentDescription = "Bookmark",
                                ),
                                LightBarButton.LightIcon(
                                    icon = LightIcons.PENCIL,
                                    onClick = if (state.isLoading || state.isSaving || state.verses.isEmpty()) null else viewModel::showHighlightPicker,
                                    contentDescription = "Highlight",
                                ),
                                LightBarButton.LightIcon(
                                    icon = LightIcons.COMPOSE_MESSAGE,
                                    onClick = if (state.isLoading || state.isSaving || state.verses.isEmpty()) null else viewModel::openNoteEditor,
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
                        onRemove = viewModel::removeHighlight,
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

// Bound opacity even for highlights saved by older versions. Both Light themes keep
// their normal foreground colors, including secondary comparison text.
internal fun readerHighlightColor(argb: Int): Color = Color(argb).copy(alpha = 0.1f)

@Composable
private fun VerseBlock(
    display: VerseDisplay,
    selected: Boolean,
    modifier: Modifier = Modifier,
    compareEnabled: Boolean,
    supportsWordStudy: Boolean,
    onSelectVerse: () -> Unit,
    onWordTap: (String) -> Unit,
) {
    val highlightColor = display.highlight?.colorArgb?.let(::readerHighlightColor)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, if (selected) LightThemeTokens.colors.contentSecondary else Color.Transparent)
            .clickable(onClick = onSelectVerse)
            .background(highlightColor ?: Color.Transparent)
            .padding(vertical = 0.5f.gridUnitsAsDp()),
    ) {
        if (supportsWordStudy && display.tokens.isNotEmpty()) {
            val annotated = buildTokenAnnotatedString(display)
            val paragraphStyle = LightThemeTokens.typography.paragraph.copy(
                color = LightThemeTokens.colors.content,
                fontSize = LightThemeTokens.typography.paragraph.fontSize.value.designVerticalPxToSp(),
                lineHeight = LightThemeTokens.typography.paragraph.lineHeight.value.designVerticalPxToSp(),
            )
            ClickableText(
                text = annotated,
                style = paragraphStyle,
                onClick = { offset ->
                    onSelectVerse()
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
                variant = LightTextVariant.Detail,
                modifier = Modifier.padding(top = 0.15f.gridUnitsAsDp()),
            )
        }
    }
}

internal fun buildTokenAnnotatedString(display: VerseDisplay): AnnotatedString =
    buildAnnotatedString {
        val prefix = "${display.verse.verse} "
        append(prefix)
        // Keep the published Scripture text intact; interlinear tokens only add tap targets.
        append(display.verse.text)
        var cursor = 0
        display.tokens.forEach { token ->
            val tokenText = token.text.trim()
            if (tokenText.isNotEmpty()) {
                val start = display.verse.text.indexOf(tokenText, cursor, ignoreCase = true)
                if (start >= 0) {
                    val end = start + tokenText.length
                    token.strongsNumber?.takeIf { it.isNotBlank() }?.let { strongs ->
                        addStringAnnotation("strongs", strongs, prefix.length + start, prefix.length + end)
                    }
                    cursor = end
                }
            }
        }
    }

@Composable
private fun HighlightPickerModal(
    onSelect: (HighlightColor) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightThemeTokens.colors.background)
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
            LightText(
                text = "REMOVE HIGHLIGHT",
                variant = LightTextVariant.Copy,
                modifier = Modifier.clickable(onClick = onRemove).padding(vertical = 0.5f.gridUnitsAsDp()),
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
