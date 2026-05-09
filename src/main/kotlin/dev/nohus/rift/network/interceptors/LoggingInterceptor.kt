package dev.nohus.rift.network.interceptors

import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.EndpointTag
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.annotation.Single
import retrofit2.Invocation

private val logger = KotlinLogging.logger {}

@Single
class LoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        val request = chain.request()
        val invocation = request.tag(Invocation::class.java)
        val endpointAnnotation = invocation?.method()?.getAnnotation(EndpointTag::class.java)
        val endpoint = endpointAnnotation?.value?.objectInstance ?: run {
            request.tag(Endpoint::class.java)
        }
        val isLogging = endpoint?.isCounted != false
        if (isLogging) logger.debug { "Request --> ${request.url}" }
        try {
            val response = chain.proceed(request)
            if (isLogging) logger.debug { "Response <== [${response.code}] ${request.url}" }
            return@runBlocking response
        } catch (e: Exception) {
            logger.info { "Failure <== ${request.url}" }
            throw e
        }
    }
}
