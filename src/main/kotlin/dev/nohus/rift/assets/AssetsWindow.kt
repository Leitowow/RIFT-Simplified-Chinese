package dev.nohus.rift.assets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import dev.nohus.rift.assets.AssetsViewModel.AssetsTab
import dev.nohus.rift.assets.AssetsViewModel.FitAction
import dev.nohus.rift.assets.AssetsViewModel.UiState
import dev.nohus.rift.assets.FittingController.Fitting
import dev.nohus.rift.assets.compose.AssetsContent
import dev.nohus.rift.assets.compose.OwnersContent
import dev.nohus.rift.assets.compose.RenameLocationDialog
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.LoadingSpinner
import dev.nohus.rift.compose.LoadingSpinnerAmbient
import dev.nohus.rift.compose.OnVisibilityChange
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftTabBar
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.Tab
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_assets
import dev.nohus.rift.network.Result
import dev.nohus.rift.settings.persistence.LocationPinStatus
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

@Composable
fun AssetsWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: AssetsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Assets",
        icon = Res.drawable.window_assets,
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
        AssetsWindowContent(
            state = state,
            onFiltersUpdate = viewModel::onFiltersUpdate,
            onFitAction = viewModel::onFitAction,
            onPinChange = viewModel::onPinChange,
            onRenameClick = viewModel::onRenameClick,
            onReloadClick = viewModel::onReloadClick,
        )
        OnVisibilityChange(viewModel::onVisibilityChange)

        val renameDialog = state.renameLocationDialog
        if (renameDialog != null) {
            RenameLocationDialog(
                location = renameDialog.location,
                parentWindowState = windowState,
                onDismiss = viewModel::onRenameClose,
                onConfirmClick = viewModel::onRenameConfirm,
            )
        }
    }
}

@Composable
private fun ToolbarRow(
    state: UiState,
    fixedHeight: Dp,
    onTabSelected: (AssetsTab) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tabs = remember {
            AssetsTab.entries.mapIndexed { index, tab ->
                val title = when (tab) {
                    AssetsTab.Owners -> "Owners"
                    AssetsTab.Assets -> "Assets"
                }
                Tab(id = index, title = title, isCloseable = false)
            }
        }
        RiftTabBar(
            tabs = tabs,
            selectedTab = AssetsTab.entries.indexOf(state.tab),
            onTabSelected = { onTabSelected(AssetsTab.entries[it]) },
            onTabClosed = {},
            withUnderline = false,
            withWideTabs = true,
            fixedHeight = fixedHeight,
            modifier = Modifier.weight(1f),
        )

        AnimatedVisibility(state.isLoading && state.loadedData is Result.Success) {
            LoadingSpinner(modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun AssetsWindowContent(
    state: UiState,
    onFiltersUpdate: (AssetsFilters) -> Unit,
    onFitAction: (Fitting, FitAction) -> Unit,
    onPinChange: (Long, LocationPinStatus) -> Unit,
    onRenameClick: (locationId: Long) -> Unit,
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

        if (state.isLoading && state.loadedData !is Result.Success) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize().padding(Spacing.large),
            ) {
                LoadingSpinnerAmbient()
                Text(
                    text = "Loading assets…",
                    style = RiftTheme.typography.headlinePrimary,
                )
            }
        } else {
            when (val resource = state.loadedData) {
                is Result.Failure -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(Spacing.large),
                    ) {
                        Text(
                            text = "Could not load assets",
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
                        AnimatedContent(state.tab) { selectedTab ->
                            when (selectedTab) {
                                AssetsTab.Owners -> OwnersContent(state, resource.data, onFiltersUpdate)
                                AssetsTab.Assets -> AssetsContent(
                                    state = state,
                                    data = resource.data,
                                    onFiltersUpdate = onFiltersUpdate,
                                    onFitAction = onFitAction,
                                    onPinChange = onPinChange,
                                    onRenameClick = onRenameClick,
                                    onReloadClick = onReloadClick,
                                )
                            }
                        }
                    }
                }

                null -> {}
            }
        }
    }
}
