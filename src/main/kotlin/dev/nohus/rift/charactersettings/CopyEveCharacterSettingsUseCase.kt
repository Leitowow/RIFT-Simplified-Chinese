package dev.nohus.rift.charactersettings

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isWritable
import kotlin.io.path.nameWithoutExtension

private val logger = KotlinLogging.logger {}

@Single
class CopyEveCharacterSettingsUseCase(
    private val localCharactersRepository: LocalCharactersRepository,
    private val accountAssociationsRepository: AccountAssociationsRepository,
    private val getAccounts: GetAccountsUseCase,
) {

    operator fun invoke(fromCharacterId: Int, toCharacterIds: List<Int>): Boolean {
        val characters = localCharactersRepository.characters.value
        val accounts = getAccounts()
        val accountAssociations = accountAssociationsRepository.getAssociations()

        val fromAccountId = accountAssociations[fromCharacterId] ?: run {
            logger.error { "Source character doesn't have an account set" }
            return false
        }
        val toAccountIds = toCharacterIds.map { toCharacterId ->
            accountAssociations[toCharacterId] ?: run {
                logger.error { "Target character doesn't have an account set" }
                return false
            }
        }
        val fromCharacterFile = characters.firstOrNull {
            it.characterId == fromCharacterId
        }?.settingsFile ?: run {
            logger.error { "Source character doesn't have a settings file set" }
            return false
        }
        val toCharacterFiles = toCharacterIds.map { toCharacterId ->
            characters.firstOrNull { it.characterId == toCharacterId }?.settingsFile ?: run {
                logger.error { "Target character doesn't have a settings file set" }
                return false
            }
        }
        val fromAccountFile = accounts.firstOrNull { it.id == fromAccountId }?.path ?: run {
            logger.error { "Source account doesn't have a settings file" }
            return false
        }
        val toAccountFiles = toAccountIds.map { toAccountId ->
            accounts.firstOrNull { it.id == toAccountId }?.path ?: run {
                logger.error { "Target account doesn't have a settings file" }
                return false
            }
        }

        if (!replicate(fromCharacterFile, toCharacterFiles)) return false
        if (!replicate(fromAccountFile, toAccountFiles)) return false
        return true
    }

    private fun replicate(
        fromFile: Path,
        toFiles: List<Path>,
    ): Boolean {
        try {
            if (!fromFile.exists()) {
                logger.error { "Source settings file does not exist" }
                return false
            }
            if (toFiles.any { !it.exists() }) {
                logger.error { "Target settings file does not exist" }
                return false
            }
            val directory = fromFile.parent
            if (toFiles.any { it.parent != directory }) {
                logger.error { "Settings files are not in the same directory" }
                return false
            }
            if (!directory.isWritable()) {
                logger.error { "Settings directory is not writeable" }
                return false
            }

            toFiles.filter { it != fromFile }.forEach { file ->
                val backup = getNewBackupFile(directory, file)
                file.copyTo(backup)
                file.deleteIfExists()
                fromFile.copyTo(file)
            }

            return true
        } catch (e: IOException) {
            logger.error(e) { "Copying settings failed" }
            return false
        }
    }

    private fun getNewBackupFile(directory: Path, file: Path): Path {
        var count = 1
        while (true) {
            val backup = directory.resolve("${file.nameWithoutExtension}_rift_backup_$count.${file.extension}")
            if (!backup.exists()) return backup
            count++
        }
    }
}
