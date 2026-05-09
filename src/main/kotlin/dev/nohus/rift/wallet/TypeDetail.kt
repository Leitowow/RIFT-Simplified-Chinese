package dev.nohus.rift.wallet

import dev.nohus.rift.location.LocationRepository
import dev.nohus.rift.repositories.CelestialsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.character.CharacterDetailsRepository

sealed interface TypeDetail {
    data class Structure(val structure: LocationRepository.Structure) : TypeDetail
    data class Station(val station: LocationRepository.Station) : TypeDetail
    data class Character(val character: CharacterDetailsRepository.CharacterDetails) : TypeDetail
    data class Corporation(val corporation: CharacterDetailsRepository.CorporationDetails) : TypeDetail
    data class Alliance(val alliance: CharacterDetailsRepository.AllianceDetails) : TypeDetail
    data class Faction(val id: Long, val name: String) : TypeDetail
    data class SolarSystem(val system: SolarSystemsRepository.MapSolarSystem) : TypeDetail
    data class Type(val type: TypesRepository.Type, val count: Long? = null) : TypeDetail
    data class CorporationProject(val id: String, val name: String) : TypeDetail
    data class FreelanceProject(val id: String, val name: String) : TypeDetail
    data class DailyGoal(val name: String) : TypeDetail
    data class Celestial(val celestial: CelestialsRepository.Celestial) : TypeDetail
    data class Unknown(val id: Long) : TypeDetail
}

fun TypeDetail?.isMatching(search: String): Boolean {
    if (this == null) return false
    val texts: List<String> = when (this) {
        is TypeDetail.Structure -> listOfNotNull(structure.name, structure.owner?.corporationName, structure.owner?.corporationTicker, structure.owner?.allianceName, structure.owner?.allianceTicker)
        is TypeDetail.Station -> listOfNotNull(station.name, station.owner?.corporationName, station.owner?.corporationTicker, station.owner?.allianceName, station.owner?.allianceTicker)
        is TypeDetail.Character -> listOfNotNull(character.name, character.corporationName, character.corporationTicker, character.allianceName, character.allianceTicker)
        is TypeDetail.Corporation -> listOfNotNull(corporation.corporationName, corporation.corporationTicker, corporation.allianceName, corporation.allianceTicker)
        is TypeDetail.Alliance -> listOf(alliance.allianceName, alliance.allianceTicker)
        is TypeDetail.Faction -> listOf(name)
        is TypeDetail.SolarSystem -> listOf(system.name)
        is TypeDetail.Type -> listOf(type.name)
        is TypeDetail.CorporationProject -> listOf(name)
        is TypeDetail.FreelanceProject -> listOf(name)
        is TypeDetail.DailyGoal -> listOf(name)
        is TypeDetail.Celestial -> listOf(celestial.name)
        is TypeDetail.Unknown -> emptyList()
    }
    return texts.any { search in it.lowercase() }
}
