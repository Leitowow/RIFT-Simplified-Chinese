package dev.nohus.rift.startupwarning

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.charactersettings.AccountAssociationsRepository
import dev.nohus.rift.charactersettings.GetAccountsUseCase
import dev.nohus.rift.charactersettings.GetAccountsUseCase.Account
import dev.nohus.rift.network.AsyncResource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import okio.IOException
import org.koin.core.annotation.Single
import kotlin.io.path.readBytes
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

@Single
class GetAccountsWithDisabledChatLogsUseCase(
    private val getAccounts: GetAccountsUseCase,
    private val accountAssociationsRepository: AccountAssociationsRepository,
    private val localCharactersRepository: LocalCharactersRepository,
) {

    /**
     * Returns a list of info messages for each account with disabled chat logs
     */
    suspend operator fun invoke(): List<String> = coroutineScope {
        val accounts = getAccounts()
        val characters = async { getLoadedCharacters() }
        accounts.mapNotNull { account ->
            try {
                val bytes = account.path.readBytes()
                val indexOfLogChat = bytes.indexOf("logchat")
                if (indexOfLogChat != null) {
                    if (bytes[indexOfLogChat + CHAT_LOGS_DISABLED_BYTE_OFFSET].toInt() == CHAT_LOGS_DISABLED_BYTE_VALUE) {
                        getMessageForAccount(account, characters.await())
                    } else {
                        null // Chat logs are enabled
                    }
                } else {
                    null // The setting is not set, and defaults to enabled
                }
            } catch (e: IOException) {
                logger.warn { "Could not read settings file to determine if chat logs are enabled: ${e.message}" }
                null
            }
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun getLoadedCharacters(): List<LocalCharacter> {
        return try {
            localCharactersRepository.characters.filter { characters ->
                characters.all { it.info != AsyncResource.Loading }
            }.timeout(5.seconds).first()
        } catch (e: TimeoutCancellationException) {
            localCharactersRepository.characters.value
        }
    }

    private fun getMessageForAccount(account: Account, characters: List<LocalCharacter>): String {
        val accountAssociations = accountAssociationsRepository.getAssociations()
        val charactersOnAccount = characters.filter { character ->
            val characterAccountId = accountAssociations[character.characterId] ?: return@filter false
            characterAccountId == account.id
        }

        return buildString {
            val profile = account.profile.takeIf { it != "Default" }
            if (profile != null) {
                append("In profile $profile: ")
            }
            append("Account with ID ${account.id}")
            if (charactersOnAccount.isNotEmpty()) {
                val names = charactersOnAccount.mapNotNull { it.info.success?.name }
                if (names.isNotEmpty()) {
                    val characterNames = names.joinToString(", ")
                    append(" and characters $characterNames")
                } else {
                    val characterIds = charactersOnAccount.joinToString(", ") { it.characterId.toString() }
                    append(" and character IDs $characterIds")
                }
            }
        }
    }

    private fun ByteArray.indexOf(text: String): Int? {
        main@ for (index in indices) {
            for (characterIndex in text.indices) {
                val character = text[characterIndex].code.toByte()
                val byte = get(index + characterIndex)
                if (character != byte) continue@main
            }
            return index
        }
        return null
    }

    companion object {
        private const val CHAT_LOGS_DISABLED_BYTE_OFFSET = -3
        private const val CHAT_LOGS_DISABLED_BYTE_VALUE = 8
    }
}
