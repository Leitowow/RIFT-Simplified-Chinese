package dev.nohus.rift.network.requests

import dev.nohus.rift.network.requests.CacheStatistics.CacheStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.annotation.Single
import retrofit2.Invocation

private val logger = KotlinLogging.logger {}

@Single
class CacheStatisticsNetworkInterceptor(
    private val cacheStatistics: CacheStatistics,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val invocation = request.tag(Invocation::class.java)
        val endpointAnnotation = invocation?.method()?.getAnnotation(EndpointTag::class.java)
        val endpoint = endpointAnnotation?.value?.objectInstance ?: request.tag(Endpoint::class.java)
        val response = chain.proceed(request)

        if (endpoint != null) {
            val responseHeaders = response.headers.toMultimap()
            val esiCacheStatus = responseHeaders["x-esi-cache-status"]?.firstOrNull()
            val status = when (esiCacheStatus) {
                "HIT" -> CacheStatus.EsiCacheHit
                "MISS" -> CacheStatus.EsiCacheMiss
                "DYNAMIC" -> CacheStatus.EsiDynamic
                "REVALIDATED" -> CacheStatus.EsiRevalidated
                "EXPIRED" -> CacheStatus.EsiExpired
                null -> CacheStatus.EsiNull
                else -> CacheStatus.Unknown
            }
            if (status == CacheStatus.Unknown) {
                logger.warn { "Unknown cache status for $endpoint: $esiCacheStatus" }
            }
            cacheStatistics.addRequest(endpoint, status, response.code)
        }

        return response
    }
}
