package dev.nohus.rift.network.interceptors

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.annotation.Single

/**
 * Rewrites responses with a 302 status code to 200 so that the redirect can be handled by application code
 */
@Single
class RedirectAsSuccessInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code == 302) {
            return@runBlocking response.newBuilder()
                .code(200)
                .build()
        } else {
            return@runBlocking response
        }
    }
}
