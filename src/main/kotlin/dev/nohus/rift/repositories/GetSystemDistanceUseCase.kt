package dev.nohus.rift.repositories

import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

@Single
class GetSystemDistanceUseCase(
    private val getRouteUseCase: GetRouteUseCase,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val jumpBridgesRepository: JumpBridgesRepository,
) {

    private data class CacheKey(
        val from: Int,
        val to: Int,
        val withJumpBridges: Boolean,
    )
    private sealed interface CacheValue {
        data object NoRoute : CacheValue
        data class Distance(val distance: Int) : CacheValue
    }

    private val cache = ConcurrentHashMap<CacheKey, CacheValue>()

    @Volatile
    private var jumpBridgeConnectionsKey = jumpBridgesRepository.connectionsKey

    operator fun invoke(from: Int, to: Int, withJumpBridges: Boolean): Int? {
        if (from == to) return 0

        invalidateCacheIfJumpBridgesChanged()
        val key = CacheKey(from, to, withJumpBridges)

        val value = cache.computeIfAbsent(key) {
            if (!solarSystemsRepository.isKnownSpace(from) || !solarSystemsRepository.isKnownSpace(to)) {
                CacheValue.NoRoute
            } else {
                val distance = getRouteUseCase(from, to, withJumpBridges)?.let { it.size - 1 }
                distance?.let { CacheValue.Distance(it) } ?: CacheValue.NoRoute
            }
        }

        return when (value) {
            is CacheValue.Distance -> value.distance
            CacheValue.NoRoute -> null
        }
    }

    /**
     * Clears cached distances if the jump bridge network changed
     */
    private fun invalidateCacheIfJumpBridgesChanged() {
        val current = jumpBridgesRepository.connectionsKey
        if (current != jumpBridgeConnectionsKey) {
            jumpBridgeConnectionsKey = current
            cache.keys.removeIf { it.withJumpBridges }
        }
    }
}
