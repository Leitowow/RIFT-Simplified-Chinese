package dev.nohus.rift.compose

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyLayoutScrollScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.layout.LazyLayoutScrollScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import kotlinx.coroutines.delay

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T> InfiniteScrollingCarousel(
    items: List<T>,
    delay: Long,
    modifier: Modifier = Modifier,
    content: @Composable (item: T) -> Unit,
) {
    suspend fun LazyListState.customScroll(block: suspend LazyLayoutScrollScope.() -> Unit) = scroll {
        block.invoke(LazyLayoutScrollScope(this@customScroll, this))
    }

    val listState = rememberLazyListState()
    var isScrolling by remember { mutableStateOf(true) }
    var currentIndex by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            listState.scrollToItem(currentIndex)
            delay(delay)
            if (isScrolling) {
                listState.customScroll {
                    snapToItem(currentIndex)
                    val distance = calculateDistanceTo(currentIndex + 1).toFloat()
                    var previousValue = 0f
                    animate(0f, distance, animationSpec = tween(1_000)) { currentValue, _ ->
                        previousValue += scrollBy(currentValue - previousValue)
                    }
                }
                currentIndex++
            }
        }
    }
    LazyRow(
        state = listState,
        userScrollEnabled = false,
        modifier = modifier
            .onPointerEvent(PointerEventType.Enter) { isScrolling = false }
            .onPointerEvent(PointerEventType.Exit) { isScrolling = true },
    ) {
        items(Int.MAX_VALUE) { index ->
            val item = items[index % items.size]
            content(item)
        }
    }
}
