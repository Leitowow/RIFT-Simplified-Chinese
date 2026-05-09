package dev.nohus.rift.loglite

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single
import java.io.IOException
import java.net.ServerSocket

private val logger = KotlinLogging.logger {}

@Single
class LogLiteServer(
    private val logLiteRepository: LogLiteRepository,
) {

    private var sessions = mutableListOf<LogLiteSession>()

    suspend fun start() = coroutineScope {
        val serverSocket = try {
            ServerSocket(3273)
        } catch (e: IOException) {
            logger.error { "Could not bind server for LogLite: ${e.message}" }
            return@coroutineScope
        }
        addShutdownHook()

        while (true) {
            val socket = serverSocket.accept()
            launch(Dispatchers.IO) {
                val session = LogLiteSession(
                    socket = socket,
                    onNewMessage = {
                        logLiteRepository.add(it)
                    },
                )
                sessions += session
                try {
                    session.start()
                } catch (e: IOException) {
                    logger.warn { "LogLite session ended: ${e.message}" }
                    session.stop()
                    sessions -= session
                }
            }
        }
    }

    private fun addShutdownHook() {
        try {
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    runBlocking(Dispatchers.IO) {
                        sessions.toList().forEach {
                            launch {
                                it.stop()
                            }
                        }
                    }
                },
            )
        } catch (e: IllegalStateException) {
            logger.warn { "Could not add shutdown hook for LogLite: ${e.message}" }
        }
    }
}
