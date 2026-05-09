package dev.nohus.rift.loglite

import io.github.oshai.kotlinlogging.KotlinLogging
import okio.IOException
import java.net.Socket
import java.time.Instant

private val logger = KotlinLogging.logger {}

private const val MESSAGE_SIZE = 344

class LogLiteSession(
    private val socket: Socket,
    private val onNewMessage: (message: LogMessage) -> Unit,
) {
    private var client: Client? = null
    private var incompleteMessage: LogMessage? = null

    fun start() {
        val stream = socket.getInputStream()
        while (!socket.isClosed) {
            val bytes = stream.readNBytes(MESSAGE_SIZE)
            val reader = ByteReader(bytes)
            val type = getMessageType(reader.read4ByteNumber())
            if (client == null && type != MessageType.Connection) {
                logger.error { "Expected connection message but got $type. Closing connection." }
                socket.close()
                return
            }
            reader.skip(4)
            when (type) {
                MessageType.Connection -> readConnectionMessage(reader)
                MessageType.Simple -> readSimpleMessage(reader)
                MessageType.Large -> readLargeMessage(reader)
                MessageType.Continuation -> readContinuationMessage(reader)
                MessageType.ContinuationEnd -> readContinuationEndMessage(reader)
                MessageType.Unknown -> {
                    logger.error { "Received unknown message. Closing connection." }
                    socket.close()
                }
            }
        }
        logger.info { "LogLite session disconnected" }
    }

    fun stop() {
        socket.close()
    }

    private fun readConnectionMessage(reader: ByteReader) {
        val version = reader.read4ByteNumber()
        reader.skip(4)
        val pid = reader.read8ByteNumber()
        val machineName = reader.readString(32)
        val executablePath = reader.readString(260)
        logger.info { "Connection -> Version: $version, PID: $pid, machineName: $machineName, executablePath: $executablePath" }
        client = Client(
            pid = pid,
            machine = machineName,
            executable = executablePath,
        )
    }

    private fun readSimpleMessage(reader: ByteReader) {
        val logMessage = readLogMessage(reader)
        log(logMessage)
        handleSessionMessage(logMessage)
    }

    private fun readLargeMessage(reader: ByteReader) {
        incompleteMessage = readLogMessage(reader)
    }

    private fun readContinuationMessage(reader: ByteReader) {
        val logMessage = readLogMessage(reader)
        incompleteMessage?.let {
            incompleteMessage = it.copy(message = it.message + logMessage.message)
        }
    }

    private fun readContinuationEndMessage(reader: ByteReader) {
        val logMessage = readLogMessage(reader)
        incompleteMessage?.let {
            val completeMessage = it.copy(message = it.message + logMessage.message)
            log(completeMessage)
        }
    }

    private fun readLogMessage(reader: ByteReader): LogMessage {
        val timestamp = Instant.ofEpochMilli(reader.read8ByteNumber())
        val severity = getLogSeverity(reader.read4ByteNumber())
        val module = reader.readString(32)
        val channel = reader.readString(32)
        val message = reader.readString(256)
        return LogMessage(client!!, timestamp, severity, module, channel, message)
    }

    private fun handleSessionMessage(message: LogMessage) {
        if (message.channel == "General") {
            if (message.message == "Terminate - atexit done, about to shut down") {
                throw IOException("Client closed")
            }
        }
    }

    private fun log(message: LogMessage) {
        onNewMessage(message)
    }

    private fun getMessageType(value: Int) = when (value) {
        0 -> MessageType.Connection
        1 -> MessageType.Simple
        2 -> MessageType.Large
        3 -> MessageType.Continuation
        4 -> MessageType.ContinuationEnd
        else -> MessageType.Unknown
    }

    enum class MessageType {
        Connection,
        Simple,
        Large,
        Continuation,
        ContinuationEnd,
        Unknown,
    }

    private fun getLogSeverity(value: Int) = when (value) {
        0 -> LogSeverity.Info
        1 -> LogSeverity.Notice
        2 -> LogSeverity.Warn
        3 -> LogSeverity.Error
        4 -> LogSeverity.Count
        else -> LogSeverity.Unknown
    }
}
