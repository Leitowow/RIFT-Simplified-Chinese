package dev.nohus.rift.windowing

import androidx.compose.ui.window.WindowPlacement
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
class WindowStatesController(
    private val settings: Settings,
) {

    private var maximizedWindows = MutableStateFlow<Set<RiftWindow>>(emptySet())

    fun isAlwaysOnTop(window: RiftWindow?) = flow {
        if (window == null) {
            emit(false)
        } else {
            emit(window in settings.alwaysOnTopWindows)
            emitAll(settings.updateFlow.map { window in it.alwaysOnTopWindows })
        }
    }

    fun isLocked(window: RiftWindow?) = flow {
        if (window == null) {
            emit(false)
        } else {
            emit(window in settings.lockedWindows)
            emitAll(settings.updateFlow.map { window in it.lockedWindows })
        }
    }

    fun isMaximized(window: RiftWindow?) = flow {
        if (window == null) {
            emit(false)
        } else {
            emit(window in maximizedWindows.value)
            emitAll(maximizedWindows.map { window in it })
        }
    }

    fun toggleAlwaysOnTop(window: RiftWindow?) {
        window ?: return
        if (window in settings.alwaysOnTopWindows) {
            settings.alwaysOnTopWindows -= window
        } else {
            settings.alwaysOnTopWindows += window
        }
    }

    fun toggleLocked(window: RiftWindow?) {
        window ?: return
        if (window in settings.lockedWindows) {
            settings.lockedWindows -= window
        } else {
            settings.lockedWindows += window
        }
    }

    suspend fun toggleMaximized(window: RiftWindow): WindowPlacement {
        return if (window in maximizedWindows.value) {
            maximizedWindows.value -= window
            WindowPlacement.Floating
        } else {
            // Unlock window if locked, so that it can be maximized
            if (window in settings.lockedWindows) {
                settings.lockedWindows -= window
                // Unlocking has to take effect in the native OS for the maximize request to take effect
                delay(100)
            }
            maximizedWindows.value += window
            WindowPlacement.Maximized
        }
    }
}
