package dev.nohus.rift.compose

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import java.awt.event.MouseEvent

enum class MouseButton {
    Left, Middle, Right
}

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.onMouseClick(onClick: (clickedButton: MouseButton) -> Unit): Modifier {
    return onPointerEvent(PointerEventType.Release) { event ->
        val awtEvent = event.awtEventOrNull ?: return@onPointerEvent
        val mouseButton = when (awtEvent.button) {
            MouseEvent.BUTTON1 -> MouseButton.Left
            MouseEvent.BUTTON2 -> MouseButton.Middle
            MouseEvent.BUTTON3 -> MouseButton.Right
            else -> null
        }
        if (mouseButton != null) {
            onClick(mouseButton)
        }
    }
}

fun Modifier.onMouseClick(button: MouseButton, onClick: () -> Unit): Modifier {
    return onMouseClick { clickedButton ->
        if (clickedButton == button) {
            onClick()
        }
    }
}
