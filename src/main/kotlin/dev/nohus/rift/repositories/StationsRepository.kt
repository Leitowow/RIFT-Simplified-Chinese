package dev.nohus.rift.repositories

import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.database.static.Stations
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single
class StationsRepository(
    staticDatabase: StaticDatabase,
) {

    data class Station(
        val id: Int,
        val typeId: Int,
        val systemId: Int,
        val name: String,
    )

    private val stations: Map<Int, List<Station>>
    private val stationById: Map<Int, Station>

    init {
        val rows = staticDatabase.transaction {
            Stations.selectAll().toList()
        }
        stations = rows.groupBy {
            it[Stations.systemId]
        }.map { (systemId, stations) ->
            systemId to stations.map {
                Station(
                    id = it[Stations.id],
                    typeId = it[Stations.typeId],
                    systemId = it[Stations.systemId],
                    name = it[Stations.name],
                )
            }
        }.toMap()
        stationById = rows.associate {
            it[Stations.id] to Station(
                id = it[Stations.id],
                typeId = it[Stations.typeId],
                systemId = it[Stations.systemId],
                name = it[Stations.name],
            )
        }
    }

    fun getStation(id: Int): Station? {
        return stationById[id]
    }

    fun getStations(): Map<Int, List<Station>> {
        return stations
    }
}
