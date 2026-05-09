package dev.nohus.rift.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer

fun Modifier.fadingRightEdge(): Modifier {
    return fadingEdge(rightEdgeMaskBrush)
}

private fun Modifier.fadingEdge(brush: Brush): Modifier {
    return graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            translate(left = size.width - MASK_SIZE) {
                drawRect(
                    size = Size(MASK_SIZE, size.height),
                    brush = brush,
                    blendMode = BlendMode.DstIn,
                )
            }
        }
}

private const val MASK_SIZE = 20f
private val rightEdgeMaskBrush = Brush.horizontalGradient(0f to Color.Black, 1f to Color.Transparent, startX = 0f, endX = MASK_SIZE)
