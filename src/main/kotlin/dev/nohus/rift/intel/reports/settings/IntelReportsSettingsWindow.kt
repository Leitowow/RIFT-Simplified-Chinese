package dev.nohus.rift.intel.reports.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.SectionTitle
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_settings
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

@Composable
fun IntelReportsSettingsWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: IntelReportsSettingsViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    RiftWindow(
        title = "预警提示设置",
        icon = Res.drawable.window_settings,
        state = windowState,
        onCloseClick = onCloseRequest,
        isResizable = false,
    ) {
        IntelReportsSettingsWindowContent(
            isUsingCompactMode = state.isUsingCompactMode,
            onIsUsingCompactModeChange = viewModel::onIsUsingCompactModeChange,
            isShowingReporter = state.isShowingReporter,
            onIsShowingReporterChange = viewModel::onIsShowingReporterChange,
            isShowingChannel = state.isShowingChannel,
            onIsShowingChannelChange = viewModel::onIsShowingChannelChange,
            isShowingRegion = state.isShowingRegion,
            onIsShowingRegionChange = viewModel::onIsShowingRegionChange,
        )
    }
}

@Composable
private fun IntelReportsSettingsWindowContent(
    isUsingCompactMode: Boolean,
    onIsUsingCompactModeChange: (Boolean) -> Unit,
    isShowingReporter: Boolean,
    onIsShowingReporterChange: (Boolean) -> Unit,
    isShowingChannel: Boolean,
    onIsShowingChannelChange: (Boolean) -> Unit,
    isShowingRegion: Boolean,
    onIsShowingRegionChange: (Boolean) -> Unit,
) {
    Column {
        SectionTitle("用户界面", Modifier.padding(bottom = Spacing.medium))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            RiftCheckboxWithLabel(
                label = "紧凑模式",
                isChecked = isUsingCompactMode,
                onCheckedChange = onIsUsingCompactModeChange,
            )
        }
        SectionTitle("显示信息", Modifier.padding(vertical = Spacing.medium))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            RiftCheckboxWithLabel(
                label = "显示提交预警的角色名",
                isChecked = isShowingReporter,
                onCheckedChange = onIsShowingReporterChange,
            )
            RiftCheckboxWithLabel(
                label = "显示频道名称",
                isChecked = isShowingChannel,
                onCheckedChange = onIsShowingChannelChange,
            )
            RiftCheckboxWithLabel(
                label = "显示预警频道所在星域",
                isChecked = isShowingRegion,
                onCheckedChange = onIsShowingRegionChange,
            )
        }
    }
}
