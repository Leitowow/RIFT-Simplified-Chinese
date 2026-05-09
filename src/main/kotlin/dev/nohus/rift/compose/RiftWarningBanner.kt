package dev.nohus.rift.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.warning
import dev.nohus.rift.generated.resources.warningtriangle
import org.jetbrains.compose.resources.painterResource

@Composable
fun RiftWarningBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(RiftTheme.colors.warningBackground),
    ) {
        Image(
            painter = painterResource(Res.drawable.warningtriangle),
            contentDescription = null,
            colorFilter = ColorFilter.tint(RiftTheme.colors.warningColor),
            modifier = Modifier.size(8.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.padding(Spacing.medium),
        ) {
            Image(
                painter = painterResource(Res.drawable.warning),
                contentDescription = null,
                colorFilter = ColorFilter.tint(RiftTheme.colors.warningColor),
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = text,
                style = RiftTheme.typography.bodyPrimary.copy(color = RiftTheme.colors.warningColor),
            )
        }
    }
}
