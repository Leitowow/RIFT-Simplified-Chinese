package dev.nohus.rift.repositories.character

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.zkillboard.RecentActivity
import dev.nohus.rift.network.zkillboard.ZkillboardApi
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

@Single
class ZkillboardRecentActivityRepository(
    private val zkillboardApi: ZkillboardApi,
) {

    var activeCharacterIds: Set<Int>? = null
        private set

    suspend fun start() {
        while (true) {
            val recentActivity = getRecentActivity()
            if (recentActivity != null) {
                activeCharacterIds = recentActivity.characterIds.toSet()
                logger.info { "Recent activity loaded from zKillboard" }
                delay(1.hours)
            } else {
                delay(30.seconds)
            }
        }
    }

    private suspend fun getRecentActivity(): RecentActivity? {
        return when (val response = zkillboardApi.getRecentActivity()) {
            is Result.Success -> {
                response.data
            }
            is Result.Failure -> {
                logger.error { "Could not get recent activity from zKillboard: ${response.cause?.message}" }
                null
            }
        }
    }
}
