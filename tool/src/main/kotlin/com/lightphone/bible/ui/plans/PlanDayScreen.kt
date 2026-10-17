package com.lightphone.bible.ui.plans

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
import com.lightphone.bible.ui.reader.ReaderScreen
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

class PlanDayScreen(
    sealedActivity: SealedLightActivity,
    private val planId: String,
    private val dayIndex: Int,
) : LightScreen<Unit, PlanDayViewModel>(sealedActivity) {

    override val viewModelClass: Class<PlanDayViewModel>
        get() = PlanDayViewModel::class.java

    override fun createViewModel(): PlanDayViewModel =
        PlanDayViewModel(lightContext, planId, dayIndex)

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
                    center = LightTopBarCenter.Text(
                        "Day ${state.dayIndex + 1}",
                    ),
                    rightButton = if (state.dayIndex + 1 < state.dayCount) {
                        LightBarButton.LightIcon(
                            icon = LightIcons.ARROW_RIGHT,
                            onClick = if (state.isLoading || state.isSaving) null else {
                                { viewModel.showDay(state.dayIndex + 1) }
                            },
                        )
                    } else {
                        null
                    },
                    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                )

                LightScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 1f.gridUnitsAsDp()),
                ) {
                    LightText(
                        text = state.planName,
                        variant = LightTextVariant.Detail,
                        lighten = true,
                        modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                    )
                    if (state.isLoading || state.errorMessage != null) {
                        LightText(
                            text = state.errorMessage ?: "Loading…",
                            variant = LightTextVariant.Copy,
                        )
                    }
                    state.readings.forEach { reading ->
                        LightText(
                            text = reading.label,
                            variant = LightTextVariant.Copy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navigateTo(screenFactory = {
                                        ReaderScreen(
                                            it,
                                            reading.bookId,
                                            reading.chapter,
                                            reading.verse,
                                        )
                                    })
                                }
                                .padding(vertical = 0.75f.gridUnitsAsDp()),
                        )
                    }
                }

                LightBottomBar(
                    items = listOf(
                        if (state.dayIndex > 0) {
                            LightBarButton.LightIcon(
                                icon = LightIcons.BACK,
                                contentDescription = "Previous day",
                                onClick = if (state.isLoading || state.isSaving) null else {
                                    { viewModel.showDay(state.dayIndex - 1) }
                                },
                            )
                        } else {
                            null
                        },
                        LightBarButton.Text(
                            text = if (state.completed) "DONE ✓" else "MARK DONE",
                            onClick = if (state.isLoading || state.isSaving) null else viewModel::toggleComplete,
                        ),
                        if (state.dayIndex + 1 < state.dayCount) {
                            LightBarButton.LightIcon(
                                icon = LightIcons.ARROW_RIGHT,
                                contentDescription = "Next day",
                                onClick = if (state.isLoading || state.isSaving) null else {
                                    { viewModel.showDay(state.dayIndex + 1) }
                                },
                            )
                        } else {
                            null
                        },
                    ),
                )
            }
        }
    }
}
