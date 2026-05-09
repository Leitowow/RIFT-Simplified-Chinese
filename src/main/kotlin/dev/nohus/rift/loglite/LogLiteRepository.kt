package dev.nohus.rift.loglite

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class LogLiteRepository {

    private val _flow = MutableSharedFlow<LogMessage>()
    val flow = _flow.asSharedFlow()

    private val scope = CoroutineScope(Job())

    fun add(message: LogMessage) {
        scope.launch {
            _flow.emit(message)
        }
    }
}
