package dev.nohus.rift.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.RiftTheme
import java.time.Duration
import java.time.Instant

@Composable
fun IntelTimer(
    timestamp: Instant,
    style: TextStyle,
    rowHeight: Dp? = null,
    modifier: Modifier = Modifier,
) {
    val now = LocalNow.current
    val duration = Duration.between(timestamp, now)
    val colorFadePercentage = (duration.toSeconds() / Duration.ofMinutes(3).seconds.toFloat()).coerceIn(0f, 1f)
    val color = lerp(RiftTheme.colors.textSpecialHighlighted, RiftTheme.colors.textSecondary, colorFadePercentage)
    val borderColor = lerp(RiftTheme.colors.textSpecialHighlighted, RiftTheme.colors.borderGreyLight, colorFadePercentage)
    val content = @Composable {
        Text(
            text = formatDuration(duration),
            style = style.copy(color = color),
            modifier = modifier,
        )
    }
    if (rowHeight != null) {
        BorderedToken(rowHeight, borderColor) {
            content()
        }
    } else {
        content()
    }
}

private fun formatDuration(duration: Duration): String {
    val minutes = duration.toMinutes()
    return if (minutes < 10) {
        val seconds = duration.toSecondsPart()
        String.format("%d:%02d", minutes, seconds)
    } else if (minutes < 60) {
        "${minutes}m"
    } else {
        val hours = duration.toHours()
        "${hours}h"
    }
}

@Composable
fun BorderedToken(
    rowHeight: Dp,
    borderColor: Color = RiftTheme.colors.borderGreyLight,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(IntrinsicSize.Max)
            .heightIn(min = rowHeight)
            .border(1.dp, borderColor),
        content = content,
    )
}
