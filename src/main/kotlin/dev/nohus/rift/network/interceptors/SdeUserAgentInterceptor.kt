package dev.nohus.rift.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.annotation.Single

@Single
class SdeUserAgentInterceptor : Interceptor {

    companion object {
        const val USER_AGENT_KEY = "User-Agent"
        private const val USER_AGENT = "RIFT Static Data Build Pipeline (developer@riftforeve.online; discord:nohus)"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(USER_AGENT_KEY, USER_AGENT)
            .build()
        return chain.proceed(request)
    }
}
