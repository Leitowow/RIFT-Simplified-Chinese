package dev.nohus.rift.opportunities.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AsyncAllianceLogo
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.ClickableAlliance
import dev.nohus.rift.compose.ClickableCharacter
import dev.nohus.rift.compose.ClickableCorporation
import dev.nohus.rift.compose.ClickableLocation
import dev.nohus.rift.compose.ClickableShip
import dev.nohus.rift.compose.ClickableSystem
import dev.nohus.rift.compose.ConstellationIllustrationIconSmall
import dev.nohus.rift.compose.RegionIllustrationIconSmall
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.SystemIllustrationIconSmall
import dev.nohus.rift.compose.VerticalGrid
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitStandings
import dev.nohus.rift.map.SecurityColors
import dev.nohus.rift.opportunities.GetOpportunityContributionAttributesUseCase.OpportunityContributionAttribute
import dev.nohus.rift.standings.Standing
import dev.nohus.rift.utils.roundSecurity
import dev.nohus.rift.utils.withColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun OpportunityAttribute(
    caption: String,
    icon: DrawableResource?,
    values: List<OpportunityContributionAttribute>,
    tooltip: String?,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Text(
            text = caption,
            style = RiftTheme.typography.bodySecondary,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            RiftTooltipArea(
                text = tooltip,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CutCornerShape(bottomStart = 8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .size(32.dp),
                ) {
                    if (icon != null) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = RiftTheme.colors.textSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            OpportunityAttributeValuesGrid(
                values = values,
            )
        }
    }
}

@Composable
private fun OpportunityAttributeValuesGrid(
    values: List<OpportunityContributionAttribute>,
) {
    VerticalGrid(
        minColumnWidth = 170.dp,
        verticalSpacing = Spacing.medium,
        horizontalSpacing = Spacing.medium,
    ) {
        sort(values).forEach { value ->
            when (value) {
                is OpportunityContributionAttribute.Text -> {
                    OpportunityAttributeValuesGridItem(
                        type = null,
                        name = value.text,
                        alpha = if (value.isPlain) 0.1f else 0.2f,
                    )
                }

                is OpportunityContributionAttribute.Type -> OpportunityAttributeValuesGridItem(
                    type = "Type",
                    name = value.type.name,
                    icon = {
                        AsyncTypeIcon(
                            typeId = value.type.id,
                            modifier = Modifier.size(32.dp),
                        )
                    },
                )

                is OpportunityContributionAttribute.TypeGroup -> OpportunityAttributeValuesGridItem(
                    type = "Group",
                    name = value.name,
                )

                is OpportunityContributionAttribute.Ship -> OpportunityAttributeValuesGridItem(
                    type = "Ship Type",
                    name = value.type.name,
                    icon = {
                        AsyncTypeIcon(
                            typeId = value.type.id,
                            modifier = Modifier.size(32.dp),
                        )
                    },
                    decorator = {
                        ClickableShip(value.type) {
                            it()
                        }
                    },
                )

                is OpportunityContributionAttribute.ShipGroup -> OpportunityAttributeValuesGridItem(
                    type = "Ship Group",
                    name = value.name,
                    icon = {
                        Image(
                            painter = painterResource(value.icon),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(RiftTheme.colors.textSecondary),
                            modifier = Modifier.size(24.dp),
                        )
                    },
                )

                is OpportunityContributionAttribute.SolarSystem -> OpportunityAttributeValuesGridItem(
                    type = "Solar System",
                    name = buildAnnotatedString {
                        append(value.solarSystem.name)
                        append(" ")
                        val security = value.solarSystem.security.roundSecurity()
                        withColor(SecurityColors[security]) {
                            append("$security")
                        }
                    },
                    icon = {
                        SystemIllustrationIconSmall(value.solarSystem.id)
                    },
                    decorator = {
                        ClickableSystem(value.solarSystem.id) {
                            it()
                        }
                    },
                )

                is OpportunityContributionAttribute.Constellation -> OpportunityAttributeValuesGridItem(
                    type = "Constellation",
                    name = value.constellation.name,
                    icon = {
                        ConstellationIllustrationIconSmall(value.constellation.id)
                    },
                )

                is OpportunityContributionAttribute.Region -> OpportunityAttributeValuesGridItem(
                    type = "Region",
                    name = value.region.name,
                    icon = {
                        RegionIllustrationIconSmall(value.region.id)
                    },
                )

                is OpportunityContributionAttribute.Station -> OpportunityAttributeValuesGridItem(
                    type = null,
                    name = buildAnnotatedString {
                        val security = value.solarSystem?.security?.roundSecurity()
                        if (security != null) {
                            withColor(SecurityColors[security]) {
                                append("$security")
                                append(" ")
                            }
                        }
                        append(value.station?.name ?: "Unknown Station")
                    },
                    decorator = {
                        ClickableLocation(
                            systemId = value.station?.solarSystemId,
                            locationId = value.station?.stationId?.toLong(),
                            locationTypeId = value.station?.typeId,
                            locationName = value.station?.name,
                        ) {
                            it()
                        }
                    },
                )

                is OpportunityContributionAttribute.Structure -> OpportunityAttributeValuesGridItem(
                    type = null,
                    name = buildAnnotatedString {
                        val security = value.solarSystem?.security?.roundSecurity()
                        if (security != null) {
                            withColor(SecurityColors[security]) {
                                append("$security")
                                append(" ")
                            }
                        }
                        append(value.structure?.name ?: "Unknown Structure")
                    },
                    decorator = {
                        ClickableLocation(
                            systemId = value.structure?.solarSystemId,
                            locationId = value.structure?.structureId,
                            locationTypeId = value.structure?.typeId,
                            locationName = value.structure?.name,
                        ) {
                            it()
                        }
                    },
                )

                is OpportunityContributionAttribute.Character -> OpportunityAttributeValuesGridItem(
                    type = "Capsuleer",
                    name = value.character?.name ?: "${value.id}",
                    icon = {
                        DynamicCharacterPortraitStandings(
                            characterId = value.id,
                            size = 32.dp,
                            standingLevel = value.character?.standingLevel ?: Standing.Neutral,
                            isAnimated = true,
                        )
                    },
                    decorator = {
                        ClickableCharacter(value.id) {
                            it()
                        }
                    },
                )

                is OpportunityContributionAttribute.Corporation -> OpportunityAttributeValuesGridItem(
                    type = "Corporation",
                    name = value.name ?: "${value.id}",
                    icon = {
                        AsyncCorporationLogo(
                            corporationId = value.id,
                            size = 32,
                            modifier = Modifier.size(32.dp),
                        )
                    },
                    decorator = {
                        ClickableCorporation(value.id) {
                            it()
                        }
                    },
                )

                is OpportunityContributionAttribute.Alliance -> OpportunityAttributeValuesGridItem(
                    type = "Alliance",
                    name = value.name ?: "${value.id}",
                    icon = {
                        AsyncAllianceLogo(
                            allianceId = value.id,
                            size = 32,
                            modifier = Modifier.size(32.dp),
                        )
                    },
                    decorator = {
                        ClickableAlliance(value.id) {
                            it()
                        }
                    },
                )

                is OpportunityContributionAttribute.Faction -> OpportunityAttributeValuesGridItem(
                    type = "Faction",
                    name = value.name,
                    icon = {
                        AsyncCorporationLogo(
                            corporationId = value.id,
                            size = 32,
                            modifier = Modifier.size(32.dp),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun OpportunityAttributeValuesGridItem(
    type: String?,
    name: String,
    icon: @Composable (() -> Unit)? = null,
    alpha: Float = 0.2f,
    decorator: @Composable ((@Composable () -> Unit) -> Unit) = { Box(content = { it() }) },
) {
    OpportunityAttributeValuesGridItem(
        type = type,
        name = AnnotatedString(name),
        icon = icon,
        alpha = alpha,
        decorator = decorator,
    )
}

@Composable
private fun OpportunityAttributeValuesGridItem(
    type: String?,
    name: AnnotatedString,
    icon: @Composable (() -> Unit)? = null,
    alpha: Float = 0.2f,
    decorator: @Composable ((@Composable () -> Unit) -> Unit) = { Box(content = { it() }) },
) {
    decorator {
        Box(
            modifier = Modifier
                .clip(CutCornerShape(bottomEnd = 8.dp))
                .background(Color.White.copy(alpha = alpha))
                .fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .padding(Spacing.medium)
                    .clipToBounds()
                    .wrapContentWidth(Alignment.Start, unbounded = true),
            ) {
                if (icon != null) {
                    icon()
                }

                Column {
                    if (type != null) {
                        Text(
                            text = type,
                            style = RiftTheme.typography.detailSecondary,
                        )
                    }
                    Text(
                        text = name,
                        style = RiftTheme.typography.bodyHighlighted,
                        overflow = TextOverflow.Visible,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun sort(values: List<OpportunityContributionAttribute>): List<OpportunityContributionAttribute> {
    return values.sortedWith(
        compareBy({
            when (it) {
                is OpportunityContributionAttribute.SolarSystem -> 1
                is OpportunityContributionAttribute.Constellation -> 2
                is OpportunityContributionAttribute.Region -> 3
                is OpportunityContributionAttribute.Type -> 4
                is OpportunityContributionAttribute.TypeGroup -> 5
                is OpportunityContributionAttribute.Ship -> 6
                is OpportunityContributionAttribute.ShipGroup -> 7
                is OpportunityContributionAttribute.Character -> 8
                is OpportunityContributionAttribute.Corporation -> 9
                is OpportunityContributionAttribute.Alliance -> 10
                is OpportunityContributionAttribute.Faction -> 11
                is OpportunityContributionAttribute.Station -> 12
                is OpportunityContributionAttribute.Structure -> 13
                is OpportunityContributionAttribute.Text -> 14
            }
        }, {
            when (it) {
                is OpportunityContributionAttribute.Alliance -> it.name
                is OpportunityContributionAttribute.Character -> it.character?.name
                is OpportunityContributionAttribute.Constellation -> it.constellation.name
                is OpportunityContributionAttribute.Corporation -> it.name
                is OpportunityContributionAttribute.Faction -> it.name
                is OpportunityContributionAttribute.Region -> it.region.name
                is OpportunityContributionAttribute.Ship -> it.type.name
                is OpportunityContributionAttribute.ShipGroup -> it.name
                is OpportunityContributionAttribute.SolarSystem -> it.solarSystem.name
                is OpportunityContributionAttribute.Station -> it.station?.name
                is OpportunityContributionAttribute.Structure -> it.structure?.name
                is OpportunityContributionAttribute.Text -> it.text
                is OpportunityContributionAttribute.Type -> it.type.name
                is OpportunityContributionAttribute.TypeGroup -> it.name
            }
        }),
    )
}
