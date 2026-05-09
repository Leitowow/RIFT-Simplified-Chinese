package dev.nohus.rift.opportunities.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.RiftPill
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.opportunities.OpportunityCategoryFilter
import dev.nohus.rift.opportunities.OpportunityCategoryFilterType
import org.jetbrains.compose.resources.painterResource

@Composable
fun OpportunityCategoryFilterChips(
    filters: List<OpportunityCategoryFilter>,
    enabledFilters: Set<OpportunityCategoryFilter>,
    onCategoryFilterChange: (OpportunityCategoryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = modifier,
    ) {
        filters.forEach { filter ->
            RiftTooltipArea(
                tooltip = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.padding(Spacing.large),
                    ) {
                        Text(
                            text = filter.type.name,
                            style = RiftTheme.typography.bodySecondary,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        ) {
                            if (filter.icon != null) {
                                Icon(
                                    painter = painterResource(filter.icon),
                                    contentDescription = null,
                                    tint = RiftTheme.colors.textPrimary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Text(
                                text = filter.name,
                                style = RiftTheme.typography.bodyPrimary,
                            )
                        }
                        Text(
                            text = filter.description,
                            style = RiftTheme.typography.bodyPrimary,
                        )
                    }
                },
            ) {
                RiftPill(
                    text = filter.name,
                    icon = filter.icon.takeIf { filter.type == OpportunityCategoryFilterType.Feature },
                    isIconColor = false,
                    isSelected = filter in enabledFilters,
                    onClick = {
                        onCategoryFilterChange(filter)
                    },
                )
            }
        }
    }
}
