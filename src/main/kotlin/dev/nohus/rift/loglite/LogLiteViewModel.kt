package dev.nohus.rift.loglite

import dev.nohus.rift.ViewModel
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.toggle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import java.time.ZoneId

@Factory
class LogLiteViewModel(
    private val settings: Settings,
    private val logLiteRepository: LogLiteRepository,
) : ViewModel() {

    data class UiState(
        val allMessages: List<LogMessage> = emptyList(),
        val filteredMessages: List<LogMessage> = emptyList(),
        val modules: List<String> = emptyList(),
        val channels: List<String> = emptyList(),
        val clients: List<Client> = emptyList(),
        val selectedClient: Client? = null,
        val ignoredModules: List<String> = emptyList(),
        val ignoredChannels: List<String> = emptyList(),
        val search: String? = null,
        val moduleCounts: Map<String, Int> = emptyMap(),
        val channelCounts: Map<String, Int> = emptyMap(),
        val displayTimezone: ZoneId,
    )

    private val _state = MutableStateFlow(
        UiState(
            displayTimezone = settings.displayTimeZone,
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            logLiteRepository.flow.collect { message ->
                onNewMessage(message)
                _state.update { it.copy(allMessages = it.allMessages + message) }
                applyFilters()
            }
        }
        viewModelScope.launch {
            settings.updateFlow.collect {
                _state.update {
                    it.copy(
                        displayTimezone = settings.displayTimeZone,
                    )
                }
            }
        }
    }

    fun onModuleFilterSelect(module: String) {
        _state.update { it.copy(ignoredModules = it.ignoredModules.toggle(module)) }
        applyFilters()
    }

    fun onChannelFilterSelect(channel: String) {
        _state.update { it.copy(ignoredChannels = it.ignoredChannels.toggle(channel)) }
        applyFilters()
    }

    fun onClientSelect(client: Client?) {
        _state.update { it.copy(selectedClient = client) }
        applyFilters()
    }

    fun onToggleAllModules() {
        if (_state.value.ignoredModules.isEmpty()) {
            _state.update { it.copy(ignoredModules = it.modules) }
        } else {
            _state.update { it.copy(ignoredModules = emptyList()) }
        }
        applyFilters()
    }

    fun onToggleAllChannels() {
        if (_state.value.ignoredChannels.isEmpty()) {
            _state.update { it.copy(ignoredChannels = it.channels) }
        } else {
            _state.update { it.copy(ignoredChannels = emptyList()) }
        }
        applyFilters()
    }

    fun onSearchChange(text: String) {
        val search = text.takeIf { it.isNotBlank() }?.trim()
        _state.update { it.copy(search = search) }
        applyFilters()
    }

    private fun applyFilters() {
        _state.update { it.copy(filteredMessages = filter(it.allMessages)) }
    }

    private fun filter(messages: List<LogMessage>): List<LogMessage> {
        val search = _state.value.search
        return messages
            .filter { it.client == _state.value.selectedClient }
            .filter { it.module !in _state.value.ignoredModules }
            .filter { it.channel !in _state.value.ignoredChannels }
            .filter { search == null || search.lowercase() in it.message.lowercase() }
    }

    private fun onNewMessage(message: LogMessage) {
        val moduleCount = (_state.value.moduleCounts[message.module] ?: 0) + 1
        val channelCount = (_state.value.channelCounts[message.channel] ?: 0) + 1
        _state.update {
            it.copy(
                modules = (it.modules + message.module).distinct(),
                channels = (it.channels + message.channel).distinct(),
                moduleCounts = it.moduleCounts + (message.module to moduleCount),
                channelCounts = it.channelCounts + (message.channel to channelCount),
            )
        }
        if (message.client !in _state.value.clients) {
            _state.update {
                it.copy(
                    clients = it.clients + message.client,
                    selectedClient = message.client,
                )
            }
            applyFilters()
        }
    }
}
