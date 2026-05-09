package dev.nohus.rift.wallet.compose

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.MulticolorIconType
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftContextMenuPopup
import dev.nohus.rift.compose.RiftDropdown
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftMulticolorIcon
import dev.nohus.rift.compose.RiftSearchField
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.deleteicon
import dev.nohus.rift.utils.formatDateTime
import dev.nohus.rift.utils.formatIsk
import dev.nohus.rift.utils.toggle
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.wallet.TransferDirection
import dev.nohus.rift.wallet.Wallet
import dev.nohus.rift.wallet.WalletDivisionsRepository
import dev.nohus.rift.wallet.WalletFilters
import dev.nohus.rift.wallet.WalletJournalItem
import dev.nohus.rift.wallet.WalletViewModel.UiState
import dev.nohus.rift.wallet.WalletViewModel.WalletTab
import dev.nohus.rift.wallet.getReferenceTypeName
import kotlin.math.absoluteValue

@Composable
fun TransactionsContent(
    state: UiState,
    items: List<WalletJournalItem>,
    onFiltersUpdate: (WalletFilters) -> Unit,
) {
    Column {
        FiltersRow(state, onFiltersUpdate)

        if (items.isNotEmpty()) {
            AppliedFiltersDescription(state, items, WalletTab.Transactions, Modifier.padding(top = Spacing.medium))

            ScrollbarLazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.large),
                contentPadding = PaddingValues(end = Spacing.medium),
                modifier = Modifier
                    .padding(top = Spacing.large)
                    .clipToBounds(),
            ) {
                items(items, key = { it.id }) {
                    JournalItem(state, it, Modifier.animateItem())
                }
            }
        }

        if (items.isEmpty()) {
            EmptyState(state)
        }
    }
}

@Composable
private fun JournalItem(
    state: UiState,
    item: WalletJournalItem,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = modifier,
    ) {
        // Transaction basics row
        Row {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val color = getAmountColor(item.amount)
                    val showCents = if (item.amount.absoluteValue < 10) true else state.showCents
                    Text(
                        text = formatIsk(item.amount, showCents),
                        style = RiftTheme.typography.headerPrimary.copy(color = color, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = getReferenceTypeName(item.refType),
                        style = RiftTheme.typography.headerSecondary,
                    )
                    val description = item.description.takeIf { it.isNotBlank() && it != "-" }
                    if (description != null) {
                        RiftTooltipArea(
                            text = description,
                        ) {
                            RiftMulticolorIcon(MulticolorIconType.Info)
                        }
                    }
                }
                Text(
                    text = formatDateTime(item.date, state.displayTimezone),
                    style = RiftTheme.typography.bodySecondary,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
            ) {
                if (item.balance != null) {
                    Text(
                        text = formatIsk(item.balance, state.showCents),
                        style = RiftTheme.typography.headerPrimary,
                    )
                }
                val walletDivisionsRepository: WalletDivisionsRepository = remember { koin.get() }
                Text(
                    text = when (item.wallet) {
                        is Wallet.Character -> state.availableWalletFilters.characters.find { it.id == item.wallet.characterId }?.name ?: "Unknown character"

                        is Wallet.Corporation -> {
                            val corporationName = state.availableWalletFilters.corporations.find { it.id == item.wallet.corporationId }?.name ?: "Unknown corporation"
                            val divisionName = walletDivisionsRepository.getDivisionNameOrDefault(item.wallet.corporationId, item.wallet.divisionId)
                            "$corporationName $divisionName"
                        }
                    },
                    style = RiftTheme.typography.bodySecondary,
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true),
        ) {
            // Transaction parties
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true),
            ) {
                if (item.firstParty != null) {
                    TypeDetailItem(item.firstParty)
                }
                if (item.firstParty != null && item.secondParty != null) {
                    Text(
                        text = "paid",
                        style = RiftTheme.typography.bodySecondary,
                    )
                }
                if (item.secondParty != null) {
                    if (item.refType == "market_escrow" && item.secondParty == item.firstParty && item.transaction?.client != null) {
                        // For market escrow, when buying, the first party is duplicated as the second party and should be the client instead
                        TypeDetailItem(item.transaction.client)
                    } else {
                        TypeDetailItem(item.secondParty)
                    }
                }
            }

            if (item.context != null && item.context != item.firstParty && item.context != item.secondParty) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true),
                ) {
                    TypeDetailItem(item.context, showCents = state.showCents)
                    if (item.refType == "corporation_account_withdrawal") {
                        Text(
                            text = "performed the withdrawal",
                            style = RiftTheme.typography.bodySecondary,
                        )
                    }
                }
            }
        }

        if (item.reasonTypeDetails.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                item.reasonTypeDetails.forEach {
                    TypeDetailItem(it, showCents = state.showCents)
                }
            }
        }
        if (item.reasonTypeDetails.isEmpty() && !item.reason.isNullOrBlank()) {
            Text(
                text = buildAnnotatedString {
                    append("Reason: \"")
                    withColor(RiftTheme.colors.textPrimary) {
                        append(item.reason)
                    }
                    append("\"")
                },
                style = RiftTheme.typography.bodySecondary,
            )
        }

        if (item.taxReceiver != null && item.tax != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true),
            ) {
                TypeDetailItem(item.taxReceiver)
                Text(
                    text = "received tax: ${formatIsk(item.tax, withCents = state.showCents)}",
                    style = RiftTheme.typography.bodySecondary,
                )
            }
        }

        // Associated market transaction
        if (item.transaction != null) {
            if (item.transaction.client != null) {
                when (item.refType) {
                    "market_transaction" -> {
                        // For market transactions (selling), the client is already shown as the first party
                    }
                    "market_escrow" -> {
                        // For market escrow (buying), the client is already shown as the second party
                    }
                    else -> {
                        TypeDetailItem(item.transaction.client)
                    }
                }
            }
            if (item.transaction.type != null) {
                TypeDetailItem(
                    item.transaction.type,
                    transaction = item.transaction,
                    showCents = state.showCents,
                )
            }
            if (item.transaction.location != null) {
                TypeDetailItem(item.transaction.location)
            }
        }
    }
}

@Composable
private fun FiltersRow(
    state: UiState,
    onFiltersUpdate: (WalletFilters) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.animateContentSize(),
    ) {
        fun updateFilters(update: WalletFilters.() -> WalletFilters) {
            onFiltersUpdate(state.filters.update())
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            val referenceTypesFilterItems = buildList<ContextMenuItem> {
                add(
                    ContextMenuItem.CheckboxItem(
                        text = "All types",
                        isSelected = state.filters.referenceTypes.isEmpty(),
                        onClick = { updateFilters { copy(referenceTypes = emptyList()) } },
                    ),
                )
                val (marketTransactionTypes, otherTypes) = state.availableWalletFilters.referenceTypes.partition {
                    it in listOf(
                        "market_transaction",
                        "market_escrow",
                    )
                }
                if (marketTransactionTypes.isNotEmpty()) {
                    add(ContextMenuItem.DividerItem)
                    add(ContextMenuItem.HeaderItem("Market Transactions"))
                    marketTransactionTypes.forEach { type ->
                        add(
                            ContextMenuItem.CheckboxItem(
                                text = getReferenceTypeName(type),
                                isSelected = type in state.filters.referenceTypes,
                                onClick = {
                                    val updated = state.filters.referenceTypes.toggle(type)
                                    updateFilters { copy(referenceTypes = updated) }
                                },
                            ),
                        )
                    }
                }
                if (otherTypes.isNotEmpty()) {
                    add(ContextMenuItem.DividerItem)
                    add(ContextMenuItem.HeaderItem("Other Types"))
                    otherTypes.forEach { type ->
                        add(
                            ContextMenuItem.CheckboxItem(
                                text = getReferenceTypeName(type),
                                isSelected = type in state.filters.referenceTypes,
                                onClick = {
                                    val updated = state.filters.referenceTypes.toggle(type)
                                    updateFilters { copy(referenceTypes = updated) }
                                },
                            ),
                        )
                    }
                }
            }
            Box(contentAlignment = Alignment.BottomStart) {
                var isShown by remember { mutableStateOf(false) }
                RiftButton(
                    text = "Types",
                    onClick = { isShown = true },
                )
                if (isShown) {
                    val offset = with(LocalDensity.current) {
                        32.dp.toPx().toInt()
                    }
                    RiftContextMenuPopup(
                        items = referenceTypesFilterItems,
                        offset = IntOffset(0, offset),
                        onDismissRequest = { isShown = false },
                    )
                }
            }

            RiftDropdown(
                items = listOf(null, TransferDirection.Income, TransferDirection.Expense),
                selectedItem = state.filters.direction,
                onItemSelected = { updateFilters { copy(direction = it) } },
                getItemName = {
                    when (it) {
                        null -> "All"
                        TransferDirection.Income -> "Income"
                        TransferDirection.Expense -> "Expenses"
                    }
                },
            )

            TimespanDropdown(state, onFiltersUpdate)

            Spacer(Modifier.weight(1f))

            RiftSearchField(
                search = state.filters.search,
                isCompact = false,
                onSearchChange = { updateFilters { copy(search = it.takeIf { it.isNotBlank() }) } },
            )
        }
        if (state.filters.party != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                Text(
                    text = "Showing only transactions involving:",
                    style = RiftTheme.typography.bodySecondary,
                )
                TypeDetailItem(state.filters.party)
                RiftTooltipArea(
                    text = "Remove filter",
                ) {
                    RiftImageButton(
                        resource = Res.drawable.deleteicon,
                        size = 20.dp,
                        onClick = { updateFilters { copy(party = null) } },
                    )
                }
            }
        }
    }
}
