package dev.nohus.rift.compose.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.RiftContextMenuPopup
import dev.nohus.rift.compose.theme.Spacing

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LinkedText(
    text: FormattedText,
    style: TextStyle,
) {
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var hoveredLink by remember { mutableStateOf<Link?>(null) }
    var shownContextMenu by remember { mutableStateOf<Pair<Offset, List<ContextMenuItem>>?>(null) }

    val linkedAnnotatedString = text.toLinkedAnnotatedString(hoveredLink)

    Box {
        shownContextMenu?.let { (offset, items) ->
            RiftContextMenuPopup(
                items = items,
                offset = IntOffset(offset.x.toInt(), offset.y.toInt()),
                onDismissRequest = { shownContextMenu = null },
            )
        }

        Text(
            text = linkedAnnotatedString.text,
            style = style,
            onTextLayout = {
                textLayout = it
            },
            modifier = Modifier
                .pointerInput(text) {
                    awaitPointerEventScope {
                        fun getLink(event: PointerEvent): Link? {
                            val change = event.changes.firstOrNull() ?: return null
                            val offset = change.position
                            val index = textLayout?.getOffsetForPosition(offset)
                            return linkedAnnotatedString.links.firstOrNull { (indices, _) ->
                                index in indices.start..indices.end
                            }?.second
                        }

                        var pressedButton: PointerButton? = null
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press) {
                                pressedButton = event.button
                            } else if (event.type == PointerEventType.Release) {
                                val button = event.button
                                if (pressedButton == button) {
                                    pressedButton = null
                                    val link = getLink(event)
                                    if (link != null) {
                                        when (button) {
                                            PointerButton.Primary -> {
                                                link.onClick?.invoke()
                                            }
                                            PointerButton.Secondary -> {
                                                event.changes.firstOrNull()?.let { change ->
                                                    link.contextMenuItems?.let { items ->
                                                        shownContextMenu = change.position to items
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (event.type == PointerEventType.Enter || event.type == PointerEventType.Move) {
                                hoveredLink = getLink(event)
                            } else if (event.type == PointerEventType.Exit) {
                                hoveredLink = null
                            }
                        }
                    }
                }
                .padding(vertical = Spacing.small),
        )
    }
}
