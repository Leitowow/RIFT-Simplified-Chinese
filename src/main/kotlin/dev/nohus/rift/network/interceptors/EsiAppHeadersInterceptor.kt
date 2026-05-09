package dev.nohus.rift.network.interceptors

import dev.nohus.rift.BuildConfig
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.utils.OperatingSystem
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.koin.core.annotation.Single
import java.util.UUID

@Single
class EsiAppHeadersInterceptor(
    operatingSystem: OperatingSystem,
) : Interceptor {

    companion object {
        const val OPERATING_SYSTEM_KEY = "App-Operating-System"
        const val FEATURE_KEY = "App-Feature"
        const val NAME_KEY = "App-Name"
        const val VERSION_KEY = "App-Version"
        const val CONTACT_KEY = "App-Contact"
        const val REQUEST_ID_KEY = "App-Request-Id"
    }

    private val operatingSystemValue = when (operatingSystem) {
        OperatingSystem.Linux -> "Linux"
        OperatingSystem.Windows -> "Windows"
        OperatingSystem.MacOs -> "macOS"
    }
    private val nameValue = "RIFT"
    private val versionValue = "${BuildConfig.version}-source"
    private val contactValue = "developer@riftforeve.online; discord:nohus"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val originator = request.tag(Originator::class.java) ?: return createSyntheticFailure(request, "Missing originator tag")
        val requestId = request.tag(UUID::class.java) ?: return createSyntheticFailure(request, "Missing requestId tag")

        val newRequest = request.newBuilder()
            .header(NAME_KEY, nameValue)
            .header(VERSION_KEY, versionValue)
            .header(CONTACT_KEY, contactValue)
            .header(OPERATING_SYSTEM_KEY, operatingSystemValue)
            .header(FEATURE_KEY, originator.name)
            .header(REQUEST_ID_KEY, requestId.toString())
            .build()
        return chain.proceed(newRequest)
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
