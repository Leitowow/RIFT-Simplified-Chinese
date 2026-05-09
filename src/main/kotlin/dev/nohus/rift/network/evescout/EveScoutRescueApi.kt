package dev.nohus.rift.network.evescout

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.network.requests.RequestExecutor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

@Single
class EveScoutRescueApi(
    @Named("network") json: Json,
    @Named("api") client: OkHttpClient,
    requestExecutor: RequestExecutor,
) : RequestExecutor by requestExecutor {

    private val contentType = "application/json".toMediaType()
    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("https://api.eve-scout.com/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
    private val service = retrofit.create(EveScoutRescueService::class.java)

    suspend fun getObservations(originator: Originator): Result<List<Observation>> {
        return execute { service.getObservations(originator) }
    }

    suspend fun getSignatures(originator: Originator): Result<List<Signature>> {
        return execute { service.getSignatures(originator) }
    }
}
