package dev.nohus.rift.network.pushover

import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.EndpointTag
import dev.nohus.rift.network.requests.Originator
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Tag

interface PushoverService {

    @POST("/1/messages.json")
    @EndpointTag(Endpoint.Pushover::class)
    suspend fun postMessages(
        @Tag originator: Originator,
        @Body message: Messages,
    ): MessagesResponse
}
