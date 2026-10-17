package com.lightphone.bible.ui.books

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.lightphone.bible.data.model.BibleBooks
import com.lightphone.bible.ui.reader.ReaderScreen
import com.thelightphone.sdk.SimpleLightScreen
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

class ChapterPickerScreen(
    sealedActivity: SealedLightActivity,
    private val bookId: String,
    private val bookName: String,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val chapters = remember(bookId) {
            (1..BibleBooks.chapterCount(bookId)).toList()
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
                    center = LightTopBarCenter.Text(bookName),
                    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                )

                LightScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 1f.gridUnitsAsDp()),
                ) {
                    chapters.chunked(5).forEach { row ->
                        Column(modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp())) {
                            row.forEach { chapter ->
                                LightText(
                                    text = "Chapter $chapter",
                                    variant = LightTextVariant.Copy,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navigateTo(screenFactory = {
                                                ReaderScreen(it, bookId, chapter, 1)
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
