package dev.nohus.rift.network.ntfy

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.core.annotation.Single
import retrofit2.Retrofit

@Single
class NtfyApi(
    json: Json,
    client: OkHttpClient,
) {

    private val contentType = "application/json".toMediaType()
    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("https://ntfy.sh/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
    private val service = retrofit.create(NtfyService::class.java)

    suspend fun post(request: Ntfy): Result<Unit> {
        return try {
            Success(withContext(Dispatchers.IO) { service.post(request) })
        } catch (e: Exception) {
            Failure(e)
        }
    }
}
