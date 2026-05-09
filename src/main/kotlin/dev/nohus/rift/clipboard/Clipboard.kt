package dev.nohus.rift.clipboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.IOException
import org.koin.core.annotation.Single
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException
import kotlin.time.measureTimedValue

@Single
class Clipboard {

    private val _state = MutableStateFlow<String?>(null)
    val state = _state.asStateFlow()

    suspend fun start() = coroutineScope {
        launch {
            observeClipboard()
        }
    }

    private suspend fun observeClipboard() = withContext(Dispatchers.IO) {
        while (true) {
            val (contents, duration) = measureTimedValue {
                getClipboardContents()
            }
            _state.value = contents
            val delay = (duration.inWholeMilliseconds * 4).coerceIn(1000, 2000)
            delay(delay)
        }
    }

    private fun getClipboardContents(): String? {
        return try {
            val transferable = clipboard.getContents(null)
            transferable.getTransferData(DataFlavor.stringFlavor) as? String
        } catch (_: IllegalStateException) {
            null
        } catch (_: UnsupportedFlavorException) {
            null
        } catch (_: IOException) {
            null
        }
    }

    companion object {
        private val clipboard = Toolkit.getDefaultToolkit().systemClipboard

        fun copy(text: String) {
            val selection = StringSelection(text)
            clipboard.setContents(selection, null)
        }
    }
}
