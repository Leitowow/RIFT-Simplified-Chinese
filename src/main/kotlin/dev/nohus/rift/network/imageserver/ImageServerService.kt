package dev.nohus.rift.network.imageserver

import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.EndpointTag
import dev.nohus.rift.network.requests.Originator
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag

interface ImageServerService {

    @HEAD("characters/{characterId}/portrait")
    @EndpointTag(Endpoint.ImageServiceHeadCharacterPortrait::class)
    suspend fun headCharacterPortrait(
        @Tag originator: Originator,
        @Path("characterId") characterId: Int,
    ): Response<Void>

    @GET("characters/{characterId}/portrait")
    @EndpointTag(Endpoint.ImageServiceHeadCharacterPortrait::class)
    suspend fun getCharacterPortrait(
        @Tag originator: Originator,
        @Path("characterId") characterId: Int,
        @Query("size") size: Int,
    ): Response<ResponseBody>

    @GET("alliances/{allianceId}/logo")
    @EndpointTag(Endpoint.ImageServiceGetAllianceLogo::class)
    suspend fun getAllianceLogo(
        @Tag originator: Originator,
        @Path("allianceId") allianceId: Int,
        @Query("size") size: Int,
    ): Response<ResponseBody>

    @GET("corporations/{corporationId}/logo")
    @EndpointTag(Endpoint.ImageServiceGetCorporationLogo::class)
    suspend fun getCorporationLogo(
        @Tag originator: Originator,
        @Path("corporationId") corporationId: Int,
        @Query("size") size: Int,
    ): Response<ResponseBody>
}
