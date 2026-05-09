package dev.nohus.rift.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.LocalRiftColors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.compose.theme.getRiftColors
import org.jetbrains.compose.resources.DrawableResource
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RiftAutocompleteTextField(
    text: String,
    icon: DrawableResource? = null,
    suggestions: List<String>,
    placeholder: String? = null,
    onTextChanged: (String) -> Unit,
    onSuggestionConfirmed: (String) -> Unit = {},
    height: Dp = 32.dp,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        var size by remember { mutableStateOf(IntSize.Zero) }
        val focusRequester = remember { FocusRequester() }

        RiftTextField(
            text = text,
            icon = icon,
            placeholder = placeholder,
            onTextChanged = onTextChanged,
            height = height,
            onDeleteClick = onDeleteClick,
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusable()
                .onSizeChanged { size = it },
        )

        val fieldHeightPixels = LocalDensity.current.run { height.toPx().roundToInt() }
        var isExpanded by remember { mutableStateOf(false) }
        var lastAcceptedSuggestion: String? by remember { mutableStateOf(null) }
        LaunchedEffect(suggestions, text) {
            isExpanded = suggestions.isNotEmpty() && text != lastAcceptedSuggestion && text.isNotEmpty()
        }
        if (isExpanded) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, fieldHeightPixels + 2),
                onDismissRequest = { isExpanded = false },
            ) {
                CompositionLocalProvider(LocalRiftColors provides getRiftColors(isTransparent = false)) {
                    val width = with(LocalDensity.current) { size.width.toDp() }
                    Box(
                        modifier = Modifier
                            .width(width)
                            .height(IntrinsicSize.Min)
                            .background(RiftTheme.colors.windowBackground.copy(alpha = 1f))
                            .pointerHoverIcon(PointerIcon(Cursors.pointerDropdown)),
                    ) {
                        // Borders
                        Box(Modifier.height(1.dp).fillMaxWidth().align(Alignment.TopCenter).background(RiftTheme.colors.borderGreyDropdown))
                        Box(Modifier.height(1.dp).fillMaxWidth().align(Alignment.BottomCenter).background(RiftTheme.colors.borderGreyDropdown))
                        Box(Modifier.width(1.dp).fillMaxHeight().align(Alignment.CenterStart).background(RiftTheme.colors.borderGreyDropdown))
                        Box(Modifier.width(1.dp).fillMaxHeight().align(Alignment.CenterEnd).background(RiftTheme.colors.borderGreyDropdown))

                        Column(
                            modifier = Modifier,
                        ) {
                            for (suggestion in suggestions) {
                                Row(
                                    modifier = Modifier
                                        .hoverBackground()
                                        .onClick {
                                            isExpanded = false
                                            focusRequester.requestFocus()
                                            lastAcceptedSuggestion = suggestion
                                            onTextChanged(suggestion)
                                            onSuggestionConfirmed(suggestion)
                                        }
                                        .height(21.dp)
                                        .padding(Spacing.small)
                                        .fillMaxWidth(),
                                ) {
                                    Text(
                                        text = suggestion,
                                        style = RiftTheme.typography.bodyPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Divider(
                                    color = RiftTheme.colors.divider,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
