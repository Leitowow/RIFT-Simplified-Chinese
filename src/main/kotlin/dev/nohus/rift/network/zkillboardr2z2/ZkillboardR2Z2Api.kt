package dev.nohus.rift.network.zkillboardr2z2

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.network.requests.Reply
import dev.nohus.rift.network.requests.RequestExecutor
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private val logger = KotlinLogging.logger {}

@Single
class ZkillboardR2Z2Api(
    @Named("network") json: Json,
    @Named("zkillr2z2") client: OkHttpClient,
    requestExecutor: RequestExecutor,
) : RequestExecutor by requestExecutor {

    private val contentType = "application/json".toMediaType()
    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("https://r2z2.zkillboard.com/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
    private val service = retrofit.create(ZkillboardR2Z2Service::class.java)

    init {
        setHttpErrorHandler(::handleHttpError)
    }

    suspend fun getSequence(originator: Originator): Result<Sequence> {
        return execute { service.getSequence(originator) }
    }

    suspend fun getKillmail(originator: Originator, killmailId: Long): Result<R2Z2Killmail> {
        return execute { service.getKillmail(originator, killmailId) }
    }

    private fun handleHttpError(e: HttpException): Failure? {
        if (e.code() == 404) {
            // Expected
        } else {
            logger.error { "API HTTP error: ${e.code()}" }
        }
        return null
    }
}
