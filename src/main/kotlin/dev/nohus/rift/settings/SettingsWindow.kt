package dev.nohus.rift.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.clipboard.Clipboard
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.FlagIcon
import dev.nohus.rift.compose.LinkText
import dev.nohus.rift.compose.MulticolorIconType
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RequirementIcon
import dev.nohus.rift.compose.RiftAutocompleteTextField
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftDialog
import dev.nohus.rift.compose.RiftDropdown
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftFileChooserButton
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftMessageDialog
import dev.nohus.rift.compose.RiftMulticolorIcon
import dev.nohus.rift.compose.RiftRadioButtonWithLabel
import dev.nohus.rift.compose.RiftSliderWithLabel
import dev.nohus.rift.compose.RiftSolarSystemChip
import dev.nohus.rift.compose.RiftTabBar
import dev.nohus.rift.compose.RiftTable
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.SectionTitle
import dev.nohus.rift.compose.Tab
import dev.nohus.rift.compose.TableCell
import dev.nohus.rift.compose.TableRow
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.configurationpack.ConfigurationPackRepository
import dev.nohus.rift.configurationpack.displayName
import dev.nohus.rift.di.koin
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitParallax
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitStandings
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.deleteicon
import dev.nohus.rift.generated.resources.window_settings
import dev.nohus.rift.generated.resources.window_warning
import dev.nohus.rift.notifications.NotificationEditWindow
import dev.nohus.rift.repositories.SolarSystemChipState
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.settings.SettingsViewModel.JumpBridgeCopyState
import dev.nohus.rift.settings.SettingsViewModel.JumpBridgeSearchState
import dev.nohus.rift.settings.SettingsViewModel.SettingsTab
import dev.nohus.rift.settings.SettingsViewModel.SovereigntyUpgradesCopyState
import dev.nohus.rift.settings.SettingsViewModel.UiState
import dev.nohus.rift.settings.persistence.CharacterPortraitsParallaxStrength
import dev.nohus.rift.settings.persistence.CharacterPortraitsStandingsTargets
import dev.nohus.rift.settings.persistence.ConfigurationPack
import dev.nohus.rift.standings.Standing
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.OperatingSystem.MacOs
import dev.nohus.rift.utils.formatDate
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.roundSecurity
import dev.nohus.rift.utils.toURIOrNull
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import javax.swing.JFileChooser
import kotlin.io.path.absolutePathString

@Composable
fun SettingsWindow(
    inputModel: SettingsInputModel,
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel(inputModel)
    val state by viewModel.state.collectAsState()

    RiftWindow(
        title = "RIFT 设置",
        icon = Res.drawable.window_settings,
        state = windowState,
        onCloseClick = onCloseRequest,
        titleBarContent = { height ->
            ToolbarRow(
                selectedTab = state.selectedTab,
                fixedHeight = height,
                onTabSelected = viewModel::onTabSelected,
            )
        },
        withContentPadding = false,
        isResizable = false,
    ) {
        SettingsWindowContent(
            inputModel = inputModel,
            state = state,
            viewModel = viewModel,
        )

        state.dialogMessage?.let {
            RiftMessageDialog(
                dialog = it,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseDialogMessage,
            )
        }

        if (state.isJumpBridgeSearchDialogShown) {
            RiftDialog(
                title = "跳桥搜索",
                icon = Res.drawable.window_warning,
                parentState = windowState,
                state = rememberWindowState(width = 350.dp, height = Dp.Unspecified),
                onCloseClick = viewModel::onJumpBridgeDialogDismissed,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    Text(
                        text = "此功能并非 RIFT 独有，目前也无已知故障报告，但有观点认为它可能触发 " +
                            "ESI 的隐藏限流并导致你的 IP 被封禁。",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                    Text(
                        text = "请自行承担使用风险！",
                        textAlign = TextAlign.Center,
                        style = RiftTheme.typography.headerPrimary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    ) {
                        RiftButton(
                            text = "取消",
                            cornerCut = ButtonCornerCut.BottomLeft,
                            type = ButtonType.Secondary,
                            onClick = viewModel::onJumpBridgeDialogDismissed,
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

    if (state.isEditNotificationWindowOpen) {
        NotificationEditWindow(
            position = state.notificationEditPlacement,
            onCloseRequest = viewModel::onEditNotificationDone,
        )
    }
}

@Composable
private fun SettingsWindowContent(
    inputModel: SettingsInputModel,
    state: UiState,
    viewModel: SettingsViewModel,
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

        Layout(
            content = {
                // General Settings
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier
                        .padding(Spacing.medium)
                        .height(IntrinsicSize.Max),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel) {
                            UserInterfaceSection(state, viewModel)
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel, SettingsInputModel.EveInstallation) {
                            EveInstallationSection(state, viewModel)
                        }
                        SectionContainer(inputModel) {
                            CharacterPortraitsSection(state, viewModel)
                        }
                    }
                }

                // Intel & Alerts
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier
                        .padding(Spacing.medium)
                        .height(IntrinsicSize.Max),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel, SettingsInputModel.IntelChannels) {
                            IntelChannelsSection(state, viewModel)
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel) {
                            IntelTimeoutSection(state, viewModel)
                        }
                        SectionContainer(inputModel) {
                            AlertsSection(state, viewModel)
                        }
                        SectionContainer(inputModel) {
                            KillmailMonitoringSection(state, viewModel)
                        }
                    }
                }

                // Map & Autopilot
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier
                        .padding(Spacing.medium)
                        .height(IntrinsicSize.Max),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel) {
                            MapUserInterfaceSection(state, viewModel)
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel) {
                            MapAutopilotSection(state, viewModel)
                        }
                        SectionContainer(inputModel) {
                            MapIntelPopupsSection(state, viewModel)
                        }
                    }
                }

                // Sovereignty
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier
                        .padding(Spacing.medium)
                        .height(IntrinsicSize.Max),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel) {
                            JumpBridgeNetworkSection(state, viewModel)
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel) {
                            SovereigntyUpgradesSection(state, viewModel)
                        }
                    }
                }

                // Misc
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier
                        .padding(Spacing.medium)
                        .height(IntrinsicSize.Max),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel) {
                            OtherSettingsSection(state, viewModel)
                        }
                        SectionContainer(inputModel) {
                            StorageSection(state, viewModel)
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel) {
                            ClipboardSection(state, viewModel)
                        }
                    }
                }
            },
        ) { measurables, constraints ->
            val placeables = measurables.map { measurable ->
                measurable.measure(constraints)
            }
            val height = placeables.maxOf { it.height }
            layout(constraints.maxWidth, height) {
                placeables[state.selectedTab.id].place(0, 0)
            }
        }
    }
}

@Composable
private fun ToolbarRow(
    selectedTab: SettingsTab,
    fixedHeight: Dp,
    onTabSelected: (SettingsTab) -> Unit,
) {
    val tabs = remember {
        listOf(
            Tab(id = SettingsTab.General.id, title = "常规设置", isCloseable = false, payload = SettingsTab.General),
            Tab(id = SettingsTab.Intel.id, title = "预警与告警", isCloseable = false, payload = SettingsTab.Intel),
            Tab(id = SettingsTab.Map.id, title = "星图", isCloseable = false, payload = SettingsTab.Map),
            Tab(id = SettingsTab.Sovereignty.id, title = "主权", isCloseable = false, payload = SettingsTab.Sovereignty),
            Tab(id = SettingsTab.Misc.id, title = "其他", isCloseable = false, payload = SettingsTab.Misc),
        )
    }
    RiftTabBar(
        tabs = tabs,
        selectedTab = selectedTab.id,
        onTabSelected = { tab ->
            onTabSelected(tabs.first { it.id == tab }.payload as SettingsTab)
        },
        onTabClosed = {},
        withUnderline = false,
        withWideTabs = true,
        fixedHeight = fixedHeight,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SectionContainer(
    inputModel: SettingsInputModel,
    enabledInputModel: SettingsInputModel? = null,
    content: @Composable () -> Unit,
) {
    val isEnabled = inputModel == SettingsInputModel.Normal || inputModel == enabledInputModel
    Box(
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .modifyIf(!isEnabled) {
                alpha(0.3f)
            },
    ) {
        Column {
            content()
        }
        if (!isEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onClick {},
            ) {}
        }
    }
}

@Composable
private fun UserInterfaceSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("用户界面", Modifier.padding(bottom = Spacing.medium))
    RiftCheckboxWithLabel(
        label = "记住已打开窗口",
        tooltip = "开启后在重启应用时恢复上次打开的窗口",
        isChecked = state.isRememberOpenWindows,
        onCheckedChange = viewModel::onRememberOpenWindowsChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "记住窗口位置与大小",
        tooltip = "开启后在重启应用时恢复窗口位置与尺寸",
        isChecked = state.isRememberWindowPlacement,
        onCheckedChange = viewModel::onRememberWindowPlacementChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "使用 EVE 时间显示",
        tooltip = "开启后以 EVE 时间显示，而非本地时区",
        isChecked = state.isDisplayEveTime,
        onCheckedChange = viewModel::onIsDisplayEveTimeChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "使用深色托盘图标",
        tooltip = "开启后使用深色样式的系统托盘图标",
        isChecked = state.isUsingDarkTrayIcon,
        onCheckedChange = viewModel::onIsUsingDarkTrayIconChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "ISK 显示小数",
        tooltip = "开启后在 ISK 金额中显示小数位",
        isChecked = state.isShowIskCents,
        onCheckedChange = viewModel::onIsShowIskCentsChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    if (koin.get<OperatingSystem>() != MacOs) {
        RiftCheckboxWithLabel(
            label = "智能置顶",
            tooltip = "设为「始终置顶」的窗口仅在 EVE 客户端获得焦点时保持最前",
            isChecked = state.isSmartAlwaysAbove,
            onCheckedChange = viewModel::onIsSmartAlwaysAboveChanged,
            modifier = Modifier.padding(bottom = Spacing.small),
        )
    }
    RiftCheckboxWithLabel(
        label = "星系旁显示距离",
        tooltip = "开启后在星名旁显示距最近角色的跳数",
        isChecked = state.isShowingSystemDistance,
        onCheckedChange = viewModel::onIsShowingSystemDistanceChange,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "距离计算包含跳桥",
        tooltip = "开启后在星系距离计算中包含跳桥",
        isChecked = state.isUsingJumpBridgesForDistance,
        onCheckedChange = viewModel::onIsUsingJumpBridgesForDistance,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "启用窗口透明",
        tooltip = "开启后可调节窗口透明度",
        isChecked = state.isWindowTransparencyEnabled,
        onCheckedChange = viewModel::onIsWindowTransparencyChanged,
    )
    Spacer(Modifier.height(Spacing.small))
    RiftDropdownWithLabel(
        label = "窗口透明度：",
        items = listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
        selectedItem = state.windowTransparencyModifier,
        onItemSelected = viewModel::onWindowTransparencyModifierChanged,
        getItemName = {
            when (it) {
                0f -> "最高"
                0.25f -> "高"
                0.5f -> "中"
                0.75f -> "低"
                1f -> "最低"
                else -> "自定义"
            }
        },
    )
    RiftDropdownWithLabel(
        label = "界面缩放：",
        items = listOf(0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f),
        selectedItem = state.uiScale,
        onItemSelected = viewModel::onUiScaleChanged,
        getItemName = { String.format("%d%%", (it * 100).toInt()) },
    )
}

@Composable
private fun AlertsSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("告警", Modifier.padding(bottom = Spacing.medium))
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "选择通知位置：",
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.weight(1f),
        )
        RiftButton(
            text = "编辑位置",
            onClick = viewModel::onEditNotificationClick,
        )
    }
    RiftSliderWithLabel(
        label = "告警音量：",
        width = 100.dp,
        range = 0..100,
        currentValue = state.soundsVolume,
        onValueChange = viewModel::onSoundsVolumeChange,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "移动端推送：",
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.weight(1f),
        )
        RiftButton(
            text = "配置",
            onClick = viewModel::onConfigurePushoverClick,
        )
    }
}

@Composable
private fun KillmailMonitoringSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("Kill 监视", Modifier.padding(bottom = Spacing.medium))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            text = "RIFT 会监视 killmail 以获取宝贵预警信息，\n例如在星图上显示攻击者",
            style = RiftTheme.typography.bodySecondary,
        )
        RiftCheckboxWithLabel(
            label = "监视 zKillboard.com",
            tooltip = "RIFT 将订阅 zKillboard.com\n以实时接收新的 killmail",
            isChecked = state.isZkillboardMonitoringEnabled,
            onCheckedChange = viewModel::onIsZkillboardMonitoringChanged,
            modifier = Modifier.padding(bottom = Spacing.small),
        )
    }
}

@Composable
private fun OtherSettingsSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("其他设置", Modifier.padding(bottom = Spacing.medium))
    RiftDropdownWithLabel(
        label = "联盟配置包：",
        items = listOf(null) + ConfigurationPack.entries,
        selectedItem = state.configurationPack,
        onItemSelected = viewModel::onConfigurationPackChange,
        getItemName = { it?.displayName ?: "默认" },
        tooltip = """
            启用针对玩家团体的预设，
            例如推荐的预警频道。
            若需添加请联系开发者（Discord）。
        """.trimIndent(),
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "下次启动显示设置向导",
        tooltip = "Aura 也是位「向导」哦。",
        isChecked = state.isShowSetupWizardOnNextStartEnabled,
        onCheckedChange = viewModel::onShowSetupWizardOnNextStartChanged,
    )
}

@Composable
private fun StorageSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("存储", Modifier.padding(bottom = Spacing.medium))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.verySmall),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "RIFT 数据目录",
                    style = RiftTheme.typography.bodyPrimary,
                )
                Text(
                    text = state.storageStats?.dataDirectory?.toString() ?: "…",
                    style = RiftTheme.typography.detailSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                )
            }
            RiftButton(
                text = "打开数据目录",
                type = ButtonType.Primary,
                onClick = viewModel::onOpenAppData,
            )
        }
        UsedSpace("全部数据：", state.storageStats?.dataSize)

        Divider(color = RiftTheme.colors.divider, modifier = Modifier.padding(vertical = Spacing.small))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.verySmall),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "RIFT 缓存目录",
                    style = RiftTheme.typography.bodyPrimary,
                )
                Text(
                    text = state.storageStats?.cacheDirectory?.toString() ?: "…",
                    style = RiftTheme.typography.detailSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                )
            }
            RiftButton(
                text = "打开缓存目录",
                type = ButtonType.Primary,
                onClick = viewModel::onOpenAppCache,
            )
        }
        UsedSpace("ESI 缓存：", state.storageStats?.esiCacheSize)
        UsedSpace("Killmail 缓存：", state.storageStats?.zkillCacheSize)
        UsedSpace("HTTP 缓存：", state.storageStats?.httpCacheSize)
        UsedSpace("头像缓存：", state.storageStats?.portraitsSize, state.storageStats?.portraitsCount?.let { "$it 个角色，" })
        UsedSpace("其他缓存：", state.storageStats?.otherCacheSize)
    }
}

@Composable
private fun UsedSpace(text: String, bytes: Long?, secondaryText: String? = null) {
    Row {
        Text(
            text = text,
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.weight(1f),
        )
        if (secondaryText != null) {
            Text(
                text = secondaryText,
                style = RiftTheme.typography.bodyPrimary,
                modifier = Modifier.padding(end = Spacing.small),
            )
        }
        if (bytes != null) {
            Text(
                text = buildAnnotatedString {
                    append(formatBytes(bytes))
                    withColor(RiftTheme.colors.textSecondary) {
                        append(" 已用")
                    }
                },
                style = RiftTheme.typography.bodyPrimary,
            )
        } else {
            Text(
                text = "计算中…",
                style = RiftTheme.typography.bodySecondary,
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024f)
        bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024f * 1024))
        else -> String.format("%.2f GB", bytes / (1024f * 1024 * 1024))
    }
}

@Composable
private fun ClipboardSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("剪贴板", Modifier.padding(bottom = Spacing.medium))
    Text(
        text = "RIFT 可从剪贴板导入部分数据，例如跳桥与主权增强",
        style = RiftTheme.typography.bodySecondary,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("导入问题排查")
        RiftButton(
            text = "剪贴板测试",
            type = ButtonType.Primary,
            onClick = viewModel::onClipboardTesterClick,
        )
    }
}

@Composable
private fun EveInstallationSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("EVE 安装路径")
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "EVE Online 日志目录",
            style = RiftTheme.typography.bodyPrimary,
        )
        RequirementIcon(
            isFulfilled = state.isLogsDirectoryValid,
            fulfilledTooltip = "日志目录有效",
            notFulfilledTooltip = if (state.logsDirectory.isBlank()) "未设置日志目录" else "日志目录无效",
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        var text by remember(state.logsDirectory) { mutableStateOf(state.logsDirectory) }
        RiftTextField(
            text = text,
            onTextChanged = {
                text = it
                viewModel.onLogsDirectoryChanged(it)
            },
            modifier = Modifier.weight(1f),
        )
        RiftFileChooserButton(
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY,
            typesDescription = "聊天日志目录",
            currentPath = text,
            type = ButtonType.Secondary,
            cornerCut = ButtonCornerCut.None,
            onFileChosen = {
                text = it.absolutePathString()
                viewModel.onLogsDirectoryChanged(it.absolutePathString())
            },
        )
        RiftButton(
            text = "检测",
            type = if (state.isLogsDirectoryValid) ButtonType.Secondary else ButtonType.Primary,
            onClick = viewModel::onDetectLogsDirectoryClick,
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "EVE Online 角色设置目录",
            style = RiftTheme.typography.bodyPrimary,
        )
        RequirementIcon(
            isFulfilled = state.isSettingsDirectoryValid,
            fulfilledTooltip = "设置目录有效",
            notFulfilledTooltip = if (state.settingsDirectory.isBlank()) "未设置设置目录" else "设置目录无效",
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        var text by remember(state.settingsDirectory) { mutableStateOf(state.settingsDirectory) }
        RiftTextField(
            text = text,
            onTextChanged = {
                text = it
                viewModel.onSettingsDirectoryChanged(it)
            },
            modifier = Modifier.weight(1f),
        )
        RiftFileChooserButton(
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY,
            typesDescription = "游戏日志目录",
            currentPath = text,
            type = ButtonType.Secondary,
            cornerCut = ButtonCornerCut.None,
            onFileChosen = {
                text = it.absolutePathString()
                viewModel.onSettingsDirectoryChanged(it.absolutePathString())
            },
        )
        RiftButton(
            text = "检测",
            type = if (state.isSettingsDirectoryValid) ButtonType.Secondary else ButtonType.Primary,
            onClick = viewModel::onDetectSettingsDirectoryClick,
        )
    }
}

@Composable
private fun CharacterPortraitsSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("角色头像")
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(top = Spacing.small),
        ) {
            listOf(91217127, 2123140346, 2119893075, 2118421377).forEach {
                DynamicCharacterPortraitParallax(
                    characterId = it,
                    size = 48.dp,
                    enterTimestamp = null,
                    pointerInteractionStateHolder = null,
                )
            }
        }
        RiftDropdownWithLabel(
            label = "视差效果：",
            items = CharacterPortraitsParallaxStrength.entries,
            selectedItem = state.characterPortraits.parallaxStrength,
            onItemSelected = { viewModel.onCharacterPortraitsParallaxStrengthChanged(it) },
            getItemName = {
                when (it) {
                    CharacterPortraitsParallaxStrength.None -> "关闭"
                    CharacterPortraitsParallaxStrength.Reduced -> "减弱"
                    CharacterPortraitsParallaxStrength.Normal -> "标准"
                }
            },
            tooltip = """
                对你的角色以及非预警场景下的角色显示
            """.trimIndent(),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Standing.entries.forEach { standing ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    FlagIcon(standing)
                    DynamicCharacterPortraitStandings(
                        characterId = 324677773,
                        size = 32.dp,
                        standingLevel = standing,
                        isAnimated = true,
                    )
                }
            }
        }
        RiftDropdownWithLabel(
            label = "声望背景：",
            items = CharacterPortraitsStandingsTargets.entries,
            selectedItem = state.characterPortraits.standingsTargets,
            onItemSelected = { viewModel.onCharacterPortraitsStandingsTargetsChanged(it) },
            getItemName = {
                when (it) {
                    CharacterPortraitsStandingsTargets.All -> "全部"
                    CharacterPortraitsStandingsTargets.OnlyFriendly -> "仅友好"
                    CharacterPortraitsStandingsTargets.OnlyHostile -> "仅敌对"
                    CharacterPortraitsStandingsTargets.OnlyNonNeutral -> "仅非中立"
                    CharacterPortraitsStandingsTargets.None -> "关闭"
                }
            },
            tooltip = """
                在预警场景下对角色显示
            """.trimIndent(),
        )
        RiftSliderWithLabel(
            label = "声望背景强度：",
            width = 100.dp,
            range = 30..100,
            currentValue = (state.characterPortraits.standingsEffectStrength * 100).toInt().coerceIn(0..100),
            onValueChange = { viewModel.onCharacterPortraitsStandingsEffectStrengthChanged(it / 100f) },
            getValueName = { "$it%" },
        )
    }
}

@Composable
private fun IntelChannelsSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("预警频道")
    Text(
        text = "预警报告将从此处配置的频道读取：",
        style = RiftTheme.typography.bodyPrimary,
        modifier = Modifier.padding(vertical = Spacing.medium),
    )
    ScrollbarColumn(
        modifier = Modifier
            .height(300.dp)
            .border(1.dp, RiftTheme.colors.borderGrey),
        scrollbarModifier = Modifier.padding(vertical = Spacing.small),
    ) {
        for (channel in state.intelChannels) {
            key(channel) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .hoverBackground()
                        .padding(Spacing.small),
                ) {
                    val text = buildAnnotatedString {
                        append(channel.name)
                        withStyle(SpanStyle(color = RiftTheme.colors.textSecondary)) {
                            append(" – ${channel.region ?: "全部星域"}")
                        }
                    }
                    Text(
                        text = text,
                        style = RiftTheme.typography.bodyPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    RiftImageButton(
                        resource = Res.drawable.deleteicon,
                        size = 20.dp,
                        onClick = { viewModel.onIntelChannelDelete(channel) },
                    )
                }
            }
        }
        if (state.intelChannels.isEmpty()) {
            Text(
                text = "尚未配置预警频道",
                style = RiftTheme.typography.headerPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.large)
                    .padding(horizontal = Spacing.large),
            )
            if (state.suggestedIntelChannels != null) {
                Text(
                    text = state.suggestedIntelChannels.promptTitleText,
                    style = RiftTheme.typography.bodyPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.medium)
                        .padding(horizontal = Spacing.large),
                )
                RiftButton(
                    text = state.suggestedIntelChannels.promptButtonText,
                    type = ButtonType.Primary,
                    cornerCut = ButtonCornerCut.Both,
                    onClick = viewModel::onSuggestedIntelChannelsClick,
                    modifier = Modifier
                        .padding(top = Spacing.medium)
                        .align(Alignment.CenterHorizontally),
                )
            }
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.padding(top = Spacing.medium),
    ) {
        var addChannelText by remember { mutableStateOf("") }
        RiftAutocompleteTextField(
            text = addChannelText,
            suggestions = state.autocompleteIntelChannels.filter { it.lowercase().startsWith(addChannelText.lowercase()) }.take(5),
            placeholder = "频道名称",
            onTextChanged = {
                addChannelText = it
            },
            modifier = Modifier.weight(1f),
        )
        val regionPlaceholder = "选择星域"
        var selectedRegion by remember { mutableStateOf<String?>(regionPlaceholder) }
        RiftDropdown(
            items = listOf(null) + state.regions,
            selectedItem = selectedRegion,
            onItemSelected = { selectedRegion = it },
            getItemName = { it ?: "全部星域" },
            maxItems = 5,
        )

        val isNameSelected = addChannelText.isNotEmpty()
        val isRegionSelected = selectedRegion != regionPlaceholder
        RiftTooltipArea(
            text = if (!isNameSelected) {
                "请输入频道名称"
            } else if (!isRegionSelected) {
                "请选择该频道所属星域"
            } else {
                null
            },
        ) {
            RiftButton(
                text = "添加频道",
                isEnabled = isNameSelected && isRegionSelected,
                onClick = {
                    if (addChannelText.isNotEmpty() && selectedRegion != regionPlaceholder) {
                        viewModel.onIntelChannelAdded(addChannelText, selectedRegion)
                        addChannelText = ""
                        selectedRegion = regionPlaceholder
                    }
                },
            )
        }
    }
}

@Composable
private fun IntelTimeoutSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("预警过期时间", Modifier.padding(bottom = Spacing.medium))
    val expiryItems = mapOf(
        "1 分钟" to 60,
        "2 分钟" to 60 * 2,
        "5 分钟" to 60 * 5,
        "10 分钟" to 60 * 10,
        "15 分钟" to 60 * 15,
        "30 分钟" to 60 * 30,
        "1 小时" to 60 * 60,
        "不过期" to Int.MAX_VALUE,
    )
    RiftDropdownWithLabel(
        label = "预警在此时间后过期：",
        items = expiryItems.values.toList(),
        selectedItem = state.intelExpireSeconds,
        onItemSelected = viewModel::onIntelExpireSecondsChange,
        getItemName = { item -> expiryItems.entries.firstOrNull { it.value == item }?.key ?: "$item" },
        tooltip = """
                    预警在多长时间后不再显示于预警流或星图。
        """.trimIndent(),
    )
}

@Composable
private fun MapUserInterfaceSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("星图界面", Modifier.padding(bottom = Spacing.medium))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        RiftCheckboxWithLabel(
            label = "紧凑模式",
            isChecked = state.intelMap.isUsingCompactMode,
            onCheckedChange = viewModel::onIsUsingCompactModeChange,
        )
        RiftCheckboxWithLabel(
            label = "星图随角色移动",
            tooltip = "当你跃迁到当前星图可见的另一星系时，\n星图会移动并居中到新星系",
            isChecked = state.intelMap.isFollowingCharacterWithinLayouts,
            onCheckedChange = { viewModel.onIsFollowingCharacterWithinLayoutsChange(it) },
        )
        RiftCheckboxWithLabel(
            label = "切换星图以跟随角色",
            tooltip = "当你跃迁到当前星图不可见的星系时，\n星图会切换到包含该星系的区域视图",
            isChecked = state.intelMap.isFollowingCharacterAcrossLayouts,
            onCheckedChange = { viewModel.onIsFollowingCharacterAcrossLayoutsChange(it) },
        )
        RiftCheckboxWithLabel(
            label = "反转滚轮缩放方向",
            tooltip = "缩放方向与默认相反",
            isChecked = state.intelMap.isInvertZoom,
            onCheckedChange = { viewModel.onIsScrollZoomInvertedChange(it) },
        )
        RiftCheckboxWithLabel(
            label = "始终显示星系标签",
            tooltip = "缩放时星系名称不会自动隐藏",
            isChecked = state.intelMap.isAlwaysShowingSystems,
            onCheckedChange = { viewModel.onIsAlwaysShowingSystemsChange(it) },
        )
        RiftCheckboxWithLabel(
            label = "优先在星域图上显示星系",
            tooltip = "在 RIFT 中点击星系时，优先在星域图而非新伊甸总图打开",
            isChecked = state.intelMap.isPreferringRegionMaps,
            onCheckedChange = { viewModel.onIsPreferringRegionMapsChange(it) },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(end = Spacing.medium).fillMaxWidth(),
        ) {
            Text("查看与编辑星图标记")
            RiftButton(
                text = "星图标记",
                type = ButtonType.Primary,
                onClick = viewModel::onMapNotesClick,
            )
        }
        Text(
            text = buildAnnotatedString {
                withColor(RiftTheme.colors.textPrimary) {
                    append("提示：")
                }
                append(" 在星图上按空格可自动调整缩放")
            },
            style = RiftTheme.typography.bodySecondary,
        )
    }
}

@Composable
private fun MapAutopilotSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("自动导航", Modifier.padding(bottom = Spacing.medium))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            text = "设置自动导航目的地时使用：",
            style = RiftTheme.typography.bodySecondary,
        )
        RiftRadioButtonWithLabel(
            label = "RIFT 计算航线",
            tooltip = "与 RIFT 星图显示的最短路径一致。\n忽略游戏内自动导航设置。",
            isChecked = state.isUsingRiftAutopilotRoute,
            onChecked = { viewModel.onIsUsingRiftAutopilotRouteChange(true) },
        )
        RiftRadioButtonWithLabel(
            label = "EVE 计算航线",
            tooltip = "与游戏内设置的航线一致。\n可能与 RIFT 星图显示不完全相同。",
            isChecked = !state.isUsingRiftAutopilotRoute,
            onChecked = { viewModel.onIsUsingRiftAutopilotRouteChange(false) },
        )
    }
}

@Composable
private fun MapIntelPopupsSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("预警气泡", Modifier.padding(bottom = Spacing.medium))
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
            label = "自动显示气泡：",
            items = timeoutItems.values.toList(),
            selectedItem = state.intelMap.intelPopupTimeoutSeconds,
            onItemSelected = viewModel::onIntelPopupTimeoutSecondsChange,
            getItemName = { item -> timeoutItems.entries.firstOrNull { it.value == item }?.key ?: "$item" },
            tooltip = """
                    有新预警时气泡显示的时长。
                    超过此时间后仍可悬停查看。
            """.trimIndent(),
        )
    }
}

@Composable
private fun JumpBridgeNetworkSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("跳桥网络", Modifier.padding())
    Column {
        val solarSystemsRepository: SolarSystemsRepository = remember { koin.get() }
        ScrollbarLazyColumn(
            modifier = Modifier
                .height(250.dp)
                .border(1.dp, RiftTheme.colors.borderGrey),
            scrollbarModifier = Modifier.padding(vertical = Spacing.small),
            contentPadding = PaddingValues(vertical = Spacing.verySmall),
        ) {
            if (state.jumpBridgeNetwork.isNotEmpty()) {
                val connections = state.jumpBridgeNetwork.sortedBy { it.from.name }
                for (connection in connections) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .hoverBackground()
                                .padding(horizontal = Spacing.small, vertical = Spacing.verySmall),
                        ) {
                            RiftSolarSystemChip(
                                state = SolarSystemChipState(
                                    locationsText = null,
                                    jumpsText = null,
                                    name = connection.from.name,
                                    security = connection.from.security.roundSecurity(),
                                    region = solarSystemsRepository.getRegionBySystem(connection.from.name)?.name,
                                ),
                                hasBackground = false,
                            )
                            Text(
                                text = "→",
                                style = RiftTheme.typography.bodyPrimary,
                            )
                            RiftSolarSystemChip(
                                state = SolarSystemChipState(
                                    locationsText = null,
                                    jumpsText = null,
                                    name = connection.to.name,
                                    security = connection.to.security.roundSecurity(),
                                    region = solarSystemsRepository.getRegionBySystem(connection.to.name)?.name,
                                ),
                                hasBackground = false,
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "尚未导入跳桥",
                        style = RiftTheme.typography.headerPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.large)
                            .padding(horizontal = Spacing.large),
                    )
                    AnimatedContent(state.jumpBridgeCopyState) { copyState ->
                        when (copyState) {
                            JumpBridgeCopyState.NotCopied -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = Spacing.medium),
                                ) {
                                    Text("从剪贴板复制列表以导入跳桥")
                                    when (state.jumpBridgesReference) {
                                        is ConfigurationPackRepository.JumpBridgesReference.Url -> {
                                            Text("可在此页面 Ctrl+A、Ctrl+C：")
                                            LinkText(
                                                text = "${state.jumpBridgesReference.packName} 跳桥列表",
                                                onClick = { state.jumpBridgesReference.url.toURIOrNull()?.openBrowser() },
                                            )
                                        }
                                        is ConfigurationPackRepository.JumpBridgesReference.Text -> {
                                            Text(
                                                text = "已有 ${state.jumpBridgesReference.packName} 于 ${formatDate(state.jumpBridgesReference.date)} 的跳桥列表",
                                                textAlign = TextAlign.Center,
                                            )
                                            LinkText(
                                                text = "点击复制",
                                                onClick = { Clipboard.copy(state.jumpBridgesReference.text) },
                                            )
                                        }
                                        null -> {
                                            val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
                                            RiftTooltipArea(
                                                text = buildAnnotatedString {
                                                    appendLine("每行包含两个星系名即可，格式不限：")
                                                    appendLine()
                                                    withColor(RiftTheme.colors.textHighlighted) {
                                                        appendLine("Jita -> Perimeter")
                                                        appendLine("New Caldari -> Alikara")
                                                        append("Hirtamon -> Ikuchi")
                                                    }
                                                },
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                                    modifier = Modifier
                                                        .pointerInteraction(pointerInteractionStateHolder)
                                                        .padding(vertical = Spacing.small),
                                                ) {
                                                    Text(
                                                        text = "格式说明",
                                                        style = RiftTheme.typography.bodySecondary,
                                                    )
                                                    RiftMulticolorIcon(
                                                        type = MulticolorIconType.Info,
                                                        parentPointerInteractionStateHolder = pointerInteractionStateHolder,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is JumpBridgeCopyState.Copied -> {
                                val tooltip = buildString {
                                    val connections = copyState.network.take(5).joinToString("\n") {
                                        "${it.from.name} → ${it.to.name}"
                                    }
                                    append(connections)
                                    if (copyState.network.size > 5) {
                                        appendLine()
                                        append("还有更多…")
                                    }
                                }
                                RiftTooltipArea(
                                    text = tooltip,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = Spacing.medium),
                                    ) {
                                        Text("已复制的网络")
                                        RiftButton(
                                            text = "导入 ${copyState.network.size} 条连接",
                                            onClick = viewModel::onJumpBridgeImportClick,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        AnimatedContent(state.jumpBridgeNetwork) { network ->
            if (network.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = Spacing.medium).fillMaxWidth(),
                ) {
                    Text("已加载 ${network.size} 条跳桥连接")
                    Spacer(Modifier.weight(1f))
                    RiftButton(
                        text = "复制",
                        type = ButtonType.Primary,
                        cornerCut = ButtonCornerCut.None,
                        onClick = viewModel::onJumpBridgeCopyClick,
                        modifier = Modifier.padding(end = Spacing.medium),
                    )
                    RiftButton(
                        text = "清除",
                        type = ButtonType.Negative,
                        onClick = viewModel::onJumpBridgeForgetClick,
                    )
                }
            }
        }
        AnimatedContent(state.jumpBridgeSearchState, contentKey = { it::class }) { searchState ->
            when (searchState) {
                JumpBridgeSearchState.NotSearched -> {
                    if (state.jumpBridgeNetwork.isEmpty()) {
                        Column(
                            modifier = Modifier.padding(top = Spacing.medium),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("自动搜索？")
                                RiftButton(
                                    text = "搜索",
                                    onClick = viewModel::onJumpBridgeSearchClick,
                                )
                            }
                        }
                    }
                }
                is JumpBridgeSearchState.Searching -> {
                    Column(
                        modifier = Modifier.padding(top = Spacing.medium),
                    ) {
                        Text("搜索中 – ${String.format("%.1f", searchState.progress * 100)}%")
                        Text(
                            text = "已找到 ${searchState.connectionsCount} 条星门连接",
                            style = RiftTheme.typography.bodySecondary,
                        )
                    }
                }
                JumpBridgeSearchState.SearchFailed -> {
                    Column(
                        modifier = Modifier.padding(top = Spacing.medium),
                    ) {
                        Text("无法搜索")
                    }
                }
                is JumpBridgeSearchState.SearchDone -> {
                    Column(
                        modifier = Modifier.padding(top = Spacing.medium),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("已找到包含 ${searchState.network.size} 条连接的网络")
                            RiftButton(
                                text = "导入",
                                onClick = viewModel::onJumpBridgeSearchImportClick,
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(state.jumpBridgeNetwork.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                modifier = Modifier.padding(top = Spacing.medium),
            ) {
                RiftCheckboxWithLabel(
                    label = "在星图显示网络",
                    tooltip = "跳桥连线将绘制在星图上",
                    isChecked = state.intelMap.isJumpBridgeNetworkShown,
                    onCheckedChange = viewModel::onIsJumpBridgeNetworkShownChange,
                )
                RiftSliderWithLabel(
                    label = "连线不透明度：",
                    width = 100.dp,
                    range = 10..100,
                    currentValue = state.intelMap.jumpBridgeNetworkOpacity,
                    onValueChange = viewModel::onJumpBridgeNetworkOpacityChange,
                    getValueName = { "$it%" },
                    tooltip = """
                    跳桥连线的可见度。
                    """.trimIndent(),
                )
            }
        }
    }
}

@Composable
private fun SovereigntyUpgradesSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("主权增强", Modifier.padding())
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        val solarSystemsRepository: SolarSystemsRepository = remember { koin.get() }
        ScrollbarLazyColumn(
            modifier = Modifier
                .height(250.dp)
                .border(1.dp, RiftTheme.colors.borderGrey),
            scrollbarModifier = Modifier.padding(vertical = Spacing.small),
            contentPadding = PaddingValues(vertical = Spacing.verySmall),
        ) {
            if (state.sovereigntyUpgrades.isNotEmpty()) {
                for ((system, upgrades) in state.sovereigntyUpgrades) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .hoverBackground()
                                .padding(horizontal = Spacing.small, vertical = Spacing.verySmall),
                        ) {
                            RiftSolarSystemChip(
                                state = SolarSystemChipState(
                                    locationsText = null,
                                    jumpsText = null,
                                    name = system.name,
                                    security = system.security,
                                    region = solarSystemsRepository.getRegionBySystem(system.name)?.name,
                                ),
                                hasBackground = false,
                            )
                            for (type in upgrades) {
                                RiftTooltipArea(
                                    text = type.name,
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    AsyncTypeIcon(
                                        type = type,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "尚未导入主权增强",
                        style = RiftTheme.typography.headerPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.large)
                            .padding(horizontal = Spacing.large),
                    )
                    AnimatedContent(state.sovereigntyUpgradesCopyState) { copyState ->
                        when (copyState) {
                            SovereigntyUpgradesCopyState.NotCopied -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = Spacing.medium),
                                ) {
                                    Text("从剪贴板复制列表以导入增强")
                                    if (state.sovereigntyUpgradesUrl != null) {
                                        Text(
                                            text = "可在此页面列表上 Ctrl+A、Ctrl+C：",
                                            textAlign = TextAlign.Center,
                                        )
                                        LinkText(
                                            text = "联盟主权增强列表",
                                            onClick = { state.sovereigntyUpgradesUrl.toURIOrNull()?.openBrowser() },
                                        )
                                    } else {
                                        Text("每行需包含星系名与增强名称")
                                    }
                                }
                            }
                            is SovereigntyUpgradesCopyState.Copied -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = Spacing.medium),
                                ) {
                                    Text("已复制的增强")
                                    RiftButton(
                                        text = "导入 ${copyState.upgrades.size} 个星系",
                                        onClick = viewModel::onSovereigntyUpgradesImportClick,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedContent(state.sovereigntyUpgrades) { upgrades ->
            if (upgrades.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = Spacing.medium).fillMaxWidth(),
                ) {
                    Text("已加载 ${upgrades.size} 个星系的增强")
                    Spacer(Modifier.weight(1f))
                    RiftButton(
                        text = "复制",
                        type = ButtonType.Primary,
                        cornerCut = ButtonCornerCut.None,
                        onClick = viewModel::onSovereigntyUpgradesCopyClick,
                        modifier = Modifier.padding(end = Spacing.medium),
                    )
                    RiftButton(
                        text = "清除",
                        type = ButtonType.Negative,
                        onClick = viewModel::onSovereigntyUpgradesForgetClick,
                    )
                }
            }
        }

        RiftCheckboxWithLabel(
            label = "从被黑主权枢纽自动导入增强",
            tooltip = "在被黑主权枢纽结果界面点击复制时自动导入主权增强",
            isChecked = state.isSovereigntyUpgradesHackImportingEnabled,
            onCheckedChange = viewModel::onIsSovereigntyUpgradesHackImportingEnabledClick,
        )
        RiftCheckboxWithLabel(
            label = "同时导入离线增强",
            tooltip = "从被黑主权枢纽导入时，同时导入离线状态的增强",
            isChecked = state.isSovereigntyUpgradesHackImportingOfflineEnabled,
            onCheckedChange = viewModel::onIsSovereigntyUpgradesHackImportingOfflineEnabledClick,
        )
    }
}
