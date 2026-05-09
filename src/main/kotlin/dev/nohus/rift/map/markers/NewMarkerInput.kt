package dev.nohus.rift.map.markers

import androidx.compose.ui.graphics.Color

data class NewMarkerInput(
    val system: String,
    val label: String,
    val color: Color? = null,
    val icon: String,
)
