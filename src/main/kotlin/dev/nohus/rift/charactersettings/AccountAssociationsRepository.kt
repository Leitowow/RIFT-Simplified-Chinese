package dev.nohus.rift.charactersettings

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.loglite.Client
import dev.nohus.rift.loglite.ClientLogLiteAction
import dev.nohus.rift.loglite.LogLiteAction
import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.nameWithoutExtension

private val logger = KotlinLogging.logger {}

@Single
class AccountAssociationsRepository(
    private val settings: Settings,
    private val localCharactersRepository: LocalCharactersRepository,
    private val getAccounts: GetAccountsUseCase,
) {

    private val clientAccountId = mutableMapOf<Client, Int>()

    fun onCharacterLogin(characterId: Int) {
        val now = Instant.now()
        val character = localCharactersRepository.characters.value.firstOrNull { it.characterId == characterId } ?: return
        val accountId = getAccounts().flatMap { it.paths.values }
            .map { accountSettingsFile ->
                val accountLastModified = accountSettingsFile.getLastModifiedTime().toInstant()
                accountSettingsFile to Duration.between(accountLastModified, now)
            }.singleOrNull { (_, age) -> age < Duration.ofSeconds(10) }
            ?.first?.nameWithoutExtension?.substringAfterLast("_")?.toIntOrNull() ?: return
        if (settings.accountAssociations[characterId] != accountId) {
            settings.accountAssociations += characterId to accountId
            logger.info { "Set account association for character ${character.info?.name} to $accountId." }
        }
    }

    fun onLogLiteAction(clientAction: ClientLogLiteAction) {
        when (val action = clientAction.action) {
            is LogLiteAction.AccountId -> clientAccountId[clientAction.client] = action.id
            is LogLiteAction.CharacterId -> clientAccountId[clientAction.client]?.let { accountId ->
                associate(action.id, accountId)
            }
            else -> {}
        }
    }

    /**
     * Returns a map of Character ID -> Account ID
     */
    fun getAssociations(): Map<Int, Int> = settings.accountAssociations

    fun associate(characterId: Int, accountId: Int) {
        settings.accountAssociations += characterId to accountId
    }
}
