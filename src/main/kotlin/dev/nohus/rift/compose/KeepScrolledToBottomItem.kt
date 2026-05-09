package dev.nohus.rift.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListScope

/**
 * Trick to keep scrolled to the bottom only if we are at the bottom
 * Having a 0px high item at the very start (therefore bottom with reverseLayout) will be the current scroll position
 * if and only if we are scrolled to the very bottom
 */
fun LazyListScope.keepScrolledToBottomItem() {
    item(key = "KeepScrolledToBottom") {
        Box {}
    }
}
