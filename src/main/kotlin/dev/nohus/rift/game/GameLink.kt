package dev.nohus.rift.game

import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.TypesRepository

object GameLink {

    fun forType(type: TypesRepository.Type): String {
        return "<url=showinfo:${type.id}>${type.name}</url>"
    }

    fun forSystem(system: MapSolarSystem): String {
        return "<url=showinfo:5//${system.id}>${system.name}</url>"
    }

    fun forLocation(locationId: Long, locationTypeId: Int, name: String): String {
        return "<url=showinfo:$locationTypeId//$locationId>$name</url>"
    }

    fun forCorporationProject(id: String, name: String): String {
        return "<url=opportunity:corporation_goals:$id>$name</url>"
    }

    fun forFreelanceJob(id: String, name: String): String {
        return "<url=opportunity:freelance_projects:$id>$name</url>"
    }
}
