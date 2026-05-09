package dev.nohus.rift.network.adashboardinfo

import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.EndpointTag
import dev.nohus.rift.network.requests.Originator
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Tag

interface AdashboardInfoService {

    @GET("/intel/dscan/view/{id}")
    @EndpointTag(Endpoint.ADashboard::class)
    suspend fun getScan(
        @Tag originator: Originator,
        @Path("id") scanId: String,
    ): String
}
