@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.nohus.rift.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

data class SharedElementScopes(
    val sharedTransitionScope: SharedTransitionScope,
    val animatedVisibilityScope: AnimatedVisibilityScope,
    val overlayClip: SharedTransitionScope.OverlayClip,
)

val LocalSharedElementScopes = staticCompositionLocalOf<SharedElementScopes?> { null }

@Composable
fun <S> SharedTransitionAnimatedContent(
    targetState: S,
    contentKey: (targetState: S) -> Any? = { it },
    content: @Composable (targetState: S) -> Unit,
) {
    var sharedTransitionLayoutSize by remember { mutableStateOf(Offset.Zero) }
    val overlayClip = remember(sharedTransitionLayoutSize) {
        getLayoutBoundsOverlayClip(sharedTransitionLayoutSize)
    }
    SharedTransitionLayout(
        modifier = Modifier
            .onSizeChanged {
                sharedTransitionLayoutSize = Offset(it.width.toFloat(), it.height.toFloat())
            },
    ) {
        AnimatedContent(targetState, contentKey = contentKey) { target ->
            CompositionLocalProvider(
                LocalSharedElementScopes provides SharedElementScopes(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent,
                    overlayClip = overlayClip,
                ),
            ) {
                content(target)
            }
        }
    }
}

@Composable
fun Modifier.sharedTransitionElement(
    key: Any,
): Modifier {
    val scopes = LocalSharedElementScopes.current ?: return this
    with(scopes.sharedTransitionScope) transition@{
        with(scopes.animatedVisibilityScope) animation@{
            return sharedElement(
                sharedContentState = rememberSharedContentState(key),
                animatedVisibilityScope = this@animation,
                clipInOverlayDuringTransition = scopes.overlayClip,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
private fun getLayoutBoundsOverlayClip(
    sharedTransitionLayoutSize: Offset,
): SharedTransitionScope.OverlayClip {
    return object : SharedTransitionScope.OverlayClip {
        override fun getClipPath(
            sharedContentState: SharedTransitionScope.SharedContentState,
            bounds: Rect,
            layoutDirection: LayoutDirection,
            density: Density,
        ): Path {
            return Path().apply {
                addRect(Rect(Offset.Zero, sharedTransitionLayoutSize))
            }
        }
    }
}
