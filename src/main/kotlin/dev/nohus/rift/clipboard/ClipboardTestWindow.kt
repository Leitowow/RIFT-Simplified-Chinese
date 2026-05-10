package dev.nohus.rift.clipboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nohus.rift.clipboard.ClipboardTestViewModel.ClipboardImportType
import dev.nohus.rift.clipboard.ClipboardTestViewModel.UiState
import dev.nohus.rift.compose.MulticolorIconType
import dev.nohus.rift.compose.RequirementIcon
import dev.nohus.rift.compose.RiftMulticolorIcon
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftToggleButton
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWarningBanner
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.ToggleButtonType
import dev.nohus.rift.compose.fadingRightEdge
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_clipboard
import dev.nohus.rift.settings.JumpBridgesParser
import dev.nohus.rift.settings.SovereigntyUpgradesParser
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

@Composable
fun ClipboardTestWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: ClipboardTestViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "剪贴板导入测试器",
        icon = Res.drawable.window_clipboard,
        state = windowState,
        onCloseClick = onCloseRequest,
    ) {
        ClipboardTestWindowContent(
            state = state,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun ClipboardTestWindowContent(
    state: UiState,
    viewModel: ClipboardTestViewModel,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            RiftToggleButton(
                text = "Jump Bridges",
                isSelected = state.type == ClipboardImportType.JumpBridges,
                type = ToggleButtonType.Left,
                onClick = { viewModel.onImportTypeChange(ClipboardImportType.JumpBridges) },
            )
            RiftToggleButton(
                text = "Sovereignty Upgrades",
                isSelected = state.type == ClipboardImportType.SovereigntyUpgrades,
                type = ToggleButtonType.Right,
                onClick = { viewModel.onImportTypeChange(ClipboardImportType.SovereigntyUpgrades) },
            )
        }

        ScrollbarColumn(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, RiftTheme.colors.borderGrey)
                .padding(Spacing.small),
            scrollbarModifier = Modifier.padding(vertical = Spacing.small),
            contentPadding = PaddingValues(vertical = Spacing.verySmall),
        ) {
            when (state.type) {
                ClipboardImportType.JumpBridges -> {
                    JumpBridgesContent(state)
                }
                ClipboardImportType.SovereigntyUpgrades -> {
                    SovereigntyUpgradesContent(state)
                }
                null -> {
                    EmptyState("Choose what are you trying to import above")
                }
            }
        }
    }
}

@Composable
private fun JumpBridgesContent(state: UiState) {
    when (val result = state.jumpBridgesResult) {
        JumpBridgesParser.ParsingResult.Empty, null -> {
            EmptyState("Your clipboard is empty.\nCopy a list of jump bridges.")
        }

        JumpBridgesParser.ParsingResult.TooShort -> {
            WarningState("Your clipboard only contains 1 line of text.\nCopy a list of jump bridges with at least 2 lines.")
        }

        is JumpBridgesParser.ParsingResult.ParsedNotEnough -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.verySmall),
            ) {
                WarningState("Your clipboard doesn't contain enough jump bridge connections.\nCopy a list of jump bridges with at least 2 connections.")
                result.lines.forEach { line ->
                    JumpBridgeParsedLine(line)
                }
            }
        }

        is JumpBridgesParser.ParsingResult.ParsedValid -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.verySmall),
            ) {
                SuccessState("You copied a valid list of ${result.connections.size} jump bridge connections.\nYou can import them in Settings.")
                result.lines.forEach { line ->
                    JumpBridgeParsedLine(line)
                }
            }
        }
    }
}

@Composable
private fun JumpBridgeParsedLine(line: JumpBridgesParser.ParsedLine) {
    when (line) {
        is JumpBridgesParser.ParsedLine.Connection -> {
            ParsedLine(
                icon = MulticolorIconType.Check,
                description = "This line is correct and contains a connection from ${line.connection.from.name} to ${line.connection.to.name}",
                line = line.text,
            )
        }

        is JumpBridgesParser.ParsedLine.NoSystems -> {
            ParsedLine(
                icon = MulticolorIconType.Info,
                description = "This line is ignored because it doesn't contain any system names",
                line = line.text,
            )
        }

        is JumpBridgesParser.ParsedLine.OneSystem -> {
            ParsedLine(
                icon = MulticolorIconType.Warning,
                description = "This line is invalid because it contains only one system name: ${line.system.name}",
                line = line.text,
            )
        }

        is JumpBridgesParser.ParsedLine.TooManySystems -> {
            ParsedLine(
                icon = MulticolorIconType.Warning,
                description = "This line is invalid because it contains more than 2 system names: ${line.systems.joinToString { it.name }}",
                line = line.text,
            )
        }
    }
}

@Composable
private fun SovereigntyUpgradesContent(state: UiState) {
    when (val result = state.sovereigntyUpgradesResult) {
        SovereigntyUpgradesParser.ParsingResult.Empty, null -> {
            EmptyState("Your clipboard is empty.\nCopy a list of sovereignty upgrades.")
        }
        is SovereigntyUpgradesParser.ParsingResult.ParsedNotEnough -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.verySmall),
            ) {
                WarningState("Your clipboard doesn't contain any sovereignty upgrades.")
                result.lines.forEach { line ->
                    SovereigntyUpgradesParsedLine(line)
                }
            }
        }
        is SovereigntyUpgradesParser.ParsingResult.ParsedValid -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.verySmall),
            ) {
                SuccessState("You copied a valid list of sovereignty upgrades for ${result.upgrades.size} systems.\nYou can import them in Settings.")
                result.lines.forEach { line ->
                    SovereigntyUpgradesParsedLine(line)
                }
            }
        }
    }
}

@Composable
private fun SovereigntyUpgradesParsedLine(line: SovereigntyUpgradesParser.ParsedLine) {
    when (line) {
        is SovereigntyUpgradesParser.ParsedLine.SystemWithUpgrades -> {
            ParsedLine(
                icon = MulticolorIconType.Check,
                description = "This line is correct and contains upgrades for system: ${line.system.name}, upgrades: ${line.upgrades.joinToString { it.name }}",
                line = line.text,
            )
        }
        is SovereigntyUpgradesParser.ParsedLine.NoSystem -> {
            ParsedLine(
                icon = MulticolorIconType.Info,
                description = "This line is ignored because it doesn't contain any system name",
                line = line.text,
            )
        }
        is SovereigntyUpgradesParser.ParsedLine.NoUpgrades -> {
            ParsedLine(
                icon = MulticolorIconType.Warning,
                description = "This line is invalid because it only contains a system: ${line.system.name}, but no upgrade names",
                line = line.text,
            )
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Text(
        text = message,
        style = RiftTheme.typography.headlineSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.large),
    )
}

@Composable
private fun WarningState(message: String) {
    Text(
        text = message,
        style = RiftTheme.typography.headlinePrimary.copy(color = RiftTheme.colors.warningColor),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.large),
    )
}

@Composable
private fun SuccessState(message: String) {
    Text(
        text = message,
        style = RiftTheme.typography.headlinePrimary.copy(color = RiftTheme.colors.textHighlighted),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.large),
    )
}

@Composable
private fun ParsedLine(
    icon: MulticolorIconType,
    description: String,
    line: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = Modifier
            .height(24.dp)
            .fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier
                .border(1.dp, RiftTheme.colors.borderGrey)
                .background(RiftTheme.colors.windowBackgroundSecondary)
                .padding(Spacing.small),
        ) {
            RiftTooltipArea(
                text = description,
            ) {
                RiftMulticolorIcon(type = icon)
            }
        }
        Text(
            text = line,
            style = RiftTheme.typography.bodyPrimary,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            softWrap = false,
            modifier = Modifier
                .weight(1f)
                .fadingRightEdge(),
        )
    }
}
