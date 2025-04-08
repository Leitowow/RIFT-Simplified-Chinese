package dev.nohus.rift.map

import dev.nohus.rift.database.static.MapLayout
import dev.nohus.rift.database.static.MapLayouts
import dev.nohus.rift.database.static.RegionMapLayout
import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.repositories.SolarSystemsRepository
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single
import kotlin.math.pow
import kotlin.math.roundToInt

@Single
class MapLayoutRepository(
    staticDatabase: StaticDatabase,
    val solarSystemsRepository: SolarSystemsRepository,
) {

    data class Layout(
        val layoutId: Int,
        val name: String,
        val regionIds: List<Int>,
    )

    data class Position(
        val x: Int,
        val y: Int,
    ) {
        fun distanceSquared(other: Position) = (x - other.x).toDouble().pow(2) + (y - other.y).toDouble().pow(2)
    }

    // layout ID -> system ID -> position
    private val systemPositionsByLayoutId: Map<Int, Map<Int, Position>>

    // layout ID -> layout
    private val layoutsById: Map<Int, Layout>

    // region ID -> layouts
    private val layoutsByRegionId: Map<Int, List<Layout>>

    // region ID -> position
    private val regionPositionsById: Map<Int, Position>

    init {
        layoutsById = staticDatabase.transaction {
            MapLayouts.selectAll().toList()
        }.groupBy { it[MapLayouts.layoutId] }.map { (layoutId, rows) ->
            val regionIds = rows.map { it[MapLayouts.regionId] }
            layoutId to Layout(
                layoutId = layoutId,
                name = rows.first()[MapLayouts.name],
                regionIds = regionIds,
            )
        }.toMap()

        layoutsByRegionId = layoutsById.values.flatMap { layout ->
            layout.regionIds.map { it to layout }
        }.groupBy { it.first }.mapValues { it.value.map { it.second } }

        systemPositionsByLayoutId = staticDatabase.transaction {
            MapLayout.selectAll().toList()
        }.groupBy { it[MapLayout.layoutId] }.mapValues { (_, rows) ->
            rows.associate {
                it[MapLayout.solarSystemId] to Position(it[MapLayout.x], it[MapLayout.y])
            }
        }
        val regionRows = staticDatabase.transaction {
            RegionMapLayout.selectAll().toList()
        }
        regionPositionsById = regionRows.associate {
            it[RegionMapLayout.regionId] to Position(it[RegionMapLayout.x], it[RegionMapLayout.y])
        }
    }

    fun getLayouts(regionId: Int): List<Layout> {
        return layoutsByRegionId[regionId] ?: emptyList()
    }

    fun getLayout(layoutId: Int): Layout? {
        return layoutsById[layoutId]
    }

    fun getLayoutSystemPositions(layoutId: Int): Map<Int, Position>? {
        return systemPositionsByLayoutId[layoutId]
    }

    fun getNewEdenSystemPosition(): Map<Int, Position> {
        return solarSystemsRepository.getSystems(knownSpace = true).associate { system ->
            system.id to transformNewEdenCoordinate(system.x, system.z)
        }
    }

    fun getRegionsPositions(): Map<Int, Position> {
        return regionPositionsById
    }

    companion object {
        /**
         * Maps in-game system coordinates to map layout coordinates with reasonable values
         */
        fun transformNewEdenCoordinate(x: Double, z: Double): Position {
            val scale = 100_000_000_000_000
            val shiftX = 5087
            val shiftY = 4729
            return Position((x / scale).roundToInt() + shiftX, (z / scale).roundToInt() + shiftY)
        }
    }
}
