package dev.nohus.rift.jukebox

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.compose.TypingText
import dev.nohus.rift.compose.onMouseClick
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.jukebox_old
import dev.nohus.rift.generated.resources.window_jukebox
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OldJukeboxWindow(
    onCloseClick: () -> Unit,
    onFinish: (WindowPosition) -> Unit,
) {
    val size = DpSize(650.dp, 500.dp)
    val state = rememberWindowState(size = size)
    Window(
        onCloseRequest = onCloseClick,
        state = state,
        title = "Jukebox",
        icon = painterResource(Res.drawable.window_jukebox),
        undecorated = true,
        resizable = false,
    ) {
        val isSequenceStarted = remember { CompletableDeferred<Unit>() }
        var alphaTarget by remember { mutableStateOf(0f) }
        val alpha by animateFloatAsState(alphaTarget, tween(1000))
        var isText1Visible by remember { mutableStateOf(false) }
        val isText1Finished = remember { CompletableDeferred<Unit>() }
        var text1AlphaTarget by remember { mutableStateOf(1f) }
        val text1Alpha by animateFloatAsState(text1AlphaTarget, tween(1000))
        var isText2Visible by remember { mutableStateOf(false) }
        val isText2Finished = remember { CompletableDeferred<Unit>() }
        LaunchedEffect(Unit) {
            isSequenceStarted.await()
            alphaTarget = 0.7f
            delay(1500)
            isText1Visible = true
            isText1Finished.await()
            delay(2000)
            text1AlphaTarget = 0f
            delay(1500)
            isText2Visible = true
            isText2Finished.await()
            delay(2000)
            onFinish(state.position)
        }

        WindowDraggableArea {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.onMouseClick {
                    isSequenceStarted.complete(Unit)
                },
            ) {
                Image(
                    painter = painterResource(Res.drawable.jukebox_old),
                    contentDescription = null,
                    modifier = Modifier
                        .size(size)
                        .pointerHoverIcon(PointerIcon(Cursors.pointer)),
                )
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = alpha))
                        .fillMaxSize(),
                ) {}
                if (isText1Visible) {
                    TypingText(
                        text = buildAnnotatedString {
                            append("Some things are gone forever...")
                        },
                        style = RiftTheme.typography.headlinePrimary,
                        characterDuration = 50,
                        onFinishedTyping = { isText1Finished.complete(Unit) },
                        modifier = Modifier.alpha(text1Alpha),
                    )
                }
                if (isText2Visible) {
                    TypingText(
                        text = buildAnnotatedString {
                            append("...but some find their way back")
                        },
                        style = RiftTheme.typography.headlinePrimary,
                        characterDuration = 50,
                        onFinishedTyping = { isText2Finished.complete(Unit) },
                    )
                }
            }
        }
    }
}
