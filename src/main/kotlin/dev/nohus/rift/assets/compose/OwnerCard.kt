package dev.nohus.rift.assets.compose

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.fadingRightEdge
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.rememberPointerInteractionStateHolder
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.utils.multiplyBrightness

@Composable
fun OwnerCard(
    icon: @Composable () -> Unit,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    text: String,
    pointerInteractionStateHolder: PointerInteractionStateHolder = rememberPointerInteractionStateHolder(),
) {
    OwnerCardBox(pointerInteractionStateHolder, isSelected, onClick) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                Text(
                    text = name,
                    style = RiftTheme.typography.bodyPrimary.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    softWrap = false,
                    modifier = Modifier
                        .padding(end = Spacing.small)
                        .fadingRightEdge()
                        .padding(end = Spacing.large),
                )
                Divider(color = Color.White.copy(alpha = 0.15f))
                Text(
                    text = text,
                    style = RiftTheme.typography.bodyPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    softWrap = false,
                    modifier = Modifier
                        .animateContentSize()
                        .fadingRightEdge()
                        .padding(end = Spacing.large)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OwnerCardBox(
    pointerInteractionStateHolder: PointerInteractionStateHolder,
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .pointerInteraction(pointerInteractionStateHolder)
            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
            .onClick { onClick() }
            .width(IntrinsicSize.Max)
            .height(IntrinsicSize.Max),
    ) {
        val shape = CutCornerShape(bottomEnd = 8.dp)
        val color = Color(0xFF6E966B)
        val alpha by animateFloatAsState(
            if (isSelected) {
                0.5f
            } else if (pointerInteractionStateHolder.isHovered) {
                0.3f
            } else {
                0.1f
            },
        )
        Box(
            modifier = Modifier
                .alpha(alpha)
                .clip(shape)
                .background(color.multiplyBrightness(2f)),
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 2.dp)
                    .clip(shape)
                    .background(color),
            ) {
                Spacer(Modifier.fillMaxSize())
            }
        }
        Box(
            modifier = Modifier
                .padding(end = 2.dp)
                .padding(Spacing.small),
        ) {
            content()
        }
    }
}
