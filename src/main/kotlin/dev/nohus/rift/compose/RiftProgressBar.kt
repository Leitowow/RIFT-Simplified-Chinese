package dev.nohus.rift.compose

import androidx.compose.animation.core.Spring.StiffnessVeryLow
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times
import dev.nohus.rift.compose.theme.RiftTheme

@Composable
fun RiftProgressBar(
    percentage: Float,
    secondaryPercentage: Float? = null,
    color: Color,
    secondaryColor: Color? = null,
    hasInitialAnimation: Boolean = true,
    modifier: Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .background(RiftTheme.colors.progressBarBackground),
    ) {
        val width = maxWidth
        val height = maxHeight
        var animatedPercentage by remember { mutableStateOf(if (hasInitialAnimation) 0f else percentage) }
        var animatedSecondaryPercentage by remember { mutableStateOf(if (hasInitialAnimation) 0f else secondaryPercentage ?: 0f) }
        val progressWidth by animateDpAsState(animatedPercentage * width, spring(stiffness = StiffnessVeryLow))
        val secondaryProgressWidth by animateDpAsState(animatedSecondaryPercentage * width, spring(stiffness = StiffnessVeryLow))
        LaunchedEffect(percentage, secondaryPercentage) {
            animatedPercentage = percentage
            animatedSecondaryPercentage = secondaryPercentage ?: 0f
        }
        ProgressBarFill(progressWidth, height, color)
        if (secondaryColor != null) {
            ProgressBarFill(secondaryProgressWidth, height, secondaryColor, Modifier.offset(x = progressWidth))
        }
    }
}

@Composable
private fun ProgressBarFill(
    width: Dp,
    height: Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Box(
            modifier = Modifier
                .size(width, height)
                .graphicsLayer(renderEffect = BlurEffect(6f, 6f, edgeTreatment = TileMode.Decal))
                .background(color),
        ) {}
        Box(
            modifier = Modifier
                .size(width, height)
                .background(color),
        ) {}
    }
}
