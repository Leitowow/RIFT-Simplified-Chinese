package dev.nohus.rift.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource

/**
 * Version of imageResource that will do a high quality rescale of the image and cache the result
 */
@Composable
fun imageResourceRescaling(
    resource: DrawableResource,
    sizeToScale: Int,
    cache: MutableMap<Pair<DrawableResource, Int>, ImageBitmap>,
): ImageBitmap {
    val original = imageResource(resource)
    val scaled = remember {
        cache[resource to sizeToScale]?.let { return@remember it }
        original.scale(sizeToScale, sizeToScale).also { cache[resource to sizeToScale] = it }
    }
    return scaled
}
