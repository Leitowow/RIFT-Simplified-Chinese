package dev.nohus.rift.charactersettings

import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.extension
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

private val logger = KotlinLogging.logger {}

@Single
class GetAccountsUseCase(
    private val settings: Settings,
) {

    private val regex = """core_user_[0-9]+""".toRegex()

    data class Account(
        val id: Int,
        val paths: Map<String, Path>, // Launcher profile name -> File
        val lastModified: Instant,
    )

    operator fun invoke(): List<Account> {
        val directory = settings.eveSettingsDirectory ?: return emptyList()
        return try {
            directory.listDirectoryEntries()
                .filter { file ->
                    file.isDirectory() && file.name.startsWith("settings_") && "backup" !in file.name
                }.flatMap { directory ->
                    directory.listDirectoryEntries()
                        .filter { file -> file.isRegularFile() && file.extension == "dat" }
                        .filter { file -> file.nameWithoutExtension.matches(regex) }
                }.groupBy { accountFile ->
                    accountFile.nameWithoutExtension.substringAfterLast("_").toIntOrNull()
                }.mapNotNull { (id, accountFiles) ->
                    id ?: return@mapNotNull null
                    val lastModified = accountFiles.maxOf { it.getLastModifiedTime() }.toInstant()
                    val paths = accountFiles.associateBy { accountFile ->
                        accountFile.parent.name.substringAfter("settings_")
                    }
                    Account(id, paths, lastModified)
                }
        } catch (e: IOException) {
            logger.error(e) { "Failed reading account settings" }
            emptyList()
        }
    }
}
