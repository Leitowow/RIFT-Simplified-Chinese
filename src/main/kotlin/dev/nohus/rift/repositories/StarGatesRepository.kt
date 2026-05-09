package dev.nohus.rift.repositories

import dev.nohus.rift.database.static.StarGates
import dev.nohus.rift.database.static.StaticDatabase
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single
class StarGatesRepository(
    staticDatabase: StaticDatabase,
) {

    data class StarGate(
        val typeId: Int?,
        val locationId: Long?,
    )

    // { fromSystem -> { toSystem -> StarGate } }
    private val stargates: Map<Int, Map<Int, StarGate>>
    val connections: List<Pair<Int, Int>>

    init {
        val rows = staticDatabase.transaction {
            StarGates.selectAll().toList()
        }
        stargates = rows
            .groupBy { it[StarGates.fromSystemId] }
            .mapValues { (_, rows) ->
                rows
                    .groupBy { it[StarGates.toSystemId] }
                    .mapValues { (_, rows) ->
                        StarGate(
                            typeId = rows.single()[StarGates.starGateTypeId],
                            locationId = rows.single()[StarGates.locationId],
                        )
                    }
            }
        connections = rows.map {
            it[StarGates.fromSystemId] to it[StarGates.toSystemId]
        }
    }

    /**
     * Returns a list of stargates in this system as pairs of target system ID and stargate
     */
    fun getStargates(fromSystemId: Int): List<Pair<Int, StarGate>> {
        return stargates[fromSystemId]?.map { it.key to it.value } ?: emptyList()
    }

    fun getStargate(fromSystemId: Int, toSystemId: Int): StarGate? {
        return stargates[fromSystemId]?.get(toSystemId)
    }

    /**
     * Returns a stargate or Ansiblex type ID if found, with location ID if available
     */
    fun getGate(
        isAnsiblex: Boolean,
        fromSystemId: Int?,
        toSystemId: Int,
    ): StarGate {
        return if (isAnsiblex) {
            StarGate(35841, null)
        } else {
            val stargate = if (fromSystemId != null) {
                getStargate(fromSystemId, toSystemId)
            } else {
                StarGate(null, null)
            }
            stargate ?: StarGate(16, null) // Default to Caldari system gate
        }
    }
}
