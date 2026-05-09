package dev.nohus.rift.logs

import kotlinx.coroutines.delay
import org.koin.core.annotation.Single
import java.io.File
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

@Single
class WindowsFilePoker {

    private val recentAge = Duration.ofDays(7)
    private val pokeAge = Duration.ofHours(2)
    private val rescanFilesToPokeDuration = Duration.ofSeconds(20)

    /**
     * On Windows the file modification events are not consistently delivered.
     * Asking for the file size triggers Windows to deliver the event.
     */
    suspend fun pokeFiles(path: Path) {
        val ignoredFiles = mutableListOf<File>()
        while (true) {
            val minTime = (Instant.now() - recentAge).toEpochMilli()
            val allFiles = path.toFile().listFiles() ?: emptyArray()
            val nonIgnoredFiles = allFiles.filter { it !in ignoredFiles }
            val (recentFiles, newIgnoredFiles) = nonIgnoredFiles.partition { it.lastModified() > minTime }
            ignoredFiles += newIgnoredFiles
            pokeRecentFiles(recentFiles)
        }
    }

    /**
     * Recent files are files that were modified in the last 7 days
     */
    private suspend fun pokeRecentFiles(recentFiles: List<File>) {
        val minTimeToPoke = (Instant.now() - pokeAge).toEpochMilli()
        val filesToPoke = recentFiles.filter { it.lastModified() > minTimeToPoke }
        pokeFilesToPoke(filesToPoke)
    }

    /**
     * Files to poke are files that were modified in the last 2 hours
     */
    private suspend fun pokeFilesToPoke(filesToPoke: List<File>) {
        val pokeUntil = Instant.now() + rescanFilesToPokeDuration
        while (Instant.now() < pokeUntil) {
            filesToPoke.forEach { it.length() }
            delay(200)
        }
    }
}
