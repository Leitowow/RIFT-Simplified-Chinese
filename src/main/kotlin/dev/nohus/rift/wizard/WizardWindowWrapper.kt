package dev.nohus.rift.wizard

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.compose.UiScaleController
import dev.nohus.rift.di.koin
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

@Composable
fun WizardWindowWrapper(
    isVisible: Boolean,
    onCloseRequest: () -> Unit,
) {
    if (isVisible) {
        val scale = koin.get<UiScaleController>().uiScale
        WizardWindow(
            windowState = RiftWindowState(
                windowState = rememberWindowState(width = (550 * scale).dp, height = (300 * scale).dp),
                isVisible = true,
                minimumSize = 550 to 300,
            ),
            onCloseRequest = onCloseRequest,
        )
    }
}
