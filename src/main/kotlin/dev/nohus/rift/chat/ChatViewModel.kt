package dev.nohus.rift.chat

import dev.nohus.rift.ViewModel
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.chat.ChatsController.Channel
import dev.nohus.rift.compose.EntityInteractionProvider
import dev.nohus.rift.compose.text.FormattedText
import dev.nohus.rift.compose.text.Link
import dev.nohus.rift.compose.text.LinkStyle
import dev.nohus.rift.compose.text.buildFormattedText
import dev.nohus.rift.compose.text.toFormattedText
import dev.nohus.rift.logs.parse.ChatMessage
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.character.CharacterAffiliationRepository
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import dev.nohus.rift.repositories.character.CharacterDetailsRepository.CharacterDetails
import dev.nohus.rift.repositories.character.CharactersRepository
import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import java.time.Instant
import java.time.ZoneId

private val logger = KotlinLogging.logger {}

@Factory
class ChatViewModel(
    private val chatsController: ChatsController,
    private val localCharactersRepository: LocalCharactersRepository,
    private val charactersRepository: CharactersRepository,
    private val characterDetailsRepository: CharacterDetailsRepository,
    private val entityInteractionProvider: EntityInteractionProvider,
    private val linkMessageUseCase: LinkMessageUseCase,
    private val settings: Settings,
    private val characterAffiliationRepository: CharacterAffiliationRepository,
) : ViewModel() {

    data class UiState(
        val messages: List<RichChatMessage> = emptyList(),
        val channels: List<Channel> = emptyList(),
        val openChannels: List<Channel> = emptyList(),
        val selectedChannel: Channel? = null,
        val characters: List<LocalCharacter> = emptyList(),
        val lastMessageTimestamp: Map<Channel, Instant> = emptyMap(),
        val lastViewedTimestamp: Map<Channel, Instant> = emptyMap(),
        val displayTimezone: ZoneId = ZoneId.systemDefault(),
    )

    data class RichChatMessage(
        val timestamp: Instant,
        val author: Author,
        val authorText: FormattedText,
        val message: FormattedText,
    )

    sealed interface Author {
        data class Character(
            val characterId: Int?,
            val characterDetails: CharacterDetails?,
        ) : Author
        data object EveSystem : Author
    }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.map { it.selectedChannel }.distinctUntilChanged().collectLatest { channel ->
                _state.update { it.copy(messages = emptyList()) }
                chatsController.chats.map { it.messages[channel] ?: emptyList() }.distinctUntilChanged().collect { messages ->
                    val new = messages.drop(_state.value.messages.size)
                    val initialRichMessages = new.map { it to getRichChatMessageImmediate(it) }
                    _state.update { it.copy(messages = it.messages + initialRichMessages.map { it.second }) }
                    processAndUpdateStubs(initialRichMessages.filter { it.second.author is Author.Character })
                }
            }
        }
        viewModelScope.launch {
            chatsController.chats.collect { chats ->
                _state.update {
                    it.copy(
                        channels = chats.channels,
                        lastMessageTimestamp = chats.lastMessageTimestamps,
                    )
                }
            }
        }
        viewModelScope.launch {
            localCharactersRepository.characters.collect { characters ->
                _state.update { it.copy(characters = characters) }
            }
        }
        viewModelScope.launch {
            settings.updateFlow.map { it.isDisplayEveTime }.collect {
                _state.update {
                    it.copy(
                        displayTimezone = settings.displayTimeZone,
                    )
                }
            }
        }
    }

    fun onChannelSelected(channel: Channel?) {
        if (channel != null) {
            val previousSelectedChannel = _state.value.selectedChannel
            val now = Instant.now()
            val lastViewedTimestamp = if (previousSelectedChannel != null) {
                _state.value.lastViewedTimestamp + (channel to now) + (previousSelectedChannel to now)
            } else {
                _state.value.lastViewedTimestamp + (channel to now)
            }
            _state.update {
                it.copy(
                    selectedChannel = channel,
                    lastViewedTimestamp = lastViewedTimestamp,
                )
            }
        } else {
            _state.update { it.copy(selectedChannel = null) }
        }
    }

    fun onPlusClick() {
        _state.update { it.copy(selectedChannel = null) }
    }

    fun onChannelOpenClick(channel: Channel) {
        val now = Instant.now()
        val lastViewedTimestamp = _state.value.lastViewedTimestamp + (channel to now)
        _state.update {
            it.copy(
                openChannels = (it.openChannels - channel) + channel,
                lastViewedTimestamp = lastViewedTimestamp,
            )
        }
    }

    fun onChannelCloseClick(channel: Channel) {
        if (_state.value.selectedChannel == channel) {
            val openChannels = _state.value.openChannels
            val index = openChannels.indexOf(channel)
            val nextChannel = openChannels.getOrNull(index + 1) ?: openChannels.firstOrNull()
            onChannelSelected(nextChannel.takeIf { it != channel })
        }
        _state.update { it.copy(openChannels = it.openChannels - channel) }
    }

    /**
     * Returns a stub rich message to be processed later, or if this was an EVE System message, the final rich message
     * since no more processing is required
     */
    private fun getRichChatMessageImmediate(message: ChatMessage): RichChatMessage {
        val author = if (message.author == "EVE System") Author.EveSystem else Author.Character(null, null)
        return RichChatMessage(
            timestamp = message.timestamp,
            author = author,
            authorText = message.author.toFormattedText(),
            message = message.message.toFormattedText(),
        )
    }

    private fun processAndUpdateStubs(processedStubs: List<Pair<ChatMessage, RichChatMessage>>) {
        viewModelScope.launch {
            val authorsCharacterIds = processedStubs.mapNotNull {
                it.first.author to (charactersRepository.getCharacterId(Originator.ChatLogs, it.first.author) ?: return@mapNotNull null)
            }.toMap()
            val authorsAffiliations = characterAffiliationRepository.getCharacterAffiliations(Originator.ChatLogs, authorsCharacterIds.values.toList())
            processedStubs.forEach { (message, processedStub) ->
                launch {
                    val authorCharacterId = authorsCharacterIds[message.author]
                    val characterDetails = if (authorCharacterId != null) {
                        val authorAffiliation = authorsAffiliations[authorCharacterId]
                        characterDetailsRepository.getCharacterDetails(Originator.ChatLogs, authorCharacterId, authorAffiliation)
                    } else {
                        null
                    }

                    var stub = processedStub
                    if (authorCharacterId != null && characterDetails != null) {
                        stub = stub.copy(author = Author.Character(authorCharacterId, characterDetails))
                        _state.value.messages.indexOfLast { it == processedStub }.takeIf { it >= 0 }?.let { index ->
                            _state.update { it.copy(messages = it.messages.toMutableList().apply { set(index, stub) }) }
                        }
                    }

                    val processed = processMessage(message, authorCharacterId, characterDetails)
                    _state.value.messages.indexOfLast { it == stub }.takeIf { it >= 0 }?.let { index ->
                        _state.update { it.copy(messages = it.messages.toMutableList().apply { set(index, processed) }) }
                    }
                }
            }
        }
    }

    private val processedMessagesCache = Cache.Builder<ChatMessage, RichChatMessage>()
        .maximumCacheSize(1_000)
        .build()
    private suspend fun processMessage(message: ChatMessage, characterId: Int?, characterDetails: CharacterDetails?): RichChatMessage {
        processedMessagesCache.get(message)?.let { return it }

        val authorFormattedText = buildFormattedText {
            if (characterId != null) {
                val interaction = entityInteractionProvider.getCharacter(characterId)
                val authorLink = Link(
                    style = LinkStyle.HoverUnderline,
                    onClick = interaction.onClick,
                    contextMenuItems = interaction.contextMenuItems,
                )
                withLink(authorLink) {
                    append(message.author)
                }
            } else {
                append(message.author)
            }
        }
        val formattedText = linkMessageUseCase(message.message)
        return RichChatMessage(
            timestamp = message.timestamp,
            author = Author.Character(characterId, characterDetails),
            authorText = authorFormattedText,
            message = formattedText,
        ).also { processedMessagesCache.put(message, it) }
    }
}
