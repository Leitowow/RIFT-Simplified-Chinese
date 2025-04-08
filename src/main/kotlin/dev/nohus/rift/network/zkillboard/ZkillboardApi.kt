package dev.nohus.rift.network.zkillboard

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.nohus.rift.network.RequestExecutor
import dev.nohus.rift.network.Result
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.core.annotation.Single
import retrofit2.Retrofit

@Single
class ZkillboardApi(
    json: Json,
    client: OkHttpClient,
    requestExecutor: RequestExecutor,
) : RequestExecutor by requestExecutor {

    private val contentType = "application/json".toMediaType()
    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("https://zkillboard.com/api/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
    private val service = retrofit.create(ZkillboardService::class.java)

    suspend fun getRecentActivity(): Result<RecentActivity> {
        return execute { service.getRecentActivity() }
    }
}
