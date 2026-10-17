package com.lightphone.bible.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.lightphone.bible.ui.HighlightColor
import com.thelightphone.sdk.ui.LightThemeColors
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

class ReaderHighlightTest {
    @Test
    fun `highlight text remains readable in both Light themes including old opaque colors`() {
        val storedColors = HighlightColor.entries.map { it.argb } + listOf(-1, 0xFF000000.toInt())
        for (theme in listOf(LightThemeColors.Light, LightThemeColors.Dark)) {
            for (argb in storedColors) {
                val background = readerHighlightColor(argb).compositeOver(theme.background)
                for (foreground in listOf(theme.content, theme.contentSecondary)) {
                    assertTrue(contrastRatio(foreground, background) >= 4.5f)
                }
            }
        }
    }

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val first = foreground.luminance()
        val second = background.luminance()
        return (max(first, second) + 0.05f) / (min(first, second) + 0.05f)
    }
}
