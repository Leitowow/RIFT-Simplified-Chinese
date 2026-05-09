package dev.nohus.rift.network.zkillboardr2z2

import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.EndpointTag
import dev.nohus.rift.network.requests.Originator
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Tag

interface ZkillboardR2Z2Service {

    @GET("ephemeral/sequence.json")
    @EndpointTag(Endpoint.ZkillboardR2Z2Sequence::class)
    suspend fun getSequence(
        @Tag originator: Originator,
    ): Sequence

    @GET("ephemeral/{id}.json")
    @EndpointTag(Endpoint.ZkillboardR2Z2Killmail::class)
    suspend fun getKillmail(
        @Tag originator: Originator,
        @Path("id") id: Long,
    ): R2Z2Killmail
}
