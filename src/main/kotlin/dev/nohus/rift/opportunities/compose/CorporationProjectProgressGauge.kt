package dev.nohus.rift.opportunities.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.RiftCircularCharacterPortrait
import dev.nohus.rift.compose.theme.RiftTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val saveLayerPaint = Paint()

@Composable
fun CorporationProjectProgressGauge(
    progress: Float,
    color: Color,
    iconResource: DrawableResource?,
    characterId: Int?,
    characterName: String?,
) {
    Box(
        contentAlignment = Alignment.Center,
    ) {
        val diameter = 80.dp
        val gaugeWidth = 15.dp
        val gaugeWidthPx = LocalDensity.current.run { gaugeWidth.toPx() }

        var progressTarget by remember { mutableStateOf(0f) }
        val animatedProgress by animateFloatAsState(
            targetValue = progressTarget,
            animationSpec = tween(1000),
        )
        LaunchedEffect(Unit) {
            progressTarget = progress
        }

        Canvas(
            modifier = Modifier.size(diameter, diameter),
        ) {
            drawGauge(
                gaugeWidthPx = gaugeWidthPx,
                progress = animatedProgress,
                color = color,
            )
        }

        if (characterId != null) {
            RiftCircularCharacterPortrait(
                characterId = characterId,
                name = characterName ?: "",
                hasPadding = false,
                size = 48.dp,
            )
        } else if (iconResource != null) {
            Icon(
                painter = painterResource(iconResource),
                contentDescription = null,
                tint = RiftTheme.colors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun DrawScope.drawGauge(
    gaugeWidthPx: Float,
    progress: Float,
    color: Color,
) {
    val canvasRadius = size.width / 2
    val gaugeRadiusStartPercent = (canvasRadius - gaugeWidthPx / 2 - gaugeWidthPx / 2) / canvasRadius
    val gaugeWidthPercent = gaugeWidthPx / canvasRadius

    drawCircle(
        color = Color.Black,
        radius = canvasRadius - gaugeWidthPx / 2,
        style = Stroke(width = gaugeWidthPx),
    )

    drawContext.canvas.withSaveLayer(size.toRect(), paint = saveLayerPaint) {
        drawCircle(
            brush = Brush.radialGradient(
                gaugeRadiusStartPercent to Color.Transparent,
                gaugeRadiusStartPercent + (gaugeWidthPercent * 0.35f) to color.copy(alpha = 0.2f),
                gaugeRadiusStartPercent + (gaugeWidthPercent * 0.5f) to color,
                gaugeRadiusStartPercent + (gaugeWidthPercent * 0.65f) to color.copy(alpha = 0.2f),
                gaugeRadiusStartPercent + gaugeWidthPercent to Color.Transparent,
                radius = canvasRadius,
            ),
        )

        drawArc(
            color = Color.Transparent,
            startAngle = -90f + (progress * 360f),
            sweepAngle = (1f - progress) * 360f,
            useCenter = true,
            blendMode = BlendMode.Clear,
        )
    }
}
