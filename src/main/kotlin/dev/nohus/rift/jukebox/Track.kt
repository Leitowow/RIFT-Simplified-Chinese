package dev.nohus.rift.jukebox

import java.nio.file.Path
import java.time.Duration
import java.util.UUID

data class Track(
    val id: UUID,
    val title: String,
    val duration: Duration,
    val source: Source,
)

sealed interface Source {
    data class File(val path: Path) : Source
    data class Network(val url: String) : Source
}
