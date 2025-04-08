package dev.nohus.rift.repositories.character

sealed interface CharacterStatus {

    sealed class Exists(
        val characterId: Int,
    ) : CharacterStatus

    /**
     * Has a killboard entry within the last 90 days
     */
    class Active(
        characterId: Int,
    ) : Exists(characterId)

    /**
     * No killboard entries within the last 90 days
     */
    class Inactive(
        characterId: Int,
    ) : Exists(characterId)

    /**
     * Not logged in since 2011
     */
    class Dormant(
        characterId: Int,
    ) : Exists(characterId)

    data object DoesNotExist : CharacterStatus
}
