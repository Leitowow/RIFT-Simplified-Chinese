package dev.nohus.rift.network.zkillboard

import retrofit2.http.GET

interface ZkillboardService {

    @GET("recentactivity/")
    suspend fun getRecentActivity(): RecentActivity
}
