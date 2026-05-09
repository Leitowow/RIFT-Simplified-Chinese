package dev.nohus.rift.network.requests

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.koin.core.annotation.Single
import retrofit2.Invocation
import java.time.Instant
import java.util.Collections.synchronizedList

@Single
class RequestStatisticsInterceptor : Interceptor {

    data class Bucket(
        val epochSeconds: Long,
        var requests: List<StatisticsRequest> = listOf(),
    )

    data class StatisticsRequest(
        val endpoint: Endpoint,
        val originator: Originator,
        val timestamp: Instant,
        var response: StatisticsResponse? = null,
    )

    data class StatisticsResponse(
        val timestamp: Instant,
        val isSuccess: Boolean,
    )

    private val mutex = Mutex()
    private val _buckets: MutableList<Bucket> = synchronizedList(mutableListOf())
    val buckets: MutableSharedFlow<List<Bucket>> = MutableSharedFlow(replay = 1)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val originator = request.tag(Originator::class.java) ?: return createSyntheticFailure(request, "Missing originator tag")
        val invocation = request.tag(Invocation::class.java)
        val endpointAnnotation = invocation?.method()?.getAnnotation(EndpointTag::class.java)
        val endpoint = endpointAnnotation?.value?.objectInstance ?: run {
            request.tag(Endpoint::class.java) ?: return createSyntheticFailure(request, "Missing endpoint tag")
        }

        val statisticsRequest = StatisticsRequest(endpoint, originator, Instant.now())
        runBlocking {
            addRequest(statisticsRequest)
        }

        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            val statisticsResponse = StatisticsResponse(Instant.now(), isSuccess = false)
            runBlocking {
                addResponse(statisticsRequest, statisticsResponse)
            }
            throw e
        }

        val statisticsResponse = StatisticsResponse(Instant.now(), isSuccess = response.code in 200..399)

        runBlocking {
            addResponse(statisticsRequest, statisticsResponse)
        }

        return response
    }

    /**
     * Adds a request to the statistics without intercepting it
     */
    fun addExternalRequest(originator: Originator, endpoint: Endpoint, isSuccess: Boolean) = runBlocking {
        val now = Instant.now()
        val statisticsResponse = StatisticsResponse(now, isSuccess)
        val statisticsRequest = StatisticsRequest(endpoint, originator, now, statisticsResponse)
        addRequest(statisticsRequest)
    }

    private suspend fun getCurrentBucket(): Bucket {
        cleanOldBuckets()
        val latestBucket = mutex.withLock {
            _buckets.lastOrNull()
        }
        val currentEpochSeconds = Instant.now().epochSecond
        return if (latestBucket == null || currentEpochSeconds > latestBucket.epochSeconds) {
            Bucket(currentEpochSeconds).also {
                mutex.withLock { _buckets.add(it) }
            }
        } else {
            latestBucket
        }
    }

    private suspend fun cleanOldBuckets() {
        mutex.withLock {
            if (_buckets.size >= MAX_BUCKETS * 2) {
                _buckets.subList(0, _buckets.size - MAX_BUCKETS).clear()
            }
        }
    }

    private suspend fun addRequest(request: StatisticsRequest) {
        if (request.endpoint.isCounted) {
            val bucket = getCurrentBucket()
            bucket.requests += request
            publish()
        }
    }

    private suspend fun addResponse(request: StatisticsRequest, response: StatisticsResponse) {
        request.response = response
        publish()
    }

    private suspend fun publish() {
        buckets.emit(_buckets)
    }

    companion object {
        const val MAX_BUCKETS = 120
    }

    private fun createSyntheticFailure(request: okhttp3.Request, message: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(400)
            .message(message)
            .body(message.toResponseBody(null))
            .build()
    }
}
