package dev.nohus.rift.map

import dev.nohus.rift.DataEvent
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import dev.nohus.rift.windowing.WindowManager.WindowEvent.WindowClosed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.util.UUID

@Single
class MapExternalControl(
    private val windowManager: WindowManager,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val settings: Settings,
) {

    private val scope = CoroutineScope(Job())
    private val _event = MutableStateFlow<DataEvent<MapExternalControlEvent>?>(null)
    val event = _event.asStateFlow()

    private val _openedRegions = MutableStateFlow<Map<UUID, List<Int>>>(emptyMap())
    val openedRegions = _openedRegions.asStateFlow()

    init {
        scope.launch {
            windowManager.windowEvents.collect { event ->
                when (event) {
                    is WindowClosed -> {
                        if (event.window == RiftWindow.Map) {
                            setOpenedRegions(event.uuid, null)
                        }
                    }
                }
            }
        }
    }

    /**
     * windowUuid controls which map window should handle this event. When it's null, it will be handled by all windows,
     * used when there are no map windows open, and so the first opened window should handle it.
     */
    sealed class MapExternalControlEvent(open val windowUuid: UUID?) {
        data class ShowSystemOnNewEdenMap(override val windowUuid: UUID?, val solarSystemId: Int) : MapExternalControlEvent(windowUuid)
        data class ShowSystemOnRegionMap(override val windowUuid: UUID?, val solarSystemId: Int) : MapExternalControlEvent(windowUuid)
    }

    fun setOpenedRegions(uuid: UUID, regionIds: List<Int>?) {
        if (regionIds != null) {
            _openedRegions.value += (uuid to regionIds)
        } else {
            _openedRegions.value -= uuid
        }
    }

    fun getOpenedRegions(): List<Int> {
        return _openedRegions.value.values.flatten()
    }

    fun showSystemOnMap(solarSystemId: Int) {
        if (settings.intelMap.isPreferringRegionMaps) {
            showSystemOnRegionMap(solarSystemId)
        } else {
            showSystemOnNewEdenMap(solarSystemId)
        }
    }

    fun showSystemOnNewEdenMap(solarSystemId: Int) {
        val windows = _openedRegions.value
        val mapWindow = windows
            .filterValues { it.isEmpty() }.keys.firstOrNull() // Open on a map not showing any region
            ?: windows.keys.firstOrNull() // Open on any map
        if (mapWindow == null) windowManager.onWindowOpen(RiftWindow.Map)
        scope.launch {
            _event.emit(DataEvent(MapExternalControlEvent.ShowSystemOnNewEdenMap(mapWindow, solarSystemId)))
        }
    }

    fun showSystemOnRegionMap(solarSystemId: Int) {
        val regionId = solarSystemsRepository.getSystem(solarSystemId)?.regionId
        val mapWindow = if (regionId != null) {
            val windows = _openedRegions.value
            windows.filterValues { regionId in it }.keys.firstOrNull() // Open on a map showing this system's region
                ?: windows.filterValues { it.isEmpty() }.keys.firstOrNull() // Open on a map not showing any region
                ?: windows.keys.firstOrNull() // Open on any map
        } else {
            null
        }
        if (mapWindow == null) windowManager.onWindowOpen(RiftWindow.Map)
        scope.launch {
            _event.emit(DataEvent(MapExternalControlEvent.ShowSystemOnRegionMap(mapWindow, solarSystemId)))
        }
    }
}
