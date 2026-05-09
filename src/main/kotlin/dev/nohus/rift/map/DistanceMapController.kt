package dev.nohus.rift.map

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.location.CharacterLocationRepository
import dev.nohus.rift.map.MapLayoutRepository.Position
import dev.nohus.rift.repositories.GetSystemsInRangeBandsUseCase
import dev.nohus.rift.repositories.SolarSystemsRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

@Factory
class DistanceMapController(
    private val getSystemsInRangeBandsUseCase: GetSystemsInRangeBandsUseCase,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val localCharactersRepository: LocalCharactersRepository,
    private val characterLocationRepository: CharacterLocationRepository,
    private val onlineCharactersRepository: OnlineCharactersRepository,
) {

    data class DistanceMapState(
        val centerSystemId: Int,
        val centerSystemName: String? = null,
        val followingCharacterId: Int? = null,
        val followingCharacterName: String? = null,
        val isEditedCenterValid: Boolean = true,
        val distance: Int,
        val layout: Map<Int, Position> = emptyMap(),
    )

    private val _state = MutableStateFlow(getInitialMapState())
    val state = _state.asStateFlow()

    private fun getInitialMapState(): DistanceMapState {
        val characters = localCharactersRepository.characters.value
        val locations = characterLocationRepository.locations.value
        val onlineCharacterIds = onlineCharactersRepository.onlineCharacters.value

        val onlineCharacterWithLocation = characters.firstOrNull { character ->
            character.characterId in onlineCharacterIds &&
                locations[character.characterId]?.solarSystemId != null
        }

        val firstCharacterWithLocation = onlineCharacterWithLocation ?: characters.firstOrNull { character ->
            locations[character.characterId]?.solarSystemId != null
        }

        val systemId = firstCharacterWithLocation?.let { character ->
            locations[character.characterId]?.solarSystemId
        } ?: solarSystemsRepository.getSystemId("Central Point")!! // Fallback if no character location found

        return DistanceMapState(
            centerSystemId = systemId,
            centerSystemName = solarSystemsRepository.getSystemName(systemId),
            followingCharacterId = firstCharacterWithLocation?.characterId,
            followingCharacterName = firstCharacterWithLocation?.info?.name,
            distance = 3,
            layout = getLayout(systemId, 3),
        )
    }

    suspend fun start() = coroutineScope {
        launch {
            localCharactersRepository.characters.collect { characters ->
                val name = characters.find { it.characterId == _state.value.followingCharacterId }?.info?.name
                _state.update { it.copy(followingCharacterName = name) }
            }
        }
        launch {
            characterLocationRepository.locations.collect {
                val systemId = it[_state.value.followingCharacterId]?.solarSystemId
                if (systemId != null) setCenterSystem(systemId)
            }
        }
    }

    fun setSettings(centerSystemId: Int, followingCharacterId: Int?, distance: Int) {
        val name = localCharactersRepository.characters.value.firstOrNull { it.characterId == followingCharacterId }?.info?.name
        _state.update {
            it.copy(
                centerSystemId = centerSystemId,
                centerSystemName = solarSystemsRepository.getSystemName(centerSystemId),
                followingCharacterId = followingCharacterId,
                followingCharacterName = name ?: followingCharacterId?.toString(),
                distance = distance,
                layout = getLayout(centerSystemId, distance),
            )
        }
    }

    /**
     * From user input, which can be a character name or system name
     */
    fun setCenterTarget(target: String) {
        val characterId = localCharactersRepository.characters.value.firstOrNull { it.info?.name == target }?.characterId
        val systemId = solarSystemsRepository.getSystemId(target)
        if (characterId != null) {
            setFollowingCharacter(characterId)
            _state.update { it.copy(isEditedCenterValid = true) }
        } else if (systemId != null) {
            setCenterSystem(systemId)
            _state.update { it.copy(isEditedCenterValid = true, followingCharacterId = null, followingCharacterName = null) }
        } else {
            _state.update { it.copy(isEditedCenterValid = false) }
        }
    }

    fun setCenterSystem(systemId: Int) {
        if (systemId == _state.value.centerSystemId) return
        _state.update {
            it.copy(
                centerSystemId = systemId,
                centerSystemName = solarSystemsRepository.getSystemName(systemId),
                layout = getLayout(systemId, _state.value.distance),
            )
        }
    }

    fun setFollowingCharacter(characterId: Int?) {
        if (characterId != null) {
            val name = localCharactersRepository.characters.value.firstOrNull { it.characterId == characterId }?.info?.name
            _state.update {
                it.copy(
                    followingCharacterId = characterId,
                    followingCharacterName = name,
                )
            }
            val systemId = characterLocationRepository.locations.value[characterId]?.solarSystemId
            if (systemId != null) setCenterSystem(systemId)
        } else {
            _state.update {
                it.copy(
                    followingCharacterId = null,
                    followingCharacterName = null,
                )
            }
        }
    }

    fun setDistance(distance: Int) {
        _state.update {
            it.copy(
                distance = distance,
                layout = getLayout(_state.value.centerSystemId, distance),
            )
        }
    }

    private fun getLayout(centerSystemId: Int, distance: Int): Map<Int, Position> {
        val result = getSystemsInRangeBandsUseCase(centerSystemId, distance)
        return result.flatMapIndexed { distance, systems ->
            val startX = -((systems.size - 1) * SYSTEM_DISTANCE) / 2
            systems.mapIndexed { index, system ->
                system to Position(startX + index * SYSTEM_DISTANCE, distance * SYSTEM_DISTANCE)
            }
        }.toMap()
    }

    companion object {
        const val SYSTEM_DISTANCE = 65
    }
}
