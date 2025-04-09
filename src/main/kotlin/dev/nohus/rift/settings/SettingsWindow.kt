package dev.nohus.rift.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.onClick
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RequirementIcon
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftDropdown
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftFileChooserButton
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftMessageDialog
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.SectionTitle
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.configurationpack.ConfigurationPackRepository.SuggestedIntelChannels
import dev.nohus.rift.configurationpack.displayName
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.deleteicon
import dev.nohus.rift.generated.resources.window_settings
import dev.nohus.rift.notifications.NotificationEditWindow
import dev.nohus.rift.settings.SettingsViewModel.UiState
import dev.nohus.rift.settings.persistence.ConfigurationPack
import dev.nohus.rift.settings.persistence.IntelChannel
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import javax.swing.JFileChooser
import kotlin.io.path.absolutePathString

@Composable
fun SettingsWindow(
    inputModel: SettingsInputModel,
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    RiftWindow(
        title = "RIFT 设置",
        icon = Res.drawable.window_settings,
        state = windowState,
        onCloseClick = onCloseRequest,
        isResizable = false,
    ) {
        SettingsWindowContent(
            inputModel = inputModel,
            state = state,
            viewModel = viewModel,
            onDoneClick = onCloseRequest,
        )

        if (state.isEditNotificationWindowOpen) {
            NotificationEditWindow(
                position = state.notificationEditPlacement,
                onCloseRequest = viewModel::onEditNotificationDone,
            )
        }

        state.dialogMessage?.let {
            RiftMessageDialog(
                dialog = it,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseDialogMessage,
            )
        }
    }
}

@Composable
private fun SettingsWindowContent(
    inputModel: SettingsInputModel,
    state: UiState,
    viewModel: SettingsViewModel,
    onDoneClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.height(IntrinsicSize.Max),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.weight(1f),
        ) {
            SectionContainer(inputModel, SettingsInputModel.IntelChannels) {
                IntelChannelsSection(
                    intelChannels = state.intelChannels,
                    onIntelChannelDelete = viewModel::onIntelChannelDelete,
                    regions = state.regions,
                    suggestedIntelChannels = state.suggestedIntelChannels,
                    onSuggestedIntelChannelsClick = viewModel::onSuggestedIntelChannelsClick,
                    onIntelChannelAdded = viewModel::onIntelChannelAdded,
                )
            }

            SectionContainer(inputModel) {
                IntelSection(
                    isShowingSystemDistance = state.isShowingSystemDistance,
                    onIsShowingSystemDistanceChange = viewModel::onIsShowingSystemDistanceChange,
                    isUsingJumpBridgesForDistance = state.isUsingJumpBridgesForDistance,
                    onIsUsingJumpBridgesForDistance = viewModel::onIsUsingJumpBridgesForDistance,
                    intelExpireSeconds = state.intelExpireSeconds,
                    onIntelExpireSecondsChange = viewModel::onIntelExpireSecondsChange,
                )
            }

            SectionContainer(inputModel, SettingsInputModel.EveInstallation) {
                EveInstallationSection(
                    isLogsDirectoryValid = state.isLogsDirectoryValid,
                    logsDirectory = state.logsDirectory,
                    onLogsDirectoryChanged = viewModel::onLogsDirectoryChanged,
                    onDetectLogsDirectoryClick = viewModel::onDetectLogsDirectoryClick,
                    isSettingsDirectoryValid = state.isSettingsDirectoryValid,
                    settingsDirectory = state.settingsDirectory,
                    onSettingsDirectoryChanged = viewModel::onSettingsDirectoryChanged,
                    onDetectSettingsDirectoryClick = viewModel::onDetectSettingsDirectoryClick,
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.weight(1f),
        ) {
            SectionContainer(inputModel) {
                UserInterfaceSection(
                    isRememberOpenWindowsEnabled = state.isRememberOpenWindows,
                    onRememberOpenWindowsChanged = viewModel::onRememberOpenWindowsChanged,
                    isRememberWindowPlacementEnabled = state.isRememberWindowPlacement,
                    onRememberWindowPlacementChanged = viewModel::onRememberWindowPlacementChanged,
                    isDisplayEveTime = state.isDisplayEveTime,
                    onIsDisplayEveTimeChanged = viewModel::onIsDisplayEveTimeChanged,
                    isUsingDarkTrayIcon = state.isUsingDarkTrayIcon,
                    onIsUsingDarkTrayIconChanged = viewModel::onIsUsingDarkTrayIconChanged,
                    uiScale = state.uiScale,
                    onUiScaleChanged = viewModel::onUiScaleChanged,
                )
            }

            SectionContainer(inputModel) {
                AlertsSection(
                    soundsVolume = state.soundsVolume,
                    onSoundsVolumeChange = viewModel::onSoundsVolumeChange,
                    onEditNotificationClick = viewModel::onEditNotificationClick,
                    onConfigurePushoverClick = viewModel::onConfigurePushoverClick,
                )
            }

            SectionContainer(inputModel) {
                OtherSettingsSection(
                    configurationPack = state.configurationPack,
                    onConfigurationPackChange = viewModel::onConfigurationPackChange,
                    isLoadOldMessagesEnabled = state.isLoadOldMessagesEnabled,
                    onLoadOldMessagesChanged = viewModel::onLoadOldMessagedChanged,
                    isShowSetupWizardOnNextStartEnabled = state.isShowSetupWizardOnNextStartEnabled,
                    onShowSetupWizardOnNextStartChanged = viewModel::onShowSetupWizardOnNextStartChanged,
                )
            }

            Spacer(Modifier.weight(1f))
            RiftButton(
                text = "完成",
                onClick = onDoneClick,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = Spacing.medium),
            )
        }
    }
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
    isRememberOpenWindowsEnabled: Boolean,
    onRememberOpenWindowsChanged: (Boolean) -> Unit,
    isRememberWindowPlacementEnabled: Boolean,
    onRememberWindowPlacementChanged: (Boolean) -> Unit,
    isDisplayEveTime: Boolean,
    onIsDisplayEveTimeChanged: (Boolean) -> Unit,
    isUsingDarkTrayIcon: Boolean,
    onIsUsingDarkTrayIconChanged: (Boolean) -> Unit,
    uiScale: Float,
    onUiScaleChanged: (Float) -> Unit,
) {
    SectionTitle("用户界面", Modifier.padding(bottom = Spacing.medium))
    RiftCheckboxWithLabel(
        label = "记住打开的窗口",
        tooltip = "启用后将在应用重启时\n记住打开的窗口",
        isChecked = isRememberOpenWindowsEnabled,
        onCheckedChange = onRememberOpenWindowsChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "记住窗口位置",
        tooltip = "启用后将在应用重启时\n记住窗口位置和大小",
        isChecked = isRememberWindowPlacementEnabled,
        onCheckedChange = onRememberWindowPlacementChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "使用 EVE 时间显示",
        tooltip = "启用后将以 EVE 时间显示,\n而不是您自己的时区。",
        isChecked = isDisplayEveTime,
        onCheckedChange = onIsDisplayEveTimeChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "使用深色托盘图标",
        tooltip = "如果您喜欢，可以启用使用深色托盘图标",
        isChecked = isUsingDarkTrayIcon,
        onCheckedChange = onIsUsingDarkTrayIconChanged,
    )
    RiftDropdownWithLabel(
        label = "界面缩放:",
        items = listOf(0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f),
        selectedItem = uiScale,
        onItemSelected = onUiScaleChanged,
        getItemName = { String.format("%d%%", (it * 100).toInt()) },
    )
}

@Composable
private fun AlertsSection(
    soundsVolume: Int,
    onSoundsVolumeChange: (Int) -> Unit,
    onEditNotificationClick: () -> Unit,
    onConfigurePushoverClick: () -> Unit,
) {
    SectionTitle("预警", Modifier.padding(bottom = Spacing.medium))
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "选择通知位置:",
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.weight(1f),
        )
        RiftButton(
            text = "编辑位置",
            onClick = onEditNotificationClick,
        )
    }
    RiftDropdownWithLabel(
        label = "预警音量:",
        items = (0..100 step 10).reversed().toList(),
        selectedItem = soundsVolume,
        onItemSelected = onSoundsVolumeChange,
        getItemName = { "$it%" },
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "移动推送通知:",
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.weight(1f),
        )
        RiftButton(
            text = "配置",
            onClick = onConfigurePushoverClick,
        )
    }
}

@Composable
private fun OtherSettingsSection(
    configurationPack: ConfigurationPack?,
    onConfigurationPackChange: (ConfigurationPack?) -> Unit,
    isLoadOldMessagesEnabled: Boolean,
    onLoadOldMessagesChanged: (Boolean) -> Unit,
    isShowSetupWizardOnNextStartEnabled: Boolean,
    onShowSetupWizardOnNextStartChanged: (Boolean) -> Unit,
) {
    SectionTitle("高级设置", Modifier.padding(bottom = Spacing.medium))
    RiftDropdownWithLabel(
        label = "配置包:",
        items = listOf(null) + ConfigurationPack.entries,
        selectedItem = configurationPack,
        onItemSelected = onConfigurationPackChange,
        getItemName = { it?.displayName ?: "默认" },
        tooltip = """
            启用特定玩家团体的设置，
            如预警频道建议。
            如果您想添加自己的配置包，
            请在 Discord 上联系我。
        """.trimIndent(),
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "加载历史聊天记录",
        tooltip = "启用后可以读取历史聊天记录，\n而不仅仅是新消息。\n不推荐使用。",
        isChecked = isLoadOldMessagesEnabled,
        onCheckedChange = onLoadOldMessagesChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "下次启动时显示设置向导",
        tooltip = "您知道 Aura 是个向导吗？",
        isChecked = isShowSetupWizardOnNextStartEnabled,
        onCheckedChange = onShowSetupWizardOnNextStartChanged,
    )
}

@Composable
private fun EveInstallationSection(
    isLogsDirectoryValid: Boolean,
    logsDirectory: String,
    onLogsDirectoryChanged: (String) -> Unit,
    onDetectLogsDirectoryClick: () -> Unit,
    isSettingsDirectoryValid: Boolean,
    settingsDirectory: String,
    onSettingsDirectoryChanged: (String) -> Unit,
    onDetectSettingsDirectoryClick: () -> Unit,
) {
    SectionTitle("EVE 安装")
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "EVE Online 日志目录",
            style = RiftTheme.typography.bodyPrimary,
        )
        RequirementIcon(
            isFulfilled = isLogsDirectoryValid,
            fulfilledTooltip = "日志目录有效",
            notFulfilledTooltip = if (logsDirectory.isBlank()) "无日志目录" else "无效的日志目录",
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        var text by remember(logsDirectory) { mutableStateOf(logsDirectory) }
        RiftTextField(
            text = text,
            onTextChanged = {
                text = it
                onLogsDirectoryChanged(it)
            },
            modifier = Modifier.weight(1f),
        )
        RiftFileChooserButton(
            text = "浏览",
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY,
            typesDescription = "Chat logs directory",
            currentPath = text,
            type = ButtonType.Secondary,
            cornerCut = ButtonCornerCut.None,
            onFileChosen = {
                text = it.absolutePathString()
                onLogsDirectoryChanged(it.absolutePathString())
            },
        )
        RiftButton(
            text = "自动检测",
            type = if (isLogsDirectoryValid) ButtonType.Secondary else ButtonType.Primary,
            onClick = onDetectLogsDirectoryClick,
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
            isFulfilled = isSettingsDirectoryValid,
            fulfilledTooltip = "设置目录有效",
            notFulfilledTooltip = if (settingsDirectory.isBlank()) "无设置目录" else "无效的设置目录",
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        var text by remember(settingsDirectory) { mutableStateOf(settingsDirectory) }
        RiftTextField(
            text = text,
            onTextChanged = {
                text = it
                onSettingsDirectoryChanged(it)
            },
            modifier = Modifier.weight(1f),
        )
        RiftFileChooserButton(
            text = "浏览",
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY,
            typesDescription = "Game logs directory",
            currentPath = text,
            type = ButtonType.Secondary,
            cornerCut = ButtonCornerCut.None,
            onFileChosen = {
                text = it.absolutePathString()
                onSettingsDirectoryChanged(it.absolutePathString())
            },
        )
        RiftButton(
            text = "自动检测",
            type = if (isSettingsDirectoryValid) ButtonType.Secondary else ButtonType.Primary,
            onClick = onDetectSettingsDirectoryClick,
        )
    }
}

@Composable
private fun IntelChannelsSection(
    intelChannels: List<IntelChannel>,
    onIntelChannelDelete: (IntelChannel) -> Unit,
    regions: List<String>,
    suggestedIntelChannels: SuggestedIntelChannels?,
    onSuggestedIntelChannelsClick: () -> Unit,
    onIntelChannelAdded: (name: String, region: String) -> Unit,
) {
    SectionTitle("预警频道")
    Text(
        text = "将从以下频道读取预警报告:",
        style = RiftTheme.typography.bodyPrimary,
        modifier = Modifier.padding(vertical = Spacing.medium),
    )
    ScrollbarColumn(
        modifier = Modifier
            .height(140.dp)
            .border(1.dp, RiftTheme.colors.borderGrey),
        scrollbarModifier = Modifier.padding(vertical = Spacing.small),
    ) {
        for (channel in intelChannels) {
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
                            append(" – ${channel.region}")
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
                        onClick = { onIntelChannelDelete(channel) },
                    )
                }
            }
        }
        if (intelChannels.isEmpty()) {
            Text(
                text = "未配置预警频道",
                style = RiftTheme.typography.titlePrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.large)
                    .padding(horizontal = Spacing.large),
            )
            if (suggestedIntelChannels != null) {
                Text(
                    text = suggestedIntelChannels.promptTitleText,
                    style = RiftTheme.typography.bodyPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.medium)
                        .padding(horizontal = Spacing.large),
                )
                RiftButton(
                    text = suggestedIntelChannels.promptButtonText,
                    type = ButtonType.Primary,
                    cornerCut = ButtonCornerCut.Both,
                    onClick = onSuggestedIntelChannelsClick,
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
        RiftTextField(
            text = addChannelText,
            placeholder = "频道名称",
            onTextChanged = {
                addChannelText = it
            },
            modifier = Modifier.weight(1f),
        )
        val regionPlaceholder = "选择区域"
        var selectedRegion by remember { mutableStateOf(regionPlaceholder) }
        RiftDropdown(
            items = regions,
            selectedItem = selectedRegion,
            onItemSelected = { selectedRegion = it },
            getItemName = { it },
            maxItems = 5,
        )
        RiftButton("添加频道", onClick = {
            if (addChannelText.isNotEmpty() && selectedRegion != regionPlaceholder) {
                onIntelChannelAdded(addChannelText, selectedRegion)
                addChannelText = ""
                selectedRegion = regionPlaceholder
            }
        })
    }
}

@Composable
private fun IntelSection(
    isShowingSystemDistance: Boolean,
    onIsShowingSystemDistanceChange: (Boolean) -> Unit,
    isUsingJumpBridgesForDistance: Boolean,
    onIsUsingJumpBridgesForDistance: (Boolean) -> Unit,
    intelExpireSeconds: Int,
    onIntelExpireSecondsChange: (Int) -> Unit,
) {
    SectionTitle("预警", Modifier.padding(bottom = Spacing.medium))
    RiftCheckboxWithLabel(
        label = "显示星系距离",
        tooltip = "启用后将在星系名称旁显示\n到最近角色的跳跃数。\n仅显示最多9跳的距离。",
        isChecked = isShowingSystemDistance,
        onCheckedChange = onIsShowingSystemDistanceChange,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "使用跳跃桥计算距离",
        tooltip = "启用后将在星系距离计算中包含跳跃桥",
        isChecked = isUsingJumpBridgesForDistance,
        onCheckedChange = onIsUsingJumpBridgesForDistance,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    val expiryItems = mapOf(
        "1分钟" to 60,
        "2分钟" to 60 * 2,
        "5分钟" to 60 * 5,
        "15分钟" to 60 * 15,
        "30分钟" to 60 * 30,
        "1小时" to 60 * 60,
        "永不过期" to Int.MAX_VALUE,
    )
    RiftDropdownWithLabel(
        label = "预警过期时间:",
        items = expiryItems.values.toList(),
        selectedItem = intelExpireSeconds,
        onItemSelected = onIntelExpireSecondsChange,
        getItemName = { item -> expiryItems.entries.firstOrNull { it.value == item }?.key ?: "$item" },
        tooltip = """
                    预警信息将在指定时间后
                    不再显示在信息流或地图上。
        """.trimIndent(),
    )
}
