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
        val corporationId: Int,
        val hasLoyaltyPointStore: Boolean,
    )

    private val stationsBySystemId: Map<Int, List<Station>>
    private val stationById: Map<Int, Station>
    private val loyaltyPointStoresByCorporationId: Map<Int, List<Station>>

    init {
        val rows = staticDatabase.transaction {
            Stations.selectAll().toList()
        }
        val stations = rows.map {
            Station(
                id = it[Stations.id],
                typeId = it[Stations.typeId],
                systemId = it[Stations.systemId],
                name = it[Stations.name],
                corporationId = it[Stations.corporationId],
                hasLoyaltyPointStore = it[Stations.hasLoyaltyPointsStore],
            )
        }

        stationsBySystemId = stations.groupBy { it.systemId }
        stationById = stations.associateBy { it.id }
        loyaltyPointStoresByCorporationId = stations.filter { it.hasLoyaltyPointStore }.groupBy { it.corporationId }
    }

    fun getStation(id: Int): Station? {
        return stationById[id]
    }

    fun getStations(): Map<Int, List<Station>> {
        return stationsBySystemId
    }

    fun getLoyaltyPointStores(corporationId: Int): List<Station> {
        return loyaltyPointStoresByCorporationId[corporationId] ?: emptyList()
    }
}
