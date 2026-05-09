package dev.nohus.rift.jukebox

import dev.nohus.rift.ViewModel
import dev.nohus.rift.jukebox.data.TracksRepository
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.sound.Mp3Player
import dev.nohus.rift.utils.sound.getMp3FileDuration
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import java.nio.file.Path
import java.time.Duration
import java.util.UUID
import kotlin.io.path.nameWithoutExtension

@Factory
class JukeboxViewModel(
    private val tracksRepository: TracksRepository,
    private val player: Mp3Player,
    private val windowManager: WindowManager,
    private val settings: Settings,
) : ViewModel() {

    data class UiState(
        val isRevealed: Boolean = false,
        val volume: Int = 50,
        val playlists: List<Playlist> = emptyList(),
        val currentPlaylist: UUID? = null,
        val tracks: List<Track> = emptyList(),
        val selectedTrack: UUID? = null,
        val currentTrack: Track? = null,
        val playState: PlayState = PlayState.Stopped,
        val playDuration: Duration = Duration.ZERO,
        val isShuffling: Boolean = false,
    )

    sealed class PlayState(open val track: Track?) {
        data class Playing(override val track: Track) : PlayState(track)
        data class Paused(override val track: Track) : PlayState(track)
        data object Stopped : PlayState(null)
    }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        _state.update { it.copy(isRevealed = settings.isJukeboxRevealed) }
        onVolumeChange(100)
        viewModelScope.launch {
            tracksRepository.tracks.collect { tracks ->
                _state.update {
                    it.copy(
                        playlists = tracks.playlists,
                        tracks = tracks.tracks,
                        currentPlaylist = it.currentPlaylist ?: tracks.playlists.firstOrNull()?.id,
                        selectedTrack = it.selectedTrack ?: tracks.tracks.firstOrNull()?.id,
                    )
                }
            }
        }
    }

    fun onExpandedToggleClick(isExpanded: Boolean) {
        if (isExpanded) {
            windowManager.onWindowOpen(RiftWindow.Jukebox)
            windowManager.onWindowClose(RiftWindow.JukeboxCollapsed, null)
        } else {
            windowManager.onWindowOpen(RiftWindow.JukeboxCollapsed)
            windowManager.onWindowClose(RiftWindow.Jukebox, null)
        }
    }

    fun onVolumeChange(volume: Int) {
        _state.update { it.copy(volume = volume) }
        player.setVolume(volume / 100.0)
    }

    fun onPreviousClick() {
        if (_state.value.isShuffling) {
            playRandom()
            return
        }

        val tracks = getTracksInCurrentPlaylist()
        if (tracks.isEmpty()) {
            _state.update { it.copy(currentTrack = null) }
            return
        }
        val currentTrack = _state.value.currentTrack
        val previousIndex = if (currentTrack != null) {
            val currentIndex = tracks.indexOfFirst { it.id == currentTrack.id }
            (currentIndex - 1).takeUnless { it < 0 } ?: tracks.lastIndex
        } else {
            0
        }
        val track = tracks[previousIndex]
        play(track)
    }

    fun onNextClick() {
        if (_state.value.isShuffling) {
            playRandom()
            return
        }

        val tracks = getTracksInCurrentPlaylist()
        if (tracks.isEmpty()) {
            _state.update { it.copy(currentTrack = null) }
            return
        }
        val currentTrack = _state.value.currentTrack
        val nextIndex = if (currentTrack != null) {
            val currentIndex = tracks.indexOfFirst { it.id == currentTrack.id }
            (currentIndex + 1).takeUnless { it > tracks.lastIndex } ?: 0
        } else {
            0
        }
        val track = tracks[nextIndex]
        play(track)
    }

    private fun playRandom() {
        val tracks = getTracksInCurrentPlaylist()
        if (tracks.isEmpty()) {
            _state.update { it.copy(currentTrack = null) }
            return
        }
        play(tracks.random())
    }

    private fun getTracksInCurrentPlaylist(): List<Track> {
        val playlistId = _state.value.currentPlaylist ?: return emptyList()
        val currentPlaylist = _state.value.playlists.firstOrNull { it.id == playlistId } ?: return emptyList()
        return _state.value.tracks.filter { it.id in currentPlaylist.tracks }
    }

    fun onPlayClick() {
        val selected = _state.value.tracks.firstOrNull { it.id == _state.value.selectedTrack }
        when (val playState = _state.value.playState) {
            is PlayState.Playing -> {
                pause()
            }
            is PlayState.Stopped -> {
                if (selected != null) {
                    play(selected)
                }
            }
            is PlayState.Paused -> {
                if (selected != null) {
                    if (playState.track == selected) {
                        resume()
                    } else {
                        play(selected)
                    }
                }
            }
        }
    }

    fun onShuffleClick() {
        val new = !_state.value.isShuffling
        _state.update { it.copy(isShuffling = new) }
    }

    fun onTrackClick(id: UUID) {
        _state.update { it.copy(selectedTrack = id) }
    }

    fun onTrackDoubleClick(id: UUID) {
        val track = _state.value.tracks.firstOrNull { it.id == id } ?: return
        play(track)
    }

    fun onTrackRemove(id: UUID) {
        tracksRepository.removeTrack(id)
    }

    fun onPlaylistClick(id: UUID) {
        val playlist = _state.value.playlists.firstOrNull { it.id == id } ?: return
        openPlaylist(playlist)
    }

    fun onPlayPlaylistClick(id: UUID) {
        val track = _state.value.currentTrack
        val playlistId = _state.value.currentPlaylist
        val playlist = _state.value.playlists.firstOrNull { it.id == playlistId }
        if (playlist != null && track != null && id == playlistId && track.id in playlist.tracks) {
            // Already playing this playlist, restart track
            play(track)
            return
        }

        onPlaylistClick(id)
        onNextClick()
    }

    fun onDeletePlaylistClick(id: UUID) {
        tracksRepository.removePlaylist(id)
        if (id == _state.value.currentPlaylist) {
            openPlaylist(_state.value.playlists.first())
        }
    }

    fun onPlaylistRename(id: UUID, name: String) {
        tracksRepository.renamePlaylist(id, name)
    }

    fun onPlaylistCreate(name: String) {
        tracksRepository.addPlaylist(name)
    }

    fun onFilesChosen(files: List<Path>) {
        val playlistId = _state.value.currentPlaylist ?: return
        val tracks = files.map { file ->
            Track(
                id = UUID.randomUUID(),
                title = file.nameWithoutExtension,
                duration = getMp3FileDuration(file),
                source = Source.File(file),
            )
        }
        tracksRepository.addTracks(tracks, playlistId)
    }

    fun onJukeboxRevealed() {
        _state.update { it.copy(isRevealed = true) }
        settings.isJukeboxRevealed = true
        onPlayClick()
    }

    private fun openPlaylist(playlist: Playlist) {
        val currentTrack = _state.value.currentTrack
        val selectedTrack = if (currentTrack?.id in playlist.tracks) {
            currentTrack
        } else {
            null
        }
        _state.update { it.copy(currentPlaylist = playlist.id, selectedTrack = selectedTrack?.id) }
    }

    private fun play(track: Track) {
        _state.update {
            it.copy(
                selectedTrack = track.id,
                currentTrack = track,
                playState = PlayState.Playing(track),
                playDuration = Duration.ZERO,
            )
        }
        player.play(
            source = track.source,
            onPositionUpdated = { millis ->
                if (_state.value.currentTrack == track) {
                    _state.update { it.copy(playDuration = Duration.ofMillis(millis.toLong())) }
                }
            },
            onFinished = {
                val playState = _state.value.playState
                if (playState is PlayState.Playing && playState.track == track) {
                    _state.update { it.copy(playState = PlayState.Stopped) }
                    onNextClick()
                }
            },
        )
    }

    private fun resume() {
        (_state.value.playState as? PlayState.Paused)?.let { playing ->
            _state.update { it.copy(playState = PlayState.Playing(playing.track)) }
            player.resume()
        }
    }

    private fun pause() {
        (_state.value.playState as? PlayState.Playing)?.let { playing ->
            _state.update { it.copy(playState = PlayState.Paused(playing.track)) }
            player.pause()
        }
    }

    override fun onClose() {
        player.close()
    }
}
