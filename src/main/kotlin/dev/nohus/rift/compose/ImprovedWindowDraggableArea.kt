package dev.nohus.rift.compose

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.WindowScope
import com.jetbrains.JBR
import java.awt.event.MouseEvent

@Composable
fun WindowScope.ImprovedWindowDraggableArea(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    if (JBR.isWindowMoveSupported()) {
        Box(
            modifier = modifier
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        val windowMove = JBR.getWindowMove()
                        windowMove.startMovingTogetherWithMouse(this@ImprovedWindowDraggableArea.window, MouseEvent.BUTTON1)
                    }
                },
        ) {
            content()
        }
    } else {
        WindowDraggableArea(
            modifier = modifier,
            content = content,
        )
    }
}
