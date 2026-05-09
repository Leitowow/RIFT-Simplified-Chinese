package dev.nohus.rift.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.map_constellation
import dev.nohus.rift.generated.resources.map_region
import dev.nohus.rift.generated.resources.map_system
import dev.nohus.rift.map.SecurityColors
import dev.nohus.rift.repositories.SolarSystemChipLocation
import dev.nohus.rift.repositories.SolarSystemChipState
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.utils.roundSecurity
import dev.nohus.rift.utils.withColor
import org.jetbrains.compose.resources.painterResource

@Composable
fun RiftSolarSystemChip(
    state: SolarSystemChipState,
    hasBackground: Boolean = true,
    isOnlyShowingClosest: Boolean = false,
) {
    ClickableSystem(state.name) {
        val content = movableContentOf {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = Spacing.small, vertical = Spacing.verySmall),
            ) {
                val text = if (state.locationsText != null && !isOnlyShowingClosest) {
                    state.locationsText
                } else {
                    state.jumpsText
                }

                if (text != null) {
                    Text(
                        text = text,
                        maxLines = 1,
                        style = RiftTheme.typography.detailSecondary,
                        modifier = Modifier.padding(start = Spacing.small, end = Spacing.medium),
                    )
                }
                val securityColor = SecurityColors[state.security?.roundSecurity() ?: 0.0]
                Surface(
                    color = securityColor.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(2.dp),
                ) {
                    val text = buildAnnotatedString {
                        append(state.name)
                        if (state.region != null) {
                            withColor(RiftTheme.colors.textSecondary) {
                                append(" ${state.region}")
                            }
                        }
                    }
                    Text(
                        text = text,
                        maxLines = 1,
                        style = RiftTheme.typography.detailPrimary,
                        modifier = Modifier.padding(horizontal = Spacing.small, vertical = 1.dp),
                    )
                }
                if (state.security != null) {
                    Surface(
                        color = securityColor.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(2.dp),
                    ) {
                        Text(
                            text = state.security.roundSecurity().toString(),
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            style = RiftTheme.typography.detailPrimary,
                            modifier = Modifier.padding(horizontal = Spacing.small, vertical = 1.dp),
                        )
                    }
                }
            }
        }

        RiftTooltipArea(
            tooltip = if (state.tooltipLocations != null && !isOnlyShowingClosest) {
                { SolarSystemChipTooltip(state.tooltipLocations) }
            } else {
                null
            },
        ) {
            if (hasBackground) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
                ) {
                    content()
                }
            } else {
                content()
            }
        }
    }
}

@Composable
private fun SolarSystemChipTooltip(locations: List<SolarSystemChipLocation>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = Modifier.padding(Spacing.large),
    ) {
        val solarSystemRepository: SolarSystemsRepository = remember { koin.get() }
        locations.forEach { location ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                val icon = when (location) {
                    is SolarSystemChipLocation.SolarSystem -> Res.drawable.map_system
                    is SolarSystemChipLocation.Constellation -> Res.drawable.map_constellation
                    is SolarSystemChipLocation.Region -> Res.drawable.map_region
                    is SolarSystemChipLocation.Text -> null
                }
                if (icon != null) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = RiftTheme.colors.textPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }

                when (location) {
                    is SolarSystemChipLocation.SolarSystem -> {
                        val system = solarSystemRepository.getSystem(location.solarSystemId)
                        Text(
                            text = system?.name ?: "${location.solarSystemId}",
                            style = RiftTheme.typography.bodyPrimary,
                        )
                        val securityColor = SecurityColors[system?.security?.roundSecurity() ?: 0.0]
                        system?.security?.let {
                            Text(
                                text = it.roundSecurity().toString(),
                                style = RiftTheme.typography.bodySecondary.copy(fontWeight = FontWeight.Bold),
                                color = securityColor,
                            )
                        }
                    }

                    is SolarSystemChipLocation.Constellation -> {
                        val constellation = solarSystemRepository.getConstellation(location.constellationId)
                        Text(
                            text = "[Constellation]",
                            style = RiftTheme.typography.bodySecondary,
                        )
                        Text(
                            text = constellation?.name ?: "${location.constellationId}",
                            style = RiftTheme.typography.bodyPrimary,
                        )
                    }

                    is SolarSystemChipLocation.Region -> {
                        val region = solarSystemRepository.getRegion(location.regionId)
                        Text(
                            text = "[Region]",
                            style = RiftTheme.typography.bodySecondary,
                        )
                        Text(
                            text = region?.name ?: "${location.regionId}",
                            style = RiftTheme.typography.bodyPrimary,
                        )
                    }

                    is SolarSystemChipLocation.Text -> Text(location.toString())
                }
            }
        }
    }
}
