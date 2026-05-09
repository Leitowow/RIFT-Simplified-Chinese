package dev.nohus.rift.map.markers

import dev.nohus.rift.ViewModel
import dev.nohus.rift.compose.DialogMessage
import dev.nohus.rift.compose.MessageDialogType
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.allDrawableResources
import dev.nohus.rift.generated.resources.map_marker_place_bookmark
import dev.nohus.rift.map.MapExternalControl
import dev.nohus.rift.map.markers.MapMarkersInputModel.AddToSystem
import dev.nohus.rift.map.markers.MapMarkersInputModel.New
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.settings.persistence.MapMarker
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import java.util.UUID

@Factory
class MapMarkersViewModel(
    @InjectedParam private val inputModel: MapMarkersInputModel,
    private val settings: Settings,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val mapExternalControl: MapExternalControl,
) : ViewModel() {

    data class UiState(
        val systemText: String = "",
        var editingMarker: MapMarkerItem? = null,
        val markers: List<MapMarkerItem> = emptyList(),
        val dialog: DialogMessage? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        when (inputModel) {
            New -> {}
            is AddToSystem -> _state.update {
                it.copy(systemText = solarSystemsRepository.getSystemName(inputModel.systemId) ?: "")
            }
        }
        loadMarkers()
        viewModelScope.launch {
            settings.updateFlow.map { it.mapMarkers }.collect {
                loadMarkers()
            }
        }
    }

    private fun loadMarkers() {
        val markers = settings.mapMarkers.map {
            MapMarkerItem(
                id = it.id,
                systemId = it.systemId,
                systemName = solarSystemsRepository.getSystemName(it.systemId) ?: "Unknown",
                regionName = solarSystemsRepository.getRegionBySystemId(it.systemId)?.name ?: "Unknown",
                label = it.label,
                color = it.color,
                iconName = it.icon,
                icon = Res.allDrawableResources[it.icon] ?: Res.drawable.map_marker_place_bookmark,
            )
        }.sortedWith(compareBy({ it.regionName }, { it.systemName }))
        _state.update { it.copy(markers = markers) }
    }

    fun onSystemTextChange(text: String) {
        _state.update { it.copy(systemText = text) }
    }

    fun onAddMarkerClick(input: NewMarkerInput): Boolean {
        if (input.system.isNotBlank()) {
            val system = solarSystemsRepository.getSystem(input.system)
            if (system != null) {
                addMarker(system, input)
                return true
            } else {
                showError("System \"${input.system}\" does not exist")
            }
        } else {
            showError("You need to enter a system name")
        }
        return false
    }

    fun onDeleteMarkerClick(id: UUID) {
        deleteMarker(id)
    }

    private fun addMarker(system: MapSolarSystem, input: NewMarkerInput) {
        val id = _state.value.editingMarker?.id ?: UUID.randomUUID()
        deleteMarker(id)
        val newMarker = MapMarker(
            id = id,
            systemId = system.id,
            label = input.label,
            color = input.color,
            icon = input.icon,
        )
        _state.update { it.copy(editingMarker = null) }
        settings.mapMarkers += newMarker
    }

    private fun deleteMarker(id: UUID) {
        settings.mapMarkers -= settings.mapMarkers.firstOrNull { it.id == id } ?: return
    }

    fun onCancelEditClick() {
        _state.update {
            it.copy(
                systemText = "",
                editingMarker = null,
            )
        }
    }

    fun onMarkerClick(id: UUID) {
        val marker = _state.value.markers.firstOrNull { it.id == id } ?: return
        mapExternalControl.showSystemOnMap(marker.systemId)
    }

    fun onEditMarkerClick(id: UUID) {
        val marker = _state.value.markers.firstOrNull { it.id == id } ?: return
        _state.update {
            it.copy(
                systemText = marker.systemName,
                editingMarker = marker,
            )
        }
    }

    private fun showError(text: String) {
        _state.update {
            it.copy(
                dialog = DialogMessage(
                    title = "Cannot create marker",
                    message = text,
                    type = MessageDialogType.Info,
                ),
            )
        }
    }

    fun onCloseDialogMessage() {
        _state.update { it.copy(dialog = null) }
    }
}
