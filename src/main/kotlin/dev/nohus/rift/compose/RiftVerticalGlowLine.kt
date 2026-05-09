package dev.nohus.rift.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

enum class Side {
    Left,
    Right,
}

@Composable
fun RiftVerticalGlowLine(
    pointerInteractionStateHolder: PointerInteractionStateHolder,
    color: Color,
    side: Side,
    isSelected: Boolean = false,
) {
    val offset = when (side) {
        Side.Left -> (-4).dp
        Side.Right -> 4.dp
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxHeight()
            .width(7.dp)
            .offset(offset)
            .zIndex(1f),
    ) {
        val isActive = pointerInteractionStateHolder.isHovered || isSelected
        val alpha by animateFloatAsState(if (isActive) 0.5f else 0.1f)
        val blur by animateFloatAsState(if (isActive) 4f else 0.5f)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(5.dp)
                .graphicsLayer(renderEffect = BlurEffect(blur, blur, edgeTreatment = TileMode.Decal))
                .background(color.copy(alpha = alpha)),
        ) {}
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(color.copy(alpha = 0.8f)),
        ) {}
    }
}
