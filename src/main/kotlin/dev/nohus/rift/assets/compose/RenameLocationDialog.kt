package dev.nohus.rift.assets.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.alerts.creategroup.CreateGroupInputModel
import dev.nohus.rift.assets.AssetsViewModel.AssetLocation
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftDialog
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_assets
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.windowing.WindowManager

@Composable
fun WindowScope.RenameLocationDialog(
    location: AssetLocation,
    parentWindowState: WindowManager.RiftWindowState,
    onDismiss: () -> Unit,
    onConfirmClick: (name: String) -> Unit,
) {
    RiftDialog(
        title = "命名位置",
        icon = Res.drawable.window_assets,
        parentState = parentWindowState,
        state = rememberWindowState(width = 400.dp, height = Dp.Unspecified),
        onCloseClick = onDismiss,
    ) {
        RenameLocationDialogContent(
            location = location,
            onCancelClick = onDismiss,
            onConfirmClick = onConfirmClick,
        )
    }
}

@Composable
private fun RenameLocationDialogContent(
    location: AssetLocation,
    onCancelClick: () -> Unit,
    onConfirmClick: (name: String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        if (location.isNameAuthoritative) {
            Text(
                text = location.name,
                style = RiftTheme.typography.bodyHighlighted,
            )
        }

        val description = buildAnnotatedString {
            if (location.isNameAuthoritative) {
                append("可在下方为该位置设置自定义名称：")
            } else {
                append("ESI 未提供此位置的名称，可在下方自行命名：")
            }
        }
        Text(
            text = description,
            style = RiftTheme.typography.bodyPrimary,
        )
        val initialText = location.customName ?: ""
        var text by remember { mutableStateOf(initialText) }
        val placeholder = if (location.isNameAuthoritative) "自定义名称" else location.name
        RiftTextField(
            text = text,
            placeholder = placeholder,
            onTextChanged = { text = it.take(64) },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            RiftButton(
                text = "取消",
                cornerCut = ButtonCornerCut.BottomLeft,
                type = ButtonType.Secondary,
                onClick = onCancelClick,
                modifier = Modifier.weight(1f),
            )
            RiftButton(
                text = "保存名称",
                onClick = { onConfirmClick(text) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
