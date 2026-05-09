package dev.nohus.rift.network.zkillboard

import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.EndpointTag
import dev.nohus.rift.network.requests.Originator
import retrofit2.http.GET
import retrofit2.http.Tag

interface ZkillboardService {

    @GET("recentactivity/")
    @EndpointTag(Endpoint.Zkillboard::class)
    suspend fun getRecentActivity(
        @Tag originator: Originator,
    ): RecentActivity
}
