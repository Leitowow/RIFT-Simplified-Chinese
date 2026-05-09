package dev.nohus.rift.wallet.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.RiftIconButton
import dev.nohus.rift.compose.RiftRadioButton
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.rememberPointerInteractionStateHolder
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.navigate_back_16px
import dev.nohus.rift.utils.formatIsk
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.wallet.TransactionGroup
import dev.nohus.rift.wallet.TransferDirection
import dev.nohus.rift.wallet.WalletFilters
import dev.nohus.rift.wallet.WalletViewModel.Statistics
import dev.nohus.rift.wallet.WalletViewModel.UiState
import dev.nohus.rift.wallet.WalletViewModel.WalletTab
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OverviewContent(
    state: UiState,
    statistics: Statistics,
    onFiltersUpdate: (WalletFilters) -> Unit,
) {
    Column {
        TimespanDropdown(state, onFiltersUpdate)

        if (statistics.journal.isNotEmpty()) {
            AppliedFiltersDescription(state, statistics.journal, WalletTab.Overview, Modifier.padding(top = Spacing.medium))

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.veryLarge),
                modifier = Modifier.padding(top = Spacing.large),
            ) {
                var shownBreakdown by remember { mutableStateOf(TransferDirection.Income) }
                var selectedGroup: TransactionGroup? by remember(shownBreakdown) { mutableStateOf(null) }
                val segments = when (shownBreakdown) {
                    TransferDirection.Income -> statistics.incomeSegments[selectedGroup] ?: statistics.incomeGroupSegments
                    TransferDirection.Expense -> statistics.expensesSegments[selectedGroup] ?: statistics.expensesGroupSegments
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                ) {
                    WalletPieChart(
                        group = selectedGroup,
                        days = state.filters.timeSpan.toDays().toInt(),
                        shownBreakdown = shownBreakdown,
                        segments = segments,
                        onSegmentClick = { segment ->
                            segment.group?.let { selectedGroup = it }
                        },
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier
                        .animateContentSize()
                        .width(IntrinsicSize.Max),
                ) {
                    val incomePointerInteraction = rememberPointerInteractionStateHolder()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                            .onClick { shownBreakdown = TransferDirection.Income }
                            .pointerInteraction(incomePointerInteraction),
                    ) {
                        RiftRadioButton(
                            isChecked = shownBreakdown == TransferDirection.Income,
                            onChecked = { shownBreakdown = TransferDirection.Income },
                            pointerInteractionStateHolder = incomePointerInteraction,
                        )
                        Column {
                            Text(
                                text = "Income",
                                style = RiftTheme.typography.bodyPrimary,
                            )
                            Text(
                                text = "+${formatIsk(statistics.income, withCents = state.showCents)}",
                                style = RiftTheme.typography.headerPrimary.copy(
                                    color = getAmountColor(1.0),
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                    val expensesPointerInteraction = rememberPointerInteractionStateHolder()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                            .onClick { shownBreakdown = TransferDirection.Expense }
                            .pointerInteraction(expensesPointerInteraction),
                    ) {
                        RiftRadioButton(
                            isChecked = shownBreakdown == TransferDirection.Expense,
                            onChecked = { shownBreakdown = TransferDirection.Expense },
                            pointerInteractionStateHolder = expensesPointerInteraction,
                        )
                        Column {
                            Text(
                                text = "Expenses",
                                style = RiftTheme.typography.bodyPrimary,
                            )
                            Text(
                                text = formatIsk(statistics.expenses, withCents = state.showCents),
                                style = RiftTheme.typography.headerPrimary.copy(
                                    color = getAmountColor(-1.0),
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    ) {
                        Spacer(Modifier.width(20.dp))
                        Column {
                            Text(
                                text = "Balance",
                                style = RiftTheme.typography.bodyPrimary,
                            )
                            Text(
                                text = formatIsk(statistics.balance, withCents = state.showCents),
                                style = RiftTheme.typography.headerPrimary.copy(
                                    color = getAmountColor(statistics.balance),
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }

                    Spacer(Modifier.height(Spacing.large))
                    AnimatedContent(segments) { segments ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                        ) {
                            segments.sortedByDescending { it.value.absoluteValue }.forEach { segment ->
                                RiftTooltipArea(
                                    text = buildAnnotatedString {
                                        withColor(RiftTheme.colors.textPrimary) {
                                            appendLine(segment.name)
                                        }
                                        withColor(RiftTheme.colors.textHighlighted) {
                                            append(formatIsk(segment.value, withCents = state.showCents))
                                        }
                                    },
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                        modifier = Modifier
                                            .hoverBackground()
                                            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                                            .padding(horizontal = Spacing.verySmall)
                                            .onClick { segment.group?.let { selectedGroup = it } }
                                            .fillMaxWidth(),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(segment.color),
                                        )
                                        val percentage = String.format("%.1f%%", segment.ratio * 100)
                                        Text(
                                            text = "$percentage ${segment.name}",
                                            style = RiftTheme.typography.bodyHighlighted,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(selectedGroup != null) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                        ) {
                            Spacer(Modifier.height(Spacing.medium))
                            RiftTooltipArea("Back", modifier = Modifier.align(Alignment.End)) {
                                RiftIconButton(
                                    icon = Res.drawable.navigate_back_16px,
                                    onClick = { selectedGroup = null },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (statistics.journal.isEmpty()) {
            EmptyState(state)
        }
    }
}
