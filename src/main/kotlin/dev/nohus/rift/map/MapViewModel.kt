package dev.nohus.rift.map

import androidx.compose.ui.geometry.Offset
import dev.nohus.rift.DataEvent
import dev.nohus.rift.Event
import dev.nohus.rift.ViewModel
import dev.nohus.rift.compose.Tab
import dev.nohus.rift.game.AutopilotController
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.map_constellation
import dev.nohus.rift.generated.resources.map_region
import dev.nohus.rift.generated.resources.map_universe
import dev.nohus.rift.get
import dev.nohus.rift.intel.state.IntelStateController
import dev.nohus.rift.intel.state.SystemEntity
import dev.nohus.rift.location.GetOnlineCharactersLocationUseCase
import dev.nohus.rift.location.GetOnlineCharactersLocationUseCase.OnlineCharacterLocation
import dev.nohus.rift.map.DistanceMapController.DistanceMapState
import dev.nohus.rift.map.MapExternalControl.MapExternalControlEvent
import dev.nohus.rift.map.MapJumpRangeController.MapJumpRangeState
import dev.nohus.rift.map.MapLayoutRepository.Layout
import dev.nohus.rift.map.MapLayoutRepository.Position
import dev.nohus.rift.map.MapPlanetsController.MapPlanetsState
import dev.nohus.rift.map.MapViewModel.MapType.ClusterRegionsMap
import dev.nohus.rift.map.MapViewModel.MapType.ClusterSystemsMap
import dev.nohus.rift.map.MapViewModel.MapType.DistanceMap
import dev.nohus.rift.map.MapViewModel.MapType.RegionMap
import dev.nohus.rift.repositories.JumpBridgesRepository
import dev.nohus.rift.repositories.JumpBridgesRepository.JumpBridgeConnection
import dev.nohus.rift.repositories.MapGateConnectionsRepository
import dev.nohus.rift.repositories.MapGateConnectionsRepository.GateConnection
import dev.nohus.rift.repositories.MapStatusRepository
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus
import dev.nohus.rift.repositories.PlanetTypes.PlanetType
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapConstellation
import dev.nohus.rift.repositories.SolarSystemsRepository.MapRegion
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.settings.persistence.IntelMap
import dev.nohus.rift.settings.persistence.MapOpenedTab
import dev.nohus.rift.settings.persistence.MapSystemInfoType
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.sovupgrades.MapSovereigntyUpgradesController
import dev.nohus.rift.sovupgrades.MapSovereigntyUpgradesController.MapSovereigntyUpgradesState
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import dev.nohus.rift.settings.persistence.MapType as SettingsMapType

@Factory
class MapViewModel(
    @InjectedParam private val windowUuid: UUID,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val gateConnectionsRepository: MapGateConnectionsRepository,
    private val layoutRepository: MapLayoutRepository,
    private val distanceMapController: DistanceMapController,
    private val getOnlineCharactersLocationUseCase: GetOnlineCharactersLocationUseCase,
    private val intelStateController: IntelStateController,
    private val mapExternalControl: MapExternalControl,
    private val jumpBridgesRepository: JumpBridgesRepository,
    private val autopilotController: AutopilotController,
    private val mapStatusRepository: MapStatusRepository,
    private val mapJumpRangeController: MapJumpRangeController,
    private val mapPlanetsController: MapPlanetsController,
    private val mapSovereigntyUpgradesController: MapSovereigntyUpgradesController,
    private val windowManager: WindowManager,
    private val settings: Settings,
) : ViewModel() {

    data class Cluster(
        val systems: List<MapSolarSystem>,
        val constellations: List<MapConstellation>,
        val regions: List<MapRegion>,
        val connections: List<GateConnection>,
        val jumpBridgeConnections: List<JumpBridgeConnection>?,
    )

    data class MapState(
        val hoveredSystem: Int? = null,
        val selectedSystem: Int? = null,
        val centeredSystem: DataEvent<Int>? = null,
        val searchResults: List<Int> = emptyList(),
        val intel: Map<Int, List<IntelStateController.Dated<SystemEntity>>> = emptyMap(),
        val intelPopupSystems: List<Int> = emptyList(),
        val onlineCharacterLocations: Map<Int, List<OnlineCharacterLocation>> = emptyMap(),
        val systemStatus: Map<Int, SolarSystemStatus> = emptyMap(),
        val contextMenuSystem: Int? = null,
        val initialTransform: Transform? = null,
        val autopilotConnections: List<Pair<Int, Int>> = emptyList(),
    )

    sealed interface MapType {
        data class ClusterSystemsMap(val is2D: Boolean) : MapType
        data object ClusterRegionsMap : MapType
        data class RegionMap(val layoutId: Int, val regionIds: List<Int>) : MapType
        data object DistanceMap : MapType
    }

    data class VoronoiLayout(
        val position: Position,
        val polygon: List<Position>,
    )

    data class SystemInfoTypes(
        val starSelected: Map<SettingsMapType, MapSystemInfoType>,
        val starApplied: Map<SettingsMapType, MapSystemInfoType>,
        val cellSelected: Map<SettingsMapType, MapSystemInfoType?>,
        val cellApplied: Map<SettingsMapType, MapSystemInfoType?>,
        val indicators: Map<SettingsMapType, List<MapSystemInfoType>>,
        val infoBox: Map<SettingsMapType, List<MapSystemInfoType>>,
    )

    data class UiState(
        val tabs: List<Tab>,
        val selectedTab: Int,
        val search: String?,
        val systemInfoTypes: SystemInfoTypes,
        val mapJumpRangeState: MapJumpRangeState,
        val mapPlanetsState: MapPlanetsState,
        val mapSovereigntyUpgradesState: MapSovereigntyUpgradesState,
        val distanceMapState: DistanceMapState,
        val cluster: Cluster,
        val mapType: MapType,
        val layout: Map<Int, VoronoiLayout>,
        val jumpBridgeAdditionalSystems: Set<Int>,
        val mapState: MapState = MapState(),
        val alternativeLayouts: List<Layout>,
        val settings: IntelMap,
        val fitMapEvent: Event? = null,
    )

    private val openLayouts = mutableSetOf<Int>()

    data class Transform(val center: Offset, val zoom: Double)
    private val mapTransforms = mutableMapOf<MapType, Transform>()

    private val _state = MutableStateFlow(
        UiState(
            tabs = createTabs(),
            selectedTab = 0,
            search = null,
            systemInfoTypes = getColorModes(),
            mapJumpRangeState = mapJumpRangeController.state.value,
            mapPlanetsState = mapPlanetsController.state.value,
            mapSovereigntyUpgradesState = MapSovereigntyUpgradesState(),
            distanceMapState = distanceMapController.state.value,
            cluster = Cluster(
                systems = solarSystemsRepository.getSystems(knownSpace = true),
                constellations = solarSystemsRepository.mapConstellations,
                regions = solarSystemsRepository.mapRegions,
                connections = gateConnectionsRepository.gateConnections,
                jumpBridgeConnections = jumpBridgesRepository.getConnections(),
            ),
            mapType = ClusterSystemsMap(is2D = false),
            layout = emptyMap(),
            jumpBridgeAdditionalSystems = emptySet(),
            alternativeLayouts = emptyList(),
            settings = settings.intelMap,
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            intelStateController.state.collect { updateIntel() }
        }
        viewModelScope.launch {
            getOnlineCharactersLocationUseCase().collect(::onOnlineCharacterLocationsUpdated)
        }
        viewModelScope.launch {
            mapStatusRepository.status.collect { status -> updateMapState { copy(systemStatus = status) } }
        }
        viewModelScope.launch {
            mapJumpRangeController.state.collect { state -> _state.update { it.copy(mapJumpRangeState = state) } }
        }
        viewModelScope.launch {
            mapPlanetsController.state.collect { state -> _state.update { it.copy(mapPlanetsState = state) } }
        }
        viewModelScope.launch {
            mapSovereigntyUpgradesController.state.collect { state -> _state.update { it.copy(mapSovereigntyUpgradesState = state) } }
        }
        viewModelScope.launch {
            distanceMapController.state.collect { state ->
                _state.update { it.copy(distanceMapState = state) }
                if (_state.value.mapType == DistanceMap) {
                    // Distance map is open, so update it with the new state
                    openMap(DistanceMap, null)
                }
            }
        }
        viewModelScope.launch {
            settings.updateFlow.collect {
                _state.update {
                    it.copy(
                        systemInfoTypes = getColorModes(),
                        settings = settings.intelMap,
                        cluster = it.cluster.copy(jumpBridgeConnections = jumpBridgesRepository.getConnections()),
                    )
                }
            }
        }
        viewModelScope.launch {
            autopilotController.activeRoutes.map { map ->
                map.flatMap { (characterId, route) ->
                    route.systems.windowed(2).map { (from, to) -> from to to }
                }
            }.collect {
                updateMapState { copy(autopilotConnections = it) }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                updateIntel()
            }
        }
        viewModelScope.launch {
            mapExternalControl.event.collect {
                if (it?.value?.let { event -> event.windowUuid == windowUuid || event.windowUuid == null } == true) {
                    delay(50) // If this event comes from a context menu, let the menu disappear
                    when (val event = it.get()) {
                        is MapExternalControlEvent.ShowSystemOnNewEdenMap -> {
                            showSystemOnNewEdenMap(event.solarSystemId)
                        }
                        is MapExternalControlEvent.ShowSystemOnRegionMap -> {
                            showSystemOnRegionMap(event.solarSystemId)
                        }
                        null -> {}
                    }
                }
            }
        }
        viewModelScope.launch {
            distanceMapController.start()
        }

        initialize()
    }

    private fun initialize() {
        val openedTab = settings.intelMap.openedTabs2[windowUuid]
        if (openedTab is MapOpenedTab.DistanceMap) {
            distanceMapController.setSettings(openedTab.centerSystemId, openedTab.followingCharacterId, openedTab.distance)
        }
        openInitialTab(openedTab)
    }

    private fun openInitialTab(openedTab: MapOpenedTab?) {
        if (openedTab != null) {
            when (openedTab) {
                is MapOpenedTab.ClusterSystemsMap -> openTab(0, focusedId = null)
                MapOpenedTab.ClusterRegionsMap -> openTab(1, focusedId = null)
                is MapOpenedTab.DistanceMap -> openTab(2, focusedId = null)
                is MapOpenedTab.RegionMap -> openLayoutMap(openedTab.layoutId, focusedId = null)
            }
        } else {
            openTab(_state.value.selectedTab, focusedId = null)
        }
    }

    private fun getColorModes(): SystemInfoTypes {
        return SystemInfoTypes(
            starSelected = settings.intelMap.mapTypeSystemColor,
            starApplied = settings.intelMap.mapTypeSystemColor,
            cellSelected = settings.intelMap.mapTypeBackgroundColor,
            cellApplied = settings.intelMap.mapTypeBackgroundColor,
            indicators = settings.intelMap.mapTypeIndicatorInfoTypes,
            infoBox = settings.intelMap.mapTypeInfoBoxInfoTypes,
        )
    }

    fun onMapHover(offset: Offset, mapScale: Float) {
        if (_state.value.mapType == ClusterRegionsMap) return
        val (closestSystemId, closestSystemLayout) = _state.value.layout.minByOrNull { (_, layout) ->
            val position = layout.position
            (offset.x - position.x).pow(2) + (offset.y - position.y).pow(2)
        } ?: return
        val closestSystemLayoutPosition = closestSystemLayout.position
        val distanceInPixels = sqrt((offset.x - closestSystemLayoutPosition.x).pow(2) + (offset.y - closestSystemLayoutPosition.y).pow(2)) / mapScale
        val hoveredSystem = if (distanceInPixels < 10) closestSystemId else null
        updateMapState { copy(hoveredSystem = hoveredSystem) }
    }

    fun onRegionPointerEnter(regionId: Int) {
        updateMapState { copy(hoveredSystem = regionId) }
    }

    fun onRegionPointerExit(regionId: Int) {
        updateMapState { copy(hoveredSystem = if (hoveredSystem == regionId) null else hoveredSystem) }
    }

    fun onRegionClick(regionId: Int, systemId: Int) {
        openRegionMap(regionId, focusedId = systemId)
    }

    fun onTabClose(tabId: Int) {
        val tab = _state.value.tabs.firstOrNull { it.id == tabId } ?: return
        val layoutId = (tab.payload as? RegionMap)?.layoutId ?: return
        closeRegionMap(layoutId)
    }

    fun onMapClick(button: Int) {
        val hoveredSystem = _state.value.mapState.hoveredSystem
        if (button == 1) { // Left click
            updateMapState { copy(selectedSystem = hoveredSystem, contextMenuSystem = null) }
            if (state.value.mapType == ClusterRegionsMap && hoveredSystem != null) {
                openRegionMap(hoveredSystem, focusedId = null)
            }
        } else if (button == 3) { // Right click
            updateMapState { copy(contextMenuSystem = hoveredSystem) }
        }
    }

    private fun closeRegionMap(layoutId: Int) {
        openLayouts -= layoutId
        val tabs = createTabs()
        val tabIndex = _state.value.selectedTab.coerceAtMost(tabs.last().id)
        _state.update { it.copy(tabs = tabs) }
        openTab(tabIndex, focusedId = null)
    }

    private fun openRegionMap(regionId: Int, focusedId: Int?) {
        val layout = layoutRepository.getLayouts(regionId).first()
        openLayoutMap(layout.layoutId, focusedId)
    }

    private fun openLayoutMap(layoutId: Int, focusedId: Int?) {
        openLayouts += layoutId
        val tabs = createTabs()
        val tabIndex = tabs.reversed().firstOrNull { (it.payload as? RegionMap)?.layoutId == layoutId }?.id ?: return
        _state.update { it.copy(tabs = tabs) }
        openTab(tabIndex, focusedId)
    }

    fun onContextMenuDismiss() {
        updateMapState { copy(contextMenuSystem = null) }
    }

    fun onTabSelect(id: Int) {
        openTab(id, focusedId = null)
    }

    fun onMapTransformChanged(mapType: MapType, transform: Transform) {
        mapTransforms[mapType] = transform
    }

    fun onSearchChange(text: String) {
        val search = text.takeIf { it.isNotBlank() }?.trim()
        if (search != null && search.length >= 2) {
            val systemIds = _state.value.cluster.systems
                .filter { text.lowercase() in it.name.lowercase() }
                .map { it.id }
            val regionsIds = _state.value.cluster.regions
                .filter { text.lowercase() in it.name.lowercase() }
                .map { it.id }
            val resultIds = systemIds + regionsIds
            updateMapState { copy(searchResults = resultIds) }
        } else {
            updateMapState { copy(searchResults = emptyList()) }
        }
        _state.update { it.copy(search = search) }
    }

    fun onSearchSubmit() {
        val resultIds = _state.value.mapState.searchResults
        val visibleIds = _state.value.layout.keys
        val visibleResultIds = resultIds.intersect(visibleIds).toList()
        if (visibleResultIds.isEmpty()) {
            if (resultIds.isNotEmpty()) {
                if (settings.intelMap.isPreferringRegionMaps) {
                    showSystemOnRegionMap(resultIds.first())
                } else {
                    showSystemOnNewEdenMap(resultIds.first())
                }
            }
            return
        }

        val centered = _state.value.mapState.centeredSystem?.value
        var index = visibleResultIds.indexOf(centered) + 1
        if (index > visibleResultIds.lastIndex) index = 0

        updateMapState { copy(centeredSystem = DataEvent(visibleResultIds[index])) }
    }

    fun onSystemColorChange(mapType: SettingsMapType, selected: MapSystemInfoType) {
        val new = settings.intelMap.mapTypeSystemColor + (mapType to selected)
        settings.intelMap = settings.intelMap.copy(mapTypeSystemColor = new)
    }

    fun onSystemColorHover(mapType: SettingsMapType, selected: MapSystemInfoType, isHovered: Boolean) {
        val new = if (isHovered) _state.value.systemInfoTypes.starSelected + (mapType to selected) else getColorModes().starSelected
        _state.update { it.copy(systemInfoTypes = it.systemInfoTypes.copy(starApplied = new)) }
    }

    fun onCellColorChange(mapType: SettingsMapType, selected: MapSystemInfoType?) {
        val new = settings.intelMap.mapTypeBackgroundColor + (mapType to selected)
        settings.intelMap = settings.intelMap.copy(mapTypeBackgroundColor = new)
    }

    fun onCellColorHover(mapType: SettingsMapType, selected: MapSystemInfoType?, isHovered: Boolean) {
        val new = if (isHovered) _state.value.systemInfoTypes.cellSelected + (mapType to selected) else getColorModes().cellSelected
        _state.update { it.copy(systemInfoTypes = it.systemInfoTypes.copy(cellApplied = new)) }
    }

    fun onIndicatorChange(mapType: SettingsMapType, selected: MapSystemInfoType) {
        val newMap = toggleType(mapType, settings.intelMap.mapTypeIndicatorInfoTypes, selected)
        settings.intelMap = settings.intelMap.copy(mapTypeIndicatorInfoTypes = newMap)
    }

    fun onInfoBoxChange(mapType: SettingsMapType, selected: MapSystemInfoType) {
        val newMap = toggleType(mapType, settings.intelMap.mapTypeInfoBoxInfoTypes, selected)
        settings.intelMap = settings.intelMap.copy(mapTypeInfoBoxInfoTypes = newMap)
    }

    private fun toggleType(mapType: SettingsMapType, currentMap: Map<SettingsMapType, List<MapSystemInfoType>>, selected: MapSystemInfoType): Map<SettingsMapType, List<MapSystemInfoType>> {
        val currentTypes = currentMap[mapType].orEmpty()
        val newTypes = if (selected in currentTypes) currentTypes - selected else currentTypes + selected
        return currentMap + (mapType to newTypes.sortedBy { it.ordinal })
    }

    fun onJumpRangeTargetUpdate(target: String) {
        mapJumpRangeController.onTargetUpdate(target)
    }

    fun onJumpRangeDistanceUpdate(distanceLy: Double) {
        mapJumpRangeController.onRangeUpdate(distanceLy)
    }

    fun onPlanetTypesUpdate(types: List<PlanetType>) {
        mapPlanetsController.onPlanetTypesUpdate(types)
    }

    fun onSovereigntyUpgradeTypesUpdate(types: List<Type>) {
        mapSovereigntyUpgradesController.onSovereigntyUpgradeTypesUpdate(types)
    }

    fun onLayoutSelected(layoutId: Int) {
        openLayoutMap(layoutId, _state.value.mapState.centeredSystem?.value)
    }

    fun onDistanceMapCenterUpdate(target: String) {
        distanceMapController.setCenterTarget(target)
    }

    fun onDistanceMapRangeUpdate(range: Int) {
        distanceMapController.setDistance(range)
    }

    fun onFocusCurrentClick() {
        val locations = _state.value.mapState.onlineCharacterLocations.values.flatten()
        if (locations.isEmpty()) return
        when (val mapType = _state.value.mapType) {
            ClusterRegionsMap -> {
                val regionId = locations.firstNotNullOfOrNull { it.location.regionId } ?: return
                updateMapState { copy(centeredSystem = DataEvent(regionId)) }
            }
            is ClusterSystemsMap -> {
                updateMapState { copy(centeredSystem = DataEvent(locations.first().location.solarSystemId)) }
            }
            is RegionMap -> {
                val centeredId = locations.firstOrNull { it.location.regionId in mapType.regionIds }?.location?.solarSystemId
                if (centeredId != null) {
                    updateMapState { copy(centeredSystem = DataEvent(centeredId)) }
                } else {
                    val location = locations.firstOrNull {
                        it.location.regionId != null && layoutRepository.getLayouts(it.location.regionId).isNotEmpty()
                    }?.location ?: return
                    openRegionMap(location.regionId!!, location.solarSystemId)
                }
            }
            is DistanceMap -> {}
        }
    }

    fun onFitMapClick() {
        _state.update { it.copy(fitMapEvent = Event()) }
    }

    fun onToggle2dLayoutClick() {
        val is2D = (_state.value.mapType as? ClusterSystemsMap)?.is2D ?: return
        _state.update { it.copy(tabs = it.tabs.map { tab -> if (tab.id == 0) tab.copy(payload = ClusterSystemsMap(!is2D)) else tab }) }
        settings.intelMap = settings.intelMap.copy(isUsing2DClusterLayout = !is2D)
        openMap(ClusterSystemsMap(is2D = !is2D), null, recenter = false)
    }

    /**
     * Open the given map tab, and the map inside it
     */
    private fun openTab(id: Int, focusedId: Int?) {
        val tab = _state.value.tabs.firstOrNull { it.id == id } ?: return
        val mapType = tab.payload as? MapType ?: return
        _state.update { it.copy(selectedTab = id) }
        openMap(mapType, focusedId)
    }

    /**
     * Open the given map type. The correct tab should already be open.
     */
    private fun openMap(mapType: MapType, focusedId: Int?, recenter: Boolean = true) {
        rememberOpenedLayout(mapType)
        mapExternalControl.setOpenedRegions(windowUuid, (mapType as? RegionMap)?.regionIds ?: emptyList())

        val layout = when (mapType) {
            is ClusterSystemsMap -> if (mapType.is2D) layoutRepository.getNewEdenSystemPosition2D() else layoutRepository.getNewEdenSystemPosition()
            ClusterRegionsMap -> layoutRepository.getRegionsPositions()
            is RegionMap -> layoutRepository.getLayoutSystemPositions(mapType.layoutId) ?: throw IllegalArgumentException("No such layout: ${mapType.layoutId}")
            is DistanceMap -> distanceMapController.state.value.layout
        }
        val jumpBridgeAdditionalSystemsLayout = if (mapType is RegionMap) getJumpBridgeDestinationsLayout(layout) else emptyMap()
        val combined = calculateVoronoi(layout + jumpBridgeAdditionalSystemsLayout)

        val alternativeLayouts = getAlternativeLayouts(mapType)

        val isPanning = settings.intelMap.isFollowingCharacterWithinLayouts
        var centeredId = focusedId ?: if (isPanning) getOnlineCharacterLocationId(mapType) else null
        if (centeredId !in layout.keys) centeredId = null
        val initialTransform = mapTransforms[mapType]

        if (recenter) {
            updateMapState { copy(hoveredSystem = null, centeredSystem = centeredId?.let { DataEvent(it) }, contextMenuSystem = null, initialTransform = initialTransform) }
        } else {
            updateMapState { copy(hoveredSystem = null, contextMenuSystem = null, initialTransform = initialTransform) }
        }
        _state.update {
            it.copy(
                mapType = mapType,
                layout = combined,
                jumpBridgeAdditionalSystems = jumpBridgeAdditionalSystemsLayout.keys,
                alternativeLayouts = alternativeLayouts,
            )
        }
    }

    private fun getAlternativeLayouts(mapType: MapType): List<Layout> {
        return when (mapType) {
            ClusterRegionsMap -> emptyList()
            is ClusterSystemsMap -> emptyList()
            is RegionMap -> {
                mapType.regionIds.flatMap { layoutRepository.getLayouts(it) }.distinct()
            }
            is DistanceMap -> emptyList()
        }
    }

    private fun rememberOpenedLayout(mapType: MapType) {
        val openedMapTab = when (mapType) {
            ClusterRegionsMap -> MapOpenedTab.ClusterRegionsMap
            is ClusterSystemsMap -> MapOpenedTab.ClusterSystemsMap(mapType.is2D)
            is DistanceMap -> {
                val state = distanceMapController.state.value
                MapOpenedTab.DistanceMap(state.centerSystemId, state.followingCharacterId, state.distance)
            }
            is RegionMap -> MapOpenedTab.RegionMap(mapType.layoutId)
        }
        val openWindowUuids = windowManager.getOpenWindowUuids(RiftWindow.Map)
        val openedTabs = settings.intelMap.openedTabs2.filter { it.key in openWindowUuids } + (windowUuid to openedMapTab)
        settings.intelMap = settings.intelMap.copy(openedTabs2 = openedTabs)
    }

    private fun calculateVoronoi(systems: Map<Int, Position>): Map<Int, VoronoiLayout> {
        if (systems.size == 1) {
            return systems.map { (systemId, position) ->
                systemId to VoronoiLayout(
                    position,
                    listOf(
                        position.copy(position.x - 100, position.y - 100),
                        position.copy(position.x + 100, position.y - 100),
                        position.copy(position.x + 100, position.y + 100),
                        position.copy(position.x - 100, position.y + 100),
                    ),
                )
            }.toMap()
        }

        val coordinates = systems.map { (system, position) ->
            Coordinate(position.x.toDouble(), position.y.toDouble()) to system
        }.toMap()
        val builder = VoronoiDiagramBuilder()
        builder.setSites(coordinates.keys)
        val diagram = builder.getDiagram(GeometryFactory()) as GeometryCollection
        return List(diagram.numGeometries) {
            val polygon = diagram.getGeometryN(it) as Polygon
            val points = polygon.exteriorRing.coordinates.map {
                Position(it.x.toInt(), it.y.toInt())
            }
            val coordinate = polygon.userData as Coordinate
            val system = coordinates[coordinate]!!
            system to VoronoiLayout(systems[system]!!, points)
        }.toMap()
    }

    /**
     * Layout of systems that are connected by jump bridges from systems in this layout
     */
    private fun getJumpBridgeDestinationsLayout(originsLayout: Map<Int, Position>): Map<Int, Position> {
        val outgoingJumpBridgeConnectionsInLayout = jumpBridgesRepository.getConnections()?.filter {
            (it.from.id in originsLayout.keys) xor (it.to.id in originsLayout.keys)
        }.orEmpty()
        return outgoingJumpBridgeConnectionsInLayout.fold(emptyMap()) { layout, connection ->
            val (origin, destination) = if (connection.from.id in originsLayout.keys) connection.from to connection.to else connection.to to connection.from
            if (destination.id in layout.keys) return@fold layout // Already in layout
            val entry = destination.id to getOutOfRegionLayoutPosition(originsLayout, layout, origin.id)
            layout + entry
        }
    }

    private fun getOutOfRegionLayoutPosition(
        originsLayout: Map<Int, Position>,
        outOfRegionLayout: Map<Int, Position>,
        system: Int,
    ): Position {
        val layoutCenter = Position(originsLayout.maxOf { it.value.x } / 2, originsLayout.maxOf { it.value.y } / 2)
        val position = originsLayout[system]!!
        val slope = if (position.x != layoutCenter.x) {
            (position.y - layoutCenter.y) / (position.x - layoutCenter.x).toFloat()
        } else {
            if (position.y > layoutCenter.y) 1000f else -1000f
        }
        val b = position.y - (position.x * slope)

        val xDelta = if (position.x > layoutCenter.x) 1 else -1
        var x = position.x + xDelta
        val combinedLayout = originsLayout + outOfRegionLayout
        val minDistance = 80
        while (combinedLayout.minOf { Position(x, (x * slope + b).toInt()).distanceSquared(it.value) } < (minDistance * minDistance)) {
            x += xDelta
        }

        val y = x * slope + b
        return Position(x, y.roundToInt())
    }

    private fun getOnlineCharacterLocationId(mapType: MapType): Int? {
        return _state.value.mapState.onlineCharacterLocations.values
            .flatten()
            .filter {
                when (mapType) {
                    ClusterRegionsMap, is ClusterSystemsMap, is DistanceMap -> true
                    is RegionMap -> it.location.regionId in mapType.regionIds
                }
            }
            .map {
                when (mapType) {
                    ClusterRegionsMap -> it.location.regionId
                    is ClusterSystemsMap, is RegionMap, is DistanceMap -> it.location.solarSystemId
                }
            }
            .firstOrNull()
    }

    private fun onOnlineCharacterLocationsUpdated(onlineCharacterLocations: List<OnlineCharacterLocation>) {
        val isPanning = settings.intelMap.isFollowingCharacterWithinLayouts
        val isSwitching = settings.intelMap.isFollowingCharacterAcrossLayouts
        if (isPanning || isSwitching) {
            val current = _state.value.mapState.onlineCharacterLocations.values.flatten()
            onlineCharacterLocations.forEach { onlineCharacterLocation ->
                val previous = current.firstOrNull { it.id == onlineCharacterLocation.id }
                if (previous?.location?.solarSystemId == onlineCharacterLocation.location.solarSystemId) return@forEach
                focusOnlineCharacterLocation(onlineCharacterLocation, isPanning, isSwitching)
            }
        }

        val locations = onlineCharacterLocations.groupBy { it.location.solarSystemId }
        _state.update { it.copy(mapState = it.mapState.copy(onlineCharacterLocations = locations)) }
    }

    private fun focusOnlineCharacterLocation(
        onlineCharacterLocation: OnlineCharacterLocation,
        isPanning: Boolean = true,
        isSwitching: Boolean = true,
    ) {
        val systemId = onlineCharacterLocation.location.solarSystemId
        val regionId = onlineCharacterLocation.location.regionId ?: return

        when (val mapType = _state.value.mapType) {
            ClusterRegionsMap -> {
                if (isPanning) {
                    updateMapState { copy(centeredSystem = DataEvent(regionId)) }
                }
            }
            is ClusterSystemsMap -> {
                if (isPanning) {
                    updateMapState { copy(centeredSystem = DataEvent(systemId)) }
                }
            }
            is RegionMap -> {
                if (regionId in mapType.regionIds) {
                    if (isPanning) {
                        updateMapState { copy(centeredSystem = DataEvent(systemId)) }
                    }
                } else {
                    if (isSwitching && layoutRepository.getLayouts(regionId).isNotEmpty()) {
                        openRegionMap(regionId, systemId.takeIf { isPanning })
                    }
                }
            }
            is DistanceMap -> {}
        }
    }

    private fun createTabs(): List<Tab> {
        return listOf(
            Tab(id = 0, title = "New Eden", isCloseable = false, icon = Res.drawable.map_universe, payload = ClusterSystemsMap(is2D = settings.intelMap.isUsing2DClusterLayout)),
            Tab(id = 1, title = "Regions", isCloseable = false, icon = Res.drawable.map_region, payload = ClusterRegionsMap),
            Tab(id = 2, title = "Distance", isCloseable = false, icon = Res.drawable.map_constellation, payload = DistanceMap),
        ) + openLayouts.mapIndexed { index, layoutId ->
            val layout = layoutRepository.getLayout(layoutId)
            val name = layout?.name ?: "$layoutId"
            val regionIds = layout?.regionIds ?: emptyList()
            Tab(id = 3 + index, title = name, isCloseable = true, payload = RegionMap(layoutId, regionIds))
        }
    }

    private fun updateMapState(update: MapState.() -> MapState) {
        _state.update { state -> state.copy(mapState = state.mapState.update()) }
    }

    private fun updateIntel() = viewModelScope.launch {
        val intelBySystem = intelStateController.state.value
        val intelBySystemId = intelBySystem.mapKeys { (key, _) ->
            key.id
        }

        val popupMinTimestamp = Instant.now() - Duration.ofSeconds(settings.intelMap.intelPopupTimeoutSeconds.toLong())
        val filtered = intelBySystemId.filter { (_, datedEntities) ->
            datedEntities.isNotEmpty() // Remove systems that no longer have any intel to show
        }
        val popupSystems = filtered.mapNotNull { (systemId, datedEntities) ->
            val showPopup =
                datedEntities.any { it.timestamp >= popupMinTimestamp } || // Only show on a system if within the popup timeout setting
                    systemId == _state.value.mapState.hoveredSystem // Or is hovered
            systemId.takeIf { showPopup }
        }

        updateMapState { copy(intel = filtered, intelPopupSystems = popupSystems) }
    }

    private fun showSystemOnNewEdenMap(solarSystemId: Int) {
        openTab(0, solarSystemId)
    }

    private fun showSystemOnRegionMap(solarSystemId: Int) {
        val regionId = solarSystemsRepository.getRegionIdBySystemId(solarSystemId) ?: return
        if (regionId in solarSystemsRepository.getKnownSpaceRegions().map { it.id }) {
            openRegionMap(regionId, solarSystemId)
        }
    }
}
