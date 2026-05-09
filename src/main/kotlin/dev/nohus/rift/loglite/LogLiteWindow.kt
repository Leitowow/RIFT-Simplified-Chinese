package dev.nohus.rift.loglite

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftSearchField
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.VerticalDivider
import dev.nohus.rift.compose.animateBackgroundHover
import dev.nohus.rift.compose.animateWindowBackgroundSecondaryHover
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_log
import dev.nohus.rift.loglite.LogLiteViewModel.UiState
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun LogLiteWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: LogLiteViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "LogLite",
        icon = Res.drawable.window_log,
        state = windowState,
        onCloseClick = onCloseRequest,
    ) {
        LogLiteWindowContent(
            state = state,
            onModuleFilterSelect = viewModel::onModuleFilterSelect,
            onChannelFilterSelect = viewModel::onChannelFilterSelect,
            onClientSelect = viewModel::onClientSelect,
            onToggleAllModules = viewModel::onToggleAllModules,
            onToggleAllChannels = viewModel::onToggleAllChannels,
            onSearchChange = viewModel::onSearchChange,
        )
    }
}

@Composable
private fun LogLiteWindowContent(
    state: UiState,
    onModuleFilterSelect: (String) -> Unit,
    onChannelFilterSelect: (String) -> Unit,
    onClientSelect: (Client?) -> Unit,
    onToggleAllModules: () -> Unit,
    onToggleAllChannels: () -> Unit,
    onSearchChange: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Sidebar(state, onModuleFilterSelect, onChannelFilterSelect, onToggleAllModules, onToggleAllChannels)
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            var isAutoScrolling by remember { mutableStateOf(true) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                RiftDropdownWithLabel(
                    label = "Clients (${state.clients.size}):",
                    items = state.clients,
                    selectedItem = state.selectedClient,
                    onItemSelected = onClientSelect,
                    getItemName = {
                        if (it != null) {
                            "${it.pid} @ ${it.executable}"
                        } else {
                            "None"
                        }
                    },
                    height = 24.dp,
                    modifier = Modifier.weight(1f),
                )
                RiftCheckboxWithLabel(
                    label = "Autoscroll",
                    isChecked = isAutoScrolling,
                    onCheckedChange = { isAutoScrolling = it },
                )
                RiftSearchField(
                    search = state.search,
                    isCompact = true,
                    onSearchChange = onSearchChange,
                )
            }
            LogsView(state, isAutoScrolling)
        }
    }
}

@Composable
private fun Sidebar(
    state: UiState,
    onModuleFilterSelect: (String) -> Unit,
    onChannelFilterSelect: (String) -> Unit,
    onToggleAllModules: () -> Unit,
    onToggleAllChannels: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        val width = 200.dp

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier.padding(bottom = Spacing.medium),
            ) {
                Text(
                    text = "Modules",
                    style = RiftTheme.typography.headerPrimary,
                )
                RiftButton(
                    text = "Toggle all",
                    isCompact = true,
                    onClick = onToggleAllModules,
                )
            }
            val modules = state.modules.sortedByDescending { state.moduleCounts[it] }
            ScrollbarLazyColumn(
                contentPadding = PaddingValues(Spacing.small),
                modifier = Modifier
                    .fillMaxHeight()
                    .border(1.dp, RiftTheme.colors.borderGrey)
                    .width(width),
            ) {
                items(modules) { module ->
                    val count = state.moduleCounts[module] ?: 0
                    RiftCheckboxWithLabel(
                        label = "${cleanupName(module)} ($count)",
                        isChecked = module !in state.ignoredModules,
                        onCheckedChange = { onModuleFilterSelect(module) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier.padding(bottom = Spacing.medium),
            ) {
                Text(
                    text = "Channels",
                    style = RiftTheme.typography.headerPrimary,
                )
                RiftButton(
                    text = "Toggle all",
                    isCompact = true,
                    onClick = onToggleAllChannels,
                )
            }
            val channels = state.channels.sortedByDescending { state.channelCounts[it] }
            ScrollbarLazyColumn(
                contentPadding = PaddingValues(Spacing.small),
                modifier = Modifier
                    .fillMaxHeight()
                    .border(1.dp, RiftTheme.colors.borderGrey)
                    .width(width),
            ) {
                items(channels) { channel ->
                    val count = state.channelCounts[channel] ?: 0
                    RiftCheckboxWithLabel(
                        label = "${cleanupName(channel)} ($count)",
                        isChecked = channel !in state.ignoredChannels,
                        onCheckedChange = { onChannelFilterSelect(channel) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

private fun cleanupName(name: String): String {
    return name.ifBlank { "None" }
}

@Composable
private fun LogsView(
    state: UiState,
    isAutoScrolling: Boolean,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.filteredMessages) {
        if (isAutoScrolling) {
            state.filteredMessages.lastIndex.takeIf { it > -1 }?.let {
                listState.scrollToItem(it)
            }
        }
    }

    SelectionContainer {
        ScrollbarLazyColumn(
            listState = listState,
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier
                .fillMaxHeight()
                .border(1.dp, RiftTheme.colors.borderGrey),
        ) {
            val filtered = state.filteredMessages
            items(filtered) { message ->
                val pointerState = remember { PointerInteractionStateHolder() }
                val background by pointerState.animateBackgroundHover()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier
                        .pointerInteraction(pointerState)
                        .background(background)
                        .fillMaxWidth(),
                ) {
                    MessageMetadata(state, message, pointerState)
                    val style = when (message.severity) {
                        LogSeverity.Info -> RiftTheme.typography.bodyPrimary
                        LogSeverity.Notice -> RiftTheme.typography.bodyPrimary
                        LogSeverity.Warn -> RiftTheme.typography.bodyPrimary.copy(color = RiftTheme.colors.awayYellow)
                        LogSeverity.Error -> RiftTheme.typography.bodyPrimary.copy(color = RiftTheme.colors.hotRed)
                        else -> RiftTheme.typography.bodySecondary
                    }
                    Text(
                        text = message.message,
                        style = style,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageMetadata(
    state: UiState,
    message: LogMessage,
    pointerState: PointerInteractionStateHolder,
) {
    val background by pointerState.animateWindowBackgroundSecondaryHover()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .heightIn(min = 24.dp)
            .border(1.dp, RiftTheme.colors.borderGrey)
            .background(background),
    ) {
        val time = ZonedDateTime.ofInstant(message.timestamp, state.displayTimezone).toLocalTime()
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val formattedTime = formatter.format(time)
        Text(
            text = formattedTime,
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.padding(Spacing.small),
        )
        VerticalDivider(color = RiftTheme.colors.borderGrey)
        Text(
            text = message.severity.toString(),
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.padding(Spacing.small),
        )
        VerticalDivider(color = RiftTheme.colors.borderGrey)
        Text(
            text = message.module,
            style = RiftTheme.typography.bodyHighlighted,
            modifier = Modifier.padding(Spacing.small),
        )
        VerticalDivider(color = RiftTheme.colors.borderGrey)
        Text(
            text = message.channel,
            style = RiftTheme.typography.bodyHighlighted,
            modifier = Modifier.padding(Spacing.small),
        )
    }
}
