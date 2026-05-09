package dev.nohus.rift.opportunities.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.RiftVerticalGlowLine
import dev.nohus.rift.compose.Side
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.rememberPointerInteractionStateHolder
import dev.nohus.rift.compose.theme.EveColors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.checkmark_16px
import dev.nohus.rift.generated.resources.corporation_project_state_close_16px
import dev.nohus.rift.generated.resources.corporation_project_state_time_16px
import dev.nohus.rift.network.esi.models.OpportunityState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private data class OpportunityState(
    val text: String,
    val color: Color,
    val icon: DrawableResource,
)

@Composable
fun OpportunityStateInfo(state: OpportunityState) {
    val state = when (state) {
        OpportunityState.Unspecified -> null
        OpportunityState.Active -> null
        OpportunityState.Closed -> OpportunityState(
            text = "Closed",
            color = EveColors.warningOrange,
            icon = Res.drawable.corporation_project_state_close_16px,
        )
        OpportunityState.Completed -> OpportunityState(
            text = "Completed",
            color = EveColors.successGreen,
            icon = Res.drawable.checkmark_16px,
        )
        OpportunityState.Expired -> OpportunityState(
            text = "Expired",
            color = EveColors.dangerRed,
            icon = Res.drawable.corporation_project_state_time_16px,
        )
        OpportunityState.Deleted -> null
    }

    if (state != null) {
        val pointerInteractionStateHolder = rememberPointerInteractionStateHolder()
        Box(
            modifier = Modifier
                .pointerInteraction(pointerInteractionStateHolder)
                .height(IntrinsicSize.Min)
                .fillMaxWidth()
                .background(state.color.copy(alpha = 0.2f)),
        ) {
            RiftVerticalGlowLine(pointerInteractionStateHolder, state.color, Side.Left)

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.large),
                modifier = Modifier
                    .padding(vertical = Spacing.medium, horizontal = Spacing.large),
            ) {
                Image(
                    painter = painterResource(state.icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(state.color),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = state.text,
                    style = RiftTheme.typography.bodyPrimary.copy(color = state.color),
                )
            }
        }
    }
}
