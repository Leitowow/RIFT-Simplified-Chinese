package dev.nohus.rift.jukebox

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftContextMenuArea
import dev.nohus.rift.compose.RiftDialog
import dev.nohus.rift.compose.RiftIconButton
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftSlider
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.VerticalDivider
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.collapse
import dev.nohus.rift.generated.resources.expand
import dev.nohus.rift.generated.resources.jukebox_next
import dev.nohus.rift.generated.resources.jukebox_pause
import dev.nohus.rift.generated.resources.jukebox_play
import dev.nohus.rift.generated.resources.jukebox_previous
import dev.nohus.rift.generated.resources.jukebox_shuffle
import dev.nohus.rift.generated.resources.jukebox_volume_down
import dev.nohus.rift.generated.resources.jukebox_volume_muted
import dev.nohus.rift.generated.resources.jukebox_volume_up
import dev.nohus.rift.generated.resources.play
import dev.nohus.rift.generated.resources.window_jukebox
import dev.nohus.rift.generated.resources.window_question
import dev.nohus.rift.jukebox.JukeboxViewModel.PlayState.Paused
import dev.nohus.rift.jukebox.JukeboxViewModel.PlayState.Playing
import dev.nohus.rift.jukebox.JukeboxViewModel.PlayState.Stopped
import dev.nohus.rift.jukebox.JukeboxViewModel.UiState
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.painterResource
import java.awt.Window
import java.nio.file.Path
import java.time.Duration
import java.util.UUID
import javax.swing.JFileChooser
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun JukeboxWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: JukeboxViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    if (state.isRevealed) {
        val isExpanded = windowState.window == RiftWindow.Jukebox
        var isPlaylistDialogShown by remember { mutableStateOf(false) }
        var renamedPlaylist: Playlist? by remember { mutableStateOf(null) }

        RiftWindow(
            title = "Jukebox",
            icon = Res.drawable.window_jukebox,
            state = windowState,
            onCloseClick = onCloseRequest,
            withContentPadding = false,
            isResizable = isExpanded,
        ) {
            JukeboxWindowContent(
                state = state,
                isExpanded = isExpanded,
                onExpandedToggleClick = { viewModel.onExpandedToggleClick(!isExpanded) },
                onVolumeChange = viewModel::onVolumeChange,
                onPreviousClick = viewModel::onPreviousClick,
                onNextClick = viewModel::onNextClick,
                onPlayClick = viewModel::onPlayClick,
                onShuffleClick = viewModel::onShuffleClick,
                onTrackClick = viewModel::onTrackClick,
                onTrackDoubleClick = viewModel::onTrackDoubleClick,
                onTrackRemove = viewModel::onTrackRemove,
                onPlaylistClick = viewModel::onPlaylistClick,
                onPlayPlaylistClick = viewModel::onPlayPlaylistClick,
                onRenamePlaylistClick = { renamedPlaylist = it },
                onDeletePlaylistClick = viewModel::onDeletePlaylistClick,
                onNewPlaylistClick = { isPlaylistDialogShown = true },
                onAddTracksClick = { chooseFile(window, viewModel::onFilesChosen) },
            )

            if (isPlaylistDialogShown) {
                PlaylistDialog(
                    windowState = windowState,
                    name = "",
                    onDismiss = { isPlaylistDialogShown = false },
                    onCreate = viewModel::onPlaylistCreate,
                )
            }
            renamedPlaylist?.let { playlist ->
                PlaylistDialog(
                    windowState = windowState,
                    name = playlist.name,
                    onDismiss = { renamedPlaylist = null },
                    onCreate = { viewModel.onPlaylistRename(playlist.id, it) },
                )
            }
        }
    } else {
        OldJukeboxWindow(
            onCloseClick = onCloseRequest,
            onFinish = {
                windowState.windowState.position = it
                viewModel.onJukeboxRevealed()
            },
        )
    }
}

private fun chooseFile(
    frame: Window,
    onFileChosen: (List<Path>) -> Unit,
) {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    val chooser = JFileChooser(null as String?)
    chooser.fileSelectionMode = JFileChooser.FILES_ONLY
    chooser.fileFilter = FileNameExtensionFilter("MP3 files", "mp3")
    chooser.isMultiSelectionEnabled = true
    val returnValue = chooser.showOpenDialog(frame)
    if (returnValue == JFileChooser.APPROVE_OPTION) {
        onFileChosen(chooser.selectedFiles.map { it.toPath() })
    }
}

@Composable
private fun WindowScope.PlaylistDialog(
    windowState: RiftWindowState,
    name: String,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    RiftDialog(
        title = "Enter Playlist Name",
        icon = Res.drawable.window_question,
        parentState = windowState,
        state = rememberWindowState(width = 300.dp, height = Dp.Unspecified),
        onCloseClick = onDismiss,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text(
                text = "Playlist Name",
                style = RiftTheme.typography.bodyPrimary,
            )
            var text by remember { mutableStateOf(name) }
            val focusRequested = remember { FocusRequester() }
            RiftTextField(
                text = text,
                onTextChanged = { text = it },
                modifier = Modifier
                    .focusRequester(focusRequested)
                    .fillMaxWidth(),
            )
            LaunchedEffect(Unit) {
                if (name.isEmpty()) focusRequested.requestFocus()
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                RiftButton(
                    text = "OK",
                    cornerCut = ButtonCornerCut.BottomLeft,
                    onClick = {
                        onCreate(text)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                )
                RiftButton(
                    text = "Cancel",
                    type = ButtonType.Secondary,
                    cornerCut = ButtonCornerCut.BottomRight,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun JukeboxWindowContent(
    state: UiState,
    isExpanded: Boolean,
    onExpandedToggleClick: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onTrackClick: (UUID) -> Unit,
    onTrackDoubleClick: (UUID) -> Unit,
    onTrackRemove: (UUID) -> Unit,
    onPlaylistClick: (UUID) -> Unit,
    onPlayPlaylistClick: (UUID) -> Unit,
    onRenamePlaylistClick: (Playlist) -> Unit,
    onDeletePlaylistClick: (UUID) -> Unit,
    onNewPlaylistClick: () -> Unit,
    onAddTracksClick: () -> Unit,
) {
    Column {
        Box(
            modifier = Modifier.padding(horizontal = Spacing.medium),
        ) {
            ControlBar(
                state = state,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                onPlayClick = onPlayClick,
                onShuffleClick = onShuffleClick,
                onVolumeChange = onVolumeChange,
            )
        }

        RiftImageButton(
            resource = if (isExpanded) Res.drawable.collapse else Res.drawable.expand,
            size = 16.dp,
            onClick = onExpandedToggleClick,
            modifier = Modifier.padding(Spacing.medium),
        )
        if (isExpanded) {
            Divider(color = RiftTheme.colors.divider)
            Row {
                Playlists(
                    playlists = state.playlists,
                    currentPlaylistId = state.currentPlaylist,
                    onPlaylistClick = onPlaylistClick,
                    onPlayPlaylistClick = onPlayPlaylistClick,
                    onRenamePlaylistClick = onRenamePlaylistClick,
                    onDeletePlaylistClick = onDeletePlaylistClick,
                    onNewPlaylistClick = onNewPlaylistClick,
                )
                VerticalDivider(color = RiftTheme.colors.borderGrey)
                val currentPlaylist = remember(state.playlists, state.currentPlaylist) {
                    state.playlists.firstOrNull { it.id == state.currentPlaylist }
                }
                val tracksInPlaylist = remember(currentPlaylist, state.tracks) {
                    val trackIds = currentPlaylist?.tracks ?: emptyList()
                    state.tracks.filter { it.id in trackIds }
                }
                Tracks(
                    playlist = currentPlaylist,
                    tracks = tracksInPlaylist,
                    selectedTrack = state.selectedTrack,
                    currentTrack = state.currentTrack,
                    onTrackClick = onTrackClick,
                    onTrackDoubleClick = onTrackDoubleClick,
                    onTrackRemove = onTrackRemove,
                    onAddTracksClick = onAddTracksClick,
                )
            }
        }
    }
}

@Composable
private fun ControlBar(
    state: UiState,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onVolumeChange: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .background(RiftTheme.colors.windowBackgroundSecondary)
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            val percentage = state.currentTrack?.duration?.let { duration ->
                state.playDuration.toMillis() / duration.toMillis().toFloat()
            }
            if (percentage != null) {
                Box(
                    modifier = Modifier
                        .background(RiftTheme.colors.windowBackgroundSecondaryHovered)
                        .fillMaxWidth(percentage)
                        .fillMaxHeight(),
                )
            }
            Row(
                modifier = Modifier
                    .padding(Spacing.small)
                    .fillMaxWidth(),
            ) {
                val durationText = when (state.playState) {
                    is Playing -> state.playDuration.format()
                    is Paused -> state.playDuration.format()
                    is Stopped -> "00:00"
                }
                Text(
                    text = durationText,
                    style = RiftTheme.typography.bodyPrimary,
                    maxLines = 1,
                )
                Spacer(Modifier.width(25.dp))
                Text(
                    text = state.currentTrack?.title ?: "",
                    style = RiftTheme.typography.bodyPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RiftTooltipArea(
                text = "Play Previous Track",
            ) {
                RiftIconButton(
                    icon = Res.drawable.jukebox_previous,
                    cornerCut = ButtonCornerCut.BottomLeft,
                    onClick = onPreviousClick,
                )
            }
            RiftTooltipArea(
                text = if (state.playState is Playing) "Pause" else "Play",
            ) {
                RiftIconButton(
                    icon = if (state.playState is Playing) Res.drawable.jukebox_pause else Res.drawable.jukebox_play,
                    onClick = onPlayClick,
                    cornerCut = ButtonCornerCut.None,
                    modifier = Modifier.padding(start = Spacing.small),
                )
            }
            RiftTooltipArea(
                text = "Play Next Track",
            ) {
                RiftIconButton(
                    icon = Res.drawable.jukebox_next,
                    onClick = onNextClick,
                    cornerCut = ButtonCornerCut.BottomRight,
                    modifier = Modifier.padding(start = Spacing.small),
                )
            }
            RiftTooltipArea(
                text = if (state.isShuffling) "Turn Shuffle Off" else "Turn Shuffle On",
            ) {
                RiftImageButton(
                    resource = Res.drawable.jukebox_shuffle,
                    size = 24.dp,
                    onClick = onShuffleClick,
                    tint = if (state.isShuffling) Color(0xFFA5DA88) else Color.White,
                    modifier = Modifier.padding(start = Spacing.mediumLarge),
                )
            }
            Spacer(Modifier.weight(1f))
            Volume(state.volume, onVolumeChange)
            Spacer(Modifier.width(Spacing.medium))
        }
    }
}

private fun Duration.format(): String {
    return String.format("%02d:%02d", toMinutes(), toSecondsPart())
}

@Composable
private fun Volume(
    volume: Int,
    onVolumeChange: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        RiftTooltipArea(
            text = "Volume Down",
        ) {
            RiftImageButton(
                resource = if (volume == 0) Res.drawable.jukebox_volume_muted else Res.drawable.jukebox_volume_down,
                size = 24.dp,
                onClick = { onVolumeChange(0) },
            )
        }
        RiftSlider(
            width = 100.dp,
            range = 0..100,
            currentValue = volume,
            onValueChange = { onVolumeChange(it) },
        )
        RiftTooltipArea(
            text = "Volume Up",
        ) {
            RiftImageButton(
                resource = Res.drawable.jukebox_volume_up,
                size = 24.dp,
                onClick = { onVolumeChange(100) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Playlists(
    playlists: List<Playlist>,
    currentPlaylistId: UUID?,
    onPlaylistClick: (UUID) -> Unit,
    onPlayPlaylistClick: (UUID) -> Unit,
    onRenamePlaylistClick: (Playlist) -> Unit,
    onDeletePlaylistClick: (UUID) -> Unit,
    onNewPlaylistClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(Spacing.medium),
    ) {
        ScrollbarColumn(
            isScrollbarConditional = true,
            modifier = Modifier
                .width(150.dp)
                .weight(1f),
        ) {
            for (playlist in playlists.sortedWith(compareBy({ !it.isBuiltIn }, { it.name }))) {
                val isSelected = playlist.id == currentPlaylistId
                RiftContextMenuArea(
                    items = buildList {
                        add(ContextMenuItem.TextItem(text = "Play", onClick = { onPlayPlaylistClick(playlist.id) }))
                        if (!playlist.isBuiltIn) {
                            add(ContextMenuItem.TextItem(text = "Delete", onClick = { onDeletePlaylistClick(playlist.id) }))
                            add(ContextMenuItem.TextItem(text = "Rename", onClick = { onRenamePlaylistClick(playlist) }))
                        }
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .hoverBackground(
                                normalColor = if (isSelected) RiftTheme.colors.backgroundHovered else null,
                            )
                            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                            .onClick { onPlaylistClick(playlist.id) }
                            .padding(Spacing.small)
                            .fillMaxWidth(),
                    ) {
                        Text(
                            text = playlist.name,
                            style = RiftTheme.typography.bodyPrimary,
                        )
                    }
                }
            }
        }
        RiftButton(
            text = "New",
            isCompact = true,
            onClick = onNewPlaylistClick,
        )
    }
}

@Composable
private fun Tracks(
    playlist: Playlist?,
    tracks: List<Track>,
    selectedTrack: UUID?,
    currentTrack: Track?,
    onTrackClick: (UUID) -> Unit,
    onTrackDoubleClick: (UUID) -> Unit,
    onTrackRemove: (UUID) -> Unit,
    onAddTracksClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(Spacing.medium),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
        ) {
            if (tracks.isNotEmpty()) {
                Column {
                    var hasScrollbar by remember { mutableStateOf(false) }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.modifyIf(hasScrollbar) { padding(end = 8.dp) },
                    ) {
                        Box(Modifier.width(32.dp))
                        Text(
                            text = "Number",
                            style = RiftTheme.typography.bodySecondary,
                            maxLines = 1,
                            modifier = Modifier.width(50.dp),
                        )
                        Text(
                            text = "Title",
                            style = RiftTheme.typography.bodySecondary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "Duration",
                            style = RiftTheme.typography.bodySecondary,
                            maxLines = 1,
                            modifier = Modifier.width(70.dp),
                        )
                    }

                    val rowHeight = 24.dp
                    val rowHeightPx = LocalDensity.current.run { rowHeight.toPx().toInt() }
                    val scrollState = rememberScrollState()
                    LaunchedEffect(playlist) {
                        scrollState.scrollTo(0)
                    }

                    LaunchedEffect(currentTrack) {
                        if (currentTrack != null) {
                            val offset = playlist?.tracks?.indexOf(currentTrack.id)?.let { rowHeightPx * it }?.toInt()
                            if (offset != null) {
                                if (offset < (scrollState.value + rowHeightPx)) {
                                    scrollState.scrollTo(offset.toInt() - (3 * rowHeightPx))
                                } else if (offset > (scrollState.value + scrollState.viewportSize - rowHeightPx)) {
                                    scrollState.scrollTo(offset.toInt() - scrollState.viewportSize + (3 * rowHeightPx))
                                }
                            }
                        }
                    }

                    ScrollbarColumn(
                        scrollState = scrollState,
                        isScrollbarConditional = true,
                        onHasScrollbarChange = { hasScrollbar = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        for ((index, track) in tracks.withIndex()) {
                            RiftContextMenuArea(
                                items = buildList {
                                    if (playlist?.isBuiltIn == false) {
                                        add(ContextMenuItem.TextItem("Remove From Playlist", onClick = { onTrackRemove(track.id) }))
                                    }
                                },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                    modifier = Modifier
                                        .hoverBackground(
                                            normalColor = if (track.id == selectedTrack) RiftTheme.colors.backgroundHovered else null,
                                        )
                                        .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                                        .pointerInput(track) {
                                            detectTapGestures(
                                                onPress = { onTrackClick(track.id) },
                                                onDoubleTap = { onTrackDoubleClick(track.id) },
                                            )
                                        }
                                        .height(rowHeight),
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.width(32.dp),
                                    ) {
                                        val alpha by animateFloatAsState(if (track.id == currentTrack?.id) 1f else 0f)
                                        Image(
                                            painter = painterResource(Res.drawable.play),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .alpha(alpha)
                                                .size(16.dp),
                                        )
                                    }
                                    Text(
                                        text = "${index + 1}",
                                        style = RiftTheme.typography.bodyPrimary,
                                        maxLines = 1,
                                        modifier = Modifier.width(50.dp),
                                    )
                                    Text(
                                        text = track.title,
                                        style = RiftTheme.typography.bodyPrimary,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = track.duration.format(),
                                        style = RiftTheme.typography.bodyPrimary,
                                        maxLines = 1,
                                        modifier = Modifier.width(70.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "No Tracks on Playlist",
                    style = RiftTheme.typography.headlineSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = Spacing.large, start = Spacing.large),
                )
            }
        }
        RiftButton(
            text = "Add Tracks",
            isCompact = true,
            isEnabled = playlist?.isBuiltIn == false,
            onClick = onAddTracksClick,
        )
    }
}
