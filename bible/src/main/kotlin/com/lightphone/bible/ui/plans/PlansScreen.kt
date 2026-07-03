package com.lightphone.bible.ui.plans

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

private const val ROW_HEIGHT = 3.5f

class PlansScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, PlansViewModel>(sealedActivity) {

    override val viewModelClass: Class<PlansViewModel>
        get() = PlansViewModel::class.java

    override fun createViewModel(): PlansViewModel = PlansViewModel(lightContext)

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
                    center = LightTopBarCenter.Text("Reading Plans"),
                    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                )

                LightLazyScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 1f.gridUnitsAsDp()),
                    uniformItemHeightGridUnits = ROW_HEIGHT,
                ) {
                    items(state.plans, key = { it.plan.id }) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ROW_HEIGHT.gridUnitsAsDp())
                                .clickable {
                                    navigateTo(screenFactory = {
                                        PlanDayScreen(it, item.plan.id, 0)
                                    })
                                }
                                .padding(vertical = 0.35f.gridUnitsAsDp()),
                        ) {
                            LightText(text = item.plan.name, variant = LightTextVariant.Heading)
                            LightText(
                                text = "${item.completedDays}/${item.plan.dayCount} days",
                                variant = LightTextVariant.Detail,
                                lighten = true,
                            )
                        }
                    }
                }
            }
        }
    }
}
