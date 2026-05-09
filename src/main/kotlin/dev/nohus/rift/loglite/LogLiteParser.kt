package dev.nohus.rift.loglite

import dev.nohus.rift.charactersettings.AccountAssociationsRepository
import dev.nohus.rift.game.AutopilotController
import dev.nohus.rift.loglite.LogLiteAction.AccountId
import dev.nohus.rift.loglite.LogLiteAction.AutopilotPath
import dev.nohus.rift.loglite.LogLiteAction.CharacterId
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class LogLiteParser(
    private val logLiteRepository: LogLiteRepository,
    private val accountAssociationsRepository: AccountAssociationsRepository,
    private val autopilotController: AutopilotController,
) {

    suspend fun start() = coroutineScope {
        launch {
            logLiteRepository.flow.collect { message ->
                handleMessage(message)
            }
        }
    }

    private fun handleMessage(logMessage: LogMessage) {
        val action = parseMessage(logMessage) ?: return
        val clientAction = ClientLogLiteAction(action, logMessage.client)
        accountAssociationsRepository.onLogLiteAction(clientAction)
        autopilotController.onLogLiteAction(clientAction)
    }

    private fun parseMessage(logMessage: LogMessage): LogLiteAction? {
        val module = logMessage.module
        val channel = logMessage.channel
        val message = logMessage.message
        when {
            module == "launchdarkly" && channel == "client.client_sdk" -> {
                LAUNCHDARKLY_USER_ID.onMatch(message) { (userId) -> return AccountId(userId.toInt()) }
            }
            module == "svc" && channel == "eveMachoNet transport" -> {
                MACHONET_USER_ID.onMatch(message) { (userId) -> return AccountId(userId.toInt()) }
            }
            module == "svc" && channel == "gameui" -> {
                SVC_GAMEUI_USER_ID.onMatch(message) { (userId) -> return AccountId(userId.toInt()) }
            }
            module == "svc" && channel == "clientDogmaIM" -> {
                SVC_CLIENT_DOGMA_IM_CHARACTER_ID.onMatch(message) { (characterId) -> return CharacterId(characterId.toInt()) }
            }
            module == "svc" && channel == "starmap" -> {
                SVC_STARMAP_PATH.onMatch(message) { (path) -> return AutopilotPath(path.split(", ").mapNotNull { it.toIntOrNull() }) }
            }
            module == "windowSettings" && channel == "General" -> {
                WINDOWSETTINGS_USER_ID.onMatch(message) { (userId) -> return AccountId(userId.toInt()) }
            }
        }
        return null
    }

    private inline fun Regex.onMatch(text: String, block: (List<String>) -> Unit) {
        val match = find(text) ?: return
        val groups = match.groups.drop(1).mapNotNull { it?.value }
        block(groups)
    }
}

private val LAUNCHDARKLY_USER_ID = """^LD user attributes updated: \{'userID': u'(?<userid>[0-9]+)', 'characterID'.*}$""".toRegex()
private val MACHONET_USER_ID = """^.*'userid': \(None, (?<userid>[0-9]+)\),.*$""".toRegex()
private val WINDOWSETTINGS_USER_ID = """^Validate Window Settings, user: (?<userid>[0-9]+), char: (?<char>.*)$""".toRegex()
private val SVC_GAMEUI_USER_ID = """^ProcessSessionChange:  \{.*'userid': \(None, (?<userid>[0-9]+)\).*}.*$""".toRegex()
private val SVC_CLIENT_DOGMA_IM_CHARACTER_ID = """^[0-9]+ OnServerBrainUpdated received for character (?<characterid>[0-9]+), brain version .*$""".toRegex()
private val SVC_STARMAP_PATH = """^self destination path 0 is own solarsystem, picking next node instead. Path:  \[(?<path>[0-9, ]+)]$""".toRegex()
