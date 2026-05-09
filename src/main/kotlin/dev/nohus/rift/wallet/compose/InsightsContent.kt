package dev.nohus.rift.wallet.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.BorderedToken
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftSearchField
import dev.nohus.rift.compose.RiftTabBar
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.Tab
import dev.nohus.rift.compose.fadingRightEdge
import dev.nohus.rift.compose.theme.EveColors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.utils.formatDate
import dev.nohus.rift.utils.formatIsk
import dev.nohus.rift.utils.formatIskReadable
import dev.nohus.rift.utils.formatNumber
import dev.nohus.rift.utils.plural
import dev.nohus.rift.wallet.GetNpcShipGroupUseCase
import dev.nohus.rift.wallet.TypeDetail
import dev.nohus.rift.wallet.WalletFilters
import dev.nohus.rift.wallet.WalletJournalItem
import dev.nohus.rift.wallet.WalletViewModel.InsightsTab
import dev.nohus.rift.wallet.WalletViewModel.PartyActivity
import dev.nohus.rift.wallet.WalletViewModel.PartyDailyGoals
import dev.nohus.rift.wallet.WalletViewModel.PartyTransactions
import dev.nohus.rift.wallet.WalletViewModel.Statistics
import dev.nohus.rift.wallet.WalletViewModel.UiState
import dev.nohus.rift.wallet.WalletViewModel.WalletTab
import dev.nohus.rift.wallet.getReferenceTypeName
import dev.nohus.rift.wallet.isMatching
import dev.nohus.rift.wallet.toEveLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue

@Composable
fun InsightsContent(
    state: UiState,
    statistics: Statistics,
    onFiltersUpdate: (WalletFilters) -> Unit,
    onTabSelected: (InsightsTab) -> Unit,
    onViewPartyTransactions: (PartyTransactions) -> Unit,
    onViewPartyDailyGoals: (PartyDailyGoals) -> Unit,
    onViewPartyActivity: (PartyActivity) -> Unit,
) {
    Column {
        var search by remember { mutableStateOf("") }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            TimespanDropdown(state, onFiltersUpdate)

            Spacer(Modifier.weight(1f))

            RiftSearchField(
                search = state.filters.search,
                isCompact = false,
                onSearchChange = { search = it },
            )
        }

        if (statistics.journal.isNotEmpty()) {
            AppliedFiltersDescription(state, statistics.journal, WalletTab.Insights, Modifier.padding(top = Spacing.medium))

            val tabs = remember {
                InsightsTab.entries.mapIndexed { index, tab ->
                    val title = when (tab) {
                        InsightsTab.IncomeByParty -> "Income by party"
                        InsightsTab.ExpenseByParty -> "Expense by party"
                        InsightsTab.BalanceByParty -> "Balance by party"
                        InsightsTab.DestroyedRatsByParty -> "Wanted ships destroyed"
                        InsightsTab.DailyGoals -> "Daily goals"
                        InsightsTab.Activity -> "Activity"
                    }
                    Tab(id = index, title = title, isCloseable = false)
                }
            }
            RiftTabBar(
                tabs = tabs,
                selectedTab = InsightsTab.entries.indexOf(state.insightsTab),
                onTabClosed = {},
                onTabSelected = { onTabSelected(InsightsTab.entries[it]) },
                modifier = Modifier.padding(top = Spacing.medium),
            )

            ScrollbarLazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.large),
                contentPadding = PaddingValues(end = Spacing.medium, top = Spacing.large),
                modifier = Modifier.clipToBounds(),
            ) {
                if (state.insightsTab == InsightsTab.IncomeByParty || state.insightsTab == InsightsTab.ExpenseByParty || state.insightsTab == InsightsTab.BalanceByParty) {
                    val partyTransactions = when (state.insightsTab) {
                        InsightsTab.IncomeByParty -> statistics.incomeByParty
                        InsightsTab.ExpenseByParty -> statistics.expensesByParty
                        InsightsTab.BalanceByParty -> statistics.balanceByParty
                        else -> emptyList()
                    }.filter { it.party.isMatching(search) }

                    items(partyTransactions, { state.insightsTab to it.party }) { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier.animateItem(),
                        ) {
                            TypeDetailItem(item.party)

                            val color = getAmountColor(item.total)
                            val showCents = if (item.total.absoluteValue < 10) true else state.showCents
                            RiftTooltipArea(
                                text = formatIsk(item.total, showCents),
                            ) {
                                Text(
                                    text = formatIskReadable(item.total),
                                    style = RiftTheme.typography.headerPrimary.copy(color = color, fontWeight = FontWeight.Bold),
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .padding(end = Spacing.small)
                                    .fadingRightEdge()
                                    .padding(end = Spacing.large)
                                    .weight(1f),
                            ) {
                                RiftTooltipArea(
                                    text = item.refTypes.joinToString("\n") {
                                        "${getReferenceTypeName(it.first)}: ${formatIsk(it.second, state.showCents)}"
                                    },
                                ) {
                                    Text(
                                        text = item.refTypes.joinToString { getReferenceTypeName(it.first) },
                                        style = RiftTheme.typography.headerSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Visible,
                                        softWrap = false,
                                    )
                                }
                            }

                            RiftButton(
                                text = "View all",
                                type = ButtonType.Secondary,
                                onClick = { onViewPartyTransactions(item) },
                            )
                        }
                    }

                    if (partyTransactions.isEmpty()) {
                        item {
                            EmptyState()
                        }
                    }
                } else if (state.insightsTab == InsightsTab.DestroyedRatsByParty) {
                    val partyTransactions = statistics.destroyedRatsByParty.filter { it.party.isMatching(search) }

                    items(partyTransactions, { state.insightsTab to it.party }) { item ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                            modifier = Modifier.animateItem(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            ) {
                                TypeDetailItem(item.party)

                                val color = getAmountColor(item.totalBounty)
                                val showCents = if (item.totalBounty.absoluteValue < 10) true else state.showCents
                                RiftTooltipArea(
                                    text = formatIsk(item.totalBounty, showCents),
                                ) {
                                    Text(
                                        text = formatIskReadable(item.totalBounty),
                                        style = RiftTheme.typography.headerPrimary.copy(color = color, fontWeight = FontWeight.Bold),
                                    )
                                }

                                Text(
                                    text = "${formatNumber(item.totalRats)} ship${item.totalRats.plural} destroyed",
                                    style = RiftTheme.typography.headerSecondary,
                                )
                            }

                            Text(
                                text = "Destroyed ships by system",
                                style = RiftTheme.typography.headerSecondary,
                                modifier = Modifier.padding(top = Spacing.small),
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                            ) {
                                item.countBySystem.forEach { (system, count) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        TypeDetailItem(system)

                                        BorderedToken(32.dp) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = Spacing.small),
                                            ) {
                                                Text(
                                                    text = formatNumber(count),
                                                    style = RiftTheme.typography.bodySecondary,
                                                )
                                                Text(
                                                    text = "ship${count.plural}",
                                                    style = RiftTheme.typography.detailSecondary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            val getNpcShipGroupUseCase: GetNpcShipGroupUseCase = remember { koin.get() }
                            item.destroyedRats
                                .groupBy { getNpcShipGroupUseCase(it.type) }
                                .entries
                                .sortedByDescending { it.key?.ordinal }
                                .forEach { (group, ships) ->
                                    val total = ships.sumOf { it.count ?: 0 }
                                    Text(
                                        text = "Destroyed ${formatNumber(total)}x ${group?.displayName ?: "Other"}",
                                        style = RiftTheme.typography.headerSecondary,
                                        modifier = Modifier.padding(top = Spacing.small),
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                    ) {
                                        ships.sortedWith(compareBy({ it.type.groupId }, { it.type.id })).forEach {
                                            TypeDetailItem(it, showCents = state.showCents)
                                        }
                                    }
                                }
                        }
                    }

                    if (partyTransactions.isEmpty()) {
                        item {
                            EmptyState()
                        }
                    }
                } else if (state.insightsTab == InsightsTab.DailyGoals) {
                    val dailyGoals = statistics.dailyGoals.filter { it.party.isMatching(search) }
                    // Adding 1 day because the oldest day in the data is likely incomplete and so misleading
                    val oldestDay = statistics.journal.lastOrNull()?.date?.toEveLocalDate()?.plusDays(1)
                    val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy").withLocale(Locale.ENGLISH)

                    items(dailyGoals, { state.insightsTab to it.party }) { item ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                            modifier = Modifier.animateItem(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            ) {
                                TypeDetailItem(item.party)

                                val color = getAmountColor(item.totalReward)
                                val showCents = if (item.totalReward.absoluteValue < 10) true else state.showCents
                                RiftTooltipArea(
                                    text = formatIsk(item.totalReward, showCents),
                                ) {
                                    Text(
                                        text = formatIskReadable(item.totalReward),
                                        style = RiftTheme.typography.headerPrimary.copy(color = color, fontWeight = FontWeight.Bold),
                                    )
                                }

                                Text(
                                    text = "${formatNumber(item.totalGoals)} daily goals completed on ${formatNumber(item.dailyGoals.size)} days",
                                    style = RiftTheme.typography.headerSecondary,
                                    modifier = Modifier.weight(1f),
                                )

                                RiftButton(
                                    text = "View all",
                                    type = ButtonType.Secondary,
                                    onClick = { onViewPartyDailyGoals(item) },
                                )
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                            ) {
                                val today = Instant.now().toEveLocalDate()
                                val latestMonth = item.dailyGoals.first().first.withDayOfMonth(1)
                                val oldestMonth = item.dailyGoals.last().first.withDayOfMonth(1)
                                var month = latestMonth
                                do {
                                    Text(
                                        text = monthYear.format(month),
                                        style = RiftTheme.typography.detailSecondary,
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                    ) {
                                        val latestDay = month.withDayOfMonth(month.lengthOfMonth()).coerceAtMost(today)
                                        val firstDay = month.coerceAtLeast(oldestDay ?: today)
                                        var day = latestDay
                                        val goalsThisMonth = item.dailyGoals
                                            .filter { it.first in firstDay..latestDay } // dropUntil, takeWhile
                                            .associateBy { it.first }
                                            .mapValues { it.value.second }
                                        do {
                                            val goalsThisDay = goalsThisMonth[day] ?: emptyList()
                                            DailyGoalDay(day, goalsThisDay)
                                            day = day.minusDays(1)
                                        } while (day >= firstDay)
                                    }

                                    month = month.minusMonths(1)
                                } while (month >= oldestMonth)
                            }
                        }
                    }

                    if (dailyGoals.isEmpty()) {
                        item {
                            EmptyState()
                        }
                    }
                } else if (state.insightsTab == InsightsTab.Activity) {
                    val activity = statistics.activity.filter { it.party.isMatching(search) }
                    // Adding 1 day because the oldest day in the data is likely incomplete and so misleading
                    val oldestDay = statistics.journal.lastOrNull()?.date?.toEveLocalDate()?.plusDays(1)
                    val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy").withLocale(Locale.ENGLISH)

                    items(activity, { state.insightsTab to it.party }) { item ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                            modifier = Modifier.animateItem(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            ) {
                                TypeDetailItem(item.party)

                                val color = getAmountColor(item.totalBalance)
                                val showCents = if (item.totalBalance.absoluteValue < 10) true else state.showCents
                                RiftTooltipArea(
                                    text = formatIsk(item.totalBalance, showCents),
                                ) {
                                    Text(
                                        text = formatIskReadable(item.totalBalance),
                                        style = RiftTheme.typography.headerPrimary.copy(color = color, fontWeight = FontWeight.Bold),
                                    )
                                }
                                Spacer(Modifier.weight(1f))

                                RiftButton(
                                    text = "View all",
                                    type = ButtonType.Secondary,
                                    onClick = { onViewPartyActivity(item) },
                                )
                            }

                            Column(
                                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                            ) {
                                val today = Instant.now().toEveLocalDate()
                                val latestMonth = item.transactions.first().first.withDayOfMonth(1)
                                val oldestMonth = item.transactions.last().first.withDayOfMonth(1)
                                var month = latestMonth
                                do {
                                    Text(
                                        text = monthYear.format(month),
                                        style = RiftTheme.typography.detailSecondary,
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                    ) {
                                        val latestDay = month.withDayOfMonth(month.lengthOfMonth()).coerceAtMost(today)
                                        val firstDay = month.coerceAtLeast(oldestDay ?: today)
                                        var day = latestDay
                                        val transactionsThisMonth = item.transactions
                                            .filter { it.first in firstDay..latestDay } // dropUntil, takeWhile
                                            .associateBy { it.first }
                                            .mapValues { it.value.second }
                                        do {
                                            val transactionsThisDay = transactionsThisMonth[day] ?: emptyList()
                                            ActivityDay(day, transactionsThisDay)
                                            day = day.minusDays(1)
                                        } while (day >= firstDay)
                                    }

                                    month = month.minusMonths(1)
                                } while (month >= oldestMonth)
                            }
                        }
                    }

                    if (activity.isEmpty()) {
                        item {
                            EmptyState()
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

@Composable
private fun DailyGoalDay(date: LocalDate, goals: List<TypeDetail.DailyGoal>) {
    val color = when (goals.size) {
        0 -> EveColors.gunmetalGrey
        1 -> EveColors.airTurquoise
        else -> EveColors.leafyGreen
    }
    RiftTooltipArea(
        text = buildAnnotatedString {
            withStyle(RiftTheme.typography.headerSecondary.toSpanStyle()) {
                appendLine(formatDate(date))
            }
            if (goals.isEmpty()) {
                withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                    appendLine("No daily goals completed")
                }
            } else {
                withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                    appendLine("Completed ${goals.size} daily goal${goals.size.plural}:\n")
                }
                goals.groupingBy { it.name }.eachCount().forEach { (goal, count) ->
                    if (count > 1) {
                        appendLine("${count}x $goal")
                    } else {
                        appendLine(goal)
                    }
                }
            }
        }.trim() as AnnotatedString,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(17.dp),
        ) {
            val blur = 4f
            if (goals.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(17.dp)
                        .graphicsLayer(renderEffect = BlurEffect(blur, blur, edgeTreatment = TileMode.Decal))
                        .background(color),
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(16.dp)
                    .background(color),
            ) {
                if (goals.isNotEmpty()) {
                    Text(
                        text = "${goals.size}",
                        style = RiftTheme.typography.detailPrimary.copy(fontWeight = FontWeight.Bold, shadow = null),
                        color = EveColors.coalBlack,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityDay(date: LocalDate, items: List<WalletJournalItem>) {
    val balance = items.sumOf { it.amount }
    val alpha = (balance.absoluteValue / 10_000_000).toFloat().coerceIn(0.2f..1f)
    val blur = (balance.absoluteValue / 10_000_000).toFloat().coerceIn(0.0f..4f)
    val color = when {
        balance < 0 -> EveColors.hotRed.copy(alpha = alpha)
        balance == 0.0 -> EveColors.gunmetalGrey
        balance > 0 -> EveColors.leafyGreen.copy(alpha = alpha)
        else -> Color.White // Should never happen
    }
    RiftTooltipArea(
        text = buildAnnotatedString {
            withStyle(RiftTheme.typography.headerSecondary.toSpanStyle()) {
                appendLine(formatDate(date))
            }
            if (items.isEmpty()) {
                withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                    appendLine("No wallet transactions")
                }
            } else {
                withStyle(RiftTheme.typography.bodySecondary.copy(color = getAmountColor(balance), fontWeight = FontWeight.Bold).toSpanStyle()) {
                    appendLine(formatIsk(balance, withCents = false))
                }
                items.groupingBy { it.refType }.eachCount().forEach { (refType, count) ->
                    appendLine("${count}x ${getReferenceTypeName(refType)}")
                }
            }
        }.trim() as AnnotatedString,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(17.dp),
        ) {
            if (items.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(17.dp)
                        .graphicsLayer(renderEffect = BlurEffect(blur, blur, edgeTreatment = TileMode.Decal))
                        .background(color),
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(16.dp)
                    .background(color),
            ) {}
        }
    }
}

@Composable
private fun EmptyState() {
    Text(
        text = "No transactions",
        style = RiftTheme.typography.displaySecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.large),
    )
}
