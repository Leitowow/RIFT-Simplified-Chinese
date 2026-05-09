package dev.nohus.rift.compose

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.RiftTheme

@Composable
fun ScrollbarColumn(
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    scrollbarModifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    isScrollbarConditional: Boolean = false,
    hasScrollbarBackground: Boolean = false,
    isFillWidth: Boolean = true,
    onHasScrollbarChange: (Boolean) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier,
    ) {
        val density = LocalDensity.current
        var scrollbarHeight by remember { mutableStateOf(0.dp) }
        Column(
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            modifier = Modifier
                .modifyIf(isFillWidth) { weight(1f) }
                .verticalScroll(scrollState)
                .onSizeChanged {
                    with(density) {
                        scrollbarHeight = it.height.toDp()
                    }
                }
                .clipToBounds()
                .padding(contentPadding),
        ) {
            content()
        }
        val canScroll = scrollState.canScrollBackward || scrollState.canScrollForward
        val hasScrollbar = !isScrollbarConditional || canScroll && scrollbarHeight > 0.dp
        LaunchedEffect(hasScrollbar) {
            onHasScrollbarChange(hasScrollbar)
        }
        if (hasScrollbar) {
            RiftVerticalScrollbar(
                hasBackground = hasScrollbarBackground,
                scrollState = scrollState,
                modifier = scrollbarModifier.height(scrollbarHeight),
            )
        }
    }
}

@Composable
fun ScrollbarLazyColumn(
    listState: LazyListState = rememberLazyListState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    scrollbarBackground: Color = RiftTheme.colors.windowBackground,
    modifier: Modifier = Modifier,
    scrollbarModifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    Row(
        modifier = modifier,
    ) {
        val density = LocalDensity.current
        var scrollbarHeight by remember { mutableStateOf(0.dp) }
        LazyColumn(
            state = listState,
            verticalArrangement = verticalArrangement,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            modifier = Modifier
                .weight(1f)
                .onSizeChanged {
                    with(density) {
                        scrollbarHeight = it.height.toDp()
                    }
                },
            content = content,
        )
        RiftVerticalScrollbar(
            listState = listState,
            reverseLayout = reverseLayout,
            background = scrollbarBackground,
            modifier = scrollbarModifier.height(scrollbarHeight),
        )
    }
}

@Composable
fun ScrollbarLazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    scrollbarModifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: LazyGridScope.() -> Unit,
) {
    Row(
        modifier = modifier,
    ) {
        val density = LocalDensity.current
        var scrollbarHeight by remember { mutableStateOf(0.dp) }
        LazyVerticalGrid(
            columns = columns,
            state = gridState,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            modifier = Modifier
                .weight(1f)
                .onSizeChanged {
                    with(density) {
                        scrollbarHeight = it.height.toDp()
                    }
                },
            content = content,
        )
        RiftVerticalScrollbar(
            gridState = gridState,
            modifier = scrollbarModifier.height(scrollbarHeight),
        )
    }
}
