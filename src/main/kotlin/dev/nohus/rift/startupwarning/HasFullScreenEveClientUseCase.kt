package dev.nohus.rift.startupwarning

import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.FileSystemException
import kotlin.io.path.absolutePathString
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText

private val logger = KotlinLogging.logger {}

@Single
class HasFullScreenEveClientUseCase(
    private val settings: Settings,
) {

    suspend operator fun invoke(): Boolean {
        return withContext(Dispatchers.IO) {
            hasFullScreenEveClient()
        }
    }

    private fun hasFullScreenEveClient(): Boolean {
        if (!settings.isSetupWizardFinished) return false
        val dir = settings.eveSettingsDirectory ?: return false
        return try {
            dir.listDirectoryEntries()
                .filter { file ->
                    file.isDirectory() && file.name.startsWith("settings_")
                }.flatMap { directory ->
                    directory.listDirectoryEntries()
                        .filter { file -> file.isRegularFile() && file.name == "core_public__.yaml" }
                }.mapNotNull { prefsFile ->
                    try {
                        prefsFile.readText().lines().forEach {
                            if (it.startsWith("  WindowMode")) {
                                val mode = it.substringAfter("[").substringBefore("]")
                                    .substringAfter(", ").toIntOrNull()
                                if (mode == 0) {
                                    logger.warn { "Client is in fullscreen mode" }
                                    return true
                                } else {
                                    return false
                                }
                            }
                        }
                        return false
                    } catch (_: IOException) {
                        logger.info { "Could not read preferences file: ${prefsFile.absolutePathString()}" }
                        return@mapNotNull null
                    }
                }.any()
        } catch (e: FileSystemException) {
            logger.error(e) { "Failed reading EVE client settings" }
            false
        }
    }
}
