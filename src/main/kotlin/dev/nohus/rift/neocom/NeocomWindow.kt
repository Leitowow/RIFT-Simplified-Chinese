package dev.nohus.rift.neocom

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.PointerInteractionState
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.TitleBarStyle
import dev.nohus.rift.compose.getStandardTransitionSpec
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_assets
import dev.nohus.rift.generated.resources.window_bleedchannel
import dev.nohus.rift.generated.resources.window_characters
import dev.nohus.rift.generated.resources.window_chatchannels
import dev.nohus.rift.generated.resources.window_contacts
import dev.nohus.rift.generated.resources.window_evemailtag
import dev.nohus.rift.generated.resources.window_loudspeaker_icon
import dev.nohus.rift.generated.resources.window_map
import dev.nohus.rift.generated.resources.window_planets
import dev.nohus.rift.generated.resources.window_quitgame
import dev.nohus.rift.generated.resources.window_rift_64
import dev.nohus.rift.generated.resources.window_satellite
import dev.nohus.rift.generated.resources.window_settings
import dev.nohus.rift.generated.resources.window_sovereignty
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.ceil
import kotlin.math.floor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NeocomWindow(
    windowState: WindowManager.RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: NeocomViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    RiftWindow(
        title = "RIFT",
        icon = Res.drawable.window_rift_64,
        state = windowState,
        onCloseClick = {
            viewModel.onClose()
            onCloseRequest()
        },
        titleBarStyle = TitleBarStyle.Small,
        withContentPadding = false,
        isResizable = true,
    ) {
        val buttons = buildList {
            add(ButtonModel(icon = Res.drawable.window_loudspeaker_icon, name = "警报") { viewModel.onButtonClick(RiftWindow.Alerts) })
            add(ButtonModel(icon = Res.drawable.window_map, name = "星图") { viewModel.onButtonClick(RiftWindow.Map) })
            add(ButtonModel(icon = Res.drawable.window_satellite, name = "预警总览", shortName = "总览") { viewModel.onButtonClick(RiftWindow.IntelFeed) })
            add(ButtonModel(icon = Res.drawable.window_bleedchannel, name = "预警提示", shortName = "提示") { viewModel.onButtonClick(RiftWindow.IntelReports) })
            add(ButtonModel(icon = Res.drawable.window_characters, name = "角色", "角色") { viewModel.onButtonClick(RiftWindow.Characters) })
            add(ButtonModel(icon = Res.drawable.window_assets, name = "资产") { viewModel.onButtonClick(RiftWindow.Assets) })
            add(ButtonModel(icon = Res.drawable.window_planets, name = "行星工业", "行星") { viewModel.onButtonClick(RiftWindow.PlanetaryIndustry) })
            add(ButtonModel(icon = Res.drawable.window_contacts, name = "联系人") { viewModel.onButtonClick(RiftWindow.Contacts) })
            if (state.isJabberEnabled) {
                add(ButtonModel(icon = Res.drawable.window_sovereignty, name = "通知") { viewModel.onButtonClick(RiftWindow.Pings) })
                add(ButtonModel(icon = Res.drawable.window_chatchannels, name = "Jabber") { viewModel.onButtonClick(RiftWindow.Jabber) })
            }
            add(ButtonModel(icon = Res.drawable.window_settings, name = "设置") { viewModel.onButtonClick(RiftWindow.Settings) })
            add(ButtonModel(icon = Res.drawable.window_evemailtag, name = "关于") { viewModel.onButtonClick(RiftWindow.About) })
            add(ButtonModel(icon = Res.drawable.window_quitgame, name = "退出") { viewModel.onQuitClick() })
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
        ) {
            val maxHeightPx = constraints.maxHeight
            val heightPx = maxHeightPx / buttons.size
            val height = LocalDensity.current.run { heightPx.toDp() }.coerceAtMost(47.dp)
            if (height >= 32.dp) {
                Column {
                    for (button in buttons) {
                        NeocomRowButton(height, icon = button.icon, name = button.name, shortName = button.shortName ?: button.name, onClick = button.action)
                    }
                }
            } else {
                val buttonSizePx = LocalDensity.current.run { 47.dp.toPx() }
                val maxWidthPx = constraints.maxWidth
                val buttonsPerRow = floor(maxWidthPx / buttonSizePx)
                val rows = ceil(buttons.size / buttonsPerRow).toInt()
                val totalHeight = buttonSizePx * rows
                val buttonSize = if (totalHeight < constraints.maxHeight) {
                    47.dp
                } else {
                    24.dp
                }
                FlowRow(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    for (button in buttons) {
                        NeocomIconButton(size = buttonSize, icon = button.icon, onClick = button.action)
                    }
                }
            }
        }
    }
}

data class ButtonModel(
    val icon: DrawableResource,
    val name: String,
    val shortName: String? = null,
    val action: () -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NeocomRowButton(
    height: Dp,
    icon: DrawableResource,
    name: String,
    shortName: String,
    onClick: () -> Unit,
) {
    val pointerState = remember { PointerInteractionStateHolder() }
    val colorTransitionSpec = getStandardTransitionSpec<Color>()
    val transition = updateTransition(pointerState.current)
    val textColor by transition.animateColor(colorTransitionSpec) {
        when (it) {
            PointerInteractionState.Normal -> RiftTheme.colors.textPrimary
            PointerInteractionState.Hover -> RiftTheme.colors.textHighlighted
            PointerInteractionState.Press -> RiftTheme.colors.textHighlighted
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .hoverBackground()
            .fillMaxWidth()
            .pointerInteraction(pointerState)
            .onClick { onClick() }
            .padding(end = Spacing.medium),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(47.dp, height),
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        }
        BoxWithConstraints {
            val textMeasurer = rememberTextMeasurer()
            val style = RiftTheme.typography.titlePrimary.copy(color = textColor)
            val measured = textMeasurer.measure(name, style)
            if (measured.size.width > constraints.maxWidth) {
                Text(
                    text = shortName,
                    style = style,
                    maxLines = 1,
                    modifier = Modifier,
                )
            } else {
                Text(
                    text = name,
                    style = style,
                    maxLines = 1,
                    modifier = Modifier,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NeocomIconButton(
    size: Dp,
    icon: DrawableResource,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .hoverBackground()
            .onClick { onClick() }
            .size(size),
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )
    }
}
