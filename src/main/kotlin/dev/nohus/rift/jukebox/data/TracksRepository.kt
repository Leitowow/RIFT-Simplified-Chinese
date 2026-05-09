package dev.nohus.rift.jukebox.data

import dev.nohus.rift.jukebox.Playlist
import dev.nohus.rift.jukebox.Source
import dev.nohus.rift.jukebox.Track
import dev.nohus.rift.utils.directories.AppDirectories
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.FileSystemException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.time.Duration
import java.util.UUID
import kotlin.io.path.absolutePathString
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val logger = KotlinLogging.logger {}

@Single
class TracksRepository(
    appDirectories: AppDirectories,
    @Named("settings") private val json: Json,
) {

    private val jukeboxFile = appDirectories.getAppDataDirectory().resolve("jukebox.json")
    private val scope = CoroutineScope(Job())
    private val mutex = Mutex()

    data class TracksState(
        val tracks: List<Track>,
        val playlists: List<Playlist>,
    )

    init {
        jukeboxFile.createParentDirectories()
    }

    private val _tracks = MutableStateFlow(load())
    val tracks = _tracks.asStateFlow()

    fun addPlaylist(name: String) {
        val playlist = Playlist(UUID.randomUUID(), name, emptyList())
        _tracks.update { it.copy(playlists = it.playlists + playlist) }
        save()
    }

    fun removePlaylist(id: UUID) {
        val playlist = _tracks.value.playlists.firstOrNull { it.id == id } ?: return
        val tracks = _tracks.value.tracks.filter { it.id in playlist.tracks }.map { it.id }
        _tracks.update {
            it.copy(
                playlists = it.playlists.filter { it.id != id },
                tracks = it.tracks.filter { it.id !in tracks },
            )
        }
        save()
    }

    fun renamePlaylist(id: UUID, newName: String) {
        _tracks.update { it.copy(playlists = it.playlists.map { if (it.id == id) it.copy(name = newName) else it }) }
        save()
    }

    fun addTracks(tracks: List<Track>, playlistId: UUID) {
        _tracks.update {
            it.copy(
                tracks = it.tracks + tracks,
                playlists = it.playlists.map { playlist ->
                    if (playlist.id == playlistId) playlist.copy(tracks = playlist.tracks + tracks.map { it.id }) else playlist
                },
            )
        }
        save()
    }

    fun removeTrack(id: UUID) {
        _tracks.update { it.copy(tracks = it.tracks.filter { it.id != id }) }
        save()
    }

    private fun load(): TracksState {
        val model = try {
            val serialized = jukeboxFile.readText()
            json.decodeFromString<JukeboxModel>(serialized)
        } catch (e: NoSuchFileException) {
            logger.info { "Jukebox file not found" }
            JukeboxModel()
        } catch (e: FileSystemException) {
            logger.error(e) { "Jukebox file could not be read" }
            jukeboxFile.deleteIfExists()
            JukeboxModel()
        } catch (e: SerializationException) {
            logger.error(e) { "Could not deserialize jukebox" }
            jukeboxFile.deleteIfExists()
            JukeboxModel()
        }

        return TracksState(
            tracks = getTracks(model),
            playlists = getPlaylists(model),
        )
    }

    private fun save() = scope.launch(Dispatchers.IO) {
        mutex.withLock {
            val state = _tracks.value
            val model = JukeboxModel(
                playlists = state.playlists.mapNotNull {
                    if (it.isBuiltIn) return@mapNotNull null
                    PlaylistModel(it.id, it.name, it.tracks)
                },
                tracks = state.tracks.mapNotNull {
                    val path = (it.source as? Source.File)?.path?.absolutePathString() ?: return@mapNotNull null
                    TrackModel(it.id, it.title, it.duration.seconds, path)
                },
            )
            try {
                val serialized = json.encodeToString(model)
                jukeboxFile.writeText(serialized)
            } catch (e: FileSystemException) {
                logger.error { "Could not write jukebox: $e" }
            } catch (e: IOException) {
                logger.error { "Could not write jukebox: $e" }
            }
        }
    }

    private fun getTracks(model: JukeboxModel): List<Track> {
        return NetworkTracks.soundtrack +
            NetworkTracks.trailer +
            NetworkTracks.permaband +
            model.tracks.map {
                Track(
                    id = it.id,
                    title = it.title,
                    duration = Duration.ofSeconds(it.durationSeconds),
                    source = Source.File(Path.of(it.path)),
                )
            }
    }

    private fun getPlaylists(model: JukeboxModel): List<Playlist> {
        return listOf(
            Playlist(UUID.randomUUID(), "EVE Soundtrack", tracks = NetworkTracks.soundtrack.map { it.id }, isBuiltIn = true),
            Playlist(UUID.randomUUID(), "EVE Trailer Music", tracks = NetworkTracks.trailer.map { it.id }, isBuiltIn = true),
            Playlist(UUID.randomUUID(), "Permaband", tracks = NetworkTracks.permaband.map { it.id }, isBuiltIn = true),
        ) + model.playlists.map {
            Playlist(
                id = it.id,
                name = it.name,
                tracks = it.tracks,
            )
        }
    }
}
