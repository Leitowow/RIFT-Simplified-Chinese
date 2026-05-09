package dev.nohus.rift.map.markers

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.allDrawableResources
import dev.nohus.rift.generated.resources.map_marker_place_bookmark
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.DrawableResource
import org.koin.core.annotation.Single

@Single
class MapMarkersRepository(
    settings: Settings,
) {

    data class MapMarker(
        val systemId: Int,
        val label: String,
        val color: Color?,
        val icon: DrawableResource,
    )

    val markers: Flow<Map<Int, List<MapMarker>>> = settings.updateFlow
        .map { it.mapMarkers }
        .map { markers ->
            markers.map {
                MapMarker(
                    systemId = it.systemId,
                    label = it.label,
                    color = it.color,
                    icon = Res.allDrawableResources[it.icon] ?: Res.drawable.map_marker_place_bookmark,
                )
            }.groupBy { it.systemId }
        }
}
