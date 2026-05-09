package dev.nohus.rift.logs

import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.moveTo

private val logger = KotlinLogging.logger {}

@Single
class RotateLogsUseCase(
    private val settings: Settings,
    private val getChatLogsDirectoryUseCase: GetChatLogsDirectoryUseCase,
    private val getGameLogsDirectoryUseCase: GetGameLogsDirectoryUseCase,
) {

    private val oldLogsAge = Duration.ofDays(7)
    private val cutoffTimestamp = Instant.now() - oldLogsAge
    private val oldDirectoryName = "old"

    operator fun invoke() {
        val logsDirectory = settings.eveLogsDirectory
        val chatLogsDirectory = getChatLogsDirectoryUseCase(logsDirectory)
        val gameLogsDirectory = getGameLogsDirectoryUseCase(logsDirectory)

        if (chatLogsDirectory != null) {
            rotateLogs(chatLogsDirectory) {
                it.extension == "txt"
            }
        }
        if (gameLogsDirectory != null) {
            rotateLogs(gameLogsDirectory) {
                it.extension == "txt"
            }
        }
    }

    private fun rotateLogs(directory: Path, filter: (Path) -> Boolean) {
        try {
            val logFiles = directory
                .listDirectoryEntries()
                .filter { it.getLastModifiedTime().toInstant().isBefore(cutoffTimestamp) }
                .filter(filter)
            if (logFiles.isEmpty()) return
            logger.info { "Rotating old logs in ${directory.fileName} directory: ${logFiles.size}" }
            val oldDirectory = directory.resolve(oldDirectoryName)
            oldDirectory.createDirectories()
            logFiles.forEach { file ->
                file.moveTo(oldDirectory.resolve(file.fileName))
            }
        } catch (e: IOException) {
            logger.error(e) { "Failed rotating old log files" }
        }
    }
}
