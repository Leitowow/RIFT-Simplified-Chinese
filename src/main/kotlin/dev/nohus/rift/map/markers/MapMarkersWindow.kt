package dev.nohus.rift.map.markers

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RiftAutocompleteTextField
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftMessageDialog
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.allDrawableResources
import dev.nohus.rift.generated.resources.deleteicon
import dev.nohus.rift.generated.resources.editplanicon
import dev.nohus.rift.generated.resources.flag_background
import dev.nohus.rift.generated.resources.window_locations
import dev.nohus.rift.map.markers.MapMarkersViewModel.UiState
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.painterResource
import java.util.UUID

@Composable
fun MapMarkersWindow(
    inputModel: MapMarkersInputModel,
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: MapMarkersViewModel = viewModel(inputModel)
    val state by viewModel.state.collectAsState()

    RiftWindow(
        title = "Map Markers",
        icon = Res.drawable.window_locations,
        state = windowState,
        onCloseClick = onCloseRequest,
        isResizable = false,
    ) {
        MapMarkersWindowContent(
            state = state,
            onSystemTextChange = viewModel::onSystemTextChange,
            onAddMarkerClick = viewModel::onAddMarkerClick,
            onDeleteMarkerClick = viewModel::onDeleteMarkerClick,
            onCancelEditClick = viewModel::onCancelEditClick,
            onMarkerClick = viewModel::onMarkerClick,
            onEditMarkerClick = viewModel::onEditMarkerClick,
        )

        state.dialog?.let {
            RiftMessageDialog(
                dialog = it,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseDialogMessage,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MapMarkersWindowContent(
    state: UiState,
    onSystemTextChange: (String) -> Unit,
    onAddMarkerClick: (NewMarkerInput) -> Boolean,
    onDeleteMarkerClick: (UUID) -> Unit,
    onCancelEditClick: () -> Unit,
    onMarkerClick: (UUID) -> Unit,
    onEditMarkerClick: (UUID) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        ScrollbarColumn(
            modifier = Modifier
                .height(200.dp)
                .border(1.dp, RiftTheme.colors.borderGrey),
            scrollbarModifier = Modifier.padding(vertical = Spacing.small),
        ) {
            for (marker in state.markers) {
                key(marker) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .hoverBackground()
                            .onClick {
                                onMarkerClick(marker.id)
                            }
                            .padding(Spacing.small),
                    ) {
                        Image(
                            painter = painterResource(marker.icon),
                            contentDescription = null,
                            colorFilter = marker.color?.let { ColorFilter.tint(it) },
                            modifier = Modifier
                                .size(16.dp),
                        )
                        Text(
                            text = "${marker.systemName} (${marker.regionName})",
                            style = RiftTheme.typography.bodyHighlighted,
                            maxLines = 1,
                        )
                        Text(
                            text = marker.label,
                            style = RiftTheme.typography.bodyPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Row {
                            RiftImageButton(
                                resource = Res.drawable.editplanicon,
                                size = 20.dp,
                                onClick = { onEditMarkerClick(marker.id) },
                            )
                            RiftImageButton(
                                resource = Res.drawable.deleteicon,
                                size = 20.dp,
                                onClick = { onDeleteMarkerClick(marker.id) },
                            )
                        }
                    }
                }
            }
            if (state.markers.isEmpty()) {
                Text(
                    text = "No map markers created",
                    style = RiftTheme.typography.headerPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.large)
                        .padding(horizontal = Spacing.large),
                )
            }
        }

        var markerText by remember { mutableStateOf("") }
        var markerColor: Color? by remember { mutableStateOf(null) }
        var markerIcon by remember { mutableStateOf("map_marker_place_bookmark") }

        LaunchedEffect(state.editingMarker) {
            state.editingMarker?.let {
                markerText = it.label
                markerColor = it.color
                markerIcon = it.iconName
            } ?: run {
                markerText = ""
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            val solarSystemsRepository: SolarSystemsRepository = remember { koin.get() }
            RiftAutocompleteTextField(
                text = state.systemText,
                suggestions = remember(state.systemText) { getSuggestions(solarSystemsRepository, state.systemText) },
                placeholder = "System name",
                onTextChanged = {
                    onSystemTextChange(it)
                },
                onDeleteClick = { onSystemTextChange("") },
                modifier = Modifier.weight(1f),
            )
            RiftTextField(
                text = markerText,
                placeholder = "Label",
                onTextChanged = { markerText = it.take(50) },
                onDeleteClick = { markerText = "" },
                modifier = Modifier.weight(1f),
            )
            val isEditing = state.editingMarker != null
            if (isEditing) {
                RiftButton(
                    text = "Cancel",
                    type = ButtonType.Secondary,
                    cornerCut = ButtonCornerCut.None,
                    onClick = onCancelEditClick,
                )
            }
            RiftButton(
                text = if (isEditing) "Update" else "Add marker",
                onClick = {
                    val added = onAddMarkerClick(NewMarkerInput(state.systemText, markerText, markerColor, markerIcon))
                    if (added) {
                        onSystemTextChange("")
                        markerText = ""
                    }
                },
            )
        }

        Text(
            text = "Marker color",
            style = RiftTheme.typography.headerPrimary,
        )
        val animatedColor by animateColorAsState(markerColor ?: Color.White)
        val colors = listOf(
            null,
            Color(0xFF2C75E1),
            Color(0xFF399AEB),
            Color(0xFF4ECEF8),
            Color(0xFF60DBA3),
            Color(0xFF71E754),
            Color(0xFFF5FF83),
            Color(0xFFDC6C06),
            Color(0xFFCE440F),
            Color(0xFFBB1116),
            Color(0xFF731F1F),
            Color(0xFF8D3163),
        )
        Row(
            modifier = Modifier
                .border(1.dp, RiftTheme.colors.borderGreyLight),
        ) {
            for (color in colors) {
                Image(
                    painter = painterResource(Res.drawable.flag_background),
                    contentDescription = null,
                    colorFilter = color?.let { ColorFilter.tint(it) },
                    modifier = Modifier
                        .hoverBackground()
                        .modifyIf(color == markerColor) {
                            background(RiftTheme.colors.backgroundSelected)
                        }
                        .onClick {
                            markerColor = color
                        }
                        .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                        .padding(6.dp)
                        .size(12.dp),
                )
            }
        }

        Text(
            text = "Marker icon",
            style = RiftTheme.typography.headerPrimary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Res.allDrawableResources
                .filter { (name, _) -> name.startsWith("map_marker_") }
                .entries
                .groupBy { it.key.substringAfter("map_marker_").substringBefore("_") }
                .forEach { (_, markers) ->
                    Row(
                        modifier = Modifier
                            .border(1.dp, RiftTheme.colors.borderGreyLight),
                    ) {
                        for ((name, drawable) in markers) {
                            Image(
                                painter = painterResource(drawable),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(animatedColor),
                                modifier = Modifier
                                    .hoverBackground()
                                    .modifyIf(name == markerIcon) {
                                        background(RiftTheme.colors.backgroundSelected)
                                    }
                                    .onClick {
                                        markerIcon = name
                                    }
                                    .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                                    .padding(Spacing.small)
                                    .size(16.dp),
                            )
                        }
                    }
                }
        }
    }
}

private fun getSuggestions(solarSystemsRepository: SolarSystemsRepository, text: String): List<String> {
    solarSystemsRepository.getSystem(text)?.let { return emptyList() }
    return solarSystemsRepository.getSystems()
        .asSequence()
        .map { it.name }
        .filter { it.lowercase().startsWith(text.lowercase()) }
        .take(5)
        .toList()
}
