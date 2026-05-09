package dev.nohus.rift.repositories

import dev.nohus.rift.assets.AssetsRepository
import dev.nohus.rift.clones.ClonesRepository
import dev.nohus.rift.map.MapJumpRangeController
import dev.nohus.rift.map.MapJumpRangeController.SystemDistance
import dev.nohus.rift.map.MapPlanetsController
import dev.nohus.rift.map.markers.MapMarkersRepository
import dev.nohus.rift.map.markers.MapMarkersRepository.MapMarker
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.models.FactionWarfareSystem
import dev.nohus.rift.network.esi.models.Incursion
import dev.nohus.rift.network.esi.models.IndustryActivity
import dev.nohus.rift.network.esi.models.SovereigntySystem
import dev.nohus.rift.network.evescout.GetMetaliminalStormsUseCase
import dev.nohus.rift.network.evescout.GetMetaliminalStormsUseCase.Storm
import dev.nohus.rift.network.evescout.GetPublicWormholesUseCase
import dev.nohus.rift.network.evescout.GetPublicWormholesUseCase.Wormhole
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository
import dev.nohus.rift.repositories.PlanetsRepository.Planet
import dev.nohus.rift.repositories.RatsRepository.RatType
import dev.nohus.rift.repositories.StationsRepository.Station
import dev.nohus.rift.sovupgrades.MapSovereigntyUpgradesController
import dev.nohus.rift.sovupgrades.MapSovereigntyUpgradesController.SovereigntyUpgrade
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.minutes

@Single
class MapStatusRepository(
    private val esiApi: EsiApi,
    private val assetsRepository: AssetsRepository,
    private val stationsRepository: StationsRepository,
    private val namesRepository: NamesRepository,
    private val getMetaliminalStormsUseCase: GetMetaliminalStormsUseCase,
    private val getPublicWormholesUseCase: GetPublicWormholesUseCase,
    private val mapJumpRangeController: MapJumpRangeController,
    private val mapPlanetsController: MapPlanetsController,
    private val mapSovereigntyUpgradesController: MapSovereigntyUpgradesController,
    private val planetaryIndustryRepository: PlanetaryIndustryRepository,
    private val clonesRepository: ClonesRepository,
    private val ratsRepository: RatsRepository,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val mapMarkersRepository: MapMarkersRepository,
) {

    private data class UniverseSystemStatus(
        val shipJumps: Int?,
        val npcKills: Int?,
        val podKills: Int?,
        val shipKills: Int?,
    )

    data class SolarSystemStatus(
        val regionName: String?,
        val constellationName: String?,
        val shipJumps: Int?,
        val npcKills: Int?,
        val podKills: Int?,
        val shipKills: Int?,
        val assetCount: Int?,
        val incursion: Incursion?,
        val factionWarfare: FactionWarfareSystem?,
        val sovereignty: SovereigntySystem?,
        val sovereigntyUpgrades: List<SovereigntyUpgrade>,
        val stations: List<Station>,
        val storms: List<Storm>,
        val wormholes: List<Wormhole>,
        val industryIndices: Map<IndustryActivity, Float>,
        val distance: SystemDistance?,
        val planets: List<Planet>,
        val colonies: Int,
        val clones: Map<Int, Int>, // Character ID -> Count
        val ratType: RatType?,
        val markers: List<MapMarker>,
    )

    private val universeSystemStatus = MutableStateFlow<Map<Int, UniverseSystemStatus>>(emptyMap())
    private val incursions = MutableStateFlow<Map<Int, Incursion>>(emptyMap())
    private val factionWarfare = MutableStateFlow<Map<Int, FactionWarfareSystem>>(emptyMap())
    private val sovereignty = MutableStateFlow<Map<Int, SovereigntySystem>>(emptyMap())
    private val storms = MutableStateFlow<Map<Int, List<Storm>>>(emptyMap())
    private val wormholes = MutableStateFlow<Map<Int, List<Wormhole>>>(emptyMap())
    private val industryIndices = MutableStateFlow<Map<Int, Map<IndustryActivity, Float>>>(emptyMap())
    private val _status = MutableStateFlow<Map<Int, SolarSystemStatus>>(emptyMap())
    val status = _status.asStateFlow()

    suspend fun start() = coroutineScope {
        launch {
            while (true) {
                load()
                delay(5.minutes)
            }
        }
        launch {
            combine(
                universeSystemStatus,
                incursions,
                factionWarfare,
                sovereignty,
                mapSovereigntyUpgradesController.state,
                storms,
                wormholes,
                industryIndices,
                assetsRepository.state,
                mapJumpRangeController.state.map { it.systemDistances },
                mapPlanetsController.state,
                planetaryIndustryRepository.colonies,
                clonesRepository.clones,
                mapMarkersRepository.markers,
            ) { universe, incursions, factionWarfare, sovereignty, sovereigntyUpgrades, storms, wormholes, industryIndices, assets, distances, planets, colonies, clones, markers ->
                val assetsPerSystem = getAssetCountPerSystem(assets)
                val stationsPerSystem = stationsRepository.getStations()
                val systems = (
                    universe.keys + incursions.keys + factionWarfare.keys + sovereignty.keys + storms.keys +
                        industryIndices.keys + assetsPerSystem.keys + stationsPerSystem.keys + distances.keys
                    ).distinct()
                val clones = clones.entries.flatMap { (characterId, clones) ->
                    clones.mapNotNull { clone ->
                        val system = clone.structure?.solarSystemId ?: clone.station?.solarSystemId ?: return@mapNotNull null
                        system to characterId
                    }
                }.groupBy { it.first }.map { (system, clones) ->
                    system to clones.groupBy { it.second }.map { (characterId, clones) ->
                        characterId to clones.size
                    }.toMap()
                }.toMap()
                systems.associateWith { systemId ->
                    SolarSystemStatus(
                        regionName = solarSystemsRepository.getRegionBySystemId(systemId)?.name,
                        constellationName = solarSystemsRepository.getConstellationBySystemId(systemId)?.name,
                        shipJumps = universe[systemId]?.shipJumps,
                        npcKills = universe[systemId]?.npcKills,
                        podKills = universe[systemId]?.podKills,
                        shipKills = universe[systemId]?.shipKills,
                        assetCount = assetsPerSystem[systemId] ?: 0,
                        incursion = incursions[systemId],
                        factionWarfare = factionWarfare[systemId],
                        sovereignty = sovereignty[systemId],
                        sovereigntyUpgrades = filterSovereigntyUpgrades(sovereigntyUpgrades.upgrades[systemId] ?: emptyList()),
                        stations = stationsPerSystem[systemId] ?: emptyList(),
                        storms = storms[systemId] ?: emptyList(),
                        wormholes = wormholes[systemId] ?: emptyList(),
                        industryIndices = industryIndices[systemId] ?: emptyMap(),
                        distance = distances[systemId],
                        planets = (planets.planets[systemId] ?: emptyList()).filter { it.type in planets.selectedTypes },
                        colonies = colonies.success?.count { it.value.colony.system.id == systemId } ?: 0,
                        clones = clones[systemId] ?: emptyMap(),
                        ratType = ratsRepository.getRats(systemId),
                        markers = markers[systemId] ?: emptyList(),
                    )
                }
            }.collect {
                _status.value = it
            }
        }
    }

    private suspend fun load() {
        coroutineScope {
            launch {
                loadUniverseSystemStatus()
            }
            launch {
                loadIncursions()
            }
            launch {
                loadFactionWarfare()
            }
            launch {
                loadSovereignty()
            }
            launch {
                loadMetaliminalStorms()
            }
            launch {
                loadPublicWormholes()
            }
            launch {
                loadIndustryIndices()
            }
        }
    }

    private fun getAssetCountPerSystem(state: AssetsRepository.State): Map<Int, Int> {
        val assets = state.loadedState?.success?.assets ?: emptyList()
        return assets.mapNotNull {
            when (val location = it.location) {
                is AssetsRepository.AssetLocation.Station -> location.systemId
                is AssetsRepository.AssetLocation.Structure -> location.systemId
                is AssetsRepository.AssetLocation.System -> location.systemId
                is AssetsRepository.AssetLocation.AssetSafety -> null
                is AssetsRepository.AssetLocation.CustomsOffice -> null
                is AssetsRepository.AssetLocation.Unknown -> null
            }
        }.groupBy { it }.map { (systemId, assets) -> systemId to assets.size }.toMap()
    }

    private fun filterSovereigntyUpgrades(upgrades: List<SovereigntyUpgrade>): List<SovereigntyUpgrade> {
        val selectedTypes = mapSovereigntyUpgradesController.selectedTypes
        return if (selectedTypes.isEmpty()) {
            upgrades
        } else {
            upgrades.filter { it.type in selectedTypes }
        }
    }

    private suspend fun loadUniverseSystemStatus() {
        val jumps = esiApi.getUniverseSystemJumps(Originator.Map).success?.associateBy { it.systemId } ?: return
        val kills = esiApi.getUniverseSystemKills(Originator.Map).success?.associateBy { it.systemId } ?: return
        val systems = (jumps.keys + kills.keys).distinct()
        universeSystemStatus.value = systems.associateWith { systemId ->
            UniverseSystemStatus(
                shipJumps = jumps[systemId]?.shipJumps,
                npcKills = kills[systemId]?.npcKills,
                podKills = kills[systemId]?.podKills,
                shipKills = kills[systemId]?.shipKills,
            )
        }
    }

    private suspend fun loadIncursions() {
        val response = esiApi.getIncursions(Originator.Map).success ?: return
        val systems = response.flatMap { it.infestedSolarSystems }.distinct()
        incursions.value = systems.associateWith { systemId ->
            response.first { systemId in it.infestedSolarSystems }
        }
    }

    private suspend fun loadFactionWarfare() {
        val response = esiApi.getFactionWarfareSystems(Originator.Map).success ?: return
        factionWarfare.value = response
            .also {
                val ids = it.flatMap { listOf(it.ownerFactionId, it.occupierFactionId) }
                namesRepository.resolveNames(Originator.Map, ids)
            }
            .associateBy { it.solarSystemId }
    }

    private suspend fun loadSovereignty() {
        val response = esiApi.getSovereigntyMap(Originator.Map).success ?: return
        sovereignty.value = response
            .filter { it.factionId != null || it.allianceId != null || it.corporationId != null }
            .also {
                val ids = it.flatMap { listOfNotNull(it.factionId, it.allianceId, it.corporationId) }
                namesRepository.resolveNames(Originator.Map, ids)
            }
            .associateBy { it.systemId }
    }

    private suspend fun loadMetaliminalStorms() {
        storms.value = getMetaliminalStormsUseCase(Originator.Map)
    }

    private suspend fun loadPublicWormholes() {
        wormholes.value = getPublicWormholesUseCase(Originator.Map)
    }

    private suspend fun loadIndustryIndices() {
        val response = esiApi.getIndustrySystems(Originator.Map).success ?: return
        industryIndices.value = response.associate { industrySystem ->
            val indices = industrySystem.indices.associate { index -> index.activity to index.costIndex }
            industrySystem.solarSystemId to indices
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, R> combine(
        flow: Flow<T1>,
        flow2: Flow<T2>,
        flow3: Flow<T3>,
        flow4: Flow<T4>,
        flow5: Flow<T5>,
        flow6: Flow<T6>,
        flow7: Flow<T7>,
        flow8: Flow<T8>,
        flow9: Flow<T9>,
        flow10: Flow<T10>,
        flow11: Flow<T11>,
        flow12: Flow<T12>,
        flow13: Flow<T13>,
        flow14: Flow<T14>,
        transform: suspend (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14) -> R,
    ): Flow<R> = combine(flow, flow2, flow3, flow4, flow5, flow6, flow7, flow8, flow9, flow10, flow11, flow12, flow13, flow14) { args: Array<*> ->
        transform(
            args[0] as T1,
            args[1] as T2,
            args[2] as T3,
            args[3] as T4,
            args[4] as T5,
            args[5] as T6,
            args[6] as T7,
            args[7] as T8,
            args[8] as T9,
            args[9] as T10,
            args[10] as T11,
            args[11] as T12,
            args[12] as T13,
            args[13] as T14,
        )
    }
}
