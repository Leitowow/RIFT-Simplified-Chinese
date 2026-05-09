package dev.nohus.rift.opportunities.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.LoadingSpinner
import dev.nohus.rift.compose.RiftToggleButton
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftVerticalGlowLine
import dev.nohus.rift.compose.Side
import dev.nohus.rift.compose.ToggleButtonType
import dev.nohus.rift.compose.getActiveWindowTransitionSpec
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.rememberPointerInteractionStateHolder
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.contact_tag
import dev.nohus.rift.generated.resources.house_16px
import dev.nohus.rift.opportunities.OpportunitiesViewModel
import dev.nohus.rift.opportunities.OpportunitiesViewModel.OpportunityLifecycleFilter
import dev.nohus.rift.opportunities.OpportunityCategoryFilter
import dev.nohus.rift.opportunities.OpportunityCategoryFilterType
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun SideNavigation(
    state: OpportunitiesViewModel.UiState,
    onLifecycleFilterChange: (OpportunityLifecycleFilter) -> Unit,
    onClick: (OpportunityCategoryFilter?) -> Unit,
) {
    val activeWindowTransition = updateTransition(LocalWindowInfo.current.isWindowFocused)
    val colorWindowTransitionSpec = getActiveWindowTransitionSpec<Color>()
    val color by activeWindowTransition.animateColor(colorWindowTransitionSpec) {
        if (it) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.025f)
    }

    Column(
        modifier = Modifier
            .background(color)
            .width(240.dp)
            .fillMaxHeight(),
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(start = 8.dp),
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier.padding(end = 8.dp, top = 8.dp),
                ) {
                    RiftToggleButton(
                        text = "Current",
                        isSelected = state.lifecycleFilter == OpportunityLifecycleFilter.Active,
                        type = ToggleButtonType.Left,
                        onClick = { onLifecycleFilterChange(OpportunityLifecycleFilter.Active) },
                        modifier = Modifier.weight(1f),
                    )
                    RiftToggleButton(
                        text = "History",
                        isSelected = state.lifecycleFilter == OpportunityLifecycleFilter.History,
                        type = ToggleButtonType.Right,
                        onClick = { onLifecycleFilterChange(OpportunityLifecycleFilter.History) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            val allFilters = OpportunityCategoryFilter::class.sealedSubclasses.map { it.objectInstance!! }
            val primaryFilter = if (state.participatingFilter) null else state.primaryFilter

            item {
                Header("Features")
            }
            item {
                Item(
                    text = "All",
                    count = state.categoryFilters.opportunityCount[null] ?: 0,
                    icon = Res.drawable.house_16px,
                    isSelected = primaryFilter == null,
                    onClick = { onClick(null) },
                    modifier = Modifier.animateItem(),
                )
            }
            val features = allFilters.filter { it.type == OpportunityCategoryFilterType.Feature }
            items(features, key = { it }) {
                Item(state.categoryFilters, primaryFilter, it, onClick, Modifier.animateItem())
            }

            item {
                Header("Career Paths")
            }
            val careerPaths = allFilters.filter { it.type == OpportunityCategoryFilterType.CareerPath }
            items(careerPaths, key = { it }) {
                Item(state.categoryFilters, primaryFilter, it, onClick, Modifier.animateItem())
            }

            item {
                Header("Other Tags")
            }
            val activities = allFilters
                .filter { it.type == OpportunityCategoryFilterType.Activity }
                .filter { (state.categoryFilters.opportunityCount[it] ?: 0) > 0 || it == state.primaryFilter }
            items(activities, key = { it }) {
                Item(state.categoryFilters, primaryFilter, it, onClick, Modifier.animateItem())
            }
        }
        Spacer(Modifier.weight(1f))
        LoadingFooter(isLoading = state.isLoading)
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text = text,
        style = RiftTheme.typography.detailSecondary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 8.dp, end = 8.dp),
    )
}

@Composable
private fun Item(
    categoryFilters: OpportunitiesViewModel.CategoryFilters,
    primaryFilter: OpportunityCategoryFilter?,
    filter: OpportunityCategoryFilter,
    onClick: (OpportunityCategoryFilter) -> Unit,
    modifier: Modifier,
) {
    val showIcon = filter.type in listOf(OpportunityCategoryFilterType.Feature, OpportunityCategoryFilterType.CareerPath)
    Item(
        text = filter.name,
        count = categoryFilters.opportunityCount[filter] ?: 0,
        icon = filter.icon.takeIf { showIcon },
        isSelected = primaryFilter == filter,
        onClick = { onClick(filter) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Item(
    text: String,
    count: Int,
    icon: DrawableResource?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val isEnabled = count > 0
    val pointerInteractionStateHolder = rememberPointerInteractionStateHolder()
    Box(
        modifier = modifier
            .onClick { onClick() }
            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
            .pointerInteraction(pointerInteractionStateHolder)
            .height(IntrinsicSize.Min)
            .hoverBackground(
                pressColor = RiftTheme.colors.backgroundPrimary,
                pointerInteractionStateHolder = pointerInteractionStateHolder,
                isSelected = isSelected,
            ),
    ) {
        val activeWindowTransition = updateTransition(LocalWindowInfo.current.isWindowFocused)
        val colorWindowTransitionSpec = getActiveWindowTransitionSpec<Color>()
        val glowLineColor by activeWindowTransition.animateColor(colorWindowTransitionSpec) {
            if (it) RiftTheme.colors.borderPrimaryLight else RiftTheme.colors.textPrimary
        }
        if (isSelected) {
            RiftVerticalGlowLine(pointerInteractionStateHolder, glowLineColor, Side.Left, isSelected = LocalWindowInfo.current.isWindowFocused)
        }

        val color = when {
            isSelected && isEnabled -> RiftTheme.colors.textHighlighted
            isEnabled -> RiftTheme.colors.textPrimary
            else -> RiftTheme.colors.textDisabled
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(top = 4.dp, bottom = 4.dp, start = 8.dp, end = 16.dp),
        ) {
            if (icon != null) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(color),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(16.dp),
                )
            } else if (isSelected) {
                Image(
                    painter = painterResource(Res.drawable.contact_tag),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(color),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(16.dp),
                )
            }
            Text(
                text = text,
                style = RiftTheme.typography.bodyPrimary.copy(color = color),
                modifier = Modifier.padding(start = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            if (isEnabled) {
                Text(
                    text = "$count",
                    style = when {
                        isSelected -> RiftTheme.typography.bodyHighlighted
                        else -> RiftTheme.typography.bodySecondary
                    },
                )
            }
        }
    }
}

@Composable
private fun LoadingFooter(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.medium),
    ) {
        AnimatedVisibility(isLoading) {
            RiftTooltipArea("Loading opportunities…") {
                LoadingSpinner(
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}
