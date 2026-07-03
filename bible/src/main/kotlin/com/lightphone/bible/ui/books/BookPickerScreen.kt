package com.lightphone.bible.ui.books

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

private const val BOOK_ROW_HEIGHT = 2.5f

class BookPickerScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, BookPickerViewModel>(sealedActivity) {

    override val viewModelClass: Class<BookPickerViewModel>
        get() = BookPickerViewModel::class.java

    override fun createViewModel(): BookPickerViewModel =
        BookPickerViewModel(lightContext)

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
                    center = LightTopBarCenter.Text("Books"),
                    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                )

                LightLazyScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 1f.gridUnitsAsDp()),
                    uniformItemHeightGridUnits = BOOK_ROW_HEIGHT,
                ) {
                    item {
                        SectionHeader("OLD TESTAMENT")
                    }
                    items(state.oldTestament, key = { "ot-${it.bookId}" }) { book ->
                        BookRow(
                            name = book.commonName.ifBlank { book.name },
                            onClick = {
                                navigateTo(screenFactory = {
                                    ChapterPickerScreen(it, book.bookId, book.commonName)
                                })
                            },
                        )
                    }
                    item {
                        SectionHeader("NEW TESTAMENT")
                    }
                    items(state.newTestament, key = { "nt-${it.bookId}" }) { book ->
                        BookRow(
                            name = book.commonName.ifBlank { book.name },
                            onClick = {
                                navigateTo(screenFactory = {
                                    ChapterPickerScreen(it, book.bookId, book.commonName)
                                })
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    LightText(
        text = title,
        variant = LightTextVariant.Detail,
        lighten = true,
        modifier = Modifier
            .fillMaxWidth()
            .height(BOOK_ROW_HEIGHT.gridUnitsAsDp())
            .padding(top = 0.75f.gridUnitsAsDp()),
    )
}

@Composable
private fun BookRow(name: String, onClick: () -> Unit) {
    LightText(
        text = name,
        variant = LightTextVariant.Copy,
        modifier = Modifier
            .fillMaxWidth()
            .height(BOOK_ROW_HEIGHT.gridUnitsAsDp())
            .clickable(onClick = onClick)
            .padding(vertical = 0.5f.gridUnitsAsDp()),
    )
}
