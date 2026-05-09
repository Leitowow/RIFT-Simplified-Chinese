package dev.nohus.rift.network.evescout

import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.EndpointTag
import dev.nohus.rift.network.requests.Originator
import retrofit2.http.GET
import retrofit2.http.Tag

interface EveScoutRescueService {

    @GET("/v2/public/observations")
    @EndpointTag(Endpoint.EveScoutRescue::class)
    suspend fun getObservations(
        @Tag originator: Originator,
    ): List<Observation>

    @GET("/v2/public/signatures")
    @EndpointTag(Endpoint.EveScoutRescue::class)
    suspend fun getSignatures(
        @Tag originator: Originator,
    ): List<Signature>
}
