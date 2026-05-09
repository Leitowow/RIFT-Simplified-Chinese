package dev.nohus.rift.infodialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ImprovedWindowDraggableArea
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.text.LinkedText
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_info
import dev.nohus.rift.generated.resources.window_warning
import dev.nohus.rift.infodialog.InfoDialogViewModel.UiState
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun InfoDialogWindow(
    inputModel: InfoDialogInputModel,
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: InfoDialogViewModel = viewModel(inputModel)
    val state by viewModel.state.collectAsState()

    val icon = if (state.isWarning) Res.drawable.window_warning else Res.drawable.window_info

    RiftWindow(
        title = state.title,
        icon = icon,
        state = windowState,
        onCloseClick = onCloseRequest,
        titleBarStyle = null,
        withContentPadding = false,
        isResizable = false,
    ) {
        ImprovedWindowDraggableArea {
            InfoDialogContent(
                state = state,
                icon = icon,
                onCloseRequest = onCloseRequest,
            )
        }
    }
}

@Composable
private fun InfoDialogContent(
    state: UiState,
    icon: DrawableResource,
    onCloseRequest: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
        modifier = Modifier.padding(Spacing.large),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = state.title,
                style = RiftTheme.typography.displayHighlighted,
            )
        }
        LinkedText(
            text = state.text,
            style = RiftTheme.typography.bodyPrimary,
        )
        Spacer(Modifier.height(Spacing.medium))
        RiftButton(
            text = "OK",
            cornerCut = ButtonCornerCut.Both,
            onClick = { onCloseRequest() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
