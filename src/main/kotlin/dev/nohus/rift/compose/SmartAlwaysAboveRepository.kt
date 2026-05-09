package dev.nohus.rift.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalWindowInfo
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.activewindow.ActiveEveWindowRepository
import dev.nohus.rift.windowing.WindowManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.util.UUID

/**
 * Controls whether "always above" property of windows should take effect
 */
@Single
class SmartAlwaysAboveRepository(
    private val activeEveWindowRepository: ActiveEveWindowRepository,
    private val settings: Settings,
) {

    private var focusedRiftWindow: UUID? = null
    private val _isActive = MutableStateFlow(false)
    val isActive = _isActive.asStateFlow()

    suspend fun start() = coroutineScope {
        launch {
            activeEveWindowRepository.activeWindowCharacter.map { it != null }.collect {
                updateState()
            }
        }
    }

    @Composable
    fun registerWindow(state: WindowManager.RiftWindowState) {
        val isFocused = LocalWindowInfo.current.isWindowFocused
        val windowUuid = state.uuid
        LaunchedEffect(isFocused, windowUuid) {
            if (isFocused) {
                focusedRiftWindow = windowUuid
                updateState()
            } else {
                if (focusedRiftWindow == windowUuid) {
                    focusedRiftWindow = null
                    if (settings.isSmartAlwaysAbove) {
                        activeEveWindowRepository.checkNow()
                    }
                    updateState()
                }
            }
        }
    }

    private fun updateState() {
        val isSmartAboveDisabled = !settings.isSmartAlwaysAbove
        val isRiftFocused = focusedRiftWindow != null
        val isEveFocused = activeEveWindowRepository.activeWindowCharacter.value != null
        val isActive = isSmartAboveDisabled || isRiftFocused || isEveFocused
        _isActive.value = isActive
    }
}
