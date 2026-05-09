package dev.nohus.rift.location

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.location.LocationRepository.Station
import dev.nohus.rift.location.LocationRepository.Structure
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.models.CharacterIdShip
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.sso.scopes.ScopeGroups
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

@Single
class CharacterLocationRepository(
    private val localCharactersRepository: LocalCharactersRepository,
    private val onlineCharactersRepository: OnlineCharactersRepository,
    private val esiApi: EsiApi,
    private val localSystemChangeController: LocalSystemChangeController,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val locationRepository: LocationRepository,
) {

    private val _locations = MutableStateFlow<Map<Int, Location>>(emptyMap())
    val locations = _locations.asStateFlow()

    private val locationExpiry = Duration.ofSeconds(20)
    private val locationExpiryOffline = Duration.ofMinutes(2)

    data class Location(
        val solarSystemId: Int,
        val regionId: Int?,
        val station: Station?,
        val structure: Structure?,
        val ship: CharacterIdShip?,
        val timestamp: Instant,
    )

    suspend fun start() = coroutineScope {
        launch {
            localCharactersRepository.characters.collect {
                loadLocations()
            }
        }
        launch {
            while (true) {
                delay(10.seconds)
                loadLocations()
            }
        }
        launch {
            localSystemChangeController.characterSystemChanges.collect { change ->
                val current = _locations.value[change.characterId]
                if (change.systemId != current?.solarSystemId) {
                    _locations.value += change.characterId to Location(
                        solarSystemId = change.systemId,
                        regionId = solarSystemsRepository.getRegionIdBySystemId(change.systemId),
                        station = null,
                        structure = null,
                        ship = current?.ship,
                        timestamp = change.timestamp,
                    )
                    logger.debug { "Location updated from logs for character ${change.characterId}" }
                }
            }
        }
    }

    fun isUndocked(characterId: Int): Boolean {
        val location = _locations.value[characterId] ?: return false
        return location.station == null && location.structure == null
    }

    private suspend fun loadLocations() {
        val minTime = Instant.now() - locationExpiry
        val minTimeOffline = Instant.now() - locationExpiryOffline
        localCharactersRepository.characters.value
            .filter { character ->
                ScopeGroups.readCharacterLocation in character.scopes
            }
            .filter { character ->
                val id = character.characterId
                val isMissing = id !in _locations.value.keys
                if (isMissing) return@filter true
                val timestamp = _locations.value[id]?.timestamp
                val isOnline = id in onlineCharactersRepository.onlineCharacters.value
                val isExpired = if (isOnline) {
                    timestamp?.isBefore(minTime) == true
                } else {
                    timestamp?.isBefore(minTimeOffline) == true
                }
                isExpired
            }
            .forEach { character ->
                loadLocation(character)
            }
    }

    private suspend fun loadLocation(character: LocalCharacter) {
        val location = esiApi.getCharacterIdLocation(Originator.LocalCharacters, character.characterId).success ?: return
        val ship = if (ScopeGroups.readCurrentShip in character.scopes) {
            esiApi.getCharacterIdShip(Originator.LocalCharacters, character.characterId).success ?: _locations.value[character.characterId]?.ship
        } else {
            null
        }
        val station = locationRepository.getStation(Originator.LocalCharacters, location.stationId)
        val structure = locationRepository.getStructure(Originator.LocalCharacters, location.structureId, character.characterId)
        _locations.value +=
            character.characterId to Location(
                solarSystemId = location.solarSystemId,
                regionId = solarSystemsRepository.getRegionIdBySystemId(location.solarSystemId),
                station = station,
                structure = structure,
                ship = ship,
                timestamp = Instant.now(),
            )
        logger.debug { "Location updated from ESI for character ${character.characterId}" }
    }
}
