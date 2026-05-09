package dev.nohus.rift.compose

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.EveColors
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.multicolor_checkmark
import dev.nohus.rift.generated.resources.multicolor_circlebg
import dev.nohus.rift.generated.resources.multicolor_exclamationmark
import dev.nohus.rift.generated.resources.multicolor_info
import dev.nohus.rift.windowing.LocalRiftWindowState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

sealed class MulticolorIconType(
    val backgroundResource: DrawableResource,
    val foregroundResource: DrawableResource,
    val backgroundTint: Color,
    val foregroundTint: Color?,
) {
    data object Info : MulticolorIconType(
        backgroundResource = Res.drawable.multicolor_circlebg,
        foregroundResource = Res.drawable.multicolor_info,
        backgroundTint = Color(0xFFADDEEB),
        foregroundTint = null,
    )

    data object Warning : MulticolorIconType(
        backgroundResource = Res.drawable.multicolor_circlebg,
        foregroundResource = Res.drawable.multicolor_exclamationmark,
        backgroundTint = Color(0xFFF39058),
        foregroundTint = null,
    )

    data object Check : MulticolorIconType(
        backgroundResource = Res.drawable.multicolor_checkmark,
        foregroundResource = Res.drawable.multicolor_checkmark,
        backgroundTint = EveColors.successGreen,
        foregroundTint = EveColors.successGreen,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RiftMulticolorIcon(
    type: MulticolorIconType,
    parentPointerInteractionStateHolder: PointerInteractionStateHolder? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val size = 16.dp

    val windowOpenTimestamp = LocalRiftWindowState.current?.openTimestamp
    // This is to clear hover / press states and animations when window is reopened
    key(windowOpenTimestamp) {
        val pointerInteractionStateHolder = parentPointerInteractionStateHolder ?: remember { PointerInteractionStateHolder() }
        val transition = updateTransition(pointerInteractionStateHolder.current)
        val highlightAlpha by transition.animateFloat {
            when (it) {
                PointerInteractionState.Normal -> 0.5f
                PointerInteractionState.Hover -> 1f
                PointerInteractionState.Press -> 1f
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .modifyIf(parentPointerInteractionStateHolder == null) {
                    pointerInteraction(pointerInteractionStateHolder)
                }
                .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                .modifyIfNotNull(onClick) {
                    onClick(onClick = it)
                },
        ) {
            val blur = LocalDensity.current.run { size.toPx() } * 0.35f
            repeat(3) {
                Image(
                    painter = painterResource(type.backgroundResource),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(type.backgroundTint),
                    modifier = Modifier
                        .graphicsLayer(renderEffect = BlurEffect(blur, blur, edgeTreatment = TileMode.Decal))
                        .size(size)
                        .alpha(highlightAlpha),
                )
            }
            Image(
                painter = painterResource(type.backgroundResource),
                contentDescription = null,
                colorFilter = ColorFilter.tint(type.backgroundTint),
                modifier = Modifier
                    .size(size),
            )
            Image(
                painter = painterResource(type.foregroundResource),
                contentDescription = null,
                colorFilter = ColorFilter.tint(type.foregroundTint ?: Color.White),
                modifier = Modifier
                    .size(size),
            )
        }
    }
}
