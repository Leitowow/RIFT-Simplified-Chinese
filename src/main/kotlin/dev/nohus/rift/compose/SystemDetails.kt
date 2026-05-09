package dev.nohus.rift.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.location.LocationRepository
import dev.nohus.rift.repositories.GetSystemDistanceFromCharacterUseCase
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.WormholeRegionClasses
import dev.nohus.rift.standings.getColor
import dev.nohus.rift.utils.plural
import dev.nohus.rift.utils.withColor

@Composable
fun RowScope.SystemDetails(
    systemId: Int,
    rowHeight: Dp,
    isShowingSystemDistance: Boolean,
    isUsingJumpBridges: Boolean,
    enterAnimation: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) },
    isShowingName: Boolean = true,
) {
    val repository: SolarSystemsRepository by koin.inject()
    val system = repository.getSystem(systemId) ?: return
    SystemDetails(
        system = system,
        rowHeight = rowHeight,
        isShowingSystemDistance = isShowingSystemDistance,
        isUsingJumpBridges = isUsingJumpBridges,
        enterAnimation = enterAnimation,
        isShowingName = isShowingName,
    )
}

@Suppress("UnusedReceiverParameter")
@Composable
fun RowScope.SystemDetails(
    system: MapSolarSystem,
    rowHeight: Dp,
    isShowingSystemDistance: Boolean,
    isUsingJumpBridges: Boolean,
    enterAnimation: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) },
    isShowingName: Boolean = true,
) {
    val repository: SolarSystemsRepository by koin.inject()
    ClickableSystem(system.id) {
        RiftTooltipArea(
            text = system.name,
        ) {
            SystemIllustrationIconSmall(
                solarSystemId = system.id,
                size = rowHeight,
                animation = enterAnimation,
                modifier = Modifier.clipToBounds(),
            )
        }
    }
    VerticalDivider(color = RiftTheme.colors.borderGreyLight, modifier = Modifier.height(rowHeight))
    if (isShowingName) {
        ClickableSystem(system.id) {
            Column(
                modifier = Modifier.padding(horizontal = Spacing.small),
            ) {
                if (system.abyssalName != null) {
                    Text(
                        text = system.abyssalName,
                        style = RiftTheme.typography.bodyTriglavian.copy(fontWeight = FontWeight.Bold, color = RiftTheme.colors.textLink),
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                } else {
                    Text(
                        text = system.name,
                        style = RiftTheme.typography.bodyLink.copy(fontWeight = FontWeight.Bold),
                    )
                    if (rowHeight >= 32.dp) {
                        repository.getRegionBySystem(system.name)?.let { region ->
                            val text = WormholeRegionClasses[region.name] ?: region.name
                            Text(
                                text = text,
                                style = RiftTheme.typography.detailPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
    if (isShowingSystemDistance) {
        SystemDistanceIndicator(system.id, rowHeight, isUsingJumpBridges)
    }
}

@Composable
fun RowScope.LocationDetails(
    station: LocationRepository.Station,
    rowHeight: Dp,
    enterAnimation: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) },
) {
    ClickableLocation(
        systemId = station.solarSystemId,
        locationId = station.stationId.toLong(),
        locationTypeId = station.typeId,
        locationName = station.name,
    ) {
        RiftTooltipArea(
            text = station.name,
        ) {
            AsyncTypeIcon(
                typeId = station.typeId,
                modifier = Modifier.size(rowHeight),
            )
        }
    }
    SystemDetails(
        systemId = station.solarSystemId,
        rowHeight = rowHeight,
        isShowingSystemDistance = false,
        isUsingJumpBridges = false,
        enterAnimation = enterAnimation,
        isShowingName = false,
    )
    if (station.owner != null) {
        CharacterDetails(station.owner, rowHeight, isShowingName = false, isShowingContactLabel = false)
    }

    ClickableLocation(
        systemId = station.solarSystemId,
        locationId = station.stationId.toLong(),
        locationTypeId = station.typeId,
        locationName = station.name,
    ) {
        val ticker = buildString {
            station.owner?.corporationTicker.let { append("$it ") }
            station.owner?.allianceTicker?.let { append(it) }
        }
        var nameStyle = RiftTheme.typography.bodyHighlighted.copy(fontWeight = FontWeight.Bold)
        station.owner?.standingLevel?.getColor()?.let { nameStyle = nameStyle.copy(color = it) }
        Column(
            modifier = Modifier.padding(horizontal = Spacing.small),
        ) {
            Text(
                text = station.name,
                style = nameStyle,
            )
            Text(
                text = ticker,
                style = RiftTheme.typography.bodySecondary,
            )
        }
    }

    if (station.owner != null) {
        ContactLabelTag(
            details = station.owner,
            modifier = Modifier.padding(end = Spacing.small),
        )
    }
}

@Composable
fun RowScope.LocationDetails(
    structure: LocationRepository.Structure,
    rowHeight: Dp,
    enterAnimation: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) },
) {
    ClickableLocation(
        systemId = structure.solarSystemId,
        locationId = structure.structureId,
        locationTypeId = structure.typeId,
        locationName = structure.name,
    ) {
        RiftTooltipArea(
            text = structure.name,
        ) {
            AsyncTypeIcon(
                typeId = structure.typeId,
                modifier = Modifier.size(rowHeight),
            )
        }
    }
    SystemDetails(
        systemId = structure.solarSystemId,
        rowHeight = rowHeight,
        isShowingSystemDistance = false,
        isUsingJumpBridges = false,
        enterAnimation = enterAnimation,
        isShowingName = false,
    )
    if (structure.owner != null) {
        CharacterDetails(structure.owner, rowHeight, isShowingName = false, isShowingContactLabel = false)
    }

    ClickableLocation(
        systemId = structure.solarSystemId,
        locationId = structure.structureId,
        locationTypeId = structure.typeId,
        locationName = structure.name,
    ) {
        val ticker = buildString {
            structure.owner?.corporationTicker.let { append("$it ") }
            structure.owner?.allianceTicker?.let { append(it) }
        }
        var nameStyle = RiftTheme.typography.bodyHighlighted.copy(fontWeight = FontWeight.Bold)
        structure.owner?.standingLevel?.getColor()?.let { nameStyle = nameStyle.copy(color = it) }
        Column(
            modifier = Modifier.padding(horizontal = Spacing.small),
        ) {
            Text(
                text = structure.name,
                style = nameStyle,
            )
            Text(
                text = ticker,
                style = RiftTheme.typography.bodySecondary,
            )
        }
    }

    if (structure.owner != null) {
        ContactLabelTag(
            details = structure.owner,
            modifier = Modifier.padding(end = Spacing.small),
        )
    }
}

@Composable
private fun SystemDistanceIndicator(
    systemId: Int,
    height: Dp,
    isUsingJumpBridges: Boolean,
) {
    val getDistance: GetSystemDistanceFromCharacterUseCase by koin.inject()
    val localCharactersRepository: LocalCharactersRepository by koin.inject()
    val characterDistance = remember(systemId, isUsingJumpBridges, localCharactersRepository.characters.value) {
        getDistance(systemId, withJumpBridges = isUsingJumpBridges)
    }
    if (characterDistance == null) return
    val distanceColor = getDistanceColor(characterDistance.distance)
    val characterName = localCharactersRepository.characters.value
        .firstOrNull { it.characterId == characterDistance.characterId }
        ?.info?.name
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .height(height)
            .padding(top = 1.dp, bottom = 1.dp, end = 1.dp)
            .border(2.dp, distanceColor, RoundedCornerShape(100))
            .padding(horizontal = 4.dp),
    ) {
        RiftTooltipArea(
            buildAnnotatedString {
                withColor(RiftTheme.colors.textHighlighted) {
                    append("${characterDistance.distance}")
                }
                append(" jump${characterDistance.distance.plural} from ")
                withColor(RiftTheme.colors.textHighlighted) {
                    append(characterName ?: "${characterDistance.distance}")
                }
            },
        ) {
            Text(
                text = "${characterDistance.distance}",
                style = RiftTheme.typography.bodyPrimary.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier,
            )
        }
    }
}

private fun getDistanceColor(distance: Int): Color {
    return when {
        distance >= 5 -> Color(0xFF2E74DF)
        distance >= 4 -> Color(0xFF4ACFF3)
        distance >= 3 -> Color(0xFF5CDCA6)
        distance >= 2 -> Color(0xFF70E552)
        distance >= 1 -> Color(0xFFDC6C08)
        else -> Color(0xFFBC1113)
    }
}
