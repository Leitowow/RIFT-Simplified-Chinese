package dev.nohus.rift.network.interceptors

import dev.nohus.rift.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.annotation.Single

@Single
class UserAgentInterceptor : Interceptor {

    companion object {
        const val USER_AGENT_KEY = "User-Agent"
    }
    private val riftFragment = "RIFT/${BuildConfig.version}-source"
    private val contactFragment = "developer@riftforeve.online; discord:nohus"
    val userAgentValue = "$riftFragment ($contactFragment)"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newRequest = request.newBuilder()
            .header(USER_AGENT_KEY, userAgentValue)
            .build()
        return chain.proceed(newRequest)
    }
}
