package dev.nohus.rift.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.window.Popup
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.LocalRiftColors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.compose.theme.getRiftColors
import dev.nohus.rift.di.koin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import java.util.UUID

private var openContextMenuId by mutableStateOf<String?>(null)

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun RiftContextMenuArea(
    items: List<ContextMenuItem>,
    acceptsLeftClick: Boolean = false,
    acceptsRightClick: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (items.isEmpty()) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    val contextMenuId = remember { UUID.randomUUID().toString() }
    var isMenuShown by remember { mutableStateOf(false) }
    LaunchedEffect(openContextMenuId) { if (openContextMenuId != contextMenuId) isMenuShown = false }

    val uiScaleController: UiScaleController = remember { koin.get() }
    val scale = uiScaleController.uiScale
    var areaOffset by remember { mutableStateOf(IntOffset.Zero) }
    var offset by remember { mutableStateOf(IntOffset.Zero) }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .onPlaced {
                if (it.isAttached) {
                    areaOffset = it.positionInRoot().let { IntOffset(it.x.toInt(), it.y.toInt()) }
                }
            }
            .modifyIf(acceptsLeftClick) {
                pointerInput(Unit) {
                    // We only consume left-click events without any action because the Release event handles them already.
                    // This is only to stop the left click from propagating to other consumers behind
                    detectTapGestures {}
                }
            }
            .onPointerEvent(PointerEventType.Release) { event ->
                val awtEvent = event.awtEventOrNull ?: return@onPointerEvent
                if ((acceptsRightClick && awtEvent.button == 3) || (acceptsLeftClick && awtEvent.button == 1)) {
                    with(density) {
                        offset = IntOffset((awtEvent.x / scale).dp.roundToPx(), (awtEvent.y / scale).dp.roundToPx()) - areaOffset
                    }
                    openContextMenuId = contextMenuId

                    scope.launch {
                        delay(5)
                        if (openContextMenuId != contextMenuId) {
                            // Another menu had taken over. This is to allow for nested menu areas to take priority.
                            return@launch
                        }
                        isMenuShown = true
                    }
                }
            },
    ) {
        content()
        if (items.isNotEmpty()) {
            RiftContextMenuPopup(
                isExpanded = isMenuShown,
                offset = offset,
                onDismissRequest = {
                    isMenuShown = false
                    openContextMenuId = null
                },
                items = items,
            )
        }
    }
}

/**
 * Manually shown version (for the map and text links)
 */
@Composable
fun RiftContextMenuPopup(
    items: List<ContextMenuItem>,
    offset: IntOffset,
    onDismissRequest: () -> Unit,
) {
    val contextMenuId = remember { UUID.randomUUID().toString() }
    DisposableEffect(Unit) {
        openContextMenuId = contextMenuId
        onDispose {
            if (openContextMenuId == contextMenuId) {
                openContextMenuId = null
            }
        }
    }
    RiftContextMenuPopup(
        isExpanded = true,
        offset = offset,
        onDismissRequest = {
            if (openContextMenuId == contextMenuId) {
                openContextMenuId = null
            }
            onDismissRequest()
        },
        items = items,
    )
}

@Composable
private fun RiftContextMenuPopup(
    isExpanded: Boolean,
    offset: IntOffset,
    onDismissRequest: () -> Unit,
    items: List<ContextMenuItem>,
) {
    if (isExpanded) {
        Popup(
            offset = offset,
            onDismissRequest = onDismissRequest,
        ) {
            CompositionLocalProvider(LocalRiftColors provides getRiftColors(isTransparent = false)) {
                ScrollbarColumn(
                    isScrollbarConditional = true,
                    contentPadding = PaddingValues(vertical = 7.dp),
                    modifier = Modifier
                        .pointerHoverIcon(PointerIcon(Cursors.pointer))
                        .width(IntrinsicSize.Max)
                        .background(RiftTheme.colors.windowBackgroundActive)
                        .border(1.dp, RiftTheme.colors.divider)
                        .padding(1.dp),
                ) {
                    val hasIconSpace = items.any { it is ContextMenuItem.TextItem && it.iconResource != null || it is ContextMenuItem.CheckboxItem || it is ContextMenuItem.RadioItem }
                    for (item in items) {
                        when (item) {
                            is ContextMenuItem.TextItem -> ContextMenuRow(item.text, item.iconResource, item.iconContent, hasIconSpace, isSelected = null, isHighlighted = item.isHighlighted) {
                                onDismissRequest()
                                item.onClick()
                            }
                            is ContextMenuItem.CheckboxItem -> ContextMenuRow(item.text, null, null, hasIconSpace, item.isSelected, isHighlighted = false, isRadio = false, item.onClick)
                            is ContextMenuItem.CheckboxItemWithInternalState -> {
                                var isSelected by remember { mutableStateOf(item.isSelected()) }
                                ContextMenuRow(
                                    text = item.text,
                                    iconResource = null,
                                    iconContent = null,
                                    hasIconSpace = hasIconSpace,
                                    isSelected = isSelected,
                                    isHighlighted = false,
                                    isRadio = false,
                                    onClick = {
                                        item.onClick(isSelected)
                                        isSelected = item.isSelected()
                                    },
                                )
                            }
                            is ContextMenuItem.RadioItem -> ContextMenuRow(item.text, null, null, hasIconSpace, item.isSelected, isHighlighted = false, isRadio = true, item.onClick)
                            is ContextMenuItem.HeaderItem -> ContextMenuHeader(item.text)
                            ContextMenuItem.DividerItem -> ContextMenuDivider()
                        }
                    }
                }
            }
        }
    }
}

sealed interface ContextMenuItem {
    data class TextItem(
        val text: String,
        val iconResource: DrawableResource? = null,
        val iconContent: (@Composable (PointerInteractionStateHolder) -> Unit)? = null,
        val isHighlighted: Boolean = false,
        val onClick: () -> Unit,
    ) : ContextMenuItem

    data class CheckboxItem(
        val text: String,
        val isSelected: Boolean,
        val onClick: () -> Unit,
    ) : ContextMenuItem

    data class CheckboxItemWithInternalState(
        val text: String,
        val isSelected: () -> Boolean,
        val onClick: (isSelected: Boolean) -> Unit,
    ) : ContextMenuItem

    data class RadioItem(
        val text: String,
        val isSelected: Boolean,
        val onClick: () -> Unit,
    ) : ContextMenuItem

    data class HeaderItem(
        val text: String,
    ) : ContextMenuItem

    data object DividerItem : ContextMenuItem
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContextMenuRow(
    text: String,
    iconResource: DrawableResource?,
    iconContent: (@Composable (PointerInteractionStateHolder) -> Unit)?,
    hasIconSpace: Boolean,
    isSelected: Boolean?,
    isHighlighted: Boolean = false,
    isRadio: Boolean = false,
    onClick: () -> Unit,
) {
    val pointerState = remember { PointerInteractionStateHolder() }
    val background by pointerState.animateBackgroundHover()
    val iconAreaSize = if (hasIconSpace) 34.dp else 0.dp
    val itemPadding = 7.dp
    Box(
        modifier = Modifier.height(21.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = itemPadding)
                .fillMaxHeight()
                .background(background)
                .pointerInteraction(pointerState)
                .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                .onClick { onClick() },
        ) {
            val style = if (isHighlighted) RiftTheme.typography.bodyPrimary.copy(color = RiftTheme.colors.primary) else RiftTheme.typography.bodyPrimary
            Text(
                text = text,
                style = style,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp)
                    .padding(start = max(iconAreaSize - itemPadding, 0.dp), end = Spacing.small),
            )
        }
        if (iconContent != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.requiredSize(iconAreaSize),
            ) {
                iconContent(pointerState)
            }
        } else if (iconResource != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.requiredSize(iconAreaSize),
            ) {
                val painter = painterResource(iconResource)
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier.size(painter.intrinsicSize.height.dp),
                )
            }
        }
        if (isSelected != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.requiredSize(iconAreaSize),
            ) {
                if (isRadio) {
                    RiftRadioButton(
                        isChecked = isSelected,
                        onChecked = { onClick() },
                        pointerInteractionStateHolder = pointerState,
                    )
                } else {
                    RiftCheckbox(
                        isChecked = isSelected,
                        onCheckedChange = { onClick() },
                        pointerInteractionStateHolder = pointerState,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextMenuHeader(text: String) {
    Text(
        text = text,
        style = RiftTheme.typography.headlineHighlighted,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.small)
            .padding(Spacing.verySmall)
            .padding(horizontal = Spacing.medium),
    )
}

@Composable
private fun ContextMenuDivider() {
    Divider(
        color = RiftTheme.colors.divider,
        modifier = Modifier.padding(horizontal = Spacing.medium, vertical = 7.dp),
    )
}
