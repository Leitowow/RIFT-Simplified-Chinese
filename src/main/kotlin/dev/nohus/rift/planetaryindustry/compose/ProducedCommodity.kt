package dev.nohus.rift.planetaryindustry.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.planetaryindustry.models.Commodities
import dev.nohus.rift.planetaryindustry.models.Pin
import dev.nohus.rift.planetaryindustry.models.Route
import dev.nohus.rift.planetaryindustry.models.RoutedState
import dev.nohus.rift.planetaryindustry.models.isRouted
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.utils.formatDurationCompact
import dev.nohus.rift.utils.plural

@Composable
fun ProducedCommodity(
    pin: Pin,
    routes: List<Route>,
    type: Type?,
) {
    val verb = if (pin is Pin.Extractor) "开采中" else "生产中"
    if (type != null) {
        val tooltip = buildAnnotatedString {
            withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                append(type.name)
            }
            append(" ")
            withStyle(SpanStyle(color = RiftTheme.colors.textSecondary)) {
                append(Commodities.getTierName(type.name))
            }

            if (pin is Pin.Factory && pin.schematic != null) {
                appendLine()
                withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                    val quantity = pin.schematic.outputQuantity
                    val duration = formatDurationCompact(pin.schematic.cycleTime)
                    append("每 $duration 生产 $quantity 单位")
                }
            }
        }

        RiftTooltipArea(
            text = tooltip,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                AsyncTypeIcon(
                    type = type,
                    modifier = Modifier.size(32.dp),
                )
                Column {
                    Text(
                        text = verb,
                        style = RiftTheme.typography.bodyHighlighted,
                    )
                    Text(
                        text = type.name,
                        style = RiftTheme.typography.bodyPrimary,
                    )

                    when (pin.isRouted(routes)) {
                        RoutedState.Routed -> {}
                        RoutedState.InputNotRouted -> {
                            Text(
                                text = "无输入路线",
                                style = RiftTheme.typography.bodyPrimary.copy(color = RiftTheme.colors.textRed, fontWeight = FontWeight.Bold),
                            )
                        }
                        RoutedState.OutputNotRouted -> {
                            Text(
                                text = "未设置路线",
                                style = RiftTheme.typography.bodyPrimary.copy(color = RiftTheme.colors.textRed, fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Column {
                Text(
                    text = verb,
                    style = RiftTheme.typography.bodyHighlighted,
                )
                Text(
                    text = "未选择",
                    style = RiftTheme.typography.bodyPrimary.copy(color = RiftTheme.colors.textRed, fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}
