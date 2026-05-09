package dev.nohus.rift.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object LoggingRepository {

    private const val MAX_SIZE = 20_000
    private val _state = MutableStateFlow<List<ILoggingEvent>>(emptyList())
    val state = _state.asStateFlow()

    fun append(event: ILoggingEvent) {
        _state.value += event

        if (_state.value.size > MAX_SIZE) {
            _state.value = _state.value.drop(MAX_SIZE / 2)
        }
    }
}
