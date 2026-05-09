package dev.nohus.rift.clipboard

import dev.nohus.rift.ViewModel
import dev.nohus.rift.settings.JumpBridgesParser
import dev.nohus.rift.settings.SovereigntyUpgradesParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

@Factory
class ClipboardTestViewModel(
    private val clipboard: Clipboard,
    private val jumpBridgesParser: JumpBridgesParser,
    private val sovereigntyUpgradesParser: SovereigntyUpgradesParser,
) : ViewModel() {

    data class UiState(
        val type: ClipboardImportType? = null,
        val text: String = "",
        val jumpBridgesResult: JumpBridgesParser.ParsingResult? = null,
        val sovereigntyUpgradesResult: SovereigntyUpgradesParser.ParsingResult? = null,
    )

    enum class ClipboardImportType {
        JumpBridges,
        SovereigntyUpgrades,
    }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            clipboard.state.collect { _ ->
                parse()
            }
        }
    }

    fun onImportTypeChange(type: ClipboardImportType) {
        _state.update { it.copy(type = type) }
        viewModelScope.launch {
            parse()
        }
    }

    private suspend fun parse() {
        val text = clipboard.state.value
        _state.update { it.copy(text = text ?: "") }
        if (text == null) return

        when (_state.value.type) {
            ClipboardImportType.JumpBridges -> {
                val result = jumpBridgesParser.parseResult(text)
                _state.update { it.copy(jumpBridgesResult = result) }
            }
            ClipboardImportType.SovereigntyUpgrades -> {
                val result = sovereigntyUpgradesParser.parseResult(text)
                _state.update { it.copy(sovereigntyUpgradesResult = result) }
            }
            null -> {}
        }
    }
}
