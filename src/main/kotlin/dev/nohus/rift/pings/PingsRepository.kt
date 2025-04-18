package dev.nohus.rift.pings

import dev.nohus.rift.alerts.AlertsTriggerController
import dev.nohus.rift.jabber.client.JabberClient
import dev.nohus.rift.jabber.client.UserChatController
import dev.nohus.rift.utils.directories.AppDirectories
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.FileSystemException
import java.nio.file.NoSuchFileException
import java.time.Duration
import java.time.Instant
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val logger = KotlinLogging.logger {}

@Single
class PingsRepository(
    appDirectories: AppDirectories,
    @Named("settings") private val json: Json,
    private val jabberClient: JabberClient,
    private val parsePingUseCase: ParsePingUseCase,
    private val alertsTriggerController: AlertsTriggerController,
    private val pingTranslator: PingTranslator,
) {

    private val pingsFile = appDirectories.getAppDataDirectory().resolve("pings.json")
    private var lastReceivedTimestamp: Instant = Instant.EPOCH
    private val scope = CoroutineScope(Job())
    private val mutex = Mutex()

    private val _pings = MutableStateFlow<List<PingModel>>(emptyList())
    val pings = _pings.asStateFlow()

    init {
        pingsFile.createParentDirectories()
        val savedPings = load()
        val recentPings = savedPings.filter { it.timestamp.isAfter(Instant.now() - Duration.ofHours(48)) }
        _pings.update { recentPings }
    }

    private fun translatePing(ping: PingModel): PingModel {
        return when (ping) {
            is PingModel.PlainText -> ping.copy(
                sourceText = pingTranslator.translate(ping.sourceText),
                text = pingTranslator.translate(ping.text),
            )
            is PingModel.FleetPing -> ping.copy(
                sourceText = pingTranslator.translate(ping.sourceText),
                description = pingTranslator.translate(ping.description),
                fleet = ping.fleet?.let { pingTranslator.translate(it) },
                formupLocations = ping.formupLocations.map { location ->
                    when (location) {
                        is FormupLocation.Text -> FormupLocation.Text(pingTranslator.translate(location.text))
                        is FormupLocation.System -> location
                    }
                },
            )
        }
    }

    private fun load(): List<PingModel> {
        return try {
            val serialized = pingsFile.readText()
            val loadedPings = json.decodeFromString<List<PingModel>>(serialized)
            // Translate the source text of each ping
            loadedPings.map { translatePing(it) }
        } catch (e: NoSuchFileException) {
            logger.info { "Pings file not found" }
            emptyList()
        } catch (e: FileSystemException) {
            logger.error(e) { "Pings file could not be read" }
            pingsFile.deleteIfExists()
            emptyList()
        } catch (e: SerializationException) {
            logger.error(e) { "Could not deserialize pings" }
            pingsFile.deleteIfExists()
            emptyList()
        }
    }

    private suspend fun onNewPingMessage(message: UserChatController.UserMessage) {
        val ping = parsePingUseCase(message.timestamp, message.text) ?: return
        val translatedPing = translatePing(ping)
        _pings.update { (it + translatedPing).sortedBy { it.timestamp } }
        alertsTriggerController.onNewJabberPing(ping)
        save(_pings.value)
    }

    private fun save(model: List<PingModel>) = scope.launch(Dispatchers.IO) {
        mutex.withLock {
            try {
                val serialized = json.encodeToString(model)
                pingsFile.writeText(serialized)
            } catch (e: FileSystemException) {
                logger.error { "Could not write pings: $e" }
            } catch (e: IOException) {
                logger.error { "Could not write pings: $e" }
            }
        }
    }

    suspend fun start() = coroutineScope {
        launch {
            jabberClient.state.map {
                it.userChatMessages.entries.firstOrNull {
                    it.key.xmppAddressOfChatPartner.localpartOrNull?.toString() in listOf("directorbot")
                }?.value ?: emptyList()
            }.collect {
                val newMessages = it.reversed().takeWhile { it.timestamp > lastReceivedTimestamp }
                newMessages.maxOfOrNull { it.timestamp }?.let { lastReceivedTimestamp = it }
                newMessages.forEach { onNewPingMessage(it) }
            }
        }
    }
}
