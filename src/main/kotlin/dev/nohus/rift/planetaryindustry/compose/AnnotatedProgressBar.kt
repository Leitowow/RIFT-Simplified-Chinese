package dev.nohus.rift.planetaryindustry.compose

import androidx.compose.animation.core.Spring.StiffnessVeryLow
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import dev.nohus.rift.compose.getNow
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.utils.formatDurationNumeric
import java.time.Duration
import java.time.Instant

@Composable
fun AnnotatedProgressBar(
    title: String,
    duration: Duration,
    totalDuration: Duration,
    color: Color,
    titleStyle: TextStyle = RiftTheme.typography.bodyPrimary,
    isAdvancingTime: Boolean,
    onFinished: () -> Unit,
) {
    val initialTime = remember(duration) { Instant.now() }
    val elapsedRealTime = if (isAdvancingTime) Duration.between(initialTime, getNow()) else Duration.ZERO
    val tickingDuration = (duration + elapsedRealTime).coerceAtMost(totalDuration)
    val percentage = tickingDuration.seconds / totalDuration.seconds.toFloat()
    LaunchedEffect(tickingDuration) {
        if (tickingDuration >= totalDuration) onFinished()
    }
    AnnotatedProgressBar(
        title = title,
        percentage = percentage,
        description = "${formatDurationNumeric(tickingDuration)} / ${formatDurationNumeric(totalDuration)}",
        color = color,
        titleStyle = titleStyle,
    )
}

@Composable
fun AnnotatedProgressBar(
    title: String,
    percentage: Float,
    secondaryPercentage: Float? = null,
    description: String,
    color: Color,
    secondaryColor: Color? = null,
    titleStyle: TextStyle = RiftTheme.typography.bodyPrimary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.verySmall),
    ) {
        Text(
            text = title,
            style = titleStyle,
        )
        ProgressBar(
            percentage = percentage.coerceIn(0f, 1f),
            secondaryPercentage = secondaryPercentage?.coerceIn(0f, 1f),
            color = color,
            secondaryColor = secondaryColor,
        )
        Text(
            text = description,
            style = RiftTheme.typography.bodyPrimary,
        )
    }
}

@Composable
private fun ProgressBar(
    percentage: Float,
    secondaryPercentage: Float? = null,
    color: Color,
    secondaryColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val width = 144.dp
    val height = 6.dp
    Box(
        modifier = modifier
            .size(width, height)
            .background(RiftTheme.colors.progressBarBackground),
    ) {
        var animatedPercentage by remember { mutableStateOf(0f) }
        var animatedSecondaryPercentage by remember { mutableStateOf(0f) }
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
