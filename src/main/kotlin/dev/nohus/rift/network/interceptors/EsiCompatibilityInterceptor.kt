package dev.nohus.rift.network.interceptors

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.koin.core.annotation.Single

/**
 * [Documentation](https://developers.eveonline.com/docs/services/esi/overview/#versioning)
 */
@Single
class EsiCompatibilityInterceptor : Interceptor {

    companion object {
        const val COMPATIBILITY_DATE_KEY = "X-Compatibility-Date"
        const val COMPATIBILITY_DATE = "2025-12-16"
        const val COMPATIBILITY_DATE_EARLY_ACCESS = "2099-01-01"
    }

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        val request = chain.request()
        val newRequest = request.newBuilder()
            .header(COMPATIBILITY_DATE_KEY, request.getRequiredCompatibilityDate())
            .build()
        chain.proceed(newRequest)
    }

    private fun Request.getRequiredCompatibilityDate(): String {
        return if (isEarlyAccess()) COMPATIBILITY_DATE_EARLY_ACCESS else COMPATIBILITY_DATE
    }

    @Suppress("UnusedReceiverParameter")
    private fun Request.isEarlyAccess(): Boolean {
        return false
    }
}
