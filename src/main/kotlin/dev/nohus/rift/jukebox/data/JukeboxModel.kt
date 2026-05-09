package dev.nohus.rift.jukebox.data

import dev.nohus.rift.settings.persistence.UuidSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class JukeboxModel(
    val playlists: List<PlaylistModel> = emptyList(),
    val tracks: List<TrackModel> = emptyList(),
)

@Serializable
data class PlaylistModel(
    @Serializable(with = UuidSerializer::class)
    val id: UUID,
    val name: String,
    val tracks: List<
        @Serializable(with = UuidSerializer::class)
        UUID,
        >,
)

@Serializable
data class TrackModel(
    @Serializable(with = UuidSerializer::class)
    val id: UUID,
    val title: String,
    val durationSeconds: Long,
    val path: String,
)
