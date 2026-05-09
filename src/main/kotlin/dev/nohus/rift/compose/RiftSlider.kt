package dev.nohus.rift.compose

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import kotlin.math.roundToInt
import kotlin.math.sign

@Composable
fun RiftSlider(
    width: Dp,
    range: IntRange,
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    getValueName: (Int) -> String? = { "$it" },
    isPreciseScroll: Boolean = false,
    isImmediate: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }
    val widthPx = LocalDensity.current.run { width.toPx() }
    val offset = remember(widthPx) { mutableStateOf(((currentValue.toFloat() - range.first) / (range.last - range.first)) * widthPx) }
    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }

    val currentTransientValue = lerp(range.first.toFloat(), range.last.toFloat(), offset.value / widthPx).roundToInt()
    LaunchedEffect(isPressed) {
        if (!isPressed) {
            onValueChange(currentTransientValue)
        }
    }
    if (isImmediate) {
        LaunchedEffect(currentTransientValue) {
            onValueChange(currentTransientValue)
        }
    }
    LaunchedEffect(currentValue) {
        if (!isPressed) {
            offset.value = ((currentValue.toFloat() - range.first) / (range.last - range.first)) * widthPx
        }
    }

    RiftTooltipArea(
        text = getValueName(currentTransientValue),
        contentAnchor = Anchor.Left,
        horizontalOffset = LocalDensity.current.run { offset.value.toDp() - 1.dp },
        verticalOffset = LocalDensity.current.run { 0.toDp() },
        forcedVisibility = pointerInteractionStateHolder.isHovered || pointerInteractionStateHolder.isPressed,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .pointerInput(currentValue) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Press -> isPressed = true
                                PointerEventType.Release -> isPressed = false
                                PointerEventType.Scroll -> {
                                    event.changes.forEach { change ->
                                        if (isPreciseScroll) {
                                            val scrollDelta = -sign(change.scrollDelta.y)
                                            val offsetPerUnit = widthPx / (range.last - range.first)
                                            offset.value = (offset.value + (scrollDelta * offsetPerUnit)).coerceIn(0f..widthPx)
                                        } else {
                                            val scrollDelta = -change.scrollDelta.y
                                            offset.value = (offset.value + (widthPx * 0.1f * scrollDelta)).coerceIn(0f..widthPx)
                                        }
                                        val value = lerp(range.first.toFloat(), range.last.toFloat(), offset.value / widthPx).roundToInt()
                                        if (value != currentValue) {
                                            onValueChange(value)
                                        }
                                    }
                                }
                            }
                            if (isPressed) {
                                val position = event.changes.lastOrNull()?.position
                                if (position != null) {
                                    offset.value = position.x.coerceIn(0f..widthPx)
                                }
                            }
                        }
                    }
                }
                .pointerInteraction(pointerInteractionStateHolder)
                .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                .width(width),
        ) {
            Track()
            Thumb(
                offset = offset,
                pointerInteractionStateHolder = pointerInteractionStateHolder,
            )
        }
    }
}

@Composable
private fun Track() {
    Spacer(
        modifier = Modifier
            .padding(top = 5.dp)
            .border(1.dp, RiftTheme.colors.borderGreyLight, RoundedCornerShape(2.dp))
            .height(6.dp)
            .fillMaxWidth(),
    )
}

@Composable
private fun Thumb(
    offset: MutableState<Float>,
    pointerInteractionStateHolder: PointerInteractionStateHolder,
) {
    val transition = updateTransition(pointerInteractionStateHolder.current)
    val color by transition.animateColor {
        when (it) {
            PointerInteractionState.Normal -> RiftTheme.colors.sliderThumb
            PointerInteractionState.Hover -> RiftTheme.colors.sliderThumbSelected
            PointerInteractionState.Press -> RiftTheme.colors.sliderThumbHighlighted
        }
    }
    val blur by transition.animateFloat {
        when (it) {
            PointerInteractionState.Normal -> 0f
            PointerInteractionState.Hover -> 1f
            PointerInteractionState.Press -> 0.5f
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset((-6).dp)
            .offset { IntOffset(offset.value.roundToInt(), 0) }
            .size(12.dp, 16.dp),
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .graphicsLayer(renderEffect = BlurEffect(8f * blur, 8f * blur, edgeTreatment = TileMode.Decal))
                    .background(RiftTheme.colors.sliderThumbSelected)
                    .size(4.dp, 8.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(4.dp, 8.dp)
                .background(color),
        )
    }
}
