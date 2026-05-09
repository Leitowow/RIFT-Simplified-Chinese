package dev.nohus.rift.chat

import dev.nohus.rift.logs.GetChatLogsDirectoryUseCase
import dev.nohus.rift.logs.MatchChatLogFilenameUseCase
import dev.nohus.rift.logs.parse.ChannelChatMessage
import dev.nohus.rift.logs.parse.ChatMessage
import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import java.io.IOException
import java.time.Instant
import kotlin.io.path.listDirectoryEntries

private val logger = KotlinLogging.logger {}
private const val MAX_MESSAGES_PER_CHANNEL = 100

@Single
class ChatsController(
    private val getChatLogsDirectoryUseCase: GetChatLogsDirectoryUseCase,
    private val matchChatLogFilenameUseCase: MatchChatLogFilenameUseCase,
    private val settings: Settings,
) {

    private val _chats = MutableStateFlow<Chats>(Chats())
    val chats = _chats.asStateFlow()

    private val scope = CoroutineScope(Job())
    private val mutex = Mutex()
    private val characterSpecificChannels = setOf("Local", "Corp", "Alliance")

    data class Chats(
        val channels: List<Channel> = emptyList(),
        val messages: Map<Channel, List<ChatMessage>> = emptyMap(),
        val lastMessageTimestamps: Map<Channel, Instant> = emptyMap(),
    )

    data class Channel(
        val characterId: Int?,
        val name: String,
    )

    init {
        updateAvailableChannels()
    }

    fun onNewChatMessage(channelChatMessage: ChannelChatMessage) {
        val channel = getChannel(channelChatMessage) ?: run {
            logger.warn { "Channel not found for chat message: $channelChatMessage" }
            return
        }
        scope.launch(Dispatchers.Default) {
            mutex.withLock {
                val messages = ((_chats.value.messages[channel] ?: emptyList()) + channelChatMessage.chatMessage).takeLast(MAX_MESSAGES_PER_CHANNEL)
                _chats.value = _chats.value.copy(
                    messages = _chats.value.messages + (channel to messages),
                    lastMessageTimestamps = _chats.value.lastMessageTimestamps + (channel to channelChatMessage.chatMessage.timestamp),
                )
            }
        }
    }

    private fun getChannel(channelChatMessage: ChannelChatMessage): Channel? {
        getChannelOrNull(channelChatMessage)?.let { return it }
        // Channel is not found, this message might be in a newly opened channel, so rescan channels
        updateAvailableChannels()
        return getChannelOrNull(channelChatMessage)
    }

    private fun getChannelOrNull(channelChatMessage: ChannelChatMessage): Channel? {
        return _chats.value.channels.firstOrNull {
            (it.characterId == null || it.characterId == channelChatMessage.metadata.characterId) &&
                it.name == channelChatMessage.metadata.channelName
        }
    }

    private fun updateAvailableChannels() {
        val channels = try {
            getChatLogsDirectoryUseCase(settings.eveLogsDirectory)
                ?.listDirectoryEntries()
                ?.mapNotNull { matchChatLogFilenameUseCase(it) }
                ?.map { chatLogFile ->
                    Channel(
                        characterId = chatLogFile.characterId.takeIf { chatLogFile.channelName in characterSpecificChannels },
                        name = chatLogFile.channelName,
                    ) to chatLogFile.lastModified
                }
                ?.distinct()
                ?: emptyList()
        } catch (e: IOException) {
            logger.error { "Could not get chat channels: ${e.message}" }
            emptyList()
        }
        _chats.update { it.copy(channels = channels.map { it.first }.distinct()) }

        channels.forEach { (channel, lastModified) ->
            val timestamp = _chats.value.lastMessageTimestamps[channel] ?: Instant.EPOCH
            if (lastModified > timestamp) {
                _chats.update { it.copy(lastMessageTimestamps = it.lastMessageTimestamps + (channel to lastModified)) }
            }
        }
    }
}
