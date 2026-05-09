package dev.nohus.rift.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LinkText(
    text: String,
    normalStyle: TextStyle = RiftTheme.typography.bodyLink,
    hoveredStyle: TextStyle = normalStyle,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    onClick: (() -> Unit)? = null,
    hasHoverCursor: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    val coloredStyle = if (pointerInteractionStateHolder.isHovered) {
        hoveredStyle.copy(textDecoration = TextDecoration.Underline)
    } else {
        normalStyle
    }
    val cursor = if (hasHoverCursor) Cursors.hand else null
    Text(
        text = text,
        style = coloredStyle,
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap,
        modifier = modifier
            .pointerInteraction(pointerInteractionStateHolder)
            .modifyIfNotNull(cursor) {
                pointerHoverIcon(PointerIcon(it))
            }
            .modifyIfNotNull(onClick) {
                onClick { it() }
            },
    )
}
