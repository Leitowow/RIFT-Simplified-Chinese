package dev.nohus.rift.compose

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import org.jetbrains.skiko.FPSCounter

@Composable
fun FpsCounter() {
    var fps by remember { mutableStateOf(0) }
    Text("FPS: $fps")
    LaunchedEffect(Unit) {
        val fpsCounter = FPSCounter(logOnTick = false)
        while (true) {
            withFrameNanos {
                fps = fpsCounter.average
                fpsCounter.tick()
            }
        }
    }
}
