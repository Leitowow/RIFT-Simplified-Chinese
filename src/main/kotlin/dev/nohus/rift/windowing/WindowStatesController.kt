package dev.nohus.rift.windowing

import androidx.compose.ui.window.WindowPlacement
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.settings.persistence.WindowSettings
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import java.util.UUID

@Single
class WindowStatesController(
    private val settings: Settings,
) {

    private var maximizedWindows = MutableStateFlow<Set<UUID>>(emptySet())

    fun isAlwaysOnTop(window: RiftWindow?, uuid: UUID) = isState(window, uuid) { isAlwaysOnTop }

    fun isLocked(window: RiftWindow?, uuid: UUID) = isState(window, uuid) { isLocked }

    fun isTransparent(window: RiftWindow?, uuid: UUID) = isState(window, uuid) { isTransparent }

    fun isMaximized(uuid: UUID) = flow {
        emit(uuid in maximizedWindows.value)
        emitAll(maximizedWindows.map { uuid in it })
    }

    fun toggleAlwaysOnTop(window: RiftWindow?, uuid: UUID) {
        setWindowSettings(window, uuid) {
            copy(isAlwaysOnTop = !isAlwaysOnTop)
        }
    }

    fun toggleLocked(window: RiftWindow?, uuid: UUID) {
        setWindowSettings(window, uuid) {
            copy(isLocked = !isLocked)
        }
    }

    fun toggleTransparent(window: RiftWindow?, uuid: UUID) {
        setWindowSettings(window, uuid) {
            copy(isTransparent = !isTransparent)
        }
    }

    suspend fun toggleMaximized(window: RiftWindow, uuid: UUID): WindowPlacement {
        return if (uuid in maximizedWindows.value) {
            maximizedWindows.value -= uuid
            WindowPlacement.Floating
        } else {
            // Unlock window if locked, so that it can be maximized
            val isLocked = getWindowSettings(settings.windowSettings, window, uuid)?.isLocked == true
            if (isLocked) {
                toggleLocked(window, uuid)
                // Unlocking has to take effect in the native OS for the maximize request to take effect
                delay(100)
            }
            maximizedWindows.value += uuid
            WindowPlacement.Maximized
        }
    }

    private fun isState(window: RiftWindow?, uuid: UUID, getState: WindowSettings.() -> Boolean) = flow {
        if (window == null) {
            emit(false)
        } else {
            emit(getWindowSettings(settings.windowSettings, window, uuid)?.getState() ?: false)
            emitAll(settings.updateFlow.map { it.windowSettings }.map { getWindowSettings(settings.windowSettings, window, uuid)?.getState() == true })
        }
    }

    private fun getWindowSettings(settings: Map<RiftWindow, List<WindowSettings>>?, window: RiftWindow?, uuid: UUID): WindowSettings? {
        val settings = settings?.let { it[window] } ?: return null
        return settings.firstOrNull { it.uuid == uuid }
    }

    private fun setWindowSettings(window: RiftWindow?, uuid: UUID, update: WindowSettings.() -> WindowSettings) {
        window ?: return
        val existingList = settings.windowSettings[window]
        if (existingList != null) {
            val existingWindow = existingList.firstOrNull { it.uuid == uuid }
            if (existingWindow != null) {
                settings.windowSettings += window to (existingList - existingWindow + existingWindow.update())
            } else {
                settings.windowSettings += window to (existingList + WindowSettings(uuid = uuid).update())
            }
        } else {
            settings.windowSettings += window to listOf(WindowSettings(uuid = uuid).update())
        }
    }
}
