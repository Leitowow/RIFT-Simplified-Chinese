package dev.nohus.rift.clones

import dev.nohus.rift.location.LocationRepository.Station
import dev.nohus.rift.location.LocationRepository.Structure
import dev.nohus.rift.repositories.TypesRepository.Type

data class Clone(
    val id: Int,
    val characterId: Int,
    val implants: List<Type>,
    val solarSystemId: Int?,
    val station: Station?,
    val structure: Structure?,
    val isActive: Boolean,
)
