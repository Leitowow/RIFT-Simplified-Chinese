package dev.nohus.rift.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.expand_more_16px
import org.jetbrains.compose.resources.painterResource

@Composable
fun ExpandChevron(isExpanded: Boolean) {
    val rotation by animateFloatAsState(if (isExpanded) 0f else -90f)
    Image(
        painter = painterResource(Res.drawable.expand_more_16px),
        contentDescription = null,
        modifier = Modifier
            .padding(horizontal = Spacing.small)
            .rotate(rotation)
            .size(16.dp),
    )
}
