package dev.nohus.rift.compose

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.zIndex
import dev.nohus.rift.Event
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.LocalRiftColors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.compose.theme.getRiftColors
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_background_dots
import dev.nohus.rift.generated.resources.window_background_dots_light
import dev.nohus.rift.generated.resources.window_light_background_off_16px
import dev.nohus.rift.generated.resources.window_light_background_on_16px
import dev.nohus.rift.generated.resources.window_locked_16px
import dev.nohus.rift.generated.resources.window_overlay_fullscreen_off_16px
import dev.nohus.rift.generated.resources.window_overlay_fullscreen_on_16px
import dev.nohus.rift.generated.resources.window_titlebar_close
import dev.nohus.rift.generated.resources.window_titlebar_float
import dev.nohus.rift.generated.resources.window_titlebar_fullscreen
import dev.nohus.rift.generated.resources.window_titlebar_kebab
import dev.nohus.rift.generated.resources.window_titlebar_minimize
import dev.nohus.rift.generated.resources.window_titlebar_tune
import dev.nohus.rift.generated.resources.window_unlocked_16px
import dev.nohus.rift.get
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.windowing.LocalRiftWindow
import dev.nohus.rift.windowing.LocalRiftWindowState
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import dev.nohus.rift.windowing.WindowStatesController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension

@Composable
fun RiftWindow(
    title: String,
    icon: DrawableResource,
    state: RiftWindowState,
    onTuneClick: (() -> Unit)? = null,
    tuneContextMenuItems: List<ContextMenuItem>? = null,
    onCloseClick: () -> Unit,
    titleBarStyle: TitleBarStyle? = TitleBarStyle.Full,
    titleBarContent: @Composable ((height: Dp) -> Unit)? = null,
    withContentPadding: Boolean = true,
    isResizable: Boolean = true,
    isMaximizeButtonShown: Boolean = false,
    content: @Composable WindowScope.() -> Unit,
) {
    val uiScaleController: UiScaleController = remember { koin.get() }
    val transparentWindowController: TransparentWindowController = remember { koin.get() }
    val windowStatesController: WindowStatesController = remember { koin.get() }
    val smartAlwaysAboveRepository: SmartAlwaysAboveRepository = remember { koin.get() }
    val scope = rememberCoroutineScope()
    val isAlwaysOnTopActive by smartAlwaysAboveRepository.isActive.collectAsState(false)
    val isAlwaysOnTop by windowStatesController.isAlwaysOnTop(state.window, state.uuid).collectAsState(false)
    val isLocked by windowStatesController.isLocked(state.window, state.uuid).collectAsState(false)
    val isTransparent by windowStatesController.isTransparent(state.window, state.uuid).collectAsState(false)
    val isComposeWindowTransparent = transparentWindowController.isComposeWindowTransparent()
    val isMaximized by windowStatesController.isMaximized(state.uuid).collectAsState(false)
    val (effectiveAlwaysOnTop, isFocusable, bringToBackEvent) = alwaysOnTopHandler(isAlwaysOnTop, isAlwaysOnTopActive)
    Window(
        onCloseRequest = onCloseClick,
        state = state.windowState,
        title = title,
        icon = painterResource(icon),
        undecorated = true,
        focusable = isFocusable,
        resizable = isResizable && !isLocked,
        alwaysOnTop = effectiveAlwaysOnTop,
        transparent = isComposeWindowTransparent,
    ) {
        uiScaleController.withScale {
            transparentWindowController.setTransparency(window, isTransparent)
            smartAlwaysAboveRepository.registerWindow(state)
            MinimumSizeHandler(state)
            BringToFrontHandler(state.bringToFrontEvent)
            BringToBackHandler(bringToBackEvent)
            CompositionLocalProvider(
                LocalRiftWindow provides window,
                LocalRiftWindowState provides state,
                LocalRiftColors provides getRiftColors(isTransparent && isComposeWindowTransparent),
            ) {
                RiftWindowContent(
                    title = title,
                    icon = icon,
                    isAlwaysOnTop = isAlwaysOnTop,
                    isLocked = isLocked,
                    isTransparent = isTransparent,
                    isMaximized = isMaximized,
                    isResizable = isResizable,
                    isMaximizeButtonShown = isMaximizeButtonShown,
                    onTuneClick = onTuneClick,
                    tuneContextMenuItems = tuneContextMenuItems,
                    onAlwaysOnTopClick = if (state.window != null) {
                        { windowStatesController.toggleAlwaysOnTop(state.window, state.uuid) }
                    } else {
                        null
                    },
                    onLockClick = if (state.window != null) {
                        { windowStatesController.toggleLocked(state.window, state.uuid) }
                    } else {
                        null
                    },
                    onTransparentClick = if (state.window != null && transparentWindowController.isEnabled) {
                        { windowStatesController.toggleTransparent(state.window, state.uuid) }
                    } else {
                        null
                    },
                    onMaximizeClick = if (state.window != null && isResizable) {
                        { scope.launch { window.placement = windowStatesController.toggleMaximized(state.window, state.uuid) } }
                    } else {
                        null
                    },
                    onMinimizeClick = { state.windowState.isMinimized = true },
                    onCloseClick = onCloseClick,
                    width = state.windowState.size.width / uiScaleController.uiScale,
                    height = state.windowState.size.height / uiScaleController.uiScale,
                    titleBarStyle = titleBarStyle,
                    titleBarContent = titleBarContent,
                    withContentPadding = withContentPadding,
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun alwaysOnTopHandler(
    isAlwaysOnTop: Boolean,
    isAlwaysOnTopActive: Boolean,
): Triple<Boolean, Boolean, Event?> {
    val alwaysOnTop = isAlwaysOnTop && isAlwaysOnTopActive
    var effectiveAlwaysOnTop by remember { mutableStateOf(alwaysOnTop) }
    var isFocusable by remember { mutableStateOf(true) }
    var bringToBackEvent: Event? by remember { mutableStateOf(null) }
    val operatingSystem: OperatingSystem = remember { koin.get() }
    if (operatingSystem == OperatingSystem.Windows) {
        LaunchedEffect(alwaysOnTop) {
            isFocusable = false
            effectiveAlwaysOnTop = alwaysOnTop
            if (isAlwaysOnTop && !isAlwaysOnTopActive) {
                bringToBackEvent = Event()
            }
            delay(100)
            isFocusable = true
        }
    } else {
        effectiveAlwaysOnTop = alwaysOnTop
    }
    return Triple(effectiveAlwaysOnTop, isFocusable, bringToBackEvent)
}

@Composable
private fun FrameWindowScope.MinimumSizeHandler(state: RiftWindowState) {
    var isSet by remember { mutableStateOf(false) }
    if (isSet) return
    LaunchedEffect(state.minimumSize, state.windowState.size) {
        val minimumWidth = state.minimumSize.first?.dp
            ?: state.windowState.size.width.takeIf { it != Dp.Unspecified } ?: return@LaunchedEffect
        val minimumHeight = state.minimumSize.second?.dp
            ?: state.windowState.size.height.takeIf { it != Dp.Unspecified } ?: return@LaunchedEffect
        val minimumSize = Dimension(minimumWidth.value.toInt(), minimumHeight.value.toInt())
        window.minimumSize = minimumSize
        isSet = true
    }
}

@Composable
private fun FrameWindowScope.BringToFrontHandler(event: Event?) {
    if (event.get()) {
        if (window.isMinimized) {
            window.isMinimized = false
        } else {
            window.isVisible = false
            window.isVisible = true
        }
    }
}

@Composable
private fun FrameWindowScope.BringToBackHandler(event: Event?) {
    if (event.get()) {
        window.toBack()
    }
}

@Composable
fun WindowScope.RiftDialog(
    title: String,
    icon: DrawableResource,
    parentState: RiftWindowState,
    state: WindowState,
    onCloseClick: () -> Unit,
    content: @Composable WindowScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onCloseClick,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        CompositionLocalProvider(
            LocalRiftWindowState provides parentState,
            LocalRiftColors provides getRiftColors(isTransparent = false),
        ) {
            RiftWindowContent(
                title = title,
                icon = icon,
                isAlwaysOnTop = false,
                isLocked = false,
                isTransparent = false,
                isMaximized = false,
                isMaximizeButtonShown = false,
                isResizable = false,
                onTuneClick = null,
                tuneContextMenuItems = null,
                onAlwaysOnTopClick = null,
                onLockClick = null,
                onTransparentClick = null,
                onMaximizeClick = null,
                onMinimizeClick = { parentState.windowState.isMinimized = true },
                onCloseClick = onCloseClick,
                width = state.size.width,
                height = state.size.height,
                titleBarStyle = TitleBarStyle.Full,
                withContentPadding = true,
                content = content,
            )
        }
    }
}

enum class TitleBarStyle {
    Full,
    Small,
    Minimal,
}

@Composable
private fun WindowScope.RiftWindowContent(
    title: String,
    icon: DrawableResource,
    isAlwaysOnTop: Boolean,
    isLocked: Boolean,
    isTransparent: Boolean,
    isMaximized: Boolean,
    isResizable: Boolean,
    isMaximizeButtonShown: Boolean,
    onTuneClick: (() -> Unit)?,
    tuneContextMenuItems: List<ContextMenuItem>?,
    onAlwaysOnTopClick: (() -> Unit)?,
    onLockClick: (() -> Unit)?,
    onTransparentClick: (() -> Unit)?,
    onMaximizeClick: (() -> Unit)?,
    onMinimizeClick: () -> Unit,
    onCloseClick: () -> Unit,
    width: Dp,
    height: Dp,
    titleBarStyle: TitleBarStyle?,
    titleBarContent: @Composable ((height: Dp) -> Unit)? = null,
    withContentPadding: Boolean,
    content: @Composable WindowScope.() -> Unit,
) {
    val activeTransition = updateTransition(LocalWindowInfo.current.isWindowFocused)
    val transparentWindowController: TransparentWindowController = remember { koin.get() }
    val backgroundColor = transparentWindowController.getWindowBackgroundColor(activeTransition, isTransparent)

    Box(
        modifier = Modifier
            .background(backgroundColor)
            .size(width, height)
            .pointerHoverIcon(PointerIcon(Cursors.pointer)),
    ) {
        BackgroundDots(activeTransition, width, height, isTransparent)
        WindowBorder(activeTransition, width, height)
        Column(
            modifier = Modifier.padding(1.dp),
        ) {
            if (titleBarStyle != null) {
                TitleBar(
                    style = titleBarStyle,
                    title = title,
                    icon = icon,
                    titleBarContent = titleBarContent,
                    isAlwaysOnTop = isAlwaysOnTop,
                    isLocked = isLocked,
                    isTransparent = isTransparent,
                    isMaximized = isMaximized,
                    isResizable = isResizable,
                    isMaximizeButtonShown = isMaximizeButtonShown,
                    onTuneClick = onTuneClick,
                    tuneContextMenuItems = tuneContextMenuItems,
                    onAlwaysOnTopClick = onAlwaysOnTopClick,
                    onLockClick = onLockClick,
                    onTransparentClick = onTransparentClick,
                    onMaximizeClick = onMaximizeClick,
                    onMinimizeClick = onMinimizeClick,
                    onCloseClick = onCloseClick,
                    width = width,
                )
            }
            Box(
                modifier = Modifier
                    .zIndex(-1f)
                    .modifyIf(withContentPadding) {
                        padding(start = Spacing.large, end = Spacing.large, bottom = Spacing.large)
                    },
            ) {
                content()
            }
        }
    }
}

@Composable
private fun WindowBorder(
    activeTransition: Transition<Boolean>,
    width: Dp,
    height: Dp,
) {
    val transitionSpec = getActiveWindowTransitionSpec<Color>()
    val borderColor by activeTransition.animateColor(transitionSpec) {
        if (it) RiftTheme.colors.windowBorderActive else RiftTheme.colors.windowBorder
    }
    val activeBorderColor by activeTransition.animateColor(transitionSpec) {
        if (it) RiftTheme.colors.borderPrimary else Color.Transparent
    }
    Box(
        modifier = Modifier
            .size(width, height)
            .border(1.dp, borderColor),
    )
    Box(
        modifier = Modifier
            .height(1.dp)
            .width(width)
            .background(activeBorderColor),
    )
    Box(
        modifier = Modifier
            .graphicsLayer(renderEffect = BlurEffect(6f, 6f, edgeTreatment = TileMode.Decal))
            .height(2.dp)
            .width(width)
            .background(activeBorderColor),
    )
}

@Composable
private fun WindowScope.TitleBar(
    style: TitleBarStyle,
    title: String,
    icon: DrawableResource,
    titleBarContent: @Composable ((height: Dp) -> Unit)? = null,
    width: Dp,
    isAlwaysOnTop: Boolean,
    isLocked: Boolean,
    isTransparent: Boolean,
    isMaximized: Boolean,
    isResizable: Boolean,
    isMaximizeButtonShown: Boolean,
    onTuneClick: (() -> Unit)?,
    tuneContextMenuItems: List<ContextMenuItem>?,
    onAlwaysOnTopClick: (() -> Unit)?,
    onLockClick: (() -> Unit)?,
    onTransparentClick: (() -> Unit)?,
    onMaximizeClick: (() -> Unit)?,
    onMinimizeClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    val contextMenuItems = getTitleBarContextMenuItems(
        isAlwaysOnTop = isAlwaysOnTop,
        isLocked = isLocked,
        isTransparent = isTransparent,
        isMaximized = isMaximized,
        isResizable = isResizable,
        onAlwaysOnTopClick = onAlwaysOnTopClick,
        onLockClick = onLockClick,
        onTransparentClick = onTransparentClick,
        onMinimizeClick = onMinimizeClick,
        onMaximizeClick = onMaximizeClick,
        onCloseClick = onCloseClick,
    )
    if (isLocked) {
        TitleBar(
            style = style,
            width = width,
            contextMenuItems = contextMenuItems,
            icon = icon,
            titleBarContent = titleBarContent,
            title = title,
            onTuneClick = onTuneClick,
            tuneContextMenuItems = tuneContextMenuItems,
            onAlwaysOnTopClick = onAlwaysOnTopClick,
            isAlwaysOnTop = isAlwaysOnTop,
            onLockClick = onLockClick,
            isLocked = isLocked,
            onTransparentClick = onTransparentClick,
            isTransparent = isTransparent,
            onMaximizeClick = onMaximizeClick.takeIf { isMaximizeButtonShown || isMaximized },
            isMaximized = isMaximized,
            onMinimizeClick = onMinimizeClick,
            onCloseClick = onCloseClick,
        )
    } else {
        ImprovedWindowDraggableArea {
            TitleBar(
                style = style,
                width = width,
                contextMenuItems = contextMenuItems,
                icon = icon,
                titleBarContent = titleBarContent,
                title = title,
                onTuneClick = onTuneClick,
                tuneContextMenuItems = tuneContextMenuItems,
                onAlwaysOnTopClick = onAlwaysOnTopClick,
                isAlwaysOnTop = isAlwaysOnTop,
                onLockClick = onLockClick,
                isLocked = isLocked,
                onTransparentClick = onTransparentClick,
                isTransparent = isTransparent,
                onMaximizeClick = onMaximizeClick.takeIf { isMaximizeButtonShown || isMaximized },
                isMaximized = isMaximized,
                onMinimizeClick = onMinimizeClick,
                onCloseClick = onCloseClick,
            )
        }
    }
}

@Composable
private fun TitleBar(
    style: TitleBarStyle,
    width: Dp,
    contextMenuItems: List<ContextMenuItem>,
    icon: DrawableResource,
    titleBarContent: @Composable ((height: Dp) -> Unit)?,
    title: String,
    onTuneClick: (() -> Unit)?,
    tuneContextMenuItems: List<ContextMenuItem>?,
    onAlwaysOnTopClick: (() -> Unit)?,
    isAlwaysOnTop: Boolean,
    onLockClick: (() -> Unit)?,
    isLocked: Boolean,
    onTransparentClick: (() -> Unit)?,
    isTransparent: Boolean,
    onMaximizeClick: (() -> Unit)?,
    isMaximized: Boolean,
    onMinimizeClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    val horizontalPadding = when (style) {
        TitleBarStyle.Full -> Spacing.mediumLarge
        TitleBarStyle.Small -> Spacing.medium
        TitleBarStyle.Minimal -> Spacing.medium
    }
    val height = when (style) {
        TitleBarStyle.Full -> 48.dp
        TitleBarStyle.Small -> 32.dp
        TitleBarStyle.Minimal -> 32.dp
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .pointerHoverIcon(PointerIcon(Cursors.drag))
            .size(width, height),
    ) {
        // Window icon
        RiftContextMenuArea(contextMenuItems) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = horizontalPadding),
            ) {
                if (style == TitleBarStyle.Full) {
                    Image(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = Spacing.medium)
                            .padding(vertical = Spacing.medium)
                            .size(32.dp),
                    )
                }
            }
        }
        // Window title or custom content
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            if (titleBarContent != null) {
                titleBarContent(height)
            } else {
                RiftContextMenuArea(contextMenuItems, modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = when (style) {
                            TitleBarStyle.Full -> RiftTheme.typography.headlineHighlighted
                            TitleBarStyle.Small -> RiftTheme.typography.headerHighlighted
                            TitleBarStyle.Minimal -> RiftTheme.typography.headerHighlighted
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        // Tune button
        RiftContextMenuArea(contextMenuItems) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier
                    .padding(start = Spacing.medium)
                    .padding(end = Spacing.small)
                    .padding(vertical = Spacing.medium),
            ) {
                if (onTuneClick != null) {
                    RiftImageButton(Res.drawable.window_titlebar_tune, 16.dp, onTuneClick)
                }
                if (tuneContextMenuItems != null) {
                    RiftContextMenuArea(
                        items = tuneContextMenuItems,
                        acceptsLeftClick = true,
                        acceptsRightClick = false,
                    ) {
                        RiftImageButton(Res.drawable.window_titlebar_tune, 16.dp, {})
                    }
                }
            }
        }
        // Menu button
        if (contextMenuItems.size > 2) {
            RiftContextMenuArea(contextMenuItems, acceptsLeftClick = true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier
                        .padding(horizontal = Spacing.small)
                        .padding(vertical = Spacing.medium),
                ) {
                    RiftImageButton(Res.drawable.window_titlebar_kebab, 16.dp, {})
                }
            }
        }
        // Window management buttons
        RiftContextMenuArea(contextMenuItems) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier
                    .padding(start = Spacing.small)
                    .padding(vertical = Spacing.medium)
                    .padding(end = horizontalPadding),
            ) {
                if (onAlwaysOnTopClick != null && isAlwaysOnTop) {
                    RiftImageButton(Res.drawable.window_overlay_fullscreen_on_16px, 16.dp, onAlwaysOnTopClick)
                }
                if (onLockClick != null && isLocked) {
                    RiftImageButton(Res.drawable.window_locked_16px, 16.dp, onLockClick)
                }
                if (onTransparentClick != null && isTransparent) {
                    RiftImageButton(Res.drawable.window_light_background_on_16px, 16.dp, onTransparentClick)
                }
                if (onMaximizeClick != null) {
                    val maximizeIcon = if (isMaximized) Res.drawable.window_titlebar_fullscreen else Res.drawable.window_titlebar_float
                    RiftImageButton(maximizeIcon, 16.dp, onMaximizeClick)
                }
                RiftImageButton(Res.drawable.window_titlebar_minimize, 16.dp, onMinimizeClick)
                RiftImageButton(Res.drawable.window_titlebar_close, 16.dp, onCloseClick)
            }
        }
    }
}

private fun getTitleBarContextMenuItems(
    isAlwaysOnTop: Boolean,
    isLocked: Boolean,
    isTransparent: Boolean,
    isMaximized: Boolean,
    isResizable: Boolean,
    onAlwaysOnTopClick: (() -> Unit)?,
    onLockClick: (() -> Unit)?,
    onTransparentClick: (() -> Unit)?,
    onMaximizeClick: (() -> Unit)?,
    onMinimizeClick: () -> Unit,
    onCloseClick: () -> Unit,
): List<ContextMenuItem> {
    return buildList {
        if (onAlwaysOnTopClick != null) {
            if (isAlwaysOnTop) {
                add(
                    ContextMenuItem.TextItem(
                        "Disable always above",
                        Res.drawable.window_overlay_fullscreen_on_16px,
                        onClick = onAlwaysOnTopClick,
                    ),
                )
            } else {
                add(
                    ContextMenuItem.TextItem(
                        "Enable always above",
                        Res.drawable.window_overlay_fullscreen_off_16px,
                        onClick = onAlwaysOnTopClick,
                    ),
                )
            }
        }
        if (onLockClick != null && !isMaximized) {
            if (isLocked) {
                add(
                    ContextMenuItem.TextItem(
                        "Unlock window size and position",
                        Res.drawable.window_locked_16px,
                        onClick = onLockClick,
                    ),
                )
            } else {
                add(
                    ContextMenuItem.TextItem(
                        "Lock window size and position",
                        Res.drawable.window_unlocked_16px,
                        onClick = onLockClick,
                    ),
                )
            }
        }
        if (onTransparentClick != null) {
            if (isTransparent) {
                add(
                    ContextMenuItem.TextItem(
                        "Disable transparency",
                        Res.drawable.window_light_background_on_16px,
                        onClick = onTransparentClick,
                    ),
                )
            } else {
                add(
                    ContextMenuItem.TextItem(
                        "Enable transparency",
                        Res.drawable.window_light_background_off_16px,
                        onClick = onTransparentClick,
                    ),
                )
            }
        }
        if (onAlwaysOnTopClick != null || onLockClick != null || onTransparentClick != null) {
            add(ContextMenuItem.DividerItem)
        }
        if (onMaximizeClick != null && isResizable) {
            if (isMaximized) {
                add(
                    ContextMenuItem.TextItem(
                        "Restore to floating",
                        Res.drawable.window_titlebar_fullscreen,
                        onClick = onMaximizeClick,
                    ),
                )
            } else {
                add(
                    ContextMenuItem.TextItem(
                        "Maximize",
                        Res.drawable.window_titlebar_float,
                        onClick = onMaximizeClick,
                    ),
                )
            }
        }
        add(ContextMenuItem.TextItem("Minimize", onClick = onMinimizeClick))
        add(ContextMenuItem.TextItem("Close", iconContent = { RiftMulticolorIcon(MulticolorIconType.Warning, it) }, onClick = onCloseClick))
    }
}

@Composable
private fun BackgroundDots(
    activeTransition: Transition<Boolean>,
    width: Dp,
    height: Dp,
    isTransparent: Boolean,
) {
    val bitmap = imageResource(if (isTransparent) Res.drawable.window_background_dots_light else Res.drawable.window_background_dots)
    val brush = remember(bitmap) { ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated)) }
    val transitionSpec = getActiveWindowTransitionSpec<Float>()
    val alpha by activeTransition.animateFloat(transitionSpec) {
        if (it) {
            if (isTransparent) 0.1f else 1f
        } else {
            0f
        }
    }
    Box(Modifier.alpha(alpha).size(width, height).background(brush))
}

fun <T> getActiveWindowTransitionSpec(): @Composable Transition.Segment<Boolean>.() -> FiniteAnimationSpec<T> {
    return {
        when {
            false isTransitioningTo true -> spring(stiffness = Spring.StiffnessMedium)
            else -> spring(stiffness = Spring.StiffnessVeryLow)
        }
    }
}
