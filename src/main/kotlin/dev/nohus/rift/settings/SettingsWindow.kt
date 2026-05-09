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
        title = "RIFT Settings",
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
                title = "Jump Bridge Search",
                icon = Res.drawable.window_warning,
                parentState = windowState,
                state = rememberWindowState(width = 350.dp, height = Dp.Unspecified),
                onCloseClick = viewModel::onJumpBridgeDialogDismissed,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    Text(
                        text = "This feature is not unique to RIFT, and no problems were reported with it, but some " +
                            "concerns were raised that it might trip ESI's hidden rate limits and block your IP address.",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                    Text(
                        text = "Use at your own risk!",
                        textAlign = TextAlign.Center,
                        style = RiftTheme.typography.headerPrimary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    ) {
                        RiftButton(
                            text = "Cancel",
                            cornerCut = ButtonCornerCut.BottomLeft,
                            type = ButtonType.Secondary,
                            onClick = viewModel::onJumpBridgeDialogDismissed,
                            modifier = Modifier.weight(1f),
                        )
                        RiftButton(
                            text = "Confirm",
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
            Tab(id = SettingsTab.General.id, title = "General Settings", isCloseable = false, payload = SettingsTab.General),
            Tab(id = SettingsTab.Intel.id, title = "Intel & Alerts", isCloseable = false, payload = SettingsTab.Intel),
            Tab(id = SettingsTab.Map.id, title = "Map", isCloseable = false, payload = SettingsTab.Map),
            Tab(id = SettingsTab.Sovereignty.id, title = "Sovereignty", isCloseable = false, payload = SettingsTab.Sovereignty),
            Tab(id = SettingsTab.Misc.id, title = "Misc", isCloseable = false, payload = SettingsTab.Misc),
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
    SectionTitle("User Interface", Modifier.padding(bottom = Spacing.medium))
    RiftCheckboxWithLabel(
        label = "Remember open windows",
        tooltip = "Enable to remember open windows\nacross app restarts",
        isChecked = state.isRememberOpenWindows,
        onCheckedChange = viewModel::onRememberOpenWindowsChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "Remember window placement",
        tooltip = "Enable to remember window positions and\nsizes across app restarts",
        isChecked = state.isRememberWindowPlacement,
        onCheckedChange = viewModel::onRememberWindowPlacementChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "Display times in EVE time",
        tooltip = "Enable to show times in EVE time,\ninstead of your own time zone.",
        isChecked = state.isDisplayEveTime,
        onCheckedChange = viewModel::onIsDisplayEveTimeChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "Use dark tray icon",
        tooltip = "Enable to use a dark tray icon,\nif you prefer it.",
        isChecked = state.isUsingDarkTrayIcon,
        onCheckedChange = viewModel::onIsUsingDarkTrayIconChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "Show ISK cents",
        tooltip = "Enable to show decimal places in ISK amounts",
        isChecked = state.isShowIskCents,
        onCheckedChange = viewModel::onIsShowIskCentsChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    if (koin.get<OperatingSystem>() != MacOs) {
        RiftCheckboxWithLabel(
            label = "Smart always above",
            tooltip = "Windows set to \"always above\" will\nonly be on top while an EVE client is focused.",
            isChecked = state.isSmartAlwaysAbove,
            onCheckedChange = viewModel::onIsSmartAlwaysAboveChanged,
            modifier = Modifier.padding(bottom = Spacing.small),
        )
    }
    RiftCheckboxWithLabel(
        label = "Show distance on systems",
        tooltip = "Enable to show the number of jumps to\nthe closest character next to system names.",
        isChecked = state.isShowingSystemDistance,
        onCheckedChange = viewModel::onIsShowingSystemDistanceChange,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "Use jump bridges for distance",
        tooltip = "Enable to include jump bridges in system distances",
        isChecked = state.isUsingJumpBridgesForDistance,
        onCheckedChange = viewModel::onIsUsingJumpBridgesForDistance,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "Enable transparent windows",
        tooltip = "Enable to be able to set\nwindows transparent.",
        isChecked = state.isWindowTransparencyEnabled,
        onCheckedChange = viewModel::onIsWindowTransparencyChanged,
    )
    Spacer(Modifier.height(Spacing.small))
    RiftDropdownWithLabel(
        label = "Window transparency:",
        items = listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
        selectedItem = state.windowTransparencyModifier,
        onItemSelected = viewModel::onWindowTransparencyModifierChanged,
        getItemName = {
            when (it) {
                0f -> "Maximal"
                0.25f -> "High"
                0.5f -> "Medium"
                0.75f -> "Low"
                1f -> "Minimal"
                else -> "Custom"
            }
        },
    )
    RiftDropdownWithLabel(
        label = "UI scale:",
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
    SectionTitle("Alerts", Modifier.padding(bottom = Spacing.medium))
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Choose notification position:",
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.weight(1f),
        )
        RiftButton(
            text = "Edit position",
            onClick = viewModel::onEditNotificationClick,
        )
    }
    RiftSliderWithLabel(
        label = "Alert volume:",
        width = 100.dp,
        range = 0..100,
        currentValue = state.soundsVolume,
        onValueChange = viewModel::onSoundsVolumeChange,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Mobile push notifications:",
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.weight(1f),
        )
        RiftButton(
            text = "Configure",
            onClick = viewModel::onConfigurePushoverClick,
        )
    }
}

@Composable
private fun KillmailMonitoringSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("Killmail Monitoring", Modifier.padding(bottom = Spacing.medium))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            text = "RIFT watches killmails for valuable intel,\nfor example to show attackers on the map",
            style = RiftTheme.typography.bodySecondary,
        )
        RiftCheckboxWithLabel(
            label = "Monitor zKillboard.com",
            tooltip = "RIFT will subscribe to zKillboard.com\nto receive new killmails live",
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
    SectionTitle("Other Settings", Modifier.padding(bottom = Spacing.medium))
    RiftDropdownWithLabel(
        label = "Configuration pack:",
        items = listOf(null) + ConfigurationPack.entries,
        selectedItem = state.configurationPack,
        onItemSelected = viewModel::onConfigurationPackChange,
        getItemName = { it?.displayName ?: "Default" },
        tooltip = """
            Enables settings specific to a player group,
            like intel channel suggestions.
            Contact me on Discord if you'd like to add yours.
        """.trimIndent(),
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "Show setup wizard on next start",
        tooltip = "Did you know that Aura is a wizard?",
        isChecked = state.isShowSetupWizardOnNextStartEnabled,
        onCheckedChange = viewModel::onShowSetupWizardOnNextStartChanged,
    )
}

@Composable
private fun StorageSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("Storage", Modifier.padding(bottom = Spacing.medium))
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
                    text = "RIFT data directory",
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
                text = "Open app data",
                type = ButtonType.Primary,
                onClick = viewModel::onOpenAppData,
            )
        }
        UsedSpace("All data:", state.storageStats?.dataSize)

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
                    text = "RIFT cache directory",
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
                text = "Open app cache",
                type = ButtonType.Primary,
                onClick = viewModel::onOpenAppCache,
            )
        }
        UsedSpace("ESI cache:", state.storageStats?.esiCacheSize)
        UsedSpace("Killmail cache:", state.storageStats?.zkillCacheSize)
        UsedSpace("HTTP cache:", state.storageStats?.httpCacheSize)
        UsedSpace("Portraits cache:", state.storageStats?.portraitsSize, state.storageStats?.portraitsCount?.let { "$it characters," })
        UsedSpace("Other cache:", state.storageStats?.otherCacheSize)
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
                        append(" used")
                    }
                },
                style = RiftTheme.typography.bodyPrimary,
            )
        } else {
            Text(
                text = "Calculating…",
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
    SectionTitle("Clipboard", Modifier.padding(bottom = Spacing.medium))
    Text(
        text = "RIFT can import some data from your clipboard, like Jump Bridges and Sovereignty Upgrades",
        style = RiftTheme.typography.bodySecondary,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Troubleshoot import issues")
        RiftButton(
            text = "Clipboard tester",
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
    SectionTitle("EVE Installation")
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "EVE Online logs directory",
            style = RiftTheme.typography.bodyPrimary,
        )
        RequirementIcon(
            isFulfilled = state.isLogsDirectoryValid,
            fulfilledTooltip = "Logs directory valid",
            notFulfilledTooltip = if (state.logsDirectory.isBlank()) "No logs directory" else "Invalid logs directory",
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
            typesDescription = "Chat logs directory",
            currentPath = text,
            type = ButtonType.Secondary,
            cornerCut = ButtonCornerCut.None,
            onFileChosen = {
                text = it.absolutePathString()
                viewModel.onLogsDirectoryChanged(it.absolutePathString())
            },
        )
        RiftButton(
            text = "Detect",
            type = if (state.isLogsDirectoryValid) ButtonType.Secondary else ButtonType.Primary,
            onClick = viewModel::onDetectLogsDirectoryClick,
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "EVE Online character settings directory",
            style = RiftTheme.typography.bodyPrimary,
        )
        RequirementIcon(
            isFulfilled = state.isSettingsDirectoryValid,
            fulfilledTooltip = "Settings directory valid",
            notFulfilledTooltip = if (state.settingsDirectory.isBlank()) "No settings directory" else "Invalid settings directory",
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
            typesDescription = "Game logs directory",
            currentPath = text,
            type = ButtonType.Secondary,
            cornerCut = ButtonCornerCut.None,
            onFileChosen = {
                text = it.absolutePathString()
                viewModel.onSettingsDirectoryChanged(it.absolutePathString())
            },
        )
        RiftButton(
            text = "Detect",
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
    SectionTitle("Character Portraits")
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
            label = "Parallax effect:",
            items = CharacterPortraitsParallaxStrength.entries,
            selectedItem = state.characterPortraits.parallaxStrength,
            onItemSelected = { viewModel.onCharacterPortraitsParallaxStrengthChanged(it) },
            getItemName = {
                when (it) {
                    CharacterPortraitsParallaxStrength.None -> "Turned off"
                    CharacterPortraitsParallaxStrength.Reduced -> "Reduced"
                    CharacterPortraitsParallaxStrength.Normal -> "Normal"
                }
            },
            tooltip = """
                Shown for your characters and 
                characters outside of intel contexts
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
            label = "Standings background:",
            items = CharacterPortraitsStandingsTargets.entries,
            selectedItem = state.characterPortraits.standingsTargets,
            onItemSelected = { viewModel.onCharacterPortraitsStandingsTargetsChanged(it) },
            getItemName = {
                when (it) {
                    CharacterPortraitsStandingsTargets.All -> "For all"
                    CharacterPortraitsStandingsTargets.OnlyFriendly -> "For friendly"
                    CharacterPortraitsStandingsTargets.OnlyHostile -> "For hostile"
                    CharacterPortraitsStandingsTargets.OnlyNonNeutral -> "For non-neutral"
                    CharacterPortraitsStandingsTargets.None -> "Turned off"
                }
            },
            tooltip = """
                Shown for characters
                in intel contexts
            """.trimIndent(),
        )
        RiftSliderWithLabel(
            label = "Standings background strength:",
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
    SectionTitle("Intel Channels")
    Text(
        text = "Intel reports will be read from these channels:",
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
                            append(" – ${channel.region ?: "All regions"}")
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
                text = "No intel channels configured",
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
            placeholder = "Channel name",
            onTextChanged = {
                addChannelText = it
            },
            modifier = Modifier.weight(1f),
        )
        val regionPlaceholder = "Choose region"
        var selectedRegion by remember { mutableStateOf<String?>(regionPlaceholder) }
        RiftDropdown(
            items = listOf(null) + state.regions,
            selectedItem = selectedRegion,
            onItemSelected = { selectedRegion = it },
            getItemName = { it ?: "All regions" },
            maxItems = 5,
        )

        val isNameSelected = addChannelText.isNotEmpty()
        val isRegionSelected = selectedRegion != regionPlaceholder
        RiftTooltipArea(
            text = if (!isNameSelected) {
                "Enter a channel name"
            } else if (!isRegionSelected) {
                "Choose a region for this channel"
            } else {
                null
            },
        ) {
            RiftButton(
                text = "Add channel",
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
    SectionTitle("Intel Timeout", Modifier.padding(bottom = Spacing.medium))
    val expiryItems = mapOf(
        "1 minute" to 60,
        "2 minutes" to 60 * 2,
        "5 minutes" to 60 * 5,
        "10 minutes" to 60 * 10,
        "15 minutes" to 60 * 15,
        "30 minutes" to 60 * 30,
        "1 hour" to 60 * 60,
        "Don't expire" to Int.MAX_VALUE,
    )
    RiftDropdownWithLabel(
        label = "Expire intel after:",
        items = expiryItems.values.toList(),
        selectedItem = state.intelExpireSeconds,
        onItemSelected = viewModel::onIntelExpireSecondsChange,
        getItemName = { item -> expiryItems.entries.firstOrNull { it.value == item }?.key ?: "$item" },
        tooltip = """
                    Time after a piece of intel will no longer
                    be shown on the feed or map.
        """.trimIndent(),
    )
}

@Composable
private fun MapUserInterfaceSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("Map User Interface", Modifier.padding(bottom = Spacing.medium))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        RiftCheckboxWithLabel(
            label = "Compact mode",
            isChecked = state.intelMap.isUsingCompactMode,
            onCheckedChange = viewModel::onIsUsingCompactModeChange,
        )
        RiftCheckboxWithLabel(
            label = "Move map to follow character",
            tooltip = "When you jump to another system visible\non your current map, the map will move\nto center on the new system",
            isChecked = state.intelMap.isFollowingCharacterWithinLayouts,
            onCheckedChange = { viewModel.onIsFollowingCharacterWithinLayoutsChange(it) },
        )
        RiftCheckboxWithLabel(
            label = "Switch maps to follow character",
            tooltip = "When you jump to another system not visible\non the current map, the map will switch\nto a region showing that system",
            isChecked = state.intelMap.isFollowingCharacterAcrossLayouts,
            onCheckedChange = { viewModel.onIsFollowingCharacterAcrossLayoutsChange(it) },
        )
        RiftCheckboxWithLabel(
            label = "Invert scroll wheel zoom",
            tooltip = "Zoom direction will be reversed",
            isChecked = state.intelMap.isInvertZoom,
            onCheckedChange = { viewModel.onIsScrollZoomInvertedChange(it) },
        )
        RiftCheckboxWithLabel(
            label = "Always show system labels",
            tooltip = "System labels won't hide when zooming out",
            isChecked = state.intelMap.isAlwaysShowingSystems,
            onCheckedChange = { viewModel.onIsAlwaysShowingSystemsChange(it) },
        )
        RiftCheckboxWithLabel(
            label = "Prefer showing systems on region maps",
            tooltip = "When clicking a system somewhere in RIFT, it will\nopen on a region map instead of the New Eden map",
            isChecked = state.intelMap.isPreferringRegionMaps,
            onCheckedChange = { viewModel.onIsPreferringRegionMapsChange(it) },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(end = Spacing.medium).fillMaxWidth(),
        ) {
            Text("View and edit your map markers")
            RiftButton(
                text = "Map markers",
                type = ButtonType.Primary,
                onClick = viewModel::onMapNotesClick,
            )
        }
        Text(
            text = buildAnnotatedString {
                withColor(RiftTheme.colors.textPrimary) {
                    append("Tip:")
                }
                append(" Press Space on the map to automatically resize")
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
    SectionTitle("Autopilot", Modifier.padding(bottom = Spacing.medium))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            text = "When setting autopilot destination, use:",
            style = RiftTheme.typography.bodySecondary,
        )
        RiftRadioButtonWithLabel(
            label = "RIFT calculated route",
            tooltip = "Shortest route as shown on the RIFT map.\nIgnores your EVE autopilot settings.",
            isChecked = state.isUsingRiftAutopilotRoute,
            onChecked = { viewModel.onIsUsingRiftAutopilotRouteChange(true) },
        )
        RiftRadioButtonWithLabel(
            label = "EVE calculated route",
            tooltip = "Route as set by EVE.\nMay not match the route on the RIFT map.",
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
    SectionTitle("Intel Popups", Modifier.padding(bottom = Spacing.medium))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        val timeoutItems = mapOf(
            "Don't show" to 0,
            "10 seconds" to 10,
            "30 seconds" to 30,
            "1 minute" to 60,
            "2 minutes" to 60 * 2,
            "5 minutes" to 60 * 5,
            "15 minutes" to 60 * 15,
            "No limit" to Int.MAX_VALUE,
        )
        RiftDropdownWithLabel(
            label = "Automatically show popups for:",
            items = timeoutItems.values.toList(),
            selectedItem = state.intelMap.intelPopupTimeoutSeconds,
            onItemSelected = viewModel::onIntelPopupTimeoutSecondsChange,
            getItemName = { item -> timeoutItems.entries.firstOrNull { it.value == item }?.key ?: "$item" },
            tooltip = """
                    For how long will intel popups be visible
                    when new information is available.
                    They are visible on hover even after this time.
            """.trimIndent(),
        )
    }
}

@Composable
private fun JumpBridgeNetworkSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("Jump Bridge Network", Modifier.padding())
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
                        text = "No jump bridges imported",
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
                                    Text("Import jump bridges by copying a list to clipboard")
                                    when (state.jumpBridgesReference) {
                                        is ConfigurationPackRepository.JumpBridgesReference.Url -> {
                                            Text("You can press Ctrl+A, Ctrl+C on this page:")
                                            LinkText(
                                                text = "${state.jumpBridgesReference.packName} Jump Bridge List",
                                                onClick = { state.jumpBridgesReference.url.toURIOrNull()?.openBrowser() },
                                            )
                                        }
                                        is ConfigurationPackRepository.JumpBridgesReference.Text -> {
                                            Text(
                                                text = "A list of jump bridges for ${state.jumpBridgesReference.packName} from ${formatDate(state.jumpBridgesReference.date)} is available",
                                                textAlign = TextAlign.Center,
                                            )
                                            LinkText(
                                                text = "Click to use it",
                                                onClick = { Clipboard.copy(state.jumpBridgesReference.text) },
                                            )
                                        }
                                        null -> {
                                            val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
                                            RiftTooltipArea(
                                                text = buildAnnotatedString {
                                                    appendLine("Any format will work as long as there are\ntwo system names somewhere in each line:")
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
                                                        text = "Format info",
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
                                        append("And more…")
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
                                        Text("Copied network")
                                        RiftButton(
                                            text = "Import ${copyState.network.size} connections",
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
                    Text("Network with ${network.size} connections loaded")
                    Spacer(Modifier.weight(1f))
                    RiftButton(
                        text = "Copy",
                        type = ButtonType.Primary,
                        cornerCut = ButtonCornerCut.None,
                        onClick = viewModel::onJumpBridgeCopyClick,
                        modifier = Modifier.padding(end = Spacing.medium),
                    )
                    RiftButton(
                        text = "Forget",
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
                                Text("Search automatically?")
                                RiftButton(
                                    text = "Search",
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
                        Text("Searching – ${String.format("%.1f", searchState.progress * 100)}%")
                        Text(
                            text = "Found ${searchState.connectionsCount} jump gate connections",
                            style = RiftTheme.typography.bodySecondary,
                        )
                    }
                }
                JumpBridgeSearchState.SearchFailed -> {
                    Column(
                        modifier = Modifier.padding(top = Spacing.medium),
                    ) {
                        Text("Unable to search")
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
                            Text("Found network with ${searchState.network.size} connections")
                            RiftButton(
                                text = "Import",
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
                    label = "Show network on map",
                    tooltip = "Jump bridge connection lines\nwill be shown on the map",
                    isChecked = state.intelMap.isJumpBridgeNetworkShown,
                    onCheckedChange = viewModel::onIsJumpBridgeNetworkShownChange,
                )
                RiftSliderWithLabel(
                    label = "Connection opacity:",
                    width = 100.dp,
                    range = 10..100,
                    currentValue = state.intelMap.jumpBridgeNetworkOpacity,
                    onValueChange = viewModel::onJumpBridgeNetworkOpacityChange,
                    getValueName = { "$it%" },
                    tooltip = """
                    Visibility of the jump bridge
                    connection lines.
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
    SectionTitle("Sovereignty Upgrades", Modifier.padding())
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
                        text = "No sovereignty upgrades imported",
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
                                    Text("Import upgrades by copying a list to clipboard")
                                    if (state.sovereigntyUpgradesUrl != null) {
                                        Text(
                                            text = "You can press Ctrl+A, Ctrl+C on the list\nyou can find on this page:",
                                            textAlign = TextAlign.Center,
                                        )
                                        LinkText(
                                            text = "Alliance Sovereignty Upgrades List",
                                            onClick = { state.sovereigntyUpgradesUrl.toURIOrNull()?.openBrowser() },
                                        )
                                    } else {
                                        Text("You need a system name and the upgrade names on each line")
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
                                    Text("Copied upgrades")
                                    RiftButton(
                                        text = "Import for ${copyState.upgrades.size} systems",
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
                    Text("Upgrades for ${upgrades.size} systems loaded")
                    Spacer(Modifier.weight(1f))
                    RiftButton(
                        text = "Copy",
                        type = ButtonType.Primary,
                        cornerCut = ButtonCornerCut.None,
                        onClick = viewModel::onSovereigntyUpgradesCopyClick,
                        modifier = Modifier.padding(end = Spacing.medium),
                    )
                    RiftButton(
                        text = "Forget",
                        type = ButtonType.Negative,
                        onClick = viewModel::onSovereigntyUpgradesForgetClick,
                    )
                }
            }
        }

        RiftCheckboxWithLabel(
            label = "Import upgrades from hacked Sovereignty Hubs",
            tooltip = "Automatically import sovereignty upgrades\nwhen clicking the copy button on\na hacked Sovereignty Hub result",
            isChecked = state.isSovereigntyUpgradesHackImportingEnabled,
            onCheckedChange = viewModel::onIsSovereigntyUpgradesHackImportingEnabledClick,
        )
        RiftCheckboxWithLabel(
            label = "Import offline upgrades from hacked Sovereignty Hubs",
            tooltip = "When importing sovereignty upgrades from\na hacked Sovereignty Hub,\nalso import offline upgrades",
            isChecked = state.isSovereigntyUpgradesHackImportingOfflineEnabled,
            onCheckedChange = viewModel::onIsSovereigntyUpgradesHackImportingOfflineEnabledClick,
        )
    }
}
