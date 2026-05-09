package dev.nohus.rift.compose

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.PointerInteractionState.Hover
import dev.nohus.rift.compose.PointerInteractionState.Normal
import dev.nohus.rift.compose.PointerInteractionState.Press
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.EveColors
import dev.nohus.rift.compose.theme.RiftTheme

enum class ToggleButtonState {
    Selected,
    Normal,
}

enum class ToggleButtonType {
    Left,
    Middle,
    Right,
}

private data class ToggleButtonColors(
    val normalBackground: Color,
    val hoverBackground: Color,
    val pressBackground: Color,
)

@Composable
fun RiftToggleButton(
    text: String,
    isSelected: Boolean,
    type: ToggleButtonType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RiftToggleButton(
        content = { contentColor ->
            Text(
                text = text,
                color = contentColor,
                style = RiftTheme.typography.bodyPrimary.copy(shadow = Shadow(offset = Offset(0f, 0f), blurRadius = 3f)),
                maxLines = 1,
                modifier = Modifier
                    .padding(horizontal = 15.dp),
            )
        },
        isSelected = isSelected,
        type = type,
        onClick = onClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RiftToggleButton(
    content: @Composable (contentColor: Color) -> Unit,
    isSelected: Boolean,
    type: ToggleButtonType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = remember(type) { createButtonShape(type) }
    val state = if (isSelected) ToggleButtonState.Selected else ToggleButtonState.Normal

    val colors = when (state) {
        ToggleButtonState.Selected -> ToggleButtonColors(
            normalBackground = EveColors.smokeBlue,
            hoverBackground = EveColors.smokeBlue,
            pressBackground = EveColors.smokeBlue,
        )
        ToggleButtonState.Normal -> ToggleButtonColors(
            normalBackground = Color(0xFFFFFFFF).copy(alpha = 0.1f),
            hoverBackground = Color(0xFFFFFFFF).copy(alpha = 0.3f),
            pressBackground = EveColors.tungstenGrey,
        )
    }

    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    val transition = updateTransition(pointerInteractionStateHolder.current)
    val colorTransitionSpec = getButtonTransitionSpec<Color>()
    val backgroundColor by transition.animateColor(colorTransitionSpec) {
        when (it) {
            Normal -> colors.normalBackground
            Hover -> colors.hoverBackground
            Press -> colors.pressBackground
        }
    }
    val contentColor by transition.animateColor(colorTransitionSpec) {
        when (state) {
            ToggleButtonState.Selected -> RiftTheme.colors.textPrimary
            ToggleButtonState.Normal -> when (it) {
                Normal -> RiftTheme.colors.textPrimary
                Hover -> RiftTheme.colors.textHighlighted
                Press -> Color(0xFF595959)
            }
        }
    }

    Box(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .height(IntrinsicSize.Max),
    ) {
        Surface(
            shape = shape,
            color = backgroundColor,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInteraction(pointerInteractionStateHolder)
                .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                .onClick(onClick = onClick),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.height(32.dp),
            ) {
                content(contentColor)
            }
        }
    }
}

private fun createButtonShape(type: ToggleButtonType): Shape {
    return object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density,
        ): Outline {
            val cutoutSize = with(density) { 8.dp.toPx() }
            val cutoutSmallSize = with(density) { 2.dp.toPx() }
            val path = when (type) {
                ToggleButtonType.Left -> createLeftButtonPath(size, cutoutSize, cutoutSmallSize)
                ToggleButtonType.Middle -> createMiddleButtonPath(size, cutoutSize, cutoutSmallSize)
                ToggleButtonType.Right -> createRightButtonPath(size, cutoutSize, cutoutSmallSize)
            }
            return Outline.Generic(path)
        }
    }
}

private fun createLeftButtonPath(size: Size, cutoutSize: Float, cutoutSmallSize: Float): Path {
    return Path().apply {
        moveTo(0f, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, cutoutSize)
        lineTo(size.width - cutoutSmallSize, cutoutSize + cutoutSmallSize)
        lineTo(size.width - cutoutSmallSize, size.height - cutoutSize - cutoutSmallSize)
        lineTo(size.width, size.height - cutoutSize)
        lineTo(size.width, size.height)
        lineTo(cutoutSize, size.height)
        lineTo(0f, size.height - cutoutSize)
        close()
    }
}

private fun createRightButtonPath(size: Size, cutoutSize: Float, cutoutSmallSize: Float): Path {
    return Path().apply {
        moveTo(size.width, 0f)
        lineTo(0f, 0f)
        lineTo(0f, cutoutSize)
        lineTo(0f + cutoutSmallSize, cutoutSize + cutoutSmallSize)
        lineTo(0f + cutoutSmallSize, size.height - cutoutSize - cutoutSmallSize)
        lineTo(0f, size.height - cutoutSize)
        lineTo(0f, size.height)
        lineTo(size.width - cutoutSize, size.height)
        lineTo(size.width, size.height - cutoutSize)
        close()
    }
}

private fun createMiddleButtonPath(size: Size, cutoutSize: Float, cutoutSmallSize: Float): Path {
    return Path().apply {
        moveTo(0f, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, cutoutSize)
        lineTo(size.width - cutoutSmallSize, cutoutSize + cutoutSmallSize)
        lineTo(size.width - cutoutSmallSize, size.height - cutoutSize - cutoutSmallSize)
        lineTo(size.width, size.height - cutoutSize)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        lineTo(0f, size.height - cutoutSize)
        lineTo(cutoutSmallSize, size.height - cutoutSize - cutoutSmallSize)
        lineTo(cutoutSmallSize, cutoutSize + cutoutSmallSize)
        lineTo(0f, cutoutSize)
        close()
    }
}

private fun <T> getButtonTransitionSpec(): @Composable Transition.Segment<PointerInteractionState>.() -> FiniteAnimationSpec<T> {
    return {
        when {
            Normal isTransitioningTo Hover || Hover isTransitioningTo Press -> spring(stiffness = Spring.StiffnessMedium)
            else -> spring(stiffness = Spring.StiffnessLow)
        }
    }
}
