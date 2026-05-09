package dev.nohus.rift.network.pushover

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import dev.nohus.rift.network.requests.Originator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Single
class PushoverApi(
    @Named("network") private val json: Json,
    @Named("api") client: OkHttpClient,
) {

    private val contentType = "application/json".toMediaType()
    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("https://api.pushover.net/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
    private val service = retrofit.create(PushoverService::class.java)

    suspend fun postMessages(messages: Messages): Result<MessagesResponse> {
        return try {
            Success(withContext(Dispatchers.IO) { service.postMessages(Originator.Alerts, messages) })
        } catch (e: Exception) {
            if (e is HttpException) {
                val body = e.response()?.errorBody()?.string()
                if (body != null) {
                    try {
                        val errorResponse: MessagesResponse = json.decodeFromString(body)
                        Success(errorResponse)
                    } catch (ignored: SerializationException) {
                        Failure(e)
                    }
                } else {
                    Failure(e)
                }
            } else {
                Failure(e)
            }
        }
    }
}
