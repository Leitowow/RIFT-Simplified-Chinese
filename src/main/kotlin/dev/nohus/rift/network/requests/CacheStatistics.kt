package dev.nohus.rift.network.requests

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class CacheStatistics {

    private val mutex = Mutex()
    private val _requests = MutableStateFlow<Map<BucketKey, Int>>(emptyMap())
    val requests = _requests.asStateFlow()

    data class BucketKey(
        val endpoint: Endpoint,
        val cacheStatus: CacheStatus,
        val responseStatus: Int?,
    )

    enum class CacheStatus {
        LocalCacheHit,
        EsiCacheHitNotModified,
        EsiCacheMissNotModified,
        EsiCacheHit,
        EsiCacheMiss,
        EsiDynamic,
        EsiRevalidated,
        EsiExpired,
        EsiNull,
        Unknown,
    }

    fun addRequest(endpoint: Endpoint, cacheStatus: CacheStatus, responseStatus: Int?) {
        runBlocking {
            mutex.withLock {
                val cacheStatus = when (cacheStatus) {
                    CacheStatus.EsiCacheHit if responseStatus == 304 -> CacheStatus.EsiCacheHitNotModified
                    CacheStatus.EsiCacheMiss if responseStatus == 304 -> CacheStatus.EsiCacheMissNotModified
                    else -> cacheStatus
                }
                val key = BucketKey(endpoint, cacheStatus, responseStatus)
                val count = _requests.value.getOrDefault(key, 0) + 1
                _requests.value += (key to count)
            }
        }
    }
}
