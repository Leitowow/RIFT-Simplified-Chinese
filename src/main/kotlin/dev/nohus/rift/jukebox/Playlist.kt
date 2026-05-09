package dev.nohus.rift.jukebox

import java.util.UUID

data class Playlist(
    val id: UUID,
    val name: String,
    val tracks: List<UUID>,
    val isBuiltIn: Boolean = false,
)
