package dev.nohus.rift.intel.state

import dev.nohus.rift.repositories.CelestialsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.repositories.character.CharacterDetailsRepository.CharacterDetails
import dev.nohus.rift.standings.Standing

/**
 * An entity or event present in a system
 */
sealed interface SystemEntity {
    data class Character(
        val name: String,
        val characterId: Int,
        val details: CharacterDetails,
    ) : SystemEntity,
        CharacterBound

    data class UnspecifiedCharacter(
        val count: Int,
    ) : SystemEntity,
        CharacterBound

    data class Ship(
        val type: Type,
        val count: Int,
        val standing: Standing? = null,
    ) : SystemEntity,
        CharacterBound

    data class Gate(
        val system2: MapSolarSystem,
        val isAnsiblex: Boolean,
        val distanceKm: Int? = null,
    ) : SystemEntity,
        CharacterBound

    data class Celestial(
        val celestial: CelestialsRepository.Celestial,
        val distanceKm: Int,
    ) : SystemEntity,
        CharacterBound

    data class Killmail(
        val url: String,
        val ship: Type?,
        val typeName: String?,
        val victim: KillmailVictim,
    ) : SystemEntity,
        Clearable

    data class KillmailVictim(
        val characterId: Int?,
        val details: CharacterDetails?,
        val corporationId: Int?,
        val corporationName: String?,
        val corporationTicker: String?,
        val allianceId: Int?,
        val allianceName: String?,
        val allianceTicker: String?,
        val standing: Standing?,
    )

    data object Wormhole : SystemEntity
    data object Spike : SystemEntity, Clearable
    data object Ess : SystemEntity
    data object Skyhook : SystemEntity
    data object GateCamp : SystemEntity, Clearable
    data object CombatProbes : SystemEntity
    data object NoVisual : SystemEntity, CharacterBound
    data object Bubbles : SystemEntity
}

// Marker for system entities that go away if the character situation changes
interface CharacterBound :
    SystemEntity,
    Clearable

// Marker for entities that go away when a system is reported clear
interface Clearable : SystemEntity
