package dev.nohus.rift.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.flag_background
import dev.nohus.rift.generated.resources.flag_negative
import dev.nohus.rift.generated.resources.flag_neutral
import dev.nohus.rift.generated.resources.flag_positive
import dev.nohus.rift.generated.resources.flag_self
import dev.nohus.rift.generated.resources.flag_star
import dev.nohus.rift.standings.Standing
import dev.nohus.rift.standings.StandingUtils
import dev.nohus.rift.standings.getFlagColor
import org.jetbrains.compose.resources.painterResource

@Composable
fun FlagIcon(standing: Float, modifier: Modifier = Modifier) {
    val level = StandingUtils.getStandingLevel(standing)
    FlagIcon(level, modifier)
}

@Composable
fun FlagIcon(standing: Standing, modifier: Modifier = Modifier) {
    val tooltip = when (standing) {
        Standing.Terrible -> "极差声望"
        Standing.Bad -> "不佳声望"
        Standing.Neutral -> "中立声望"
        Standing.Good -> "良好声望"
        Standing.Excellent -> "极佳声望"
        Standing.Self -> "你自己"
        Standing.Corporation -> "同属克隆飞行员军团"
        Standing.Alliance -> "同属联盟"
    }
    RiftTooltipArea(tooltip, modifier) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            val tint = standing.getFlagColor().copy(alpha = 0.75f)
            Image(
                painter = painterResource(Res.drawable.flag_background),
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier.size(12.dp),
            )
            val icon = when (standing) {
                Standing.Terrible, Standing.Bad -> Res.drawable.flag_negative
                Standing.Neutral -> Res.drawable.flag_neutral
                Standing.Good, Standing.Excellent -> Res.drawable.flag_positive
                Standing.Self -> Res.drawable.flag_self
                Standing.Corporation, Standing.Alliance -> Res.drawable.flag_star
            }
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(8.dp),
            )
        }
    }
}
