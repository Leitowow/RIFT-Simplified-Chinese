package dev.nohus.rift.wallet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.LoadingSpinner
import dev.nohus.rift.compose.OnVisibilityChange
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftTabBar
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.Tab
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_wallet
import dev.nohus.rift.network.Result
import dev.nohus.rift.viewModel
import dev.nohus.rift.wallet.WalletViewModel.InsightsTab
import dev.nohus.rift.wallet.WalletViewModel.PartyActivity
import dev.nohus.rift.wallet.WalletViewModel.PartyDailyGoals
import dev.nohus.rift.wallet.WalletViewModel.PartyTransactions
import dev.nohus.rift.wallet.WalletViewModel.UiState
import dev.nohus.rift.wallet.WalletViewModel.WalletTab
import dev.nohus.rift.wallet.compose.InsightsContent
import dev.nohus.rift.wallet.compose.LoyaltyPointsContent
import dev.nohus.rift.wallet.compose.OverviewContent
import dev.nohus.rift.wallet.compose.TransactionsContent
import dev.nohus.rift.wallet.compose.WalletLoadingProgress
import dev.nohus.rift.wallet.compose.WalletsContent
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

@Composable
fun WalletWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: WalletViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Wallets",
        icon = Res.drawable.window_wallet,
        state = windowState,
        onCloseClick = onCloseRequest,
        titleBarContent = { height ->
            ToolbarRow(
                state = state,
                fixedHeight = height,
                onTabSelected = viewModel::onTabClick,
            )
        },
        withContentPadding = false,
    ) {
        WalletWindowContent(
            state = state,
            onFiltersUpdate = viewModel::onFiltersUpdate,
            onInsightsTabSelected = viewModel::onInsightsTabSelected,
            onViewPartyTransactions = viewModel::onViewTransactions,
            onViewPartyDailyGoals = viewModel::onViewTransactions,
            onViewPartyActivity = viewModel::onViewTransactions,
            onReloadClick = viewModel::onReloadClick,
        )
        OnVisibilityChange(viewModel::onVisibilityChange)
    }
}

@Composable
private fun ToolbarRow(
    state: UiState,
    fixedHeight: Dp,
    onTabSelected: (WalletTab) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tabs = remember {
            WalletTab.entries.mapIndexed { index, tab ->
                val title = when (tab) {
                    WalletTab.Wallets -> "Wallets"
                    WalletTab.Overview -> "Overview"
                    WalletTab.Transactions -> "Transactions"
                    WalletTab.Insights -> "Insights"
                    WalletTab.LoyaltyPoints -> "Loyalty Points"
                }
                Tab(id = index, title = title, isCloseable = false)
            }
        }
        RiftTabBar(
            tabs = tabs,
            selectedTab = WalletTab.entries.indexOf(state.tab),
            onTabSelected = { onTabSelected(WalletTab.entries[it]) },
            onTabClosed = {},
            withUnderline = false,
            withWideTabs = true,
            fixedHeight = fixedHeight,
            modifier = Modifier.weight(1f),
        )

        AnimatedVisibility((state.loading.stage != null || state.isProcessing) && state.loadedData is Result.Success) {
            LoadingSpinner(modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun WalletWindowContent(
    state: UiState,
    onFiltersUpdate: (WalletFilters) -> Unit,
    onInsightsTabSelected: (InsightsTab) -> Unit,
    onViewPartyTransactions: (PartyTransactions) -> Unit,
    onViewPartyDailyGoals: (PartyDailyGoals) -> Unit,
    onViewPartyActivity: (PartyActivity) -> Unit,
    onReloadClick: () -> Unit,
) {
    Column {
        val offset = LocalDensity.current.run { 1.dp.toPx() }
        Box(
            modifier = Modifier
                .graphicsLayer(translationY = -offset)
                .fillMaxWidth()
                .height(1.dp)
                .background(RiftTheme.colors.borderGreyLight),
        )

        if (state.loading.stage != null && state.loadedData !is Result.Success) {
            WalletLoadingProgress(state.loading, state.loading.stage)
        } else {
            when (val resource = state.loadedData) {
                is Result.Failure -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(Spacing.large),
                    ) {
                        Text(
                            text = "Could not load wallets",
                            style = RiftTheme.typography.headerPrimary,
                            textAlign = TextAlign.Center,
                        )
                        RiftButton(
                            text = "Try again",
                            type = ButtonType.Primary,
                            onClick = onReloadClick,
                        )
                    }
                }

                is Result.Success -> {
                    Box(
                        modifier = Modifier.padding(Spacing.large),
                    ) {
                        val items = resource.data.filteredJournal
                        AnimatedContent(state.tab) { selectedTab ->
                            when (selectedTab) {
                                WalletTab.Wallets -> WalletsContent(state, resource.data, onFiltersUpdate)
                                WalletTab.Overview -> OverviewContent(state, resource.data.statistics, onFiltersUpdate)
                                WalletTab.Transactions -> TransactionsContent(state, items, onFiltersUpdate)
                                WalletTab.Insights -> InsightsContent(
                                    state = state,
                                    statistics = resource.data.statistics,
                                    onFiltersUpdate = onFiltersUpdate,
                                    onTabSelected = onInsightsTabSelected,
                                    onViewPartyTransactions = onViewPartyTransactions,
                                    onViewPartyDailyGoals = onViewPartyDailyGoals,
                                    onViewPartyActivity = onViewPartyActivity,
                                )
                                WalletTab.LoyaltyPoints -> LoyaltyPointsContent(state, resource.data, onFiltersUpdate)
                            }
                        }
                    }
                }

                null -> {}
            }
        }
    }
}
