package dev.nohus.rift.network.requests

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.koin.core.annotation.Single

@Single
class RateLimitingInterceptor : Interceptor {

    private val mutex = Mutex()
    private val semaphores = mutableMapOf<Originator, Semaphore>()

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        val request = chain.request()
        val originator = request.tag(Originator::class.java) ?: return@runBlocking createSyntheticFailure(request, "Missing originator tag")

        getSemaphore(originator).withPermit {
            chain.proceed(request)
        }
    }

    private suspend fun getSemaphore(originator: Originator): Semaphore {
        return mutex.withLock {
            semaphores.computeIfAbsent(originator) { Semaphore(getConcurrencyLimit(originator)) }
        }
    }

    private fun getConcurrencyLimit(originator: Originator): Int = when (originator) {
        else -> 10
    }

    private fun createSyntheticFailure(request: Request, message: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(400)
            .message(message)
            .body(message.toResponseBody(null))
            .build()
    }
}
