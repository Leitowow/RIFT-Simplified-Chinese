package dev.nohus.rift.intel

import dev.nohus.rift.alerts.AlertsTriggerController
import dev.nohus.rift.chat.ChatsController
import dev.nohus.rift.intel.state.IntelStateController
import dev.nohus.rift.intel.state.UnderstandMessageUseCase
import dev.nohus.rift.location.LocalSystemChangeController
import dev.nohus.rift.logs.ChatLogsObserver
import dev.nohus.rift.logs.DetectLogsDirectoryUseCase
import dev.nohus.rift.logs.GetChatLogsDirectoryUseCase
import dev.nohus.rift.logs.parse.ChannelChatMessage
import dev.nohus.rift.logs.parse.ChatMessageParser
import dev.nohus.rift.logs.parse.ChooseChatMessageTokenizationUseCase
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.mapAsync
import io.github.oshai.kotlinlogging.KotlinLogging
import io.sentry.Sentry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Single
class ChatLogWatcher(
    private val chatLogsObserver: ChatLogsObserver,
    private val detectLogsDirectoryUseCase: DetectLogsDirectoryUseCase,
    private val getChatLogsDirectoryUseCase: GetChatLogsDirectoryUseCase,
    private val settings: Settings,
    private val chatMessageParser: ChatMessageParser,
    private val chooseChatMessageTokenizationUseCase: ChooseChatMessageTokenizationUseCase,
    private val intelStateController: IntelStateController,
    private val localSystemChangeController: LocalSystemChangeController,
    private val alertsTriggerController: AlertsTriggerController,
    private val understandMessageUseCase: UnderstandMessageUseCase,
    private val characterDetailsRepository: CharacterDetailsRepository,
    private val chatsController: ChatsController,
) {

    private val _channelChatMessages = MutableStateFlow<List<ParsedChannelChatMessage>>(emptyList())
    val channelChatMessages = _channelChatMessages.asStateFlow()

    private var watchedDirectory: Path? = null
    private var isWatchedDirectoryInitialized = false
    private val mutexByChannel = mutableMapOf<String, Mutex>()

    suspend fun start() = coroutineScope {
        launch {
            observeChatLogs()
        }
        launch {
            settings.updateFlow.collect {
                settings.eveLogsDirectory.let { new ->
                    if (isWatchedDirectoryInitialized && new != watchedDirectory) {
                        launch {
                            observeChatLogs()
                        }
                    }
                }
            }
        }
    }

    private suspend fun observeChatLogs() {
        chatLogsObserver.stop()
        val logsDirectory = settings.eveLogsDirectory ?: detectLogsDirectoryUseCase()
        val chatLogsDirectory = getChatLogsDirectoryUseCase(logsDirectory)
        if (chatLogsDirectory != null) {
            watchedDirectory = logsDirectory
            isWatchedDirectoryInitialized = true
            observeChatLogs(chatLogsDirectory)
        } else {
            watchedDirectory = null
            isWatchedDirectoryInitialized = true
            logger.warn { "No chat logs directory detected" }
        }
    }

    private suspend fun observeChatLogs(directory: Path) = coroutineScope {
        chatLogsObserver.observe(directory) { channelChatMessage ->
            launch {
                getMutex(channelChatMessage.metadata.channelName).withLock {
                    val age = Duration.between(channelChatMessage.chatMessage.timestamp, Instant.now())
                    val isFresh = age < Duration.ofMinutes(2)
                    if (isFresh) {
                        alertsTriggerController.onNewChatMessage(channelChatMessage)
                        chatsController.onNewChatMessage(channelChatMessage)
                    }

                    if (isMessageRelevantForIntel(channelChatMessage)) {
                        val regions = getIntelRegions(channelChatMessage)
                        try {
                            val parsings = chatMessageParser.parse(channelChatMessage.chatMessage.message, regions)
                            if (parsings.isNotEmpty()) {
                                val bestParsing = fillCharacterDetails(chooseChatMessageTokenizationUseCase(parsings))
                                val understanding = understandMessageUseCase(bestParsing)
                                val parsed = ParsedChannelChatMessage(
                                    chatMessage = channelChatMessage.chatMessage,
                                    channelRegions = regions,
                                    metadata = channelChatMessage.metadata,
                                    parsed = bestParsing,
                                    understanding = understanding,
                                )

                                val context = getMessageContext(parsed)
                                intelStateController.submitMessage(parsed, context, isFresh)
                                _channelChatMessages.update { previous ->
                                    (previous + parsed).sortedBy { it.chatMessage.timestamp }
                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Sentry.captureException(IOException("Could not parse: \"${channelChatMessage.chatMessage.message}\", regions: \"$regions\"", e))
                        }
                    }

                    if (isMessageLocalEveSystem(channelChatMessage)) {
                        if (channelChatMessage.chatMessage.message.startsWith("Channel changed to Local")) {
                            val system = channelChatMessage.chatMessage.message.substringAfter(" : ")
                            localSystemChangeController.onSystemChangeMessage(
                                system = system,
                                timestamp = channelChatMessage.chatMessage.timestamp,
                                characterId = channelChatMessage.metadata.characterId,
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun fillCharacterDetails(parsing: List<ChatMessageParser.Token>): List<ChatMessageParser.Token> {
        return parsing.mapAsync { token ->
            if (token.type is ChatMessageParser.TokenType.Character) {
                token.copy(
                    type = token.type.copy(
                        details = characterDetailsRepository.getCharacterDetails(Originator.ChatLogs, token.type.characterId),
                    ),
                )
            } else if (token.type is ChatMessageParser.TokenType.Kill) {
                token.type.characterId?.let { characterId ->
                    token.copy(
                        type = token.type.copy(
                            details = characterDetailsRepository.getCharacterDetails(Originator.ChatLogs, characterId),
                        ),
                    )
                } ?: token
            } else {
                token
            }
        }
    }

    /**
     * Retrieves recent chat messages preceding this chat message
     */
    private fun getMessageContext(parsed: ParsedChannelChatMessage): List<ParsedChannelChatMessage> {
        return _channelChatMessages.value.reversed()
            .asSequence()
            .filter { it.metadata.channelId == parsed.metadata.channelId }
            .take(10)
            .filter { Duration.between(it.chatMessage.timestamp, parsed.chatMessage.timestamp) < Duration.ofMinutes(5) }
            .toList()
    }

    private fun isMessageLocalEveSystem(message: ChannelChatMessage): Boolean {
        if (message.metadata.channelName != "Local") return false
        if (message.chatMessage.author != "EVE System") return false
        return true
    }

    @Suppress("RedundantIf")
    private fun isMessageRelevantForIntel(message: ChannelChatMessage): Boolean {
        if (!isIntelChannel(message.metadata.channelName)) return false
        if (message.chatMessage.author == "EVE System") return false
        val messageAge = Duration.between(message.chatMessage.timestamp, Instant.now())
        val isMessageOld = messageAge > Duration.ofMinutes(10)
        if (isMessageOld) return false
        return true
    }

    private fun getMutex(channel: String): Mutex {
        return mutexByChannel[channel] ?: Mutex().also { mutexByChannel[channel] = it }
    }

    private fun isIntelChannel(channelName: String): Boolean {
        return channelName in settings.intelChannels.map { it.name }
    }

    private fun getIntelRegions(message: ChannelChatMessage): List<String> {
        return settings.intelChannels.filter { it.name == message.metadata.channelName }.mapNotNull { it.region }
    }
}
