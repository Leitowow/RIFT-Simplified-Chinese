package dev.nohus.rift.utils.activewindow

import dev.nohus.rift.utils.OperatingSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class ActiveEveWindowRepository(
    private val operatingSystem: OperatingSystem,
    private val getActiveWindowUseCase: GetActiveWindowUseCase,
) {

    private val _activeWindowCharacter = MutableStateFlow<String?>(null)
    val activeWindowCharacter = _activeWindowCharacter.asStateFlow()

    private val windowTitleRegex = """EVE - (?<character>[A-z0-9 '-]{3,37})""".toRegex()

    suspend fun start() = coroutineScope {
        // Checking active window does not work on macOS
        if (operatingSystem != OperatingSystem.MacOs) {
            while (true) {
                checkActiveWindow()
                delay(2000)
            }
        }
    }

    suspend fun checkNow() {
        if (operatingSystem != OperatingSystem.MacOs) {
            checkActiveWindow()
        }
    }

    private suspend fun checkActiveWindow() = withContext(Dispatchers.IO) {
        val activeWindowTitle = getActiveWindowUseCase()
        val characterName = if (activeWindowTitle != null) {
            windowTitleRegex.find(activeWindowTitle)?.groups?.get("character")?.value
        } else {
            null
        }
        _activeWindowCharacter.value = characterName
    }
}
