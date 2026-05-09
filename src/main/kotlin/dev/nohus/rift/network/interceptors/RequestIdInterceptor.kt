package dev.nohus.rift.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.annotation.Single
import java.util.UUID

/**
 * Adds a request ID tag to the request, readable in other interceptors
 */
@Single
class RequestIdInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestId = UUID.randomUUID()
        val request = chain.request()
            .newBuilder()
            .tag(UUID::class.java, requestId)
            .build()
        return chain.proceed(request)
    }
}
