package dev.nohus.rift.map.markers

import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource
import java.util.UUID

data class MapMarkerItem(
    val id: UUID,
    val systemId: Int,
    val systemName: String,
    val regionName: String,
    val label: String,
    val color: Color?,
    val iconName: String,
    val icon: DrawableResource,
)
