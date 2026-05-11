package dev.nohus.rift.neocom

import dev.nohus.rift.ApplicationViewModel
import dev.nohus.rift.ViewModel
import dev.nohus.rift.configurationpack.ConfigurationPackRepository
import dev.nohus.rift.jabber.client.JabberClient
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

@Factory
class NeocomViewModel(
    private val windowManager: WindowManager,
    private val applicationViewModel: ApplicationViewModel,
    private val configurationPackRepository: ConfigurationPackRepository,
    private val jabberClient: JabberClient,
    private val settings: Settings,
) : ViewModel() {

    data class UiState(
        val isJabberEnabled: Boolean = false,
        val isJabberConnected: Boolean = false,
    )

    private val _state = MutableStateFlow(
        UiState(
            isJabberEnabled = configurationPackRepository.isJabberEnabled(),
            isJabberConnected = jabberClient.state.value.isConnected,
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            jabberClient.state.collect { jabberState ->
                _state.update { it.copy(isJabberConnected = jabberState.isConnected) }
            }
        }
    }

    fun onButtonClick(window: RiftWindow) {
        if (window == RiftWindow.Jukebox && windowManager.getOpenWindowUuids(RiftWindow.JukeboxCollapsed).isNotEmpty()) {
            // We want to open the Jukebox, but collapsed Jukebox is already open so bring that up instead
            windowManager.onWindowOpen(RiftWindow.JukeboxCollapsed)
        } else {
            windowManager.onWindowOpen(window)
        }
    }

    fun onQuitClick() {
        applicationViewModel.onQuit()
    }

    override fun onClose() {
        if (!settings.isTrayIconWorking) {
            // We don't have confirmation that the tray icon is working
            // Close the app, so it's not running in the background without the user having the ability to quit it
            applicationViewModel.onQuit()
        }
    }
}
