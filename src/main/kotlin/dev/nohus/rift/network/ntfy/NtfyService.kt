package dev.nohus.rift.network.ntfy

import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.EndpointTag
import dev.nohus.rift.network.requests.Originator
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Tag

interface NtfyService {

    @POST("/")
    @EndpointTag(Endpoint.Ntfy::class)
    suspend fun post(
        @Tag originator: Originator,
        @Body body: Ntfy,
    )
}
