package dev.nohus.rift.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun AnimatedImage(
    resource: String,
    modifier: Modifier = Modifier,
) {
    KamelImage(
        resource = {
            asyncPainterResource(resource)
        },
        contentDescription = null,
        modifier = modifier,
    )
}
