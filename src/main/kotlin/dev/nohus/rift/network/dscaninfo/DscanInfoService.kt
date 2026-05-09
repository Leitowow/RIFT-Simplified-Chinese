package dev.nohus.rift.network.dscaninfo

import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.EndpointTag
import dev.nohus.rift.network.requests.Originator
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Tag

interface DscanInfoService {

    @GET("/v/{id}?output=json")
    @EndpointTag(Endpoint.DscanInfo::class)
    suspend fun getScan(
        @Tag originator: Originator,
        @Path("id") scanId: String,
    ): DscanInfoScanResponse
}
