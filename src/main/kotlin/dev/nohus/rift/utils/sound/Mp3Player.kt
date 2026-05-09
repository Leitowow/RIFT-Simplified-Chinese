package dev.nohus.rift.utils.sound

import dev.nohus.rift.jukebox.Source
import dev.nohus.rift.utils.sound.jlayer.Player
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Factory
import java.io.IOException
import java.io.InputStream
import java.net.URI
import kotlin.io.path.inputStream

private val logger = KotlinLogging.logger {}

@Factory
class Mp3Player {

    private val scope = CoroutineScope(SupervisorJob())
    private var session: PlaySession? = null
    private var volume: Double = 1.0
    private val mutex = Mutex()

    data class PlaySession(
        val job: Job,
        val player: Player,
        val semaphore: Semaphore,
    )

    fun play(
        source: Source,
        onPositionUpdated: (Int) -> Unit,
        onFinished: () -> Unit,
    ) {
        scope.launch(Dispatchers.IO) {
            mutex.withLock {
                session?.let {
                    it.player.close()
                    it.job.cancel()
                }
                session = null

                val inputStream = openSource(source) ?: return@launch
                val player = Player(inputStream)
                val pauseSemaphore = Semaphore(1)
                val job = scope.launch(Dispatchers.IO) {
                    var hasSetInitialVolume = false
                    try {
                        while (true) {
                            if (!player.play(1)) break
                            if (!hasSetInitialVolume) {
                                player.setVolume(volume)
                                hasSetInitialVolume = true
                            }
                            onPositionUpdated(player.position)
                            pauseSemaphore.acquire()
                            pauseSemaphore.release()
                        }
                    } catch (e: Exception) {
                        logger.error { "Play failed: ${e.message}" }
                    }
                    onFinished()
                    player.close()
                }
                session = PlaySession(job, player, pauseSemaphore)
            }
        }
    }

    private fun openSource(source: Source): InputStream? {
        try {
            return when (source) {
                is Source.File -> source.path.inputStream()
                is Source.Network -> {
                    val url = URI(source.url).toURL()
                    val connection = url.openConnection()
                    connection.connectTimeout = 5_000
                    connection.readTimeout = 5_000
                    connection.getInputStream()
                }
            }
        } catch (e: IOException) {
            logger.error(e) { "Error opening input stream" }
            return null
        }
    }

    fun resume() {
        try {
            session?.semaphore?.release()
        } catch (e: IllegalStateException) {
            logger.warn { "Could not resume playing (already resumed?): ${e.message}" }
        }
    }

    fun pause() {
        scope.launch {
            session?.semaphore?.acquire()
        }
    }

    /**
     * From 0 to 1
     */
    fun setVolume(volume: Double) {
        this.volume = volume
        session?.player?.setVolume(volume)
    }

    fun close() {
        session?.let {
            it.player.close()
            it.job.cancel()
        }
        scope.cancel()
    }
}
