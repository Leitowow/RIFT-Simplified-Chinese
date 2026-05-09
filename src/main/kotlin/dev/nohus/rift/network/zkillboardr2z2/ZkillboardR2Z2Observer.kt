package dev.nohus.rift.network.zkillboardr2z2

import dev.nohus.rift.killboard.Attacker
import dev.nohus.rift.killboard.Killmail
import dev.nohus.rift.killboard.KillmailProcessor
import dev.nohus.rift.killboard.Victim
import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import dev.nohus.rift.network.requests.Originator.Killmails
import dev.nohus.rift.repositories.Position
import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import retrofit2.HttpException
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}
private val PAST_POLL_REQUEST_DELAY = Duration.ofMillis(500).toMillis()
private val POLL_REQUEST_DELAY = Duration.ofSeconds(6).toMillis()
private val POLL_SUCCESS_DELAY = Duration.ofMillis(100).toMillis()
private val FAILED_REQUEST_DELAY = Duration.ofSeconds(10).toMillis()
private val MAX_WAIT_BEFORE_SEQUENCE_RECHECK = Duration.ofSeconds(30)

@Single
class ZkillboardR2Z2Observer(
    private val zkillboardR2Z2Api: ZkillboardR2Z2Api,
    private val killmailProcessor: KillmailProcessor,
    private val settings: Settings,
) {
    private var isEnabled: Boolean = settings.isZkillboardMonitoringEnabled
    private var maxAge: Duration = Duration.ofMinutes(5)

    suspend fun start() = coroutineScope {
        launch {
            settings.updateFlow.map { it.intelExpireSeconds }.collect {
                maxAge = Duration.ofSeconds(it.toLong()).coerceAtMost(Duration.ofMinutes(15))
            }
        }
        launch {
            settings.updateFlow.map { it.isZkillboardMonitoringEnabled }.collect {
                isEnabled = it
            }
        }

        launch {
            val latestKillmailId = getLatestKillmailId()
            val pastKillmailsFlow = observePastKillmails(latestKillmailId)
            val killmailsFlow = observeKillmails(latestKillmailId + 1)

            launch {
                pastKillmailsFlow.collect {
                    killmailProcessor.submit(it)
                }
            }
            launch {
                killmailsFlow.collect {
                    killmailProcessor.submit(it)
                }
            }
        }
    }

    private suspend fun getLatestKillmailId(): Long {
        while (true) {
            when (val result = zkillboardR2Z2Api.getSequence(Killmails)) {
                is Success -> {
                    val killmailId = result.data.sequence
                    logger.debug { "Killmail sequence number: $killmailId" }
                    return killmailId
                }
                is Failure -> {
                    logger.error { "Failed to receive current killmail sequence number" }
                    delay(FAILED_REQUEST_DELAY)
                }
            }
        }
    }

    private fun observePastKillmails(fromId: Long): Flow<Killmail> = channelFlow {
        var currentId = fromId
        var fetchedKillmailsCount = 0
        var failureCount = 0
        while (true) {
            val killmail = getKillmail(currentId) as? KillmailReply.Success
            delay(PAST_POLL_REQUEST_DELAY)
            killmail?.also { send(it.killmail) }
            fetchedKillmailsCount++
            currentId--
            val date = killmail?.timestamp
            if (date == null) {
                failureCount++
                if (failureCount > 5) {
                    logger.error { "Could not fetch 5 past killmails in a row, cancelling. Fetched $fetchedKillmailsCount past killmails." }
                    break
                }
            } else if (date < Instant.now().minus(maxAge)) {
                logger.debug { "All $fetchedKillmailsCount past killmails were fetched up to intel expiry date." }
                break
            }
        }
    }

    private fun observeKillmails(fromId: Long) = flow {
        var currentId = fromId
        var lastSuccess = Instant.now()
        while (true) {
            when (val reply = getKillmail(currentId)) {
                is KillmailReply.Success -> {
                    emit(reply.killmail)
                    currentId++
                    lastSuccess = Instant.now()
                    delay(POLL_SUCCESS_DELAY)
                }
                KillmailReply.NotFound -> {
                    val timeSinceLastSuccess = Duration.between(lastSuccess, Instant.now())
                    if (timeSinceLastSuccess > MAX_WAIT_BEFORE_SEQUENCE_RECHECK) {
                        logger.warn { "No killmail found for ${timeSinceLastSuccess.toSeconds()}s, rechecking sequence" }
                        currentId = getLatestKillmailId()
                    }
                    delay(POLL_REQUEST_DELAY)
                }
                is KillmailReply.Failure -> {
                    logger.error { "Failed getting killmail: ${reply.cause?.message ?: "unknown error"}" }
                    delay(FAILED_REQUEST_DELAY)
                }
            }
        }
    }

    private sealed interface KillmailReply {
        data class Success(val killmail: Killmail, val timestamp: Instant, val sequenceId: Long) : KillmailReply
        data object NotFound : KillmailReply
        data class Failure(val cause: Throwable?) : KillmailReply
    }

    private suspend fun getKillmail(killmailId: Long): KillmailReply {
        return when (val result = zkillboardR2Z2Api.getKillmail(Killmails, killmailId)) {
            is Success -> {
                val response = result.data
                KillmailReply.Success(mapKillmail(response.esi), response.uploadedAt, response.sequenceId)
            }
            is Failure -> {
                if (result.cause is HttpException && result.cause.code() == 404) {
                    KillmailReply.NotFound
                } else {
                    KillmailReply.Failure(result.cause)
                }
            }
        }
    }

    private fun mapKillmail(killmail: EsiKillmail): Killmail {
        return Killmail(
            killmailId = killmail.killmailId,
            killmailTime = killmail.killmailTime,
            solarSystemId = killmail.solarSystemId.toInt(),
            url = "https://zkillboard.com/kill/${killmail.killmailId}/",
            victim = Victim(
                characterId = killmail.victim.characterId?.toInt(),
                corporationId = killmail.victim.corporationId?.toInt(),
                allianceId = killmail.victim.allianceId?.toInt(),
                shipTypeId = killmail.victim.shipTypeId.toInt(),
            ),
            attackers = killmail.attackers.map { attacker ->
                Attacker(
                    characterId = attacker.characterId?.toInt(),
                    corporationId = attacker.corporationId?.toInt(),
                    allianceId = attacker.allianceId?.toInt(),
                    shipTypeId = attacker.shipTypeId?.toInt(),
                )
            },
            position = killmail.victim.position?.let { Position(it.x, it.y, it.z) },
        )
    }
}
