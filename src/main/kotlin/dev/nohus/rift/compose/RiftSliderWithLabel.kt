package dev.nohus.rift.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing

@Composable
fun RiftSliderWithLabel(
    label: String,
    width: Dp,
    range: IntRange,
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    getValueName: (Int) -> String = { "$it" },
    tooltip: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = modifier,
    ) {
        RiftTooltipArea(
            text = tooltip,
        ) {
            Text(
                text = label,
                style = RiftTheme.typography.bodyPrimary,
            )
        }

        RiftSlider(
            width = width,
            range = range,
            currentValue = currentValue,
            onValueChange = onValueChange,
            getValueName = getValueName,
        )
    }
}
