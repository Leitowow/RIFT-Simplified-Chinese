package dev.nohus.rift.repositories

import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.location.CharacterLocationRepository
import org.koin.core.annotation.Single

@Single
class GetSystemDistanceFromCharacterUseCase(
    private val getSystemDistanceUseCase: GetSystemDistanceUseCase,
    private val onlineCharactersRepository: OnlineCharactersRepository,
    private val characterLocationRepository: CharacterLocationRepository,
) {

    data class CharacterDistance(
        val characterId: Int,
        val distance: Int,
    )

    /**
     * @param characterId - The character to measure from.
     * If not provided, uses the closest online character, or closest any character.
     */
    operator fun invoke(
        systemId: Int,
        withJumpBridges: Boolean,
        characterId: Int? = null,
    ): CharacterDistance? {
        val characterLocations = characterLocationRepository.locations.value

        if (characterId != null) {
            getClosestDistance(systemId, listOf(characterId), characterLocations, withJumpBridges)?.let { return it }
        } else {
            val onlineCharacters = onlineCharactersRepository.onlineCharacters.value
            getClosestDistance(systemId, onlineCharacters, characterLocations, withJumpBridges)?.let { return it }
        }

        return null
    }

    private fun getClosestDistance(
        systemId: Int,
        characterIds: List<Int>,
        characterLocations: Map<Int, CharacterLocationRepository.Location>,
        withJumpBridges: Boolean,
    ): CharacterDistance? {
        if (characterIds.isEmpty()) return null
        characterIds
            .mapNotNull { characterId ->
                characterId to (characterLocations[characterId]?.solarSystemId ?: return@mapNotNull null)
            }
            .distinct()
            .mapNotNull { (characterId, characterSystemId) ->
                characterId to (getSystemDistanceUseCase(characterSystemId, systemId, withJumpBridges = withJumpBridges) ?: return@mapNotNull null)
            }
            .minByOrNull { (_, distance) ->
                distance
            }
            ?.let { (characterId, distance) ->
                return CharacterDistance(characterId, distance)
            }
        return null
    }
}
