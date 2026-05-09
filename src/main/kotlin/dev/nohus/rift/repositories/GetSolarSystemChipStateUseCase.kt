package dev.nohus.rift.repositories

import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.location.CharacterLocationRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.utils.plural
import dev.nohus.rift.utils.roundSecurity
import org.koin.core.annotation.Single
import kotlin.time.measureTimedValue

data class SolarSystemChipState(
    val locationsText: String?,
    val jumpsText: String?,
    val name: String,
    val security: Double?,
    val distance: Int? = null,
    val region: String? = null,
    val tooltipLocations: List<SolarSystemChipLocation>? = null,
)

sealed interface SolarSystemChipLocation {
    data class SolarSystem(val solarSystemId: Int) : SolarSystemChipLocation
    data class Constellation(val constellationId: Int) : SolarSystemChipLocation
    data class Region(val regionId: Int) : SolarSystemChipLocation
    data class Text(val text: String) : SolarSystemChipLocation
}

@Single
class GetSolarSystemChipStateUseCase(
    private val solarSystemsRepository: SolarSystemsRepository,
    private val characterLocationRepository: CharacterLocationRepository,
    private val onlineCharactersRepository: OnlineCharactersRepository,
    private val getSystemDistanceUseCase: GetSystemDistanceUseCase,
) {

    operator fun invoke(
        location: SolarSystemChipLocation,
        nameOverride: String? = null,
        characterId: Int? = null,
        isShowingRegion: Boolean = false,
    ): SolarSystemChipState {
        return this(listOf(location), nameOverride, characterId, isShowingRegion)
    }

    operator fun invoke(
        locations: List<SolarSystemChipLocation>,
        nameOverride: String? = null,
        characterId: Int? = null,
        isShowingRegion: Boolean = false,
    ): SolarSystemChipState {
        // For custom text locations, just show that text without looking for a system
        locations.firstOrNull { it is SolarSystemChipLocation.Text }?.let {
            return SolarSystemChipState(
                locationsText = null,
                jumpsText = null,
                name = (it as SolarSystemChipLocation.Text).text,
                security = null,
                distance = null,
            )
        }

        val (pair, time) = measureTimedValue {
            getClosestSolarSystem(locations, characterId)
        }
        val (closestSolarSystem, distance) = pair
        val hasTooltip = locations.size > 1 || locations.any { it !is SolarSystemChipLocation.SolarSystem }
        val locationsText = if (hasTooltip) "${locations.size} Location${locations.size.plural}" else null
        val jumpsText = if (distance != null) {
            if (distance == 0) {
                "Current System"
            } else {
                "$distance jump${distance.plural}"
            }
        } else {
            if (characterLocationRepository.locations.value.isEmpty()) {
                null
            } else {
                "No gate route"
            }
        }

        val regionName = if (isShowingRegion && closestSolarSystem?.regionId != null) {
            solarSystemsRepository.getRegionBySystemId(closestSolarSystem.regionId)?.name
        } else {
            null
        }

        return SolarSystemChipState(
            locationsText = locationsText,
            jumpsText = jumpsText,
            name = nameOverride ?: closestSolarSystem?.name ?: "Unknown",
            security = closestSolarSystem?.security?.roundSecurity(),
            distance = distance,
            region = regionName,
            tooltipLocations = locations.takeIf { hasTooltip },
        )
    }

    /**
     * Returns the closest system to an online character from these locations, and the distance
     * If no character is online, uses offline characters
     */
    private fun getClosestSolarSystem(
        locations: List<SolarSystemChipLocation>,
        characterId: Int?,
    ): Pair<MapSolarSystem?, Int?> {
        val characterIds = if (characterId != null) {
            listOf(characterId)
        } else {
            onlineCharactersRepository.onlineCharacters.value
                .takeIf { it.isNotEmpty() } ?: characterLocationRepository.locations.value.keys
        }
        if (characterIds.isEmpty()) {
            getTargetSolarSystems(locations).firstOrNull()?.let {
                solarSystemsRepository.getSystem(it)?.let {
                    return it to null
                }
            }
            return null to null
        }

        // Check if one of the locations is a system a character is in
        val characterSolarSystems = characterIds.mapNotNull { characterId ->
            characterLocationRepository.locations.value[characterId]?.solarSystemId
        }.toSet()
        locations.filterIsInstance<SolarSystemChipLocation.SolarSystem>()
            .firstOrNull { it.solarSystemId in characterSolarSystems }
            ?.let { solarSystemsRepository.getSystem(it.solarSystemId)?.let { return it to 0 } }

        // Check if one of the locations is a constellation a character is in
        val characterConstellations = characterSolarSystems.mapNotNull { solarSystemId ->
            (solarSystemsRepository.getConstellationBySystemId(solarSystemId)?.id ?: return@mapNotNull null) to solarSystemId
        }.toMap()
        locations.filterIsInstance<SolarSystemChipLocation.Constellation>()
            .firstNotNullOfOrNull { characterConstellations[it.constellationId] }
            ?.let { solarSystemsRepository.getSystem(it)?.let { return it to 0 } }

        // Check if one of the locations is a region a character is in
        val characterRegions = characterSolarSystems.mapNotNull { solarSystemId ->
            (solarSystemsRepository.getRegionBySystemId(solarSystemId)?.id ?: return@mapNotNull null) to solarSystemId
        }.toMap()
        locations.filterIsInstance<SolarSystemChipLocation.Region>()
            .firstNotNullOfOrNull { characterRegions[it.regionId] }
            ?.let { solarSystemsRepository.getSystem(it)?.let { return it to 0 } }

        // None of the characters are in the given locations, we need to check which one is closest
        val targetSolarSystems = getTargetSolarSystems(locations)
        characterSolarSystems.flatMap { fromSolarSystem ->
            targetSolarSystems.map { toSolarSystem ->
                fromSolarSystem to toSolarSystem
            }
        }.mapNotNull { (fromSolarSystem, toSolarSystem) ->
            val distance = getSystemDistanceUseCase(fromSolarSystem, toSolarSystem, withJumpBridges = true) ?: return@mapNotNull null
            distance to toSolarSystem
        }.minByOrNull {
            it.first
        }?.let { (distance, toSolarSystem) ->
            solarSystemsRepository.getSystem(toSolarSystem)?.let {
                return it to distance
            }
        }

        targetSolarSystems.firstOrNull()?.let {
            solarSystemsRepository.getSystem(it)?.let {
                return it to null
            }
        }
        return null to null
    }

    private fun getTargetSolarSystems(locations: List<SolarSystemChipLocation>): Set<Int> {
        return locations.flatMap { location ->
            when (location) {
                is SolarSystemChipLocation.SolarSystem -> listOf(location.solarSystemId)
                is SolarSystemChipLocation.Constellation -> solarSystemsRepository.getSystemsInConstellation(location.constellationId)
                is SolarSystemChipLocation.Region -> solarSystemsRepository.getSystemsInRegion(location.regionId)
                is SolarSystemChipLocation.Text -> emptyList()
            }
        }.toSet()
    }
}
