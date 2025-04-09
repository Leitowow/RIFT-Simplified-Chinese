package dev.nohus.rift.map.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.LinkText
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftDialog
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftRadioButtonWithLabel
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.SectionTitle
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_settings
import dev.nohus.rift.generated.resources.window_warning
import dev.nohus.rift.map.settings.MapSettingsViewModel.JumpBridgeNetworkState
import dev.nohus.rift.map.settings.MapSettingsViewModel.UiState
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

@Composable
fun MapSettingsWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: MapSettingsViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    RiftWindow(
        title = "地图设置",
        icon = Res.drawable.window_settings,
        state = windowState,
        onCloseClick = onCloseRequest,
        isResizable = false,
    ) {
        MapSettingsWindowContent(
            state = state,
            onIntelPopupTimeoutSecondsChange = viewModel::onIntelPopupTimeoutSecondsChange,
            onIsUsingCompactModeChange = viewModel::onIsUsingCompactModeChange,
            onIsCharacterFollowingChange = viewModel::onIsCharacterFollowingChange,
            onIsScrollZoomInvertedChange = viewModel::onIsScrollZoomInvertedChange,
            onIsAlwaysShowingSystemsChange = viewModel::onIsAlwaysShowingSystemsChange,
            onIsUsingRiftAutopilotRouteChange = viewModel::onIsUsingRiftAutopilotRouteChange,
            onIsJumpBridgeNetworkShownChange = viewModel::onIsJumpBridgeNetworkShownChange,
            onJumpBridgeNetworkOpacityChange = viewModel::onJumpBridgeNetworkOpacityChange,
            onJumpBridgeForgetClick = viewModel::onJumpBridgeForgetClick,
            onJumpBridgeImportClick = viewModel::onJumpBridgeImportClick,
            onJumpBridgeSearchClick = viewModel::onJumpBridgeSearchClick,
            onJumpBridgeSearchImportClick = viewModel::onJumpBridgeSearchImportClick,
        )

        if (state.isJumpBridgeSearchDialogShown) {
            RiftDialog(
                title = "跳桥搜索",
                icon = Res.drawable.window_warning,
                parentState = windowState,
                state = rememberWindowState(width = 350.dp, height = Dp.Unspecified),
                onCloseClick = viewModel::onDialogDismissed,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    Text(
                        text = "此功能并非 RIFT 独有，目前没有人报告问题，但有人担心它可能会触发 ESI 的隐藏速率限制并阻止您的 IP 地址。",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                    Text(
                        text = "使用风险自负！",
                        textAlign = TextAlign.Center,
                        style = RiftTheme.typography.titlePrimary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    ) {
                        RiftButton(
                            text = "取消",
                            cornerCut = ButtonCornerCut.BottomLeft,
                            type = ButtonType.Secondary,
                            onClick = viewModel::onDialogDismissed,
                            modifier = Modifier.weight(1f),
                        )
                        RiftButton(
                            text = "确认",
                            type = ButtonType.Secondary,
                            onClick = viewModel::onJumpBridgeSearchDialogConfirmClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapSettingsWindowContent(
    state: UiState,
    onIntelPopupTimeoutSecondsChange: (Int) -> Unit,
    onIsUsingCompactModeChange: (Boolean) -> Unit,
    onIsCharacterFollowingChange: (Boolean) -> Unit,
    onIsScrollZoomInvertedChange: (Boolean) -> Unit,
    onIsAlwaysShowingSystemsChange: (Boolean) -> Unit,
    onIsUsingRiftAutopilotRouteChange: (Boolean) -> Unit,
    onIsJumpBridgeNetworkShownChange: (Boolean) -> Unit,
    onJumpBridgeNetworkOpacityChange: (Int) -> Unit,
    onJumpBridgeForgetClick: () -> Unit,
    onJumpBridgeImportClick: () -> Unit,
    onJumpBridgeSearchClick: () -> Unit,
    onJumpBridgeSearchImportClick: () -> Unit,
) {
    val intelMap = state.intelMap
    ScrollbarColumn {
        SectionTitle("用户界面", Modifier.padding(bottom = Spacing.medium))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            RiftCheckboxWithLabel(
                label = "紧凑模式",
                isChecked = intelMap.isUsingCompactMode,
                onCheckedChange = onIsUsingCompactModeChange,
            )
            RiftCheckboxWithLabel(
                label = "跟随角色移动",
                tooltip = "当您跳跃到另一个星系时\n地图将跟随",
                isChecked = intelMap.isCharacterFollowing,
                onCheckedChange = { onIsCharacterFollowingChange(it) },
            )
            RiftCheckboxWithLabel(
                label = "反转滚轮缩放",
                tooltip = "缩放方向将被反转",
                isChecked = intelMap.isInvertZoom,
                onCheckedChange = { onIsScrollZoomInvertedChange(it) },
            )
            RiftCheckboxWithLabel(
                label = "始终显示星系标签",
                tooltip = "缩放时星系标签不会隐藏",
                isChecked = intelMap.isAlwaysShowingSystems,
                onCheckedChange = { onIsAlwaysShowingSystemsChange(it) },
            )
            Text(
                text = buildAnnotatedString {
                    withColor(RiftTheme.colors.textPrimary) {
                        append("提示：在地图上按空格键自动调整大小")
                    }
                },
                style = RiftTheme.typography.bodySecondary,
            )
        }

        SectionTitle("自动导航", Modifier.padding(vertical = Spacing.medium))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Text(
                text = "设置自动导航目的地时，使用：",
                style = RiftTheme.typography.bodySecondary,
            )
            RiftRadioButtonWithLabel(
                label = "RIFT 计算路线",
                tooltip = "RIFT 地图上显示的最短路线。\n忽略您的 EVE 自动导航设置。",
                isChecked = state.isUsingRiftAutopilotRoute,
                onChecked = { onIsUsingRiftAutopilotRouteChange(true) },
            )
            RiftRadioButtonWithLabel(
                label = "EVE 计算路线",
                tooltip = "由 EVE 设置的路线。\n可能与 RIFT 地图上的路线不匹配。",
                isChecked = !state.isUsingRiftAutopilotRoute,
                onChecked = { onIsUsingRiftAutopilotRouteChange(false) },
            )
        }

        SectionTitle("跳桥网络", Modifier.padding(top = Spacing.medium))
        Column {
            AnimatedContent(state.jumpBridgeNetworkState) { networkState ->
                when (networkState) {
                    JumpBridgeNetworkState.Empty -> {
                        // Empty
                    }
                    is JumpBridgeNetworkState.Loaded -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(top = Spacing.medium, end = Spacing.medium).fillMaxWidth(),
                        ) {
                            RiftTooltipArea(
                                text = "通过复制列表到剪贴板导入跳跃桥",
                            ) {
                                Text("已加载 ${networkState.network.connections.size} 个连接的网络")
                            }
                            RiftButton(
                                text = "重置",
                                type = ButtonType.Negative,
                                onClick = onJumpBridgeForgetClick,
                            )
                        }
                    }
                }
            }
            AnimatedContent(state.jumpBridgeCopyState) { copyState ->
                when (copyState) {
                    MapSettingsViewModel.JumpBridgeCopyState.NotCopied -> {
                        if (state.jumpBridgeNetworkState == JumpBridgeNetworkState.Empty) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                modifier = Modifier.padding(top = Spacing.medium),
                            ) {
                                Text("通过复制列表到剪贴板导入跳跃桥")
                                if (state.jumpBridgeNetworkUrl != null) {
                                    Text("您可以在此页面上按 Ctrl+A, Ctrl+C：")
                                    LinkText(
                                        text = "Alliance Jump Bridge List",
                                        onClick = { state.jumpBridgeNetworkUrl.toURIOrNull()?.openBrowser() },
                                    )
                                }
                            }
                        }
                    }
                    is MapSettingsViewModel.JumpBridgeCopyState.Copied -> {
                        val tooltip = buildString {
                            val connections = copyState.network.connections.take(5).joinToString("\n") {
                                "${it.from.name} → ${it.to.name}"
                            }
                            append(connections)
                            if (copyState.network.connections.size > 5) {
                                appendLine()
                                append("And more…")
                            }
                        }
                        RiftTooltipArea(
                            text = tooltip,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(top = Spacing.medium, end = Spacing.medium).fillMaxWidth(),
                            ) {
                                Text("已复制 ${copyState.network.connections.size} 个连接的网络")
                                RiftButton(
                                    text = "导入",
                                    onClick = onJumpBridgeImportClick,
                                )
                            }
                        }
                    }
                }
            }
            AnimatedContent(state.jumpBridgeSearchState, contentKey = { it::class }) { searchState ->
                when (searchState) {
                    MapSettingsViewModel.JumpBridgeSearchState.NotSearched -> {
                        if (state.jumpBridgeNetworkState == JumpBridgeNetworkState.Empty) {
                            Column(
                                modifier = Modifier.padding(top = Spacing.medium),
                            ) {
                                Divider(
                                    color = RiftTheme.colors.divider,
                                    modifier = Modifier.padding(bottom = Spacing.medium),
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(end = Spacing.medium).fillMaxWidth(),
                                ) {
                                    Text("自动搜索？")
                                    RiftButton(
                                        text = "搜索",
                                        onClick = onJumpBridgeSearchClick,
                                    )
                                }
                            }
                        }
                    }
                    is MapSettingsViewModel.JumpBridgeSearchState.Searching -> {
                        Column(
                            modifier = Modifier.padding(top = Spacing.medium),
                        ) {
                            Divider(
                                color = RiftTheme.colors.divider,
                                modifier = Modifier.padding(bottom = Spacing.medium),
                            )
                            Text("搜索中 – ${String.format("%.1f", searchState.progress * 100)}%")
                            Text(
                                text = "找到 ${searchState.connectionsCount} 个跳跃门连接",
                                style = RiftTheme.typography.bodySecondary,
                            )
                        }
                    }
                    MapSettingsViewModel.JumpBridgeSearchState.SearchFailed -> {
                        Column(
                            modifier = Modifier.padding(top = Spacing.medium),
                        ) {
                            Divider(
                                color = RiftTheme.colors.divider,
                                modifier = Modifier.padding(bottom = Spacing.medium),
                            )
                            Text("无法搜索")
                        }
                    }
                    is MapSettingsViewModel.JumpBridgeSearchState.SearchDone -> {
                        Column(
                            modifier = Modifier.padding(top = Spacing.medium),
                        ) {
                            Divider(
                                color = RiftTheme.colors.divider,
                                modifier = Modifier.padding(bottom = Spacing.medium),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(end = Spacing.medium).fillMaxWidth(),
                            ) {
                                Text("找到 ${searchState.network.connections.size} 个连接的网络")
                                RiftButton(
                                    text = "导入",
                                    onClick = onJumpBridgeSearchImportClick,
                                )
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(state.jumpBridgeNetworkState is JumpBridgeNetworkState.Loaded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier.padding(top = Spacing.medium),
                ) {
                    RiftCheckboxWithLabel(
                        label = "在地图上显示网络",
                        isChecked = intelMap.isJumpBridgeNetworkShown,
                        onCheckedChange = onIsJumpBridgeNetworkShownChange,
                    )
                    val jumpBridgeOpacityItems = mapOf(
                        "10%" to 10,
                        "25%" to 25,
                        "50%" to 50,
                        "75%" to 75,
                        "100%" to 100,
                    )
                    RiftDropdownWithLabel(
                        label = "连接透明度：",
                        items = jumpBridgeOpacityItems.values.toList(),
                        selectedItem = intelMap.jumpBridgeNetworkOpacity,
                        onItemSelected = onJumpBridgeNetworkOpacityChange,
                        getItemName = { item -> jumpBridgeOpacityItems.entries.firstOrNull { it.value == item }?.key ?: "$item" },
                        tooltip = """
                    跳桥连接线的可见度。
                        """.trimIndent(),
                    )
                }
            }
        }

        SectionTitle("预警可见度", Modifier.padding(vertical = Spacing.medium))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            val timeoutItems = mapOf(
                "不显示" to 0,
                "10 秒" to 10,
                "30 秒" to 30,
                "1 分钟" to 60,
                "2 分钟" to 60 * 2,
                "5 分钟" to 60 * 5,
                "15 分钟" to 60 * 15,
                "无限制" to Int.MAX_VALUE,
            )
            RiftDropdownWithLabel(
                label = "自动显示弹出窗口：",
                items = timeoutItems.values.toList(),
                selectedItem = intelMap.intelPopupTimeoutSeconds,
                onItemSelected = onIntelPopupTimeoutSecondsChange,
                getItemName = { item -> timeoutItems.entries.firstOrNull { it.value == item }?.key ?: "$item" },
                tooltip = """
                    当有新信息时，预警弹出窗口将显示多长时间。
                    即使超过此时间，悬停时仍然可见。
                """.trimIndent(),
            )
        }
    }
}
