package dev.nohus.rift.repositories

import dev.nohus.rift.database.static.Constellations
import dev.nohus.rift.database.static.Regions
import dev.nohus.rift.database.static.SolarSystems
import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.utils.roundSecurity
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single
class SolarSystemsRepository(
    staticDatabase: StaticDatabase,
) {

    private val systemNames: Set<String>
    private val lowercaseSystemNames: Map<String, String>
    private val shortened4: Map<String, List<String>>
    private val shortened3: Map<String, List<String>>
    private val shortened2: Map<String, List<String>>
    private val systemIdsByName: Map<String, Int>
    private val systemNamesById: Map<Int, String>
    private val systemsById: Map<Int, MapSolarSystem>
    private val mapSolarSystems: List<MapSolarSystem>
    val mapConstellations: List<MapConstellation>
    val mapRegions: List<MapRegion>
    private val mapSystemConstellation: Map<Int, Int> // System ID -> Constellation ID
    private val mapConstellationSystems: Map<Int, List<Int>> // Constellation ID -> System IDs
    private val mapRegionSystems: Map<Int, List<Int>> // Region ID -> System IDs
    private val regionsById: Map<Int, MapRegion> // Region ID -> Region
    private val regionsBySystemName: Map<String, MapRegion> // System name -> Region
    private val regionIdBySystemId: Map<Int, Int> // System ID -> Region ID
    private val regionIdsByName: Map<String, Int> // Region name -> Region ID
    private val constellationsById: Map<Int, MapConstellation> // Constellation ID -> Constellation

    data class MapSolarSystem(
        val id: Int,
        val name: String,
        val abyssalName: String?,
        val constellationId: Int,
        val regionId: Int,
        val x: Double,
        val y: Double,
        val z: Double,
        val x2d: Double?,
        val y2d: Double?,
        val security: Double,
        val sunTypeId: Int,
        val hasJoveObservatory: Boolean,
        val asteroidBeltCount: Int,
        val iceFieldCount: Int,
    )

    data class MapConstellation(
        val id: Int,
        val name: String,
        val regionId: Int,
        val x: Double,
        val y: Double,
        val z: Double,
    )

    data class MapRegion(
        val id: Int,
        val name: String,
        val x: Double,
        val y: Double,
        val z: Double,
        val x2d: Double?,
        val y2d: Double?,
    )

    init {
        val systemRows = staticDatabase.transaction {
            SolarSystems.selectAll().toList()
        }
        val constellationRows = staticDatabase.transaction {
            Constellations.selectAll().toList()
        }
        val regionRows = staticDatabase.transaction {
            Regions.selectAll().toList()
        }
        systemNames = systemRows.map { it[SolarSystems.solarSystemName] }.toSet()
        systemIdsByName = systemRows.associate { it[SolarSystems.solarSystemName] to it[SolarSystems.solarSystemId] }
        systemNamesById = systemIdsByName.map { (name, id) -> id to name }.toMap()
        lowercaseSystemNames = systemNames.associateBy { it.lowercase() }
        val codeNames = systemNames.filter { "-" in it }
        shortened4 = codeNames.groupBy { it.take(4).lowercase() }
        shortened3 = codeNames.groupBy { it.take(3).lowercase() }
            .filterKeys { it !in listOf("frt", "ooo") }
        shortened2 = codeNames.groupBy { it.take(2).lowercase() }
            .filterKeys { it.count { it.isDigit() } == 1 } // We only allow 2 character abbreviations if they include a digit
            .filterKeys { it !in listOf("t1", "t2") } // No tech level names
        mapSolarSystems = systemRows.map {
            MapSolarSystem(
                id = it[SolarSystems.solarSystemId],
                name = it[SolarSystems.solarSystemName],
                abyssalName = AbyssalSystemNames[it[SolarSystems.solarSystemName]],
                constellationId = it[SolarSystems.constellationId],
                regionId = it[SolarSystems.regionId],
                x = it[SolarSystems.x],
                y = it[SolarSystems.y],
                z = -it[SolarSystems.z],
                x2d = it[SolarSystems.x2d],
                y2d = it[SolarSystems.y2d],
                security = it[SolarSystems.security],
                sunTypeId = it[SolarSystems.sunTypeId],
                hasJoveObservatory = it[SolarSystems.hasJoveObservatory],
                asteroidBeltCount = it[SolarSystems.asteroidBeltCount],
                iceFieldCount = it[SolarSystems.iceFieldCount],
            )
        }
        systemsById = mapSolarSystems.associateBy { it.id }
        mapConstellations = constellationRows.map {
            MapConstellation(
                id = it[Constellations.constellationId],
                name = it[Constellations.constellationName],
                regionId = it[Constellations.regionId],
                x = it[Constellations.x],
                y = it[Constellations.y],
                z = -it[Constellations.z],
            )
        }
        mapRegions = regionRows.map {
            MapRegion(
                id = it[Regions.regionId],
                name = it[Regions.regionName],
                x = it[Regions.x],
                y = it[Regions.y],
                z = -it[Regions.z],
                x2d = it[Regions.x2d],
                y2d = it[Regions.y2d],
            )
        }
        mapSystemConstellation = systemRows.associate {
            it[SolarSystems.solarSystemId] to it[SolarSystems.constellationId]
        }
        mapConstellationSystems = systemRows.groupBy {
            it[SolarSystems.constellationId]
        }.entries.associate { (constellationId, rows) ->
            constellationId to rows.map { it[SolarSystems.solarSystemId] }
        }
        mapRegionSystems = systemRows.groupBy {
            it[SolarSystems.regionId]
        }.entries.associate { (regionId, rows) ->
            regionId to rows.map { it[SolarSystems.solarSystemId] }
        }
        regionsById = mapRegions.associateBy { it.id }
        regionsBySystemName = mapSolarSystems.associate { it.name to regionsById[it.regionId]!! }
        regionIdBySystemId = mapSolarSystems.associate { it.id to it.regionId }
        regionIdsByName = mapRegions.associate { it.name to it.id }
        constellationsById = mapConstellations.associateBy { it.id }
    }

    fun getSystems(knownSpace: Boolean = true): List<MapSolarSystem> {
        return if (knownSpace) {
            mapSolarSystems.filter { it.regionId <= 10001004 } // K-space
        } else {
            mapSolarSystems
        }
    }

    /**
     * @param name Potential system name
     * @param regionsHint Regions the system is expected to be in, to prioritise ambiguous names
     * @param systemHints System IDs to prioritise for ambiguous names
     * @return System or null if it's not a system name
     */
    fun getFuzzySystem(
        name: String,
        regionsHint: List<String>,
        systemHints: List<Int> = emptyList(),
    ): MapSolarSystem? {
        getSystemWithoutTypos(name, regionsHint, systemHints)?.let { return getSystem(it) }
        if ('0' in name) getSystemWithoutTypos(name.replace('0', 'O'), regionsHint, systemHints)?.let { return getSystem(it) }
        if ('O' in name) getSystemWithoutTypos(name.replace('O', '0'), regionsHint, systemHints)?.let { return getSystem(it) }
        return null
    }

    private fun getSystemWithoutTypos(
        name: String,
        regionsHint: List<String>,
        systemHints: List<Int>,
    ): String? {
        if (name in systemNames) return name
        lowercaseSystemNames[name.lowercase()]?.let { return it }
        val candidates = when (name.length) {
            4 -> shortened4[name.lowercase()] ?: emptyList()
            3 -> shortened3[name.lowercase()] ?: emptyList()
            2 -> shortened2[name.lowercase()] ?: emptyList()
            else -> emptyList()
        }
        return if (candidates.size > 1) {
            candidates.singleOrNull { candidate ->
                getRegionBySystem(candidate)?.name in regionsHint
            } ?: candidates.singleOrNull { candidate ->
                val systemId = systemIdsByName[candidate]
                systemId in systemHints
            }
        } else {
            candidates.firstOrNull()
        }
    }

    fun getSystemId(name: String): Int? {
        return systemIdsByName[name]
    }

    fun getSystemName(id: Int): String? {
        return systemNamesById[id]
    }

    fun getSystem(name: String): MapSolarSystem? {
        val id = getSystemId(name) ?: return null
        return systemsById[id]
    }

    fun getSystem(id: Int): MapSolarSystem? {
        return systemsById[id]
    }

    fun getSystemsInConstellation(constellationId: Int): List<Int> {
        return mapConstellationSystems[constellationId] ?: emptyList()
    }

    fun getSystemsInRegion(regionId: Int): List<Int> {
        return mapRegionSystems[regionId] ?: emptyList()
    }

    fun getSystemSecurity(id: Int): Double? {
        return mapSolarSystems.firstOrNull { it.id == id }?.security
    }

    fun getRegion(regionId: Int): MapRegion? {
        return regionsById[regionId]
    }

    fun getKnownSpaceRegions(): List<MapRegion> {
        return mapRegions.filter { it.id in 10000001..10001000 }
    }

    fun getWormholeSpaceRegions(): List<MapRegion> {
        return mapRegions.filter { it.id in 11000001..11000033 }
    }

    fun getAbyssalSpaceRegions(): List<MapRegion> {
        return mapRegions.filter { it.id in 12000001..14000005 }
    }

    fun getRegionBySystem(systemName: String): MapRegion? {
        return regionsBySystemName[systemName]
    }

    fun getRegionIdBySystemId(systemId: Int): Int? {
        return regionIdBySystemId[systemId]
    }

    fun getRegionId(name: String): Int? {
        return regionIdsByName[name]
    }

    fun getRegionBySystemId(systemId: Int): MapRegion? {
        return systemsById[systemId]?.regionId?.let { regionsById[it] }
    }

    fun getConstellation(constellationId: Int): MapConstellation? {
        return constellationsById[constellationId]
    }

    fun getConstellationBySystemId(systemId: Int): MapConstellation? {
        return systemsById[systemId]?.constellationId?.let { constellationsById[it] }
    }

    fun getSystems() = mapSolarSystems

    fun isKnownSpace(systemId: Int): Boolean {
        return getSystem(systemId)?.regionId?.let { it in 10000001..10001004 } == true
    }

    fun isWormholeSpace(systemId: Int): Boolean {
        return getSystem(systemId)?.regionId?.let { it in 11000001..11000033 } == true
    }

    /**
     * Non-NPC Null-Sec systems
     */
    fun getSovSystems(): List<MapSolarSystem> {
        val npcNullRegions = listOf("Curse", "Great Wildlands", "Outer Ring", "Stain", "Syndicate", "Venal", "Yasna Zakh")
        val npcNullConstellations = listOf("38G6-L", "N-K4Q0", "Phoenix", "U-7RBK", "XPJ1-6", "6-UCYU")
        val npcNullRegionIds = mapRegions.filter { it.name in npcNullRegions }.map { it.id }
        val npcNullConstellationIds = mapConstellations.filter { it.name in npcNullConstellations }.map { it.id }
        return mapSolarSystems
            .filter { system ->
                system.regionId !in npcNullRegionIds &&
                    system.regionId < 11000000 && // K-space regions
                    system.constellationId !in npcNullConstellationIds
            }
            .filter { system ->
                system.security.roundSecurity() <= 0.0
            }
    }
}
