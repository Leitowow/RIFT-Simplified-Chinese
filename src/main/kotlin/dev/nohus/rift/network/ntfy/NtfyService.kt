package dev.nohus.rift.network.ntfy

import retrofit2.http.Body
import retrofit2.http.POST

interface NtfyService {

    @POST("/")
    suspend fun post(
        @Body body: Ntfy,
    )
}
